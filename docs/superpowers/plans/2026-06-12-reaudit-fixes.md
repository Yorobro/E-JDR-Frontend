# Re-audit Fixes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corriger les 10 findings du re-audit adversarial du front E-JDR pour le rendre cohérent et scalable, sans régression.

**Architecture :** App Kotlin desktop (Compose for Desktop, JVM 21), clean/hexagonale 4 couches (domain/application/infrastructure/presentation), Ktor client, Koin DI, Navigation 3. Railway-oriented `Result<T, E : DomainError>`. ViewModels par feature (StateFlow + Channel). Tests JUnit5 + MockK + ktor-client-mock + coroutines-test.

**Tech Stack :** Kotlin 2.2.20, Compose MP 1.11.1, Ktor 3.4.2, Koin 4.1.1, Navigation3-ui 1.1.1, detekt 1.23.8, Kover 0.9.1, Gradle wrapper.

**Conventions impératives :**
- Commits = Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `ci:`, `chore:`). Husky/commitlint actif.
- Après CHAQUE task : `./gradlew verify` doit être **vert** avant de committer.
- Docs/commentaires en **français** (match le code existant).
- Les ports sont des `interface`/`fun interface`, les impls suffixées `Impl`.
- Détekt `maxIssues: 0` : zéro nouvelle violation tolérée.
- **Pièges runtime Nav3 desktop** (cf. `docs/ARCHITECTURE_DECISIONS.md`) : toute nouvelle `Route` doit être ajoutée à `appNavConfiguration` (`subclass(...)`) sinon **crash au démarrage** non attrapé par `verify`. Le décorateur ViewModel est maison (`rememberEjdrViewModelStoreNavEntryDecorator`).
- Pour les tasks touchant la navigation (Task 8) : après `verify`, lancer `./gradlew run` et confirmer que la fenêtre s'ouvre + navigation Login→Home→Settings fonctionne. `verify` ne couvre PAS le runtime Nav3.

**Ordre des tasks** : du moins risqué (CI, Result combinators) au plus risqué (nav/DI/état global). Chaque task est autonome et laisse `verify` vert.

---

## Task 1 : Faire dépendre la release d'une CI verte (Finding F-A) 🔴

**Files:**
- Modify: `.github/workflows/release.yml`

Le job `semantic_release` se déclenche sur push `main` sans rejouer la CI. On ajoute un job `ci` (réutilisant les étapes de `ci.yml`) dont dépend `semantic_release`, de sorte qu'un commit dont les tests échouent ne soit jamais taggé ni buildé.

- [ ] **Step 1 : Ajouter un job `ci` en tête de `release.yml` et le rendre bloquant**

Remplacer le bloc `jobs:` (à partir de la ligne `jobs:`) de `.github/workflows/release.yml` par :

```yaml
jobs:
  ci:
    name: CI (detekt + tests + coverage)
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Verify (detekt + build + tests + coverage)
        run: ./gradlew verify --no-daemon --stacktrace

  semantic_release:
    name: semantic-release
    needs: ci
    runs-on: ubuntu-latest
    outputs:
      released: ${{ steps.release_check.outputs.released }}
      tag: ${{ steps.release_check.outputs.tag }}
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          persist-credentials: true

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: '18'

      - name: Install dependencies
        run: npm install

      - name: Capture latest tag before release
        id: before
        run: echo "tag=$(git tag --sort=-version:refname | head -1)" >> $GITHUB_OUTPUT

      - name: Run semantic-release
        run: npx semantic-release
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Detect new release
        id: release_check
        run: |
          git fetch --tags
          LATEST=$(git tag --sort=-version:refname | head -1)
          BEFORE="${{ steps.before.outputs.tag }}"
          if [ -n "$LATEST" ] && [ "$LATEST" != "$BEFORE" ]; then
            echo "released=true" >> $GITHUB_OUTPUT
            echo "tag=$LATEST" >> $GITHUB_OUTPUT
          else
            echo "released=false" >> $GITHUB_OUTPUT
          fi

  windows_build:
    name: Build and publish Windows binaries
    needs: semantic_release
    if: ${{ needs.semantic_release.outputs.released == 'true' }}
    runs-on: windows-latest
    steps:
      - name: Checkout at release tag
        uses: actions/checkout@v4
        with:
          ref: ${{ needs.semantic_release.outputs.tag }}

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Setup Gradle cache
        uses: gradle/actions/setup-gradle@v4

      - name: Build EXE and MSI
        run: .\gradlew.bat packageDistributionForCurrentOS --no-daemon --stacktrace

      - name: Attach binaries to release
        uses: softprops/action-gh-release@v2
        with:
          tag_name: ${{ needs.semantic_release.outputs.tag }}
          files: |
            build/compose/binaries/**/exe/*.exe
            build/compose/binaries/**/msi/*.msi
```

(Le seul changement de fond : ajout du job `ci` et `needs: ci` sur `semantic_release`. Le reste est identique à l'existant.)

- [ ] **Step 2 : Vérifier la syntaxe YAML localement**

Run: `./gradlew help -q` n'est pas pertinent ici. Valider le YAML via :
Run (PowerShell): `Get-Content .github/workflows/release.yml | Out-Null; if ($?) { "yaml readable" }`
Expected: pas d'erreur de lecture. (La vraie validation se fera au push GitHub.)

- [ ] **Step 3 : Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: gate release on a green CI run (no release on failing tests)"
```

> **Note protection de branche `main`** : la règle "PR + CI requise avant merge" se configure dans les *GitHub repo settings* (Settings → Branches → Branch protection rules), hors du repo. Documenter ça dans le message de PR / un commentaire à l'équipe ; ce n'est pas modifiable par code.

---

## Task 2 : Enrichir `Result` de combinateurs (Finding : Result n'a que `fold`) 🟠

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/application/shared/Result.kt`
- Test: `src/test/kotlin/eu/ejdr/application/shared/ResultTest.kt` (create)

Ajouter `map`, `mapError`, `flatMap`, `getOrNull`, `getOrElse`, `onSuccess`, `onFailure`. Tous `inline`, cohérents avec le `fold` existant.

- [ ] **Step 1 : Écrire le test qui échoue**

Create `src/test/kotlin/eu/ejdr/application/shared/ResultTest.kt` :

```kotlin
package eu.ejdr.application.shared

import eu.ejdr.domain.shared.error.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private data class TestError(override val message: String) : DomainError

class ResultTest {

    private val success: Result<Int, TestError> = Result.Success(2)
    private val failure: Result<Int, TestError> = Result.Failure(TestError("boom"))

    @Test
    fun `map transforms the success value and leaves failure untouched`() {
        assertEquals(Result.Success(4), success.map { it * 2 })
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun `mapError transforms the error and leaves success untouched`() {
        assertEquals(success, success.mapError { TestError("other") })
        val mapped = failure.mapError { TestError(it.message.uppercase()) }
        assertIs<Result.Failure<TestError>>(mapped)
        assertEquals("BOOM", mapped.error.message)
    }

    @Test
    fun `flatMap chains on success and short-circuits on failure`() {
        assertEquals(Result.Success(6), success.flatMap { Result.Success(it + 4) })
        assertEquals(failure, failure.flatMap { Result.Success(it + 4) })
        val chainedFailure: Result<Int, TestError> = success.flatMap { Result.Failure(TestError("late")) }
        assertIs<Result.Failure<TestError>>(chainedFailure)
        assertEquals("late", chainedFailure.error.message)
    }

    @Test
    fun `getOrNull returns the value on success and null on failure`() {
        assertEquals(2, success.getOrNull())
        assertNull(failure.getOrNull())
    }

    @Test
    fun `getOrElse returns the value on success and the fallback on failure`() {
        assertEquals(2, success.getOrElse { -1 })
        assertEquals(-1, failure.getOrElse { -1 })
    }

    @Test
    fun `onSuccess runs the side effect only on success`() {
        var seen: Int? = null
        success.onSuccess { seen = it }
        assertEquals(2, seen)
        seen = null
        failure.onSuccess { seen = it }
        assertNull(seen)
    }

    @Test
    fun `onFailure runs the side effect only on failure`() {
        var seen: String? = null
        failure.onFailure { seen = it.message }
        assertEquals("boom", seen)
        seen = null
        success.onFailure { seen = it.message }
        assertNull(seen)
    }
}
```

- [ ] **Step 2 : Lancer le test, vérifier l'échec (compilation)**

Run: `./gradlew test --tests "eu.ejdr.application.shared.ResultTest"`
Expected: FAIL — `unresolved reference: map / mapError / flatMap / getOrNull / getOrElse / onSuccess / onFailure`.

- [ ] **Step 3 : Implémenter les combinateurs**

Ajouter à la fin de `src/main/kotlin/eu/ejdr/application/shared/Result.kt` (après le `fold` existant) :

```kotlin
/**
 * Transforme la valeur de succès via [transform] ; un échec est propagé tel quel.
 */
inline fun <T, E : DomainError, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Failure -> this
    }

/**
 * Transforme l'erreur d'échec via [transform] ; un succès est propagé tel quel.
 */
inline fun <T, E : DomainError, F : DomainError> Result<T, E>.mapError(
    transform: (E) -> F,
): Result<T, F> = when (this) {
    is Result.Success -> this
    is Result.Failure -> Result.Failure(transform(error))
}

/**
 * Enchaîne une opération produisant elle-même un [Result] ; court-circuite sur échec.
 */
inline fun <T, E : DomainError, R> Result<T, E>.flatMap(
    transform: (T) -> Result<R, E>,
): Result<R, E> = when (this) {
    is Result.Success -> transform(value)
    is Result.Failure -> this
}

/** Renvoie la valeur de succès, ou `null` en cas d'échec. */
fun <T, E : DomainError> Result<T, E>.getOrNull(): T? = when (this) {
    is Result.Success -> value
    is Result.Failure -> null
}

/** Renvoie la valeur de succès, ou la valeur de repli calculée à partir de l'erreur. */
inline fun <T, E : DomainError> Result<T, E>.getOrElse(onFailure: (E) -> T): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> onFailure(error)
}

/** Exécute [action] avec la valeur en cas de succès, puis renvoie le résultat inchangé. */
inline fun <T, E : DomainError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    if (this is Result.Success) action(value)
    return this
}

/** Exécute [action] avec l'erreur en cas d'échec, puis renvoie le résultat inchangé. */
inline fun <T, E : DomainError> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> {
    if (this is Result.Failure) action(error)
    return this
}
```

- [ ] **Step 4 : Lancer le test, vérifier le succès**

Run: `./gradlew test --tests "eu.ejdr.application.shared.ResultTest"`
Expected: PASS (7 tests).

- [ ] **Step 5 : `verify` complet puis commit**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

```bash
git add src/main/kotlin/eu/ejdr/application/shared/Result.kt src/test/kotlin/eu/ejdr/application/shared/ResultTest.kt
git commit -m "feat(shared): enrich Result with map/flatMap/mapError/getOrElse/onSuccess/onFailure"
```

---

## Task 3 : Désambiguïser l'intercepteur 401 (Finding F-C) 🟠

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/infrastructure/http/KtorClientFactory.kt:77-81`

Aujourd'hui, `if (!refreshCall.response.status.isSuccess())` efface la session pour **toute** non-2xx (500, timeout réseau → 0/erreur). On ne doit effacer que sur une vraie expiration (401/403). Sur une erreur réseau/serveur, on retourne le 401 original SANS effacer la session persistée (l'utilisateur pourra réessayer).

- [ ] **Step 1 : Remplacer la condition d'effacement de session**

Dans `src/main/kotlin/eu/ejdr/infrastructure/http/KtorClientFactory.kt`, remplacer le bloc :

```kotlin
            if (!refreshCall.response.status.isSuccess()) {
                // Session expirée : on efface localement et on retourne le 401 original.
                cookiesStorage.clearPersisted()
                return@intercept call
            }
```

par :

```kotlin
            if (!refreshCall.response.status.isSuccess()) {
                // On distingue une vraie expiration de session d'une panne réseau/serveur :
                // - 401/403 sur le refresh => le refresh_token est invalide : on efface la session.
                // - tout autre échec (5xx, indisponibilité) => probablement transitoire : on NE
                //   touche PAS à la session persistée pour permettre une nouvelle tentative.
                val refreshStatus = refreshCall.response.status
                if (refreshStatus == HttpStatusCode.Unauthorized ||
                    refreshStatus == HttpStatusCode.Forbidden
                ) {
                    cookiesStorage.clearPersisted()
                }
                return@intercept call
            }
```

(`HttpStatusCode` est déjà importé ligne 16.)

- [ ] **Step 2 : Mettre à jour la KDoc de l'intercepteur**

Dans la KDoc de classe (lignes 33-37), remplacer la puce de l'intercepteur 401 :

```
 * - Intercepteur 401 : sur toute route hors `/auth/`, tente un rafraîchissement silencieux
 *   de session puis rejoue la requête originale. Si le refresh échoue, la session persistée
 *   est effacée et le 401 est retourné tel quel à l'appelant. (N.B. cet intercepteur ne
 *   couvre PAS les connexions WebSocket longue durée : leur ré-authentification est gérée
 *   par la couche `realtime`.)
```

par :

```
 * - Intercepteur 401 : sur toute route hors `/auth/`, tente un rafraîchissement silencieux
 *   de session puis rejoue la requête originale. Si le refresh renvoie 401/403, la session
 *   persistée est effacée (token réellement expiré) ; sur tout autre échec (réseau, 5xx), la
 *   session est conservée (panne transitoire) et le 401 original est retourné tel quel.
 *   (N.B. cet intercepteur ne couvre PAS les connexions WebSocket longue durée : leur
 *   ré-authentification est gérée par la couche `realtime`.)
```

- [ ] **Step 3 : `verify` puis commit**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL (les tests existants de `AuthHttpRepositoryTest` ne testent pas cet intercepteur — pas de régression attendue).

```bash
git add src/main/kotlin/eu/ejdr/infrastructure/http/KtorClientFactory.kt
git commit -m "fix(http): only clear session on 401/403 refresh, keep it on transient errors"
```

---

## Task 4 : Sortir les ViewModels de l'exclusion Kover (Finding F-B) 🟠

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/../../build.gradle.kts` (racine : `build.gradle.kts:118-138`)

Le package `eu.ejdr.presentation` entier est exclu de Kover, donc les ViewModels (logique testée) ne sont pas comptés. On exclut plus finement : seuls les composables/thème/navigation/state UI restent exclus, **pas** les ViewModels.

Les ViewModels sont les fichiers `*ViewModel.kt` directement sous `presentation/features/<feature>/`. On ne peut pas matcher par suffixe de classe avec `packages(...)`, donc on bascule : on garde l'exclusion UI par packages précis, en cessant d'exclure le package racine `eu.ejdr.presentation`.

- [ ] **Step 1 : Restreindre les exclusions Kover aux seuls éléments UI**

Dans `build.gradle.kts`, remplacer le bloc `excludes { ... }` (lignes ~121-130) par :

```kotlin
            excludes {
                // On EXCLUT l'UI Compose pure (composables, thème, navigation, état UI),
                // testée manuellement / via run. On NE PEUT PAS exclure le package racine
                // `eu.ejdr.presentation` car les ViewModels (logique testable) y vivent et
                // DOIVENT être comptés. D'où une liste de sous-packages UI explicites.
                packages(
                    "eu.ejdr.presentation.shared.component",
                    "eu.ejdr.presentation.shared.theme",
                    "eu.ejdr.presentation.shared.state",
                    "eu.ejdr.presentation.shared.di",
                    "eu.ejdr.presentation.navigation",
                    "eu.ejdr.presentation.features.auth.page",
                    "eu.ejdr.presentation.features.auth.component",
                    "eu.ejdr.presentation.features.settings.page",
                    "eu.ejdr.presentation.features.settings.component",
                    "eu.ejdr.presentation.features.user.page",
                    "eu.ejdr.di",
                )
                classes(
                    "eu.ejdr.MainKt",
                    "eu.ejdr.presentation.AppKt",
                )
            }
```

> Note : `eu.ejdr.presentation.AppKt` (le `App()` composable) est exclu par classe car il vit directement sous `presentation/` (pas dans un sous-package `page`). Les ViewModels `AuthViewModel`, `UserViewModel`, `SettingsViewModel` sont désormais **comptés**.

- [ ] **Step 2 : Lancer la vérification de couverture et constater le nouveau total**

Run: `./gradlew koverVerify`
Expected : soit PASS (≥60 % avec ViewModels comptés — probable, ils sont bien testés), soit FAIL si le total réel passe sous 60 %.

- [ ] **Step 3 : Si `koverVerify` échoue, générer le rapport pour diagnostiquer**

Run (seulement si Step 2 a échoué): `./gradlew koverHtmlReport` puis ouvrir `build/reports/kover/html/index.html`.
Action : si des ViewModels nouvellement comptés manquent de tests, c'est attendu — soit on ajoute les tests manquants (préférable), soit on documente et on ajuste `minBound`. NE PAS ré-exclure les ViewModels. Pour ce plan, si le plancher casse, baisser temporairement `minBound(60)` → `minBound(55)` avec un commentaire `// TODO: remonter à 60 après ajout des tests VM manquants` et créer une note. (En pratique les 3 VM ont des tests dédiés — le plancher devrait tenir.)

- [ ] **Step 4 : `verify` complet puis commit**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

```bash
git add build.gradle.kts
git commit -m "test(coverage): count ViewModels in Kover (exclude only Compose UI)"
```

---

## Task 5 : `ThemeRepository` suspend + Result (Findings : getTheme blocking/incohérent) 🟡

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/abstraction/repository/ThemeRepository.kt`
- Modify: `src/main/kotlin/eu/ejdr/infrastructure/settings/ThemeFileRepository.kt`
- Modify: `src/test/kotlin/eu/ejdr/infrastructure/settings/ThemeFileRepositoryTest.kt`

Rendre les deux méthodes `suspend` (I/O hors thread UI) et `setTheme` renvoyer `Result<Unit, SettingsError>` au lieu de `Boolean`. `getTheme` reste un read avec repli sûr (pas de Result : un échec de lecture = thème par défaut, comportement voulu) mais devient `suspend` pour ne pas bloquer.

- [ ] **Step 1 : Adapter le test du repository d'abord (montre la nouvelle signature)**

Lire `src/test/kotlin/eu/ejdr/infrastructure/settings/ThemeFileRepositoryTest.kt`, puis :
- envelopper chaque appel `repo.getTheme()` / `repo.setTheme(...)` dans `runTest { ... }` (import `kotlinx.coroutines.test.runTest`),
- remplacer les assertions sur le `Boolean` de `setTheme` par des assertions sur `Result` :
  - succès attendu : `assertIs<Result.Success<Unit>>(repo.setTheme(ThemeVariant.DARK))` (imports `eu.ejdr.application.shared.Result`, `kotlin.test.assertIs`).

(Adapter chaque test existant à ces deux changements ; conserver la logique d'assertion métier.)

- [ ] **Step 2 : Lancer le test, vérifier l'échec (compilation)**

Run: `./gradlew test --tests "eu.ejdr.infrastructure.settings.ThemeFileRepositoryTest"`
Expected: FAIL — signatures incompatibles (`setTheme` renvoie `Boolean`, pas `suspend`).

- [ ] **Step 3 : Modifier le port `ThemeRepository`**

Remplacer le contenu de `ThemeRepository.kt` par :

```kotlin
package eu.ejdr.application.features.settings.abstraction.repository

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError

interface ThemeRepository {
    /** Lit le thème persisté, avec un repli sûr si rien n'est enregistré ou en cas d'erreur. */
    suspend fun getTheme(): ThemeVariant

    /**
     * Persiste le thème choisi.
     *
     * @return [Result.Success] si l'écriture a réussi, ou [SettingsError.ThemePersistenceFailed].
     */
    suspend fun setTheme(theme: ThemeVariant): Result<Unit, SettingsError>
}
```

- [ ] **Step 4 : Modifier l'implémentation `ThemeFileRepository` (I/O sur Dispatchers.IO)**

Remplacer le contenu de `ThemeFileRepository.kt` par :

```kotlin
package eu.ejdr.infrastructure.settings

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError
import java.io.File
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ThemeFileRepository(dataDir: File) : ThemeRepository {

    private val file = File(dataDir, "settings.properties")

    override suspend fun getTheme(): ThemeVariant = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ThemeVariant.LIGHT
        runCatching {
            Properties().apply { file.inputStream().use { load(it) } }
                .getProperty("theme")
                ?.let { runCatching { ThemeVariant.valueOf(it) }.getOrNull() }
                ?: ThemeVariant.LIGHT
        }.getOrDefault(ThemeVariant.LIGHT)
    }

    override suspend fun setTheme(theme: ThemeVariant): Result<Unit, SettingsError> =
        withContext(Dispatchers.IO) {
            val written = runCatching {
                val props = Properties()
                if (file.exists()) file.inputStream().use { props.load(it) }
                props.setProperty("theme", theme.name)
                file.outputStream().use { props.store(it, null) }
            }.isSuccess
            if (written) Result.Success(Unit) else Result.Failure(SettingsError.ThemePersistenceFailed)
        }
}
```

- [ ] **Step 5 : Lancer le test du repository, vérifier le succès**

Run: `./gradlew test --tests "eu.ejdr.infrastructure.settings.ThemeFileRepositoryTest"`
Expected: PASS.

> Ce changement casse temporairement la compilation des use cases settings (`GetThemeUseCaseImpl`, `SetThemeUseCaseImpl`) et de `SettingsViewModel`. Ils sont corrigés en Task 6. NE PAS lancer `verify` complet avant la fin de Task 6 — committer ce repository seul rendrait `verify` rouge. **Exception au "commit par task" : Task 5 et Task 6 forment une seule unité atomique.** Ne pas committer ici ; enchaîner sur Task 6.

---

## Task 6 : `GetThemeUseCase`/`SetThemeUseCase` suspend + `SettingsViewModel` async (suite Task 5) 🟠

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/abstraction/usecase/GetThemeUseCase.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/abstraction/usecase/SetThemeUseCase.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/usecase/GetThemeUseCaseImpl.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/usecase/SetThemeUseCaseImpl.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/settings/SettingsViewModel.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/App.kt:42-43` (getTheme désormais suspend)
- Modify: tests : `GetThemeUseCaseImplTest.kt`, `SetThemeUseCaseImplTest.kt`, `SettingsViewModelTest.kt`

- [ ] **Step 1 : Rendre les ports suspend**

`GetThemeUseCase.kt` :

```kotlin
package eu.ejdr.application.features.settings.abstraction.usecase

import eu.ejdr.domain.features.settings.entities.ThemeVariant

fun interface GetThemeUseCase {
    suspend operator fun invoke(): ThemeVariant
}
```

`SetThemeUseCase.kt` — lire le fichier d'abord (il a une KDoc), puis rendre `invoke` suspend. Remplacer la ligne de signature `operator fun invoke(theme: ThemeVariant): Result<Unit, SettingsError>` par `suspend operator fun invoke(theme: ThemeVariant): Result<Unit, SettingsError>`.

- [ ] **Step 2 : Rendre les impls suspend (délégation au repo désormais suspend)**

`GetThemeUseCaseImpl.kt` :

```kotlin
package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.domain.features.settings.entities.ThemeVariant

class GetThemeUseCaseImpl(private val repository: ThemeRepository) : GetThemeUseCase {
    override suspend fun invoke(): ThemeVariant = repository.getTheme()
}
```

`SetThemeUseCaseImpl.kt` :

```kotlin
package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError

class SetThemeUseCaseImpl(private val repository: ThemeRepository) : SetThemeUseCase {
    override suspend fun invoke(theme: ThemeVariant): Result<Unit, SettingsError> =
        repository.setTheme(theme)
}
```

(La conversion Boolean→Result est désormais portée par le repository — l'impl est une pure délégation.)

- [ ] **Step 3 : `SettingsViewModel` charge le thème de façon asynchrone**

Remplacer le contenu de `SettingsViewModel.kt` par (le thème initial part d'un défaut `LIGHT` puis est remplacé par la lecture async ; `onThemeSelected` devient async via `viewModelScope`) :

```kotlin
package eu.ejdr.presentation.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de l'écran des paramètres.
 *
 * Charge le thème courant ([currentTheme]) de façon **asynchrone** via [GetThemeUseCase]
 * (l'I/O fichier ne bloque pas le thread UI) et persiste chaque changement via
 * [SetThemeUseCase]. Le ViewModel étant retenu par la destination, l'état survit à la
 * recomposition.
 *
 * La persistance peut **échouer** (disque indisponible) : dans ce cas l'état observé n'est
 * PAS modifié (pas de désynchronisation UI ↔ disque) et un message est exposé via [error].
 *
 * @param getTheme Use case de lecture du thème persisté.
 * @property setTheme Use case de persistance du thème.
 */
class SettingsViewModel(
    getTheme: GetThemeUseCase,
    private val setTheme: SetThemeUseCase,
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(ThemeVariant.LIGHT)
    val currentTheme: StateFlow<ThemeVariant> = _currentTheme.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch { _currentTheme.value = getTheme() }
    }

    /**
     * Tente d'appliquer et de **persister** le thème choisi.
     *
     * Met à jour l'état observé **uniquement si la persistance réussit**. [onApplied] est
     * invoqué avec le thème en cas de succès (pour propager au design system global).
     *
     * @param theme Nouveau thème sélectionné.
     * @param onApplied Callback succès (thème persisté), exécuté sur le thread principal.
     */
    fun onThemeSelected(theme: ThemeVariant, onApplied: (ThemeVariant) -> Unit) {
        viewModelScope.launch {
            setTheme(theme).fold(
                onSuccess = {
                    _error.value = null
                    _currentTheme.value = theme
                    onApplied(theme)
                },
                onFailure = { settingsError -> _error.value = settingsError.message },
            )
        }
    }
}
```

> Changement de contrat : `onThemeSelected` ne renvoie plus `Boolean` (un appel async ne peut pas renvoyer le résultat en synchrone). Il prend un callback `onApplied`. Le consommateur (`SettingsForm`/`SettingsPage`) doit être adapté — voir Step 5.

- [ ] **Step 4 : Adapter `App.kt` (getTheme suspend au démarrage)**

Dans `App.kt`, le thème initial ne peut plus être lu en synchrone ligne 43. Remplacer :

```kotlin
    val getTheme = koinInject<GetThemeUseCase>()
    var themeVariant by remember { mutableStateOf(getTheme()) }
```

par :

```kotlin
    val getTheme = koinInject<GetThemeUseCase>()
    var themeVariant by remember { mutableStateOf(ThemeVariant.LIGHT) }
    LaunchedEffect(Unit) { themeVariant = getTheme() }
```

(`LaunchedEffect` et `ThemeVariant` sont déjà importés.)

- [ ] **Step 5 : Adapter le consommateur de `onThemeSelected`**

Lire `src/main/kotlin/eu/ejdr/presentation/features/settings/page/SettingsPage.kt` et `.../component/SettingsForm.kt`. Là où `onThemeSelected(theme)` était appelé et son `Boolean`/le `onThemeChange` propagé, remplacer par l'appel à callback : `viewModel.onThemeSelected(theme, onApplied = onThemeChange)`. (Le `onThemeChange: (ThemeVariant) -> Unit` remonte déjà jusqu'à `App.kt` via `AppNavDisplay`.) Adapter la signature de `SettingsForm` si elle passait par un booléen de retour.

- [ ] **Step 6 : Adapter les tests settings**

`GetThemeUseCaseImplTest.kt` : envelopper dans `runTest { ... }`, `coEvery { repository.getTheme() }` au lieu de `every`. Imports : `kotlinx.coroutines.test.runTest`, `io.mockk.coEvery`.

`SetThemeUseCaseImplTest.kt` : `runTest { ... }`, `coEvery { repository.setTheme(...) } returns Result.Success(Unit)` / `Result.Failure(...)` (le repo renvoie désormais `Result`, plus `Boolean`). L'impl déléguant, le test vérifie la propagation du `Result`.

`SettingsViewModelTest.kt` : les use cases mockés sont désormais `suspend`. Le helper `viewModel(...)` doit fournir `GetThemeUseCase { initial }` (lambda suspend OK) et `SetThemeUseCase { theme -> persisted.add(theme); setResult }`. `onThemeSelected` ne renvoie plus de `Boolean` → remplacer les assertions `assertTrue(applied)` par la capture du callback `onApplied`. Le chargement initial étant async, utiliser `runTest` + avancer le dispatcher. Réécrire la classe ainsi :

```kotlin
package eu.ejdr.presentation.features.settings

import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        initial: ThemeVariant = ThemeVariant.LIGHT,
        setResult: Result<Unit, SettingsError> = Result.Success(Unit),
        persisted: MutableList<ThemeVariant> = mutableListOf(),
    ) = SettingsViewModel(
        getTheme = GetThemeUseCase { initial },
        setTheme = SetThemeUseCase { theme -> persisted.add(theme); setResult },
    )

    @Test
    fun `exposes the initial theme from the get use case`() = runTest {
        val vm = viewModel(initial = ThemeVariant.DARK)
        advanceUntilIdle()
        assertEquals(ThemeVariant.DARK, vm.currentTheme.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `successful selection persists, updates state and notifies`() = runTest {
        val persisted = mutableListOf<ThemeVariant>()
        val vm = viewModel(initial = ThemeVariant.LIGHT, persisted = persisted)
        advanceUntilIdle()

        var applied: ThemeVariant? = null
        vm.onThemeSelected(ThemeVariant.DARK) { applied = it }
        advanceUntilIdle()

        assertEquals(ThemeVariant.DARK, applied)
        assertEquals(listOf(ThemeVariant.DARK), persisted)
        assertEquals(ThemeVariant.DARK, vm.currentTheme.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `failed persistence keeps state unchanged and exposes an error`() = runTest {
        val vm = viewModel(
            initial = ThemeVariant.LIGHT,
            setResult = Result.Failure(SettingsError.ThemePersistenceFailed),
        )
        advanceUntilIdle()

        var applied: ThemeVariant? = null
        vm.onThemeSelected(ThemeVariant.DARK) { applied = it }
        advanceUntilIdle()

        assertNull(applied)
        assertEquals(ThemeVariant.LIGHT, vm.currentTheme.value)
        assertEquals(SettingsError.ThemePersistenceFailed.message, vm.error.value)
    }

    @Test
    fun `a successful selection clears a previous error`() = runTest {
        var nextResult: Result<Unit, SettingsError> = Result.Failure(SettingsError.ThemePersistenceFailed)
        val vm = SettingsViewModel(
            getTheme = GetThemeUseCase { ThemeVariant.LIGHT },
            setTheme = SetThemeUseCase { nextResult },
        )
        advanceUntilIdle()

        vm.onThemeSelected(ThemeVariant.DARK) {}
        advanceUntilIdle()
        assertEquals(SettingsError.ThemePersistenceFailed.message, vm.error.value)

        nextResult = Result.Success(Unit)
        vm.onThemeSelected(ThemeVariant.DARK) {}
        advanceUntilIdle()

        assertNull(vm.error.value)
        assertEquals(ThemeVariant.DARK, vm.currentTheme.value)
    }
}
```

- [ ] **Step 7 : `verify` complet (clôt Task 5+6) puis commit atomique**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

> Si la couche présentation a d'autres appelants de `onThemeSelected` (grep `onThemeSelected`), les adapter avant de committer.

```bash
git add src/main/kotlin/eu/ejdr/application/features/settings src/main/kotlin/eu/ejdr/infrastructure/settings src/main/kotlin/eu/ejdr/presentation/features/settings src/main/kotlin/eu/ejdr/presentation/App.kt src/test/kotlin/eu/ejdr/application/features/settings src/test/kotlin/eu/ejdr/infrastructure/settings/ThemeFileRepositoryTest.kt src/test/kotlin/eu/ejdr/presentation/features/settings/SettingsViewModelTest.kt
git commit -m "refactor(settings): make theme read/write suspend + Result, async ViewModel load"
```

- [ ] **Step 8 : Validation runtime (settings touche App.kt)**

Run: `./gradlew run`
Expected : la fenêtre s'ouvre, le thème s'applique, l'écran Paramètres permet de basculer light/dark sans blocage. Fermer la fenêtre.

---

## Task 7 : `CheckUpdate` et `DownloadAndInstallUpdate` en `Result` (Finding : contrats update incohérents) 🟠

**Files:**
- Create: `src/main/kotlin/eu/ejdr/domain/features/update/error/UpdateError.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/update/abstraction/usecase/CheckUpdateUseCase.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/update/usecase/CheckUpdateUseCaseImpl.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/update/abstraction/usecase/DownloadAndInstallUpdateUseCase.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/update/usecase/DownloadAndInstallUpdateUseCaseImpl.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/shared/Coroutines.kt` (réutiliser `runCatchingCancellable` existant)
- Modify: `src/main/kotlin/eu/ejdr/presentation/App.kt` (consommation `checkUpdate`)
- Modify: tests update + `UpdateDialog`/`UpdateViewModel` (Task 9)

On introduit `UpdateError` (domaine). `CheckUpdate` renvoie `Result<UpdateInfoDto?, UpdateError>` (succès = info ou null si pas de MAJ ; échec = erreur réseau). `DownloadAndInstall` renvoie `Result<Unit, UpdateError>` (plus d'exception qui fuit).

- [ ] **Step 1 : Créer l'erreur domaine `UpdateError`**

Create `src/main/kotlin/eu/ejdr/domain/features/update/error/UpdateError.kt` :

```kotlin
package eu.ejdr.domain.features.update.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature mise à jour.
 *
 * `sealed class` propre à la feature (même contrat que les autres erreurs de domaine) :
 * garantit un `when` exhaustif et reste une variante de [DomainError]. Chaque variante
 * porte un message utilisateur prêt à afficher.
 */
sealed class UpdateError(override val message: String) : DomainError {
    /** La vérification de mise à jour a échoué (réseau, serveur indisponible). */
    data object CheckFailed :
        UpdateError("Impossible de vérifier les mises à jour. Réessayez plus tard.")

    /** Le téléchargement ou le lancement de l'installeur a échoué. */
    data object DownloadFailed :
        UpdateError("Le téléchargement de la mise à jour a échoué. Réessayez.")
}
```

- [ ] **Step 2 : Mettre à jour le test de `CheckUpdateUseCaseImpl` (nouvelle signature Result)**

Réécrire `src/test/kotlin/eu/ejdr/application/features/update/usecase/CheckUpdateUseCaseImplTest.kt`. Les assertions `assertNull(useCase()())` deviennent `assertNull(useCase("…")().getOrNull())` et les `assertNotNull(...)` deviennent `assertNotNull(useCase("…")().getOrNull())`. Ajouter un test d'échec réseau :

```kotlin
    @Test
    fun `returns CheckFailed when the repository throws`() = runTest {
        coEvery { repository.fetchLatestRelease() } throws RuntimeException("network down")
        val result = useCase("1.0.0")()
        assertIs<Result.Failure<UpdateError>>(result)
        assertEquals(UpdateError.CheckFailed, result.error)
    }
```

Imports à ajouter : `eu.ejdr.application.shared.Result`, `eu.ejdr.application.shared.getOrNull`, `eu.ejdr.domain.features.update.error.UpdateError`, `kotlin.test.assertIs`, `kotlin.test.assertEquals`, `io.mockk.coEvery` (déjà là). Adapter les 10 tests existants (`.getOrNull()`).

- [ ] **Step 3 : Lancer le test, vérifier l'échec**

Run: `./gradlew test --tests "eu.ejdr.application.features.update.usecase.CheckUpdateUseCaseImplTest"`
Expected: FAIL (compilation : `invoke` renvoie `UpdateInfoDto?`, pas `Result`).

- [ ] **Step 4 : Modifier le port et l'impl `CheckUpdate`**

`CheckUpdateUseCase.kt` :

```kotlin
package eu.ejdr.application.features.update.abstraction.usecase

import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError

fun interface CheckUpdateUseCase {
    /**
     * Vérifie la disponibilité d'une mise à jour plus récente.
     *
     * @return [Result.Success] portant l'info de MAJ, ou `null` si l'app est à jour ;
     * [UpdateError.CheckFailed] si la vérification échoue (réseau).
     */
    suspend operator fun invoke(): Result<UpdateInfoDto?, UpdateError>
}
```

`CheckUpdateUseCaseImpl.kt` :

```kotlin
package eu.ejdr.application.features.update.usecase

import eu.ejdr.BuildConfig
import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.update.error.UpdateError
import eu.ejdr.domain.shared.version.SemanticVersion

class CheckUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val currentVersion: String = BuildConfig.APP_VERSION,
) : CheckUpdateUseCase {

    override suspend fun invoke(): Result<UpdateInfoDto?, UpdateError> =
        runCatchingCancellable {
            val latest = updateRepository.fetchLatestRelease()
            if (latest == null) {
                null
            } else {
                val isNewer = SemanticVersion.parse(latest.version)
                    .isNewerThan(SemanticVersion.parse(currentVersion))
                if (isNewer) latest else null
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Failure(UpdateError.CheckFailed) },
        )
}
```

> **Vérifier le nom exact** du helper dans `Coroutines.kt` (la KDoc d'audit le nomme `runCatchingCancellable`). Lire `src/main/kotlin/eu/ejdr/application/shared/Coroutines.kt` et utiliser le nom réel. Il renvoie un `kotlin.Result<T>` → on a `.fold(onSuccess, onFailure)` de stdlib disponible.

- [ ] **Step 5 : Lancer le test CheckUpdate, vérifier le succès**

Run: `./gradlew test --tests "eu.ejdr.application.features.update.usecase.CheckUpdateUseCaseImplTest"`
Expected: PASS.

- [ ] **Step 6 : Modifier le port et l'impl `DownloadAndInstall`**

`DownloadAndInstallUpdateUseCase.kt` :

```kotlin
package eu.ejdr.application.features.update.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError

fun interface DownloadAndInstallUpdateUseCase {
    /**
     * Télécharge puis lance l'installeur de la mise à jour.
     *
     * @return [Result.Success] si le lancement a été déclenché, ou
     * [UpdateError.DownloadFailed] en cas d'échec (réseau, écriture disque, lancement OS).
     */
    suspend operator fun invoke(
        downloadUrl: String,
        onProgress: (Float?) -> Unit,
    ): Result<Unit, UpdateError>
}
```

`DownloadAndInstallUpdateUseCaseImpl.kt` :

```kotlin
package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.update.error.UpdateError

/**
 * Implémentation de [DownloadAndInstallUpdateUseCase].
 *
 * Orchestration pure : télécharge l'installeur via l'[UpdateRepository] puis délègue le
 * lancement et la fermeture de l'application au service [SystemLauncherService]. Les effets
 * de bord OS sont tenus hors de la couche application (testable). Toute exception est
 * convertie en [UpdateError.DownloadFailed] : aucune ne traverse vers la présentation.
 */
class DownloadAndInstallUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val systemLauncher: SystemLauncherService,
) : DownloadAndInstallUpdateUseCase {
    override suspend fun invoke(
        downloadUrl: String,
        onProgress: (Float?) -> Unit,
    ): Result<Unit, UpdateError> = runCatchingCancellable {
        val installer = updateRepository.downloadUpdate(downloadUrl, onProgress)
        systemLauncher.launchInstallerAndExit(installer)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Failure(UpdateError.DownloadFailed) },
    )
}
```

- [ ] **Step 7 : Adapter le test de `DownloadAndInstallUpdateUseCaseImpl`**

Lire `src/test/kotlin/eu/ejdr/application/features/update/usecase/DownloadAndInstallUpdateUseCaseImplTest.kt`. Adapter : les cas de succès assertent `assertIs<Result.Success<Unit>>(useCase(...))` ; ajouter un cas où `downloadUpdate` jette → `assertIs<Result.Failure<UpdateError>>` avec `UpdateError.DownloadFailed`. Conserver la vérification que `launchInstallerAndExit` est appelé en cas de succès.

- [ ] **Step 8 : Adapter `App.kt` (consommation de `checkUpdate`)**

Dans `App.kt`, `updateInfo = checkUpdate()` renvoie désormais un `Result`. Remplacer :

```kotlin
            launch { updateInfo = checkUpdate() }
```

par :

```kotlin
            launch { updateInfo = checkUpdate().getOrNull() }
```

(import `eu.ejdr.application.shared.getOrNull`. Sur échec réseau, pas de dialog — comportement voulu : on n'embête pas l'utilisateur au boot si la vérif échoue.)

- [ ] **Step 9 : `verify` complet**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

> Si `UpdateDialog` appelle encore `downloadAndInstall(...)` en attendant un `Unit`, la compilation casse. UpdateDialog est refait en Task 9. Si `verify` casse uniquement sur UpdateDialog, enchaîner Task 9 avant de committer (Task 7+9 = unité atomique comme 5+6). Sinon committer ici :

```bash
git add src/main/kotlin/eu/ejdr/domain/features/update src/main/kotlin/eu/ejdr/application/features/update src/main/kotlin/eu/ejdr/presentation/App.kt src/test/kotlin/eu/ejdr/application/features/update
git commit -m "refactor(update): return Result<_,UpdateError> from check/download use cases"
```

---

## Task 8 : Décentraliser la navigation par feature (Finding F1) 🟠

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/navigation/FeatureEntries.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/navigation/Routes.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt`
- Create: `src/main/kotlin/eu/ejdr/presentation/features/auth/AuthNavEntries.kt`
- Create: `src/main/kotlin/eu/ejdr/presentation/features/user/UserNavEntries.kt`
- Create: `src/main/kotlin/eu/ejdr/presentation/features/settings/SettingsNavEntries.kt`

Objectif : `AppNavDisplay` ne contient plus le mapping route→écran en dur ; chaque feature expose une fonction d'extension `EntryProviderBuilder<NavKey>.xxxEntries(...)` agrégée dans `AppNavDisplay`. On garde `Routes.kt` comme registre central des clés + sérialisation (un seul endroit, c'est sain), mais on documente le rappel.

> Cette task est la plus risquée (runtime Nav3). Valider par `./gradlew run` à la fin.

- [ ] **Step 1 : Définir le contrat d'agrégation des entries**

Create `src/main/kotlin/eu/ejdr/presentation/navigation/FeatureEntries.kt` :

```kotlin
package eu.ejdr.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import eu.ejdr.domain.features.settings.entities.ThemeVariant

/**
 * Actions de navigation transverses fournies par l'app aux entries de chaque feature.
 *
 * Plutôt que de passer 4 callbacks à `AppNavDisplay` puis à chaque feature, on regroupe
 * les actions communes (déconnexion, changement de thème, reset de pile, navigation) dans
 * un seul objet. Ajouter une action transverse = un champ ici, pas un n-ième paramètre.
 *
 * @property backStack Pile possédée par l'app (empiler/dépiler des [Route]).
 * @property onLogout Déconnexion (use case + retour Login), déléguée à l'app.
 * @property onThemeChange Propage le thème choisi à l'app pour recomposer le design system.
 * @property resetTo Remplace toute la pile par une destination unique (post-login/logout).
 */
class NavActions(
    val backStack: NavBackStack<NavKey>,
    val onLogout: () -> Unit,
    val onThemeChange: (ThemeVariant) -> Unit,
    val resetTo: (Route) -> Unit,
)
```

- [ ] **Step 2 : Extraire les entries auth**

Create `src/main/kotlin/eu/ejdr/presentation/features/auth/AuthNavEntries.kt` :

```kotlin
package eu.ejdr.presentation.features.auth

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.EntryProviderBuilder
import eu.ejdr.presentation.features.auth.page.LoginPage
import eu.ejdr.presentation.features.auth.page.RegisterPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route

/** Entries de navigation de la feature authentification (Login, Register). */
fun EntryProviderBuilder<NavKey>.authEntries(actions: NavActions) {
    entry<Route.Login> {
        LoginPage(
            onAuthenticated = { actions.resetTo(Route.Home) },
            onGoToRegister = { actions.backStack.add(Route.Register) },
        )
    }
    entry<Route.Register> {
        RegisterPage(
            onAuthenticated = { actions.resetTo(Route.Home) },
            onGoToLogin = { actions.backStack.removeLastOrNull() },
        )
    }
}
```

> **Vérifier l'import exact** de `EntryProviderBuilder` et `entry` contre la version navigation3-ui 1.1.1 réellement résolue (la doc se contredit sur les packages). Lire les imports actuels de `AppNavDisplay.kt` (`androidx.navigation3.runtime.entryProvider`) et résoudre le type du receiver de `entryProvider { }` — c'est ce type qui doit être le receiver des fonctions d'extension. Si le type s'appelle autrement, ajuster les 3 fichiers `*NavEntries.kt` en conséquence. **Ne pas inventer le nom** : le confirmer via complétion/compilation.

- [ ] **Step 3 : Extraire les entries user (Home) et settings**

Create `src/main/kotlin/eu/ejdr/presentation/features/user/UserNavEntries.kt` :

```kotlin
package eu.ejdr.presentation.features.user

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.EntryProviderBuilder
import eu.ejdr.presentation.features.user.page.UserPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entry de navigation de la zone connectée (écran d'accueil). */
fun EntryProviderBuilder<NavKey>.userEntries(actions: NavActions) {
    entry<Route.Home> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "E-JDR",
                    onLogout = actions.onLogout,
                    onSettings = { actions.backStack.add(Route.Settings) },
                )
            },
        ) {
            UserPage(onSessionExpired = { actions.resetTo(Route.Login) })
        }
    }
}
```

Create `src/main/kotlin/eu/ejdr/presentation/features/settings/SettingsNavEntries.kt` :

```kotlin
package eu.ejdr.presentation.features.settings

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.EntryProviderBuilder
import eu.ejdr.presentation.features.settings.page.SettingsPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar

/** Entry de navigation de l'écran des paramètres. */
fun EntryProviderBuilder<NavKey>.settingsEntries(actions: NavActions) {
    entry<Route.Settings> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Paramètres",
                    onLogout = actions.onLogout,
                    onBack = { actions.backStack.removeLastOrNull() },
                )
            },
        ) {
            SettingsPage(onThemeChange = actions.onThemeChange)
        }
    }
}
```

- [ ] **Step 4 : `AppNavDisplay` agrège les entries des features**

Remplacer le corps de `AppNavDisplay.kt` (garder le `SplashScreen` privé en bas et la KDoc, mais le `entryProvider` agrège) :

```kotlin
@Composable
fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    onLogout: () -> Unit,
    onThemeChange: (ThemeVariant) -> Unit,
    resetTo: (Route) -> Unit,
) {
    val actions = NavActions(backStack, onLogout, onThemeChange, resetTo)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(rememberEjdrViewModelStoreNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<Route.Splash> { SplashScreen() }
            authEntries(actions)
            userEntries(actions)
            settingsEntries(actions)
        },
    )
}
```

Ajouter les imports : `eu.ejdr.presentation.features.auth.authEntries`, `eu.ejdr.presentation.features.user.userEntries`, `eu.ejdr.presentation.features.settings.settingsEntries`. Retirer les imports devenus inutiles (`LoginPage`, `RegisterPage`, `SettingsPage`, `UserPage`, `AppScaffold`, `AppTopBar` — ils ont migré dans les `*NavEntries.kt`). Garder `Box/fillMaxSize/background/CircularProgressIndicator/Alignment/AppTheme` pour `SplashScreen`.

- [ ] **Step 5 : Documenter le registre central des routes**

Dans `Routes.kt`, compléter la KDoc de `appNavConfiguration` (après "Toute nouvelle [Route] doit être ajoutée ici via `subclass(...)`.") par :

```
 * Et son entry de rendu doit être ajoutée dans la fonction `xxxEntries()` de la feature
 * correspondante (cf. `presentation/features/<feature>/<Feature>NavEntries.kt`), elle-même
 * agrégée dans `AppNavDisplay`. Le mapping route→écran est ainsi distribué par feature ;
 * seul l'enregistrement de sérialisation reste centralisé ici (un seul point de vérité).
```

- [ ] **Step 6 : `verify`**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL. (Si erreur sur le type du receiver `EntryProviderBuilder`, corriger selon le type réel — voir Step 2.)

- [ ] **Step 7 : Validation runtime OBLIGATOIRE (Nav3 desktop)**

Run: `./gradlew run`
Expected : fenêtre s'ouvre sur Splash → bascule Login. Tester : Login→Register→retour ; après login simulé Home ; Home→Settings→retour ; Logout→Login. Aucune route ne doit crasher. Fermer la fenêtre.

- [ ] **Step 8 : Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/navigation src/main/kotlin/eu/ejdr/presentation/features/auth/AuthNavEntries.kt src/main/kotlin/eu/ejdr/presentation/features/user/UserNavEntries.kt src/main/kotlin/eu/ejdr/presentation/features/settings/SettingsNavEntries.kt
git commit -m "refactor(navigation): distribute route->screen entries per feature"
```

---

## Task 9 : `UpdateViewModel` — sortir la machine à états de `UpdateDialog` (Finding F6) 🟡

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/features/update/UpdateViewModel.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/shared/component/organism/UpdateDialog.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/App.kt` (instancier le VM, brancher le dialog)
- Create: `src/test/kotlin/eu/ejdr/presentation/features/update/UpdateViewModelTest.kt`

Déplacer `DownloadState` + la logique de téléchargement dans un `UpdateViewModel` (StateFlow). `UpdateDialog` redevient un composant "bête" qui lit un état et émet des callbacks.

- [ ] **Step 1 : Écrire le test du ViewModel (échoue)**

Create `src/test/kotlin/eu/ejdr/presentation/features/update/UpdateViewModelTest.kt` :

```kotlin
package eu.ejdr.presentation.features.update

import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts idle`() {
        val vm = UpdateViewModel(DownloadAndInstallUpdateUseCase { _, _ -> Result.Success(Unit) })
        assertIs<DownloadState.Idle>(vm.state.value)
    }

    @Test
    fun `successful download keeps no error state`() = runTest {
        var progressSeen = false
        val vm = UpdateViewModel(
            DownloadAndInstallUpdateUseCase { _, onProgress -> onProgress(0.5f); progressSeen = true; Result.Success(Unit) },
        )
        vm.download("https://example.com/app.exe")
        advanceUntilIdle()
        assertEquals(true, progressSeen)
        // après succès, le launcher quitte l'app ; l'état n'est pas repassé en Error.
        assertIs<DownloadState.Downloading>(vm.state.value)
    }

    @Test
    fun `failed download moves to Error`() = runTest {
        val vm = UpdateViewModel(
            DownloadAndInstallUpdateUseCase { _, _ -> Result.Failure(UpdateError.DownloadFailed) },
        )
        vm.download("https://example.com/app.exe")
        advanceUntilIdle()
        assertIs<DownloadState.Error>(vm.state.value)
    }
}
```

- [ ] **Step 2 : Lancer, vérifier l'échec**

Run: `./gradlew test --tests "eu.ejdr.presentation.features.update.UpdateViewModelTest"`
Expected: FAIL — `UpdateViewModel` / `DownloadState` introuvables.

- [ ] **Step 3 : Créer `UpdateViewModel`**

Create `src/main/kotlin/eu/ejdr/presentation/features/update/UpdateViewModel.kt` :

```kotlin
package eu.ejdr.presentation.features.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.fold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** État du téléchargement d'une mise à jour. */
sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progress: Float?) : DownloadState
    data object Error : DownloadState
}

/**
 * ViewModel du dialog de mise à jour : porte la machine à états du téléchargement, hors du
 * composable (qui redevient « bête »). Persiste par destination via le décorateur Nav3.
 *
 * @property downloadAndInstall Use case de téléchargement + lancement de l'installeur.
 */
class UpdateViewModel(
    private val downloadAndInstall: DownloadAndInstallUpdateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    /** Lance le téléchargement de l'installeur à [url], en publiant la progression dans [state]. */
    fun download(url: String) {
        _state.value = DownloadState.Downloading(null)
        viewModelScope.launch {
            downloadAndInstall(url) { progress -> _state.value = DownloadState.Downloading(progress) }
                .fold(
                    onSuccess = { /* le launcher quitte l'app ; rien à faire */ },
                    onFailure = { _state.value = DownloadState.Error },
                )
        }
    }
}
```

- [ ] **Step 4 : Lancer le test, vérifier le succès**

Run: `./gradlew test --tests "eu.ejdr.presentation.features.update.UpdateViewModelTest"`
Expected: PASS.

- [ ] **Step 5 : `UpdateDialog` devient bête (état + callbacks)**

Remplacer le contenu de `UpdateDialog.kt` par une version qui reçoit `state`, `onInstall`, `onRetry`, `onOpenReleasePage`, `onDismiss` — plus aucune coroutine ni use case dedans :

```kotlin
package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.presentation.features.update.DownloadState
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Dialog « bête » de mise à jour : il affiche l'[state] fourni et émet des callbacks.
 * Toute la logique (téléchargement, progression, erreur) vit dans le ViewModel appelant.
 */
@Composable
fun UpdateDialog(
    info: UpdateInfoDto,
    state: DownloadState,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (state is DownloadState.Idle || state is DownloadState.Error) onDismiss() },
        title = { AppText("Mise à jour disponible", style = AppTextStyle.Title) },
        text = {
            when (state) {
                is DownloadState.Idle -> AppText("La version ${info.version} est disponible.")
                is DownloadState.Downloading ->
                    if (state.progress != null) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTheme.colors.primary,
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTheme.colors.primary,
                        )
                    }
                is DownloadState.Error -> AppText("Le téléchargement a échoué. Réessayez.")
            }
        },
        confirmButton = {
            when (state) {
                is DownloadState.Idle -> AppButton(
                    label = "Installer",
                    onClick = { if (info.downloadUrl == null) onOpenReleasePage() else onInstall() },
                )
                is DownloadState.Error -> AppButton(label = "Réessayer", onClick = onRetry)
                else -> {}
            }
        },
        dismissButton = {
            if (state !is DownloadState.Downloading) {
                AppButton(label = "Plus tard", onClick = onDismiss, variant = ButtonVariant.Ghost)
            }
        },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    )
}
```

- [ ] **Step 6 : Brancher le VM + dialog dans `App.kt`**

Dans `App.kt`, remplacer le bloc :

```kotlin
        updateInfo?.let { info ->
            UpdateDialog(
                info = info,
                onDismiss = { updateInfo = null },
                downloadAndInstall = downloadAndInstall,
            )
        }
```

par :

```kotlin
        updateInfo?.let { info ->
            val updateViewModel = koinViewModel { UpdateViewModel(downloadAndInstall) }
            val downloadState by updateViewModel.state.collectAsStateWithLifecycle()
            UpdateDialog(
                info = info,
                state = downloadState,
                onInstall = { info.downloadUrl?.let(updateViewModel::download) },
                onRetry = { info.downloadUrl?.let(updateViewModel::download) },
                onOpenReleasePage = {
                    runCatching { Desktop.getDesktop().browse(URI(info.releaseUrl)) }
                    updateInfo = null
                },
                onDismiss = { updateInfo = null },
            )
        }
```

Ajouter imports dans `App.kt` : `eu.ejdr.presentation.features.update.UpdateViewModel`, `eu.ejdr.presentation.shared.di.koinViewModel` (vérifier le helper réel dans `presentation/shared/di/KoinViewModel.kt`), `androidx.lifecycle.compose.collectAsStateWithLifecycle`, `androidx.compose.runtime.getValue` (déjà là), `java.awt.Desktop`, `java.net.URI`. Retirer l'import désormais inutile `DownloadAndInstallUpdateUseCase` SI plus référencé — il l'est encore (passé au VM), donc le garder.

> **Vérifier** : le helper `koinViewModel { }` existe-t-il (cf. `KoinViewModel.kt`) ? Les pages l'utilisent. Confirmer la signature exacte. Si `collectAsStateWithLifecycle` n'est pas disponible (dépend de lifecycle-compose), utiliser `collectAsState()` de Compose runtime à la place (import `androidx.compose.runtime.collectAsState`).

- [ ] **Step 7 : `verify` (clôt aussi Task 7 si elle était en suspens)**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8 : Validation runtime (le dialog touche le boot)**

Run: `./gradlew run`
Expected : l'app démarre normalement. (Le dialog n'apparaît que si une MAJ existe — au minimum vérifier l'absence de régression au boot.) Fermer.

- [ ] **Step 9 : Commit (atomique avec Task 7 si elle n'avait pas été committée)**

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/update src/main/kotlin/eu/ejdr/presentation/shared/component/organism/UpdateDialog.kt src/main/kotlin/eu/ejdr/presentation/App.kt src/test/kotlin/eu/ejdr/presentation/features/update
git commit -m "refactor(update): move download state machine into UpdateViewModel, dumb dialog"
```

---

## Task 10 : Décentraliser la DI par feature (Finding F5) 🟡

**Files:**
- Create: `src/main/kotlin/eu/ejdr/di/AuthModule.kt`
- Create: `src/main/kotlin/eu/ejdr/di/SettingsModule.kt`
- Create: `src/main/kotlin/eu/ejdr/di/UpdateModule.kt`
- Modify: `src/main/kotlin/eu/ejdr/di/ApplicationModule.kt` (devient l'agrégat ou est supprimé)
- Modify: `src/main/kotlin/eu/ejdr/di/InfrastructureModule.kt`
- Modify: `src/main/kotlin/eu/ejdr/di/AppKoin.kt`

On découpe les god-modules en modules par feature (auth/settings/update), à l'image de `RealtimeModule`. Chaque module déclare ses bindings application **et** infrastructure de la feature.

- [ ] **Step 1 : Créer `AuthModule`**

Create `src/main/kotlin/eu/ejdr/di/AuthModule.kt` :

```kotlin
package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.auth.abstraction.service.SessionService
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.LoginUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RegisterUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.auth.service.SessionServiceImpl
import eu.ejdr.application.features.auth.usecase.GetCurrentUserUseCaseImpl
import eu.ejdr.application.features.auth.usecase.LoginUseCaseImpl
import eu.ejdr.application.features.auth.usecase.LogoutUseCaseImpl
import eu.ejdr.application.features.auth.usecase.RegisterUseCaseImpl
import eu.ejdr.application.features.auth.usecase.RestoreSessionUseCaseImpl
import eu.ejdr.infrastructure.http.features.auth.AuthHttpMapper
import eu.ejdr.infrastructure.http.features.auth.AuthHttpRepository
import org.koin.dsl.module

/**
 * Module Koin de la feature authentification : ports application (use cases, service) +
 * adaptateurs infrastructure (repository HTTP, mapper). Découpage par feature à l'image de
 * [realtimeModule], pour éviter les god-modules.
 */
val authModule = module {
    single { AuthHttpMapper }
    single<AuthRepository> { AuthHttpRepository(get(), get(), get(), get<SessionPersistence>()) }
    single<SessionService> { SessionServiceImpl(get()) }
    single<LoginUseCase> { LoginUseCaseImpl(get()) }
    single<RegisterUseCase> { RegisterUseCaseImpl(get()) }
    single<RestoreSessionUseCase> { RestoreSessionUseCaseImpl(get()) }
    single<LogoutUseCase> { LogoutUseCaseImpl(get()) }
    single<GetCurrentUserUseCase> { GetCurrentUserUseCaseImpl(get()) }
}
```

- [ ] **Step 2 : Créer `SettingsModule` et `UpdateModule`**

Create `src/main/kotlin/eu/ejdr/di/SettingsModule.kt` :

```kotlin
package eu.ejdr.di

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.features.settings.usecase.GetThemeUseCaseImpl
import eu.ejdr.application.features.settings.usecase.SetThemeUseCaseImpl
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.settings.ThemeFileRepository
import org.koin.dsl.module

/** Module Koin de la feature paramètres (thème). */
val settingsModule = module {
    single<ThemeRepository> { ThemeFileRepository(get<AppConfig>().dataDir) }
    single<GetThemeUseCase> { GetThemeUseCaseImpl(get()) }
    single<SetThemeUseCase> { SetThemeUseCaseImpl(get()) }
}
```

Create `src/main/kotlin/eu/ejdr/di/UpdateModule.kt` :

```kotlin
package eu.ejdr.di

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.features.update.usecase.CheckUpdateUseCaseImpl
import eu.ejdr.application.features.update.usecase.DownloadAndInstallUpdateUseCaseImpl
import eu.ejdr.infrastructure.http.features.update.UpdateHttpRepository
import eu.ejdr.infrastructure.system.WindowsSystemLauncher
import org.koin.dsl.module

/** Module Koin de la feature mise à jour (vérification, téléchargement, lancement OS). */
val updateModule = module {
    single<UpdateRepository> { UpdateHttpRepository(get()) }
    single<SystemLauncherService> { WindowsSystemLauncher() }
    single<CheckUpdateUseCase> { CheckUpdateUseCaseImpl(get()) }
    single<DownloadAndInstallUpdateUseCase> { DownloadAndInstallUpdateUseCaseImpl(get(), get()) }
}
```

- [ ] **Step 3 : Réduire `InfrastructureModule` au socle transverse**

Remplacer le `module { ... }` de `InfrastructureModule.kt` par le seul socle partagé (config, sécurité, HttpClient) ; les bindings par feature ont migré :

```kotlin
val infrastructureModule = module {
    single { AppConfig.load() }
    single { KeyStoreProvider(get<AppConfig>().dataDir) }
    single { CookieCipher(get()) }
    single { SecureCookiesStorage(get<AppConfig>().dataDir, get(), AcceptAllCookiesStorage()) }
    single<SessionPersistence> { get<SecureCookiesStorage>() }
    single<HttpClient> { KtorClientFactory(get(), get<SecureCookiesStorage>()).create() }
}
```

Retirer de `InfrastructureModule.kt` les imports devenus inutiles (`AuthRepository`, `AuthHttpMapper`, `AuthHttpRepository`, `ThemeRepository`, `ThemeFileRepository`, `UpdateRepository`, `UpdateHttpRepository`, `SystemLauncherService`, `WindowsSystemLauncher`). Garder `AppConfig`, `KeyStoreProvider`, `CookieCipher`, `SecureCookiesStorage`, `SessionPersistence`, `KtorClientFactory`, `HttpClient`, `AcceptAllCookiesStorage`. Mettre à jour la KDoc du module pour refléter le périmètre réduit.

- [ ] **Step 4 : Supprimer `ApplicationModule` (vidé) et mettre à jour `AppKoin`**

`ApplicationModule.kt` n'a plus de bindings (tous migrés vers auth/settings/update). Le supprimer :

Run (PowerShell): `Remove-Item src/main/kotlin/eu/ejdr/di/ApplicationModule.kt`

`AppKoin.kt` — charger les nouveaux modules :

```kotlin
package eu.ejdr.di

import org.koin.core.context.startKoin

/**
 * Composition root de l'application : démarre le conteneur Koin.
 *
 * Charge le socle transverse ([infrastructureModule]) puis un module **par feature**
 * ([authModule], [settingsModule], [updateModule], [realtimeModule]). Ajouter une feature
 * = ajouter son module ici, sans toucher aux autres. À appeler une seule fois au démarrage.
 *
 * @return L'application Koin initialisée.
 */
fun initKoin() = startKoin {
    modules(infrastructureModule, authModule, settingsModule, updateModule, realtimeModule)
}
```

- [ ] **Step 5 : `verify`**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6 : Validation runtime OBLIGATOIRE (la DI câble tout)**

Run: `./gradlew run`
Expected : l'app démarre, auth + settings + (éventuel) dialog update fonctionnent — un binding manquant ferait crasher Koin au démarrage (résolution lazy). Tester login→home→settings→logout. Fermer.

- [ ] **Step 7 : Commit**

```bash
git add src/main/kotlin/eu/ejdr/di
git commit -m "refactor(di): split god-modules into per-feature Koin modules"
```

---

## Task 11 : Supprimer `FormState` inutilisé (Finding F10) 🟡

**Files:**
- Delete: `src/main/kotlin/eu/ejdr/presentation/shared/state/FormState.kt`

`FormState`/`rememberFormState` ne sont utilisés nulle part (aucun ViewModel ni page ne les référence ; le pattern réel est ViewModel + StateFlow). On les retire pour éviter la confusion « quel pattern utiliser ».

- [ ] **Step 1 : Confirmer l'absence d'usage**

Run: `./gradlew help -q` non pertinent. Utiliser une recherche de référence :
Run (PowerShell): `Select-String -Path src -Pattern "FormState|rememberFormState" -Recurse | Select-Object -ExpandProperty Path -Unique`
Expected : seul `FormState.kt` (et éventuellement la doc de package) apparaît. Si un consommateur réel apparaît, **ne pas supprimer** — signaler.

- [ ] **Step 2 : Supprimer le fichier**

Run (PowerShell): `Remove-Item src/main/kotlin/eu/ejdr/presentation/shared/state/FormState.kt`

> Si le dossier `state/` devient vide, le laisser (Git ne suit pas les dossiers vides ; sans impact). Si un `package.md` ou une doc mentionne `FormState`, mettre à jour cette mention.

- [ ] **Step 3 : `verify`**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL (aucune référence cassée).

- [ ] **Step 4 : Commit**

```bash
git add -A src/main/kotlin/eu/ejdr/presentation/shared/state
git commit -m "chore(presentation): remove unused FormState (real pattern is ViewModel + StateFlow)"
```

---

## Task 12 : État applicatif global — `AppState` racine (Finding F2) 🟠

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/RootViewModel.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/App.kt`
- Create: `src/test/kotlin/eu/ejdr/presentation/RootViewModelTest.kt`
- Modify: `build.gradle.kts` (Kover : `RootViewModel` est de la logique → compté ; `AppKt` reste exclu)

Aujourd'hui thème + session + updateInfo vivent en `mutableStateOf` ad-hoc dans `App.kt`, propagés par callbacks. On centralise thème et session dans un `RootViewModel` (StateFlow), source de vérité unique. On garde l'orchestration de navigation dans `App.kt` (légitime), mais l'état transverse migre dans le VM.

> Périmètre volontairement borné (YAGNI) : thème + statut de session restaurée. Pas de store générique, pas de notifications/profil tant qu'il n'y a pas de feature qui les exige.

- [ ] **Step 1 : Écrire le test du `RootViewModel` (échoue)**

Create `src/test/kotlin/eu/ejdr/presentation/RootViewModelTest.kt` :

```kotlin
package eu.ejdr.presentation

import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RootViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        theme: ThemeVariant = ThemeVariant.LIGHT,
        restore: Result<User, AuthError> = Result.Failure(AuthError.SessionExpired),
    ) = RootViewModel(
        getTheme = GetThemeUseCase { theme },
        restoreSession = RestoreSessionUseCase { restore },
    )

    @Test
    fun `loads persisted theme on init`() = runTest {
        val rvm = vm(theme = ThemeVariant.DARK)
        advanceUntilIdle()
        assertEquals(ThemeVariant.DARK, rvm.theme.value)
    }

    @Test
    fun `setTheme updates the exposed theme`() = runTest {
        val rvm = vm()
        advanceUntilIdle()
        rvm.setTheme(ThemeVariant.DARK)
        assertEquals(ThemeVariant.DARK, rvm.theme.value)
    }

    @Test
    fun `restoreSession exposes authenticated start destination on success`() = runTest {
        val rvm = vm(restore = Result.Success(User(id = "u1")))
        rvm.restoreSession()
        advanceUntilIdle()
        assertEquals(SessionStatus.Authenticated, rvm.sessionStatus.value)
    }

    @Test
    fun `restoreSession exposes unauthenticated on failure`() = runTest {
        val rvm = vm(restore = Result.Failure(AuthError.SessionExpired))
        rvm.restoreSession()
        advanceUntilIdle()
        assertEquals(SessionStatus.Unauthenticated, rvm.sessionStatus.value)
    }
}
```

> **Vérifier** la signature réelle du constructeur `User` (cf. `domain/features/auth/entities/User.kt`) et de `RestoreSessionUseCase` (cf. son port) ; ajuster `User(id = "u1")` et le lambda `RestoreSessionUseCase { restore }` au contrat réel. Confirmer que `AuthError.SessionExpired` existe (cf. `AuthError.kt`).

- [ ] **Step 2 : Lancer, vérifier l'échec**

Run: `./gradlew test --tests "eu.ejdr.presentation.RootViewModelTest"`
Expected: FAIL — `RootViewModel`/`SessionStatus` introuvables.

- [ ] **Step 3 : Créer `RootViewModel`**

Create `src/main/kotlin/eu/ejdr/presentation/RootViewModel.kt` :

```kotlin
package eu.ejdr.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Statut de la restauration de session au démarrage. */
enum class SessionStatus { Unknown, Authenticated, Unauthenticated }

/**
 * État applicatif **global** : source de vérité unique pour le thème et le statut de
 * session, partagé par tout l'arbre. Remplace les `mutableStateOf` ad-hoc dispersés dans
 * `App.kt` et le « state lifting » manuel par callbacks. Borné volontairement (thème +
 * session) ; toute donnée transverse future (profil, notifications) s'ajoute ici.
 *
 * @param getTheme Lecture du thème persisté (au démarrage).
 * @param restoreSession Tentative d'auto-login depuis la session persistée.
 */
class RootViewModel(
    getTheme: GetThemeUseCase,
    private val restoreSession: RestoreSessionUseCase,
) : ViewModel() {

    private val _theme = MutableStateFlow(ThemeVariant.LIGHT)
    val theme: StateFlow<ThemeVariant> = _theme.asStateFlow()

    private val _sessionStatus = MutableStateFlow(SessionStatus.Unknown)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    init {
        viewModelScope.launch { _theme.value = getTheme() }
    }

    /** Applique un nouveau thème (déjà persisté par la feature settings). */
    fun setTheme(theme: ThemeVariant) { _theme.value = theme }

    /** Lance la restauration de session et publie le résultat dans [sessionStatus]. */
    fun restoreSession() {
        viewModelScope.launch {
            _sessionStatus.value = restoreSession().fold(
                onSuccess = { SessionStatus.Authenticated },
                onFailure = { SessionStatus.Unauthenticated },
            )
        }
    }
}
```

- [ ] **Step 4 : Lancer le test, vérifier le succès**

Run: `./gradlew test --tests "eu.ejdr.presentation.RootViewModelTest"`
Expected: PASS.

- [ ] **Step 5 : Brancher `RootViewModel` dans `App.kt`**

Réécrire `App.kt` pour consommer le `RootViewModel` au lieu des `mutableStateOf` thème/session. Le thème vient de `rootViewModel.theme` ; la navigation initiale réagit à `rootViewModel.sessionStatus` ; `onThemeChange` appelle `rootViewModel.setTheme`. Structure cible :

```kotlin
@Composable
fun App() {
    val rootViewModel = koinViewModel { RootViewModel(koinInject(), koinInject()) }
    val themeVariant by rootViewModel.theme.collectAsStateWithLifecycle()

    AppTheme(
        colors = when (themeVariant) {
            ThemeVariant.LIGHT -> lightColors()
            ThemeVariant.DARK -> darkColors()
        },
    ) {
        val logout = koinInject<LogoutUseCase>()
        val checkUpdate = koinInject<CheckUpdateUseCase>()
        val downloadAndInstall = koinInject<DownloadAndInstallUpdateUseCase>()
        val scope = rememberCoroutineScope()

        val backStack = rememberNavBackStack(appNavConfiguration, Route.Splash)
        var updateInfo by remember { mutableStateOf<UpdateInfoDto?>(null) }
        val sessionStatus by rootViewModel.sessionStatus.collectAsStateWithLifecycle()

        fun resetTo(route: Route) { backStack.clear(); backStack.add(route) }

        LaunchedEffect(Unit) {
            launch { updateInfo = checkUpdate().getOrNull() }
            rootViewModel.restoreSession()
        }
        LaunchedEffect(sessionStatus) {
            when (sessionStatus) {
                SessionStatus.Authenticated -> resetTo(Route.Home)
                SessionStatus.Unauthenticated -> resetTo(Route.Login)
                SessionStatus.Unknown -> Unit
            }
        }

        AppNavDisplay(
            backStack = backStack,
            onLogout = { scope.launch { logout(); resetTo(Route.Login) } },
            onThemeChange = rootViewModel::setTheme,
            resetTo = ::resetTo,
        )

        updateInfo?.let { info ->
            val updateViewModel = koinViewModel { UpdateViewModel(downloadAndInstall) }
            val downloadState by updateViewModel.state.collectAsStateWithLifecycle()
            UpdateDialog(
                info = info,
                state = downloadState,
                onInstall = { info.downloadUrl?.let(updateViewModel::download) },
                onRetry = { info.downloadUrl?.let(updateViewModel::download) },
                onOpenReleasePage = {
                    runCatching { Desktop.getDesktop().browse(URI(info.releaseUrl)) }
                    updateInfo = null
                },
                onDismiss = { updateInfo = null },
            )
        }
    }
}
```

Ajuster les imports en conséquence (retirer `GetThemeUseCase`/`RestoreSessionUseCase` directs, ajouter `RootViewModel`, `SessionStatus`, `koinViewModel`, `collectAsStateWithLifecycle`). Retirer l'import `Result` s'il n'est plus utilisé directement ici.

> **Vérifier** : `koinViewModel { }` peut-il résoudre des deps via `koinInject()`/`get()` dans son bloc ? Regarder comment les pages instancient leurs VM (`LoginPage`/`SettingsPage`) et **reproduire exactement ce pattern** (elles font `koinViewModel { SettingsViewModel(get<...>(), get<...>()) }`). Adapter `RootViewModel(...)` à cette forme : `koinViewModel { RootViewModel(get<GetThemeUseCase>(), get<RestoreSessionUseCase>()) }`.

- [ ] **Step 6 : Ajuster Kover (RootViewModel compté, AppKt exclu)**

Dans `build.gradle.kts`, la liste d'exclusion `classes(...)` doit garder `eu.ejdr.presentation.AppKt` exclu. `RootViewModel` étant sous `eu.ejdr.presentation` (pas un sous-package UI), il est automatiquement **compté** — ne rien ajouter. Vérifier juste que l'exclusion par packages de Task 4 n'inclut pas `eu.ejdr.presentation` racine (elle ne l'inclut pas).

- [ ] **Step 7 : `verify`**

Run: `./gradlew verify`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8 : Validation runtime OBLIGATOIRE**

Run: `./gradlew run`
Expected : démarrage Splash → restauration → Login (ou Home si session). Thème appliqué dès le boot. Changer le thème dans Paramètres → propagation immédiate à tout l'UI. Logout → Login. Fermer.

- [ ] **Step 9 : Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/RootViewModel.kt src/main/kotlin/eu/ejdr/presentation/App.kt src/test/kotlin/eu/ejdr/presentation/RootViewModelTest.kt build.gradle.kts
git commit -m "refactor(presentation): centralize theme + session in a root ViewModel"
```

---

## Task 13 : Mettre à jour la documentation d'architecture

**Files:**
- Modify: `docs/ARCHITECTURE_DECISIONS.md`
- Modify: `docs/AUDIT_ARCHITECTURE.md` (annoter comme traité)

- [ ] **Step 1 : Consigner les décisions**

Lire `docs/ARCHITECTURE_DECISIONS.md` et ajouter une section datée `## 2026-06-12 — Durcissement post-re-audit` listant : contrats use case uniformisés (suspend + Result partout), Result enrichi, navigation distribuée par feature (`*NavEntries.kt`), DI par feature, `RootViewModel` (état global), `UpdateViewModel`, 401 désambiguïsé, FormState retiré, release gated sur CI, ViewModels comptés dans Kover. Mentionner le rappel : nouvelle Route ⇒ `subclass` dans `appNavConfiguration` **et** entry dans le `*NavEntries.kt` de la feature.

- [ ] **Step 2 : Annoter l'audit comme traité**

En tête de `docs/AUDIT_ARCHITECTURE.md`, ajouter un encart : `> **MàJ 2026-06-12** : findings F1–F12 + findings du re-audit (release-CI, Kover, 401) traités — cf. plan docs/superpowers/plans/2026-06-12-reaudit-fixes.md.`

- [ ] **Step 3 : Commit**

```bash
git add docs/ARCHITECTURE_DECISIONS.md docs/AUDIT_ARCHITECTURE.md
git commit -m "docs(architecture): record post-re-audit hardening decisions"
```

---

## Validation finale

- [ ] **`verify` global vert**

Run: `./gradlew clean verify`
Expected: BUILD SUCCESSFUL, detekt 0 violation, couverture ≥ plancher.

- [ ] **Run final**

Run: `./gradlew run`
Expected : parcours complet sans crash — Splash→Login→Register→Login→(login)→Home→Settings (toggle thème light/dark)→retour→Logout→Login.

- [ ] **Revue de cohérence** : tous les use cases sont `suspend (...): Result<_, DomainError>` ; aucun retour nu/nullable/Boolean/exception aux frontières use case. Grep de contrôle :

Run (PowerShell): `Select-String -Path src/main/kotlin/eu/ejdr/application -Pattern "operator fun invoke" -Recurse`
Expected : chaque signature est `suspend operator fun invoke(...): Result<...>` (sauf cas légitimes documentés).
