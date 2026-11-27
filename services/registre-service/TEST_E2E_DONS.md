# Test manuel de bout en bout — Dons (REST) → Registre (SOAP) + Inventaire (gRPC) · T10

Objectif : vérifier que le service Dons fonctionne avec le Registre (SOAP) et l'Inventaire (gRPC),
sur 4 cas : don accepté, donneur inéligible, Registre arrêté, donneur inconnu.
Tout le code est sur la branche **Malek** : pas de worktree ni d'autre branche à récupérer.

> Commandes pour **Windows PowerShell 5.1**, depuis `C:\Users\malek\Projet_SOA`.
> Utiliser `curl.exe` (dans PowerShell, `curl` est un alias de `Invoke-WebRequest`).

---

## 0. Ce que fait Dons sur `POST /api/v1/dons` (`ServiceDon.traiterDon`)

Le corps contient **seulement** `{"codeBanque": "BANQUE-A"}`. Le donneur est celui du **JWT** :
son `identifiantNational` est celui saisi à l'inscription dans Dons.

1. Contrôles locaux : banque connue dans Dons (sinon 404), donneur majeur (sinon `REFUSEE`).
2. SOAP `VerifierEligibilite` :
   - `eligible=false` → don **`REFUSEE`, HTTP 201** (avec le motif du Registre) ;
   - Fault `Client` « Donneur inconnu… » → **HTTP 404** ;
   - Registre injoignable / Fault `Server` → **`EN_ATTENTE`, HTTP 202**.
3. Si éligible : SOAP `EnregistrerDon` → `numeroEnregistrement`, puis gRPC `AjouterPoche` :
   **une poche O− de 450 ml est créée dans BANQUE-A** (collecte aujourd'hui, péremption à J+42).
   Le don est **`ACCEPTEE`, sans motif**. (Si Inventaire était arrêté : `ACCEPTEE` avec le motif
   « Inventaire indisponible : poche à saisir par la banque ».)

⚠️ Conséquences sur les résultats attendus :
- **un donneur inéligible donne 201 `REFUSEE`, pas 409** (c'est aussi ce que vérifie `DonApiIT` de Dons) ;
- **409** n'arrive que si `EnregistrerDon` renvoie un Fault après un `VerifierEligibilite` positif
  (deux dons simultanés) : pas reproductible à la main ; ce Fault est couvert par
  `RegistreEndpointIntegrationTest.enregistrerDon_contreIndication_faultDonneurInelegible` ;
- **404** demande un donneur inscrit dans Dons mais inconnu du Registre. À l'inscription, Dons appelle
  `VerifierExistence` et refuse un identifiant inconnu (400)… sauf si le Registre est arrêté
  (repli : accepté). Le cas 404 se prépare donc pendant le cas « Registre arrêté ».

---

## 1. Démarrage en local

| Service | Port | Remarque |
|---|---|---|
| dons_db / inventaire_db / registre_db | 5436 / 5435 / 5434 | bases du `docker-compose.yml` racine |
| config-server | 8888 | fournit `registre-service.yml`, `inventaire-service.yml`, `dons.yml` |
| discovery (Eureka) | 8761 | facultatif |
| registre-service | 8083 | SOAP, WSDL sur `/ws/registre.wsdl` |
| inventaire-service | gRPC **9090**, HTTP **8082** | 8080 est pris par Oracle XE sur ce poste → `SERVER_PORT=8082` |
| dons | 8081 | REST |

### 1.1 Une seule fois : libérer les ports 5434 et 5435
Anciens conteneurs créés avant le `docker-compose.yml` unifié :
```powershell
docker rm -f dons-db registre_pg_sql
```
(`registre_pg_sql` ne contient que les données de test : `data.sql` les recharge au démarrage du Registre.)

### 1.2 Construire (VS Code fermé, ou sa compilation automatique coupée)
```powershell
mvn -f services/pom.xml clean package -DskipTests
```

### 1.3 Démarrer les 3 bases
```powershell
docker compose up -d dons_db inventaire_db registre_db
```

### 1.4 Lancer les services — un terminal PowerShell par service, dans cet ordre
Attendre `Started …` dans chaque terminal avant le suivant.

**Terminal 1 — Config Server**
```powershell
java -jar services/config-server/target/config-server-0.0.1-SNAPSHOT.jar
```
**Terminal 2 — Discovery**
```powershell
java -jar services/discovery/target/discovery-0.0.1-SNAPSHOT.jar
```
**Terminal 3 — Registre**
```powershell
java -jar services/registre-service/target/registre-service-1.0.0.jar
```
**Terminal 4 — Inventaire**
```powershell
$env:SERVER_PORT = "8082"
java -jar services/inventaire-service/target/inventaire-service-1.0.0.jar
```
**Terminal 5 — Dons**
```powershell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
$env:JWT_SECRET = [Convert]::ToBase64String($b)
$env:DB_URL = "jdbc:postgresql://localhost:5436/dons_db"
$env:DB_USER = "dons_user"
$env:DB_PASSWORD = "dons_password"
$env:COMPTES_TEST_MDP = "Demo2026!"
$env:REGISTRE_URL = "http://localhost:8083/ws"
$env:INVENTAIRE_GRPC_ADDRESS = "static://localhost:9090"
java -jar services/dons/target/dons-1.0.0.jar
```
`REGISTRE_URL` et `INVENTAIRE_GRPC_ADDRESS` sont **obligatoires** en local : sinon le `dons.yml` du
Config Server utilise les noms Docker (`registre-service`, `inventaire-service`) et Dons ne trouve aucun des deux.

### 1.5 Contrôles avant de commencer (nouveau terminal)
```powershell
curl.exe -s -o NUL -w "config-server %{http_code}`n" http://localhost:8888/registre-service/default
curl.exe -s -o NUL -w "registre WSDL %{http_code}`n" http://localhost:8083/ws/registre.wsdl
curl.exe -s -o NUL -w "inventaire    %{http_code}`n" http://localhost:8082/actuator/health
curl.exe -s http://localhost:8081/actuator/health
```
**Attendu** : trois `200`, puis `{"status":"UP",...}`.

Faire une requête SOAP « à blanc » (Postman, `VerifierExistence` de `POSTMAN_TESTS.md`) :
le premier appel après démarrage du Registre peut être lent, et Dons coupe à **2 s**.

### 1.6 Fonctions utilisées ensuite (à coller dans le même terminal)
```powershell
$DONS = "http://localhost:8081/api/v1"
function Json($objet) { ($objet | ConvertTo-Json -Compress).Replace('"', '\"') }
function Connexion($email) {
  (curl.exe -s -X POST "$DONS/auth/login" -H "Content-Type: application/json" `
    -d (Json @{ email = $email; motDePasse = "Demo2026!" }) | ConvertFrom-Json).accessToken }
function Inscrire($nom, $email, $id, $groupe) {
  curl.exe -s -w "`nHTTP %{http_code}`n" -X POST "$DONS/donneurs" -H "Content-Type: application/json" `
    -d (Json @{ nom = $nom; email = $email; motDePasse = "Demo2026!"; identifiantNational = $id;
                dateNaissance = "1995-01-15"; poids = 72; groupeSanguin = $groupe }) }
function Proposer($jeton) {
  curl.exe -s -w "`nHTTP %{http_code}`n" -X POST "$DONS/dons" -H "Authorization: Bearer $jeton" `
    -H "Content-Type: application/json" -d (Json @{ codeBanque = "BANQUE-A" }) }
function StockONegBanqueA($jeton) {
  curl.exe -s "$DONS/stocks?groupeSanguin=O_NEGATIF&codeBanque=BANQUE-A" -H "Authorization: Bearer $jeton" }
```

---

## 2. Cas de test (dans cet ordre)

### Cas 1 — TN-00000001 éligible → 201 `ACCEPTEE` + poche créée dans Inventaire
Stock O− de BANQUE-A **avant** le don (compte de la banque A) :
```powershell
$TB = Connexion "banque-a@test.tn"; StockONegBanqueA $TB
```
**Attendu** : `{"groupeSanguin":"O_NEGATIF","codeBanque":"BANQUE-A","disponibles":3}` (POCHE-A001 à A003 de `data.sql`).

Le don :
```powershell
$T1 = Connexion "donneur@test.tn"; Proposer $T1
```
**Attendu : HTTP 201**, header `Location: /api/v1/dons/{id}`
```json
{"statut":"ACCEPTEE","groupeSanguin":"O_NEGATIF","codeBanque":"BANQUE-A",
 "motif":null,"numeroEnregistrement":"REG-TN-00000001-yyyyMMdd", ...}
```
Stock **après** : `StockONegBanqueA $TB` → **`"disponibles":4`** (la poche du don a été ajoutée par gRPC).
Côté Registre : `date_dernier_don` de TN-00000001 = aujourd'hui.
Bonus : refaire `Proposer $T1` → **201 `REFUSEE`**, motif « délai insuffisant depuis le dernier don : 0 jours (minimum 56) ».
(TN-00000001 redevient éligible au prochain redémarrage du Registre.)

### Cas 2 — TN-00000011 inéligible (don il y a 20 jours) → 201 `REFUSEE` (pas 409)
```powershell
Inscrire "Walid Saidi" "walid@test.tn" "TN-00000011" "O_POSITIF"    # HTTP 201 (VerifierExistence = true)
$T11 = Connexion "walid@test.tn"; Proposer $T11
```
**Attendu : HTTP 201**
```json
{"statut":"REFUSEE","motif":"délai insuffisant depuis le dernier don : 20 jours (minimum 56)",
 "numeroEnregistrement":null, ...}
```
Aucune poche n'est créée (stock O− de BANQUE-A inchangé).
Contrôle à l'inscription : `Inscrire "X" "x@test.tn" "TN-99999999" "O_NEGATIF"` (Registre démarré) →
**HTTP 400** « Identifiant inconnu du registre national ».

### Cas 3 — Registre arrêté → 202 `EN_ATTENTE`
Préparer un donneur pendant que le Registre tourne encore :
```powershell
Inscrire "Mariem Trabelsi" "mariem@test.tn" "TN-00000002" "O_POSITIF"   # HTTP 201
$T2 = Connexion "mariem@test.tn"
```
**Arrêter le Registre** (Ctrl+C dans le terminal 3), puis :
```powershell
Proposer $T2
```
**Attendu : HTTP 202**
```json
{"statut":"EN_ATTENTE","motif":"Registre indisponible : vérification d'éligibilité différée", ...}
```
Pendant l'arrêt, préparer le cas 4 (l'inscription passe grâce au repli de `VerifierExistence`) :
```powershell
Inscrire "Inconnu Test" "inconnu@test.tn" "TN-99999999" "O_NEGATIF"      # HTTP 201 (repli)
```
Option : `curl.exe -s http://localhost:8081/actuator/circuitbreakers` → après ≥ 5 appels dont ≥ 50 % en échec,
le circuit `registre` passe `OPEN` (les dons restent `EN_ATTENTE`, réponse immédiate).

### Cas 4 — TN-99999999 inconnu du Registre → 404
**Relancer le Registre** (terminal 3). Si le circuit est `OPEN`, attendre **15 s** (`wait-duration-in-open-state`).
```powershell
$T99 = Connexion "inconnu@test.tn"; Proposer $T99
```
**Attendu : HTTP 404** (ProblemDetail)
```json
{"title":"Ressource introuvable","status":404,
 "detail":"Donneur inconnu du registre national : TN-99999999", ...}
```
Aucun don n'est enregistré (transaction annulée) : `curl.exe -s "$DONS/dons" -H "Authorization: Bearer $T99"` → `[]`.

---

## 3. Récapitulatif

| Cas | Donneur | Registre | HTTP | Résultat |
|---|---|---|---|---|
| 1 | TN-00000001 | démarré | **201** | `ACCEPTEE` + numéro `REG-TN-00000001-yyyyMMdd` + **1 poche O− ajoutée dans BANQUE-A (3 → 4)** |
| 2 | TN-00000011 | démarré | **201** | `REFUSEE` + motif (pas 409), aucune poche |
| 3 | TN-00000002 | **arrêté** | **202** | `EN_ATTENTE` |
| 4 | TN-99999999 | démarré | **404** | rien d'enregistré |

---

## 4. Compatibilité client Dons ↔ serveur Registre

| Point | Client Dons (`RegistreClient`, `RegistreClientConfig`) | Serveur `registre-service` | |
|---|---|---|---|
| URL | `registre.url` = `REGISTRE_URL` (`http://localhost:8083/ws` en local) | servlet `/ws/*`, port 8083 | ✅ |
| Namespace | `http://donsang.com/registre/v1` | même XSD : module partagé `contrats/registre-soap` | ✅ |
| Fault inconnu | code `Client` + texte qui commence par `Donneur inconnu` → 404 | `Client`, « Donneur inconnu du registre national : … » | ✅ (vérifié par test d'intégration) |
| Fault inéligible | autre Fault `Client` → 409 | `Client`, « Donneur inéligible : … — motif » | ✅ |
| Inéligible dans VerifierEligibilite | `eligible=false` + `motif` | idem, sans Fault | ✅ |
| `groupe` | `groupe.name()` → `O_NEGATIF`… | enum `O_NEGATIF`… | ✅ |
| `dateDon` | `xs:date` du jour | utilisé comme nouvelle `date_dernier_don` | ✅ |
| `numeroEnregistrement` | stocké et renvoyé tel quel | `REG-{id}-{yyyyMMdd}` | ✅ |
| Timeout | 2 s connexion / lecture | premier appel après démarrage parfois plus lent | ⚠️ faire un appel à blanc |
| En local | `dons.yml` du Config Server : défaut = noms Docker | — | ⚠️ définir `REGISTRE_URL` et `INVENTAIRE_GRPC_ADDRESS` (§ 1.4) |
| En Docker | `REGISTRE_URL=http://registre-service:8083/ws` dans `docker-compose.yml` | service `registre-service` du Compose | ✅ |
