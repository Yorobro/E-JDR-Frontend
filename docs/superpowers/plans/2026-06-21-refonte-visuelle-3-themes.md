# Refonte visuelle — 3 thèmes + design system pro Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Donner au front une vraie identité visuelle professionnelle via 3 thèmes sélectionnables (Parchemin clair, Taupe clair, Grimoire sombre) bâtis sur une architecture de couleurs à deux niveaux (teintes brutes → rôles sémantiques), une typographie custom (Fraunces / Inter / JetBrains Mono) et un polish des composants partagés — sans réécrire les écrans métier.

**Architecture :** Le design system reste centralisé dans `presentation/shared/theme`. On insère **sous** l'`AppColors` existant (rôles sémantiques) une nouvelle couche `AppPalette` (teintes brutes nommées) : chaque valeur hex n'est définie **qu'une fois**. Les 3 thèmes deviennent des `AppColors` qui pointent vers ces teintes. L'enum `ThemeVariant` passe de `{LIGHT, DARK}` à `{PARCHEMIN, TAUPE, GRIMOIRE}` ; la persistance (qui sérialise `enum.name`) suit sans changement de format. La typo charge des polices `.ttf` via `compose.components.resources`. Le `when` de sélection des couleurs (dans les deux `App.kt`) et le sélecteur Settings sont étendus.

**Tech Stack :** Kotlin Multiplatform 2.2.20, Compose Multiplatform 1.8.2, Material3, Koin, `compose.components.resources` (déjà en dépendance), JUnit5 + MockK (tests desktop).

## Global Constraints

- **Branche dédiée :** créer `feat/refonte-visuelle` depuis `main` (on est actuellement sur `feat/sheet-detail-realtime` — NE PAS travailler dessus). Voir Task 0.
- **Vérif locale :** `./gradlew.bat verifyDesktop` (= detekt + desktopJar + koverVerify) doit rester **vert à la fin de chaque tâche**. C'est la commande CI reproduite localement.
- **Commitlint strict :** le sujet de commit NE commence PAS par une majuscule (`feat: ajouter…` pas `feat: Ajouter…`). Format Conventional Commits.
- **Taille fichier :** max 500 lignes par fichier (`ejdr/file-size`).
- **Tests :** uniquement dans `src/desktopTest/`. Pattern existant : JUnit5 (`org.junit.jupiter`), MockK, `kotlin.test` assertions.
- **Source de vérité chromatique :** après ce chantier, AUCUNE valeur `Color(0x…)` ne doit apparaître ailleurs que dans `AppPalette.kt`. C'est l'invariant qui garantit « changer une couleur = un seul endroit ».
- **Couverture Kover :** seuil global 60 %. Les packages `presentation.shared.theme` ne sont PAS exclus de Kover → toute logique non-`@Composable` ajoutée au thème doit être couverte, ou rester triviale (data classes / mappings purs sans branches). Les `@Composable` ne sont pas couverts par les tests unitaires : éviter d'y mettre de la logique.
- **Polices :** Fraunces, Inter, JetBrains Mono — toutes sous licence OFL (redistribuables). Fichiers `.ttf` à placer dans `src/commonMain/composeResources/font/`. ⚠️ Ces fichiers binaires ne peuvent pas être générés par un agent : Task 6 documente leur obtention manuelle et fournit un **fallback police-système** pour que le build reste vert même sans les fichiers.

---

## File Structure

**Créés :**
- `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppPalette.kt` — teintes brutes nommées, par thème. **Unique** source des valeurs hex.
- `src/commonMain/composeResources/font/` — dossier des polices `.ttf` (Task 6).
- `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppFonts.kt` — `FontFamily` résolues depuis les ressources (avec fallback).
- `src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppColorsTest.kt` — vérifie que chaque thème mappe les rôles attendus.
- `src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/ThemeVariantTest.kt` — vérifie le mapping `ThemeVariant → AppColors` et le défaut.

**Modifiés :**
- `src/commonMain/.../domain/features/settings/entities/ThemeVariant.kt` — enum `{PARCHEMIN, TAUPE, GRIMOIRE}` + défaut.
- `src/commonMain/.../presentation/shared/theme/AppColors.kt` — 3 fabriques de palette pointant vers `AppPalette` ; fonction `colorsFor(ThemeVariant)`.
- `src/commonMain/.../presentation/shared/theme/AppTypography.kt` — styles enrichis + branchement polices.
- `src/commonMain/.../presentation/shared/theme/AppDimens.kt` — échelle d'espacement/élévation revue.
- `src/desktopMain/.../presentation/App.kt` + `src/androidMain/.../presentation/App.kt` — `when` sur les 3 variantes via `colorsFor`.
- `src/commonMain/.../presentation/features/settings/component/SettingsForm.kt` — sélecteur 3 thèmes.
- `src/commonMain/.../presentation/features/settings/SettingsViewModel.kt` — défaut `PARCHEMIN`.
- `src/{desktop,android}Main/.../infrastructure/settings/*ThemeRepository.kt` — défaut de repli `PARCHEMIN` (3 occurrences chacun).
- `src/commonMain/.../presentation/shared/component/atomic/AppText.kt` — appliquer la `fontFamily` du style.
- Composants polish (Task 9) : `AppButton.kt`, surfaces de cartes.

---

## Task 0 : Préparer la branche

**Files:** aucun fichier code.

- [ ] **Step 1 : Créer la branche depuis main**

```bash
git fetch origin main
git switch -c feat/refonte-visuelle origin/main
```

- [ ] **Step 2 : Vérifier l'état de départ vert**

Run: `./gradlew.bat verifyDesktop`
Expected: BUILD SUCCESSFUL (état de référence avant toute modification).

---

## Task 1 : Couche teintes brutes `AppPalette`

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppPalette.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppPaletteTest.kt`

**Interfaces:**
- Produces: trois `object` (`ParchmentPalette`, `TaupePalette`, `GrimoirePalette`), chacun exposant des `val` `Color` nommés par teinte (ex. `bole`, `ink`, `vellum`). Consommés par Task 2.

- [ ] **Step 1 : Écrire le test (les teintes signature existent et sont distinctes)**

```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertNotEquals

class AppPaletteTest {
    @Test
    fun `les accents signature des trois themes sont distincts`() {
        assertNotEquals(ParchmentPalette.bole, GrimoirePalette.brass)
        assertNotEquals(TaupePalette.taupe, ParchmentPalette.bole)
    }

    @Test
    fun `aucune teinte n'est transparente`() {
        val all = listOf(
            ParchmentPalette.parchment, ParchmentPalette.vellum, ParchmentPalette.ink, ParchmentPalette.bole,
            TaupePalette.background, TaupePalette.surface, TaupePalette.taupe, TaupePalette.ink,
            GrimoirePalette.background, GrimoirePalette.surface, GrimoirePalette.brass, GrimoirePalette.cream,
        )
        all.forEach { assertNotEquals(0f, it.alpha) }
    }
}
```

- [ ] **Step 2 : Lancer le test, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.theme.AppPaletteTest"`
Expected: FAIL — `AppPalette` non défini (erreur de compilation).

- [ ] **Step 3 : Créer `AppPalette.kt`**

```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.ui.graphics.Color

/**
 * Teintes brutes du design system — **unique source de vérité chromatique**.
 *
 * Chaque couleur n'est définie qu'ICI, nommée par ce qu'elle EST (et non par son rôle).
 * Les rôles sémantiques (`AppColors`) pointent vers ces teintes. Pour changer une couleur
 * dans toute l'app, modifier une seule `val` ici.
 *
 * Trois familles, une par thème : Parchemin (clair chaleureux), Taupe (clair minimaliste),
 * Grimoire (sombre premium).
 */

/** Parchemin — beige chaud, encre brune, sceau de cire. */
object ParchmentPalette {
    val parchment = Color(0xFFF2EBDC) // fond, vieille page
    val vellum = Color(0xFFFBF7EE)    // surfaces élevées (plus claires → profondeur)
    val sand = Color(0xFFE4D8C2)      // zones secondaires
    val oak = Color(0xFFC2B393)       // bordures
    val ink = Color(0xFF2E2A22)       // texte principal (brun-noir d'encre)
    val sepia = Color(0xFF6B5D45)     // texte secondaire
    val faded = Color(0xFF9A8E76)     // texte atténué / placeholder
    val bole = Color(0xFF8A4B3A)      // ACCENT : terre de Sienne (sceau)
    val boleHi = Color(0xFFA15B48)    // accent survol/pressé
    val gold = Color(0xFFB08A3E)      // accent rare (actif/badge)
    val rust = Color(0xFF9E3B2E)      // danger
    val onAccent = Color(0xFFFBF7EE)  // contenu sur accent/danger
}

/** Taupe — gris/beige neutre, minimaliste, surfaces blanches. */
object TaupePalette {
    val background = Color(0xFFFAF8F4) // fond crème (conservé de l'ancien thème)
    val surface = Color(0xFFFFFFFF)    // blanc pur → cartes qui se détachent
    val beige = Color(0xFFEFEAE1)      // zones secondaires
    val border = Color(0xFFE0D9CC)     // bordures hairline claires
    val ink = Color(0xFF2A2722)        // texte principal
    val sepia = Color(0xFF6B655B)      // texte secondaire
    val faded = Color(0xFF9C958A)      // texte atténué
    val taupe = Color(0xFF5A5248)      // ACCENT taupe (conservé, enrichi)
    val taupeHi = Color(0xFF74695A)    // accent survol/pressé
    val rust = Color(0xFFA13D33)       // danger
    val onAccent = Color(0xFFFFFFFF)   // contenu sur accent/danger
}

/** Grimoire — brun-noir chaud, crème, laiton/or vieilli. */
object GrimoirePalette {
    val background = Color(0xFF16140F) // presque noir, chaud (pas bleuté)
    val surface = Color(0xFF211D16)    // cartes
    val raised = Color(0xFF2B261D)     // surfaces hautes / zones secondaires
    val border = Color(0xFF3D372C)     // bordures
    val cream = Color(0xFFECE4D6)      // texte principal crème chaud
    val sepia = Color(0xFF9E9683)      // texte secondaire
    val faded = Color(0xFF6E675A)      // texte atténué
    val brass = Color(0xFFC9A24B)      // ACCENT : laiton/or vieilli
    val brassHi = Color(0xFFDBB662)    // accent survol/pressé
    val ember = Color(0xFFB5503A)      // danger (rouge braise)
    val onAccent = Color(0xFF16140F)   // contenu sur accent (sombre sur or)
    val onDanger = Color(0xFFFBF7EE)   // contenu sur danger
}
```

- [ ] **Step 4 : Lancer le test, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.theme.AppPaletteTest"`
Expected: PASS.

- [ ] **Step 5 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppPalette.kt src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppPaletteTest.kt
git commit -m "feat: ajouter la couche de teintes brutes appPalette (source unique des couleurs)"
```

---

## Task 2 : Les 3 palettes de rôles `AppColors` + `colorsFor`

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppColors.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppColorsTest.kt`

**Interfaces:**
- Consumes: `ParchmentPalette`, `TaupePalette`, `GrimoirePalette` (Task 1) ; `ThemeVariant` (étendu en Task 3, mais ce test n'en dépend pas encore — on expose des fabriques nommées).
- Produces: `fun parchmentColors(): AppColors`, `fun taupeColors(): AppColors`, `fun grimoireColors(): AppColors`. La data class `AppColors` est inchangée (mêmes 12 champs). Les anciennes `lightColors()`/`darkColors()` sont SUPPRIMÉES (remplacées).

- [ ] **Step 1 : Écrire le test**

```kotlin
package eu.ejdr.presentation.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppColorsTest {
    @Test
    fun `parchemin et taupe sont des themes clairs, grimoire sombre`() {
        assertFalse(parchmentColors().isDark)
        assertFalse(taupeColors().isDark)
        assertTrue(grimoireColors().isDark)
    }

    @Test
    fun `chaque role pointe vers la teinte brute attendue`() {
        assertEquals(ParchmentPalette.bole, parchmentColors().primary)
        assertEquals(ParchmentPalette.parchment, parchmentColors().background)
        assertEquals(TaupePalette.taupe, taupeColors().primary)
        assertEquals(GrimoirePalette.brass, grimoireColors().primary)
        assertEquals(GrimoirePalette.background, grimoireColors().background)
    }
}
```

- [ ] **Step 2 : Lancer le test, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.theme.AppColorsTest"`
Expected: FAIL — `parchmentColors`/`taupeColors`/`grimoireColors` non définis.

- [ ] **Step 3 : Remplacer le bas de `AppColors.kt`**

Garder la `data class AppColors(...)` (lignes 26-39) telle quelle. **Supprimer** `lightColors()` et `darkColors()` (lignes 41-71). Les remplacer par :

```kotlin
/** Thème Parchemin — clair, chaleureux (beige + sceau de cire). */
fun parchmentColors(): AppColors = AppColors(
    background = ParchmentPalette.parchment,
    surface = ParchmentPalette.vellum,
    beige = ParchmentPalette.sand,
    border = ParchmentPalette.oak,
    muted = ParchmentPalette.faded,
    textSecondary = ParchmentPalette.sepia,
    text = ParchmentPalette.ink,
    primary = ParchmentPalette.bole,
    onPrimary = ParchmentPalette.onAccent,
    danger = ParchmentPalette.rust,
    onDanger = ParchmentPalette.onAccent,
    isDark = false,
)

/** Thème Taupe — clair, minimaliste (gris/beige neutre, surfaces blanches). */
fun taupeColors(): AppColors = AppColors(
    background = TaupePalette.background,
    surface = TaupePalette.surface,
    beige = TaupePalette.beige,
    border = TaupePalette.border,
    muted = TaupePalette.faded,
    textSecondary = TaupePalette.sepia,
    text = TaupePalette.ink,
    primary = TaupePalette.taupe,
    onPrimary = TaupePalette.onAccent,
    danger = TaupePalette.rust,
    onDanger = TaupePalette.onAccent,
    isDark = false,
)

/** Thème Grimoire — sombre, premium (brun-noir chaud, accent laiton). */
fun grimoireColors(): AppColors = AppColors(
    background = GrimoirePalette.background,
    surface = GrimoirePalette.surface,
    beige = GrimoirePalette.raised,
    border = GrimoirePalette.border,
    muted = GrimoirePalette.faded,
    textSecondary = GrimoirePalette.sepia,
    text = GrimoirePalette.cream,
    primary = GrimoirePalette.brass,
    onPrimary = GrimoirePalette.onAccent,
    danger = GrimoirePalette.ember,
    onDanger = GrimoirePalette.onDanger,
    isDark = true,
)
```

- [ ] **Step 4 : Lancer le test, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.theme.AppColorsTest"`
Expected: PASS.

- [ ] **Step 5 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppColors.kt src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/AppColorsTest.kt
git commit -m "feat: definir les 3 palettes de roles (parchemin, taupe, grimoire)"
```

> ⚠️ À ce stade `App.kt` (×2) NE COMPILE PLUS (`lightColors`/`darkColors` supprimés). C'est attendu — Task 3 + 4 réparent. Ne PAS lancer `verifyDesktop` complet avant la fin de Task 4 ; `desktopTest --tests` cible compile le sous-graphe nécessaire.

---

## Task 3 : Étendre `ThemeVariant` + mapping `colorsFor`

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/domain/features/settings/entities/ThemeVariant.kt`
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppColors.kt` (ajouter `colorsFor`)
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/ThemeVariantTest.kt`

**Interfaces:**
- Consumes: `parchmentColors()`, `taupeColors()`, `grimoireColors()` (Task 2).
- Produces: `enum class ThemeVariant { PARCHEMIN, TAUPE, GRIMOIRE; companion object { val DEFAULT = PARCHEMIN } }` ; `fun colorsFor(variant: ThemeVariant): AppColors`. Consommés par Tasks 4, 5, 7, 8.

- [ ] **Step 1 : Écrire le test**

```kotlin
package eu.ejdr.presentation.shared.theme

import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeVariantTest {
    @Test
    fun `colorsFor mappe chaque variante vers sa palette`() {
        assertEquals(parchmentColors(), colorsFor(ThemeVariant.PARCHEMIN))
        assertEquals(taupeColors(), colorsFor(ThemeVariant.TAUPE))
        assertEquals(grimoireColors(), colorsFor(ThemeVariant.GRIMOIRE))
    }

    @Test
    fun `le defaut est parchemin`() {
        assertEquals(ThemeVariant.PARCHEMIN, ThemeVariant.DEFAULT)
    }
}
```

- [ ] **Step 2 : Lancer, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.theme.ThemeVariantTest"`
Expected: FAIL — `PARCHEMIN`/`colorsFor`/`DEFAULT` non définis.

- [ ] **Step 3 : Étendre l'enum**

Remplacer le contenu de `ThemeVariant.kt` par :

```kotlin
package eu.ejdr.domain.features.settings.entities

/** Thèmes disponibles dans l'application (chacun a son ambiance figée). */
enum class ThemeVariant {
    /** Clair, chaleureux (beige + sceau de cire). */
    PARCHEMIN,

    /** Clair, minimaliste (gris/beige neutre). */
    TAUPE,

    /** Sombre, premium (brun-noir chaud, accent laiton). */
    GRIMOIRE,

    ;

    companion object {
        /** Thème par défaut / repli sûr quand rien n'est persisté. */
        val DEFAULT = PARCHEMIN
    }
}
```

- [ ] **Step 4 : Ajouter `colorsFor` en bas de `AppColors.kt`**

```kotlin
/** Projette une [ThemeVariant] persistée vers sa palette de rôles. */
fun colorsFor(variant: eu.ejdr.domain.features.settings.entities.ThemeVariant): AppColors =
    when (variant) {
        eu.ejdr.domain.features.settings.entities.ThemeVariant.PARCHEMIN -> parchmentColors()
        eu.ejdr.domain.features.settings.entities.ThemeVariant.TAUPE -> taupeColors()
        eu.ejdr.domain.features.settings.entities.ThemeVariant.GRIMOIRE -> grimoireColors()
    }
```

- [ ] **Step 5 : Lancer, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.theme.ThemeVariantTest"`
Expected: PASS.

- [ ] **Step 6 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/domain/features/settings/entities/ThemeVariant.kt src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppColors.kt src/desktopTest/kotlin/eu/ejdr/presentation/shared/theme/ThemeVariantTest.kt
git commit -m "feat: etendre themevariant aux 3 themes avec mapping colorsfor"
```

---

## Task 4 : Brancher les 2 `App.kt` + réparer les défauts de repli

**Files:**
- Modify: `src/desktopMain/kotlin/eu/ejdr/presentation/App.kt:26-28,58-63`
- Modify: `src/androidMain/kotlin/eu/ejdr/presentation/App.kt:22-24,50-55`
- Modify: `src/commonMain/.../presentation/features/settings/SettingsViewModel.kt:33`
- Modify: `src/desktopMain/.../infrastructure/settings/ThemeFileRepository.kt:17,22,23`
- Modify: `src/androidMain/.../infrastructure/settings/AndroidThemeRepository.kt:20`

**Interfaces:**
- Consumes: `colorsFor(ThemeVariant)` (Task 3), `ThemeVariant.DEFAULT` (Task 3).

- [ ] **Step 1 : desktop App.kt — remplacer imports et `when`**

Remplacer les imports (lignes 27-28) :
```kotlin
import eu.ejdr.presentation.shared.theme.colorsFor
```
(supprimer `import …darkColors` et `import …lightColors`)

Remplacer le bloc `AppTheme(colors = when …)` (lignes 58-63) par :
```kotlin
    AppTheme(colors = colorsFor(themeVariant)) {
```

- [ ] **Step 2 : android App.kt — idem**

Mêmes remplacements : import `colorsFor` à la place de `darkColors`/`lightColors` ; `AppTheme(colors = colorsFor(themeVariant)) {`.

- [ ] **Step 3 : Remplacer les défauts de repli `ThemeVariant.LIGHT` → `ThemeVariant.DEFAULT`**

- `SettingsViewModel.kt:33` : `MutableStateFlow(ThemeVariant.DEFAULT)`.
- `ThemeFileRepository.kt` : les 3 occurrences `ThemeVariant.LIGHT` (lignes 17, 22, 23) → `ThemeVariant.DEFAULT`.
- `AndroidThemeRepository.kt:20` : `?: ThemeVariant.DEFAULT`.

(`ThemeVariant.valueOf` reste valide : il lira `"PARCHEMIN"`/`"TAUPE"`/`"GRIMOIRE"`. Les anciennes valeurs persistées `"LIGHT"`/`"DARK"` ne matchent plus → `runCatching{}.getOrNull()` retombe sur `DEFAULT`. Comportement voulu : migration douce.)

- [ ] **Step 4 : Vérif complète — tout doit recompiler**

Run: `./gradlew.bat verifyDesktop`
Expected: BUILD SUCCESSFUL. (Premier point de contrôle global depuis Task 2.)

- [ ] **Step 5 : Commit**

```bash
git add src/desktopMain/kotlin/eu/ejdr/presentation/App.kt src/androidMain/kotlin/eu/ejdr/presentation/App.kt src/commonMain/kotlin/eu/ejdr/presentation/features/settings/SettingsViewModel.kt src/desktopMain/kotlin/eu/ejdr/infrastructure/settings/ThemeFileRepository.kt src/androidMain/kotlin/eu/ejdr/infrastructure/settings/AndroidThemeRepository.kt
git commit -m "feat: brancher les 3 themes dans les shells app et les repos de persistance"
```

---

## Task 5 : Sélecteur de thème dans Settings

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/features/settings/component/SettingsForm.kt`

**Interfaces:**
- Consumes: `ThemeVariant` (3 valeurs), `onThemeChange: (ThemeVariant) -> Unit` (signature inchangée).

- [ ] **Step 1 : Remplacer le corps de `SettingsForm`**

Remplacer le `Row` à 2 boutons (lignes 26-37) par une liste des 3 thèmes, dérivée de l'enum (pas de duplication) :

```kotlin
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm)) {
            ThemeVariant.entries.forEach { variant ->
                AppButton(
                    label = themeLabel(variant),
                    onClick = { onThemeChange(variant) },
                    variant = if (currentTheme == variant) ButtonVariant.Primary else ButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
```

Ajouter en bas du fichier la fonction de libellé (et l'import `androidx.compose.foundation.layout.fillMaxWidth`) :

```kotlin
private fun themeLabel(variant: ThemeVariant): String = when (variant) {
    ThemeVariant.PARCHEMIN -> "Parchemin — clair chaleureux"
    ThemeVariant.TAUPE -> "Taupe — clair minimaliste"
    ThemeVariant.GRIMOIRE -> "Grimoire — sombre"
}
```

Remplacer l'inner `Row` import par `Column` si `Row` n'est plus utilisé (detekt signalera l'import inutilisé).

- [ ] **Step 2 : Vérif**

Run: `./gradlew.bat verifyDesktop`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/features/settings/component/SettingsForm.kt
git commit -m "feat: selecteur des 3 themes dans les parametres"
```

---

## Task 6 : Embarquer les polices (avec fallback sûr)

**Files:**
- Create: `src/commonMain/composeResources/font/` + fichiers `.ttf`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppFonts.kt`

**Interfaces:**
- Produces: `@Composable fun appDisplayFamily(): FontFamily`, `appBodyFamily()`, `appMonoFamily()`. Consommés par Task 7.

> **⚠️ Étape manuelle (agent ne peut pas créer de binaire) :** télécharger les `.ttf` OFL et les placer dans `src/commonMain/composeResources/font/` :
> - Fraunces : `Fraunces-Regular.ttf`, `Fraunces-SemiBold.ttf` (https://github.com/undercasetype/Fraunces)
> - Inter : `Inter-Regular.ttf`, `Inter-Medium.ttf`, `Inter-SemiBold.ttf` (https://github.com/rsms/inter)
> - JetBrains Mono : `JetBrainsMono-Regular.ttf` (https://github.com/JetBrains/JetBrainsMono)
>
> Nom de fichier en minuscules + underscores requis par compose-resources : `fraunces_regular.ttf`, `fraunces_semibold.ttf`, `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `jetbrains_mono_regular.ttf`.
> **Si les fichiers ne sont pas fournis**, implémenter Step 2 (fallback) et SAUTER Step 3 ; le build reste vert sur polices système.

- [ ] **Step 1 : Si les `.ttf` sont présents — `AppFonts.kt` avec ressources**

```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ejdr_frontend.generated.resources.Res
import ejdr_frontend.generated.resources.fraunces_regular
import ejdr_frontend.generated.resources.fraunces_semibold
import ejdr_frontend.generated.resources.inter_medium
import ejdr_frontend.generated.resources.inter_regular
import ejdr_frontend.generated.resources.inter_semibold
import ejdr_frontend.generated.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

@Composable
fun appDisplayFamily(): FontFamily = FontFamily(
    Font(Res.font.fraunces_regular, FontWeight.Normal),
    Font(Res.font.fraunces_semibold, FontWeight.SemiBold),
)

@Composable
fun appBodyFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
)

@Composable
fun appMonoFamily(): FontFamily = FontFamily(
    Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
)
```

- [ ] **Step 2 : Fallback (toujours valable, utilisé SI pas de `.ttf`)**

Si les fichiers ne sont pas fournis, créer à la place :
```kotlin
package eu.ejdr.presentation.shared.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

// Fallback police-système tant que les .ttf OFL ne sont pas embarqués.
// Remplacer par la version ressources (cf. plan Task 6 Step 1) une fois les polices ajoutées.
@Composable fun appDisplayFamily(): FontFamily = FontFamily.Serif
@Composable fun appBodyFamily(): FontFamily = FontFamily.SansSerif
@Composable fun appMonoFamily(): FontFamily = FontFamily.Monospace
```

- [ ] **Step 3 : Vérif**

Run: `./gradlew.bat verifyDesktop`
Expected: BUILD SUCCESSFUL. (Si erreur `unresolved reference: fraunces_regular`, les `.ttf` manquent ou sont mal nommés → utiliser le fallback Step 2.)

- [ ] **Step 4 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppFonts.kt
# inclure src/commonMain/composeResources/font/ si les .ttf sont présents
git commit -m "feat: ajouter les familles de polices du design system (fraunces, inter, mono)"
```

---

## Task 7 : Typographie enrichie (hiérarchie + polices)

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppTypography.kt`
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppTheme.kt` (fournir une `AppTypography` construite avec les familles)
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppText.kt` (déjà lit `typo.*` — vérifier que la `fontFamily` est portée par le `TextStyle`)

**Interfaces:**
- Consumes: `appDisplayFamily()`, `appBodyFamily()`, `appMonoFamily()` (Task 6).
- Produces: `AppTypography` enrichi (champ `mono` ajouté + `fontFamily` dans chaque `TextStyle`) ; `@Composable fun appTypography(): AppTypography`.

- [ ] **Step 1 : Enrichir `AppTypography`**

Remplacer la data class (échelle plus contrastée + familles + style `mono` pour les stats) :

```kotlin
data class AppTypography(
    val title: TextStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp),
    val subtitle: TextStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
    val body: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    val label: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    val caption: TextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    val mono: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
)
```

- [ ] **Step 2 : Construire la typo avec les familles**

Ajouter dans `AppTypography.kt` une fabrique `@Composable` (les familles le sont) :

```kotlin
@Composable
fun appTypography(): AppTypography {
    val display = appDisplayFamily()
    val body = appBodyFamily()
    val mono = appMonoFamily()
    return AppTypography(
        title = AppTypography().title.copy(fontFamily = display),
        subtitle = AppTypography().subtitle.copy(fontFamily = display),
        body = AppTypography().body.copy(fontFamily = body),
        label = AppTypography().label.copy(fontFamily = body),
        caption = AppTypography().caption.copy(fontFamily = body),
        mono = AppTypography().mono.copy(fontFamily = mono),
    )
}
```

(imports : `androidx.compose.runtime.Composable`.)

- [ ] **Step 3 : Fournir cette typo depuis `AppTheme`**

Dans `AppTheme.kt`, la signature a déjà `typography: AppTypography = AppTypography()`. Changer le défaut pour la version à polices : remplacer le paramètre par `typography: AppTypography = appTypography()` (l'appel est `@Composable`, autorisé dans la valeur par défaut d'un paramètre de composable). Vérifier que `AppText` porte bien `style = resolved` (déjà le cas, la `fontFamily` voyage avec le `TextStyle`).

- [ ] **Step 4 : Ajouter le style Mono à `AppText`**

Dans `AppText.kt`, étendre l'enum et le `when` :
```kotlin
enum class AppTextStyle { Title, Subtitle, Body, Label, Caption, Mono }
```
```kotlin
        AppTextStyle.Mono -> typo.mono
```

- [ ] **Step 5 : Vérif**

Run: `./gradlew.bat verifyDesktop`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppTypography.kt src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppTheme.kt src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/AppText.kt
git commit -m "feat: typographie a hierarchie renforcee avec polices du design system"
```

---

## Task 8 : Profondeur — élévation des surfaces

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppDimens.kt`
- Modify: composant carte des fiches (`charactersheet/component/SheetCard.kt` ou `CharacterSheetCard.kt`) — appliquer ombre + surface.

**Interfaces:**
- Produces: `AppDimens` avec `elevationSm`/`elevationMd` (Dp) ajoutés.

- [ ] **Step 1 : Étendre `AppDimens`**

Ajouter à la data class : `val elevationSm: Dp = 1.dp,` et `val elevationMd: Dp = 3.dp,`. Affiner les rayons : `radiusSm = 8.dp`, `radiusMd = 12.dp` (cohérence). Garder le reste.

- [ ] **Step 2 : Lire la carte de fiche et y appliquer surface + ombre**

Ouvrir le composant carte (le localiser : `grep -rl "fun.*Card" src/commonMain/kotlin/eu/ejdr/presentation/features/charactersheet/component`). Envelopper le contenu de la carte dans une `Surface` Material3 avec `color = AppTheme.colors.surface`, `shape = RoundedCornerShape(AppTheme.dimens.radiusMd)`, `shadowElevation = AppTheme.dimens.elevationMd`, `border = BorderStroke(1.dp, AppTheme.colors.border)`. Retirer tout `background` direct redondant.

- [ ] **Step 3 : Vérif**

Run: `./gradlew.bat verifyDesktop`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/theme/AppDimens.kt
# + le fichier carte modifié
git commit -m "feat: donner de la profondeur aux cartes (surface elevee + ombre douce)"
```

---

## Task 9 : Polish final + validation runtime

**Files:** ajustements ciblés selon captures.

- [ ] **Step 1 : Lancer l'app desktop et inspecter chaque thème**

Run: `./gradlew.bat run`
Aller dans Paramètres → basculer Parchemin / Taupe / Grimoire. Vérifier : contraste texte lisible sur chaque fond, cartes détachées, boutons accent corrects, top bar cohérente, champs de saisie focus visible.

- [ ] **Step 2 : Corriger les contrastes insuffisants éventuels**

Si un texte secondaire est trop pâle ou un accent trop faible → ajuster **uniquement dans `AppPalette.kt`** (invariant source unique). Re-vérifier.

- [ ] **Step 3 : Vérif finale + couverture**

Run: `./gradlew.bat verifyDesktop`
Expected: BUILD SUCCESSFUL, koverVerify ≥ 60 %.

- [ ] **Step 4 : Vérifier l'invariant « source unique »**

Run: `grep -rn "Color(0x" src/commonMain/kotlin src/desktopMain/kotlin src/androidMain/kotlin | grep -v "AppPalette.kt"`
Expected: AUCUN résultat (toute valeur hex vit dans `AppPalette.kt`). Sinon, déplacer la couleur trouvée dans `AppPalette` et la référencer.

- [ ] **Step 5 : Commit final**

```bash
git add -A
git commit -m "fix: ajustements de contraste et polish visuel des 3 themes"
```

---

## Self-Review (rempli)

**Couverture spec :**
- 3 thèmes sélectionnables → Tasks 2, 3, 5. ✅
- Architecture « 1 couleur = 1 endroit » → Task 1 (`AppPalette`) + invariant vérifié Task 9 Step 4. ✅
- Mêmes familles de couleur (gris/beige conservé en Taupe, ADN chaud partout) → Task 1. ✅
- Polices custom → Tasks 6, 7. ✅
- Rendu pro (hiérarchie typo, profondeur) → Tasks 7, 8. ✅
- Persistance compatible → Task 4 Step 3 (migration douce LIGHT/DARK → DEFAULT). ✅

**Placeholders :** aucun TODO/TBD ; tout code est complet. Les seuls éléments non auto-générables (les `.ttf` binaires) sont explicitement traités avec un fallback build-vert (Task 6). ✅

**Cohérence des types :** `AppColors` (12 champs) inchangée ; `ThemeVariant` 3 valeurs + `DEFAULT` utilisé partout en repli ; `colorsFor` exhaustif sur 3 cas ; `appTypography()` retourne `AppTypography` enrichi cohérent avec `AppText`. ✅

**Risque connu :** valeur par défaut `@Composable` (`appTypography()`) dans la signature de `AppTheme` — légal en Compose mais à confirmer au premier `verifyDesktop` de Task 7 ; repli trivial si refus du compilateur : appeler `appTypography()` dans le corps et le passer explicitement.
