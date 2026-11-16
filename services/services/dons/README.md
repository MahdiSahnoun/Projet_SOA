# Service Dons (REST · port 8081)

Point d'entrée REST de la plateforme. Il gère les comptes (donneurs, hôpitaux, banques),
les dons et les demandes, émet le JWT, et orchestre deux appels inter-services :

- **REST → gRPC** vers Inventaire : réservation, libération, stock, ajout de poche ;
- **REST → SOAP** vers Registre (`WebServiceTemplate`) : existence, éligibilité, enregistrement du don.

## Endpoints (`/api/v1`)

| Méthode | URI | Rôle | Réponses |
|---|---|---|---|
| POST | `/auth/login` | public | 200 jeton · 400 · 401 |
| POST | `/donneurs` | public (inscription) | 201 + Location · 400 · 409 |
| GET | `/donneurs/{id}` | DONNEUR (soi-même) | 200 · 404 |
| POST | `/dons` | DONNEUR | 201 (accepté/refusé) · 202 (en attente) · 404 |
| GET | `/dons`, `/dons/{id}` | DONNEUR | 200 · 404 |
| POST | `/demandes` | HOPITAL | 201 + Location (ACCEPTEE ou EN_ATTENTE) · 400 |
| GET | `/demandes?page=0&size=20` | HOPITAL | 200 (paginé) |
| GET | `/demandes/{id}` | HOPITAL | 200 · 404 |
| PATCH | `/demandes/{id}/annulation` | HOPITAL | 200 · 404 · 409 |
| GET | `/stocks?groupeSanguin=O_NEGATIF[&codeBanque=BANQUE-A]` | HOPITAL, BANQUE | 200 · 503 |
| POST | `/poches` | BANQUE | 201 · 400 · 404 |

Sans jeton : **401**. Jeton valide mais mauvais rôle : **403**. Erreurs au format ProblemDetail (RFC 7807).
Service distant en panne : **503**, ou statut `EN_ATTENTE` quand une méthode de repli existe.

Documentation : `/v3/api-docs` (OpenAPI) et `/swagger-ui.html` (bouton « Authorize » pour coller le jeton).

## Lancer en local

```bash
# à la racine du dépôt
mvn -pl services/dons -am verify            # compile les contrats, lance tests unitaires (*Test) et d'intégration (*IT)

export JWT_SECRET="$(openssl rand -base64 48)"
export DB_PASSWORD=...  COMPTES_TEST_MDP=...
java -jar services/dons/target/dons-1.0.0.jar
```

Variables : `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (obligatoire, ≥ 32 caractères),
`INVENTAIRE_GRPC_ADDRESS` (défaut `static://localhost:9090`), `REGISTRE_URL` (défaut `http://localhost:8083/ws`),
`COMPTES_TEST_MDP` (crée donneur@test.tn, hopital@test.tn, banque-a@test.tn, banque-b@test.tn).

## Docker

```bash
docker compose up --build   # depuis services/, démarre PostgreSQL et Dons
```

Le fichier `.env` doit être présent dans `services/` et contenir au minimum
`DB_PASSWORD` et `JWT_SECRET`. Le Compose remplace automatiquement `DB_URL`
par l'adresse interne `postgres:5432` pour le conteneur Dons.

## Limites connues

- Pas de rejeu automatique d'un don resté `EN_ATTENTE` quand le Registre était en panne.
- Si Inventaire tombe après l'enregistrement officiel d'un don, le don est `ACCEPTEE`
  avec un motif demandant à la banque de saisir la poche (pas de transaction distribuée).
- `traiterDemande` garde la transaction SQL ouverte pendant l'appel gRPC (borné par la deadline).
