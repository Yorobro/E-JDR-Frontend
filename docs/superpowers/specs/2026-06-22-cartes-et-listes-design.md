# Cartes & listes (Lot 2) — Design

> **Statut :** spec validée en brainstorming (2026-06-22). Deuxième des 3 lots d'amélioration UX/UI.
> **Lot 1 (fait) :** socle d'animation (AppMotion, interactions, dialogs, transitions). **Lot 3 (à venir) :** en-têtes de page + snackbar + validation progressive.

## Objectif

Améliorer le ressenti des écrans de liste/grille sans toucher au backend ni aux couleurs : apparition en fondu des items, **skeletons** de chargement (au lieu d'un spinner nu), **états vides accueillants** (icône + message + bouton d'action), et meilleure mise en forme des cartes **avec les seules données déjà disponibles côté front**.

## Contrainte de données (constat vérifié, structurant)

L'API renvoie une **projection nom-seul** pour les fiches en liste : `niveau`, `formation`, `peuple` arrivent `null` dans la grille (documenté dans `CharacterSheetDto`, lignes 43-45 : *« projection nom seul, sans ces clés ; seul le détail les renseigne »*). **On n'enrichit donc PAS les cartes de fiche avec ces champs** — cela exigerait une modif backend, hors périmètre. On se limite aux données réellement présentes en liste :
- Fiche : `name`, `createdAt`.
- Campagne : `name`, `createdAt`.
- Session : `title`, `date`, `createdAt`.
- Groupe (liste) : `name`, `myRole`. Groupe (détail) : `members` (donc `members.size`).

## Personnalité (héritée du Lot 1)

Sobre & pro : fondu **discret** (pas de stagger spectaculaire), shimmer doux, durées issues de `AppMotion`. Le mouvement guide sans se faire remarquer.

## Composants & unités

### 1. `SkeletonBox` (nouveau, atome) — `presentation/shared/component/atomic/SkeletonBox.kt`

Surface grise à coins arrondis qui **pulse** (shimmer). Brique de base des cartes fantômes.
- Animation : `rememberInfiniteTransition` pilotant un `alpha` (ou un gradient translaté) entre deux valeurs, durée dérivée de `AppMotion` (un token dédié `durationSlow` = 300ms réservé à cet usage, déjà présent ; le cycle de pulsation peut être un multiple documenté). Respecte `AppMotion.enabled` : si désactivé, pas d'animation (alpha fixe).
- Couleur : dérivée de `AppTheme.colors` (ex. `beige`/`border`), pas de hex.
- Signature : `@Composable fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(AppTheme.dimens.radiusMd))`.
- **Dépend de :** `AppTheme.colors/dimens/motion`.

### 2. `SkeletonGrid` / `SkeletonList` (nouveau, molécule) — `presentation/shared/component/molecule/SkeletonPlaceholders.kt`

Affiche N `SkeletonBox` à la forme d'une grille/liste, pendant le chargement initial.
- `@Composable fun SkeletonGrid(count: Int = 6, itemHeight: Dp, columns: GridCells = GridCells.Adaptive(...), modifier)` — reproduit la disposition adaptative des grilles existantes.
- `@Composable fun SkeletonList(count: Int = 5, itemHeight: Dp, modifier)` — pour les `LazyColumn` (groupes).
- **Dépend de :** `SkeletonBox`, `AppTheme.dimens`.
- **Utilisé par :** les pages de liste à la place de `CircularProgressIndicator` durant le **chargement initial** (liste vide + isLoading).

### 3. `EmptyState` (nouveau, molécule) — `presentation/shared/component/molecule/EmptyState.kt`

Écran vide accueillant : icône + titre + message + bouton d'action optionnel.
- `@Composable fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null)`.
- Le bouton (si `actionLabel`/`onAction` fournis) réutilise `AppButton` (variant Primary).
- Centré, espacement via `AppTheme.dimens`. Texte dans la voix de l'app (impératif, accueillant — ex. titre « Aucune fiche pour l'instant », bouton « Créer ma première fiche »).
- **Dépend de :** `AppText`, `AppIcon`, `AppButton`, `AppTheme`.
- **Utilisé par :** les pages de liste, en remplacement des « Aucun X » textuels.

### 4. Utilitaire de date (nouveau) — `presentation/shared/util/DateFormat.kt`

Formatage partagé pour ne pas réimplémenter dans chaque carte.
- `fun formatDate(iso: String): String` — « 22 juin 2026 » (tolérant : si parsing échoue, renvoie la chaîne brute, jamais d'exception).
- `fun relativeDate(iso: String, today: LocalDate): String?` — « dans 3 jours » / « aujourd'hui » / « il y a 2 jours », ou `null` si hors d'une fenêtre pertinente. **`today` est INJECTÉ** (pas d'horloge interne) → fonction pure testable. L'appelant @Composable fournit la date du jour.
- Implémentation : `kotlinx-datetime` si déjà dispo, sinon parsing ISO minimal. **À spiker en tâche 1** (vérifier la présence de la lib ; sinon parser `yyyy-MM-dd` à la main).
- **Pure et testable** (package `...util` — couvert par tests unitaires).

### 5. Apparition en fondu des listes (modif des pages)

Sur chaque grille/liste, les items apparaissent en **fondu** quand les données arrivent (au lieu de « sauter »).
- `Modifier.animateItem()` (Compose 1.8 ; successeur d'`animateItemPlacement`) sur les items des `LazyVerticalGrid`/`LazyColumn`, + un `fadeIn` léger géré par l'API d'item placement. Durée/easing via `AppMotion` si l'API le permet ; sinon valeurs par défaut discrètes (à spiker tâche 5).
- Pas de stagger marqué — sobre.

### 6. Mise en forme des cartes (données dispo uniquement)

- `SessionCard` : `date` mise en valeur via `formatDate` + indice `relativeDate` quand pertinent.
- `GroupCard` : badge de rôle (dérivé de `myRole` → « MJ »/« Joueur ») ; nombre de membres affiché **là où `members` est disponible** (détail), pas en liste pure.
- `CampaignCard` : « créée le {formatDate(createdAt)} ».
- `CharacterSheetCard` : nom + date soignés, meilleure hiérarchie (nom moins centré-perdu). **PAS** de niveau/formation/peuple (indisponibles en liste).
- Préserver intégralement : clic conditionnel, `interactiveCard` (Lot 1), icônes delete, `isActive` de GroupCard.

### 7. Câblage par écran (modif des pages de liste)

Pour chaque page de liste (campagnes, fiches, références, sessions, groupes — desktop ET android) :
- `isLoading && liste vide` → `SkeletonGrid`/`SkeletonList` (au lieu du spinner).
- `liste vide && !isLoading` → `EmptyState` avec CTA branché sur l'action de création existante (FAB).
- `données` → grille/liste avec items en fondu.
- **Aucun changement de logique métier** : les ViewModels exposent déjà `isLoading`/liste/erreur.

## Flux & cohérence

- Tous les nouveaux composants lisent les tokens (`colors/dimens/motion`) → cohérents avec les 3 thèmes et la réduction de mouvement.
- `formatDate`/`relativeDate` centralisés → un seul endroit pour le format de date.
- Le shimmer respecte `AppMotion.enabled` (désactivé = pas d'animation).

## Gestion d'erreur / dégradation

- `formatDate` tolérant : entrée non parsable → chaîne brute renvoyée, jamais d'exception.
- `relativeDate` renvoie `null` hors fenêtre pertinente (la carte n'affiche alors pas d'indice relatif).
- Skeleton/EmptyState n'ont pas d'état d'erreur propre : l'erreur réseau reste gérée par le `FormError` existant au-dessus de la liste (inchangé).
- Réduction de mouvement (`AppMotion.enabled=false`) : shimmer et fondu deviennent statiques, jamais cassés.

## Tests

- **`DateFormat`** (pur) : tests unitaires `formatDate` (date valide, date invalide → brute) et `relativeDate` (aujourd'hui/passé/futur/hors-fenêtre, `today` injecté). Package `...util` couvert par Kover.
- **Composants `@Composable`** (`SkeletonBox`, `SkeletonGrid`, `EmptyState`, cartes, pages) : non couverts par tests unitaires (cohérent avec l'exclusion Kover des packages `...component`/`...page`) → **validation runtime** (tâche finale : lancer l'app, observer skeleton/fondu/états vides sur les 3 thèmes).
- `verifyDesktop` (detekt + desktopTest + koverVerify ≥ 60 %) vert à chaque tâche.

## Contraintes globales (reprises du projet)

- **Branche :** `feat/cartes-et-listes` depuis **`feat/socle-animation`** (le Lot 2 utilise `AppMotion`, `interactiveCard` du Lot 1, non mergé sur `main`).
- `verifyDesktop` vert à chaque tâche ; commitlint strict (sujet minuscule, Conventional Commits) ; max 500 lignes/fichier ; tests en `src/desktopTest/`.
- **Invariant mouvement (étendu) :** aucune durée/courbe d'animation en dur hors `AppMotion.kt` ; le shimmer dérive ses durées de `AppMotion`.
- **Pas d'horloge murale** dans le code testable (`relativeDate` reçoit `today` injecté).
- **Ne pas toucher** : `AppPalette`, les 3 thèmes, le socle d'animation (Lot 1), la logique métier des ViewModels, le backend.

## Hors périmètre (explicite)

- Enrichir les cartes de fiche avec niveau/formation/peuple (exige le backend).
- En-têtes de page, snackbar de succès/erreur, validation progressive → Lot 3.
- Pagination / chargement infini des listes.
