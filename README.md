# Projet_SOA
# Héma — Frontend de coordination du don de sang

Application React, TypeScript et Vite pour les donneurs, les hôpitaux et les banques de sang. Les appels REST sont centralisés dans `src/services/api.ts` et préfixés par `/api/v1`.

## Prérequis

- Node.js 20.19+ ou 22.12+
- pnpm
- API Spring Boot démarrée sur le port `8081`

## Développement

```bash
pnpm install
pnpm dev
```

Le serveur de développement écoute sur `http://localhost:5173`. En l’absence de `VITE_API_URL`, Vite transmet `/api` au backend `http://localhost:8081` afin d’éviter les problèmes CORS.

## Variables d’environnement

Créer `.env.local` à partir des paramètres de votre environnement, sans y inscrire de secrets partagés :

```env
VITE_API_URL=http://localhost:8081
```

Le client ajoute le préfixe `/api/v1` automatiquement. Le mot de passe des comptes de démonstration n’est jamais intégré à l’application.

## Comptes de démonstration

Les comptes `donneur@test.tn`, `hopital@test.tn`, `banque-a@test.tn` et `banque-b@test.tn` sont créés/configurés côté backend à l’aide de `COMPTES_TEST_MDP`. Utiliser le mot de passe fourni par l’environnement d’exécution.

## Construction

```bash
pnpm build
```

## Fonctionnalités

- Connexion JWT et inscription donneur, expiration de session, redirection par rôle et contrôle d’accès.
- Espaces donneur, hôpital et banque avec formulaires validés, filtres, export CSV, stocks et historique.
- États de chargement, erreurs REST humanisées, notifications et confirmations d’annulation.