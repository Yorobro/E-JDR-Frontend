# Socle d'animation (Lot 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Doter l'app d'un socle de mouvement réutilisable (tokens `AppMotion`, interactions cartes/boutons, anti double-clic, dialogs animés, transitions d'écran), sobre et professionnel, sans toucher couleurs ni layouts.

**Architecture:** On ajoute `AppMotion` (source unique des durées/courbes, exposé via `AppTheme.motion`, même mécanisme `staticCompositionLocalOf` que `colors`/`dimens`). Un `Modifier.interactiveCard()` partagé factorise le retour hover/press des 5 cartes. `AppButton` gagne un press animé + anti double-clic par garde d'état (pas d'horloge murale). `AppDialog` et les transitions Nav3 deviennent animés, le Nav3 derrière un spike avec repli `AnimatedContent`.

**Tech Stack:** Kotlin Multiplatform 2.2.20, Compose Multiplatform 1.8.2, Material3, navigation3 1.1.1, JUnit5 + MockK (tests desktop), coroutines.

## Global Constraints

- **Branche :** `feat/socle-animation` depuis **`feat/refonte-visuelle`** (PAS `main` — le socle dépend de `AppDimens.elevationSm/Md` et de l'infra `AppTheme.motion`-like introduits par la refonte visuelle, non mergée). Voir Task 0.
- **Vérif :** `./gradlew.bat verifyDesktop --no-daemon` (= detekt + desktopJar/test + koverVerify ≥ 60 %) vert à la fin de chaque tâche.
- **Commitlint strict :** sujet de commit en minuscule, Conventional Commits.
- **Max 500 lignes/fichier** (`ejdr/file-size`). **Tests en `src/desktopTest/`** (JUnit5, MockK, kotlin.test, coroutines-test).
- **Invariant mouvement :** aucune durée/courbe d'animation en dur (`tween(120)`, `300`, `CubicBezierEasing(...)`) hors `AppMotion.kt`. Vérifié en Task 8.
- **Pas d'horloge murale** pour l'anti double-clic : utiliser un garde d'état + `delay` coroutine (multiplateforme, testable). `System.currentTimeMillis()` / `Date()` interdits ici.
- **Ne pas toucher** : `AppPalette`, les 3 thèmes, les couleurs, les layouts/structure des écrans.
- **Kover :** `presentation.shared.theme` n'est PAS exclu (logique non-`@Composable` triviale/testée) ; `...component`/`navigation` SONT exclus (donc dialog/modifier/nav = validation runtime, pas test unitaire).

---

## File Structure

**Créés :**
- `src/commonMain/.../presentation/shared/theme/AppMotion.kt` — data class tokens de mouvement + réduction.
- `src/commonMain/.../presentation/shared/component/modifier/InteractiveCard.kt` — `Modifier.interactiveCard()` partagé.
- `src/commonMain/.../presentation/shared/component/atomic/ClickGuard.kt` — logique pure anti double-clic (testable).
- `src/desktopTest/.../presentation/shared/theme/AppMotionTest.kt`
- `src/desktopTest/.../presentation/shared/component/atomic/ClickGuardTest.kt`

**Modifiés :**
- `src/commonMain/.../presentation/shared/theme/AppTheme.kt` — `LocalAppMotion` + `AppTheme.motion` + param défaut.
- `src/commonMain/.../presentation/shared/component/atomic/AppButton.kt` — press animé + anti double-clic.
- 5 cartes : `CharacterSheetCard.kt`, `CampaignCard.kt`, `SessionCard.kt`, `GroupCard.kt`, `ReferenceCard.kt` — appliquer `interactiveCard()`.
- `src/commonMain/.../presentation/shared/component/organism/AppDialog.kt` — apparition fade+scale.
- `src/{desktop,android}Main/.../presentation/navigation/AppNavDisplay.kt` — transitions enter/exit (après spike).

---

## Task 0 : Préparer la branche

**Files:** aucun fichier code.

- [ ] **Step 1 : Créer la branche depuis feat/refonte-visuelle**

```bash
git switch feat/refonte-visuelle
git switch -c feat/socle-animation
```

- [ ] **Step 2 : Vérifier l'état de départ vert**

Run: `./gradlew.bat verifyDesktop --no-daemon`
Expected: BUILD SUCCESSFUL.

---

## Task 1 : Tokens `AppMotion` + câblage `AppTheme.motion`

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppMotion.kt`
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppTheme.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppMotionTest.kt`

**Interfaces:**
- Produces: `data class AppMotion(...)` avec `durationFast/Medium/Slow: Int`, `easeStandard/easeEmphasized: Easing`, `pressScale: Float`, `enabled: Boolean`, et `fun durationFast(): Int`/etc. OU un accès `effectiveFast` qui renvoie 0 si `!enabled`. `AppTheme.motion: AppMotion` (@Composable @ReadOnlyComposable). Consommé par Tasks 2-7.

- [ ] **Step 1 : Écrire le test**

```kotlin
package eu.ejdr.presentation.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppMotionTest {
    @Test
    fun `valeurs par defaut sobres`() {
        val m = AppMotion()
        assertEquals(120, m.durationFast)
        assertEquals(200, m.durationMedium)
        assertEquals(0.97f, m.pressScale)
        assertTrue(m.enabled)
    }

    @Test
    fun `desactiver le mouvement ramene les durees effectives a zero`() {
        val m = AppMotion(enabled = false)
        assertEquals(0, m.effectiveDuration(m.durationFast))
        assertEquals(0, m.effectiveDuration(m.durationMedium))
    }

    @Test
    fun `mouvement actif conserve les durees`() {
        val m = AppMotion(enabled = true)
        assertEquals(120, m.effectiveDuration(m.durationFast))
    }
}
```

- [ ] **Step 2 : Lancer, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.theme.AppMotionTest" --no-daemon`
Expected: FAIL — `AppMotion` non défini.

- [ ] **Step 3 : Créer `AppMotion.kt`**

```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Tokens de mouvement du design system — **source unique des durées et courbes**.
 *
 * Comme [AppPalette] pour les couleurs : aucune durée/courbe d'animation n'est définie
 * ailleurs. Lu via [AppTheme.motion]. Pour ralentir/accélérer toute l'app, modifier ici.
 *
 * Personnalité : sobre & pro — rapide, ease-out doux, discret mais ressenti.
 *
 * @property enabled Quand `false`, [effectiveDuration] renvoie 0 → transitions instantanées
 * (réduction de mouvement / accessibilité), sans casser l'UI.
 */
data class AppMotion(
    val durationFast: Int = 120,
    val durationMedium: Int = 200,
    val durationSlow: Int = 300,
    val easeStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val easeEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val pressScale: Float = 0.97f,
    val enabled: Boolean = true,
) {
    /** Durée réelle à utiliser : 0 si le mouvement est désactivé. */
    fun effectiveDuration(base: Int): Int = if (enabled) base else 0
}
```

- [ ] **Step 4 : Câbler dans `AppTheme.kt`**

Dans `AppTheme.kt`, ajouter à côté des autres `LocalXxx` :
```kotlin
private val LocalAppMotion = staticCompositionLocalOf { AppMotion() }
```
Ajouter dans l'objet `AppTheme` :
```kotlin
    val motion: AppMotion
        @Composable @ReadOnlyComposable get() = LocalAppMotion.current
```
Ajouter le paramètre au composable `AppTheme(...)` : `motion: AppMotion = AppMotion(),` et le fournir dans le `CompositionLocalProvider` : `LocalAppMotion provides motion,`.

- [ ] **Step 5 : Lancer, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.theme.AppMotionTest" --no-daemon`
Expected: PASS (3 tests).

- [ ] **Step 6 : Vérif complète + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppMotion.kt src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppTheme.kt src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppMotionTest.kt
git commit -m "feat: tokens de mouvement appmotion (source unique des durees et courbes)"
```

---

## Task 2 : Logique anti double-clic `ClickGuard` (pure, testable)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/ClickGuard.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/component/atomic/ClickGuardTest.kt`

**Interfaces:**
- Produces: une logique pure `class ClickGuard(private val windowMs: Long = 400L)` avec `fun tryClick(nowMs: Long): Boolean` — renvoie `true` si le clic est accepté (et arme la fenêtre), `false` s'il tombe dans la fenêtre du dernier clic accepté. `nowMs` est INJECTÉ (pas d'horloge interne) → testable sans temps réel. Consommé par Task 3.

> Rationale : on isole la DÉCISION (pure, testée ici) du déclenchement temporel (@Composable, Task 3). La pureté garantit la couverture Kover du package `...atomic` non exclu pour cette classe.

- [ ] **Step 1 : Écrire le test**

```kotlin
package eu.ejdr.presentation.shared.component.atomic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClickGuardTest {
    @Test
    fun `premier clic accepte`() {
        assertTrue(ClickGuard(windowMs = 400L).tryClick(nowMs = 1000L))
    }

    @Test
    fun `clic rapproche rejete`() {
        val g = ClickGuard(windowMs = 400L)
        assertTrue(g.tryClick(1000L))
        assertFalse(g.tryClick(1200L)) // 200ms < 400ms
    }

    @Test
    fun `clic apres la fenetre accepte`() {
        val g = ClickGuard(windowMs = 400L)
        assertTrue(g.tryClick(1000L))
        assertTrue(g.tryClick(1500L)) // 500ms > 400ms
    }
}
```

- [ ] **Step 2 : Lancer, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.component.atomic.ClickGuardTest" --no-daemon`
Expected: FAIL — `ClickGuard` non défini.

- [ ] **Step 3 : Créer `ClickGuard.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.atomic

/**
 * Garde anti double-clic, pure et testable.
 *
 * Décide si un clic doit être accepté ou ignoré (rapproché d'un clic déjà accepté).
 * L'horodatage est INJECTÉ via [tryClick] : aucune horloge interne, donc testable sans
 * temps réel et multiplateforme. L'usage @Composable (fenêtre temporelle réelle) est câblé
 * dans AppButton.
 *
 * @param windowMs Fenêtre de garde en millisecondes (deux clics plus rapprochés que ça →
 * le second est ignoré).
 */
class ClickGuard(private val windowMs: Long = 400L) {
    private var lastAcceptedMs: Long? = null

    /** @return true si le clic est accepté (et arme la fenêtre), false s'il est ignoré. */
    fun tryClick(nowMs: Long): Boolean {
        val last = lastAcceptedMs
        if (last != null && nowMs - last < windowMs) return false
        lastAcceptedMs = nowMs
        return true
    }
}
```

- [ ] **Step 4 : Lancer, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.component.atomic.ClickGuardTest" --no-daemon`
Expected: PASS (3 tests).

- [ ] **Step 5 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/ClickGuard.kt src/desktopTest/kotlin/eu/ejdr/presentation/shared/component/atomic/ClickGuardTest.kt
git commit -m "feat: garde anti double-clic pure et testable (clickguard)"
```

---

## Task 3 : `AppButton` — press animé + anti double-clic

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppButton.kt`

**Interfaces:**
- Consumes: `AppTheme.motion` (Task 1), `ClickGuard` (Task 2).

- [ ] **Step 1 : Implémenter**

Dans `AppButton`, envelopper l'`onClick` exposé et ajouter le scale animé. Modifications :
1. Ajouter un `InteractionSource` + `collectIsPressedAsState()` ; calculer `val scale by animateFloatAsState(targetValue = if (pressed && AppTheme.motion.enabled) AppTheme.motion.pressScale else 1f, animationSpec = tween(AppTheme.motion.effectiveDuration(AppTheme.motion.durationFast), easing = AppTheme.motion.easeStandard))`.
2. Appliquer `Modifier.graphicsLayer { scaleX = scale; scaleY = scale }` au `modifier` passé aux `Button/OutlinedButton/...`.
3. Anti double-clic : `val guard = remember { ClickGuard() }` ; remplacer l'appel direct `onClick` par un wrapper `val guardedClick: () -> Unit = { if (guard.tryClick(nowMillis())) onClick() }`. Pour `nowMillis()` SANS horloge murale : utiliser `withFrameMillis` n'est pas synchrone ; à la place implémenter le debounce par état+coroutine : `var clickable by remember { mutableStateOf(true) } ; val scope = rememberCoroutineScope()` et `guardedClick = { if (clickable) { clickable = false; onClick(); scope.launch { delay(400); clickable = true } } }`. (On garde `ClickGuard` testé comme spéc de référence de la règle ; l'implémentation @Composable utilise l'équivalent état+delay, multiplateforme et sans horloge.)
4. Passer `interactionSource` aux composants `Button/OutlinedButton/TextButton` pour que `pressed` reflète l'état réel.
5. Préserver : variants, `loading` (pendant loading le bouton est déjà `enabled=false` donc pas de double-clic possible), `enabled`, `leadingIcon`.

imports à ajouter : `androidx.compose.foundation.interaction.MutableInteractionSource`, `collectIsPressedAsState`, `androidx.compose.animation.core.animateFloatAsState`, `androidx.compose.animation.core.tween`, `androidx.compose.ui.graphics.graphicsLayer`, `androidx.compose.runtime.*` (mutableStateOf/remember/getValue/setValue), `rememberCoroutineScope`, `kotlinx.coroutines.delay`, `kotlinx.coroutines.launch`.

- [ ] **Step 2 : Vérif (runtime-only, pas de test unitaire @Composable)**

Run: `./gradlew.bat verifyDesktop --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppButton.kt
git commit -m "feat: bouton avec retour press anime et anti double-clic"
```

---

## Task 4 : `Modifier.interactiveCard()` partagé

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/modifier/InteractiveCard.kt`

**Interfaces:**
- Consumes: `AppTheme.motion`, `AppTheme.dimens`.
- Produces: `@Composable fun Modifier.interactiveCard(interactionSource: MutableInteractionSource, enabled: Boolean = true): Modifier` qui anime un scale au press (et hover sur desktop) ; ET `@Composable fun interactiveCardElevation(interactionSource, enabled, base: Dp): Dp` renvoyant l'élévation animée à passer à la `Surface`. Consommé par Task 5.

- [ ] **Step 1 : Créer `InteractiveCard.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.modifier

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Retour d'interaction partagé des cartes : scale au press, élévation au hover/press.
 *
 * Source unique de l'animation des cartes (pas de copie dans chaque carte). Lit les tokens
 * [AppTheme.motion]/[AppTheme.dimens]. Le press n'est animé que si [enabled] (carte cliquable)
 * — préserve l'accessibilité des tuiles non cliquables.
 */
@Composable
fun Modifier.interactiveCard(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val motion = AppTheme.motion
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed && motion.enabled) motion.pressScale else 1f,
        animationSpec = tween(motion.effectiveDuration(motion.durationFast), easing = motion.easeStandard),
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Élévation animée à appliquer à la Surface de la carte (hover/press → variation douce). */
@Composable
fun interactiveCardElevation(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    base: Dp,
): Dp {
    val motion = AppTheme.motion
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val target: Dp = when {
        !enabled -> base
        pressed -> AppTheme.dimens.elevationSm
        hovered -> base + AppTheme.dimens.elevationSm
        else -> base
    }
    val elevation by animateDpAsState(
        targetValue = target,
        animationSpec = tween(motion.effectiveDuration(motion.durationFast), easing = motion.easeStandard),
    )
    return elevation
}
```

- [ ] **Step 2 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/modifier/InteractiveCard.kt
git commit -m "feat: modifier interactivecard partage (scale press + elevation hover)"
```

---

## Task 5 : Appliquer `interactiveCard()` aux 5 cartes

**Files:**
- Modify: `CharacterSheetCard.kt`, `CampaignCard.kt`, `SessionCard.kt`, `GroupCard.kt`, `ReferenceCard.kt` (tous sous `presentation/features/*/component/`).

**Interfaces:**
- Consumes: `Modifier.interactiveCard`, `interactiveCardElevation` (Task 4).

- [ ] **Step 1 : Pour CHAQUE carte, brancher l'interaction**

Pour chaque carte qui utilise déjà une `Surface` (acquise au lot précédent) :
1. Ajouter `val interactionSource = remember { MutableInteractionSource() }`.
2. Déterminer `enabled` = la carte est-elle cliquable (ex. `onClick != null` pour CharacterSheetCard ; pour les cartes toujours cliquables, `true`).
3. Passer `interactionSource = interactionSource` à la `Surface` (et au `Modifier.clickable` s'il est explicite).
4. Remplacer l'élévation statique de la `Surface` par `shadowElevation = interactiveCardElevation(interactionSource, enabled, base = AppTheme.dimens.elevationMd)`.
5. Ajouter `.interactiveCard(interactionSource, enabled)` au `modifier` de la `Surface`.
6. **Préserver le clic conditionnel** : ne pas rendre cliquable une carte qui ne l'était pas ; `enabled=false` neutralise le press/scale.

Localiser les 5 fichiers : `grep -rl "fun .*Card(" src/commonMain/kotlin/eu/ejdr/presentation/features/*/component/`.

- [ ] **Step 2 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/features
git commit -m "feat: appliquer le retour d'interaction aux cartes (hover/press)"
```

---

## Task 6 : `AppDialog` — apparition fade + scale

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/organism/AppDialog.kt`

**Interfaces:**
- Consumes: `AppTheme.motion`.

- [ ] **Step 1 : Animer l'apparition**

`AlertDialog` Material3 gère son propre rendu ; pour l'animer proprement sans réécrire le dialog, animer le **contenu** via un état de progression d'ouverture :
1. `var visible by remember { mutableStateOf(false) }` ; `LaunchedEffect(Unit) { visible = true }`.
2. `val scale by animateFloatAsState(if (visible) 1f else 0.95f, tween(AppTheme.motion.effectiveDuration(AppTheme.motion.durationMedium), easing = AppTheme.motion.easeStandard))` et `val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(...même durée...))`.
3. Appliquer `Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }` au `modifier` passé à l'`AlertDialog`.
4. Préserver title/content/confirmButton/dismissButton/containerColor/shape/confirmEnabled.

(Si `graphicsLayer` sur le modifier de l'AlertDialog ne produit pas l'effet attendu sur la fenêtre Material — à constater au runtime —, repli documenté : envelopper `content` dans le scale/alpha plutôt que l'AlertDialog entier. Le build reste vert dans les deux cas.)

- [ ] **Step 2 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/organism/AppDialog.kt
git commit -m "feat: apparition animee des dialogs (fade + scale)"
```

---

## Task 7 : Spike + transitions d'écran Nav3

**Files:**
- Modify: `src/desktopMain/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt`
- Modify: `src/androidMain/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt`

**Interfaces:**
- Consumes: `AppTheme.motion`.

- [ ] **Step 1 : SPIKE — l'API NavDisplay accepte-t-elle des transitions ?**

Inspecter la signature réelle de `androidx.navigation3.ui.NavDisplay` dans la version 1.1.1 :
```bash
grep -rn "fun NavDisplay" ~/.gradle/caches 2>/dev/null | head
```
ou via l'IDE/sources. Déterminer si `NavDisplay(...)` expose des paramètres `transitionSpec` / `popTransitionSpec` / `contentTransform`.
- **Voie A (préférée) :** s'ils existent → fournir un `transitionSpec` = `fadeIn(tween(durationMedium)) + slideInHorizontally()` tog:ether avec `fadeOut(...) + slideOutHorizontally()`, durées via `AppTheme.motion`.
- **Voie B (repli) :** s'ils n'existent pas / instables → envelopper le contenu de chaque destination dans un `AnimatedContent` clé par la route courante (top du back-stack), avec le même `transitionSpec` fade+slide. Documenter le choix dans le rapport.

- [ ] **Step 2 : Implémenter la voie retenue (desktop puis android, identiques)**

Appliquer la transition fade+slide (durée `motion.effectiveDuration(motion.durationMedium)`, `easeEmphasized`) au `NavDisplay`. Respecter `motion.enabled` (si désactivé → pas de transition / durée 0).

- [ ] **Step 3 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
Run aussi (Android compile) : `./gradlew.bat compileDebugKotlinAndroid --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/desktopMain/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt src/androidMain/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt
git commit -m "feat: transitions d'ecran fluides (fade + glissement)"
```

---

## Task 8 : Invariant mouvement + validation runtime

**Files:** ajustements ciblés.

- [ ] **Step 1 : Vérifier l'invariant « durées centralisées »**

Run: `grep -rnE "tween\(|CubicBezierEasing|spring\(" src/commonMain/kotlin src/desktopMain/kotlin src/androidMain/kotlin --include=*.kt | grep -v "AppMotion.kt"`
Attendu : les seuls `tween(...)` restants doivent prendre leur durée de `AppTheme.motion.*` (pas de littéral numérique). Aucune `CubicBezierEasing` hors `AppMotion.kt`. Sinon, déplacer la valeur dans `AppMotion`.

- [ ] **Step 2 : Lancer l'app desktop et valider chaque interaction**

Run: `./gradlew.bat run --no-daemon`
Vérifier : press des boutons (scale), hover/press des cartes (élévation+scale), anti double-clic (double-clic rapide n'ouvre qu'une fois), apparition des dialogs (fade+scale), transitions entre écrans (fade+slide). Tester sur les 3 thèmes.

- [ ] **Step 3 : Vérif finale**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL, koverVerify ≥ 60 %.

- [ ] **Step 4 : Commit final**

```bash
git add -A
git commit -m "fix: ajustements du socle d'animation apres validation runtime"
```

---

## Self-Review (rempli)

**Couverture spec :**
- `AppMotion` source unique + `AppTheme.motion` + réduction → Task 1. ✅
- Interactions cartes (modifier partagé) → Tasks 4, 5. ✅
- `AppButton` press + anti double-clic → Tasks 2, 3. ✅
- Dialogs animés → Task 6. ✅
- Transitions Nav3 + spike/repli → Task 7. ✅
- Invariant mouvement + runtime → Task 8. ✅
- Clic conditionnel préservé → Tasks 4 (param `enabled`), 5 (Step 1.6). ✅
- Réduction de mouvement (`enabled`) → Task 1 + lue partout. ✅

**Placeholders :** aucun TBD. Les 2 points « à constater au runtime » (graphicsLayer sur AlertDialog, API NavDisplay) ont chacun un repli explicite gardant le build vert — ce sont de vraies incertitudes de lib, pas des trous.

**Cohérence des types :** `AppMotion` (champs + `effectiveDuration`) cohérent entre Task 1 et ses consommateurs ; `ClickGuard.tryClick(nowMs): Boolean` cohérent Task 2↔3 ; `interactiveCard(interactionSource, enabled)` + `interactiveCardElevation(...)` cohérents Task 4↔5.

**Risque connu :** Task 3 — la décision finale entre `ClickGuard` (pur, testé) et l'équivalent état+`delay` dans le @Composable : le plan teste la RÈGLE via ClickGuard et implémente l'effet via état+delay (multiplateforme, sans horloge). Les deux encodent la même fenêtre 400ms. Si un reviewer juge la double-représentation redondante, c'est un choix assumé (testabilité de la règle) à acter en revue, pas un bug.
