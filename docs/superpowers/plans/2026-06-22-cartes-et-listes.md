# Cartes & listes (Lot 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Améliorer le ressenti des écrans de liste sans toucher au backend : skeletons de chargement, états vides accueillants (icône + message + CTA), apparition en fondu des items, et mise en forme des cartes avec les données déjà disponibles.

**Architecture:** Trois composants partagés réutilisables (`SkeletonBox` atome, `SkeletonGrid`/`SkeletonList` molécule, `EmptyState` molécule) + un utilitaire de date pur (`DateFormat`, parsing ISO manuel car `kotlinx-datetime` absent). Les ~10 pages de liste (5 features × desktop/android) suivent toutes le même pattern `when { isLoading&&empty→skeleton ; empty→EmptyState ; else→grid avec animateItem }`. Tout lit les tokens existants (`AppTheme.colors/dimens/motion`).

**Tech Stack:** Kotlin Multiplatform 2.2.20, Compose Multiplatform 1.8.2 (`Modifier.animateItem`, `rememberInfiniteTransition`), Material3, JUnit5 + MockK (tests desktop).

## Global Constraints

- **Branche :** `feat/cartes-et-listes` depuis **`feat/socle-animation`** (le Lot 2 utilise `AppMotion`/`interactiveCard` du Lot 1, non mergé). Voir Task 0.
- **Vérif :** `./gradlew.bat verifyDesktop --no-daemon` vert à la fin de chaque tâche.
- **Commitlint strict :** sujet de commit en minuscule, Conventional Commits.
- **Max 500 lignes/fichier** ; **tests en `src/desktopTest/`** (JUnit5, kotlin.test).
- **Invariant mouvement :** aucune durée/courbe d'animation en dur hors `AppMotion.kt` ; le shimmer dérive ses durées de `AppMotion`.
- **Pas d'horloge murale** dans le code testable : `relativeDate(iso, today)` reçoit `today` injecté.
- **Données liste = nom-seul pour les fiches** : NE PAS afficher niveau/formation/peuple sur `CharacterSheetCard` (null en liste).
- **Ne pas toucher** : `AppPalette`, 3 thèmes, socle Lot 1, logique métier des ViewModels, backend.
- **Kover :** `...util` couvert (DateFormat pur testé) ; `...component`/`...page` exclus (validation runtime).

---

## File Structure

**Créés :**
- `src/commonMain/.../presentation/shared/util/DateFormat.kt` — `formatDate`, `relativeDate` (purs).
- `src/commonMain/.../presentation/shared/component/atomic/SkeletonBox.kt` — surface pulsante.
- `src/commonMain/.../presentation/shared/component/molecule/SkeletonPlaceholders.kt` — `SkeletonGrid`/`SkeletonList`.
- `src/commonMain/.../presentation/shared/component/molecule/EmptyState.kt` — icône+titre+message+CTA.
- `src/desktopTest/.../presentation/shared/util/DateFormatTest.kt`.

**Modifiés (cartes) :**
- `CampaignCard.kt`, `SessionCard.kt`, `CharacterSheetCard.kt`, `FriendGroupComponents.kt` (GroupCard).

**Modifiés (pages de liste, desktop + android) :** les sous-composables `XxxGrid`/`XxxList` de chaque feature (campaign, charactersheet, reference, session, friendgroup).

---

## Task 0 : Préparer la branche

**Files:** aucun fichier code.

- [ ] **Step 1 : Créer la branche depuis feat/socle-animation**

```bash
git switch feat/socle-animation
git switch -c feat/cartes-et-listes
```

- [ ] **Step 2 : Vérifier l'état de départ vert**

Run: `./gradlew.bat verifyDesktop --no-daemon`
Expected: BUILD SUCCESSFUL.

---

## Task 1 : Utilitaire `DateFormat` (pur, testable)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/util/DateFormat.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/util/DateFormatTest.kt`

**Interfaces:**
- Produces: `fun formatDate(iso: String): String` (« 22 juin 2026 », tolérant) ; `fun relativeDate(iso: String, todayIso: String): String?` (« aujourd'hui »/« dans 3 jours »/« il y a 2 jours »/null). `todayIso` injecté (pas d'horloge). Consommé par Tasks 6-7.

> `kotlinx-datetime` est ABSENT → parsing manuel de `yyyy-MM-dd` (on prend les 10 premiers caractères de l'ISO). Pas de gestion de fuseau/heure (les dates de l'app sont des jours).

- [ ] **Step 1 : Écrire le test**

```kotlin
package eu.ejdr.presentation.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DateFormatTest {
    @Test
    fun `formatDate rend un jour lisible en francais`() {
        assertEquals("22 juin 2026", formatDate("2026-06-22T10:30:00Z"))
        assertEquals("1 janvier 2026", formatDate("2026-01-01"))
    }

    @Test
    fun `formatDate tolere une entree invalide en la renvoyant brute`() {
        assertEquals("pas-une-date", formatDate("pas-une-date"))
        assertEquals("", formatDate(""))
    }

    @Test
    fun `relativeDate calcule aujourd'hui, futur et passe`() {
        assertEquals("aujourd'hui", relativeDate("2026-06-22", todayIso = "2026-06-22"))
        assertEquals("dans 3 jours", relativeDate("2026-06-25", todayIso = "2026-06-22"))
        assertEquals("il y a 2 jours", relativeDate("2026-06-20", todayIso = "2026-06-22"))
        assertEquals("demain", relativeDate("2026-06-23", todayIso = "2026-06-22"))
        assertEquals("hier", relativeDate("2026-06-21", todayIso = "2026-06-22"))
    }

    @Test
    fun `relativeDate renvoie null hors fenetre pertinente ou si invalide`() {
        assertNull(relativeDate("2026-08-01", todayIso = "2026-06-22")) // > 7 jours
        assertNull(relativeDate("invalide", todayIso = "2026-06-22"))
    }
}
```

- [ ] **Step 2 : Lancer, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.util.DateFormatTest" --no-daemon`
Expected: FAIL — fonctions non définies.

- [ ] **Step 3 : Implémenter `DateFormat.kt`**

```kotlin
package eu.ejdr.presentation.shared.util

private val MOIS = arrayOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

/** Parse les 10 premiers caractères ISO (yyyy-MM-dd) → (année, mois 1-12, jour), ou null. */
private fun parseIsoDate(iso: String): Triple<Int, Int, Int>? {
    val head = iso.take(10)
    val parts = head.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (m !in 1..12 || d !in 1..31) return null
    return Triple(y, m, d)
}

/** Nombre de jours depuis une époque arbitraire (algorithme du jour julien simplifié). */
private fun toEpochDay(y: Int, m: Int, d: Int): Long {
    var yy = y.toLong()
    var mm = m.toLong()
    if (mm <= 2) { yy -= 1; mm += 12 }
    val era = (if (yy >= 0) yy else yy - 399) / 400
    val yoe = yy - era * 400
    val doy = (153 * (mm - 3) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/** « 22 juin 2026 ». Entrée non parsable → renvoyée brute (jamais d'exception). */
fun formatDate(iso: String): String {
    val (y, m, d) = parseIsoDate(iso) ?: return iso
    return "$d ${MOIS[m - 1]} $y"
}

/** Indice relatif court, ou null hors fenêtre ±7 jours / entrée invalide. `todayIso` injecté. */
fun relativeDate(iso: String, todayIso: String): String? {
    val target = parseIsoDate(iso) ?: return null
    val today = parseIsoDate(todayIso) ?: return null
    val delta = toEpochDay(target.first, target.second, target.third) -
        toEpochDay(today.first, today.second, today.third)
    return when (delta) {
        0L -> "aujourd'hui"
        1L -> "demain"
        -1L -> "hier"
        in 2L..7L -> "dans $delta jours"
        in -7L..-2L -> "il y a ${-delta} jours"
        else -> null
    }
}
```

- [ ] **Step 4 : Lancer, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.util.DateFormatTest" --no-daemon`
Expected: PASS (4 tests).

- [ ] **Step 5 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/util/DateFormat.kt src/desktopTest/kotlin/eu/ejdr/presentation/shared/util/DateFormatTest.kt
git commit -m "feat: utilitaire de formatage de date (formatdate + relativedate purs)"
```

---

## Task 2 : `SkeletonBox` (atome pulsant)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/SkeletonBox.kt`

**Interfaces:**
- Consumes: `AppTheme.colors/dimens/motion`.
- Produces: `@Composable fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(AppTheme.dimens.radiusMd))`. Consommé par Task 3.

- [ ] **Step 1 : Créer `SkeletonBox.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Brique de chargement « fantôme » : surface neutre qui pulse en opacité (shimmer doux).
 *
 * Respecte [AppTheme] : couleur dérivée des tokens, durée de pulsation issue de [AppTheme.motion]
 * (donc nulle si le mouvement est désactivé → opacité fixe). Composant bête.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
) {
    val motion = AppTheme.motion
    val pulseDuration = motion.effectiveDuration(motion.durationSlow) * 2 // cycle lent
    val alpha = if (pulseDuration == 0) {
        0.4f
    } else {
        val transition = rememberInfiniteTransition(label = "skeletonPulse")
        val animated by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(pulseDuration, easing = motion.easeStandard),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeletonAlpha",
        )
        animated
    }
    Box(
        modifier = modifier
            .clip(shape)
            .alpha(alpha)
            .background(AppTheme.colors.border),
    )
}
```

- [ ] **Step 2 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/atomic/SkeletonBox.kt
git commit -m "feat: atome skeletonbox pulsant pour le chargement"
```

---

## Task 3 : `SkeletonGrid` / `SkeletonList` (molécule)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/molecule/SkeletonPlaceholders.kt`

**Interfaces:**
- Consumes: `SkeletonBox` (Task 2), `AppTheme.dimens`.
- Produces: `@Composable fun SkeletonGrid(itemHeight: Dp, modifier: Modifier = Modifier, count: Int = 6, minTileWidth: Dp = 180.dp)` ; `@Composable fun SkeletonList(itemHeight: Dp, modifier: Modifier = Modifier, count: Int = 5)`. Consommé par Task 8.

- [ ] **Step 1 : Créer `SkeletonPlaceholders.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.SkeletonBox
import eu.ejdr.presentation.shared.theme.AppTheme

/** Grille de tuiles « fantômes » pendant le chargement initial (même disposition adaptative). */
@Composable
fun SkeletonGrid(
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    count: Int = 6,
    minTileWidth: Dp = 180.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minTileWidth),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        items(count) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(itemHeight))
        }
    }
}

/** Liste de rangées « fantômes » pendant le chargement initial (pour les LazyColumn). */
@Composable
fun SkeletonList(
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    count: Int = 5,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        repeat(count) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(itemHeight))
        }
    }
}
```

(import manquant à ajouter : `androidx.compose.foundation.lazy.grid.items` — l'overload `items(count: Int)`.)

- [ ] **Step 2 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/molecule/SkeletonPlaceholders.kt
git commit -m "feat: skeletongrid et skeletonlist pour le chargement des listes"
```

---

## Task 4 : `EmptyState` (molécule)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/molecule/EmptyState.kt`

**Interfaces:**
- Consumes: `AppText`, `AppIcon`, `AppButton`, `AppTheme`.
- Produces: `@Composable fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null)`. Consommé par Task 8.

- [ ] **Step 1 : Créer `EmptyState.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

private val EmptyIconSize = 48.dp

/**
 * État vide accueillant : icône, titre, message et bouton d'action optionnel.
 *
 * Remplace les « Aucun X » textuels par un écran qui invite à agir. Composant bête.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(AppTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.colors.muted,
            modifier = Modifier.size(EmptyIconSize),
        )
        AppText(text = title, style = AppTextStyle.Subtitle, textAlign = TextAlign.Center)
        AppText(
            text = message,
            style = AppTextStyle.Body,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            AppButton(
                label = actionLabel,
                onClick = onAction,
                modifier = Modifier.padding(top = AppTheme.dimens.sm),
            )
        }
    }
}
```

> Vérifier la signature réelle d'`AppIcon` (paramètre `modifier`/`tint` disponibles) ; adapter si besoin. `AppIcon` existe dans `...component/atomic/AppIcon.kt`.

- [ ] **Step 2 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/molecule/EmptyState.kt
git commit -m "feat: molecule emptystate (icone + message + action)"
```

---

## Task 5 : Spike `animateItem` + fondu d'un écran pilote

**Files:**
- Modify: `src/desktopMain/.../charactersheet/page/MyCharacterSheetsPage.kt` (sous-composable `CharacterSheetGrid`).

**Interfaces:**
- Consumes: rien de nouveau (API Compose).

- [ ] **Step 1 : SPIKE — confirmer l'API `animateItem`**

Dans le bloc `items(sheets, key = { it.id }) { sheet -> ... }`, ajouter `Modifier.animateItem()` au composant carte. En Compose 1.8, l'API stable dans `LazyGridItemScope` est `Modifier.animateItem()` (remplace `animateItemPlacement()`). Si le compilateur ne résout pas `animateItem`, essayer `animateItemPlacement()` (ancien nom). Documenter lequel marche.

```kotlin
items(sheets, key = { it.id }) { sheet ->
    val canDelete = canEdit || sheet.ownerId == currentUserId
    CharacterSheetCard(
        sheet = sheet,
        onClick = { onOpenSheet(sheet.id, sheet.name) },
        onDelete = if (canDelete) ({ onDeleteRequest(sheet) }) else null,
        modifier = Modifier.animateItem(),
    )
}
```

(L'API `animateItem()` anime placement ET apparition/disparition par défaut — fondu inclus.)

- [ ] **Step 2 : Vérif**

Run: `./gradlew.bat verifyDesktop --no-daemon`
Expected: BUILD SUCCESSFUL. (Si `animateItem` inconnu → bascule sur `animateItemPlacement()`, re-vérifier.)

- [ ] **Step 3 : Commit**

```bash
git add src/desktopMain/kotlin/eu/ejdr/presentation/features/charactersheet/page/MyCharacterSheetsPage.kt
git commit -m "feat: apparition animee des items de la grille des fiches (pilote)"
```

---

## Task 6 : Mise en forme des cartes (données dispo)

**Files:**
- Modify: `CampaignCard.kt`, `SessionCard.kt`, `CharacterSheetCard.kt`, `FriendGroupComponents.kt` (fonction `GroupCard`).

**Interfaces:**
- Consumes: `formatDate`/`relativeDate` (Task 1). Pour `relativeDate`, la date du jour est fournie par l'appelant (le composant peut recevoir `todayIso` en paramètre OU le calculer ailleurs ; pour rester sans horloge dans le composant testable, on passe la date courante depuis un point unique — mais comme les cartes sont @Composable non testées, un accès à la date système y est tolérable). **Décision :** dans les cartes (non testées), on peut lire la date du jour via une petite fonction `todayIso()` placée en `desktopMain`/`androidMain` (expect/actual) OU se limiter à `formatDate` (pas de relatif) pour éviter expect/actual. **Le plan choisit : `formatDate` partout (toujours dispo), et `relativeDate` UNIQUEMENT sur SessionCard via un `todayIso()` expect/actual minimal.** Si expect/actual alourdit trop, se limiter à `formatDate` et documenter.

- [ ] **Step 1 : `CampaignCard`** — sous le nom, ajouter une ligne `AppText(formatDate(campaign.createdAt) précédé de "Créée le ", style Caption, color textSecondary)`. Lire la structure réelle du fichier d'abord.

- [ ] **Step 2 : `SessionCard`** — mettre la `date` en valeur via `formatDate(session.date)` (style Body/Label) ; optionnellement, si un `todayIso()` est dispo, ajouter `relativeDate(session.date, todayIso())` en Caption à côté. Si pas de `todayIso()`, se limiter à `formatDate`.

- [ ] **Step 3 : `CharacterSheetCard`** — améliorer la hiérarchie : garder le nom (Subtitle) mais l'aligner en haut plutôt que centré-perdu, et ajouter dessous `formatDate(sheet.createdAt)` en Caption/textSecondary. NE PAS afficher niveau/formation/peuple (null en liste). Préserver clic conditionnel + interactiveCard + icône delete.

- [ ] **Step 4 : `GroupCard`** — remplacer le texte de rôle plat (`if (group.myRole == "ADMIN") "Admin" else "Membre"`) par un **badge** : un petit `Surface`/`Box` à fond `beige`, coins arrondis, padding, contenant le libellé de rôle. Réutiliser le helper de libellé existant dans le fichier (`"ADMIN"→"Admin"`, `"MJ"→"MJ"`, sinon `"Membre"`). Préserver « ● Groupe actif » et les boutons.

- [ ] **Step 5 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/features
git commit -m "feat: mise en forme des cartes (dates formatees, badge de role)"
```

---

## Task 7 : Câbler skeleton + EmptyState + fondu sur TOUTES les pages de liste

**Files (desktop ET android) :** les sous-composables `XxxGrid`/`XxxList` de :
- `campaign/page/CampaignListPage.kt`
- `charactersheet/page/MyCharacterSheetsPage.kt` (android ; desktop déjà fait en Task 5 pour le fondu — y ajouter skeleton+EmptyState)
- `reference/page/ReferenceListPage.kt`
- `session/...` (liste de sessions, dans CampaignDetailPage ou SessionList)
- `friendgroup/page/GroupListPage.kt`

**Interfaces:**
- Consumes: `SkeletonGrid`/`SkeletonList` (Task 3), `EmptyState` (Task 4), `Modifier.animateItem` (Task 5).

- [ ] **Step 1 : Pour CHAQUE page, dans le `when` de la zone de contenu :**
  - Remplacer la branche `isLoading && liste.isEmpty() → CircularProgressIndicator` par `→ SkeletonGrid(itemHeight = <hauteur de carte de la feature>)` (ou `SkeletonList` pour les LazyColumn comme les groupes).
  - Remplacer la branche `liste.isEmpty() → AppText("Aucun…")` par `→ EmptyState(icon = <icône de la feature>, title = "...", message = "...", actionLabel = "Créer ...", onAction = <ouvre le dialog/FAB de création>)`. Brancher `onAction` sur la même action que le FAB existant (ex. `showCreate = true`). Textes dans la voix de l'app.
  - Dans la branche grille/liste, ajouter `Modifier.animateItem()` aux items (cf. Task 5).

  Icônes/textes par feature (suggestions, à adapter au ton) :
  - Fiches : `Icons.Default.Person`, « Aucune fiche pour l'instant », « Crée ton premier personnage pour ce groupe. », CTA « Créer une fiche ».
  - Campagnes : `Icons.AutoMirrored.Filled.List`, « Aucune campagne », « Lance ta première campagne. », CTA « Créer une campagne ».
  - Groupes : `Icons.Default.Group`, « Aucun groupe », « Crée un groupe pour jouer avec tes amis. », CTA « Créer un groupe ».
  - Références : `Icons.Default.Category`, « Aucun élément », « Ajoute ton premier élément de référence. », CTA « Ajouter ».
  - Sessions : `Icons.Default.Event` (ou dispo), « Aucune session », « Planifie ta première session. », CTA « Créer une session ».

- [ ] **Step 2 : Vérif desktop + android compile**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
Run: `./gradlew.bat compileDebugKotlinAndroid --no-daemon` → BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add src/desktopMain/kotlin/eu/ejdr/presentation/features src/androidMain/kotlin/eu/ejdr/presentation/features
git commit -m "feat: skeletons, etats vides accueillants et fondu sur toutes les listes"
```

---

## Task 8 : Invariant + validation runtime

**Files:** ajustements ciblés.

- [ ] **Step 1 : Vérifier l'invariant mouvement**

Run (via l'outil Grep) : motif `tween\(|CubicBezierEasing|spring\(` hors `AppMotion.kt`.
Attendu : tout `tween` dérive d'`AppTheme.motion.*` (y compris le shimmer de `SkeletonBox`). Aucune `CubicBezierEasing` hors `AppMotion.kt`.

- [ ] **Step 2 : Lancer l'app et valider**

Run: `./gradlew.bat run --no-daemon`
Vérifier sur les 3 thèmes : skeletons pulsants au chargement, états vides avec CTA fonctionnel (le bouton ouvre bien la création), apparition en fondu des items, cartes avec dates formatées + badge de rôle. Tester un écran vide (nouveau groupe sans fiches).

- [ ] **Step 3 : Vérif finale**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL, koverVerify ≥ 60 %.

- [ ] **Step 4 : Commit final**

```bash
git add -A
git commit -m "fix: ajustements cartes et listes apres validation runtime"
```

---

## Self-Review (rempli)

**Couverture spec :**
- `DateFormat` (parsing manuel, `today` injecté) → Task 1. ✅
- `SkeletonBox`/`SkeletonGrid`/`SkeletonList` → Tasks 2, 3. ✅
- `EmptyState` → Task 4. ✅
- Fondu des listes (`animateItem`) → Tasks 5, 7. ✅
- Mise en forme cartes (dates, badge rôle, PAS niveau/formation) → Task 6. ✅
- Câblage skeleton/empty/fondu toutes pages → Task 7. ✅
- Invariant + runtime → Task 8. ✅
- GroupCard = badge (pas ajout du rôle, déjà présent) → Task 6 Step 4. ✅

**Placeholders :** aucun TBD. Les points « à confirmer au runtime/spike » (`animateItem` vs `animateItemPlacement`, `todayIso()` expect/actual vs `formatDate` seul) ont des replis explicites gardant le build vert.

**Cohérence des types :** `formatDate(iso)`/`relativeDate(iso, todayIso)` cohérents Task 1↔6 ; `SkeletonGrid(itemHeight,...)`/`SkeletonList(itemHeight,...)` cohérents Task 3↔7 ; `EmptyState(icon,title,message,actionLabel,onAction)` cohérent Task 4↔7.

**Risque connu :** Task 6 — `relativeDate` nécessite la date du jour ; dans un composant @Composable non testé, lire la date système est tolérable mais demande un `expect/actual todayIso()`. Repli net : se limiter à `formatDate` (toujours dispo, sans horloge) et n'utiliser `relativeDate` que si l'effort expect/actual est jugé justifié. Le build reste vert dans les deux cas.
