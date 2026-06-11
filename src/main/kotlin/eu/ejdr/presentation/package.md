# Couche Presentation

La couche `presentation` regroupe toute l'interface utilisateur (Compose Desktop). Elle consomme
les use cases de la couche `application` mais ne contient aucune logique métier : son rôle est
d'afficher l'état et de remonter les interactions de l'utilisateur.

## Design system (`shared/theme`)

Le look est piloté depuis un **design system maison** (pas de dépendance au theming Material) via
des `CompositionLocal` exposés par l'objet `AppTheme` :

- `AppColors` — palette gris/beige + accent taupe (`AppTheme.colors`).
- `AppTypography` — styles `title` / `subtitle` / `body` / `label` / `caption` (`AppTheme.typography`).
- `AppDimens` — espacements, rayons, bordures (`AppTheme.dimens`).

Le composable racine `AppTheme { }` fournit ces jetons à tout l'arbre. **Aucune couleur ni
dimension en dur** dans les composants : ils lisent toujours `AppTheme`.

## Atomic design (`shared/component`)

Les composants transverses et réutilisables sont organisés selon l'atomic design, du plus simple
au plus composé :

- `atomic/` : briques de base — `AppText`, `AppButton`, `AppTextField`, `AppPasswordField`,
  `AppNumberField`, `AppCheckbox`, `AppIcon`, `AppDivider` (+ `VerticalSpacer` / `HorizontalSpacer`).
- `molecule/` : assemblages d'atomes — `LabeledField` (libellé + slot + erreur), `FieldGroup`
  (groupe vertical espacé), `FormError`.
- `organism/` : assemblages complexes — `AppTopBar` (barre supérieure de la zone connectée),
  `AppScaffold` (ossature top bar + contenu).

Tout ce qui se trouve dans `shared/` est **réutilisable** et **sans dépendance** à une feature.

## Organisation par feature (`features/<feature>`)

Comme les autres couches, la présentation sépare le transverse (`shared/`) du
spécifique à une fonctionnalité (`features/<feature>/`). Chaque fonctionnalité est
isolée dans son dossier, avec deux sous-dossiers :

- `component/` : composants **bêtes** (stateless) propres à la feature.
- `page/` : pages **intelligentes** qui orchestrent la feature.

Chaque feature regroupe, sous `features/<feature>/` :

- `<Feature>ViewModel.kt` : le ViewModel (à la **racine** de la feature, pas dans
  `page/`). Il détient l'état (`StateFlow<…UiState>`), porte la logique dans son
  `viewModelScope`, et reçoit ses **use cases par constructeur**.
- `page/` : pages **intelligentes** — fines. Elles créent le ViewModel (retenu par
  destination) et observent son état.
- `component/` (au besoin) : composants **bêtes** pilotés par props/callbacks.

### Ajouter une feature à la présentation

1. Définir une route dans `navigation/Routes.kt` : `@Serializable data … : Route`
   (les arguments d'écran voyagent dans la clé).
2. Créer `features/<feature>/<Feature>ViewModel.kt` : `class … : ViewModel()`, état
   en `StateFlow`, événements one-shot (navigation) via un `Channel`, use cases
   injectés au constructeur.
3. Créer `features/<feature>/page/<Feature>Page.kt` : `koinInject` les use cases, crée
   le VM via `viewModel { <Feature>ViewModel(useCase) }`, observe l'état avec
   `collectAsStateWithLifecycle`, collecte les événements one-shot dans un
   `LaunchedEffect` pour naviguer.
4. Brancher la route dans `navigation/AppNavDisplay.kt` (`entry<Route.X> { … }`).

## Composants bêtes vs pages intelligentes vs ViewModels

- **Composants (bêtes)** : aucun état, aucun use case. Valeurs en paramètres,
  événements via callbacks. Préservés tels quels (préviewables).
- **Pages (intelligentes)** : créent et observent le ViewModel ; ne détiennent plus
  d'état métier en `remember`. Elles traduisent les événements one-shot du VM en
  appels de navigation.
- **ViewModels** : détiennent l'état (`StateFlow`), exécutent la logique dans le
  `viewModelScope` (annulation propre à la destruction de l'écran), et exposent le
  message d'erreur via `error.message` (source unique, cf. couche domaine).

## Navigation et démarrage (Navigation 3)

- La navigation repose sur un **back-stack possédé par `App`**
  (`rememberNavBackStack`, une `SnapshotStateList<NavKey>`). Naviguer = empiler
  (`backStack.add(Route.X)`), revenir = dépiler (`removeLastOrNull`). Le retour
  multi-niveaux est donc natif. `App` délègue le mapping route → écran à
  `navigation/AppNavDisplay` (`NavDisplay` + `entryProvider`).
- Les **ViewModels sont retenus par destination** via le décorateur
  `rememberViewModelStoreNavEntryDecorator` : leur état survit à la recomposition et à
  un aller-retour de navigation.
- `App` fournit le design system (`AppTheme`), tente un **auto-login**
  (`RestoreSessionUseCase`) au démarrage, puis remplace l'écran `Splash` par `Home`
  ou `Login` (`backStack.clear()` + `add`, ce qui efface l'historique d'avant-auth).

### Zone non-connectée vs zone connectée

- **Non-connectée** (`Login` / `Register`) : pages rendues en **plein écran**.
- **Connectée** (`Home` / `Settings`) : rendue dans un `AppScaffold` avec une
  `AppTopBar` **présente partout** (titre + Déconnexion). Le profil `User` n'est plus
  transporté dans la route : `UserViewModel` le charge via `GetCurrentUserUseCase`. Le
  bouton Déconnexion appelle `LogoutUseCase` puis réinitialise la pile sur `Login`.
