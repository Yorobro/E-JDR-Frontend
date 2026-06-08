# E-JDR — Frontend Desktop

Application **desktop** (Compose for Desktop, JVM pur) du projet E-JDR, écrite en **Kotlin** sous une **clean architecture** stricte (SOLID, clean code).

## Prérequis

- JDK 17 ou supérieur (testé avec Temurin 21).
- Aucun Gradle global requis : le wrapper (`gradlew` / `gradlew.bat`) est fourni.

## Commandes

```bash
# Lancer l'application desktop
./gradlew run

# Lancer les tests unitaires
./gradlew test

# Build complet (compilation + tests)
./gradlew build

# Analyse statique (detekt) et vérification de couverture (Kover)
./gradlew detekt koverVerify

# Empaqueter une distribution native (exe/msi)
./gradlew packageDistributionForCurrentOS
```

Sous Windows PowerShell, utiliser `.\gradlew.bat run`, etc.

## Configuration

| Variable d'env   | Défaut                  | Rôle                                              |
|------------------|-------------------------|---------------------------------------------------|
| `EJDR_API_URL`   | `http://localhost:3000` | URL de base de l'API d'authentification.          |
| `EJDR_HTTP_LOG`  | `true`                  | Active/désactive le logging HTTP Ktor.            |

Les secrets et le KeyStore sont stockés dans `%APPDATA%/E-JDR/` (jamais commités).

## Architecture

Quatre couches en packages sous `eu.ejdr`, dépendances unidirectionnelles
`presentation → application → domain` et `infrastructure → application/domain` :

- **`domain/`** — entités métier **pures** (data classes sans méthode) et erreurs
  (`DomainError`, `error/entities/<feature>/...Error`).
- **`application/`** — par feature : `abstraction/{repository,service,usecase}`
  (interfaces), `service/` (logique réutilisable), `usecase/` (orchestration pure).
  Règles : un use case n'appelle jamais un autre use case ; un service peut appeler
  repositories et autres services. Les use cases renvoient `Result<T, DomainError>`.
- **`infrastructure/`** — `config/` (AppConfig), `security/`
  (KeyStore JCEKS + chiffrement AES-GCM + `SecureCookiesStorage`), `http/`
  (client Ktor, DTO, mappers, repositories HTTP).
- **`presentation/`** — `shared/component/{atomic,molecule,organism}` (atomic design
  réutilisable) ; `feature/<feature>/{page,component}`. Seules les **pages**
  appellent les use cases ; les **composants** sont bêtes (props + callbacks).

L'injection de dépendances est gérée par **Koin** (`di/`).

## Authentification & sécurité

Le backend gère l'auth **par cookies** (`access_token` / `refresh_token`) qu'il pose
lui-même. Côté desktop :

- `SecureCookiesStorage` persiste **uniquement** le `refresh_token`, **chiffré
  AES-GCM** (clé dans un KeyStore JCEKS local), sous `%APPDATA%/E-JDR/`.
  L'`access_token` reste en mémoire.
- Le mot de passe du KeyStore est **dérivé d'attributs locaux** (utilisateur + machine,
  SHA-256) et non codé en dur : le coffre est lié à son environnement. Ce n'est pas un
  secret matériel (un attaquant ayant un accès complet au poste pourrait le reconstituer),
  mais cela supprime tout secret en clair dans le binaire.
- Au démarrage, `RestoreSessionUseCase` tente un `/auth/refresh` silencieux pour
  rester connecté après redémarrage ; en cas d'échec, l'écran de connexion s'affiche.
- Logout efface les cookies en mémoire et le fichier persisté.

## État du périmètre

Inclus : squelette d'architecture complet + feature **Auth** (login, register,
auto-login, logout). À venir : WebSocket, feature Host, routes protégées
(le refresh-sur-401 est prévu mais inactif tant qu'aucune route protégée n'existe).

## Versioning & Releases

- **Stratégie**: SemVer (MAJOR.MINOR.PATCH) combiné avec *Conventional Commits*.
  - `feat:` → bump MINOR
  - `fix:` → bump PATCH
  - `BREAKING CHANGE` ou `!` → bump MAJOR

- **Automatisation**: `semantic-release` est configuré (`.releaserc.json`) pour
  analyser les commits, calculer le nouveau numéro, mettre à jour `gradle.properties`,
  générer `CHANGELOG.md` et créer la Release GitHub.

- **Hooks locaux**: `commitlint` + `husky` sont fournis pour valider les messages
  de commit. Après avoir installé les dépendances, exécute:

```bash
npm install
npm run prepare   # installe les hooks husky (commit-msg)
```

- **Flux CI/CD**:
  1. Poussez vos commits (respectant *Conventional Commits*) sur `main`.
  2. Le workflow `.github/workflows/release.yml` exécute `semantic-release` et
     publie une Release GitHub si un bump est requis.
  3. Le workflow CD (`.github/workflows/cd.yml`) est déclenché sur l'événement
     `release.published`: il build les distributions natives (Windows EXE/MSI)
     et attache les artefacts à la Release.

- **Bump manuel**: si tu préfères un contrôle manuel, mets à jour `version` dans
  `gradle.properties` avant de merger la PR.

- **Validation**: le workflow `CI - release tooling checks` valide localement les
  messages de commit dans les PRs.

