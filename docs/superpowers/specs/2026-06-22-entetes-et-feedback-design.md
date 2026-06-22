# En-têtes & feedback (Lot 3) — Design

> **Statut :** spec validée en brainstorming (2026-06-22). Dernier des 3 lots d'amélioration UX/UI.
> **Lots précédents (faits) :** Lot 1 socle d'animation, Lot 2 cartes & listes.

## Objectif

Compléter l'expérience par trois apports : (1) un **feedback global** après chaque action (snackbar « Créé ✓ » / message d'erreur), aujourd'hui totalement absent ; (2) des **en-têtes de contenu** sous la top bar (titre soigné + sous-titre contextuel + action principale) ; (3) une **validation progressive** des formulaires de saisie (message d'aide en temps réel).

## Constats code (vérifiés, structurants)

- **Aucun feedback de succès n'existe** : les `onSuccess` des ViewModels font seulement `_error.value = null` + reload silencieux. L'utilisateur ne voit rien.
- **Pattern d'événement one-shot déjà présent** : `UserViewModel.sessionExpired` utilise `Channel<T>(Channel.BUFFERED)` + `receiveAsFlow()`. On suit ce pattern (pas d'invention).
- **Un bus injecté existe déjà** : `InvalidationBus` (realtime) est un singleton Koin observé globalement → modèle pour `UiMessageBus`.
- **La top bar porte déjà un titre par écran** (`MainTopBar(title=...)`) → l'en-tête de page ne répète PAS ce titre seul ; il ajoute sous-titre contextuel + action (sinon doublon).
- **6 ViewModels** ont create/delete : `CampaignListViewModel`, `CampaignDetailViewModel`, `MyCharacterSheetsViewModel`, `GroupListViewModel`, `ReferenceListViewModel`, `SessionDetailViewModel`.
- `AppTextField` a déjà un slot `errorMessage: String?` → la validation progressive enrichit l'existant.

## Composants & unités

### 1. `UiMessage` (nouveau, domaine présentation) — `presentation/shared/feedback/UiMessage.kt`

Donnée immuable d'un message UI à afficher.
```
data class UiMessage(val text: String, val tone: Tone)
enum class Tone { SUCCESS, ERROR }
// fabriques : UiMessage.success(text), UiMessage.error(text)
```
- Pur, testable. **Dépend de :** rien.

### 2. `UiMessageBus` (nouveau, singleton injecté) — `application/shared/feedback/UiMessageBus.kt` (interface) + impl

Canal d'émission/observation des messages UI, façon `InvalidationBus`.
```
interface UiMessageBus {
    val messages: Flow<UiMessage>
    fun emit(message: UiMessage)
}
```
- Impl : `Channel<UiMessage>(Channel.BUFFERED)` + `receiveAsFlow()` (pattern existant). Injecté par Koin en singleton.
- **Émetteurs :** les 6 ViewModels create/delete. **Observateur :** l'hôte de snackbar global.
- Pur (hors `@Composable`) → **testable** : un test vérifie qu'`emit` est bien reçu par `messages`.

### 3. Modification des 6 ViewModels (logique — testée)

Chaque ViewModel create/delete reçoit `UiMessageBus` en dépendance (constructeur). Dans :
- `onSuccess` → `bus.emit(UiMessage.success("<libellé contextuel>"))` (ex. « Campagne créée », « Fiche supprimée »).
- `onFailure` → `bus.emit(UiMessage.error(error.message))`. **On conserve** `_error` pour le `FormError` inline existant (double affichage acceptable : inline + snackbar ; à ajuster au cas par cas si redondant — décision : garder les deux, le snackbar est transitoire, le FormError persiste tant que l'erreur est là).
- **Tests ajoutés** : pour au moins 1-2 ViewModels représentatifs (ex. `CampaignListViewModel`), vérifier qu'une création réussie émet un `UiMessage.success` et qu'un échec émet un `UiMessage.error`. (MockK sur le bus.)

### 4. `AppSnackbar` + hôte global (nouveau, `@Composable`)

- `AppSnackbar.kt` (`presentation/shared/component/organism/`) : surface en bas d'écran, fond dérivé du `tone` (succès → teinte neutre/positive des tokens ; erreur → `danger`), texte `onX`, coins arrondis, ombre. Apparition/disparition animées via `AppMotion` (slide+fade depuis le bas, durée `durationMedium`). Auto-dismiss après un délai (constante nommée).
- **Hôte global** : un composable `UiMessageHost` placé à la racine (dans `App.kt` desktop + android, ou dans un wrapper autour de `AppNavDisplay`) qui `collect` `uiMessageBus.messages` (injecté Koin) et affiche l'`AppSnackbar` courant. Utilise un `SnackbarHostState` Material3 OU une file maison simple (1 message à la fois, le suivant remplace).
- **Dépend de :** `UiMessageBus`, `AppTheme`.

### 5. `PageHeader` (nouveau, `@Composable`) — `presentation/shared/component/organism/PageHeader.kt`

En-tête de contenu, sous la top bar.
```
@Composable fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,   // ex. AppButton "Créer"
)
```
- Row : Column(titre style display + subtitle Caption/textSecondary) à gauche, `action` à droite.
- **Dépend de :** `AppText`, `AppTheme`.
- **Câblage par écran principal** (campagnes, fiches, groupes, références) : ajouter le `PageHeader` en haut du contenu, **l'action remplace le FAB** (le FAB est retiré de ces écrans ; l'action d'en-tête = même `onClick` que l'ancien FAB). Le sous-titre contextuel utilise des données dispo (nom du groupe actif via `ActiveGroupState`, taille de la liste).
- **CTA conditionnel préservé** : sur campagnes/références (MJ-only), l'action n'est passée que si `canEdit` (sinon pas de bouton — cohérent avec l'EmptyState du Lot 2).

### 6. Validation progressive des dialogs (modif légère)

Les dialogs de création (campagne, fiche, groupe, etc.) utilisent `AppDialog` + `AppTextField`. Ajouter :
- un message d'aide via le slot `errorMessage` d'`AppTextField` quand l'entrée est invalide **et que le champ a été touché** (ex. « Le nom ne peut pas être vide ») — pas d'erreur affichée tant que l'utilisateur n'a rien tapé.
- le `confirmEnabled` du dialog reste branché sur la validité (déjà le cas).
- Logique de validation triviale (non vide après trim) — si extraite dans une petite fonction pure, la tester ; sinon inline dans le `@Composable`.

## Flux & cohérence

- Émission : ViewModel → `UiMessageBus.emit` → `messages` Flow → `UiMessageHost` → `AppSnackbar`.
- Tous les nouveaux composants lisent les tokens (couleurs/dimens/motion) → cohérents avec les 3 thèmes et la réduction de mouvement.
- `PageHeader` action = ancienne action FAB (pas de nouvelle logique).

## Gestion d'erreur / dégradation

- Snackbar : si plusieurs messages arrivent vite, le dernier remplace (file simple, 1 à la fois) — pas de pile illimitée.
- `UiMessageBus` BUFFERED : si aucun observateur (transitoire au démarrage), les messages bufferisés ne crashent pas.
- Réduction de mouvement (`AppMotion.enabled=false`) : snackbar apparaît/disparaît sans animation (instantané), jamais cassé.
- Validation : message d'aide seulement après interaction (pas d'erreur agressive sur champ vierge).

## Tests

- **`UiMessage`** (pur) : fabriques `success`/`error` produisent le bon `tone`. Couvert.
- **`UiMessageBus` impl** (pur, hors @Composable) : `emit` → reçu par `messages`. Test avec coroutines-test.
- **ViewModels modifiés** : au moins `CampaignListViewModel` (représentatif) — création réussie émet `success`, échec émet `error` (MockK sur le bus). Les tests EXISTANTS des 6 ViewModels doivent rester verts (adapter les constructeurs avec le bus mocké).
- **Composants `@Composable`** (`AppSnackbar`, `UiMessageHost`, `PageHeader`, dialogs) : non couverts par tests unitaires (exclusion Kover `...component`/`...page`) → **validation runtime** (tâche finale).
- `verifyDesktop` (detekt + desktopTest + koverVerify ≥ 60 %) vert à chaque tâche.

## Contraintes globales (reprises du projet)

- **Branche :** `feat/entetes-et-feedback` depuis **`feat/cartes-et-listes`** (Lot 3 empilé sur Lot 2).
- `verifyDesktop` vert à chaque tâche ; commitlint strict (sujet minuscule) ; max 500 lignes/fichier ; tests en `src/desktopTest/`.
- **Invariant mouvement :** l'animation du snackbar dérive ses durées d'`AppMotion` ; aucune durée/courbe en dur hors `AppMotion.kt`.
- **DI :** `UiMessageBus` injecté par Koin en singleton (façon `InvalidationBus`). Câbler dans les modules Koin desktop + android.
- **Ne pas toucher** : `AppPalette`, 3 thèmes, socle Lot 1, cartes/listes Lot 2, le backend, la logique métier des use cases (on ne modifie QUE l'émission d'événements UI dans les ViewModels).

## Hors périmètre (explicite)

- Refonte de la top bar / navigation.
- Feedback sur les actions hors create/delete (l'édition de fiche a déjà son bandeau realtime).
- File de snackbars empilés / historique de notifications.
- Pagination, en-têtes sur les pages de détail secondaires.
