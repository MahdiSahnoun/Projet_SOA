# Test bout en bout — Inventaire + Dons

## Prérequis

- Docker Desktop actif.
- Java 21.
- Maven.
- `curl`.
- `grpcurl`.
- Les bases locales ne doivent pas partager le même port hôte.

## 1. Démarrer les bases

Depuis la racine du dépôt :

```powershell
docker run -d --name inventaire-db -e POSTGRES_DB=inventaire_db `
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres `
  -p 5435:5432 postgres:16-alpine

docker run -d --name dons-db -e POSTGRES_DB=dons_db `
  -e POSTGRES_USER=dons_user -e POSTGRES_PASSWORD=dons `
  -p 5436:5432 postgres:16-alpine
```

Le port de `inventaire_db` est donc `5435` localement.
Le port de `dons_db` est `5436` pour éviter une collision avec PostgreSQL local.

## 2. Compiler les modules

```powershell
mvn -f services/pom.xml -pl inventaire-service -am package -DskipTests
mvn -f services/pom.xml -pl dons -am package -DskipTests
```

## 3. Démarrer Inventaire

Dans un premier terminal :

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5435/inventaire_db"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
$env:SPRING_CONFIG_IMPORT="optional:configserver:http://localhost:8888"
$env:EUREKA_CLIENT_ENABLED="false"

java -jar services/inventaire-service/target/inventaire-service-1.0.0.jar
```

Inventaire écoute sur `localhost:9090`.

## 4. Démarrer Dons

Dans un second terminal :

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5436/dons_db"
$env:DB_USER="dons_user"
$env:DB_PASSWORD="dons"
$env:JWT_SECRET="change-me-with-a-secret-of-at-least-32-characters"
$env:COMPTES_TEST_MDP="Demo2026!"
$env:INVENTAIRE_GRPC_ADDRESS="static://localhost:9090"
$env:INVENTAIRE_DEADLINE_MS="2000"
$env:REGISTRE_URL="http://localhost:8083/ws"

java -jar services/dons/target/dons-1.0.0.jar
```

Dons écoute sur `localhost:8081`.

Le compte hôpital de démonstration est créé comme :

- email : `hopital@test.tn`
- mot de passe : `Demo2026!`

## 5. Login hôpital

```powershell
$LOGIN=$(curl.exe -s -X POST http://localhost:8081/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"hopital@test.tn\",\"motDePasse\":\"Demo2026!\"}')

$TOKEN=($LOGIN | Select-String -Pattern '"accessToken":"([^"]+)' -AllMatches).Matches[0].Groups[1].Value
```

`TOKEN` est le JWT à fournir dans `Authorization: Bearer $TOKEN`.

## 6. Préparer les poches

Ajouter une poche à chaque banque dans la quantité nécessaire.

```powershell
grpcurl -plaintext -d '{
  "banque_id":"BANQUE-A",
  "groupe":"O_NEGATIF",
  "volume_ml":450,
  "date_collecte":"2026-11-21",
  "date_peremption":"2027-01-15"
}' localhost:9090 inventaire.v1.InventaireService/AjouterPoche

grpcurl -plaintext -d '{
  "banque_id":"BANQUE-B",
  "groupe":"O_NEGATIF",
  "volume_ml":450,
  "date_collecte":"2026-11-21",
  "date_peremption":"2027-01-16"
}' localhost:9090 inventaire.v1.InventaireService/AjouterPoche

grpcurl -plaintext -d '{
  "banque_id":"BANQUE-B",
  "groupe":"O_NEGATIF",
  "volume_ml":450,
  "date_collecte":"2026-11-21",
  "date_peremption":"2027-01-17"
}' localhost:9090 inventaire.v1.InventaireService/AjouterPoche
```

Attendu : une `poche_id` commençant par `POCHE-` pour chaque réponse.

## 7. Demande servie par deux banques

```powershell
curl.exe -i -X POST http://localhost:8081/api/v1/demandes `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d '{\"groupeSanguin\":\"O_NEGATIF\",\"quantite\":3,\"urgence\":\"NORMALE\"}'
```

Attendu :

- HTTP `201 Created`
- Header `Location: /api/v1/demandes/{id}`
- JSON :

```json
{
  "statut": "ACCEPTEE",
  "groupeSanguin": "O_NEGATIF",
  "quantite": 3,
  "allocations": [
    {
      "codeBanque": "BANQUE-A",
      "groupeSanguin": "O_NEGATIF",
      "quantite": 1
    },
    {
      "codeBanque": "BANQUE-B",
      "groupeSanguin": "O_NEGATIF",
      "quantite": 2
    }
  ]
}
```

La répartition peut dépendre des poches déjà présentes dans la base.
Les poches périmées ou déjà réservées ne sont pas prises.

## 8. Demande impossible

Après avoir épuisé ou limité le stock `O_NEGATIF`, demander plus que la quantité disponible.

```powershell
curl.exe -i -X POST http://localhost:8081/api/v1/demandes `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d '{\"groupeSanguin\":\"O_NEGATIF\",\"quantite\":20,\"urgence\":\"VITALE\"}'
```

Attendu :

- HTTP `201 Created`
- statut `EN_ATTENTE`
- liste `allocations` vide

Inventaire répond normalement `RESOURCE_EXHAUSTED`.
Dons traduit ce code en `StockInsuffisantException` et ne déclenche pas l’exception HTTP 503.

## 9. Annulation d'une demande acceptée

Remplacer `{id}` par l'identifiant retourné lors du cas réussi.

```powershell
curl.exe -i -X PATCH http://localhost:8081/api/v1/demandes/1/annulation `
  -H "Authorization: Bearer $TOKEN"
```

Attendu :

- HTTP `200 OK`
- statut `ANNULEE`
- Dons appelle `LibererReservation("DEM-<id>")`
- les poches réservées repassent à `DISPONIBLE`

Une deuxième annulation retourne HTTP `409 Conflict`.

## 10. Consulter le stock

Toutes les banques :

```powershell
curl.exe -i "http://localhost:8081/api/v1/stocks?groupeSanguin=O_NEGATIF" `
  -H "Authorization: Bearer $TOKEN"
```

Attendu :

```json
{
  "groupeSanguin": "O_NEGATIF",
  "codeBanque": null,
  "disponibles": 3
}
```

Après l’annulation, la quantité doit correspondre aux poches disponibles
dans toutes les banques, hors poches périmées.

Une banque précise :

```powershell
curl.exe -i "http://localhost:8081/api/v1/stocks?groupeSanguin=O_NEGATIF&codeBanque=BANQUE-A" `
  -H "Authorization: Bearer $TOKEN"
```

Attendu : une réponse avec `codeBanque: "BANQUE-A"` et le nombre disponible dans cette banque.

## Arrêt

```powershell
docker rm -f inventaire-db dons-db
```

## Différences ou incompatibilités identifiées

- Dons attend sur Inventaire `static://localhost:9090`; le serveur doit donc être joint en plaintext sur le port `9090`.
- Les quatre appels utilisés par Dons sont compatibles :
  `GetStock`, `ReserverPoches`, `LibererReservation` et `AjouterPoche`.
- `ReserverPoches` reçoit bien une liste de groupes compatibles, avec le groupe exact en premier.
- La référence envoyée par Dons est `DEM-<id>`.
- Dons interprète `RESOURCE_EXHAUSTED` comme `EN_ATTENTE`, pas comme une erreur HTTP.
- Dons attend une réponse `ReservationReponse.allocations` avec `banque_id`, `groupe` et `quantite`.
- Dons convertit cette réponse en allocations locales `codeBanque`, `groupeSanguin`, `quantite`.
- Dons utilise la deadline `2000 ms`.
- Dons ne possède pas encore de client pour `StreamStocks`; ce service reste disponible pour les simulateurs ou clients gRPC.
- Dons appelle `LibererReservation` seulement si la demande est déjà `ACCEPTEE`.
- `GetStock` accepte `banque_id` vide pour toutes les banques. Dons l’envoie vide dans ce cas.
