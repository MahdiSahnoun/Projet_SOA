# Test manuel de bout en bout — Dons (REST) → Registre (SOAP)  · T10

Objectif : vérifier que le client SOAP du service Dons (branche `mahdi`) fonctionne avec le
serveur `registre-service`, sur 4 cas : don accepté, donneur inéligible, donneur inconnu, Registre arrêté.

> Les commandes `curl` sont en syntaxe **Git Bash**. Sous PowerShell, `curl` est un alias de
> `Invoke-WebRequest` : utiliser `curl.exe` ou Postman.

---

## 0. Ce que fait Dons sur `POST /api/v1/dons` (lu dans `ServiceDon.traiterDon`)

Le corps contient **seulement** `{"codeBanque": "BANQUE-A"}`. Le donneur est celui du **JWT** :
son `identifiantNational` est celui saisi à l'inscription dans Dons.

1. Contrôles locaux : banque connue dans Dons (sinon 404), donneur majeur (sinon `REFUSEE`).
2. SOAP `VerifierEligibilite` :
   - `eligible=false` → don **`REFUSEE`, HTTP 201** (avec le motif du Registre) ;
   - Fault `Client` « Donneur inconnu… » → **HTTP 404** ;
   - Registre injoignable / Fault `Server` → **`EN_ATTENTE`, HTTP 202**.
3. Si éligible : SOAP `EnregistrerDon` → `numeroEnregistrement` ; puis gRPC `AjouterPoche` vers Inventaire.
   Inventaire n'existe pas encore → le don reste **`ACCEPTEE`** avec le motif « Inventaire indisponible : poche à saisir par la banque ».

⚠️ Conséquences sur les résultats attendus :
- **un donneur inéligible donne 201 `REFUSEE`, pas 409** (c'est aussi ce que vérifie `DonApiIT` de Dons) ;
- **409** n'arrive que si `EnregistrerDon` renvoie un Fault après un `VerifierEligibilite` positif
  (deux dons simultanés) : pas reproductible à la main ; ce Fault est couvert par
  `RegistreEndpointIntegrationTest.enregistrerDon_contreIndication_faultDonneurInelegible` ;
- **404** demande un donneur inscrit dans Dons mais inconnu du Registre. À l'inscription, Dons appelle
  `VerifierExistence` et refuse un identifiant inconnu (400)… sauf si le Registre est arrêté
  (repli : accepté). Le cas 404 se prépare donc pendant le cas « Registre arrêté ».

---

## 1. Démarrage (lancement local)

| Service | Port | Lancement |
|---|---|---|
| registre_db (Postgres) | 5434 | `docker compose up -d registre_db` (racine du dépôt, branche Malek) |
| config-server | 8888 | `mvn -f services/config-server/pom.xml spring-boot:run` |
| discovery (facultatif) | 8761 | `mvn -f services/discovery/pom.xml spring-boot:run` |
| registre-service | 8083 | `mvn -f services/registre-service/pom.xml spring-boot:run` |
| dons_db (Postgres) | 5435 | conteneur temporaire, voir ci-dessous |
| dons | 8081 | jar construit depuis la branche `mahdi`, voir ci-dessous |

**Dons, sans toucher à la branche `mahdi`** (copie de travail séparée, détachée) :
```bash
git fetch origin
git worktree add ../Projet_SOA-dons origin/mahdi
cd ../Projet_SOA-dons
mvn -f services/pom.xml -pl dons -am package -DskipTests

# 5435 et pas 5432 : un PostgreSQL local occupe déjà 5432 sur ce poste
docker run -d --name dons-db -e POSTGRES_DB=dons_db -e POSTGRES_USER=dons_user \
  -e POSTGRES_PASSWORD=dons -p 5435:5432 postgres:16-alpine

export DB_URL=jdbc:postgresql://localhost:5435/dons_db DB_USER=dons_user DB_PASSWORD=dons
export JWT_SECRET="$(openssl rand -base64 48)"
export COMPTES_TEST_MDP='Demo2026!'          # crée donneur@test.tn (TN-00000001), banque-a@test.tn (BANQUE-A)...
# REGISTRE_URL par défaut = http://localhost:8083/ws : rien à changer
java -jar services/dons/target/dons-1.0.0.jar
```
Après les tests : `git worktree remove ../Projet_SOA-dons` et `docker rm -f dons-db`.

**Contrôles avant de commencer**
```bash
curl -s http://localhost:8081/actuator/health          # {"status":"UP",...}
curl -s http://localhost:8083/ws/registre.wsdl | head  # WSDL du Registre
```
Faire une première requête SOAP « à blanc » (Postman, `VerifierExistence` de `POSTMAN_TESTS.md`) :
le premier appel après démarrage du Registre peut être lent, et Dons coupe à **2 s**.

**Variables utilisées ensuite**
```bash
DONS=http://localhost:8081/api/v1
login() { curl -s -X POST $DONS/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$1\",\"motDePasse\":\"Demo2026!\"}" | sed -E 's/.*"accessToken":"([^"]+)".*/\1/'; }
inscrire() { curl -s -w '\nHTTP %{http_code}\n' -X POST $DONS/donneurs -H 'Content-Type: application/json' \
  -d "{\"nom\":\"$1\",\"email\":\"$2\",\"motDePasse\":\"Demo2026!\",\"identifiantNational\":\"$3\",
       \"dateNaissance\":\"1995-01-15\",\"poids\":72,\"groupeSanguin\":\"$4\"}"; }
proposer() { curl -s -w '\nHTTP %{http_code}\n' -X POST $DONS/dons -H "Authorization: Bearer $1" \
  -H 'Content-Type: application/json' -d '{"codeBanque":"BANQUE-A"}'; }
```

---

## 2. Cas de test (dans cet ordre)

### Cas 1 — TN-00000001 éligible → 201 `ACCEPTEE`
```bash
T1=$(login donneur@test.tn)
proposer $T1
```
**Attendu : HTTP 201**, header `Location: /api/v1/dons/{id}`
```json
{"statut":"ACCEPTEE","groupeSanguin":"O_NEGATIF","codeBanque":"BANQUE-A",
 "motif":"Inventaire indisponible : poche à saisir par la banque",
 "numeroEnregistrement":"REG-TN-00000001-20261122", ...}
```
Côté Registre : `date_dernier_don` de TN-00000001 = aujourd'hui.
Bonus : refaire `proposer $T1` → **201 `REFUSEE`**, motif « délai insuffisant depuis le dernier don : 0 jours (minimum 56) ».
(TN-00000001 redevient éligible au prochain redémarrage du Registre.)

### Cas 2 — TN-00000011 inéligible (don il y a 20 jours) → 201 `REFUSEE` (pas 409)
```bash
inscrire "Walid Saidi" walid@test.tn TN-00000011 O_POSITIF    # HTTP 201 (VerifierExistence = true)
T11=$(login walid@test.tn)
proposer $T11
```
**Attendu : HTTP 201**
```json
{"statut":"REFUSEE","motif":"délai insuffisant depuis le dernier don : 20 jours (minimum 56)",
 "numeroEnregistrement":null, ...}
```
Contrôle à l'inscription : `inscrire "X" x@test.tn TN-99999999 O_NEGATIF` (Registre démarré) →
**HTTP 400** « Identifiant inconnu du registre national ».

### Cas 3 — Registre arrêté → 202 `EN_ATTENTE`
Préparer un donneur pendant que le Registre tourne encore :
```bash
inscrire "Mariem Trabelsi" mariem@test.tn TN-00000002 O_POSITIF   # HTTP 201
T2=$(login mariem@test.tn)
```
**Arrêter `registre-service`** (Ctrl+C), puis :
```bash
proposer $T2
```
**Attendu : HTTP 202**
```json
{"statut":"EN_ATTENTE","motif":"Registre indisponible : vérification d'éligibilité différée", ...}
```
Pendant l'arrêt, préparer le cas 4 (l'inscription passe grâce au repli de `VerifierExistence`) :
```bash
inscrire "Inconnu Test" inconnu@test.tn TN-99999999 O_NEGATIF      # HTTP 201 (repli)
```
Option : `curl -s http://localhost:8081/actuator/circuitbreakers` → après ≥ 5 appels dont ≥ 50 % en échec,
le circuit `registre` passe `OPEN` (les dons restent `EN_ATTENTE`, réponse immédiate).

### Cas 4 — TN-99999999 inconnu du Registre → 404
**Redémarrer `registre-service`.** Si le circuit est `OPEN`, attendre **15 s** (`wait-duration-in-open-state`).
```bash
T99=$(login inconnu@test.tn)
proposer $T99
```
**Attendu : HTTP 404** (ProblemDetail)
```json
{"title":"Ressource introuvable","status":404,
 "detail":"Donneur inconnu du registre national : TN-99999999", ...}
```
Aucun don n'est enregistré (transaction annulée) : `GET $DONS/dons` avec `$T99` → `[]`.

---

## 3. Récapitulatif

| Cas | Donneur | Registre | HTTP | Statut du don |
|---|---|---|---|---|
| 1 | TN-00000001 | démarré | **201** | `ACCEPTEE` + numéro `REG-TN-00000001-yyyyMMdd` |
| 2 | TN-00000011 | démarré | **201** | `REFUSEE` + motif (pas 409) |
| 3 | TN-00000002 | **arrêté** | **202** | `EN_ATTENTE` |
| 4 | TN-99999999 | démarré | **404** | — (rien d'enregistré) |

---

## 4. Compatibilité client Dons ↔ serveur Registre

| Point | Client Dons (`RegistreClient`, `RegistreClientConfig`) | Serveur `registre-service` | |
|---|---|---|---|
| URL | `registre.url` = `http://localhost:8083/ws` | servlet `/ws/*`, port 8083 | ✅ |
| Namespace | `http://donsang.com/registre/v1` | même XSD : module partagé `contrats/registre-soap` | ✅ |
| Fault inconnu | code `Client` + texte qui commence par `Donneur inconnu` → 404 | `Client`, « Donneur inconnu du registre national : … » | ✅ (vérifié par test d'intégration) |
| Fault inéligible | autre Fault `Client` → 409 | `Client`, « Donneur inéligible : … — motif » | ✅ |
| Inéligible dans VerifierEligibilite | `eligible=false` + `motif` | idem, sans Fault | ✅ |
| `groupe` | `groupe.name()` → `O_NEGATIF`… | enum `O_NEGATIF`… | ✅ |
| `dateDon` | `xs:date` du jour | utilisé comme nouvelle `date_dernier_don` | ✅ |
| `numeroEnregistrement` | stocké et renvoyé tel quel | `REG-{id}-{yyyyMMdd}` | ✅ |
| Timeout | 2 s connexion / lecture | premier appel après démarrage parfois plus lent | ⚠️ faire un appel à blanc |
| URL en Docker | `REGISTRE_URL` défaut `http://host.docker.internal:8083/ws` | dans le Compose racine : `registre-service:8083` | ⚠️ si les deux tournent dans le même Compose, mettre `REGISTRE_URL=http://registre-service:8083/ws` |
| Postgres de Dons | `services/docker-compose.yml` publie **5432** | — | ⚠️ conflit avec le PostgreSQL local sur 5432 (d'où le conteneur sur 5435 ci-dessus) |
