# Tests SOAP — Registre Service

**URL :** `http://localhost:8083/ws`  
**WSDL :** `http://localhost:8083/ws/registre.wsdl`  
**Header :** `Content-Type: text/xml`

---

## T04 — VerifierExistence

### Cas : DON-001 (existe → true)
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://soa.com/registre">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:VerifierExistenceRequest>
      <reg:donneurId>DON-001</reg:donneurId>
    </reg:VerifierExistenceRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** `<existe>true</existe>`

### Cas : DON-999 (n'existe pas → false)
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://soa.com/registre">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:VerifierExistenceRequest>
      <reg:donneurId>DON-999</reg:donneurId>
    </reg:VerifierExistenceRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** `<existe>false</existe>`

---

## T08 — VerifierEligibilite

### Cas : DON-001 (éligible → true)
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://soa.com/registre">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:VerifierEligibiliteRequest>
      <reg:donneurId>DON-001</reg:donneurId>
    </reg:VerifierEligibiliteRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** `<eligible>true</eligible>`

### Cas : DON-999 → SOAP Fault DonneurInconnu (code CLIENT)
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://soa.com/registre">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:VerifierEligibiliteRequest>
      <reg:donneurId>DON-999</reg:donneurId>
    </reg:VerifierEligibiliteRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** SOAP Fault avec `faultcode=CLIENT`, `faultstring` commence par `"Donneur inconnu"`

### Cas : DON-011 (don récent → inéligible) → SOAP Fault DonneurIneligible
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://soa.com/registre">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:VerifierEligibiliteRequest>
      <reg:donneurId>DON-011</reg:donneurId>
    </reg:VerifierEligibiliteRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** SOAP Fault avec `faultcode=CLIENT`, `faultstring` commence par `"Donneur inéligible"`

---

## T08 — EnregistrerDon

### Cas : DON-001 (éligible → succes)
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://soa.com/registre">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:EnregistrerDonRequest>
      <reg:donneurId>DON-001</reg:donneurId>
    </reg:EnregistrerDonRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** `<succes>true</succes>`

---

## T15 — ListerDonneursEligibles

### Cas : groupe O_NEG
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:reg="http://soa.com/registre">
  <soapenv:Header/>
  <soapenv:Body>
    <reg:ListerDonneursEligiblesRequest>
      <reg:groupeSanguin>O_NEG</reg:groupeSanguin>
    </reg:ListerDonneursEligiblesRequest>
  </soapenv:Body>
</soapenv:Envelope>
```
**Réponse attendue :** liste contenant DON-001, ne contenant pas DON-011

---

## Comment le client Dons distingue les deux Faults

| Fault reçu | faultstring commence par | HTTP renvoyé par Dons |
|---|---|---|
| DonneurInconnu | `"Donneur inconnu"` | **404** |
| DonneurIneligible | `"Donneur inéligible"` | **409** |
