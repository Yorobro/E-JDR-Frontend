# Refonte graphique native v2 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Retirer complètement Material 3 du front E-JDR et redessiner toute l'UI sur `compose.foundation`, avec un rendu moderne/pro, la charte (3 thèmes) conservée, et un traitement « riche » sur 4 écrans vitrine.

**Architecture:** On introduit un socle de primitives maison (`presentation/shared/component/base/`) qui réimplémente sur `foundation` ce que Material fournissait (surface, champ de texte, dialog, dropdown, bottom bar, feedback tactile). On enrichit le thème (tokens ornement/gradient/ombres + enum `AppTreatment`). Puis on réimplémente les composants partagés sur ce socle et on migre les 28 fichiers utilisant material3, écran par écran. Migration progressive : l'app compile et `verifyDesktop` reste vert à chaque tâche.

**Tech Stack:** Kotlin Multiplatform (2.2.20), Compose Multiplatform (1.8.2), `compose.foundation` + `compose.ui` (plus de `compose.material3`), Nav3, JUnit 5 / MockK (desktopTest).

## Global Constraints

- **Zéro `androidx.compose.material3`** dans `src/` à la fin (vérifié par grep). `compose.material3` retiré de `build.gradle.kts`.
- **Zéro `androidx.compose.material.icons`** dans `src/` à la fin. `compose.materialIconsExtended` retiré de `build.gradle.kts`. Toutes les icônes viennent du jeu maison `AppIcons`.
- **Aucune couleur/dimension en dur** hors de `presentation/shared/theme/` : aucun `Color(0x…)` ni `.dp` littéral cosmétique dans les composants (lire `AppTheme.colors/dimens`). Le `LocalAppColors.error("…")` reste.
- **Charte inchangée** : les valeurs hex des 3 palettes (Parchemin/Taupe/Grimoire) dans `AppPalette.kt` ne changent PAS. On n'ajoute que des rôles dérivés.
- **API publique des composants conservée** quand possible (mêmes noms de fonctions et signatures que l'inventaire), pour ne pas casser les écrans appelants.
- **Chaque tâche se termine par `./gradlew verifyDesktop` VERT** (detekt + desktopJar + koverVerify) et un commit atomique (Conventional Commits).
- **Cibles** : desktop ET android doivent compiler. Commande de contrôle Android à la fin : `./gradlew compileDebugKotlinAndroid` (ou `assembleDebug`).
- **Traitement riche** = écrans Splash, Auth, Accueil/Profil, Fiche de perso (détail). **Sobre** partout ailleurs.
- Branche : `feat/refonte-graphique-v2` (déjà créée, spec commitée `ab9cd80`).

---

## File Structure (vue d'ensemble)

Nouveau paquet **socle** `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/` :
- `AppSurface.kt` — `Box` stylé (remplace `material3.Surface`)
- `AppInteractionSurface.kt` — feedback press/hover maison (remplace ripple/indication material)
- `AppTextFieldCore.kt` — champ outlined sur `BasicTextField` (remplace `OutlinedTextField`)
- `AppDialogCore.kt` — modale sur `Popup` (remplace `AlertDialog`)
- `AppDropdownCore.kt` — menu sur `Popup` (remplace `ExposedDropdownMenuBox`)
- `AppProgress.kt` — spinner/barre maison (remplace `CircularProgressIndicator`/`LinearProgressIndicator`)
- `AppIconButton.kt` — bouton-icône maison (remplace `material3.IconButton`)

Thème étendu `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/` :
- `AppColors.kt` — +champs `accentGradientTop/Bottom`, `ornament`, `hairline`
- `AppElevation.kt` — **nouveau** tokens d'ombre
- `AppTreatment.kt` — **nouveau** enum Rich/Plain + `LocalAppTreatment` + `ProvideTreatment`
- `AppTheme.kt` — réécrit sans `MaterialTheme` (fournit `LocalContentColor` maison)

Icônes `src/commonMain/kotlin/eu/ejdr/presentation/shared/icons/` :
- `AppIcons.kt` — **nouveau** 23 `ImageVector` maison

Composants réhabillés (mêmes chemins qu'aujourd'hui, réimplémentés) : `atomic/AppText,AppButton,AppTextField,AppPasswordField,AppCheckbox,AppDropdown,AppIcon,AppFab,AppDivider`, `organism/AppCard,AppTopBar,AppDialog`, plus les écrans/pages listés dans l'inventaire.

Le nom `AppIcon` (rendu d'un `ImageVector`) est distinct de `AppIcons` (le catalogue de vecteurs).

---

## Task 1: Retirer `MaterialTheme` du thème — `AppTheme` 100 % maison

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppTheme.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/LocalContentColor.kt`

**Interfaces:**
- Consumes: `AppColors` (existant), `colorsFor`, `appTypography`, `AppDimens`, `AppMotion` (existants).
- Produces: `object AppTheme { colors; typography; dimens; motion }` (inchangé) ; **nouveau** `val LocalContentColor: ProvidableCompositionLocal<Color>` dans le paquet `theme` (remplace `material3.LocalContentColor`) ; `@Composable fun AppTheme(colors, typography, dimens, motion, content)` (signature inchangée) mais **sans** `MaterialTheme`.

- [ ] **Step 1: Créer notre propre `LocalContentColor`**

Créer `theme/LocalContentColor.kt` :

```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Couleur de contenu par défaut (texte/icônes), fournie par [AppTheme].
 *
 * Remplace `androidx.compose.material3.LocalContentColor` : les atomes qui n'ont pas
 * de couleur explicite (ex. [eu.ejdr.presentation.shared.component.atomic.AppIcon])
 * héritent de cette valeur, mise à `AppColors.text` par [AppTheme].
 */
val LocalContentColor = compositionLocalOf { Color.Black }
```

- [ ] **Step 2: Réécrire `AppTheme.kt` sans Material**

Remplacer tout le contenu de `theme/AppTheme.kt` par :

```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("AppColors non fourni : encapsulez l'UI dans AppTheme { }")
}
private val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
private val LocalAppDimens = staticCompositionLocalOf { AppDimens() }
private val LocalAppMotion = staticCompositionLocalOf { AppMotion() }

/**
 * Point d'accès unique au design system depuis les composables.
 *
 * Expose les jetons de design (couleurs, typographie, dimensions, motion) fournis par le
 * composable racine [AppTheme]. Les composants lisent toujours ces valeurs plutôt que des
 * constantes en dur, ce qui centralise l'apparence. Ne dépend d'aucun framework externe.
 */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val typography: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current
    val dimens: AppDimens
        @Composable @ReadOnlyComposable get() = LocalAppDimens.current
    val motion: AppMotion
        @Composable @ReadOnlyComposable get() = LocalAppMotion.current
}

/**
 * Fournit le design system à l'arbre de composables (100 % maison, sans Material).
 *
 * @param colors Palette à utiliser (par défaut le thème de `ThemeVariant.DEFAULT`).
 * @param typography Typographie à utiliser.
 * @param dimens Dimensions à utiliser.
 * @param motion Jetons d'animation à utiliser.
 * @param content Contenu de l'application.
 */
@Composable
fun AppTheme(
    colors: AppColors = colorsFor(eu.ejdr.domain.features.settings.entities.ThemeVariant.DEFAULT),
    typography: AppTypography = appTypography(),
    dimens: AppDimens = AppDimens(),
    motion: AppMotion = AppMotion(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppDimens provides dimens,
        LocalAppMotion provides motion,
        LocalContentColor provides colors.text,
        content = content,
    )
}
```

- [ ] **Step 3: Retirer `isDark` s'il n'est plus lu ? Non — le garder**

`AppColors.isDark` reste (utile plus tard pour barres système Android). Ne rien changer dans `AppColors.kt` à cette étape.

- [ ] **Step 4: Compiler**

Run: `./gradlew compileDesktopKotlin 2>&1 | tail -20`
Expected: échoue UNIQUEMENT sur `AppIcon.kt` (import `material3.LocalContentColor`) — c'est réglé Task 2. Si d'autres erreurs `AppTheme`, corriger. (À ce stade la compilation complète n'est pas encore verte ; c'est attendu car `AppIcon` référence l'ancien import.)

- [ ] **Step 5: Corriger l'import dans `AppIcon.kt` immédiatement (sinon rien ne compile)**

Dans `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppIcon.kt`, remplacer l'import :
`import androidx.compose.material3.LocalContentColor` → `import eu.ejdr.presentation.shared.theme.LocalContentColor`
(le reste du fichier utilise `Icon` de material3 : on le laisse pour l'instant, il sera réécrit Task 5. Cette étape ne règle QUE le `LocalContentColor`.)

> Note : `AppIcon.kt` importe encore `material3.Icon` → la compilation reste rouge tant que material3 est là. C'est acceptable dans cette tâche de fondation SI on ne peut pas isoler. **Pour garder l'app verte**, cette Task 1 est fusionnée en pratique avec Task 5 lors de l'exécution si le compilateur l'exige. Sinon, garder `material3.Icon` importé (material3 encore présent dans le build à ce stade) et ne migrer que `LocalContentColor`. Vérifier :

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL (material3 encore dans le build, seul `LocalContentColor` est désormais maison).

- [ ] **Step 6: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/
git commit -m "refactor(theme): AppTheme sans MaterialTheme + LocalContentColor maison"
```

---

## Task 2: Tokens de thème étendus — ornement, gradient, ombres, traitement

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppColors.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppElevation.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppTreatment.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppColorsTreatmentTest.kt`

**Interfaces:**
- Produces:
  - `AppColors` gagne 4 champs : `accentGradientTop: Color`, `accentGradientBottom: Color`, `ornament: Color`, `hairline: Color`.
  - `enum class AppTreatment { Rich, Plain }`, `val LocalAppTreatment: ProvidableCompositionLocal<AppTreatment>` (défaut `Plain`), `@Composable fun ProvideTreatment(treatment: AppTreatment, content: @Composable () -> Unit)`, et accès `AppTheme.treatment` (getter composable) — OU fonction `@Composable fun currentTreatment(): AppTreatment`. Ce plan utilise `AppTheme.treatment`.
  - `data class AppElevation(...)` avec ombres `sm/md/lg` (offsetY + blur + color).

- [ ] **Step 1: Écrire le test des nouveaux rôles de couleur**

Créer `src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppColorsTreatmentTest.kt` :

```kotlin
package eu.ejdr.presentation.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppColorsTreatmentTest {
    @Test
    fun `grimoire expose un ornement laiton et un gradient d'accent`() {
        val c = grimoireColors()
        // l'ornement doré du grimoire = brass
        assertEquals(GrimoirePalette.brass, c.ornament)
        // le haut du gradient est plus clair que le bas (relief)
        assertEquals(GrimoirePalette.brassHi, c.accentGradientTop)
        assertEquals(GrimoirePalette.brass, c.accentGradientBottom)
    }

    @Test
    fun `les trois themes fournissent tous les nouveaux roles`() {
        listOf(parchmentColors(), taupeColors(), grimoireColors()).forEach { c ->
            assertTrue(c.ornament.alpha > 0f)
            assertTrue(c.hairline.alpha >= 0f)
            assertTrue(c.accentGradientTop.alpha > 0f)
            assertTrue(c.accentGradientBottom.alpha > 0f)
        }
    }
}
```

- [ ] **Step 2: Lancer le test — échoue (champs inexistants)**

Run: `./gradlew desktopTest --tests "*AppColorsTreatmentTest*" 2>&1 | tail -15`
Expected: échec de COMPILATION du test (`ornament` / `accentGradientTop` non résolus).

- [ ] **Step 3: Ajouter les 4 champs à `AppColors` + les câbler dans les 3 builders**

Dans `AppColors.kt`, ajouter dans la `data class AppColors` (après `onDanger`, avant `isDark`) :

```kotlin
    val accentGradientTop: Color,
    val accentGradientBottom: Color,
    val ornament: Color,
    val hairline: Color,
```

Ajouter les KDoc `@property` correspondants (une ligne chacun). Puis compléter chaque builder :

Dans `parchmentColors()` (avant `isDark = false`) :
```kotlin
    accentGradientTop = ParchmentPalette.boleHi,
    accentGradientBottom = ParchmentPalette.bole,
    ornament = ParchmentPalette.gold,
    hairline = ParchmentPalette.oak,
```
Dans `taupeColors()` :
```kotlin
    accentGradientTop = TaupePalette.taupeHi,
    accentGradientBottom = TaupePalette.taupe,
    ornament = TaupePalette.taupe,
    hairline = TaupePalette.border,
```
Dans `grimoireColors()` :
```kotlin
    accentGradientTop = GrimoirePalette.brassHi,
    accentGradientBottom = GrimoirePalette.brass,
    ornament = GrimoirePalette.brass,
    hairline = GrimoirePalette.border,
```

- [ ] **Step 4: Lancer le test — passe**

Run: `./gradlew desktopTest --tests "*AppColorsTreatmentTest*" 2>&1 | tail -15`
Expected: PASS.

- [ ] **Step 5: Créer `AppElevation.kt`**

```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Jetons d'ombre maison (Material ne fournit plus l'élévation).
 *
 * Chaque niveau décrit un décalage vertical, un rayon de flou et une couleur d'ombre.
 * Consommés par `AppSurface` pour peindre une ombre douce derrière les cartes.
 *
 * @property color Couleur de base de l'ombre (noir semi-transparent).
 * @property offsetSm/md/lg Décalage vertical par niveau.
 * @property blurSm/md/lg Rayon de flou par niveau.
 */
data class AppElevation(
    val color: Color = Color(0x33000000),
    val offsetSm: Dp = 1.dp,
    val offsetMd: Dp = 3.dp,
    val offsetLg: Dp = 8.dp,
    val blurSm: Dp = 3.dp,
    val blurMd: Dp = 10.dp,
    val blurLg: Dp = 24.dp,
)
```

Ajouter le local + getter dans `AppTheme.kt` : déclarer `private val LocalAppElevation = staticCompositionLocalOf { AppElevation() }`, l'exposer via `AppTheme.elevation` (getter composable comme les autres), l'ajouter en paramètre `elevation: AppElevation = AppElevation()` de `fun AppTheme(...)` et le fournir dans `CompositionLocalProvider`.

- [ ] **Step 6: Créer `AppTreatment.kt`**

```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Intensité de traitement visuel d'une zone de l'UI.
 *
 * [Rich] : ornements dorés, filets, reliefs (écrans vitrine : Splash, Auth, Accueil, Fiche).
 * [Plain] : sobre soigné, sans ornement (listes, référentiels, réglages, dialogues).
 * Les composants lisent `AppTheme.treatment` pour adapter leur rendu sans dupliquer d'écran.
 */
enum class AppTreatment { Rich, Plain }

val LocalAppTreatment = staticCompositionLocalOf { AppTreatment.Plain }

/** Applique un [AppTreatment] à un sous-arbre (les écrans vitrine posent [AppTreatment.Rich]). */
@Composable
fun ProvideTreatment(treatment: AppTreatment, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppTreatment provides treatment,
        content = content,
    )
}
```

Ajouter dans `object AppTheme` (dans `AppTheme.kt`) le getter :
```kotlin
    val treatment: AppTreatment
        @Composable @ReadOnlyComposable get() = LocalAppTreatment.current
```

- [ ] **Step 7: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/ src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppColorsTreatmentTest.kt
git commit -m "feat(theme): tokens ornement/gradient/elevation + AppTreatment"
```

---

## Task 3: Socle `base/` — feedback tactile + `AppSurface`

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/AppInteractionSurface.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/AppSurface.kt`

**Interfaces:**
- Consumes: `AppTheme.colors/dimens/motion/elevation`.
- Produces:
  - `@Composable fun Modifier.appPressFeedback(interactionSource: MutableInteractionSource, enabled: Boolean = true): Modifier` — scale au press (remplace le ripple par un retour scale maison, cohérent avec `interactiveCard` existant).
  - `@Composable fun AppSurface(modifier: Modifier = Modifier, shape: Shape = RectangleShape, color: Color = AppTheme.colors.surface, contentColor: Color = AppTheme.colors.text, border: BorderStroke? = null, elevation: Dp = 0.dp, onClick: (() -> Unit)? = null, interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit)` — remplace `material3.Surface`.

- [ ] **Step 1: Créer `AppInteractionSurface.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.base

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Retour tactile maison (remplace le ripple Material) : léger scale au press.
 *
 * @param interactionSource Source d'interaction partagée avec le `clickable`.
 * @param enabled Si faux, aucun effet.
 */
@Composable
fun Modifier.appPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = AppTheme.motion
    val target = if (enabled && pressed) motion.pressScale else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(motion.effectiveDuration(motion.durationFast), easing = motion.easeStandard),
        label = "appPressFeedback",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}
```

- [ ] **Step 2: Créer `AppSurface.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.base

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shadow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.LocalContentColor

/**
 * Surface stylée maison — remplace `androidx.compose.material3.Surface`.
 *
 * Peint un fond, une forme, une bordure optionnelle et une ombre douce, et propage
 * [contentColor] via [LocalContentColor]. Cliquable si [onClick] est fourni (feedback scale).
 */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.surface,
    contentColor: Color = AppTheme.colors.text,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    var m = modifier
    if (elevation > 0.dp) {
        m = m.shadow(elevation = elevation, shape = shape, clip = false)
    }
    m = m.clip(shape).background(color = color, shape = shape)
    if (border != null) m = m.border(border, shape)
    if (onClick != null) {
        m = m.appPressFeedback(source).clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
        )
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        androidx.compose.foundation.layout.Box(m) { content() }
    }
}
```

- [ ] **Step 3: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL (nouveaux fichiers, aucun usage encore).

- [ ] **Step 4: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/
git commit -m "feat(component): socle base — AppSurface + feedback tactile maison"
```

---

## Task 4: Socle `base/` — champ de texte sur `BasicTextField`

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/AppTextFieldCore.kt`

**Interfaces:**
- Consumes: `AppSurface` (non — utilise `Box`/`border` direct), `AppTheme.*`, `AppText`/`AppTextStyle` (existants).
- Produces: `@Composable fun AppTextFieldCore(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String? = null, enabled: Boolean = true, isError: Boolean = false, singleLine: Boolean = true, leadingContent: (@Composable () -> Unit)? = null, trailingContent: (@Composable () -> Unit)? = null, visualTransformation: VisualTransformation = VisualTransformation.None, keyboardOptions: KeyboardOptions = KeyboardOptions.Default)` — champ outlined complet, sans label flottant (le label est géré par `LabeledField`/`AppTextField` au-dessus).

- [ ] **Step 1: Créer `AppTextFieldCore.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.base

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Champ de saisie « outlined » maison, bâti sur [BasicTextField] (foundation, sans Material).
 *
 * Gère la bordure (repos/focus/erreur/désactivé), le placeholder, le curseur et les
 * contenus d'en-tête/fin. Le libellé et le message d'erreur sont fournis par l'appelant
 * de plus haut niveau (`AppTextField`).
 */
@Composable
fun AppTextFieldCore(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        !enabled -> colors.beige
        isError -> colors.danger
        focused -> colors.primary
        else -> colors.border
    }
    val borderWidth = if (focused) dimens.borderWidthFocused else dimens.borderWidth
    val shape = RoundedCornerShape(dimens.radiusMd)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = AppTheme.typography.body.copy(color = if (enabled) colors.text else colors.muted),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
    ) { innerTextField ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, shape)
                .padding(horizontal = dimens.md, vertical = dimens.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                leadingContent()
                androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = dimens.xs))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    AppText(placeholder, style = AppTextStyle.Body, color = colors.muted)
                }
                innerTextField()
            }
            if (trailingContent != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = dimens.xs))
                trailingContent()
            }
        }
    }
}
```

- [ ] **Step 2: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/AppTextFieldCore.kt
git commit -m "feat(component): AppTextFieldCore sur BasicTextField (sans Material)"
```

---

## Task 5: Socle `base/` — dialog, dropdown, progress, icon-button

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/AppDialogCore.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/AppDropdownCore.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/AppProgress.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/AppIconButton.kt`

**Interfaces:**
- Produces:
  - `@Composable fun AppDialogCore(onDismiss: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit)` — scrim + carte centrée animée via `Popup`.
  - `@Composable fun AppDropdownCore(expanded: Boolean, onDismissRequest: () -> Unit, anchor: @Composable (Modifier) -> Unit, content: @Composable () -> Unit)` — champ d'ancrage + popup de menu.
  - `@Composable fun AppSpinner(modifier: Modifier = Modifier, size: Dp = 24.dp)` et `@Composable fun AppProgressBar(progress: Float, modifier: Modifier = Modifier)` — remplacent `CircularProgressIndicator`/`LinearProgressIndicator`.
  - `@Composable fun AppIconButton(onClick: () -> Unit, contentDescription: String?, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit)` — remplace `material3.IconButton`.

- [ ] **Step 1: Créer `AppProgress.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.base

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.theme.AppTheme

/** Spinner circulaire maison (remplace CircularProgressIndicator). */
@Composable
fun AppSpinner(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "angle",
    )
    val color = AppTheme.colors.primary
    Canvas(modifier.size(size).graphicsLayer { rotationZ = angle }) {
        drawArc(
            color = color, startAngle = 0f, sweepAngle = 270f, useCenter = false,
            style = Stroke(width = size.toPx() * 0.12f, cap = StrokeCap.Round),
        )
    }
}

/** Barre de progression déterminée maison (remplace LinearProgressIndicator). */
@Composable
fun AppProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(AppTheme.dimens.radiusSm)
    androidx.compose.foundation.layout.Box(
        modifier.fillMaxWidth().height(6.dp).clip(shape).background(colors.beige),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).background(colors.primary),
        )
    }
}
```

- [ ] **Step 2: Créer `AppIconButton.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Bouton-icône maison (remplace material3.IconButton). Zone tactile 40dp. */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    androidx.compose.foundation.layout.Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .appPressFeedback(source, enabled)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}
```

- [ ] **Step 3: Créer `AppDialogCore.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.base

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Coquille de modale maison (remplace material3.AlertDialog) : scrim sombre + carte centrée.
 *
 * Clic sur le scrim = [onDismiss]. Le clic sur la carte est absorbé (ne ferme pas).
 * Le contenu (titre, corps, boutons) est fourni par l'appelant `AppDialog`.
 */
@Composable
fun AppDialogCore(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Absorbe le clic pour ne pas fermer quand on interagit avec la carte.
            Box(
                modifier
                    .padding(AppTheme.dimens.lg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) { content() }
        }
    }
}
```

- [ ] **Step 4: Créer `AppDropdownCore.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Menu déroulant maison (remplace ExposedDropdownMenuBox) : un ancrage + un popup sous l'ancre.
 *
 * [anchor] reçoit un Modifier à poser sur le champ cliquable. Quand [expanded] est vrai, le
 * [content] (les items) s'affiche dans un [Popup] positionné juste sous l'ancre.
 */
@Composable
fun AppDropdownCore(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchor: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    anchor(Modifier)
    if (expanded) {
        Popup(
            popupPositionProvider = BelowAnchorPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }
}

/** Positionne le popup juste sous l'ancre, aligné à gauche. */
private object BelowAnchorPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize,
    ): IntOffset = IntOffset(x = anchorBounds.left, y = anchorBounds.bottom)
}
```

- [ ] **Step 5: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/base/
git commit -m "feat(component): socle base — dialog/dropdown/progress/icon-button (sans Material)"
```

---

## Task 6: Jeu d'icônes maison `AppIcons`

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/icons/AppIcons.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/icons/AppIconsTest.kt`

**Interfaces:**
- Produces: `object AppIcons` avec 23 `val ... : ImageVector` (lazy) couvrant exactement l'inventaire : `Add, AccountCircle, Badge, Castle, Category, Close, ContentCopy, Delete, Edit, Group, Groups, Home, Mail, MenuBook, Person, PersonOutline, Settings, Visibility, VisibilityOff, ArrowBack, List, Email, Lock`.

Mapping vers les anciens noms Material (pour les tâches de migration suivantes) :
`Icons.Default.Add`→`AppIcons.Add`, `Icons.Default.AccountCircle`→`AppIcons.AccountCircle`, `Icons.Default.Badge`→`AppIcons.Badge`, `Icons.Default.Castle`→`AppIcons.Castle`, `Icons.Default.Category`→`AppIcons.Category`, `Icons.Filled.Close`→`AppIcons.Close`, `Icons.Filled.ContentCopy`→`AppIcons.ContentCopy`, `Icons.Filled.Delete`→`AppIcons.Delete`, `Icons.Filled.Edit`→`AppIcons.Edit`, `Icons.Default.Group`→`AppIcons.Group`, `Icons.Default.Groups`→`AppIcons.Groups`, `Icons.Default.Home`→`AppIcons.Home`, `Icons.Default.MailOutline`→`AppIcons.Mail`, `Icons.Default.MenuBook`→`AppIcons.MenuBook`, `Icons.Default.Person`→`AppIcons.Person`, `Icons.Outlined.Person`→`AppIcons.PersonOutline`, `Icons.Default.Settings`→`AppIcons.Settings`, `Icons.Filled.Visibility`→`AppIcons.Visibility`, `Icons.Filled.VisibilityOff`→`AppIcons.VisibilityOff`, `Icons.AutoMirrored.Filled.ArrowBack`→`AppIcons.ArrowBack`, `Icons.AutoMirrored.Filled.List`→`AppIcons.List`, `Icons.Outlined.Email`→`AppIcons.Email`, `Icons.Outlined.Lock`→`AppIcons.Lock`.

- [ ] **Step 1: Écrire le test (tous les vecteurs existent et ont une taille valide)**

Créer `src/desktopTest/kotlin/eu/ejdr/presentation/shared/icons/AppIconsTest.kt` :

```kotlin
package eu.ejdr.presentation.shared.icons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconsTest {
    @Test
    fun `les 23 icones sont definies et non vides`() {
        val all = listOf(
            AppIcons.Add, AppIcons.AccountCircle, AppIcons.Badge, AppIcons.Castle,
            AppIcons.Category, AppIcons.Close, AppIcons.ContentCopy, AppIcons.Delete,
            AppIcons.Edit, AppIcons.Group, AppIcons.Groups, AppIcons.Home, AppIcons.Mail,
            AppIcons.MenuBook, AppIcons.Person, AppIcons.PersonOutline, AppIcons.Settings,
            AppIcons.Visibility, AppIcons.VisibilityOff, AppIcons.ArrowBack, AppIcons.List,
            AppIcons.Email, AppIcons.Lock,
        )
        assertEquals(23, all.size)
        all.forEach { assertTrue(it.defaultWidth.value > 0f) }
    }
}
```

- [ ] **Step 2: Lancer — échoue (AppIcons inexistant)**

Run: `./gradlew desktopTest --tests "*AppIconsTest*" 2>&1 | tail -15`
Expected: échec de compilation (`AppIcons` non résolu).

- [ ] **Step 3: Créer `AppIcons.kt`**

Chaque icône est un `ImageVector` 24×24 construit avec `ImageVector.Builder` + `path { }`, trait fin (`stroke`), style manuscrit léger. Fournir les 23. Exemple de motif à répéter (les tracés peuvent être simples : formes géométriques lisibles, l'important est qu'ils compilent, aient 24dp et soient reconnaissables) :

```kotlin
package eu.ejdr.presentation.shared.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Jeu d'icônes maison du design system (trait fin, style manuscrit léger).
 *
 * Remplace `androidx.compose.material.icons`. Chaque icône est un [ImageVector] 24×24 tracé
 * au trait ; la couleur est appliquée par `AppIcon` via le `tint`. Un écran de galerie interne
 * (dev) permet de tous les vérifier visuellement.
 */
object AppIcons {

    // Helper interne : construit une icône « au trait » de 24dp.
    private fun stroked(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                path(
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.7f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                ) { block() }
            }
            .build()

    val Add: ImageVector by lazy { stroked("Add") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) } }
    val Close: ImageVector by lazy { stroked("Close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) } }
    val ArrowBack: ImageVector by lazy { stroked("ArrowBack") { moveTo(19f, 12f); lineTo(5f, 12f); moveTo(11f, 6f); lineTo(5f, 12f); lineTo(11f, 18f) } }
    val List: ImageVector by lazy { stroked("List") { moveTo(4f, 7f); lineTo(20f, 7f); moveTo(4f, 12f); lineTo(20f, 12f); moveTo(4f, 17f); lineTo(20f, 17f) } }
    val Delete: ImageVector by lazy { stroked("Delete") { moveTo(5f, 7f); lineTo(19f, 7f); moveTo(9f, 7f); lineTo(9f, 5f); lineTo(15f, 5f); lineTo(15f, 7f); moveTo(7f, 7f); lineTo(8f, 20f); lineTo(16f, 20f); lineTo(17f, 7f) } }
    val Edit: ImageVector by lazy { stroked("Edit") { moveTo(4f, 20f); lineTo(4f, 16f); lineTo(16f, 4f); lineTo(20f, 8f); lineTo(8f, 20f); close() } }
    val ContentCopy: ImageVector by lazy { stroked("ContentCopy") { moveTo(9f, 9f); lineTo(20f, 9f); lineTo(20f, 20f); lineTo(9f, 20f); close(); moveTo(9f, 15f); lineTo(4f, 15f); lineTo(4f, 4f); lineTo(15f, 4f); lineTo(15f, 9f) } }
    val Person: ImageVector by lazy { stroked("Person") { moveTo(12f, 4f); arcToRelative(4f, 4f, 0f, true, true, 0f, 8f); arcToRelative(4f, 4f, 0f, true, true, 0f, -8f); close(); moveTo(4f, 21f); arcToRelative(8f, 8f, 0f, false, true, 16f, 0f) } }
    val PersonOutline: ImageVector by lazy { Person }
    val AccountCircle: ImageVector by lazy { stroked("AccountCircle") { moveTo(12f, 3f); arcToRelative(9f, 9f, 0f, true, true, 0f, 18f); arcToRelative(9f, 9f, 0f, true, true, 0f, -18f); close(); moveTo(12f, 8f); arcToRelative(2.5f, 2.5f, 0f, true, true, 0f, 5f); arcToRelative(2.5f, 2.5f, 0f, true, true, 0f, -5f); close(); moveTo(6.5f, 18f); arcToRelative(5.5f, 5.5f, 0f, false, true, 11f, 0f) } }
    val Group: ImageVector by lazy { stroked("Group") { moveTo(9f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, -6f); close(); moveTo(3f, 19f); arcToRelative(6f, 6f, 0f, false, true, 12f, 0f); moveTo(16f, 8f); arcToRelative(3f, 3f, 0f, false, true, 0f, 6f); moveTo(21f, 19f); arcToRelative(6f, 6f, 0f, false, false, -4f, -5.6f) } }
    val Groups: ImageVector by lazy { Group }
    val Home: ImageVector by lazy { stroked("Home") { moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f); moveTo(6f, 10f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 10f) } }
    val Settings: ImageVector by lazy { stroked("Settings") { moveTo(12f, 9f); arcToRelative(3f, 3f, 0f, true, true, 0f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, -6f); close(); moveTo(12f, 2f); lineTo(12f, 5f); moveTo(12f, 19f); lineTo(12f, 22f); moveTo(2f, 12f); lineTo(5f, 12f); moveTo(19f, 12f); lineTo(22f, 12f) } }
    val Category: ImageVector by lazy { stroked("Category") { moveTo(12f, 3f); lineTo(16f, 9f); lineTo(8f, 9f); close(); moveTo(4f, 13f); lineTo(10f, 13f); lineTo(10f, 19f); lineTo(4f, 19f); close(); moveTo(17f, 13f); arcToRelative(3f, 3f, 0f, true, true, 0f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, -6f); close() } }
    val Castle: ImageVector by lazy { stroked("Castle") { moveTo(4f, 20f); lineTo(4f, 8f); lineTo(7f, 8f); lineTo(7f, 5f); lineTo(9f, 5f); lineTo(9f, 8f); lineTo(15f, 8f); lineTo(15f, 5f); lineTo(17f, 5f); lineTo(17f, 8f); lineTo(20f, 8f); lineTo(20f, 20f); close() } }
    val MenuBook: ImageVector by lazy { stroked("MenuBook") { moveTo(12f, 6f); lineTo(12f, 20f); moveTo(12f, 6f); arcToRelative(6f, 3f, 0f, false, false, -8f, 0f); lineTo(4f, 18f); arcToRelative(6f, 3f, 0f, false, true, 8f, 0f); moveTo(12f, 6f); arcToRelative(6f, 3f, 0f, false, true, 8f, 0f); lineTo(20f, 18f); arcToRelative(6f, 3f, 0f, false, false, -8f, 0f) } }
    val Badge: ImageVector by lazy { stroked("Badge") { moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 19f); lineTo(5f, 19f); close(); moveTo(9f, 9f); lineTo(15f, 9f); moveTo(9f, 13f); lineTo(13f, 13f) } }
    val Mail: ImageVector by lazy { stroked("Mail") { moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 18f); lineTo(4f, 18f); close(); moveTo(4f, 7f); lineTo(12f, 13f); lineTo(20f, 7f) } }
    val Email: ImageVector by lazy { Mail }
    val Lock: ImageVector by lazy { stroked("Lock") { moveTo(7f, 11f); lineTo(7f, 8f); arcToRelative(5f, 5f, 0f, false, true, 10f, 0f); lineTo(17f, 11f); moveTo(5f, 11f); lineTo(19f, 11f); lineTo(19f, 20f); lineTo(5f, 20f); close() } }
    val Visibility: ImageVector by lazy { stroked("Visibility") { moveTo(2f, 12f); arcToRelative(10f, 6f, 0f, false, true, 20f, 0f); arcToRelative(10f, 6f, 0f, false, true, -20f, 0f); close(); moveTo(12f, 9f); arcToRelative(3f, 3f, 0f, true, true, 0f, 6f); arcToRelative(3f, 3f, 0f, true, true, 0f, -6f); close() } }
    val VisibilityOff: ImageVector by lazy { stroked("VisibilityOff") { moveTo(3f, 12f); arcToRelative(10f, 6f, 0f, false, true, 18f, -3f); moveTo(21f, 12f); arcToRelative(10f, 6f, 0f, false, true, -14f, 4.5f); moveTo(4f, 4f); lineTo(20f, 20f) } }
}
```

Note : `PersonOutline`, `Groups`, `Email` réutilisent volontairement un tracé voisin (YAGNI — glyphes distincts si besoin plus tard).

- [ ] **Step 4: Lancer le test — passe**

Run: `./gradlew desktopTest --tests "*AppIconsTest*" 2>&1 | tail -15`
Expected: PASS.

- [ ] **Step 5: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/icons/ src/desktopTest/kotlin/eu/ejdr/presentation/shared/icons/AppIconsTest.kt
git commit -m "feat(icons): jeu d'icones maison AppIcons (23 glyphes, sans Material)"
```

---

## Task 7: Réimplémenter les atomes de rendu simple sur le socle

Réécrit `AppText`, `AppIcon`, `AppDivider`, `AppCheckbox`, `AppFab` pour supprimer material3, en gardant les signatures publiques.

**Files:**
- Modify: `.../atomic/AppText.kt`, `.../atomic/AppIcon.kt`, `.../atomic/AppDivider.kt`, `.../atomic/AppCheckbox.kt`, `.../atomic/AppFab.kt`

**Interfaces:**
- Consumes: `AppSurface`, `AppIconButton` (non ici), `AppIcons`, `AppTheme.*`, `LocalContentColor`.
- Produces: mêmes signatures qu'aujourd'hui (voir inventaire) — `AppText`, `AppIcon`, `AppDivider`+`VerticalSpacer`+`HorizontalSpacer`, `AppCheckbox`, `AppFab`.

- [ ] **Step 1: `AppText.kt` — `material3.Text` → `foundation.text.BasicText`**

Remplacer `import androidx.compose.material3.Text` par `import androidx.compose.foundation.text.BasicText`. Dans le corps, remplacer l'appel `Text(text = ..., style = ..., color = ..., maxLines = ..., ...)` par :
```kotlin
BasicText(
    text = text,
    modifier = modifier,
    style = resolvedStyle.merge(TextStyle(color = resolvedColor, textAlign = textAlign)),
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
)
```
où `resolvedStyle` est le `TextStyle` issu de `AppTheme.typography` selon `AppTextStyle`, et `resolvedColor = color ?: LocalContentColor.current`. Ajouter imports `androidx.compose.ui.text.TextStyle`, `androidx.compose.ui.text.style.TextOverflow`, `eu.ejdr.presentation.shared.theme.LocalContentColor`. Garder la logique de mapping style existante.

- [ ] **Step 2: `AppIcon.kt` — `material3.Icon` → `foundation.Image` + tint**

Remplacer `import androidx.compose.material3.Icon` par `import androidx.compose.foundation.Image` et `import androidx.compose.ui.graphics.ColorFilter`. Remplacer l'appel `Icon(imageVector = ..., contentDescription = ..., tint = ..., modifier = ...)` par :
```kotlin
Image(
    imageVector = imageVector,
    contentDescription = contentDescription,
    modifier = modifier.size(size),
    colorFilter = ColorFilter.tint(tint ?: LocalContentColor.current),
)
```
`LocalContentColor` est déjà l'import maison (corrigé Task 1).

- [ ] **Step 3: `AppDivider.kt` — `HorizontalDivider` → `Box` fin**

Remplacer l'usage de `HorizontalDivider` par :
```kotlin
Box(modifier.fillMaxWidth().height(AppTheme.dimens.borderWidth).background(AppTheme.colors.border))
```
Ajouter imports `foundation.layout.Box`, `foundation.layout.fillMaxWidth`, `foundation.layout.height`, `foundation.background`. Retirer l'import material3. `VerticalSpacer`/`HorizontalSpacer` inchangés.

- [ ] **Step 4: `AppCheckbox.kt` — `Checkbox` → case maison (`Canvas`/`Box`)**

Remplacer `Checkbox`+`CheckboxDefaults` par une case dessinée : un `Box` 20dp bordé `AppTheme.colors.border` (fond `primary` + coche quand `checked`), cliquable via `toggleable`, suivi du `label` en `AppText`. Coche dessinée avec un `Canvas` (deux `drawLine`). Conserver la `Row` + label + `enabled`. Retirer les imports material3.

- [ ] **Step 5: `AppFab.kt` — `FloatingActionButton`+`Icons.Filled.Add` → `AppSurface` rond + `AppIcon(AppIcons.Add)`**

```kotlin
AppSurface(
    modifier = modifier.size(56.dp),
    shape = CircleShape,
    color = AppTheme.colors.primary,
    contentColor = AppTheme.colors.onPrimary,
    elevation = AppTheme.dimens.elevationLg,
    onClick = onClick,
) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppIcon(AppIcons.Add, contentDescription = contentDescription, tint = AppTheme.colors.onPrimary)
    }
}
```
Retirer imports material3 + material.icons.

- [ ] **Step 6: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/
git commit -m "refactor(component): atomes de rendu (Text/Icon/Divider/Checkbox/Fab) sans Material"
```

---

## Task 8: Réimplémenter `AppButton` sur le socle (variants + relief riche)

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppButton.kt`

**Interfaces:**
- Consumes: `AppSurface`, `appPressFeedback`, `AppSpinner`, `AppIcon`, `AppTheme.treatment`.
- Produces: `AppButton(label, onClick, modifier, variant = ButtonVariant.Primary, enabled = true, loading = false, leadingIcon: ImageVector? = null)` — signature inchangée ; `enum ButtonVariant { Primary, Secondary, Text, Danger, Ghost }` inchangé.

- [ ] **Step 1: Réécrire `AppButton.kt`**

Supprimer tous les imports material3. Implémenter chaque variant avec `AppSurface` :
- **Primary** : en zone `Rich` → fond en dégradé vertical `accentGradientTop→accentGradientBottom` (via `Modifier.background(Brush.verticalGradient(...))` sur le contenu, `AppSurface` color transparent) + bordure fine `ornament` ; en zone `Plain` → fond plein `colors.primary`. Texte `onPrimary`.
- **Secondary** : fond `surface`, bordure `border`, texte `text`.
- **Text** : transparent, pas de bordure, texte `primary`.
- **Danger** : fond `danger`, texte `onDanger` (plain) — même logique.
- **Ghost** : transparent, bordure `border`, texte `textSecondary`.

Conserver le garde anti-double-clic existant (`ClickGuard`/`CLICK_GUARD_WINDOW_MS = 400L`) : n'appeler `onClick` que si le garde autorise et `enabled && !loading`. Quand `loading`, afficher `AppSpinner(size = 18.dp)` à la place du label et désactiver. `leadingIcon` rendu via `AppIcon` avant le label.

Le corps (contenu) : une `Row` centrée, padding `horizontal = dimens.lg, vertical = dimens.sm`, forme `RoundedCornerShape(dimens.radiusMd)`.

Reprendre la couleur de texte par variant pour la passer en `contentColor` d'`AppSurface` (les `AppText`/`AppIcon` internes héritent via `LocalContentColor`).

- [ ] **Step 2: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppButton.kt
git commit -m "refactor(component): AppButton sans Material (relief dore en zone riche)"
```

---

## Task 9: Réimplémenter les champs — `AppTextField`, `AppPasswordField`, `AppDropdown`

**Files:**
- Modify: `.../atomic/AppTextField.kt`, `.../atomic/AppPasswordField.kt`, `.../atomic/AppDropdown.kt`

**Interfaces:**
- Consumes: `AppTextFieldCore`, `AppDropdownCore`, `AppIconButton`, `AppIcon`, `AppIcons`, `LabeledField` (molecule), `AppTheme.*`.
- Produces: signatures inchangées (voir inventaire) : `AppTextField(...)`, `AppPasswordField(...)`, `AppDropdown(value, options, onSelect, label, modifier)`.

- [ ] **Step 1: `AppTextField.kt` — sur `AppTextFieldCore` + `LabeledField`**

Réécrire pour composer le label (via `LabeledField(label, errorMessage = errorMessage)`) autour de `AppTextFieldCore` :
```kotlin
LabeledField(label = label, modifier = modifier, errorMessage = errorMessage) {
    AppTextFieldCore(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        enabled = enabled,
        isError = errorMessage != null,
        singleLine = singleLine,
        leadingContent = leadingIcon?.let { { AppIcon(it, contentDescription = null, tint = AppTheme.colors.muted) } },
        trailingContent = trailingContent,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
    )
}
```
Retirer les imports material3. (Le message d'erreur désormais rendu par `LabeledField`, pas en double.)

- [ ] **Step 2: `AppPasswordField.kt` — toggle œil via `AppIconButton` + `AppIcons`**

Réécrire sur `AppTextField` (déjà maison) en passant `trailingContent` = un `AppIconButton` qui bascule `visible`, avec `AppIcon(if (visible) AppIcons.VisibilityOff else AppIcons.Visibility, ...)`, et `visualTransformation = if (visible) None else PasswordVisualTransformation()`. Retirer imports material3 + material.icons.

- [ ] **Step 3: `AppDropdown.kt` — sur `AppDropdownCore`**

Réécrire sans l'API `ExposedDropdownMenuBox`. État local `expanded`. L'ancre = un champ cliquable ressemblant à `AppTextFieldCore` en lecture seule affichant `value ?: label`, avec `AppIcon` chevron/`List` en trailing, qui bascule `expanded`. Le `content` du `AppDropdownCore` = une `AppSurface` (fond `surface`, bordure `border`, elevation `elevationMd`) contenant une `Column` d'items : chaque option est une `Row` cliquable (`AppText`), qui appelle `onSelect(option)` puis ferme. Retirer `@OptIn(ExperimentalMaterial3Api::class)` et tous imports material3.

- [ ] **Step 4: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppTextField.kt src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppPasswordField.kt src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppDropdown.kt
git commit -m "refactor(component): champs texte/password/dropdown sans Material"
```

---

## Task 10: Réimplémenter les organisms partagés — `AppCard`, `AppDialog`, `AppTopBar`

**Files:**
- Modify: `.../organism/AppCard.kt`, `.../organism/AppDialog.kt`, `.../organism/AppTopBar.kt`

**Interfaces:**
- Consumes: `AppSurface`, `AppDialogCore`, `AppIconButton`, `AppIcon`, `AppIcons`, `AppButton`, `interactiveCardElevation` (existant), `AppTheme.treatment`.
- Produces: signatures inchangées (voir inventaire) : `AppCard(...)`, `AppDialog(...)`, `AppTopBar(...)`.

- [ ] **Step 1: `AppCard.kt` — `Surface` → `AppSurface` (+ filet doré si Rich)**

Remplacer les deux branches `Surface(...)` par un unique `AppSurface(modifier = surfaceModifier, shape = shape, color = containerColor, contentColor = AppTheme.colors.text, border = border, elevation = if (onClick != null) animatedElevation else elevation, onClick = onClick, interactionSource = interactionSource) { padded() }`. Garder `interactiveCard`/`interactiveCardElevation`. **Si `AppTheme.treatment == AppTreatment.Rich`**, dessiner un filet intérieur : envelopper `padded` pour ajouter un `Modifier.drawBehind { drawRoundRect(color = ornament.copy(alpha=0.22f), style = Stroke(...), cornerRadius = ...) }` inset de `dimens.xs`. Retirer import `material3.Surface`.

- [ ] **Step 2: `AppDialog.kt` — `AlertDialog` → `AppDialogCore`**

Réécrire le corps avec `AppDialogCore(onDismiss = onDismiss)` contenant une `AppSurface` (fond `surface`, forme `radiusMd`, elevation `elevationLg`, padding `lg`) avec une `Column` : titre (`AppText` Subtitle), `content()`, puis une `Row` de boutons alignée à droite — `AppButton(dismissLabel, onDismiss, variant = Ghost)` si `dismissLabel != null`, et `AppButton(confirmLabel, onConfirm, variant = confirmVariant, enabled = confirmEnabled)`. Signature/params inchangés. Retirer import `material3.AlertDialog`.

- [ ] **Step 3: `AppTopBar.kt` — `IconButton`+`Icons.*` → `AppIconButton`+`AppIcons` (icône seule + tooltip)**

Remplacer chaque `IconButton { Icon(Icons.X, ...) }` par un `AppIconButton(onClick = onX, contentDescription = "<libellé>") { AppIcon(AppIcons.X, contentDescription = "<libellé>") }`, en mappant : Person→`AppIcons.Person` (profil), Category→`AppIcons.Category` (références), Group→`AppIcons.Group` (groupes), MailOutline→`AppIcons.Mail` (invitations), Settings→`AppIcons.Settings`, AccountCircle→`AppIcons.AccountCircle`, ArrowBack→`AppIcons.ArrowBack` (onBack), List→`AppIcons.List` (campagnes). L'onglet actif (`profileActive`) teinte l'icône en `colors.primary` (sinon `textSecondary`). Retirer imports material3 + material.icons. (Le tooltip au survol est ajouté Task 16 côté chrome desktop ; ici on garde `contentDescription`.)

- [ ] **Step 4: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/organism/
git commit -m "refactor(component): AppCard/AppDialog/AppTopBar sans Material"
```

---

## Task 11: Migrer les composants de features en commonMain (IconButton/Icons/AlertDialog/Surface/Divider)

Ces fichiers (hors socle) utilisent encore material3/icons. Migration mécanique.

**Files:**
- Modify: `.../features/reference/component/ReferenceComponents.kt` (IconButton, Icons.Edit/Delete)
- Modify: `.../features/charactersheet/component/SheetReferenceComponents.kt` (AlertDialog, IconButton, Icons.Add/Close)
- Modify: `.../features/charactersheet/component/CharacterSheetCard.kt` (IconButton, Icons.ContentCopy/Delete)
- Modify: `.../features/campaign/component/CampaignCard.kt` (IconButton, Icons.Delete)
- Modify: `.../features/auth/component/AuthForm.kt` (Surface, HorizontalDivider, Icons.Email/Lock/Person)

**Interfaces:** aucun changement de signature — remplacements internes uniquement.

- [ ] **Step 1: Recette de remplacement (appliquer à chaque fichier)**

- `material3.IconButton { … }` → `base.AppIconButton(onClick, contentDescription = "<action>") { … }`
- `material3.Icon(Icons.Filled.X, contentDescription = cd)` → `atomic.AppIcon(AppIcons.X, contentDescription = cd)` (mapping Task 6)
- `material3.AlertDialog(...)` → réécrire avec `atomic.../organism.AppDialog(...)` **ou** `base.AppDialogCore` selon la structure. Pour `SheetReferenceComponents.kt`, préférer `AppDialog` si le contenu s'y prête, sinon `AppDialogCore` + contenu maison.
- `material3.Surface(...)` → `base.AppSurface(...)`
- `material3.HorizontalDivider()` → `atomic.AppDivider()`
- Retirer tous les imports `androidx.compose.material3.*` et `androidx.compose.material.icons.*` de chaque fichier.

- [ ] **Step 2: `AuthForm.kt` (écran riche) — poser le traitement**

Dans `AuthForm.kt`, remplacer la `Surface` de la carte par `AppSurface` (fond `surface`, bordure `border`, forme `radiusLg`, elevation `elevationMd`), `HorizontalDivider`→`AppDivider`, et les 3 icônes `Icons.Outlined.Email/Lock/Person`→`AppIcons.Email/Lock/PersonOutline`. (Le `ProvideTreatment(Rich)` sera posé au niveau de la page auth en Task 13.)

- [ ] **Step 3: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Compiler Android aussi (ces fichiers sont communs)**

Run: `./gradlew compileDebugKotlinAndroid 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/features/
git commit -m "refactor(feature): composants communs (reference/sheet/campaign/auth) sans Material"
```

---

## Task 12: Migrer les pages desktop utilisant material3

**Files:**
- Modify: `src/desktopMain/.../navigation/AppNavDisplay.kt` (CircularProgressIndicator)
- Modify: `src/desktopMain/.../shared/component/organism/UpdateDialog.kt` (AlertDialog, LinearProgressIndicator)
- Modify: `src/desktopMain/.../features/user/page/UserPage.kt` (HorizontalDivider, Surface)
- Modify: `src/desktopMain/.../features/friendgroup/page/GroupDetailPage.kt` (CircularProgressIndicator)
- Modify: `src/desktopMain/.../features/friendgroup/page/InvitationsPage.kt` (CircularProgressIndicator)
- Modify: `src/desktopMain/.../features/reference/page/ReferenceListPage.kt` (Icons.Add/Category)
- Modify: `src/desktopMain/.../features/campaign/page/CampaignListPage.kt` (Icons.List/Add)
- Modify: `src/desktopMain/.../features/campaign/page/CampaignDetailPage.kt` (Icons.Add)
- Modify: `src/desktopMain/.../features/friendgroup/page/GroupListPage.kt` (Icons.Add/Group)
- Modify: `src/desktopMain/.../features/charactersheet/page/MyCharacterSheetsPage.kt` (Icons.Add/Person)

**Interfaces:** aucun changement de signature.

- [ ] **Step 1: Appliquer la recette de remplacement**

- `CircularProgressIndicator()` → `base.AppSpinner()`
- `LinearProgressIndicator(progress = { p })` → `base.AppProgressBar(progress = p)`
- `HorizontalDivider()` → `atomic.AppDivider()`
- `material3.Surface(...)` → `base.AppSurface(...)`
- `material3.AlertDialog(...)` (UpdateDialog) → `base.AppDialogCore(...)` + carte `AppSurface` + boutons `AppButton` (l'UpdateDialog a une barre de progression : utiliser `AppProgressBar`)
- Icônes `Icons.*` → `AppIcons.*` (mapping Task 6)
- **`UserPage.kt` = écran riche** : envelopper le contenu de la page dans `ProvideTreatment(AppTreatment.Rich) { … }`, et remplacer la `Surface` de la carte profil par `AppSurface`.
- Retirer tous les imports material3 + material.icons de chaque fichier.

- [ ] **Step 2: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/desktopMain/
git commit -m "refactor(desktop): pages sans Material (spinner/divider/surface/dialog/icones)"
```

---

## Task 13: Migrer les pages Android + bottom bar

**Files:**
- Modify: `src/androidMain/.../navigation/AppNavDisplay.kt` (CircularProgressIndicator, NavigationBar, NavigationBarItem)
- Modify: `src/androidMain/.../presentation/App.kt` (Surface)
- Modify: `src/androidMain/.../features/user/page/UserPage.kt` (HorizontalDivider, Surface)
- Modify: `src/androidMain/.../features/friendgroup/page/GroupDetailPage.kt` (CircularProgressIndicator)
- Modify: `src/androidMain/.../features/friendgroup/page/InvitationsPage.kt` (CircularProgressIndicator)
- Modify: `src/androidMain/.../features/reference/page/ReferenceListPage.kt` (Icons.Add/Category)
- Modify: `src/androidMain/.../features/campaign/page/CampaignListPage.kt` (Icons.List/Add)
- Modify: `src/androidMain/.../features/campaign/page/CampaignDetailPage.kt` (Icons.Add)
- Modify: `src/androidMain/.../features/friendgroup/page/GroupListPage.kt` (Icons.Add/Group)
- Modify: `src/androidMain/.../features/charactersheet/page/MyCharacterSheetsPage.kt` (Icons.Add/Person)
- Create: `src/androidMain/.../presentation/shared/component/organism/AppBottomBar.kt`

**Interfaces:**
- Consumes: `appNavItems` (existant, `NavItems.kt`), `AppIcons`, `AppSurface`, `AppIcon`, `AppText`.
- Produces: `@Composable fun AppBottomBar(items: List<...>, currentRoute: ..., onSelect: (...) -> Unit)` — icône seule, **libellé sur l'onglet actif uniquement** (pastille dorée).

- [ ] **Step 1: Créer `AppBottomBar.kt` (Android)**

Barre `Row` pleine largeur, fond `AppSurface(color = surface, elevation = elevationMd)`, un item par entrée de `appNavItems`. Item inactif = `AppIcon` seul (`textSecondary`). Item actif = pastille `AppSurface(color = colors.beige, shape = pill)` contenant `AppIcon` (`primary`) + `AppText(label)` en `primary`. Les icônes viennent de `AppIcons` (mapper depuis les icônes de `NavItems.kt` : Home, Castle, Badge, MenuBook, Groups, Settings → `AppIcons.Home/Castle/Badge/MenuBook/Groups/Settings`). Chaque item cliquable via `AppIconButton`/`clickable` → `onSelect`.

- [ ] **Step 2: Remplacer `NavigationBar`/`NavigationBarItem` dans `AppNavDisplay.kt` (Android)**

Substituer le bloc `NavigationBar { appNavItems.forEach { NavigationBarItem(...) } }` par `AppBottomBar(items = appNavItems, currentRoute = <route courante>, onSelect = { backStack... })`. `CircularProgressIndicator`→`AppSpinner`. Retirer imports material3.

- [ ] **Step 3: `NavItems.kt` — retirer les `Icons.*` Material**

`NavItems.kt` (commonMain) importe `Icons.Default.Home/Castle/Badge/MenuBook/Groups/Settings`. Remplacer le type d'icône de chaque item par `AppIcons.*` correspondant (si le modèle stocke un `ImageVector`, c'est direct). Retirer l'import material.icons.

- [ ] **Step 4: `App.kt` + `UserPage.kt` Android**

`App.kt` : `material3.Surface` racine → `base.AppSurface` (ou simple `Box` avec `background(colors.background)`). `UserPage.kt` : idem desktop — `ProvideTreatment(Rich)`, `Surface`→`AppSurface`, `HorizontalDivider`→`AppDivider`. Icônes des autres pages → `AppIcons.*`. Retirer imports material3 + material.icons partout.

- [ ] **Step 5: Compiler Android**

Run: `./gradlew compileDebugKotlinAndroid 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: verifyDesktop vert (rien de cassé côté commun)**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add src/androidMain/ src/commonMain/kotlin/eu/ejdr/presentation/navigation/NavItems.kt
git commit -m "refactor(android): pages + bottom bar maison (actif nomme) sans Material"
```

---

## Task 14: Retirer `material3` et `materialIconsExtended` du build

**Files:**
- Modify: `build.gradle.kts` (lignes ~90-91 du bloc `commonMain.dependencies`)

**Interfaces:** aucune — changement de build.

- [ ] **Step 1: Grep de contrôle AVANT (doit être vide)**

Run: `grep -rn "androidx.compose.material3" src/ ; grep -rn "androidx.compose.material.icons" src/`
Expected: AUCUNE ligne. Si des lignes restent, revenir aux tâches concernées et les migrer d'abord.

- [ ] **Step 2: Retirer les deux dépendances**

Dans `build.gradle.kts`, dans `val commonMain by getting { dependencies { … } }`, supprimer :
```kotlin
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
```
Garder `compose.runtime`, `compose.foundation`, `compose.components.resources`. Vérifier que `compose.ui` est disponible transitivement via `compose.foundation` ; si une erreur d'import `androidx.compose.ui.*` apparaît à la compilation, ajouter `implementation(compose.ui)` explicitement.

- [ ] **Step 3: Build complet desktop**

Run: `./gradlew clean verifyDesktop 2>&1 | tail -25`
Expected: BUILD SUCCESSFUL. (Un `clean` force la recompilation sans material3 dans le classpath — prouve qu'aucune référence ne subsiste.)

- [ ] **Step 4: Build Android**

Run: `./gradlew compileDebugKotlinAndroid 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add build.gradle.kts
git commit -m "build(front): retirer material3 et materialIconsExtended"
```

---

## Task 15: Écrans riches — Splash, en-têtes ornés, traitement `Rich`

**Files:**
- Modify: `src/desktopMain/.../navigation/AppNavDisplay.kt` (SplashScreen — déjà `AppBrandMark`)
- Modify: pages Auth desktop + android (poser `ProvideTreatment(Rich)`)
- Modify: `src/commonMain/.../features/charactersheet/...` page détail (poser `ProvideTreatment(Rich)`) — desktop + android
- Verify: `PageHeader(flourish = true)` sur les hubs riches

**Interfaces:** consomme `ProvideTreatment`, `AppTreatment.Rich`, `PageHeader(flourish = true)`, `AppBrandMark`, `AppCornerFlourish`.

- [ ] **Step 1: Poser `ProvideTreatment(Rich)` sur les 4 écrans vitrine**

Pour **Auth** (desktop `AuthPage`, android `AuthScreen`), **Accueil/Profil** (`UserPage` desktop+android — déjà fait Task 12/13), **Fiche de perso détail** (`CharacterSheetDetailPage` desktop+android), envelopper le contenu racine de la page :
```kotlin
ProvideTreatment(AppTreatment.Rich) {
    // contenu existant de la page
}
```
Import : `eu.ejdr.presentation.shared.theme.ProvideTreatment`, `eu.ejdr.presentation.shared.theme.AppTreatment`.

- [ ] **Step 2: Activer le flourish sur les en-têtes riches**

Sur la fiche de perso détail (et Accueil si applicable), passer `flourish = true` à `PageHeader(...)`. Les écrans sobres restent `flourish = false` (défaut).

- [ ] **Step 3: Vérifier le Splash**

Le `SplashScreen` (desktop `AppNavDisplay.kt`) utilise déjà `AppBrandMark`. S'assurer qu'il est rendu sur fond `colors.background` et centré. (Pas de changement fonctionnel requis si déjà en place.)

- [ ] **Step 4: verifyDesktop + Android**

Run: `./gradlew verifyDesktop 2>&1 | tail -15 && ./gradlew compileDebugKotlinAndroid 2>&1 | tail -15`
Expected: les deux BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat(ui): traitement riche (Rich + flourish) sur Splash/Auth/Accueil/Fiche"
```

---

## Task 16: Navigation desktop — tooltip au survol sur la top bar

**Files:**
- Create: `src/desktopMain/.../presentation/shared/component/organism/AppTooltip.kt`
- Modify: `src/desktopMain/.../navigation/MainTopBar.kt`

**Interfaces:**
- Produces: `@Composable fun AppTooltip(text: String, content: @Composable () -> Unit)` — desktop uniquement : affiche `text` dans une petite bulle au survol de `content`, via `TooltipArea` (compose.desktop) ou détection de survol maison + `Popup`.

- [ ] **Step 1: Créer `AppTooltip.kt` (desktopMain)**

Utiliser `androidx.compose.foundation.TooltipArea` (disponible sur desktop) :
```kotlin
package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.runtime.Composable
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.base.AppSurface
import eu.ejdr.presentation.shared.theme.AppTheme

/** Bulle d'aide au survol (desktop). Nomme une icône seule dans la top bar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTooltip(text: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = {
            AppSurface(
                color = AppTheme.colors.surface,
                contentColor = AppTheme.colors.text,
                elevation = AppTheme.dimens.elevationMd,
            ) {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.padding(
                        horizontal = AppTheme.dimens.sm, vertical = AppTheme.dimens.xs,
                    ),
                ) { AppText(text, style = AppTextStyle.Caption) }
            }
        },
        content = content,
    )
}
```

- [ ] **Step 2: Envelopper chaque action de `MainTopBar` dans `AppTooltip`**

Dans `MainTopBar.kt` (desktop), envelopper chaque `AppIconButton` de la top bar par `AppTooltip("<libellé>") { AppIconButton(...) }` : Profil, Campagnes, Fiches, Groupes, Éléments, Réglages, Invitations, Retour.

- [ ] **Step 3: verifyDesktop vert**

Run: `./gradlew verifyDesktop 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/desktopMain/
git commit -m "feat(nav): tooltip au survol sur la top bar desktop (icone seule nommee)"
```

---

## Task 17: Vérification finale end-to-end + revue visuelle

**Files:** aucun (validation).

- [ ] **Step 1: Grep de contrôle final — zéro Material**

Run: `grep -rn "androidx.compose.material3\|androidx.compose.material.icons" src/`
Expected: AUCUNE ligne.

- [ ] **Step 2: Build propre complet (desktop)**

Run: `./gradlew clean verifyDesktop 2>&1 | tail -25`
Expected: BUILD SUCCESSFUL, tous les tests desktop verts (dont `AppColorsTreatmentTest`, `AppIconsTest`, `filterNumericInput`, tests thème existants), koverVerify ≥ 60.

- [ ] **Step 3: Build Android**

Run: `./gradlew assembleDebug 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL (APK produit).

- [ ] **Step 4: Lancer l'app desktop et revue visuelle des 3 thèmes**

Run: `./gradlew run`
Vérifier manuellement, en basculant Parchemin/Taupe/Grimoire dans Réglages :
- Login/Register (riche) : carte encadrée, champs maison focus doré, bouton primaire en relief (Grimoire).
- Accueil/Profil (riche) : en-tête + carte ciselée, filet doré (Grimoire).
- Listes (sobre) : grille de cartes, pas d'ornement.
- Fiche de perso détail (riche) : en-tête flourish, corps lisible, onglets, dropdowns maison fonctionnels.
- Réglages (sobre), dialogues (créer/supprimer) : modales maison ouvrent/ferment.
- Top bar desktop : icônes seules, tooltip au survol, onglet actif doré.
- Aucun écran clair résiduel en thème Grimoire (preuve que Material n'impose plus rien).

- [ ] **Step 5: Commit éventuel de correctifs de revue, puis récapitulatif**

Si la revue révèle des ajustements, les corriger (commits `fix(ui): …`) et relancer `verifyDesktop`. Sinon :

```bash
git log --oneline origin/main..HEAD
```
Expected : la série de commits de la refonte, tous atomiques.

---

## Self-Review (rempli à la rédaction)

**Couverture spec ↔ tâches :**
- Retrait material3 → Task 14 + grep Task 17. ✅
- Retrait materialIconsExtended + icônes maison → Task 6 + Task 14. ✅
- Socle base/ (Surface, TextFieldCore, DialogCore, DropdownCore, BottomBar, feedback) → Tasks 3-5, 13. ✅
- Thème étendu (ornement/gradient/elevation + AppTreatment) → Task 2. ✅
- Champs maison sur BasicTextField → Task 4 + Task 9. ✅
- Composants réhabillés (mêmes noms/API) → Tasks 7-10. ✅
- Migration des 28 fichiers material3 → Tasks 7-13 (couvre chaque fichier de l'inventaire). ✅
- Riche vs sobre par écran → Task 15 (+ traitement posé dans 11/12/13). ✅
- Nav desktop tooltip + Android actif-nommé → Task 16 + Task 13. ✅
- Charte inchangée → contrainte globale, Task 2 n'ajoute que des rôles dérivés. ✅
- Animations équilibrées → feedback press (Task 3), dialog fade/scale (Task 5), spinner (Task 5) ; `AppMotion` inchangé, respecte `enabled`. ✅
- verifyDesktop vert à chaque tâche → étape de vérif dans chaque tâche. ✅
- Tests logique pure → Task 2 (couleurs), Task 6 (icônes) ; `filterNumericInput` conservé. ✅

**Placeholders :** aucun TODO/TBD ; les tâches mécaniques (11-13) donnent une recette de remplacement explicite + la liste exhaustive des fichiers/symboles issue de l'inventaire, pas « etc. ».

**Cohérence des types :** `AppSurface`, `AppTextFieldCore`, `AppDialogCore`, `AppDropdownCore`, `AppIconButton`, `AppSpinner`, `AppProgressBar`, `appPressFeedback`, `AppIcons.*`, `AppTreatment`, `ProvideTreatment`, `AppTheme.treatment`, `AppTheme.elevation` — noms utilisés de façon identique entre la tâche qui les définit et celles qui les consomment.
