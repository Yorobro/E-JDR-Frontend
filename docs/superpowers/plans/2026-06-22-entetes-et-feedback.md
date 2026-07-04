# En-têtes & feedback (Lot 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ajouter un feedback global après chaque action (snackbar succès/erreur), des en-têtes de contenu (titre + sous-titre contextuel + action qui remplace le FAB), et une validation progressive des formulaires.

**Architecture:** Un `UiMessageBus` singleton injecté par Koin (réplique exacte du pattern `InvalidationBus` : interface en `application/`, impl `MutableSharedFlow` en `infrastructure/`, enregistrée `single<>` dans un module). Les 6 ViewModels create/delete reçoivent le bus et émettent un `UiMessage` (succès/erreur). Un `UiMessageHost` à la racine de l'UI collecte le flux et affiche un `AppSnackbar` animé via `AppMotion`. Un `PageHeader` est câblé sur les écrans principaux (l'action remplace le FAB).

**Tech Stack:** Kotlin Multiplatform 2.2.20, Compose 1.8.2, Koin, coroutines (`MutableSharedFlow`), JUnit5 + MockK + coroutines-test.

## Global Constraints

- **Branche :** `feat/entetes-et-feedback` depuis **`feat/cartes-et-listes`** (Lot 3 empilé sur Lot 2). Voir Task 0.
- **Vérif :** `./gradlew.bat verifyDesktop --no-daemon` vert à chaque tâche.
- **Commitlint strict :** sujet de commit en minuscule, Conventional Commits.
- **Max 500 lignes/fichier** ; **tests en `src/desktopTest/`** (JUnit5, MockK, kotlinx-coroutines-test).
- **Invariant mouvement :** l'animation du snackbar dérive ses durées d'`AppMotion` ; aucune durée/courbe en dur hors `AppMotion.kt`.
- **DI :** `UiMessageBus` injecté Koin en `single<>` (façon `InvalidationBus` dans `RealtimeModule.kt`).
- **Ne pas toucher** : `AppPalette`, 3 thèmes, socle Lot 1, cartes/listes Lot 2, backend, la logique des use cases (on ajoute SEULEMENT l'émission d'événements UI dans les ViewModels).

---

## File Structure

**Créés :**
- `src/commonMain/.../presentation/shared/feedback/UiMessage.kt` — donnée + fabriques (pur).
- `src/commonMain/.../application/shared/feedback/UiMessageBus.kt` — interface.
- `src/commonMain/.../infrastructure/feedback/InMemoryUiMessageBus.kt` — impl SharedFlow.
- `src/commonMain/.../di/FeedbackModule.kt` — `single<UiMessageBus>`.
- `src/commonMain/.../presentation/shared/component/organism/AppSnackbar.kt` — UI snackbar.
- `src/commonMain/.../presentation/shared/feedback/UiMessageHost.kt` — hôte global @Composable.
- `src/commonMain/.../presentation/shared/component/organism/PageHeader.kt` — en-tête de contenu.
- Tests : `UiMessageTest.kt`, `InMemoryUiMessageBusTest.kt`, `CampaignListViewModelTest.kt` (compléter).

**Modifiés :**
- Les 6 ViewModels create/delete (ajout dép `UiMessageBus` + emit).
- Leurs pages (passer `get<UiMessageBus>()` au constructeur via `koinViewModel { }`).
- `App.kt` desktop + android (monter `UiMessageHost`).
- Les modules Koin (enregistrer `FeedbackModule`).
- Écrans principaux : `PageHeader` + retrait du FAB.
- Dialogs de création : validation progressive.

---

## Task 0 : Préparer la branche

- [ ] **Step 1 : Créer la branche depuis feat/cartes-et-listes**

```bash
git switch feat/cartes-et-listes
git switch -c feat/entetes-et-feedback
```

- [ ] **Step 2 : Vérifier l'état de départ vert**

Run: `./gradlew.bat verifyDesktop --no-daemon`
Expected: BUILD SUCCESSFUL.

---

## Task 1 : `UiMessage` (donnée pure)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/feedback/UiMessage.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/shared/feedback/UiMessageTest.kt`

**Interfaces:**
- Produces: `data class UiMessage(val text: String, val tone: UiMessageTone)` ; `enum class UiMessageTone { SUCCESS, ERROR }` ; `UiMessage.success(text)`, `UiMessage.error(text)`. Consommé par Tasks 2-5.

- [ ] **Step 1 : Écrire le test**

```kotlin
package eu.ejdr.presentation.shared.feedback

import kotlin.test.Test
import kotlin.test.assertEquals

class UiMessageTest {
    @Test
    fun `success produit le ton SUCCESS`() {
        val m = UiMessage.success("Campagne créée")
        assertEquals("Campagne créée", m.text)
        assertEquals(UiMessageTone.SUCCESS, m.tone)
    }

    @Test
    fun `error produit le ton ERROR`() {
        val m = UiMessage.error("Échec réseau")
        assertEquals(UiMessageTone.ERROR, m.tone)
    }
}
```

- [ ] **Step 2 : Lancer, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.feedback.UiMessageTest" --no-daemon`
Expected: FAIL — non défini.

- [ ] **Step 3 : Implémenter**

```kotlin
package eu.ejdr.presentation.shared.feedback

/** Tonalité d'un message UI transitoire. */
enum class UiMessageTone { SUCCESS, ERROR }

/**
 * Message UI transitoire à présenter à l'utilisateur (snackbar).
 *
 * @property text Texte affiché (dans la voix de l'app).
 * @property tone Tonalité visuelle (succès / erreur).
 */
data class UiMessage(val text: String, val tone: UiMessageTone) {
    companion object {
        fun success(text: String) = UiMessage(text, UiMessageTone.SUCCESS)
        fun error(text: String) = UiMessage(text, UiMessageTone.ERROR)
    }
}
```

- [ ] **Step 4 : Lancer, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.shared.feedback.UiMessageTest" --no-daemon`
Expected: PASS (2 tests).

- [ ] **Step 5 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/feedback/UiMessage.kt src/desktopTest/kotlin/eu/ejdr/presentation/shared/feedback/UiMessageTest.kt
git commit -m "feat: type uimessage pour le feedback transitoire"
```

---

## Task 2 : `UiMessageBus` (interface + impl + DI)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/application/shared/feedback/UiMessageBus.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/infrastructure/feedback/InMemoryUiMessageBus.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/di/FeedbackModule.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/infrastructure/feedback/InMemoryUiMessageBusTest.kt`

**Interfaces:**
- Consumes: `UiMessage` (Task 1).
- Produces: `interface UiMessageBus { val messages: Flow<UiMessage>; fun emit(message: UiMessage) }` ; `class InMemoryUiMessageBus : UiMessageBus` ; `val feedbackModule: Module`. Consommé par Tasks 3-5.

> `emit` est NON-suspend (via `tryEmit` sur `MutableSharedFlow`) pour être appelable directement dans le `fold` non-suspend des ViewModels. Pattern calqué sur `InMemoryInvalidationBus` (replay=0, extraBufferCapacity).

- [ ] **Step 1 : Écrire le test**

```kotlin
package eu.ejdr.infrastructure.feedback

import eu.ejdr.presentation.shared.feedback.UiMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryUiMessageBusTest {
    @Test
    fun `un message emis est recu par les abonnes`() = runTest {
        val bus = InMemoryUiMessageBus()
        val received = async { bus.messages.first() }
        // laisser l'abonnement s'installer
        kotlinx.coroutines.yield()
        bus.emit(UiMessage.success("ok"))
        assertEquals("ok", received.await().text)
    }
}
```

- [ ] **Step 2 : Lancer, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.infrastructure.feedback.InMemoryUiMessageBusTest" --no-daemon`
Expected: FAIL — non défini.

- [ ] **Step 3 : Implémenter l'interface**

`UiMessageBus.kt` :
```kotlin
package eu.ejdr.application.shared.feedback

import eu.ejdr.presentation.shared.feedback.UiMessage
import kotlinx.coroutines.flow.Flow

/**
 * Bus applicatif des messages UI transitoires : les ViewModels y publient les retours
 * d'action (succès/erreur), un hôte global les observe pour afficher un snackbar.
 */
interface UiMessageBus {
    /** Flux des messages (chaud : seuls les abonnés au moment de l'émission reçoivent). */
    val messages: Flow<UiMessage>

    /** Publie un message à destination de l'hôte de snackbar. Non bloquant. */
    fun emit(message: UiMessage)
}
```

`InMemoryUiMessageBus.kt` :
```kotlin
package eu.ejdr.infrastructure.feedback

import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.presentation.shared.feedback.UiMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus de messages UI en mémoire (SharedFlow). `replay = 0` : un message n'a de sens que pour
 * un hôte actuellement monté ; `extraBufferCapacity` évite de bloquer l'émetteur (emit non-suspend).
 */
class InMemoryUiMessageBus : UiMessageBus {
    private val mutableMessages =
        MutableSharedFlow<UiMessage>(replay = 0, extraBufferCapacity = 16)
    override val messages: Flow<UiMessage> = mutableMessages.asSharedFlow()

    override fun emit(message: UiMessage) {
        mutableMessages.tryEmit(message)
    }
}
```

`FeedbackModule.kt` :
```kotlin
package eu.ejdr.di

import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.infrastructure.feedback.InMemoryUiMessageBus
import org.koin.dsl.module

/** Module Koin du feedback UI : le bus de messages transitoires (singleton). */
val feedbackModule = module {
    single<UiMessageBus> { InMemoryUiMessageBus() }
}
```

- [ ] **Step 4 : Enregistrer le module dans l'app Koin**

Localiser où les modules sont agrégés (chercher `realtimeModule` ou `modules(` dans `di/`). Ajouter `feedbackModule` à la liste des modules chargés (desktop + android si séparés). Run: `grep -rn "realtimeModule\|modules(" src/commonMain/kotlin/eu/ejdr/di src/desktopMain/kotlin/eu/ejdr/di src/androidMain/kotlin/eu/ejdr/di`.

- [ ] **Step 5 : Lancer, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.infrastructure.feedback.InMemoryUiMessageBusTest" --no-daemon`
Expected: PASS.

- [ ] **Step 6 : Vérif complète + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/application/shared/feedback/UiMessageBus.kt src/commonMain/kotlin/eu/ejdr/infrastructure/feedback/InMemoryUiMessageBus.kt src/commonMain/kotlin/eu/ejdr/di/FeedbackModule.kt src/desktopTest/kotlin/eu/ejdr/infrastructure/feedback/InMemoryUiMessageBusTest.kt
# + le fichier d'agrégation des modules modifié
git commit -m "feat: bus de messages ui injecte (uimessagebus + module koin)"
```

---

## Task 3 : `AppSnackbar` + `UiMessageHost`

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/organism/AppSnackbar.kt`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/feedback/UiMessageHost.kt`

**Interfaces:**
- Consumes: `UiMessage`, `UiMessageTone` (Task 1), `UiMessageBus` (Task 2), `AppTheme`.
- Produces: `@Composable fun AppSnackbar(message: UiMessage, modifier: Modifier = Modifier)` ; `@Composable fun UiMessageHost(bus: UiMessageBus, modifier: Modifier = Modifier)`. Consommé par Task 4.

- [ ] **Step 1 : `AppSnackbar.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.feedback.UiMessage
import eu.ejdr.presentation.shared.feedback.UiMessageTone
import eu.ejdr.presentation.shared.theme.AppTheme

/** Bandeau transitoire de feedback (succès/erreur), couleurs dérivées du ton. */
@Composable
fun AppSnackbar(message: UiMessage, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val container = if (message.tone == UiMessageTone.ERROR) colors.danger else colors.primary
    val content = if (message.tone == UiMessageTone.ERROR) colors.onDanger else colors.onPrimary
    AppText(
        text = message.text,
        style = AppTextStyle.Body,
        color = content,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimens.radiusMd))
            .background(container)
            .padding(AppTheme.dimens.md),
    )
}
```

- [ ] **Step 2 : `UiMessageHost.kt`** (collecte le bus + anime l'apparition)

```kotlin
package eu.ejdr.presentation.shared.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.presentation.shared.component.organism.AppSnackbar
import eu.ejdr.presentation.shared.theme.AppTheme
import kotlinx.coroutines.delay

private const val SNACKBAR_VISIBLE_MS = 3000L

/**
 * Hôte global du feedback : observe [bus], affiche le dernier message en snackbar animé
 * (slide+fade depuis le bas), auto-dismiss. Le suivant remplace le courant.
 */
@Composable
fun UiMessageHost(bus: UiMessageBus, modifier: Modifier = Modifier) {
    val motion = AppTheme.motion
    var current by remember { mutableStateOf<eu.ejdr.presentation.shared.feedback.UiMessage?>(null) }

    LaunchedEffect(bus) {
        bus.messages.collect { current = it }
    }
    LaunchedEffect(current) {
        if (current != null) {
            delay(SNACKBAR_VISIBLE_MS)
            current = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val msg = current
        AnimatedVisibility(
            visible = msg != null,
            enter = slideInVertically(tween(motion.effectiveDuration(motion.durationMedium))) { it } +
                fadeIn(tween(motion.effectiveDuration(motion.durationMedium))),
            exit = slideOutVertically(tween(motion.effectiveDuration(motion.durationMedium))) { it } +
                fadeOut(tween(motion.effectiveDuration(motion.durationMedium))),
            modifier = Modifier.align(Alignment.BottomCenter).padding(AppTheme.dimens.lg),
        ) {
            if (msg != null) AppSnackbar(message = msg)
        }
    }
}
```

- [ ] **Step 3 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/organism/AppSnackbar.kt src/commonMain/kotlin/eu/ejdr/presentation/shared/feedback/UiMessageHost.kt
git commit -m "feat: appsnackbar et hote global de feedback anime"
```

---

## Task 4 : Monter `UiMessageHost` à la racine (desktop + android)

**Files:**
- Modify: `src/desktopMain/.../presentation/App.kt`
- Modify: `src/androidMain/.../presentation/App.kt`

**Interfaces:**
- Consumes: `UiMessageHost` (Task 3), `UiMessageBus` (Task 2, via `koinInject`).

- [ ] **Step 1 : Envelopper l'UI avec l'hôte**

Dans chaque `App.kt`, après le contenu de navigation (à l'intérieur de `AppTheme { ... }`), superposer l'hôte. Pattern : envelopper le contenu existant dans un `Box`, et ajouter `UiMessageHost(bus = koinInject<UiMessageBus>())` par-dessus (il occupe `fillMaxSize` et n'affiche que le snackbar en bas, donc n'intercepte rien). Importer `eu.ejdr.application.shared.feedback.UiMessageBus`, `eu.ejdr.presentation.shared.feedback.UiMessageHost`, `org.koin.compose.koinInject`.

Exemple (desktop, structurel — adapter à la structure réelle d'App.kt) :
```kotlin
Box(Modifier.fillMaxSize()) {
    AppNavDisplay( ... )           // contenu existant
    UiMessageHost(bus = koinInject())
    // (les dialogs d'update existants restent où ils sont)
}
```

- [ ] **Step 2 : Vérif desktop + android**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
Run: `./gradlew.bat compileDebugKotlinAndroid --no-daemon` → BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add src/desktopMain/kotlin/eu/ejdr/presentation/App.kt src/androidMain/kotlin/eu/ejdr/presentation/App.kt
git commit -m "feat: monter l'hote de feedback a la racine de l'app"
```

---

## Task 5 : Émettre les messages depuis les 6 ViewModels

**Files (Modify):**
- `campaign/CampaignListViewModel.kt`, `campaign/CampaignDetailViewModel.kt`, `charactersheet/MyCharacterSheetsViewModel.kt`, `friendgroup/GroupListViewModel.kt`, `reference/ReferenceListViewModel.kt`, `session/SessionDetailViewModel.kt`.
- Leurs pages (passer `get<UiMessageBus>()` au constructeur du ViewModel via `koinViewModel { }`).
- Test: compléter `src/desktopTest/.../campaign/CampaignListViewModelTest.kt`.

**Interfaces:**
- Consumes: `UiMessageBus` (Task 2), `UiMessage` (Task 1).

- [ ] **Step 1 : Compléter le test de `CampaignListViewModel`**

Lire le test existant. Ajouter (avec un `UiMessageBus` mocké MockK) :
```kotlin
    @Test
    fun `une creation reussie emet un message de succes`() = runTest {
        val bus = mockk<UiMessageBus>(relaxed = true)
        // construire le VM avec createCampaign mocké → succès, bus injecté
        // ... (suivre le pattern d'instanciation du test existant)
        vm.create("Ma campagne")
        advanceUntilIdle()
        verify { bus.emit(match { it.tone == UiMessageTone.SUCCESS }) }
    }

    @Test
    fun `une creation en echec emet un message d'erreur`() = runTest {
        val bus = mockk<UiMessageBus>(relaxed = true)
        // createCampaign mocké → Result.Failure
        vm.create("X")
        advanceUntilIdle()
        verify { bus.emit(match { it.tone == UiMessageTone.ERROR }) }
    }
```
(Adapter aux noms/fixtures réels du test existant ; `relaxed = true` pour ne pas avoir à stubber `emit`.)

- [ ] **Step 2 : Lancer le test, vérifier l'échec**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.features.campaign.CampaignListViewModelTest" --no-daemon`
Expected: FAIL (le VM ne prend pas encore le bus / n'émet pas).

- [ ] **Step 3 : Modifier les 6 ViewModels**

Pour chacun : ajouter `private val uiMessageBus: UiMessageBus` au constructeur, et dans chaque `fold` :
- `onSuccess` → `uiMessageBus.emit(UiMessage.success("<libellé>"))` (libellés contextuels : « Campagne créée »/« Campagne supprimée », « Fiche créée »/« Fiche supprimée », « Groupe créé »/« Groupe supprimé », « Élément créé »/« Élément supprimé », « Session enregistrée »/« Session supprimée », etc.).
- `onFailure` → garder `_error.value = ...` ET ajouter `uiMessageBus.emit(UiMessage.error(error.message))`.

imports : `eu.ejdr.application.shared.feedback.UiMessageBus`, `eu.ejdr.presentation.shared.feedback.UiMessage`.

- [ ] **Step 4 : Brancher le bus dans les pages**

Dans chaque page qui construit le ViewModel via `koinViewModel { XxxViewModel(..., get<...>()) }`, ajouter `get<UiMessageBus>()` à l'appel du constructeur (à la bonne position). Vérifier les pages desktop ET android de chaque feature.

- [ ] **Step 5 : Lancer les tests, vérifier le succès**

Run: `./gradlew.bat desktopTest --tests "eu.ejdr.presentation.features.campaign.CampaignListViewModelTest" --no-daemon`
Expected: PASS. Puis `verifyDesktop` complet → tous les tests des 6 VM verts (les tests existants doivent compiler avec le nouveau paramètre — ajouter un bus mocké/relaxed là où ils instancient les VM).

- [ ] **Step 6 : Vérif desktop + android + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
Run: `./gradlew.bat compileDebugKotlinAndroid --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/features src/desktopMain/kotlin/eu/ejdr/presentation/features src/androidMain/kotlin/eu/ejdr/presentation/features src/desktopTest
git commit -m "feat: emettre un message de feedback sur creation et suppression"
```

---

## Task 6 : `PageHeader` + câblage (action remplace le FAB)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/organism/PageHeader.kt`
- Modify: les écrans principaux (campagnes, fiches, groupes, références — desktop + android).

**Interfaces:**
- Consumes: `AppText`, `AppTheme`.
- Produces: `@Composable fun PageHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, action: (@Composable () -> Unit)? = null)`.

- [ ] **Step 1 : Créer `PageHeader.kt`**

```kotlin
package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * En-tête de contenu d'un écran (sous la top bar) : titre, sous-titre contextuel optionnel,
 * et action principale optionnelle alignée à droite. Composant bête.
 */
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = AppTheme.dimens.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.xs)) {
            AppText(text = title, style = AppTextStyle.Title)
            if (subtitle != null) {
                AppText(text = subtitle, style = AppTextStyle.Caption, color = AppTheme.colors.textSecondary)
            }
        }
        if (action != null) action()
    }
}
```

- [ ] **Step 2 : Câbler sur les 4 écrans principaux (desktop + android)**

Pour chaque écran de liste principal (campagnes, fiches, groupes, références) :
1. Ajouter en haut du `Column` de contenu : `PageHeader(title = "<titre>", subtitle = <contexte>, action = { AppButton(label = "<Créer…>", onClick = <ancienne action FAB>, leadingIcon = Icons.Default.Add) })`.
   - Sous-titre contextuel : utiliser le nom du groupe actif (via `ActiveGroupState` déjà injecté) + le compteur de la liste (ex. `"$groupName · ${list.size} fiches"`). Si le nom du groupe n'est pas trivialement accessible, se limiter au compteur (ex. `"${list.size} campagnes"`).
   - **CTA conditionnel** : sur campagnes/références, ne passer `action` que si `canEdit` (sinon `action = null`).
2. **Retirer le `AppFab(...)`** de l'écran (et son `Box`/`align(BottomEnd)` s'il ne sert plus qu'à ça). L'action de création est désormais dans le `PageHeader`.
3. Garder l'état `showCreate`/dialogs inchangé — seul le déclencheur passe du FAB au bouton d'en-tête.

- [ ] **Step 3 : Vérif desktop + android**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL (detekt : retirer l'import `AppFab` devenu inutilisé).
Run: `./gradlew.bat compileDebugKotlinAndroid --no-daemon` → BUILD SUCCESSFUL.

- [ ] **Step 4 : Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/organism/PageHeader.kt src/desktopMain/kotlin/eu/ejdr/presentation/features src/androidMain/kotlin/eu/ejdr/presentation/features
git commit -m "feat: en-tete de page avec action principale (remplace le fab)"
```

---

## Task 7 : Validation progressive des dialogs de création

**Files:**
- Modify: les dialogs de création (`CreateCampaignDialog`, `CreateCharacterSheetDialog`, `CreateGroupDialog`, le dialog de référence, etc.).

**Interfaces:**
- Consumes: `AppTextField` (slot `errorMessage` existant).

- [ ] **Step 1 : Pour chaque dialog de création**

Ajouter un état « touché » et un message d'aide :
```kotlin
var name by remember { mutableStateOf("") }
var touched by remember { mutableStateOf(false) }
val error = if (touched && name.isBlank()) "Le nom ne peut pas être vide" else null
AppTextField(
    value = name,
    onValueChange = { name = it; touched = true },
    label = "...",
    errorMessage = error,
    modifier = Modifier.fillMaxWidth(),
)
```
Le `confirmEnabled` du `AppDialog` reste branché sur `name.isNotBlank()` (déjà le cas). Le message n'apparaît qu'après une première frappe (`touched`), pas sur champ vierge.

- [ ] **Step 2 : Vérif + commit**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL.
```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/features
git commit -m "feat: validation progressive des formulaires de creation"
```

---

## Task 8 : Invariant + validation runtime

- [ ] **Step 1 : Invariant mouvement**

Run (Grep) : motif `tween\(|CubicBezierEasing|spring\(` hors `AppMotion.kt`.
Attendu : l'animation du snackbar (UiMessageHost) dérive ses durées de `motion.effectiveDuration(...)`. Aucun littéral de durée, aucune `CubicBezierEasing` hors `AppMotion.kt`.

- [ ] **Step 2 : Lancer l'app et valider**

Run: `./gradlew.bat run --no-daemon`
Vérifier : créer une campagne/fiche → snackbar « Créé ✓ » apparaît en bas (slide+fade) puis disparaît ; supprimer → message ; provoquer une erreur (ex. nom invalide côté serveur) → snackbar erreur (couleur danger). En-têtes affichés (titre + sous-titre + bouton à droite, plus de FAB). Validation : ouvrir un dialog, taper puis effacer → message d'aide apparaît. Tester sur les 3 thèmes.

- [ ] **Step 3 : Vérif finale**

Run: `./gradlew.bat verifyDesktop --no-daemon` → BUILD SUCCESSFUL, koverVerify ≥ 60 %.

- [ ] **Step 4 : Commit final**

```bash
git add -A
git commit -m "fix: ajustements feedback et en-tetes apres validation runtime"
```

---

## Self-Review (rempli)

**Couverture spec :**
- `UiMessage` (donnée + fabriques) → Task 1. ✅
- `UiMessageBus` (interface + impl SharedFlow + DI Koin) → Task 2. ✅
- `AppSnackbar` + `UiMessageHost` (animé AppMotion) → Task 3. ✅
- Hôte monté à la racine desktop+android → Task 4. ✅
- Émission depuis les 6 ViewModels (+ tests) → Task 5. ✅
- `PageHeader` + action remplace le FAB + CTA conditionnel → Task 6. ✅
- Validation progressive → Task 7. ✅
- Invariant + runtime → Task 8. ✅

**Placeholders :** aucun TBD. Les points « adapter à la structure réelle » (App.kt, fixtures de test, position du `get<UiMessageBus>()`) sont des instructions d'intégration précises, pas des trous — le code de référence est fourni.

**Cohérence des types :** `UiMessage`/`UiMessageTone` + fabriques cohérents Task 1↔3↔5 ; `UiMessageBus { messages: Flow ; emit }` cohérent Task 2↔3↔5 ; `PageHeader(title, subtitle, action)` cohérent Task 6 ; `UiMessageHost(bus)` cohérent Task 3↔4.

**Risque connu :** Task 5 — les tests EXISTANTS des 6 ViewModels instancient les VM sans le bus ; ils doivent être adaptés (ajouter un `mockk(relaxed=true)` à l'instanciation). C'est mécanique mais touche plusieurs fichiers de test ; si un VM n'a pas de test existant, n'en créer un que pour `CampaignListViewModel` (représentatif), les autres sont couverts par le compile + runtime. Le placement exact du `get<UiMessageBus>()` dans les pages dépend de l'ordre des params du constructeur — l'implémenteur lit chaque page.
