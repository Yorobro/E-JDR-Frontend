# Couche Infrastructure

La couche **infrastructure** regroupe les détails techniques de l'application.
Elle **implémente les ports** (interfaces) définis par la couche `application` :
le métier et la présentation ne dépendent que de ces interfaces, jamais des
implémentations concrètes présentes ici. C'est la traduction du principe
d'inversion des dépendances.

## Organisation

- `config/` — configuration applicative (`AppConfig` : URL d'API, dossier de
  données, activation du logging HTTP), chargée depuis les variables d'environnement.
- `security/` — primitives de sécurité locales : `KeyStoreProvider` (clé AES dans
  un KeyStore JCEKS), `CookieCipher` (chiffrement AES-GCM) et `SecureCookiesStorage`
  (stockage des cookies persistant le refresh_token chiffré).
- `http/` — accès réseau via Ktor. À la racine : `KtorClientFactory` (fabrique du
  `HttpClient`, transverse). Les implémentations HTTP par feature vivent sous
  `http/features/<feature>/` : DTO de transport (`http/features/auth/dto/`), mapper
  DTO/HTTP -> domaine (`AuthHttpMapper`) et implémentation des ports
  (`AuthHttpRepository`, `http/features/update/UpdateHttpRepository`).
- `realtime/` — connexion **temps réel** (WebSocket), implémentation du port
  `application/features/realtime/abstraction/RealtimeConnection`.
  `KtorRealtimeConnection` porte la **machine à états de reconnexion** (backoff via
  `ReconnectPolicy`) et délègue la mécanique du socket à un `RealtimeTransport`
  (seam testable). `KtorWebSocketTransport` ouvre la session Ktor réelle avec
  **auth-on-connect** (refresh proactif avant connexion, car une connexion longue
  durée n'est PAS couverte par l'intercepteur 401 du client REST). Câblé dans
  `di/RealtimeModule.kt`. Aucun écran ne le consomme encore (prêt pour la 1re feature
  temps réel).
- `system/` — effets de bord OS derrière des services application : `WindowsSystemLauncher`
  (lancement d'installeur + exit), implémentation de `SystemLauncherService`.

### Ajouter un accès HTTP pour une feature

Créer `http/features/<feature>/` contenant le repository HTTP (implémentation d'un
port déclaré dans `application/features/<feature>/abstraction/repository/`), ses
`dto/` et, si besoin, un mapper DTO -> domaine. Réutiliser le `HttpClient` partagé
fourni par `KtorClientFactory` (injecté via Koin). Lier ensuite le port à son implémentation dans le module Koin de la feature :
`di/<feature>Module.kt` (ex. `authModule`, `settingsModule`, `updateModule`).
`infrastructureModule` est réservé au socle technique transverse (config,
sécurité/coffre, HttpClient).

## Modèle d'authentification

L'authentification repose sur des **cookies HttpOnly posés par le serveur** : le
code client ne manipule jamais directement les jetons. Seul le **refresh_token est
persisté, chiffré** sur disque (`SecureCookiesStorage` + `CookieCipher`), afin de
restaurer la session après un redémarrage. L'access_token reste en mémoire.
En cas de refresh échoué ou de déconnexion, le cookie persisté est effacé.

## Injection de dépendances (`di/`)

Le câblage est assuré par **Koin** (package `di/`, techniquement à part).
`infrastructureModule` fournit le **socle** technique transverse (configuration,
sécurité/coffre, client HTTP). Chaque feature possède ensuite son module dédié
(`authModule`, `settingsModule`, `updateModule`, `realtimeModule`) qui enregistre
ses use cases, services et adaptateurs infra et lie chaque port application à son
implémentation infra (ex. `AuthRepository -> AuthHttpRepository`). `initKoin()`
constitue la *composition root* et charge ces modules au démarrage.
