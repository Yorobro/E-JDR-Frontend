# E-JDR Frontend — Compose Desktop, Clean Architecture (Design)

**Date:** 2026-06-06
**Statut:** Validé (en attente de relecture finale)

## Contexte

Le dépôt `E-JDR-Frontend` est un projet greenfield (git initialisé, aucun commit, aucun fichier source). On veut bâtir une application **desktop uniquement** en **Kotlin + Compose for Desktop (JVM pur)**, structurée selon une **clean architecture** stricte, modulaire et réutilisable, respectant SOLID et le clean code.

L'app consomme un backend Node.js/Express (lui-même en clean architecture) dont **seul le module d'authentification** est exposé, sous le préfixe `/auth`. L'authentification est gérée **par cookies** (`access_token` / `refresh_token`) posés par le serveur — pas de header `Authorization`.

Objectif de ce premier travail : **mettre en place tout le squelette d'architecture** + une **feature Auth (login/register/auto-login/logout) verticale complète** servant de référence pour les futures features (ex. Host). Du WebSocket viendra plus tard — on code comme s'il n'existait pas pour l'instant.

Exigence utilisateur clé : au premier lancement, formulaire de connexion ; une fois connecté, **rester connecté même après redémarrage du PC**, tout en restant **le plus sécurisé possible**.

## Stack technique

- **Langage / UI :** Kotlin, Compose for Desktop (JVM pur, `org.jetbrains.compose`), build Gradle Kotlin DSL.
- **DI :** Koin (modules déclaratifs, `koinInject()` dans les composables).
- **HTTP :** Ktor Client + plugins `HttpCookies`, `ContentNegotiation` (JSON kotlinx.serialization), `Logging`.
- **Sécurité token :** Java KeyStore (JCEKS) + AES-GCM pour chiffrer le `refresh_token` persisté.
- **Tests :** JUnit5 + MockK + kotlin.test (unitaires : domaine, use cases, services, mapping).
- **Structure :** single-module, packages par couche. Package racine : `eu.ejdr`.

## Architecture & structure des packages

```
src/main/kotlin/eu/ejdr/
├── domain/
│   ├── error/
│   │   ├── DomainError.kt              # sealed base de toutes les erreurs domaine
│   │   └── entities/
│   │       └── auth/
│   │           └── AuthError.kt        # erreurs domaine auth (sealed : InvalidCredentials, EmailAlreadyUsed, SessionExpired, NetworkError, Unknown…)
│   └── entities/
│       └── auth/                       # entités agnostiques — data classes PURES, aucune méthode
│           ├── User.kt
│           └── Credentials.kt
├── application/
│   └── auth/                           # par grosse feature
│       ├── abstraction/
│       │   ├── repository/             # interfaces repository (ex. AuthRepository)
│       │   ├── service/                # interfaces service
│       │   └── usecase/                # interfaces use case
│       ├── service/                    # impl services
│       └── usecase/                    # impl use cases
├── infrastructure/
│   ├── http/
│   │   ├── KtorClientFactory.kt        # construction du HttpClient + plugins
│   │   └── auth/
│   │       ├── dto/                    # DTO requête/réponse (LoginRequest, AuthResponse…)
│   │       ├── AuthHttpMapper.kt       # {code,message}+HTTP → AuthError ; DTO → entité domaine
│   │       └── AuthHttpRepository.kt   # impl HTTP de AuthRepository
│   ├── security/
│   │   ├── KeyStoreProvider.kt         # KeyStore JCEKS + clé AES
│   │   └── SecureCookiesStorage.kt     # CookiesStorage Ktor : mémoire + refresh_token chiffré sur disque
│   └── config/
│       └── AppConfig.kt                # base URL, env, chemins (lus depuis env/fichier local non commité)
└── presentation/
    ├── shared/
    │   └── component/
    │       ├── atomic/                 # AppButton, AppTextField…
    │       ├── molecule/               # LabeledTextField, FormError…
    │       └── organism/               # organismes réutilisables
    ├── feature/
    │   └── auth/
    │       ├── page/                   # LoginPage, RegisterPage (intelligentes : appellent use cases, tiennent l'état)
    │       └── component/              # LoginForm, RegisterForm (bêtes : props + callbacks)
    ├── navigation/
    │   └── Screen.kt                   # sealed Screen pour navigation par état
    └── di/                             # (ou racine /di) modules Koin
src/main/kotlin/eu/ejdr/main.kt          # point d'entrée application{} : démarre Koin, RestoreSession, affiche Auth ou App
```

### Règles d'architecture (contraintes encodées)

- **Use case** = orchestration uniquement. Peut utiliser repositories + services. **NE PEUT PAS** appeler un autre use case.
- **Service** = logique réutilisable. Peut utiliser repositories **et** d'autres services. Partagé entre use cases.
- **Entités domaine** = purs conteneurs de données (data classes), **aucune méthode** métier.
- **Repository (application/abstraction/repository)** = interfaces seulement ; implémentations en `infrastructure`.
- **Présentation** : seules les **pages** appellent les use cases ; les **composants** sont bêtes (valeurs + callbacks, aucun appel, aucune logique métier).
- **Sens des dépendances** : `presentation → application → domain` ; `infrastructure → application/domain` (implémente les interfaces). `domain` ne dépend de rien.

### Type de retour des use cases

Sealed maison `Result<out T, out E : DomainError>` avec `Success(value)` / `Failure(error)`. Railway-oriented, `when` exhaustif, aucune exception ne remonte jusqu'à la présentation, aucune dépendance externe.

## Flux d'authentification & gestion des cookies

Le serveur pose les cookies ; le code applicatif ne manipule jamais le token directement — il s'appuie sur `SecureCookiesStorage`.

- **`SecureCookiesStorage`** (impl de `CookiesStorage`) : cookies en mémoire pour la session ; **persiste uniquement le `refresh_token`, chiffré AES-GCM** (clé KeyStore JCEKS) dans un fichier sous `%APPDATA%/E-JDR/`. L'`access_token` reste **en mémoire seulement**.

Flux :
1. **Login / Register** → `POST /auth/login` ou `/auth/register` body `{email, password}`. Réponse `{userId, email}` + cookies posés → captés par `SecureCookiesStorage` (refresh_token persisté chiffré). Entrée dans l'app.
2. **Auto-login au démarrage** → `RestoreSessionUseCase` lit le `refresh_token` persisté ; s'il existe, `POST /auth/refresh`. Succès → nouveaux cookies (rotation) → app. Échec → cookies effacés → écran Login.
3. **Requêtes authentifiées** (futures) → Ktor renvoie les cookies automatiquement. Sur `401`, tentative `/auth/refresh` une fois puis rejeu (câblé proprement mais inactif tant qu'aucune route protégée n'existe).
4. **Logout** → `POST /auth/logout` puis effacement des cookies mémoire **et** du fichier persisté.

**Erreurs** : l'API renvoie `{code, message}` + code HTTP. `AuthHttpMapper` traduit en `AuthError` du domaine. Les use cases renvoient `Result<…, DomainError>`.

## Présentation (Compose)

- **`main.kt`** : `application { Window {…} }`, démarre Koin, lance `RestoreSessionUseCase` au boot, affiche flux Auth ou app.
- **Navigation** : par état (`sealed Screen`) au niveau racine — pas de lib de nav.
- **Pages intelligentes** (`feature/auth/page/`) : `LoginPage`, `RegisterPage`. Injectent les use cases (`koinInject()`), tiennent l'état (state holder / `mutableStateOf`), appellent les use cases en coroutines, mappent `DomainError` → message UI.
- **Composants bêtes** (`feature/auth/component/`) : `LoginForm`, `RegisterForm` — valeurs + callbacks (`onEmailChange`, `onSubmit`…), aucun appel.
- **Shared atomic design** : `AppButton`/`AppTextField` (atomic), `LabeledTextField`/`FormError` (molecule), organismes — réutilisables, sans dépendance feature.

## DI, config, secrets

- **Koin** : `infrastructureModule` (Ktor client, `SecureCookiesStorage`, `KeyStoreProvider`, `AppConfig`), `applicationModule` (services + use cases liés à leurs interfaces). Composition root dans `main.kt`. Le câblage interface→impl s'y fait ; la présentation ne connaît que les interfaces.
- **Config / secrets** : base URL + flags d'env dans `AppConfig`, lus depuis variables d'env ou fichier local non commité.
- **`.gitignore`** : exclut KeyStore, fichier de cookies chiffré, secrets/config locale, artefacts de build.

## Tests

JUnit5 + MockK + kotlin.test :
- Use cases auth : `LoginUseCase`, `RegisterUseCase`, `RestoreSessionUseCase`, `LogoutUseCase` (orchestration, repo mocké, bon `Result`/`DomainError`).
- Services auth (logique réutilisable).
- `AuthHttpMapper` (DTO/HTTP `{code,message}` → `AuthError`) + validation entités/erreurs domaine.
- Pas de tests UI Compose.

## Conventions

- **Commits** : Conventional Commits (`feat:`, `chore:`, `test:`, `build:`…), atomiques par étape logique.
- **Nommage / clean code** : conventions Kotlin standards, noms explicites, SOLID, fichiers focalisés (une responsabilité).

## Vérification (end-to-end)

1. `./gradlew build` compile sans erreur.
2. `./gradlew test` : tous les tests unitaires passent.
3. `./gradlew run` lance la fenêtre desktop : écran Login s'affiche au premier lancement.
4. Connexion via `/auth/login` (backend lancé) → entrée dans l'app ; vérifier la persistance chiffrée du `refresh_token`.
5. Fermer/relancer l'app → auto-login via `/auth/refresh` (pas de re-saisie du mot de passe).
6. Logout → cookies + fichier persisté effacés ; relance → écran Login.

## Hors périmètre (pour plus tard)

- WebSocket.
- Feature Host et autres features.
- Routes protégées (le mécanisme de refresh sur 401 est câblé mais inactif).
- Tests UI Compose, tests d'intégration HTTP (MockEngine).
