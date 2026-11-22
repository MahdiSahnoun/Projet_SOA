# Tests SOAP — Registre Service

**URL :** `POST http://localhost:8083/ws` (même URL que `registre.url` côté Dons)
**WSDL :** `http://localhost:8083/ws/registre.wsdl`
**Header :** `Content-Type: text/xml`
**Namespace :** `http://donsang.com/registre/v1` (contrat `registre.xsd` partagé avec le service Dons)

Données de démo (`data.sql`) :

| Identifiant | Groupe | Cas |
|---|---|---|
| TN-00000001 | O_NEGATIF | Éligible (compte de démo `donneur@test.tn` de Dons, redevient éligible à chaque démarrage) |
| TN-00000002 … TN-00000010 | tous les groupes | Éligibles |
| TN-00000011 | O_POSITIF | Inéligible : don il y a 20 jours |
| TN-00000012 | A_POSITIF | Inéligible : 17 ans |
| TN-00000013 | B_POSITIF | Inéligible : 68 ans |
| TN-00000014 | O_NEGATIF | Inéligible : contre-indication |
| TN-00000015 | AB_POSITIF | Inéligible : 45 kg |
| TN-99999999 | — | N'existe pas |

---

## T04 — VerifierExistence (jamais de Fault)

### Cas : TN-00000001 existe → true
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://donsang.com/registre/v1">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:VerifierExistenceRequest>
      <reg:identifiantNational>TN-00000001</reg:identifiantNational>
    </reg:VerifierExistenceRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** `<ns2:existe>true</ns2:existe>`

### Cas : TN-99999999 n'existe pas → false (pas de Fault)
Même requête avec `TN-99999999`.
**Réponse attendue :** `<ns2:existe>false</ns2:existe>`

---

## T08 — VerifierEligibilite

### Cas : TN-00000001 éligible
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://donsang.com/registre/v1">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:VerifierEligibiliteRequest>
      <reg:identifiantNational>TN-00000001</reg:identifiantNational>
    </reg:VerifierEligibiliteRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** `<eligible>true</eligible>`, sans `motif` ni `prochainDonPossible`.

### Cas : TN-00000011 inéligible (don récent) → réponse normale, PAS de Fault
Même requête avec `TN-00000011`.
**Réponse attendue :**
```xml
<ns2:eligible>false</ns2:eligible>
<ns2:motif>délai insuffisant depuis le dernier don : 20 jours (minimum 56)</ns2:motif>
<ns2:prochainDonPossible>(aujourd'hui + 36 jours)</ns2:prochainDonPossible>
```
Pour TN-00000012 à TN-00000015 : `eligible=false` + `motif` (âge, poids, contre-indication), sans `prochainDonPossible`.

### Cas : TN-99999999 → SOAP Fault DonneurInconnu
Même requête avec `TN-99999999`.
**Réponse attendue :** `faultcode = SOAP-ENV:Client`, `faultstring = "Donneur inconnu du registre national : TN-99999999"`

---

## T08 — EnregistrerDon

### Cas : TN-00000001 éligible → numéro d'enregistrement
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://donsang.com/registre/v1">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:EnregistrerDonRequest>
      <reg:identifiantNational>TN-00000001</reg:identifiantNational>
      <reg:dateDon>2026-11-22</reg:dateDon>
      <reg:groupe>O_NEGATIF</reg:groupe>
    </reg:EnregistrerDonRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** `<ns2:numeroEnregistrement>REG-TN-00000001-20261122</ns2:numeroEnregistrement>`
Ensuite `date_dernier_don = 2026-11-22` : un 2ᵉ appel renvoie le Fault DonneurInelegible
(TN-00000001 redevient éligible au prochain redémarrage du service).

### Cas : TN-00000014 inéligible → SOAP Fault DonneurInelegible
Même requête avec `TN-00000014` (groupe `O_NEGATIF`).
**Réponse attendue :** `faultcode = SOAP-ENV:Client`,
`faultstring = "Donneur inéligible : TN-00000014 — contre-indication médicale enregistrée"`

### Cas : TN-99999999 → SOAP Fault DonneurInconnu
**Réponse attendue :** `faultcode = SOAP-ENV:Client`, `faultstring` commence par `"Donneur inconnu"`

### Cas : groupe invalide → SOAP Fault Client
Même requête avec `<reg:groupe>O_NEG</reg:groupe>`.
**Réponse attendue :** `faultcode = SOAP-ENV:Client`,
`faultstring = "Groupe sanguin invalide : 'O_NEG'. Valeurs attendues : [O_NEGATIF, O_POSITIF, ...]"`

---

## T15 — ListerDonneursEligibles

### Cas : groupe O_NEGATIF
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://donsang.com/registre/v1">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:ListerDonneursEligiblesRequest>
      <reg:groupe>O_NEGATIF</reg:groupe>
    </reg:ListerDonneursEligiblesRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** `identifiantNational` = TN-00000001 et TN-00000009 ; **pas** TN-00000014 (contre-indication).

## T15 / T16 — ListerDonneursEligiblesDetail (même filtre, avec coordonnées)

### Cas : groupe O_NEGATIF
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://donsang.com/registre/v1">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:ListerDonneursEligiblesDetailRequest>
      <reg:groupe>O_NEGATIF</reg:groupe>
    </reg:ListerDonneursEligiblesDetailRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :**
```xml
<ns2:donneur>
  <ns2:identifiantNational>TN-00000001</ns2:identifiantNational>
  <ns2:nom>Ben Salah</ns2:nom>
  <ns2:prenom>Ahmed</ns2:prenom>
  <ns2:telephone>+216 20 123 401</ns2:telephone>
</ns2:donneur>
<ns2:donneur>
  <ns2:identifiantNational>TN-00000009</ns2:identifiantNational>
  ...
</ns2:donneur>
```
Groupe invalide (ex. `O_NEG`) → même SOAP Fault Client « Groupe sanguin invalide » que les autres opérations.

---

## Comment le client Dons interprète les réponses

| Réponse du Registre | Condition reconnue par Dons | Résultat côté Dons |
|---|---|---|
| `VerifierEligibilite` → `eligible=false` + `motif` | réponse normale | don `REFUSEE` (HTTP 201) avec le motif |
| Fault `Client`, faultstring commence par `"Donneur inconnu"` | `startsWith("Donneur inconnu")` | **404** |
| Fault `Client`, autre message (inéligible, groupe invalide) | tout autre Fault Client | **409** avec le faultstring comme détail |
| Fault `Server`, timeout, service arrêté | panne | circuit breaker → don `EN_ATTENTE` (HTTP 202) |
