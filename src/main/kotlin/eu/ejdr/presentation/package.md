# Couche Presentation

La couche `presentation` regroupe toute l'interface utilisateur (Compose Desktop). Elle consomme
les use cases de la couche `application` mais ne contient aucune logique métier : son rôle est
d'afficher l'état et de remonter les interactions de l'utilisateur.

## Atomic design (`shared/component`)

Les composants transverses et réutilisables sont organisés selon l'atomic design, du plus simple
au plus composé :

- `atomic/` : briques de base (ex. `AppButton`, `AppTextField`).
- `molecule/` : assemblages d'atomes (ex. `LabeledTextField`, `FormError`).
- `organism/` : assemblages plus complexes de molécules/atomes.

Tout ce qui se trouve dans `shared/` est **réutilisable** et **sans dépendance** à une feature.

## Organisation par feature (`feature/<feature>`)

Chaque fonctionnalité est isolée dans son dossier, avec deux sous-dossiers :

- `component/` : composants **bêtes** (stateless) propres à la feature.
- `page/` : pages **intelligentes** qui orchestrent la feature.

## Composants bêtes vs pages intelligentes

- **Composants (bêtes)** : ils ne détiennent aucun état et n'appellent **aucun** use case. Ils
  reçoivent leurs valeurs en paramètres et remontent les événements via des callbacks.
- **Pages (intelligentes)** : seul endroit qui injecte et appelle les use cases (via `koinInject`),
  détient l'état local, et traduit les erreurs domaine (`Result.Failure`) en messages UI.

## Navigation et démarrage

- La **navigation se fait par état** : l'écran courant est une valeur de l'interface scellée
  `Screen` (Splash / Login / Register / Home), et changer d'écran revient à modifier cet état.
- `App` est le composable racine : il tente un **auto-login** (`RestoreSessionUseCase`) au
  démarrage, puis effectue le routing par état vers la page correspondante.
