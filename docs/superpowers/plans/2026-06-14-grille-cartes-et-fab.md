# Grille de cartes + FAB — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sur « Campagnes » et « Mes fiches », remplacer le bouton texte du haut par un FAB en bas à droite, afficher les cartes en grille de tuiles ~carrées, et rendre les tuiles de fiches cliquables vers un nouvel écran détail de fiche minimal.

**Architecture:** Compose Desktop, clean archi, UI bête. On ajoute un atome `AppFab`, on convertit les 2 cartes en tuiles à hauteur fixe, on bascule les 2 pages de liste en `Box{ contenu + FAB }` avec `LazyVerticalGrid` adaptatif, et on ajoute une Route Nav3 `CharacterSheetDetail` + sa page minimale.

**Tech Stack:** Kotlin 2.2.20, Compose Multiplatform, Navigation 3, Koin, detekt + Kover.

**Spec:** `docs/superpowers/specs/2026-06-14-grille-cartes-et-fab-design.md`

**Branche :** `feat/campaigns`. Tous les chemins sont relatifs à `E-JDR-Frontend/`.

---

## Rappels d'environnement (lire avant de commencer)

- **Gradle** : `./gradlew` est réécrit en `rtk gradlew` par un hook. Lancer **UN seul** build à la fois, `--console=plain`, **sans** boucle de polling en parallèle (fige le daemon). Pour compiler vite : `./gradlew compileKotlin --console=plain`.
- **Kover** : aucune modif de `build.gradle.kts` nécessaire. Les exclusions sont **par package** : `eu.ejdr.presentation.shared.component` (couvre le nouvel `AppFab`), `...charactersheet.page` et `...campaign.page` (couvrent les pages), et la classe `CharacterSheetNavEntriesKt` est déjà exclue. Tout nouveau fichier UI tombe donc déjà dans une exclusion.
- **detekt** : `maxIssues: 0`. Respecter le style (lignes, imports triés). En cas de warning, corriger avant de committer.
- **Tokens DA** : `AppTheme.dimens` = `xs(4) sm(8) md(16) lg(24) xl(32) radiusSm(6) radiusMd(10) borderWidth(1.5) iconSize(20)`. `AppTheme.colors` = `primary`, `onPrimary`, `surface`, `background`, `border`, `danger`, `muted`, `textSecondary`, etc.
- **Pas de test unitaire** dans ce lot : c'est de l'UI pure (pas de ViewModel/logique ajoutés). La vérif = `./gradlew verify` vert + validation runtime par l'utilisateur.
- **Pièges Nav3** (Task 6) : toute nouvelle Route doit être enregistrée `subclass(...)` dans `appNavConfiguration` (Routes.kt) ET avoir une `entry<...>` dans le `*NavEntries.kt` de sa feature, sinon **crash au lancement** (non détecté par le build).

---

## File Structure

**Créés :**
- `src/main/kotlin/eu/ejdr/presentation/shared/component/atomic/AppFab.kt` — bouton flottant rond réutilisable.
- `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/page/CharacterSheetDetailPage.kt` — écran détail fiche minimal.

**Modifiés :**
- `.../presentation/features/campaign/component/CampaignCard.kt` — Row → tuile.
- `.../presentation/features/charactersheet/component/CharacterSheetCard.kt` — Row → tuile + `onClick` nullable.
- `.../presentation/features/campaign/page/CampaignListPage.kt` — Box + FAB + LazyVerticalGrid.
- `.../presentation/features/charactersheet/page/MyCharacterSheetsPage.kt` — idem + param `onOpenSheet`.
- `.../presentation/navigation/Routes.kt` — nouvelle Route `CharacterSheetDetail` + `subclass`.
- `.../presentation/features/charactersheet/CharacterSheetNavEntries.kt` — entry détail + passe `onOpenSheet`.

---

## Task 1 : Composant `AppFab`

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/shared/component/atomic/AppFab.kt`

- [ ] **Step 1 : Créer le composant**

`AppFab.kt` :
```kotlin
package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Bouton d'action flottant (FAB) de la direction artistique du site (composant bête).
 *
 * Rond, fond `primary`, icône « + » centrée. Réutilisable par tout écran ayant une action
 * de création principale (placé en bas à droite par l'appelant via un `Modifier.align`).
 *
 * @param onClick Callback déclenché au clic.
 * @param contentDescription Description d'accessibilité de l'action (ex. « Ajouter une fiche »).
 * @param modifier Modifier Compose appliqué au bouton (alignement/padding fournis par l'appelant).
 */
@Composable
fun AppFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = AppTheme.colors.primary,
        contentColor = AppTheme.colors.onPrimary,
    ) {
        AppIcon(
            imageVector = Icons.Filled.Add,
            contentDescription = contentDescription,
            modifier = Modifier.size(AppTheme.dimens.iconSize),
        )
    }
}
```
> Vérifier en lisant `shared/component/atomic/AppIcon.kt` que `AppIcon(imageVector, contentDescription, modifier, tint?)` accepte bien ces params ; sinon adapter (au pire utiliser `androidx.compose.material3.Icon` directement avec `tint = AppTheme.colors.onPrimary`).

- [ ] **Step 2 : Compiler**

Run: `./gradlew compileKotlin --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/shared/component/atomic/AppFab.kt
git commit -m "feat(design-system): add AppFab floating action button"
```

---

## Task 2 : `CampaignCard` en tuile ~carrée

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/campaign/component/CampaignCard.kt`

- [ ] **Step 1 : Réécrire la carte en tuile**

Remplacer le corps de `CampaignCard` (garder la signature `campaign, onClick, onDelete, modifier`). Nouveau contenu complet du fichier :
```kotlin
package eu.ejdr.presentation.features.campaign.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.campaign.entities.Campaign
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Tuile d'une campagne dans la grille (composant bête).
 *
 * Tuile à hauteur fixe (fond `surface`, bordure, coins arrondis) : nom centré, icône de
 * suppression en coin haut-droite. Toute la tuile est cliquable (ouvre le détail) ; le clic
 * sur l'icône de suppression remonte [onDelete] sans déclencher [onClick].
 *
 * @param campaign Campagne à afficher.
 * @param onClick Callback déclenché au clic sur la tuile (ouvre le détail).
 * @param onDelete Callback déclenché au clic sur l'icône de suppression.
 * @param modifier Modifier Compose appliqué à la tuile.
 */
@Composable
fun CampaignCard(
    campaign: Campaign,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .clickable(onClick = onClick),
    ) {
        AppText(
            text = campaign.name,
            style = AppTextStyle.Subtitle,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimens.md),
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            AppIcon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Supprimer la campagne",
                tint = AppTheme.colors.danger,
            )
        }
    }
}
```
> ⚠️ Vérifier en lisant `shared/component/atomic/AppText.kt` que `AppText` accepte `textAlign: TextAlign?` et `maxLines: Int`. Si `textAlign` n'existe pas comme paramètre, l'ajouter à `AppText` (param optionnel `textAlign: TextAlign? = null` transmis au `Text` sous-jacent) — c'est un ajout rétro-compatible. Si l'ajout à `AppText` est nécessaire, le mentionner dans le rapport.

- [ ] **Step 2 : Compiler**

Run: `./gradlew compileKotlin --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/campaign/component/CampaignCard.kt
# inclure AppText.kt SEULEMENT si modifié à l'étape 1
git commit -m "feat(campaign): render campaign card as a fixed-height tile"
```

---

## Task 3 : `CharacterSheetCard` en tuile + `onClick` nullable

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/component/CharacterSheetCard.kt`

- [ ] **Step 1 : Réécrire la carte en tuile avec clic optionnel**

Contenu complet du fichier (signature : ajout de `onClick: (() -> Unit)? = null`, `onDelete` reste nullable) :
```kotlin
package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ejdr.domain.features.charactersheet.entities.CharacterSheet
import eu.ejdr.presentation.shared.component.atomic.AppIcon
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Tuile d'une fiche de personnage (composant bête).
 *
 * Tuile à hauteur fixe (fond `surface`, bordure, coins arrondis) : nom centré, action de
 * suppression optionnelle en coin haut-droite. Quand [onClick] est `null`, la tuile n'est pas
 * cliquable (ex. liste en lecture seule du détail de campagne) ; quand [onDelete] est `null`,
 * l'icône de suppression n'est pas affichée.
 *
 * @param sheet Fiche à afficher.
 * @param onClick Callback de clic sur la tuile ; si `null`, la tuile n'est pas cliquable.
 * @param onDelete Callback de suppression ; si `null`, l'icône est masquée.
 * @param modifier Modifier Compose appliqué à la tuile.
 */
@Composable
fun CharacterSheetCard(
    sheet: CharacterSheet,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    val base = modifier
        .fillMaxWidth()
        .height(140.dp)
        .clip(shape)
        .background(AppTheme.colors.surface)
        .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
    ) {
        AppText(
            text = sheet.name,
            style = AppTextStyle.Subtitle,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimens.md),
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                AppIcon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer la fiche",
                    tint = AppTheme.colors.danger,
                )
            }
        }
    }
}
```
> Note : `CampaignDetailPage` appelle déjà `CharacterSheetCard(sheet, onDelete = …)` sans `onClick` ; avec `onClick` à `null` par défaut, l'appel reste valide et la tuile y est non cliquable. Ne PAS modifier `CampaignDetailPage` dans cette tâche.

- [ ] **Step 2 : Compiler (le code appelant existant doit toujours compiler)**

Run: `./gradlew compileKotlin --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/charactersheet/component/CharacterSheetCard.kt
git commit -m "feat(charactersheet): render sheet card as a clickable fixed-height tile"
```

---

## Task 4 : `CampaignListPage` — Box + FAB + grille

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/campaign/page/CampaignListPage.kt`

- [ ] **Step 1 : Remplacer la mise en page**

Remplacer le bloc `Column { AppButton ; FormError ; Box { … LazyColumn … } }` (≈ lignes 67-113) par un `Box` plein écran avec contenu + FAB. Le `@Composable fun CampaignListPage(...)` garde sa signature.

1. Mettre à jour les imports : retirer `AppButton`, `Column`, `LazyColumn`, `items` (lazy list), `Icons`/`Add` (le FAB porte l'icône), `Arrangement` si plus utilisé ; ajouter :
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import eu.ejdr.presentation.shared.component.atomic.AppFab
```
(garder `Box`, `Column`, `fillMaxSize`, `padding`, `Alignment`, `AppText`, `AppTextStyle`, `FormError`, `CircularProgressIndicator`, `CampaignCard`.)

2. Remplacer le corps (à partir de `Column(modifier = modifier...`) par :
```kotlin
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimens.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        ) {
            FormError(message = error)

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && campaigns.isEmpty() ->
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppTheme.colors.primary,
                        )

                    campaigns.isEmpty() ->
                        AppText(
                            text = "Aucune campagne pour le moment.",
                            style = AppTextStyle.Body,
                            color = AppTheme.colors.muted,
                            modifier = Modifier.align(Alignment.Center),
                        )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = AppTheme.dimens.sm,
                            bottom = 96.dp, // espace pour ne pas masquer la dernière rangée sous le FAB
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                    ) {
                        items(campaigns, key = { it.id }) { campaign ->
                            CampaignCard(
                                campaign = campaign,
                                onClick = { onOpenCampaign(campaign.id, campaign.name) },
                                onDelete = { pendingDelete = campaign },
                            )
                        }
                    }
                }
            }
        }

        AppFab(
            onClick = { showCreate = true },
            contentDescription = "Ajouter une campagne",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppTheme.dimens.xl),
        )
    }
```
(les blocs `if (showCreate) { … }` et `pendingDelete?.let { … }` en dessous restent inchangés.)

- [ ] **Step 2 : Compiler**

Run: `./gradlew compileKotlin --console=plain`
Expected: BUILD SUCCESSFUL. Si un import inutilisé reste (detekt le refusera plus tard), le retirer.

- [ ] **Step 3 : Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/campaign/page/CampaignListPage.kt
git commit -m "feat(campaign): grid layout with bottom-right FAB on campaigns list"
```

---

## Task 5 : `MyCharacterSheetsPage` — Box + FAB + grille + `onOpenSheet`

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/page/MyCharacterSheetsPage.kt`

- [ ] **Step 1 : Ajouter le param `onOpenSheet` à la signature**

Changer la signature :
```kotlin
@Composable
fun MyCharacterSheetsPage(
    onOpenSheet: (id: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
```
Mettre à jour la KDoc (`@param onOpenSheet Callback d'ouverture du détail d'une fiche (id + nom).`).

- [ ] **Step 2 : Remplacer la mise en page (mêmes imports/grille que Task 4)**

Mêmes ajustements d'imports qu'à la Task 4 (ajouter `Box`/`Column`/`PaddingValues`/`fillMaxSize`/`padding`/`GridCells`/`LazyVerticalGrid`/`grid.items`/`Arrangement`/`dp`/`AppFab` ; retirer `AppButton`, `LazyColumn`, lazy `items`, `Icons`/`Add` si inutilisés).

Remplacer le corps (à partir de `Column(modifier = modifier...`) par :
```kotlin
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimens.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        ) {
            FormError(message = error)

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading && sheets.isEmpty() ->
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = AppTheme.colors.primary,
                        )

                    sheets.isEmpty() ->
                        AppText(
                            text = "Aucune fiche pour le moment.",
                            style = AppTextStyle.Body,
                            color = AppTheme.colors.muted,
                            modifier = Modifier.align(Alignment.Center),
                        )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = AppTheme.dimens.sm,
                            bottom = 96.dp, // espace pour ne pas masquer la dernière rangée sous le FAB
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
                    ) {
                        items(sheets, key = { it.id }) { sheet ->
                            CharacterSheetCard(
                                sheet = sheet,
                                onClick = { onOpenSheet(sheet.id, sheet.name) },
                                onDelete = { pendingDelete = sheet },
                            )
                        }
                    }
                }
            }
        }

        AppFab(
            onClick = { showCreate = true },
            contentDescription = "Ajouter une fiche",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppTheme.dimens.xl),
        )
    }
```
(les blocs `if (showCreate)` et `pendingDelete?.let` restent inchangés.)

- [ ] **Step 3 : Compiler — ATTENTION**

Le `characterSheetEntries` (Task 6) doit fournir `onOpenSheet`. Tant que Task 6 n'est pas faite, l'appelant `MyCharacterSheetsPage()` dans `CharacterSheetNavEntries.kt` ne compile plus (param manquant). **Faire la Task 6 immédiatement après**, puis compiler. Si on veut compiler isolément cette tâche, c'est attendu d'échouer sur `CharacterSheetNavEntries.kt` jusqu'à la Task 6.

Run (après Task 6) : `./gradlew compileKotlin --console=plain` → BUILD SUCCESSFUL.

- [ ] **Step 4 : Commit (groupé avec Task 6 si nécessaire pour garder le build vert)**

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/charactersheet/page/MyCharacterSheetsPage.kt
git commit -m "feat(charactersheet): grid layout with FAB + open sheet detail on tile click"
```

---

## Task 6 : Route + page détail fiche + câblage nav

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/page/CharacterSheetDetailPage.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/navigation/Routes.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/CharacterSheetNavEntries.kt`

- [ ] **Step 1 : Ajouter la Route + son `subclass`**

Dans `navigation/Routes.kt` :
1. Après `data object CharacterSheets : Route` (≈ ligne 59), ajouter (modèle de `CampaignDetail`) :
```kotlin
    /** Détail d'une fiche de personnage (le nom voyage dans la clé). */
    @Serializable
    data class CharacterSheetDetail(val id: String, val name: String) : Route
```
2. Dans `appNavConfiguration`, après `subclass(Route.CharacterSheets::class)` (≈ ligne 86), ajouter :
```kotlin
            subclass(Route.CharacterSheetDetail::class)
```
> ⚠️ Sans ce `subclass`, l'app **crashe au démarrage** dès qu'on navigue (ou au restore). Indispensable.

- [ ] **Step 2 : Créer la page détail minimale**

`CharacterSheetDetailPage.kt` :
```kotlin
package eu.ejdr.presentation.features.charactersheet.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Page détail d'une fiche de personnage (composant INTELLIGENT minimal).
 *
 * Pour l'instant elle n'affiche que le nom de la fiche (aucun appel réseau : le nom voyage
 * dans la clé de navigation). À enrichir ultérieurement.
 *
 * @param id Identifiant de la fiche (non affiché pour l'instant ; réservé à l'enrichissement futur).
 * @param name Nom de la fiche (affiché en titre).
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun CharacterSheetDetailPage(
    id: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.dimens.xl),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = name, style = AppTextStyle.Title)
    }
}
```
> `id` est inutilisé pour l'instant. detekt peut refuser un paramètre inutilisé : si c'est le cas, le préfixer `@Suppress("UnusedParameter")` sur la fonction, OU afficher `id` discrètement. Vérifier la règle detekt et adapter ; le plus simple si detekt râle : ajouter `@Suppress("UnusedParameter")` au-dessus de `fun CharacterSheetDetailPage`. (Le `CampaignDetailPage` minimal d'origine gardait `id` utilisé via le ViewModel — ici pas de VM, donc ce point peut se poser.)

- [ ] **Step 3 : Câbler la nav entry + fournir `onOpenSheet`**

Dans `CharacterSheetNavEntries.kt`, contenu complet :
```kotlin
package eu.ejdr.presentation.features.charactersheet

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.charactersheet.page.CharacterSheetDetailPage
import eu.ejdr.presentation.features.charactersheet.page.MyCharacterSheetsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entries de navigation de la feature fiches (liste « Mes fiches » + détail d'une fiche). */
fun EntryProviderScope<Any>.characterSheetEntries(actions: NavActions) {
    entry<Route.CharacterSheets> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Mes fiches",
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            MyCharacterSheetsPage(
                onOpenSheet = { id, name -> actions.backStack.add(Route.CharacterSheetDetail(id, name)) },
            )
        }
    }
    entry<Route.CharacterSheetDetail> { key ->
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = key.name,
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            CharacterSheetDetailPage(id = key.id, name = key.name)
        }
    }
}
```
> Vérifier en lisant `CampaignNavEntries.kt` la forme EXACTE attendue par Nav3 ici : `entry<T> { key -> ... }` reçoit la clé typée (`key.id`, `key.name`). Si la signature réelle diffère (receiver, nom du lambda), s'aligner sur `CampaignNavEntries.kt` (la `Route.CampaignDetail` y est câblée pareil). Vérifier aussi que `actions.backStack.add(...)` est la bonne API (modèle : comment `campaignEntries` pousse `Route.CampaignDetail`).

- [ ] **Step 4 : Compiler (tout doit compiler maintenant, y compris Task 5)**

Run: `./gradlew compileKotlin --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5 : Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/navigation/Routes.kt src/main/kotlin/eu/ejdr/presentation/features/charactersheet/page/CharacterSheetDetailPage.kt src/main/kotlin/eu/ejdr/presentation/features/charactersheet/CharacterSheetNavEntries.kt
# si Task 5 n'a pas encore été commitée car le build n'était pas vert, l'inclure ici
git commit -m "feat(charactersheet): add minimal sheet detail screen + nav route"
```

---

## Task 7 : Vérification finale

**Files:** aucun (vérification).

- [ ] **Step 1 : `verify` (detekt + tests + Kover)**

Run: `./gradlew verify --console=plain`
Expected: BUILD SUCCESSFUL, 0 warning detekt, Kover ≥ 60 %.
> Si detekt signale des imports inutilisés (très probable après les remaniements de pages), les retirer puis relancer. Si Kover baisse, vérifier qu'aucun nouveau fichier n'est hors des packages exclus — normalement `AppFab` (shared.component), les pages (`*.page`) et les nav entries sont déjà exclus, donc aucune action attendue.

- [ ] **Step 2 : Pas de commit** (vérification seule ; les corrections detekt éventuelles se committent avec un message `style:` ou `fix:` dédié).

---

## VALIDATION RUNTIME (utilisateur — non automatisable)

`./gradlew run --console=plain` (backend up), puis valider visuellement :
1. La fenêtre s'ouvre **sans crash** (Route `CharacterSheetDetail` bien enregistrée — piège Nav3).
2. **Campagnes** : tuiles en grille sur plusieurs colonnes ; **FAB en bas à droite** ; clic FAB → dialog création ; clic tuile → détail campagne ; poubelle → confirmation suppression.
3. **Mes fiches** : tuiles en grille ; FAB ; clic FAB → création ; **clic tuile → écran détail fiche** (titre = nom) → retour.
4. **Redimensionner** la fenêtre → le nombre de colonnes s'adapte (largeur mini ~180dp).
5. Ajuster au besoin `minSize` (180.dp) et la hauteur de tuile (140.dp) selon le rendu.

---

## Notes de self-review

- **Couverture spec** : FAB bas-droite (Tasks 1,4,5) ✔ ; grille adaptative (Tasks 4,5) ✔ ; tuiles ~carrées nom centré + poubelle coin (Tasks 2,3) ✔ ; clic fiche → détail (Tasks 3,5,6) ✔ ; nouvel écran détail fiche + Route Nav3 + 2 pièges (Task 6) ✔ ; Kover inchangé, justifié (Task 7 + rappels) ✔ ; pas de test unitaire car UI pure ✔ ; validation runtime déléguée ✔ ; `CampaignDetailPage` non touché mais sa carte devient tuile (accepté, spec §4) ✔.
- **Cohérence de noms** : `AppFab(onClick, contentDescription, modifier)` ; `CharacterSheetCard(sheet, onClick?=null, onDelete?, modifier)` ; `CampaignCard(campaign, onClick, onDelete, modifier)` ; `MyCharacterSheetsPage(onOpenSheet, modifier)` ; `Route.CharacterSheetDetail(id, name)` ; `CharacterSheetDetailPage(id, name, modifier)`. Cohérents entre tâches.
- **Dépendance d'ordre** : Task 5 casse temporairement la compilation jusqu'à Task 6 (param `onOpenSheet`). Noté explicitement — faire 5 puis 6 d'affilée, committer une fois le build vert.
