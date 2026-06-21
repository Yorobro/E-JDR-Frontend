# Socle d'animation (Lot 1) — Design

> **Statut :** spec validée en brainstorming (2026-06-21). Premier des 3 lots d'amélioration UX/UI.
> **Lots suivants (hors périmètre ici) :** Lot 2 = cartes enrichies + listes animées + skeletons + états vides ; Lot 3 = en-têtes de page + snackbar + validation progressive.

## Objectif

Doter l'app (Kotlin Multiplatform / Compose, desktop + Android) d'un **socle de mouvement** réutilisable, sobre et professionnel, qui sera la fondation des lots UX suivants. Aujourd'hui l'app n'a **aucune animation custom** : toutes les interactions reposent sur le Material par défaut, les changements d'état sont instantanés, les dialogs et écrans « claquent ». Ce lot introduit un mouvement discret mais ressenti, sans toucher aux couleurs ni aux layouts.

## Personnalité du mouvement

**Sobre & pro** : rapide, discret, fonctionnel. Le mouvement guide l'œil sans se faire remarquer. Jamais fatigant à l'usage répété.
- Durées courtes : 120 ms (micro-retours), 200 ms (dialogs/écrans).
- Courbes : ease-out doux (attaque rapide, sortie douce).
- Press : léger scale (0.97) + transition d'ombre.
- Hover (desktop) : élévation +1, fond très légèrement teinté.
- Transitions d'écran : fondu rapide + léger glissement.

## Principe d'architecture central

**Source unique du mouvement** — exactement comme `AppPalette` centralise les couleurs (invariant déjà en place : zéro `Color(0x…)` hors `AppPalette`), on crée **`AppMotion`** : la seule source des durées et courbes. **Invariant : aucune durée (`tween(123)`, `400`, `100.dp` d'animation) ni courbe en dur hors `AppMotion`.** Conséquence : « tout est trop lent / trop rapide » = un seul fichier à changer.

`AppMotion` est exposé via `AppTheme.motion`, par le même mécanisme `staticCompositionLocalOf` que `AppTheme.colors`/`dimens`/`typography`.

**Factorisation des interactions** — l'animation hover/press des cartes vit dans **un seul** `Modifier.interactiveCard(...)` partagé, pas copiée dans chaque carte. Les cartes du Lot 2 réutiliseront ce modifier.

## Composants & unités

### 1. `AppMotion` (nouveau) — `presentation/shared/theme/AppMotion.kt`

Data class immuable lue via `AppTheme.motion` :

```
durationFast: Int = 120        // ms — press, hover, micro-retours
durationMedium: Int = 200      // ms — dialogs, transitions d'écran
durationSlow: Int = 300        // ms — réservé Lot 2 (apparition listes)
easeStandard: Easing           // CubicBezierEasing(0.2f, 0f, 0f, 1f) — ease-out doux
easeEmphasized: Easing         // entrées/sorties d'écran
pressScale: Float = 0.97f      // facteur d'échelle au press
```

- **Dépend de :** rien (constantes + `Easing` de Compose).
- **Utilisé par :** `interactiveCard`, `AppButton`, `AppDialog`, `AppNavDisplay`.
- **Câblage :** ajouter `LocalAppMotion` + `AppTheme.motion` dans `AppTheme.kt`, fourni par défaut dans le composable `AppTheme`.
- **Réduction de mouvement :** `AppMotion` porte un drapeau `enabled: Boolean = true`. Quand `false`, durées = 0 (transitions instantanées) — permet une désactivation globale (réglage futur / accessibilité). Le socle lit ce drapeau ; les animations dégradent en changement instantané, jamais en cassure.

### 2. `Modifier.interactiveCard()` (nouveau) — `presentation/shared/component/modifier/InteractiveCard.kt`

Modifier composable réutilisable encapsulant le retour d'interaction d'une carte :
- **hover** (pointeur, desktop) → cible d'élévation `AppDimens.elevationMd` → légèrement supérieure ; fond teinté d'un voile imperceptible (dérivé des tokens, pas de hex).
- **press** → scale `AppMotion.pressScale` + élévation abaissée.
- Valeurs animées via `animateFloatAsState` / `animateDpAsState` sur `AppMotion.durationFast` + `easeStandard`.
- Signature : retourne un `Modifier` ; prend l'`interactionSource` et expose l'élévation/scale courants à appliquer. La carte garde sa `Surface` (acquise au Lot précédent) ; le modifier pilote `graphicsLayer { scaleX/scaleY }` et fournit l'élévation à la `Surface`.
- **Dépend de :** `AppTheme.motion`, `AppTheme.dimens`.
- **Utilisé par :** les 5 cartes (`CharacterSheetCard`, `CampaignCard`, `SessionCard`, `GroupCard`, `ReferenceCard`) — et les futures cartes du Lot 2.

> Contrainte de compatibilité : préserver le **clic conditionnel** des cartes (une carte non cliquable ne doit pas réagir au press ni exposer une sémantique de bouton). Le modifier n'active le retour press que si la carte est effectivement cliquable.

### 3. `AppButton` (modifié) — `presentation/shared/component/atomic/AppButton.kt`

- **Press animé :** scale `pressScale` + ombre, sur `durationFast`. Lit `AppTheme.motion`.
- **Anti double-clic :** garde temporel — un clic est ignoré s'il survient moins de `debounceMs` (≈ 400 ms) après le précédent. Implémenté dans `AppButton` (point d'entrée unique de tous les boutons), via un `remember` de l'horodatage logique du dernier clic accepté. **Ne pas** utiliser d'horloge murale interdite ; s'appuyer sur l'état d'interaction Compose / un flag `remember` remis à `false` après recomposition différée. (Le plan précisera le mécanisme exact retenu après spike.)
- Préserver intégralement le comportement existant : variants (Primary/Secondary/Text/Danger/Ghost), `loading` (spinner), `enabled`, `leadingIcon`.

### 4. `AppDialog` (modifié) — `presentation/shared/component/organism/AppDialog.kt`

- Apparition **fade + scale** (0.95 → 1.0) sur `durationMedium`, disparition symétrique.
- Implémenté en enveloppant le contenu du dialog dans une animation d'entrée/sortie compatible avec `AlertDialog` Material3 (probablement `AnimatedVisibility` autour du contenu, ou un état d'échelle piloté à l'ouverture). Préserver `confirmEnabled`, `dismissLabel`, le comportement de confirmation/annulation.

### 5. Transitions d'écran Nav3 (modifié) — `presentation/navigation/AppNavDisplay.kt` (desktop **et** android)

- Ajouter transitions enter/exit aux destinations : **fondu rapide + léger glissement horizontal** sur `durationMedium` + `easeEmphasized`.
- **Risque & spike (obligatoire avant implémentation) :** l'API de transition de `NavDisplay` (navigation3 `1.1.1`) doit être validée sur la vraie lib — la mémoire projet note explicitement que les versions Nav3 doivent être **spikées, pas copiées de la doc**. Le plan commencera par un spike :
  - **Voie A (préférée) :** si `NavDisplay`/`SceneStrategy` expose des `enter/exit`/`transitionSpec`, les utiliser.
  - **Voie B (repli) :** sinon, envelopper le contenu de chaque destination dans un `AnimatedContent` clé par la route, ce qui produit la transition sans dépendre de l'API de la lib.
  - Le repli ne bloque aucun autre élément du lot.

## Flux & cohérence

- Tous les composants lisent `AppTheme.motion` → un changement de token se propage partout.
- Le drapeau `enabled` d'`AppMotion` désactive globalement le mouvement (durées→0) sans casser l'UI.
- Aucune couleur nouvelle ; aucune valeur hex ; aucun changement de layout. Le mouvement s'applique **sur les composants existants**.

## Gestion d'erreur / dégradation

- **Pas de mouvement = pas de cassure :** si `enabled=false` ou si une API d'animation n'est pas disponible, l'UI retombe sur un changement d'état instantané (le comportement actuel), jamais sur un écran cassé.
- **Spike Nav3 négatif :** repli `AnimatedContent` (voie B). Documenté dans le plan.
- **Clic conditionnel des cartes :** le press ne s'active que pour les cartes cliquables (préserve l'accessibilité — pas de rôle bouton sur une tuile non cliquable).

## Tests

- **`AppMotion`** : data class triviale + drapeau `enabled` ⇒ test unitaire vérifiant les valeurs par défaut et que `enabled=false` ramène les durées à 0 (si la logique de réduction est portée par une fonction pure, la tester ; sinon couverte par l'usage). Le package `presentation.shared.theme` n'est PAS exclu de Kover (seuil 60 %) → garder toute logique non-`@Composable` triviale/testée.
- **Anti double-clic** : tester la logique de garde de `AppButton` (un 2e clic dans la fenêtre est ignoré ; un clic après expiration passe) au niveau le plus pur possible — extraire la décision dans une fonction/`State` testable plutôt que de la noyer dans le `@Composable`.
- **Composants `@Composable`** (modifier, dialog, transitions) : non couverts par les tests unitaires (cohérent avec l'exclusion Kover des packages `...component`/`navigation` déjà en place) → **validation runtime** (lancer l'app desktop, observer hover/press/dialog/transition) en tâche finale.
- `verifyDesktop` (detekt + desktopTest + koverVerify ≥ 60 %) vert à chaque tâche.

## Contraintes globales (reprises du projet)

- **Branche :** `feat/socle-animation` depuis **`feat/refonte-visuelle`** (PAS `main`) — le socle dépend de `AppDimens.elevationSm/Md` et de l'infra `AppTheme` introduits par la refonte visuelle, non encore mergée sur `main`.
- `verifyDesktop` vert à chaque tâche ; commitlint strict (sujet de commit en minuscule, Conventional Commits) ; max 500 lignes/fichier (`ejdr/file-size`) ; tests en `src/desktopTest/`.
- **Invariant mouvement :** aucune durée/courbe d'animation en dur hors `AppMotion.kt`.
- **Ne pas toucher** : `AppPalette`, les 3 thèmes, les couleurs, les layouts/structure des écrans (réservés Lot 2).

## Hors périmètre (explicite)

- Enrichissement du contenu des cartes (niveau/formation/peuple/campagne…) → Lot 2.
- Skeletons de chargement, apparition en fondu des listes, `animateItemPlacement` → Lot 2.
- États vides illustrés, en-têtes de page, snackbar de succès/erreur, validation progressive → Lots 2/3.
