# Spec — Corrections d'audit F1–F7 (Frontend E-JDR)

> **Date** : 2026-06-12
> **Périmètre** : `E-JDR-Frontend/` (Compose Desktop, repo git propre, branche `main`).
> **Origine** : `docs/AUDIT_ARCHITECTURE.md`, faiblesses F1 à F7.
> **But** : améliorer cohérence et scalabilité **sans refonte**, en suivant les conventions
> déjà présentes dans le codebase.
> **Langue** : code et commentaires en **français** (convention du repo).

## Principes transverses

- **TDD** quand la logique est testable (combinators `Result`, ViewModels, contrats use case).
- **Vérification** : `./gradlew verify` (= CI : detekt + build + tests + Kover) vert **à chaque
  étape**, ET `./gradlew run` pour confirmer que la fenêtre s'ouvre (leçon Nav3 desktop : le
  compile/test ne capture pas les crashs runtime de sérialisation/decorator).
- **Commits** : Conventional Commits, un commit par faiblesse (ou sous-étape), sur `main`.
- **Pas d'i18n** (refus explicite antérieur de l'utilisateur — on garde les libellés FR en dur).
- **Pas de sur-ingénierie** : on n'ajoute pas de feature, on aligne l'existant.

---

## F4 — Harmonisation des contrats *(fait en premier : les autres en dépendent)*

### F4.a — `Result` ergonomique
`application/shared/Result.kt` gagne, en plus de `fold` :
- `map(transform: (T) -> R): Result<R, E>`
- `flatMap(transform: (T) -> Result<R, E>): Result<R, E>` *(E doit rester compatible)*
- `mapError(transform: (E) -> F): Result<T, F>` *(F : DomainError)*
- `getOrElse(onFailure: (E) -> T): T`
- `getOrNull(): T?`
- `onSuccess(action: (T) -> Unit): Result<T, E>` / `onFailure(action: (E) -> Unit): Result<T, E>`

Toutes `inline`, en fonctions d'extension (cohérent avec `fold`). KDoc en français.
**Tests** : `application/shared/ResultTest.kt` (un cas succès + un cas échec par combinator).

### F4.b — Settings homogène
- `ThemeRepository.getTheme()` → `suspend fun getTheme(): Result<ThemeVariant, SettingsError>`
- `ThemeRepository.setTheme(...)` → `suspend fun setTheme(v): Result<Unit, SettingsError>`
  *(remplace le `Boolean`)*
- `GetThemeUseCase.invoke()` → `suspend (): Result<ThemeVariant, SettingsError>`
- Impls (`ThemeFileRepository`, `GetThemeUseCaseImpl`, `SetThemeUseCaseImpl`) adaptées :
  l'I/O `Properties` reste enveloppée en `runCatching`, mappée vers
  `Result.Failure(SettingsError.ThemePersistenceFailed)` à l'échec, lecture absente →
  `Result.Success(LIGHT)` (le défaut reste un **succès**, pas une erreur).
- `SettingsError` : ajouter une variante de lecture si utile (`ThemeReadFailed`) **uniquement
  si** un échec de lecture doit être distingué ; sinon réutiliser `ThemePersistenceFailed`.
  → Décision : **réutiliser** `ThemePersistenceFailed` renommée conceptuellement n'est pas
  nécessaire ; on garde une seule variante et le défaut LIGHT reste un succès. Pas de nouvelle
  variante.

### F4.c — Update sans fuite d'exception
- Nouveau `domain/features/update/error/UpdateError.kt` (`sealed class : DomainError`) :
  `DownloadFailed`, `InstallFailed`, `Network`, `Unknown(detail)` (avec messages FR).
- `UpdateRepository.downloadUpdate(url, onProgress)` →
  `suspend (): Result<java.io.File, UpdateError>` *(le `File` reste, type JVM acceptable ici)*.
- `DownloadAndInstallUpdateUseCase.invoke(...)` → `suspend (): Result<Unit, UpdateError>`
  *(le succès n'est jamais « rendu » car l'installeur quitte la JVM ; mais l'échec **avant**
  le lancement est désormais un `Result.Failure` au lieu d'une exception qui fuit)*.
- `CheckUpdateUseCase` reste `(): UpdateInfoDto?` (pas d'erreur métier attendue — null = pas
  d'update ; on **ne** force pas un Result ici, ce serait du bruit).
- Impls (`UpdateHttpRepository`, `DownloadAndInstallUpdateUseCaseImpl`) : `runCatchingCancellable`
  → mapper l'échec en `UpdateError`. `UpdateHttpRepository` gagne un **timeout** de requête
  (corrige F12 au passage, faible coût).
- `UpdateDialog`/`UpdateViewModel` (cf. F6) consomment le `Result`.

### F4.d — `hasPersistedSession` suspend
- `AuthRepository.hasPersistedSession()` → `suspend`.
- `SessionPersistence.hasPersistedSession()` → `suspend` (impl `SecureCookiesStorage`).
- `SessionService.hasPersistedSession()` → déjà via service ; aligner suspend.
- Appelants (`SessionServiceImpl`, éventuels VMs) adaptés — déjà en contexte coroutine.

**Tests impactés** : mettre à jour les `*UseCaseImplTest`, `ThemeFileRepositoryTest`,
`UpdateHttpRepositoryTest`, `SessionServiceImplTest` pour les nouvelles signatures.

---

## F6 — `UpdateViewModel` (dépend de F4.c)

- Créer `presentation/features/update/UpdateViewModel.kt` :
  - état `StateFlow<UpdateUiState>` où `UpdateUiState` = `Idle(info)` / `Downloading(progress?)`
    / `Error(message)` (déplace la `sealed DownloadState` hors du composable).
  - `fun onInstall()` / `fun onRetry()` lançant `downloadAndInstall(...)` dans `viewModelScope`,
    consommant le `Result<Unit, UpdateError>` (succès → la JVM quitte ; échec → `Error(message)`
    en lisant `error.message`).
  - cas « pas d'URL » → ouvrir la page release via un port effet (ou laisser le composable
    gérer `Desktop.browse`, qui est un effet UI pur — **décision : garder `Desktop.browse` dans
    le composable**, ce n'est pas de la logique métier).
- `UpdateDialog` devient **bête** : reçoit `state: UpdateUiState`, `onInstall`, `onRetry`,
  `onDismiss`. Plus de `rememberCoroutineScope`/`mutableStateOf` métier dedans.
- Câblage : `App.kt` crée le `UpdateViewModel` (via `koinViewModel{}`) quand `updateInfo != null`.
- **Tests** : `UpdateViewModelTest` (install OK ne throw pas ; échec → état Error avec message).

---

## F1 — Navigation : registre par feature

- Pour chaque feature ayant des écrans (`auth`, `settings`, et `user`/`home`), créer
  `presentation/features/<feature>/navigation/<Feature>Routes.kt` exposant :
  - les `Route` `@Serializable` **de cette feature** (déplacées depuis `navigation/Routes.kt`),
  - `val <feature>Routes: List<KClass<out Route>>`,
  - `fun EntryProviderBuilder<NavKey>.<feature>Entries(backStack, callbacks…)` contenant les
    `entry<…>` correspondants (déplacés depuis `AppNavDisplay`).
- `navigation/Routes.kt` conserve `sealed interface Route : NavKey`, déplace les `data object`
  vers les fichiers feature (les sous-types restent des `Route`), et construit
  `appNavConfiguration` en **bouclant** sur `(authRoutes + settingsRoutes + … + appRoutes)` :
  `polymorphic(NavKey::class) { allRoutes.forEach { subclass(it) } }`.
  → **Plus d'oubli possible** du `subclass()` : ajouter une route à sa liste suffit.
- `AppNavDisplay` agrège : `entryProvider { authEntries(...); settingsEntries(...); appEntries(...) }`.
- **Route paramétrée** : ajouter le squelette d'une route param `@Serializable data class …(val id: …)`
  *(commentée/`TODO` only si aucun écran ne l'utilise encore — sinon on n'introduit pas de code
  mort ; **décision : ne PAS ajouter de route param fictive**. On rend le mécanisme prêt (listes
  + boucle), la 1re vraie route param viendra avec son écran)*.
- **Test de garde** : `navigation/RoutesRegistrationTest.kt` — réfléchit `Route::sealedSubclasses`
  et asserte que chacune est dans la liste agrégée (donc dans `appNavConfiguration`). Ce test
  **remplace** le risque de crash runtime par un échec de build.

---

## F2 — `AppViewModel` racine (état global)

- `presentation/AppViewModel.kt` :
  - deps : `GetThemeUseCase`, `SetThemeUseCase`, `RestoreSessionUseCase`, `LogoutUseCase`.
  - `val theme: StateFlow<ThemeVariant>` (init via `getTheme()`),
  - `val session: StateFlow<SessionState>` avec `sealed interface SessionState { Loading;
    Authenticated(user); Anonymous }`,
  - `fun onThemeChange(v)` (met à jour le flow + persiste via `setTheme`),
  - `fun bootstrap()` : lance `restoreSession()` → `Authenticated`/`Anonymous` (remplace le
    `LaunchedEffect` qui appelait les use cases directement),
  - `fun onLogout()` : `logout()` → `Anonymous`.
- `App.kt` : obtient l'`AppViewModel` via `koinViewModel{}`, observe `theme`/`session`, appelle
  `bootstrap()` dans un `LaunchedEffect(Unit)`. Le `mutableStateOf` du thème et les
  `koinInject<…UseCase>()` du composable disparaissent.
- La navigation reste pilotée par le back-stack ; `App.kt` mappe `session` → `resetTo(Home/Login)`.
- **Note Nav3 decorator** : l'`AppViewModel` est créé **hors** du back-stack (au niveau App),
  pas via le décorateur par-destination — il prend ses deps par constructeur (compatible avec la
  contrainte « pas de SavedStateHandle »).
- Câblé dans `authModule`/un module présentation selon F5.
- **Tests** : `AppViewModelTest` (bootstrap succès → Authenticated ; échec → Anonymous ;
  onThemeChange persiste ; onLogout → Anonymous).

---

## F5 — DI par feature

- Remplacer `applicationModule` + `infrastructureModule` par des modules **par feature**,
  chacun regroupant infra **et** application de la feature (modèle `realtimeModule`) :
  - `authModule` (KtorClient/cookies/security partagés + AuthRepository + use cases auth + session),
  - `settingsModule` (ThemeRepository + use cases settings),
  - `updateModule` (UpdateRepository + SystemLauncher + use cases update + UpdateViewModel),
  - `realtimeModule` (inchangé),
  - un `coreModule` pour le **transverse** réellement partagé : `AppConfig`, `KeyStoreProvider`,
    `CookieCipher`, `SecureCookiesStorage`, `HttpClient` (utilisé par auth/update/realtime).
- `AppKoin.initKoin()` : `modules(coreModule, authModule, settingsModule, updateModule, realtimeModule)`.
- **Décision périmètre** : on ne fait PAS de sous-Koin-scopes ; juste un fichier-module par
  feature. Les bindings restent `single`. Pas de changement de cycle de vie.

---

## F3 — Release gated par CI

- `release.yml` : ajouter un job `verify` (reflet de `ci.yml` : JDK 21 + Gradle + `./gradlew
  verify --no-daemon --stacktrace`, `runs-on: ubuntu-latest`) ; rendre `semantic_release`
  dépendant : `needs: verify`. `windows_build` garde `needs: semantic_release`.
- Effet : aucun tag/binaire produit si `verify` échoue.
- (Optionnel, faible coût) factoriser via un workflow réutilisable `workflow_call` plutôt que
  dupliquer les steps — **décision : dupliquer le job** (3 steps), plus simple à lire ; pas de
  réutilisable pour 3 lignes.

---

## F7 — Kover : inclure les ViewModels

- `build.gradle.kts`, bloc `kover { … excludes { … } }` : remplacer
  `packages("eu.ejdr.presentation", "eu.ejdr.di")` par une exclusion **ciblée UI** :
  - garder `packages("eu.ejdr.di")`,
  - exclure les classes **UI** seulement : `classes("eu.ejdr.presentation.*.page.*",
    "eu.ejdr.presentation.*.component.*", "eu.ejdr.presentation.navigation.*",
    "eu.ejdr.presentation.shared.theme.*", "eu.ejdr.presentation.shared.component.*",
    "eu.ejdr.MainKt")` *(syntaxe Kover exacte à valider : `classes` accepte des wildcards ;
    sinon recourir à `annotatedBy`/`packages` négatifs)*.
  - **Conséquence** : `*ViewModel` (et `AppViewModel`) comptent dans les 60 %. Comme ils sont
    déjà testés, le plancher doit rester vert — **à confirmer par `koverVerify`**. Si le seuil
    casse, ajouter les tests VM manquants (pas baisser le seuil).
- Mettre à jour `docs/AUDIT_ARCHITECTURE.md` (F7) et le commentaire du bloc Kover.

---

## Ordre d'exécution (dépendances)

1. **F4** (contrats + `Result`) — socle ; casse des signatures, donc d'abord.
2. **F6** (`UpdateViewModel`) — consomme F4.c.
3. **F1** (registre navigation) — indépendant mais touche presentation comme F6.
4. **F2** (`AppViewModel`) — s'appuie sur les use cases harmonisés (F4).
5. **F5** (DI par feature) — câble F2/F6 ; après que les classes existent.
6. **F7** (Kover) — après que les VMs (dont AppViewModel/UpdateViewModel) et leurs tests existent.
7. **F3** (CI release) — indépendant ; peut être fait à tout moment, placé en dernier.

`./gradlew verify` + `./gradlew run` après **chaque** étape. Commit par étape.

## Hors périmètre (dette documentée, non traitée ici)

- F8 portabilité OS (`WindowsSystemLauncher`), F9 PBKDF2 + 401 ambigu, F10 `FormState`,
  F11 tests UI Compose / route guard. Restent dans `AUDIT_ARCHITECTURE.md` comme axes futurs.
