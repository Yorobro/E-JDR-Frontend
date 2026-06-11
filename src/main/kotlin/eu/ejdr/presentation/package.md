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

### Ajouter une feature à la présentation

Créer `features/<feature>/` avec `page/` (composants intelligents qui injectent les
use cases via `koinInject` et détiennent l'état) et, au besoin, `component/`
(composants bêtes pilotés par props/callbacks). Réutiliser les briques de
`shared/component/` et les jetons `AppTheme` ; brancher l'écran dans la navigation
par état (`Screen` + `AppRouter`).

## Composants bêtes vs pages intelligentes

- **Composants (bêtes)** : ils ne détiennent aucun état et n'appellent **aucun** use case. Ils
  reçoivent leurs valeurs en paramètres et remontent les événements via des callbacks.
- **Pages (intelligentes)** : seul endroit qui injecte et appelle les use cases (via `koinInject`),
  détient l'état local, et traduit les erreurs domaine (`Result.Failure`) en messages UI.

## Navigation et démarrage

- La **navigation se fait par état** : l'écran courant est une valeur de l'interface scellée
  `Screen` (Splash / Login / Register / User), et changer d'écran revient à modifier cet état.
- `App` est le composable racine : il fournit le design system (`AppTheme`), tente un
  **auto-login** (`RestoreSessionUseCase`) au démarrage, puis effectue le routing par état.

### Zone non-connectée vs zone connectée

- **Non-connectée** (`Login` / `Register`) : pages rendues en **plein écran**.
- **Connectée** (`Home` / `Settings`) : rendue dans un `AppScaffold` avec une `AppTopBar`
  **présente partout** (titre + Déconnexion). `Screen.Home` porte l'`User` connecté (`null` si
  arrivé par auto-login sans profil chargé) ; le bouton Déconnexion appelle `LogoutUseCase` puis
  revient à l'écran de connexion.
