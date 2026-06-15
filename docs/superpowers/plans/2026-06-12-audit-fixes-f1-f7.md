# Corrections d'audit F1–F7 — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corriger les 7 faiblesses d'audit F1–F7 du frontend E-JDR (cohérence des contrats, navigation décentralisée, état global, DI par feature, couverture, CI release) sans refonte.

**Architecture:** Application desktop Kotlin/Compose en clean architecture (domain/application/infrastructure/presentation), type railway `Result<T, E : DomainError>`, DI Koin, Navigation 3 (back-stack possédé par l'app), ViewModels retenus par destination.

**Tech Stack:** Kotlin 2.2.20 (JVM 21), Compose Desktop 1.11.1, Ktor 3.4.2, Koin 4.1.1, Navigation 3 (navigation3-ui 1.1.1 + lifecycle-viewmodel-navigation3 2.10.0), JUnit5 + MockK + kotlinx-coroutines-test, detekt 1.23.8 + Kover 0.9.1.

---

## Conventions de ce plan

- **Langue** : code et KDoc en **français** (convention du repo). Noms de tests en anglais entre backticks (style existant).
- **Vérification d'étape** : sauf mention contraire, « lancer `verify` » = `.\gradlew.bat verify --no-daemon` (detekt + build + tests + Kover). Les commandes sont en **PowerShell Windows**.
- **Runtime Nav3** : après les tâches touchant `presentation/navigation` ou `App.kt`, lancer `.\gradlew.bat run` et **confirmer visuellement que la fenêtre s'ouvre** (un crash de sérialisation/decorator n'apparaît qu'au lancement).
- **Commits** : Conventional Commits. Hook husky `commit-msg` actif → le message DOIT être conventionnel.
- **Répertoire de travail** : `C:\Users\yomdr\Documents\ProjetDev\Equipe\E-JDR\E-JDR-Frontend` (repo git propre, branche `main`). Tous les chemins ci-dessous sont relatifs à ce répertoire.

---

## Ordre des tâches (dépendances)

F4 (socle, casse des signatures) → F6 (UpdateViewModel) → F1 (navigation) → F2 (AppViewModel) → F5 (DI par feature) → F7 (Kover) → F3 (CI). `verify` vert après chaque tâche.

---

# F4 — Harmonisation des contrats

## Task 1: Enrichir `Result` de combinators

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/application/shared/Result.kt`
- Test: `src/test/kotlin/eu/ejdr/application/shared/ResultTest.kt` (create)

- [ ] **Step 1: Écrire le test qui échoue**

Créer `src/test/kotlin/eu/ejdr/application/shared/ResultTest.kt` :

```kotlin
package eu.ejdr.application.shared

import eu.ejdr.domain.shared.error.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class TestError(override val message: String) : DomainError
private data class OtherError(override val message: String) : DomainError

class ResultTest {

    private val ok: Result<Int, TestError> = Result.Success(2)
    private val ko: Result<Int, TestError> = Result.Failure(TestError("boom"))

    @Test
    fun `map transforms success value and leaves failure untouched`() {
        assertEquals(Result.Success(4), ok.map { it * 2 })
        assertEquals(ko, ko.map { it * 2 })
    }

    @Test
    fun `flatMap chains on success and short-circuits on failure`() {
        assertEquals(Result.Success(4), ok.flatMap { Result.Success(it * 2) })
        val chainedFailure: Result<Int, TestError> = ok.flatMap { Result.Failure(TestError("inner")) }
        assertEquals(Result.Failure(TestError("inner")), chainedFailure)
        assertEquals(ko, ko.flatMap { Result.Success(it * 2) })
    }

    @Test
    fun `mapError transforms failure and leaves success untouched`() {
        assertEquals(Result.Failure(OtherError("boom")), ko.mapError { OtherError(it.message) })
        val mappedSuccess: Result<Int, OtherError> = ok.mapError { OtherError(it.message) }
        assertEquals(Result.Success(2), mappedSuccess)
    }

    @Test
    fun `getOrElse returns value on success and computed default on failure`() {
        assertEquals(2, ok.getOrElse { 0 })
        assertEquals(0, ko.getOrElse { 0 })
    }

    @Test
    fun `getOrNull returns value or null`() {
        assertEquals(2, ok.getOrNull())
        assertNull(ko.getOrNull())
    }

    @Test
    fun `onSuccess and onFailure run the matching side effect and return this`() {
        var seen = 0
        assertEquals(ok, ok.onSuccess { seen = it }.onFailure { seen = -1 })
        assertEquals(2, seen)

        var failed = false
        assertEquals(ko, ko.onFailure { failed = true }.onSuccess { })
        assertTrue(failed)
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `.\gradlew.bat test --tests "eu.ejdr.application.shared.ResultTest" --no-daemon`
Expected: échec de **compilation** (`map`/`flatMap`/`mapError`/`getOrElse`/`getOrNull`/`onSuccess`/`onFailure` non résolus).

- [ ] **Step 3: Implémenter les combinators**

Ajouter à la fin de `src/main/kotlin/eu/ejdr/application/shared/Result.kt` (après `fold`) :

```kotlin
/**
 * Transforme la valeur de succès ; laisse un échec inchangé.
 *
 * @param transform Fonction appliquée à la valeur en cas de succès.
 */
inline fun <T, E : DomainError, R> Result<T, E>.map(
    transform: (T) -> R,
): Result<R, E> = when (this) {
    is Result.Success -> Result.Success(transform(value))
    is Result.Failure -> this
}

/**
 * Enchaîne un second calcul produisant lui-même un [Result] ; court-circuite sur échec.
 *
 * @param transform Calcul appliqué à la valeur de succès, renvoyant un nouveau [Result].
 */
inline fun <T, E : DomainError, R> Result<T, E>.flatMap(
    transform: (T) -> Result<R, E>,
): Result<R, E> = when (this) {
    is Result.Success -> transform(value)
    is Result.Failure -> this
}

/**
 * Transforme l'erreur d'un échec ; laisse un succès inchangé.
 *
 * @param transform Fonction appliquée à l'erreur en cas d'échec.
 */
inline fun <T, E : DomainError, F : DomainError> Result<T, E>.mapError(
    transform: (E) -> F,
): Result<T, F> = when (this) {
    is Result.Success -> this
    is Result.Failure -> Result.Failure(transform(error))
}

/**
 * Renvoie la valeur de succès, ou une valeur de repli calculée depuis l'erreur.
 *
 * @param onFailure Repli calculé à partir de l'erreur en cas d'échec.
 */
inline fun <T, E : DomainError> Result<T, E>.getOrElse(
    onFailure: (E) -> T,
): T = when (this) {
    is Result.Success -> value
    is Result.Failure -> onFailure(error)
}

/** Renvoie la valeur de succès, ou `null` en cas d'échec. */
fun <T, E : DomainError> Result<T, E>.getOrNull(): T? = when (this) {
    is Result.Success -> value
    is Result.Failure -> null
}

/**
 * Exécute un effet de bord si succès, puis renvoie ce même [Result] (chaînable).
 *
 * @param action Effet appliqué à la valeur de succès.
 */
inline fun <T, E : DomainError> Result<T, E>.onSuccess(
    action: (T) -> Unit,
): Result<T, E> = apply { if (this is Result.Success) action(value) }

/**
 * Exécute un effet de bord si échec, puis renvoie ce même [Result] (chaînable).
 *
 * @param action Effet appliqué à l'erreur.
 */
inline fun <T, E : DomainError> Result<T, E>.onFailure(
    action: (E) -> Unit,
): Result<T, E> = apply { if (this is Result.Failure) action(error) }
```

- [ ] **Step 4: Lancer le test pour vérifier le succès**

Run: `.\gradlew.bat test --tests "eu.ejdr.application.shared.ResultTest" --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/eu/ejdr/application/shared/Result.kt src/test/kotlin/eu/ejdr/application/shared/ResultTest.kt
git commit -m "feat(result): ajoute map/flatMap/mapError/getOrElse/getOrNull/onSuccess/onFailure"
```

---

## Task 2: `UpdateError` (erreur métier de la feature update)

**Files:**
- Create: `src/main/kotlin/eu/ejdr/domain/features/update/error/UpdateError.kt`

- [ ] **Step 1: Créer la sealed class**

Créer `src/main/kotlin/eu/ejdr/domain/features/update/error/UpdateError.kt` :

```kotlin
package eu.ejdr.domain.features.update.error

import eu.ejdr.domain.shared.error.DomainError

/**
 * Erreurs métier de la feature mise à jour.
 *
 * `sealed class` propre à la feature (même contrat que [eu.ejdr.domain.features.auth.error.AuthError]
 * et [eu.ejdr.domain.features.settings.error.SettingsError]) : `when` exhaustif, variante de
 * [DomainError], chaque cas porte un message utilisateur prêt à afficher. [Unknown] isole le
 * détail technique du message générique (pas de fuite serveur à l'écran).
 */
sealed class UpdateError(override val message: String) : DomainError {
    /** Le téléchargement de l'installeur a échoué (réseau, disque, flux interrompu). */
    data object DownloadFailed :
        UpdateError("Le téléchargement de la mise à jour a échoué. Réessayez.")

    /** Le lancement de l'installeur a échoué. */
    data object InstallFailed :
        UpdateError("Le lancement de l'installation a échoué. Réessayez.")

    /** Échec technique inattendu ; [detail] reste hors affichage. */
    data class Unknown(val detail: String) :
        UpdateError("Une erreur inattendue est survenue pendant la mise à jour.")
}
```

- [ ] **Step 2: Compiler**

Run: `.\gradlew.bat compileKotlin --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/eu/ejdr/domain/features/update/error/UpdateError.kt
git commit -m "feat(update): introduit UpdateError (erreurs métier typées)"
```

---

## Task 3: `UpdateRepository` + impl renvoient `Result` (+ timeout download)

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/application/features/update/abstraction/repository/UpdateRepository.kt`
- Modify: `src/main/kotlin/eu/ejdr/infrastructure/http/features/update/UpdateHttpRepository.kt`
- Test: `src/test/kotlin/eu/ejdr/infrastructure/http/features/update/UpdateHttpRepositoryTest.kt`

- [ ] **Step 1: Mettre à jour le test existant pour la nouvelle signature**

Lire d'abord le test existant : `src/test/kotlin/eu/ejdr/infrastructure/http/features/update/UpdateHttpRepositoryTest.kt`. Adapter les assertions de `downloadUpdate` : le retour est désormais `Result<File, UpdateError>`. Remplacer toute assertion `assertEquals(file, repo.downloadUpdate(...))` / accès direct au `File` par :

```kotlin
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError
import kotlin.test.assertIs

// cas succès :
val result = repo.downloadUpdate(url) { }
assertIs<Result.Success<java.io.File>>(result)
assertTrue(result.value.exists())

// (si un cas d'échec réseau est simulé) :
// assertIs<Result.Failure<UpdateError>>(result)
```

*(Conserver les cas `fetchLatestRelease` inchangés : sa signature ne bouge pas.)*

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `.\gradlew.bat test --tests "eu.ejdr.infrastructure.http.features.update.UpdateHttpRepositoryTest" --no-daemon`
Expected: échec de compilation (signature `downloadUpdate` incompatible).

- [ ] **Step 3: Mettre à jour le port**

Remplacer `src/main/kotlin/eu/ejdr/application/features/update/abstraction/repository/UpdateRepository.kt` par :

```kotlin
package eu.ejdr.application.features.update.abstraction.repository

import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError

interface UpdateRepository {
    /** Récupère la dernière release publiée, ou `null` si aucune/indisponible (pas d'erreur métier). */
    suspend fun fetchLatestRelease(): UpdateInfoDto?

    /**
     * Télécharge l'installeur depuis [url] en signalant la progression via [onProgress].
     *
     * @return [Result.Success] avec le fichier téléchargé, ou [Result.Failure] avec un
     * [UpdateError] (échec réseau, écriture, flux interrompu).
     */
    suspend fun downloadUpdate(url: String, onProgress: (Float?) -> Unit): Result<java.io.File, UpdateError>
}
```

- [ ] **Step 4: Mettre à jour l'implémentation (Result + timeout)**

Remplacer `src/main/kotlin/eu/ejdr/infrastructure/http/features/update/UpdateHttpRepository.kt` par :

```kotlin
package eu.ejdr.infrastructure.http.features.update

import eu.ejdr.BuildConfig
import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.runCatchingCancellable
import eu.ejdr.domain.features.update.error.UpdateError
import eu.ejdr.infrastructure.http.features.update.dto.GitHubReleaseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import java.io.File

private const val GITHUB_API = "https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest"
private const val DOWNLOAD_TIMEOUT_MS = 5 * 60 * 1000L

class UpdateHttpRepository(private val client: HttpClient) : UpdateRepository {

    override suspend fun fetchLatestRelease(): UpdateInfoDto? = runCatchingCancellable {
        val response = client.get(GITHUB_API)
        if (!response.status.isSuccess()) return null
        val dto = response.body<GitHubReleaseDto>()
        val asset = dto.assets.firstOrNull { it.name.endsWith(".exe") }
            ?: dto.assets.firstOrNull { it.name.endsWith(".msi") }
        UpdateInfoDto(version = dto.tagName, releaseUrl = dto.htmlUrl, downloadUrl = asset?.browserDownloadUrl)
    }.getOrNull()

    override suspend fun downloadUpdate(
        url: String,
        onProgress: (Float?) -> Unit,
    ): Result<File, UpdateError> = runCatchingCancellable {
        val file = File(System.getProperty("java.io.tmpdir"), "E-JDR-update.exe")
        client.prepareGet(url) {
            timeout { requestTimeoutMillis = DOWNLOAD_TIMEOUT_MS }
        }.execute { response ->
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLong()
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(8 * 1024)
            var downloaded = 0L
            file.outputStream().buffered().use { output ->
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        onProgress(contentLength?.let { downloaded.toFloat() / it })
                    }
                }
            }
        }
        file
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Failure(UpdateError.DownloadFailed) },
    )
}
```

NOTE: `timeout { }` exige le plugin `HttpTimeout`. Vérifier qu'il est installé dans `KtorClientFactory` ; sinon l'ajouter (`install(HttpTimeout)`).

- [ ] **Step 5: Vérifier le plugin HttpTimeout**

Lire `src/main/kotlin/eu/ejdr/infrastructure/http/KtorClientFactory.kt`. Si `HttpTimeout` n'est pas installé, ajouter dans le bloc `HttpClient(...) { ... }` :

```kotlin
install(io.ktor.client.plugins.HttpTimeout)
```

- [ ] **Step 6: Lancer le test pour vérifier le succès**

Run: `.\gradlew.bat test --tests "eu.ejdr.infrastructure.http.features.update.UpdateHttpRepositoryTest" --no-daemon`
Expected: PASS.

- [ ] **Step 7: Commit** *(le use case casse encore la compilation globale — il sera réparé Task 4 ; ne PAS lancer `verify` complet ici, seulement le test ciblé ci-dessus)*

```bash
git add src/main/kotlin/eu/ejdr/application/features/update/abstraction/repository/UpdateRepository.kt src/main/kotlin/eu/ejdr/infrastructure/http/features/update/UpdateHttpRepository.kt src/test/kotlin/eu/ejdr/infrastructure/http/features/update/UpdateHttpRepositoryTest.kt
git commit -m "refactor(update): downloadUpdate renvoie Result<File, UpdateError> + timeout"
```

---

## Task 4: `DownloadAndInstallUpdateUseCase` renvoie `Result`

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/application/features/update/abstraction/usecase/DownloadAndInstallUpdateUseCase.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/update/usecase/DownloadAndInstallUpdateUseCaseImpl.kt`
- Test: `src/test/kotlin/eu/ejdr/application/features/update/usecase/DownloadAndInstallUpdateUseCaseImplTest.kt`

- [ ] **Step 1: Réécrire le test**

Remplacer `src/test/kotlin/eu/ejdr/application/features/update/usecase/DownloadAndInstallUpdateUseCaseImplTest.kt` par :

```kotlin
package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class DownloadAndInstallUpdateUseCaseImplTest {

    private val repository = mockk<UpdateRepository>()
    private val launcher = mockk<SystemLauncherService>()
    private val useCase = DownloadAndInstallUpdateUseCaseImpl(repository, launcher)

    @Test
    fun `downloads then launches the installer on success`() = runTest {
        val file = File.createTempFile("installer", ".exe").apply { deleteOnExit() }
        coEvery { repository.downloadUpdate(any(), any()) } returns Result.Success(file)
        every { launcher.launchInstallerAndExit(file) } throws ExitInvoked

        val result = runCatching { useCase("http://x/installer.exe") { } }

        verify { launcher.launchInstallerAndExit(file) }
        // le launcher quitte la JVM : on l'a simulé par une exception sentinelle
        assertIs<ExitInvoked>(result.exceptionOrNull())
    }

    @Test
    fun `returns failure without launching when download fails`() = runTest {
        coEvery { repository.downloadUpdate(any(), any()) } returns Result.Failure(UpdateError.DownloadFailed)

        val result = useCase("http://x/installer.exe") { }

        assertIs<Result.Failure<UpdateError>>(result)
        verify(exactly = 0) { launcher.launchInstallerAndExit(any()) }
    }
}

private object ExitInvoked : RuntimeException()
```

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `.\gradlew.bat test --tests "eu.ejdr.application.features.update.usecase.DownloadAndInstallUpdateUseCaseImplTest" --no-daemon`
Expected: échec de compilation (signature `invoke` incompatible).

- [ ] **Step 3: Mettre à jour le port**

Remplacer `src/main/kotlin/eu/ejdr/application/features/update/abstraction/usecase/DownloadAndInstallUpdateUseCase.kt` par :

```kotlin
package eu.ejdr.application.features.update.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError

fun interface DownloadAndInstallUpdateUseCase {
    /**
     * Télécharge puis lance l'installeur. En cas de succès, le launcher **quitte la JVM**
     * (la fonction ne rend alors jamais la main). En cas d'échec **avant** le lancement,
     * renvoie [Result.Failure] au lieu de laisser une exception remonter.
     */
    suspend operator fun invoke(
        downloadUrl: String,
        onProgress: (Float?) -> Unit,
    ): Result<Unit, UpdateError>
}
```

- [ ] **Step 4: Mettre à jour l'impl**

Remplacer le corps de `src/main/kotlin/eu/ejdr/application/features/update/usecase/DownloadAndInstallUpdateUseCaseImpl.kt` :

```kotlin
package eu.ejdr.application.features.update.usecase

import eu.ejdr.application.features.update.abstraction.repository.UpdateRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.application.shared.map
import eu.ejdr.domain.features.update.error.UpdateError

/**
 * Implémentation de [DownloadAndInstallUpdateUseCase].
 *
 * Orchestration pure : télécharge l'installeur via l'[UpdateRepository] puis délègue le
 * lancement et la fermeture de l'application au [SystemLauncherService]. Les effets OS
 * (processus externe, exit JVM) restent hors de la couche application (launcher mockable).
 * Un échec de téléchargement est propagé en [Result.Failure] sans atteindre le launcher.
 */
class DownloadAndInstallUpdateUseCaseImpl(
    private val updateRepository: UpdateRepository,
    private val systemLauncher: SystemLauncherService,
) : DownloadAndInstallUpdateUseCase {
    override suspend fun invoke(
        downloadUrl: String,
        onProgress: (Float?) -> Unit,
    ): Result<Unit, UpdateError> =
        updateRepository.downloadUpdate(downloadUrl, onProgress).map { installer ->
            systemLauncher.launchInstallerAndExit(installer)
        }
}
```

NOTE: `launchInstallerAndExit` renvoie `Nothing`, donc `map { … }` produit bien `Result<Unit, UpdateError>` (le lambda retourne `Nothing`, sous-type de `Unit`). En succès réel la JVM quitte avant le retour.

- [ ] **Step 5: Lancer le test pour vérifier le succès**

Run: `.\gradlew.bat test --tests "eu.ejdr.application.features.update.usecase.DownloadAndInstallUpdateUseCaseImplTest" --no-daemon`
Expected: PASS.

- [ ] **Step 6: Commit** *(la présentation `UpdateDialog`/`App.kt` casse encore — réparée en F6 ; test ciblé OK suffit ici)*

```bash
git add src/main/kotlin/eu/ejdr/application/features/update/abstraction/usecase/DownloadAndInstallUpdateUseCase.kt src/main/kotlin/eu/ejdr/application/features/update/usecase/DownloadAndInstallUpdateUseCaseImpl.kt src/test/kotlin/eu/ejdr/application/features/update/usecase/DownloadAndInstallUpdateUseCaseImplTest.kt
git commit -m "refactor(update): DownloadAndInstallUpdateUseCase renvoie Result<Unit, UpdateError>"
```

---

## Task 5: Settings — `getTheme`/`setTheme` suspend + `Result`

> ⚠️ Cascade : rendre `GetThemeUseCase`/`SetThemeUseCase` suspend casse `SettingsViewModel` (init dans le constructeur + `onThemeSelected` synchrone) et `App.kt` (`getTheme()` dans le constructeur). Ces deux-là sont réparés dans cette tâche (SettingsViewModel) et en F2 (App.kt). On câble ici tout sauf App.kt.

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/abstraction/repository/ThemeRepository.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/abstraction/usecase/GetThemeUseCase.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/usecase/GetThemeUseCaseImpl.kt`
- Modify: `src/main/kotlin/eu/ejdr/application/features/settings/usecase/SetThemeUseCaseImpl.kt`
- Modify: `src/main/kotlin/eu/ejdr/infrastructure/settings/ThemeFileRepository.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/settings/SettingsViewModel.kt`
- Tests: `GetThemeUseCaseImplTest.kt`, `SetThemeUseCaseImplTest.kt`, `ThemeFileRepositoryTest.kt`, `SettingsViewModelTest.kt`

- [ ] **Step 1: Mettre à jour les ports**

`ThemeRepository.kt` :

```kotlin
package eu.ejdr.application.features.settings.abstraction.repository

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError

interface ThemeRepository {
    /**
     * Lit le thème persisté. Absence de fichier = repli sûr sur [ThemeVariant.LIGHT] (succès) ;
     * seul un échec de lecture réel produit un [Result.Failure].
     */
    suspend fun getTheme(): Result<ThemeVariant, SettingsError>

    /** Persiste le thème choisi. [Result.Failure] avec [SettingsError.ThemePersistenceFailed] si l'écriture échoue. */
    suspend fun setTheme(theme: ThemeVariant): Result<Unit, SettingsError>
}
```

`GetThemeUseCase.kt` :

```kotlin
package eu.ejdr.application.features.settings.abstraction.usecase

import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError

fun interface GetThemeUseCase {
    suspend operator fun invoke(): Result<ThemeVariant, SettingsError>
}
```

*(`SetThemeUseCase.kt` : ajouter `suspend` au `operator fun invoke`. Le reste du KDoc reste valide.)*

```kotlin
fun interface SetThemeUseCase {
    suspend operator fun invoke(theme: ThemeVariant): Result<Unit, SettingsError>
}
```

- [ ] **Step 2: Mettre à jour les impls application**

`GetThemeUseCaseImpl.kt` :

```kotlin
package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError

class GetThemeUseCaseImpl(private val repository: ThemeRepository) : GetThemeUseCase {
    override suspend fun invoke(): Result<ThemeVariant, SettingsError> = repository.getTheme()
}
```

`SetThemeUseCaseImpl.kt` : remplacer le corps qui s'appuyait sur le `Boolean` par une délégation directe (le repo renvoie déjà un `Result`) :

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

- [ ] **Step 3: Mettre à jour l'impl infrastructure**

`ThemeFileRepository.kt` :

```kotlin
package eu.ejdr.infrastructure.settings

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError
import java.io.File
import java.util.Properties

class ThemeFileRepository(dataDir: File) : ThemeRepository {

    private val file = File(dataDir, "settings.properties")

    override suspend fun getTheme(): Result<ThemeVariant, SettingsError> {
        if (!file.exists()) return Result.Success(ThemeVariant.LIGHT)
        val theme = runCatching {
            Properties().apply { file.inputStream().use { load(it) } }
                .getProperty("theme")
                ?.let { runCatching { ThemeVariant.valueOf(it) }.getOrNull() }
                ?: ThemeVariant.LIGHT
        }.getOrDefault(ThemeVariant.LIGHT)
        return Result.Success(theme)
    }

    override suspend fun setTheme(theme: ThemeVariant): Result<Unit, SettingsError> =
        runCatching {
            val props = Properties()
            if (file.exists()) file.inputStream().use { props.load(it) }
            props.setProperty("theme", theme.name)
            file.outputStream().use { props.store(it, null) }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Failure(SettingsError.ThemePersistenceFailed) },
        )
}
```

NOTE: lecture absente/corrompue reste un **succès** (repli LIGHT), conformément au spec F4.b.

- [ ] **Step 4: Refactor `SettingsViewModel` (init async + onThemeSelected suspend-safe)**

Remplacer `src/main/kotlin/eu/ejdr/presentation/features/settings/SettingsViewModel.kt` par :

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
 * Le thème courant ([currentTheme]) est initialisé de façon asynchrone depuis [GetThemeUseCase]
 * (lecture I/O désormais `suspend`) et chaque changement est persisté via [SetThemeUseCase]. Le
 * ViewModel étant retenu par la destination, l'état survit à la recomposition.
 *
 * La persistance peut échouer : l'état observé n'est alors PAS modifié (pas de désync UI ↔ disque)
 * et un message est exposé via [error]. La sélection d'un thème efface l'erreur précédente. Le
 * callback [onApplied] notifie l'appelant d'un changement persisté avec succès (propagation au
 * design system global).
 */
class SettingsViewModel(
    private val getTheme: GetThemeUseCase,
    private val setTheme: SetThemeUseCase,
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(ThemeVariant.LIGHT)
    val currentTheme: StateFlow<ThemeVariant> = _currentTheme.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            getTheme().fold(
                onSuccess = { _currentTheme.value = it },
                onFailure = { /* repli LIGHT déjà en place */ },
            )
        }
    }

    /**
     * Tente d'appliquer et de **persister** le thème choisi, puis invoque [onApplied] si succès.
     *
     * @param theme Nouveau thème sélectionné.
     * @param onApplied Appelé avec le thème persisté en cas de succès (ex. propagation globale).
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

NOTE: la signature de `onThemeSelected` change (asynchrone + callback `onApplied` au lieu d'un `Boolean` de retour). `SettingsPage` doit être adaptée — voir Step 7.

- [ ] **Step 5: Réécrire `SettingsViewModelTest`**

Remplacer `src/test/kotlin/eu/ejdr/presentation/features/settings/SettingsViewModelTest.kt` par :

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

    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads the initial theme from the get use case`() = runTest {
        val vm = SettingsViewModel(
            getTheme = GetThemeUseCase { Result.Success(ThemeVariant.DARK) },
            setTheme = SetThemeUseCase { Result.Success(Unit) },
        )
        advanceUntilIdle()
        assertEquals(ThemeVariant.DARK, vm.currentTheme.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `successful selection persists, updates state and notifies`() = runTest {
        val persisted = mutableListOf<ThemeVariant>()
        val applied = mutableListOf<ThemeVariant>()
        val vm = SettingsViewModel(
            getTheme = GetThemeUseCase { Result.Success(ThemeVariant.LIGHT) },
            setTheme = SetThemeUseCase { theme -> persisted.add(theme); Result.Success(Unit) },
        )
        advanceUntilIdle()

        vm.onThemeSelected(ThemeVariant.DARK) { applied.add(it) }
        advanceUntilIdle()

        assertEquals(listOf(ThemeVariant.DARK), persisted)
        assertEquals(listOf(ThemeVariant.DARK), applied)
        assertEquals(ThemeVariant.DARK, vm.currentTheme.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `failed persistence keeps state unchanged, exposes error, does not notify`() = runTest {
        val applied = mutableListOf<ThemeVariant>()
        val vm = SettingsViewModel(
            getTheme = GetThemeUseCase { Result.Success(ThemeVariant.LIGHT) },
            setTheme = SetThemeUseCase { Result.Failure(SettingsError.ThemePersistenceFailed) },
        )
        advanceUntilIdle()

        vm.onThemeSelected(ThemeVariant.DARK) { applied.add(it) }
        advanceUntilIdle()

        assertEquals(ThemeVariant.LIGHT, vm.currentTheme.value)
        assertEquals(SettingsError.ThemePersistenceFailed.message, vm.error.value)
        assertEquals(emptyList(), applied)
    }
}
```

- [ ] **Step 6: Réécrire les tests use case + repo**

`GetThemeUseCaseImplTest.kt` :

```kotlin
package eu.ejdr.application.features.settings.usecase

import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetThemeUseCaseImplTest {

    private val repository = mockk<ThemeRepository>()
    private val useCase = GetThemeUseCaseImpl(repository)

    @Test
    fun `delegates to the repository and returns the persisted theme`() = runTest {
        coEvery { repository.getTheme() } returns Result.Success(ThemeVariant.DARK)
        assertEquals(Result.Success(ThemeVariant.DARK), useCase())
    }
}
```

`SetThemeUseCaseImplTest.kt` : lire l'existant et adapter — `repository.setTheme(...)` est `coEvery` et renvoie un `Result`, `useCase(...)` est appelé dans `runTest`. Exemple :

```kotlin
@Test
fun `delegates persistence to the repository`() = runTest {
    coEvery { repository.setTheme(ThemeVariant.DARK) } returns Result.Success(Unit)
    assertEquals(Result.Success(Unit), useCase(ThemeVariant.DARK))
}
```

`ThemeFileRepositoryTest.kt` : lire l'existant et envelopper chaque appel dans `runTest { }` ; les assertions passent de `assertEquals(ThemeVariant.X, repo.getTheme())` à `assertEquals(Result.Success(ThemeVariant.X), repo.getTheme())` et de `assertTrue(repo.setTheme(...))` à `assertIs<Result.Success<Unit>>(repo.setTheme(...))`.

- [ ] **Step 7: Adapter `SettingsPage` à la nouvelle signature**

Dans `src/main/kotlin/eu/ejdr/presentation/features/settings/page/SettingsPage.kt`, remplacer le bloc `onThemeChange` passé à `SettingsForm` (qui faisait `if (viewModel.onThemeSelected(newTheme)) onThemeChange(newTheme)`) par la délégation au nouveau callback asynchrone :

```kotlin
SettingsForm(
    currentTheme = currentTheme,
    // La propagation au design system global ([onThemeChange]) n'a lieu que si la
    // persistance réussit : le VM appelle onApplied uniquement en cas de succès.
    onThemeChange = { newTheme -> viewModel.onThemeSelected(newTheme, onApplied = onThemeChange) },
)
```

*(Le `onThemeChange: (ThemeVariant) -> Unit` reçu par la page est passé tel quel comme `onApplied`. La signature de `SettingsForm` ne change pas.)*

- [ ] **Step 8: Lancer les tests settings + presentation**

Run: `.\gradlew.bat test --tests "eu.ejdr.application.features.settings.*" --tests "eu.ejdr.infrastructure.settings.*" --tests "eu.ejdr.presentation.features.settings.*" --no-daemon`
Expected: PASS.

- [ ] **Step 9: Commit** *(App.kt casse encore via `getTheme()` constructeur — réparé F2 ; ne pas lancer verify complet)*

```bash
git add src/main/kotlin/eu/ejdr/application/features/settings src/main/kotlin/eu/ejdr/infrastructure/settings src/main/kotlin/eu/ejdr/presentation/features/settings src/test/kotlin/eu/ejdr/application/features/settings src/test/kotlin/eu/ejdr/infrastructure/settings/ThemeFileRepositoryTest.kt src/test/kotlin/eu/ejdr/presentation/features/settings
git commit -m "refactor(settings): getTheme/setTheme suspend + Result, SettingsViewModel async"
```

---

## Task 6: `hasPersistedSession` suspend

**Files:**
- Modify: `AuthRepository.kt`, `SessionPersistence.kt`, `SessionService.kt`, `SessionServiceImpl.kt`, `SecureCookiesStorage.kt`, `AuthHttpRepository.kt` (impl du port)
- Tests: `SessionServiceImplTest.kt`, `SecureCookiesStorageTest.kt`, `AuthHttpRepositoryTest.kt` (si touchés)

- [ ] **Step 1: Ajouter `suspend` aux ports**

- `AuthRepository.hasPersistedSession()` → `suspend fun hasPersistedSession(): Boolean`
- `SessionPersistence.hasPersistedSession()` → `suspend fun hasPersistedSession(): Boolean`
- `SessionService.hasPersistedSession()` → `suspend fun hasPersistedSession(): Boolean`

- [ ] **Step 2: Ajouter `suspend` aux impls**

- `SecureCookiesStorage.hasPersistedSession()` → `override suspend fun hasPersistedSession(): Boolean = storeFile.exists()`
- `SessionServiceImpl.hasPersistedSession()` → `override suspend fun hasPersistedSession(): Boolean = authRepository.hasPersistedSession()`
- `AuthHttpRepository` : trouver l'`override fun hasPersistedSession()` et ajouter `suspend`. Si elle délègue à `sessionPersistence.hasPersistedSession()`, l'appel devient légal en contexte suspend.

- [ ] **Step 3: Mettre à jour les tests impactés**

Lire `SessionServiceImplTest.kt`, `SecureCookiesStorageTest.kt`, `AuthHttpRepositoryTest.kt`. Pour chaque test appelant `hasPersistedSession()` : l'envelopper dans `runTest { }` (s'il ne l'est pas déjà) et remplacer un éventuel `every { … }` MockK par `coEvery { … }`.

- [ ] **Step 4: Lancer les tests auth**

Run: `.\gradlew.bat test --tests "eu.ejdr.application.features.auth.*" --tests "eu.ejdr.infrastructure.security.*" --tests "eu.ejdr.infrastructure.http.features.auth.*" --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/eu/ejdr/application/features/auth src/main/kotlin/eu/ejdr/infrastructure/security/SecureCookiesStorage.kt src/main/kotlin/eu/ejdr/infrastructure/http/features/auth src/test/kotlin/eu/ejdr/application/features/auth src/test/kotlin/eu/ejdr/infrastructure/security
git commit -m "refactor(auth): hasPersistedSession devient suspend (cohérence des ports)"
```

> À ce stade le projet **ne compile pas encore en entier** (`App.kt`/`UpdateDialog` consomment les anciennes signatures). C'est attendu : F6 puis F2 referment la boucle. La première exécution de `verify` complet vert aura lieu à la fin de F2 (Task 11).

---

# F6 — `UpdateViewModel` (UpdateDialog devient bête)

## Task 7: `UpdateViewModel` + état extrait

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/features/update/UpdateViewModel.kt`
- Test: `src/test/kotlin/eu/ejdr/presentation/features/update/UpdateViewModelTest.kt`

- [ ] **Step 1: Écrire le test qui échoue**

Créer `src/test/kotlin/eu/ejdr/presentation/features/update/UpdateViewModelTest.kt` :

```kotlin
package eu.ejdr.presentation.features.update

import eu.ejdr.application.features.update.dto.UpdateInfoDto
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

    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val info = UpdateInfoDto(version = "1.2.0", releaseUrl = "http://x/release", downloadUrl = "http://x/app.exe")

    @Test
    fun `starts Idle`() = runTest {
        val vm = UpdateViewModel(info) { _, _ -> Result.Success(Unit) }
        assertIs<UpdateUiState.Idle>(vm.state.value)
    }

    @Test
    fun `install failure moves to Error with the domain message`() = runTest {
        val vm = UpdateViewModel(info) { _, _ -> Result.Failure(UpdateError.DownloadFailed) }
        vm.onInstall()
        advanceUntilIdle()
        val s = vm.state.value
        assertIs<UpdateUiState.Error>(s)
        assertEquals(UpdateError.DownloadFailed.message, s.message)
    }

    @Test
    fun `install reports progress while downloading`() = runTest {
        val vm = UpdateViewModel(info) { _, onProgress -> onProgress(0.5f); Result.Failure(UpdateError.DownloadFailed) }
        vm.onInstall()
        advanceUntilIdle()
        assertIs<UpdateUiState.Error>(vm.state.value)
    }
}
```

NOTE: le VM prend le use case comme **fonction** `suspend (String, (Float?) -> Unit) -> Result<Unit, UpdateError>` pour la testabilité (même esprit que `AuthViewModel.submit`). En prod on lui passe `downloadAndInstall::invoke`.

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `.\gradlew.bat test --tests "eu.ejdr.presentation.features.update.UpdateViewModelTest" --no-daemon`
Expected: échec de compilation (`UpdateViewModel`/`UpdateUiState` absents).

- [ ] **Step 3: Implémenter le VM + l'état**

Créer `src/main/kotlin/eu/ejdr/presentation/features/update/UpdateViewModel.kt` :

```kotlin
package eu.ejdr.presentation.features.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.update.error.UpdateError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** État affichable du dialogue de mise à jour. */
sealed interface UpdateUiState {
    /** Mise à jour disponible, en attente d'action utilisateur. */
    data object Idle : UpdateUiState

    /** Téléchargement en cours ; [progress] est `null` tant que la taille totale est inconnue. */
    data class Downloading(val progress: Float?) : UpdateUiState

    /** Échec : [message] est le message utilisateur de l'[UpdateError]. */
    data class Error(val message: String) : UpdateUiState
}

/**
 * ViewModel du dialogue de mise à jour : sort la machine d'état hors du composable
 * (qui redevient « bête »). L'effet métier (télécharger + installer) est injecté comme
 * fonction pour la testabilité ; en succès réel le launcher quitte la JVM avant le retour.
 *
 * @property info Métadonnées de la release disponible.
 * @property downloadAndInstall Effet : télécharge puis installe, renvoyant un [Result].
 */
class UpdateViewModel(
    val info: UpdateInfoDto,
    private val downloadAndInstall: suspend (String, (Float?) -> Unit) -> Result<Unit, UpdateError>,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    /** Lance le téléchargement+installation pour l'URL portée par [info]. No-op si pas d'URL. */
    fun onInstall() {
        val url = info.downloadUrl ?: return
        _state.value = UpdateUiState.Downloading(null)
        viewModelScope.launch {
            val result = downloadAndInstall(url) { progress ->
                _state.value = UpdateUiState.Downloading(progress)
            }
            if (result is Result.Failure) _state.value = UpdateUiState.Error(result.error.message)
        }
    }

    /** Relance après un échec. */
    fun onRetry() = onInstall()
}
```

- [ ] **Step 4: Lancer le test pour vérifier le succès**

Run: `.\gradlew.bat test --tests "eu.ejdr.presentation.features.update.UpdateViewModelTest" --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/update/UpdateViewModel.kt src/test/kotlin/eu/ejdr/presentation/features/update/UpdateViewModelTest.kt
git commit -m "feat(update): UpdateViewModel extrait la machine d'etat du dialog"
```

---

## Task 8: `UpdateDialog` devient un composant bête

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/shared/component/organism/UpdateDialog.kt`

- [ ] **Step 1: Réécrire le composable (props + callbacks, plus de state métier)**

Remplacer `src/main/kotlin/eu/ejdr/presentation/shared/component/organism/UpdateDialog.kt` par :

```kotlin
package eu.ejdr.presentation.shared.component.organism

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.presentation.features.update.UpdateUiState
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.component.atomic.ButtonVariant
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Dialogue de mise à jour (composant BÊTE : props + callbacks, aucun état métier).
 *
 * L'état [state] et les actions [onInstall]/[onRetry] proviennent d'un
 * [eu.ejdr.presentation.features.update.UpdateViewModel]. Le cas « pas d'URL de
 * téléchargement » (ouverture de la page release dans le navigateur) est un effet UI pur
 * géré par l'appelant via [onOpenReleasePage].
 */
@Composable
fun UpdateDialog(
    info: UpdateInfoDto,
    state: UpdateUiState,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (state !is UpdateUiState.Downloading) onDismiss() },
        title = { AppText("Mise à jour disponible", style = AppTextStyle.Title) },
        text = {
            when (state) {
                is UpdateUiState.Idle -> AppText("La version ${info.version} est disponible.")
                is UpdateUiState.Downloading ->
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
                is UpdateUiState.Error -> AppText(state.message)
            }
        },
        confirmButton = {
            when (state) {
                is UpdateUiState.Idle -> AppButton(
                    label = "Installer",
                    onClick = { if (info.downloadUrl == null) onOpenReleasePage() else onInstall() },
                )
                is UpdateUiState.Error -> AppButton(label = "Réessayer", onClick = onRetry)
                is UpdateUiState.Downloading -> {}
            }
        },
        dismissButton = {
            if (state !is UpdateUiState.Downloading) {
                AppButton(label = "Plus tard", onClick = onDismiss, variant = ButtonVariant.Ghost)
            }
        },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(AppTheme.dimens.radiusMd),
    )
}
```

NOTE: le câblage de `UpdateDialog` dans `App.kt` (création du `UpdateViewModel`, gestion de `onOpenReleasePage` via `Desktop.browse`) se fait en F2/Task 11. Ne pas compiler le projet entier ici.

- [ ] **Step 2: Commit** *(projet entier encore cassé — referme en F2)*

```bash
git add src/main/kotlin/eu/ejdr/presentation/shared/component/organism/UpdateDialog.kt
git commit -m "refactor(update): UpdateDialog devient un composant bete (state + callbacks)"
```

---

# F1 — Navigation décentralisée par feature

## Task 9: Registre d'entries + routes par feature + test de garde

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/features/auth/navigation/AuthNavigation.kt`
- Create: `src/main/kotlin/eu/ejdr/presentation/features/settings/navigation/SettingsNavigation.kt`
- Create: `src/main/kotlin/eu/ejdr/presentation/features/user/navigation/UserNavigation.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/navigation/Routes.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt`
- Test: `src/test/kotlin/eu/ejdr/presentation/navigation/RoutesRegistrationTest.kt` (create)

> ⚠️ Découverte du spike requise AVANT d'écrire le code : ouvrir la déclaration de `entryProvider`/`entry` dans navigation3-ui 1.1.1 et noter (a) le **type exact du récepteur** du lambda `entryProvider { }` (candidat : `EntryProviderBuilder<NavKey>` ou `EntryProviderScope`), (b) l'emplacement réel de `rememberEjdrViewModelStoreNavEntryDecorator` (top-level vs objet). Remplacer `EntryProviderBuilderScope` ci-dessous par le type réel dans les 3 fichiers feature et adapter l'import du décorateur.

- [ ] **Step 1: Écrire le test de garde qui échoue**

Créer `src/test/kotlin/eu/ejdr/presentation/navigation/RoutesRegistrationTest.kt` :

```kotlin
package eu.ejdr.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Garde anti-crash Nav3 : toute sous-classe scellée de [Route] DOIT être déclarée dans
 * [allRoutes] (donc dans [appNavConfiguration]). Sans ça, `rememberNavBackStack` plante au
 * démarrage. Ce test transforme ce crash runtime en échec de build.
 */
class RoutesRegistrationTest {

    @Test
    fun `every Route subclass is registered in allRoutes`() {
        val declared = Route::class.sealedSubclasses.toSet()
        val registered = allRoutes.toSet()
        val missing = declared - registered
        assertTrue(missing.isEmpty(), "Routes non enregistrées dans appNavConfiguration : $missing")
        assertEquals(declared, registered)
    }
}
```

NOTE: `sealedSubclasses` ne voit que les sous-types **directs** de `Route` déclarés dans le même module ; comme toutes les routes implémentent directement `Route` (pas de hiérarchie intermédiaire), c'est correct ici.

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `.\gradlew.bat test --tests "eu.ejdr.presentation.navigation.RoutesRegistrationTest" --no-daemon`
Expected: échec de compilation (`allRoutes` n'existe pas encore).

- [ ] **Step 3: `Routes.kt` — déclarer `allRoutes` et boucler la config**

Remplacer `src/main/kotlin/eu/ejdr/presentation/navigation/Routes.kt` par :

```kotlin
package eu.ejdr.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import eu.ejdr.presentation.features.auth.navigation.authRoutes
import eu.ejdr.presentation.features.settings.navigation.settingsRoutes
import eu.ejdr.presentation.features.user.navigation.userRoutes
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.reflect.KClass

/**
 * Destinations de navigation (Navigation 3).
 *
 * `sealed interface` racine ; les sous-types concrets vivent **dans leur feature**
 * (`features/<feature>/navigation/`). Les arguments d'un écran voyagent dans la clé
 * (ex. un futur `Campaign(id)` en `data class @Serializable`).
 */
sealed interface Route : NavKey

/** Écran de démarrage (auto-login) — transverse, vit ici. */
@Serializable
data object SplashRoute : Route

/**
 * Toutes les routes de l'app = transverses + contributions de chaque feature.
 *
 * Ajouter une route à la liste de sa feature suffit : la boucle ci-dessous l'enregistre
 * automatiquement dans [appNavConfiguration] (plus d'oubli de `subclass()` possible — un
 * oubli est attrapé par `RoutesRegistrationTest`).
 */
val allRoutes: List<KClass<out Route>> =
    listOf(SplashRoute::class) + authRoutes + settingsRoutes + userRoutes

/**
 * Configuration de sérialisation du back-stack. `rememberNavBackStack` exige un
 * `serializersModule` déclarant le polymorphisme ouvert de [NavKey] avec chaque sous-type.
 */
val appNavConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            allRoutes.forEach { subclass(it) }
        }
    }
}
```

NOTE: `subclass(it)` où `it: KClass<out Route>` — la surcharge `PolymorphicModuleBuilder.subclass(KClass)` infère le serializer via `@Serializable`. Les routes restent donc `@Serializable` dans leurs fichiers feature.

- [ ] **Step 4: Créer les fichiers de navigation par feature**

`src/main/kotlin/eu/ejdr/presentation/features/auth/navigation/AuthNavigation.kt` :

```kotlin
package eu.ejdr.presentation.features.auth.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import eu.ejdr.presentation.features.auth.page.LoginPage
import eu.ejdr.presentation.features.auth.page.RegisterPage
import eu.ejdr.presentation.navigation.Route
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** Écran de connexion. */
@Serializable
data object LoginRoute : Route

/** Écran d'inscription. */
@Serializable
data object RegisterRoute : Route

/** Routes contribuées par la feature auth (cf. `allRoutes`). */
val authRoutes: List<KClass<out Route>> = listOf(LoginRoute::class, RegisterRoute::class)

/**
 * Enregistre les écrans auth dans l'entryProvider de l'app.
 *
 * @param backStack Back-stack possédé par l'app (navigation interne à la feature).
 * @param onAuthenticated Appelé après login/register réussi (l'app décide de la destination).
 */
fun EntryProviderBuilderScope.authEntries(
    backStack: NavBackStack<NavKey>,
    onAuthenticated: () -> Unit,
) {
    entry<LoginRoute> {
        LoginPage(
            onAuthenticated = onAuthenticated,
            onGoToRegister = { backStack.add(RegisterRoute) },
        )
    }
    entry<RegisterRoute> {
        RegisterPage(
            onAuthenticated = onAuthenticated,
            onGoToLogin = { backStack.removeLastOrNull() },
        )
    }
}
```

`src/main/kotlin/eu/ejdr/presentation/features/settings/navigation/SettingsNavigation.kt` :

```kotlin
package eu.ejdr.presentation.features.settings.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.settings.page.SettingsPage
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** Écran des paramètres. */
@Serializable
data object SettingsRoute : Route

/** Routes contribuées par la feature settings. */
val settingsRoutes: List<KClass<out Route>> = listOf(SettingsRoute::class)

/**
 * Enregistre l'écran paramètres.
 *
 * @param backStack Back-stack pour le retour.
 * @param onLogout Déconnexion (déléguée à l'app).
 * @param onThemeChange Propagation du thème au design system global.
 */
fun EntryProviderBuilderScope.settingsEntries(
    backStack: NavBackStack<NavKey>,
    onLogout: () -> Unit,
    onThemeChange: (ThemeVariant) -> Unit,
) {
    entry<SettingsRoute> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "Paramètres",
                    onLogout = onLogout,
                    onBack = { backStack.removeLastOrNull() },
                )
            },
        ) {
            SettingsPage(onThemeChange = onThemeChange)
        }
    }
}
```

`src/main/kotlin/eu/ejdr/presentation/features/user/navigation/UserNavigation.kt` :

```kotlin
package eu.ejdr.presentation.features.user.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import eu.ejdr.presentation.features.settings.navigation.SettingsRoute
import eu.ejdr.presentation.features.user.page.UserPage
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.shared.component.organism.AppScaffold
import eu.ejdr.presentation.shared.component.organism.AppTopBar
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** Zone connectée : écran d'accueil. */
@Serializable
data object HomeRoute : Route

/** Routes contribuées par la feature user/home. */
val userRoutes: List<KClass<out Route>> = listOf(HomeRoute::class)

/**
 * Enregistre l'écran d'accueil connecté.
 *
 * @param backStack Back-stack (pour ouvrir les paramètres).
 * @param onLogout Déconnexion (déléguée à l'app).
 * @param onSessionExpired Session expirée → retour connexion (décidé par l'app).
 */
fun EntryProviderBuilderScope.userEntries(
    backStack: NavBackStack<NavKey>,
    onLogout: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    entry<HomeRoute> {
        AppScaffold(
            topBar = {
                AppTopBar(
                    title = "E-JDR",
                    onLogout = onLogout,
                    onSettings = { backStack.add(SettingsRoute) },
                )
            },
        ) {
            UserPage(onSessionExpired = onSessionExpired)
        }
    }
}
```

- [ ] **Step 5: Réécrire `AppNavDisplay` (agrégation + Splash transverse)**

Remplacer `src/main/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt` par :

```kotlin
package eu.ejdr.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.auth.navigation.authEntries
import eu.ejdr.presentation.features.settings.navigation.settingsEntries
import eu.ejdr.presentation.features.user.navigation.userEntries
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Rend l'écran courant à partir du back-stack possédé par [eu.ejdr.presentation.App].
 *
 * Concentre uniquement l'**agrégation** des contributions de navigation de chaque feature
 * (`authEntries`/`settingsEntries`/`userEntries`) ; chaque feature possède ses routes et ses
 * écrans dans `features/<feature>/navigation/`. L'orchestration du démarrage et l'état du
 * thème restent dans [eu.ejdr.presentation.App].
 */
@Composable
fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    onLogout: () -> Unit,
    onThemeChange: (ThemeVariant) -> Unit,
    onAuthenticated: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(rememberEjdrViewModelStoreNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<SplashRoute> { SplashScreen() }
            authEntries(backStack, onAuthenticated)
            settingsEntries(backStack, onLogout, onThemeChange)
            userEntries(backStack, onLogout, onSessionExpired)
        },
    )
}

/** Écran de démarrage affiché pendant la restauration de session. */
@Composable
private fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(AppTheme.colors.background),
        Alignment.Center,
    ) { CircularProgressIndicator(color = AppTheme.colors.primary) }
}
```

NOTE: `rememberEjdrViewModelStoreNavEntryDecorator` est top-level dans le **même package** (`eu.ejdr.presentation.navigation`) → **aucun import nécessaire** dans `AppNavDisplay.kt`. Si l'appel `authEntries(...)` ne résout pas le récepteur, c'est que `EntryProviderBuilderScope` n'est pas le bon type — corriger dans les 3 fichiers feature (cf. encadré ⚠️).

- [ ] **Step 6: Lancer le test de garde**

Run: `.\gradlew.bat test --tests "eu.ejdr.presentation.navigation.RoutesRegistrationTest" --no-daemon`
Expected: PASS.

- [ ] **Step 7: Commit** *(App.kt référence encore l'ancienne API AppNavDisplay/Route.Splash — réparé F2)*

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/auth/navigation src/main/kotlin/eu/ejdr/presentation/features/settings/navigation src/main/kotlin/eu/ejdr/presentation/features/user/navigation src/main/kotlin/eu/ejdr/presentation/navigation src/test/kotlin/eu/ejdr/presentation/navigation
git commit -m "refactor(nav): registre d'entries+routes par feature, garde anti-crash"
```

---

# F2 — `AppViewModel` racine (état global)

## Task 10: `AppViewModel` + `SessionState`

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/AppViewModel.kt`
- Test: `src/test/kotlin/eu/ejdr/presentation/AppViewModelTest.kt`

> ⚠️ Pré-requis : ouvrir `src/main/kotlin/eu/ejdr/domain/features/auth/entities/User.kt` et noter les champs exacts du constructeur de `User` (utilisé dans le test ci-dessous comme `User(id = "u1", email = "a@b.c")` — adapter aux vrais champs).

- [ ] **Step 1: Écrire le test qui échoue**

Créer `src/test/kotlin/eu/ejdr/presentation/AppViewModelTest.kt` :

```kotlin
package eu.ejdr.presentation

import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.auth.error.AuthError
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
import kotlin.test.assertIs

class AppViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val user = User(id = "u1", email = "a@b.c")

    private fun vm(
        theme: ThemeVariant = ThemeVariant.LIGHT,
        restore: Result<User, AuthError> = Result.Success(user),
        setResult: Result<Unit, SettingsError> = Result.Success(Unit),
    ) = AppViewModel(
        getTheme = GetThemeUseCase { Result.Success(theme) },
        setTheme = SetThemeUseCase { setResult },
        restoreSession = RestoreSessionUseCase { restore },
        logout = LogoutUseCase { Result.Success(Unit) },
    )

    @Test
    fun `loads persisted theme on init`() = runTest {
        val viewModel = vm(theme = ThemeVariant.DARK)
        advanceUntilIdle()
        assertEquals(ThemeVariant.DARK, viewModel.theme.value)
    }

    @Test
    fun `bootstrap success yields Authenticated`() = runTest {
        val viewModel = vm(restore = Result.Success(user))
        viewModel.bootstrap()
        advanceUntilIdle()
        val s = viewModel.session.value
        assertIs<SessionState.Authenticated>(s)
        assertEquals(user, s.user)
    }

    @Test
    fun `bootstrap failure yields Anonymous`() = runTest {
        val viewModel = vm(restore = Result.Failure(AuthError.NoPersistedSession))
        viewModel.bootstrap()
        advanceUntilIdle()
        assertIs<SessionState.Anonymous>(viewModel.session.value)
    }

    @Test
    fun `onThemeChange updates the flow`() = runTest {
        val viewModel = vm()
        advanceUntilIdle()
        viewModel.onThemeChange(ThemeVariant.DARK)
        advanceUntilIdle()
        assertEquals(ThemeVariant.DARK, viewModel.theme.value)
    }

    @Test
    fun `onLogout yields Anonymous`() = runTest {
        val viewModel = vm(restore = Result.Success(user))
        viewModel.bootstrap(); advanceUntilIdle()
        viewModel.onLogout(); advanceUntilIdle()
        assertIs<SessionState.Anonymous>(viewModel.session.value)
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `.\gradlew.bat test --tests "eu.ejdr.presentation.AppViewModelTest" --no-daemon`
Expected: échec de compilation (`AppViewModel`/`SessionState` absents).

- [ ] **Step 3: Implémenter `AppViewModel`**

Créer `src/main/kotlin/eu/ejdr/presentation/AppViewModel.kt` :

```kotlin
package eu.ejdr.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.auth.entities.User
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** État de session global de l'application. */
sealed interface SessionState {
    /** Restauration en cours (écran Splash). */
    data object Loading : SessionState

    /** Utilisateur connecté. */
    data class Authenticated(val user: User) : SessionState

    /** Aucun utilisateur connecté. */
    data object Anonymous : SessionState
}

/**
 * ViewModel racine : détient l'état **global** (thème + session) au lieu d'un `mutableStateOf`
 * ad-hoc dans `App`. Créé hors back-stack (au niveau App), deps par constructeur (compatible
 * avec le décorateur ViewModel maison sans SavedStateHandle).
 *
 * @property getTheme Lecture du thème persisté.
 * @property setTheme Persistance du thème.
 * @property restoreSession Auto-login silencieux au démarrage.
 * @property logout Déconnexion.
 */
class AppViewModel(
    private val getTheme: GetThemeUseCase,
    private val setTheme: SetThemeUseCase,
    private val restoreSession: RestoreSessionUseCase,
    private val logout: LogoutUseCase,
) : ViewModel() {

    private val _theme = MutableStateFlow(ThemeVariant.LIGHT)
    val theme: StateFlow<ThemeVariant> = _theme.asStateFlow()

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    init {
        viewModelScope.launch {
            getTheme().fold(onSuccess = { _theme.value = it }, onFailure = { })
        }
    }

    /** Démarrage : tente l'auto-login puis bascule la session en Authenticated/Anonymous. */
    fun bootstrap() {
        viewModelScope.launch {
            _session.value = restoreSession().fold(
                onSuccess = { SessionState.Authenticated(it) },
                onFailure = { SessionState.Anonymous },
            )
        }
    }

    /** Applique et persiste le nouveau thème (mise à jour optimiste de l'état). */
    fun onThemeChange(theme: ThemeVariant) {
        _theme.value = theme
        viewModelScope.launch { setTheme(theme) }
    }

    /** Déconnecte et repasse en Anonymous. */
    fun onLogout() {
        viewModelScope.launch {
            logout()
            _session.value = SessionState.Anonymous
        }
    }
}
```

- [ ] **Step 4: Lancer le test pour vérifier le succès**

Run: `.\gradlew.bat test --tests "eu.ejdr.presentation.AppViewModelTest" --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/AppViewModel.kt src/test/kotlin/eu/ejdr/presentation/AppViewModelTest.kt
git commit -m "feat(app): AppViewModel racine (theme + session globaux)"
```

---

## Task 11: Réécrire `App.kt` (consomme AppViewModel + nouvelle nav + UpdateViewModel)

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/App.kt`

- [ ] **Step 1: Réécrire `App.kt`**

Remplacer `src/main/kotlin/eu/ejdr/presentation/App.kt` par :

```kotlin
package eu.ejdr.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.SetThemeUseCase
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.abstraction.usecase.DownloadAndInstallUpdateUseCase
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.auth.navigation.LoginRoute
import eu.ejdr.presentation.features.update.UpdateViewModel
import eu.ejdr.presentation.features.user.navigation.HomeRoute
import eu.ejdr.presentation.navigation.AppNavDisplay
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.navigation.SplashRoute
import eu.ejdr.presentation.navigation.appNavConfiguration
import eu.ejdr.presentation.shared.component.organism.UpdateDialog
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.darkColors
import eu.ejdr.presentation.shared.theme.lightColors
import java.awt.Desktop
import java.net.URI
import org.koin.compose.koinInject

/**
 * Composable racine.
 *
 * L'état global (thème + session) vit dans un [AppViewModel] (plus de `mutableStateOf`
 * ad-hoc). App fournit le design system, possède le back-stack et délègue le mapping
 * route→écran à [AppNavDisplay]. La session pilote la destination initiale.
 */
@Composable
fun App() {
    // AppViewModel est créé HORS back-stack : le décorateur ViewModel maison ne fournit un
    // ViewModelStoreOwner QUE par destination de navigation. On l'instancie donc via remember
    // (deps résolues par koinInject) au lieu de koinViewModel{}, pour éviter un crash
    // « No ViewModelStoreOwner » au niveau racine.
    val getTheme = koinInject<GetThemeUseCase>()
    val setTheme = koinInject<SetThemeUseCase>()
    val restoreSession = koinInject<RestoreSessionUseCase>()
    val logout = koinInject<LogoutUseCase>()
    val appViewModel = remember { AppViewModel(getTheme, setTheme, restoreSession, logout) }
    val theme by appViewModel.theme.collectAsStateWithLifecycle()
    val session by appViewModel.session.collectAsStateWithLifecycle()

    AppTheme(
        colors = when (theme) {
            ThemeVariant.LIGHT -> lightColors()
            ThemeVariant.DARK -> darkColors()
        },
    ) {
        val checkUpdate = koinInject<CheckUpdateUseCase>()
        val backStack = rememberNavBackStack(appNavConfiguration, SplashRoute)

        fun resetTo(route: Route) {
            backStack.clear()
            backStack.add(route)
        }

        // Démarrage : auto-login (via le VM) ; la session pilote l'écran.
        LaunchedEffect(Unit) { appViewModel.bootstrap() }
        LaunchedEffect(session) {
            when (session) {
                is SessionState.Authenticated -> resetTo(HomeRoute)
                is SessionState.Anonymous -> resetTo(LoginRoute)
                is SessionState.Loading -> Unit
            }
        }

        var updateInfo by remember { mutableStateOf<UpdateInfoDto?>(null) }
        LaunchedEffect(Unit) { updateInfo = checkUpdate() }

        AppNavDisplay(
            backStack = backStack,
            onLogout = { appViewModel.onLogout() },
            onThemeChange = appViewModel::onThemeChange,
            onAuthenticated = { resetTo(HomeRoute) },
            onSessionExpired = { resetTo(LoginRoute) },
        )

        updateInfo?.let { info ->
            // Idem AppViewModel : créé hors back-stack → remember plutôt que koinViewModel{}.
            val downloadAndInstall = koinInject<DownloadAndInstallUpdateUseCase>()
            val updateVm = remember(info) {
                UpdateViewModel(info) { url, onProgress -> downloadAndInstall(url, onProgress) }
            }
            val updateState by updateVm.state.collectAsStateWithLifecycle()
            UpdateDialog(
                info = info,
                state = updateState,
                onInstall = updateVm::onInstall,
                onRetry = updateVm::onRetry,
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

NOTE Nav3 desktop : `AppViewModel` et `UpdateViewModel` sont créés via `remember { … }` (deps par `koinInject`), PAS via `koinViewModel { }`, car ils vivent hors d'une destination de navigation — le décorateur ViewModel maison (`rememberEjdrViewModelStoreNavEntryDecorator`) ne fournit un `LocalViewModelStoreOwner` que pour les entries du back-stack. Conséquence assumée : ces deux VMs ne reçoivent pas d'`onCleared` automatique (leur `viewModelScope` vit le temps de la composition racine / du dialog), ce qui est sans effet ici (pas de ressource à libérer). Les VMs **par écran** (Auth/User/Settings) continuent eux d'utiliser `koinViewModel { }` dans leurs pages.

- [ ] **Step 2: `verify` complet — la boucle F4→F2 se referme**

Run: `.\gradlew.bat verify --no-daemon`
Expected: BUILD SUCCESSFUL (detekt + tous tests + Kover). Corriger tout import résiduel signalé.

- [ ] **Step 3: Lancer l'app et confirmer le runtime Nav3**

Run: `.\gradlew.bat run --no-daemon`
Expected: la fenêtre s'ouvre, Splash → Login (pas de session) ; navigation Login↔Register OK. **Si crash de sérialisation** : vérifier que chaque `*Route` est dans `allRoutes` (le test de garde doit déjà l'assurer). **Si crash ViewModelStoreOwner** : appliquer la variante `remember { … }` de la NOTE Step 1. Fermer la fenêtre.

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/App.kt
git commit -m "refactor(app): App consomme AppViewModel + navigation par feature + UpdateViewModel"
```

---

# F5 — DI par feature

## Task 12: Découper les modules Koin par feature

**Files:**
- Create: `src/main/kotlin/eu/ejdr/di/CoreModule.kt`, `AuthModule.kt`, `SettingsModule.kt`, `UpdateModule.kt`
- Delete: `src/main/kotlin/eu/ejdr/di/InfrastructureModule.kt`, `ApplicationModule.kt`
- Modify: `src/main/kotlin/eu/ejdr/di/AppKoin.kt`
- Keep: `RealtimeModule.kt` (inchangé)

> ⚠️ Avant d'écrire : relire `InfrastructureModule.kt` et `ApplicationModule.kt` (versions actuelles) pour **recopier à l'identique** chaque ligne `single<...> { ...(get(), ...) }` (nombre exact de `get()`). Les blocs ci-dessous reflètent l'état lu au moment de l'audit ; vérifier qu'aucune signature n'a changé.

- [ ] **Step 1: Créer `CoreModule` (transverse partagé)**

`src/main/kotlin/eu/ejdr/di/CoreModule.kt` :

```kotlin
package eu.ejdr.di

import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.dsl.module

/**
 * Module Koin transverse : briques techniques partagées par plusieurs features
 * (config, sécurité/coffre, client HTTP avec cookies sécurisés). Aucune logique de feature.
 */
val coreModule = module {
    single { AppConfig.load() }
    single { KeyStoreProvider(get<AppConfig>().dataDir) }
    single { CookieCipher(get()) }
    single { SecureCookiesStorage(get<AppConfig>().dataDir, get(), AcceptAllCookiesStorage()) }
    single<HttpClient> { KtorClientFactory(get(), get<SecureCookiesStorage>()).create() }
}
```

- [ ] **Step 2: Créer `AuthModule`**

`src/main/kotlin/eu/ejdr/di/AuthModule.kt` :

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
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import org.koin.dsl.module

/** Module Koin de la feature auth (infra HTTP + persistance session + use cases + service). */
val authModule = module {
    single<SessionPersistence> { get<SecureCookiesStorage>() }
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

- [ ] **Step 3: Créer `SettingsModule` et `UpdateModule`**

`src/main/kotlin/eu/ejdr/di/SettingsModule.kt` :

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

/** Module Koin de la feature settings (persistance fichier + use cases thème). */
val settingsModule = module {
    single<ThemeRepository> { ThemeFileRepository(get<AppConfig>().dataDir) }
    single<GetThemeUseCase> { GetThemeUseCaseImpl(get()) }
    single<SetThemeUseCase> { SetThemeUseCaseImpl(get()) }
}
```

`src/main/kotlin/eu/ejdr/di/UpdateModule.kt` :

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

/** Module Koin de la feature update (release GitHub + launcher OS + use cases). */
val updateModule = module {
    single<UpdateRepository> { UpdateHttpRepository(get()) }
    single<SystemLauncherService> { WindowsSystemLauncher() }
    single<CheckUpdateUseCase> { CheckUpdateUseCaseImpl(get()) }
    single<DownloadAndInstallUpdateUseCase> { DownloadAndInstallUpdateUseCaseImpl(get(), get()) }
}
```

NOTE: `CheckUpdateUseCaseImpl(get())` reprend la ligne exacte de l'ancien `ApplicationModule` (un seul `get()`). Vérifier contre l'original.

- [ ] **Step 4: Mettre à jour `AppKoin` et supprimer les anciens modules**

`AppKoin.kt` :

```kotlin
package eu.ejdr.di

import org.koin.core.context.startKoin

/**
 * Composition root : démarre Koin avec les modules **par feature** (+ le module transverse
 * [coreModule]). Ajouter une feature = ajouter son module ici.
 */
fun initKoin() = startKoin {
    modules(coreModule, authModule, settingsModule, updateModule, realtimeModule)
}
```

Supprimer les anciens modules :

```bash
git rm src/main/kotlin/eu/ejdr/di/InfrastructureModule.kt src/main/kotlin/eu/ejdr/di/ApplicationModule.kt
```

- [ ] **Step 5: `verify` + `run`**

Run: `.\gradlew.bat verify --no-daemon`
Expected: BUILD SUCCESSFUL.
Run: `.\gradlew.bat run --no-daemon`
Expected: la fenêtre s'ouvre (Koin résout tout le graphe). **Si « NoBeanDefFoundException »** : une dépendance manque dans un module — vérifier que `HttpClient`/`AppConfig`/`SecureCookiesStorage` (dans `coreModule`) sont bien chargés avant les modules feature (ils le sont via l'ordre dans `AppKoin`, mais Koin résout paresseusement donc l'ordre n'importe pas — c'est la présence qui compte). Fermer la fenêtre.

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/eu/ejdr/di
git commit -m "refactor(di): decoupe les modules Koin par feature (+ coreModule transverse)"
```

---

# F7 — Kover : inclure les ViewModels, exclure l'UI

## Task 13: Affiner l'exclusion Kover

**Files:**
- Modify: `build.gradle.kts` (bloc `kover { ... }`)

- [ ] **Step 1: Remplacer l'exclusion par paquet large par une exclusion UI ciblée**

Dans `build.gradle.kts`, bloc `kover { reports { filters { excludes { ... } } } }`, remplacer le contenu de `excludes { ... }` actuel :

```kotlin
packages(
    "eu.ejdr.presentation",
    "eu.ejdr.di",
)
classes(
    "eu.ejdr.MainKt",
)
```

par :

```kotlin
// Exclus de la couverture : DI (câblage), point d'entrée, et l'UI Compose pure
// (navigation/composants partagés/thème + pages & composants par feature — testée
// manuellement). Les ViewModels (eu.ejdr.presentation.features.*.*ViewModel et
// eu.ejdr.presentation.AppViewModel) NE sont PAS exclus : contrôleurs testés (cf. *ViewModelTest).
packages(
    "eu.ejdr.di",
    "eu.ejdr.presentation.navigation",
    "eu.ejdr.presentation.shared",
)
classes(
    "eu.ejdr.MainKt",
    "eu.ejdr.presentation.features.*.page.*",
    "eu.ejdr.presentation.features.*.component.*",
    "eu.ejdr.presentation.features.*.navigation.*",
)
```

NOTE wildcards Kover (confirmé doc) : `classes(...)` accepte `*`/`?` sur le **nom JVM complet** ; `packages(...)` exclut le paquet **et ses sous-paquets**. Les ViewModels vivent dans `eu.ejdr.presentation.features.<f>` (classe `<F>ViewModel`, hors sous-paquet `page`/`component`/`navigation`) et `eu.ejdr.presentation.AppViewModel` → non exclus, donc comptés. Le composable racine `App.kt` (classe `AppKt`) n'est pas exclu : s'il fait casser le plancher à lui seul, ajouter `"eu.ejdr.presentation.AppKt"` à `classes(...)`.

- [ ] **Step 2: Lancer la vérif de couverture**

Run: `.\gradlew.bat koverVerify --no-daemon`
Expected: PASS (plancher 60 %). Les VMs étant déjà testés, le plancher tient. **Si échec** : lire `build/reports/kover/verify.err` ; ajouter les tests VM manquants (NE PAS baisser le plancher). Si l'échec vient uniquement du composable `App.kt`, l'ajouter à `classes(...)` (Step 1).

- [ ] **Step 3: `verify` complet**

Run: `.\gradlew.bat verify --no-daemon`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Mettre à jour la doc d'audit (F7 traité)**

Dans `docs/AUDIT_ARCHITECTURE.md`, section F7 : ajouter une note « ✅ Corrigé : exclusion Kover affinée — ViewModels (dont AppViewModel) désormais comptés dans les 60 %, seule l'UI Compose est exclue. »

- [ ] **Step 5: Commit**

```bash
git add build.gradle.kts docs/AUDIT_ARCHITECTURE.md
git commit -m "test(kover): inclut les ViewModels dans la couverture, exclut l'UI Compose"
```

---

# F3 — Release conditionnée à une CI verte

## Task 14: `release.yml` dépend d'un job `verify`

**Files:**
- Modify: `.github/workflows/release.yml`

- [ ] **Step 1: Ajouter un job `verify` et le mettre en `needs` de `semantic_release`**

Dans `.github/workflows/release.yml`, ajouter ce job en tête de `jobs:` (avant `semantic_release`) :

```yaml
  verify:
    name: Verify (detekt + tests + coverage)
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

      - name: Verify
        run: ./gradlew verify --no-daemon --stacktrace
```

Puis ajouter `needs: verify` au job `semantic_release` (juste sous `runs-on: ubuntu-latest`) :

```yaml
  semantic_release:
    name: semantic-release
    runs-on: ubuntu-latest
    needs: verify
    outputs:
      released: ${{ steps.release_check.outputs.released }}
      tag: ${{ steps.release_check.outputs.tag }}
    steps:
      # ... (reste inchangé)
```

*(`windows_build` garde `needs: semantic_release` → il hérite indirectement de `verify`.)*

- [ ] **Step 2: Valider la syntaxe YAML**

Run: `npx --yes @action-validator/cli .github/workflows/release.yml`
Expected: aucune erreur de schéma. (Hors-ligne : relire que `verify` est bien indenté sous `jobs:` au même niveau que les autres jobs et que `needs: verify` est présent.)

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "ci: conditionne la release a une CI verte (job verify en amont)"
```

---

# Clôture

## Task 15: Vérification finale + mise à jour de l'audit

- [ ] **Step 1: `verify` + `run` de bout en bout**

Run: `.\gradlew.bat verify --no-daemon` → BUILD SUCCESSFUL.
Run: `.\gradlew.bat run --no-daemon` → la fenêtre s'ouvre ; Splash → Login/Home ; navigation vers Paramètres OK ; changement de thème appliqué ET persisté (relancer l'app : le thème est conservé). Fermer la fenêtre.

- [ ] **Step 2: Marquer F1–F7 comme traités dans l'audit**

Dans `docs/AUDIT_ARCHITECTURE.md`, marquer F1–F7 « ✅ Corrigé » (renvoi à ce plan). Laisser F8–F12 en dette.

- [ ] **Step 3: Commit final**

```bash
git add docs/AUDIT_ARCHITECTURE.md
git commit -m "docs: marque F1-F7 corriges dans l'audit d'architecture"
```

---

## Récapitulatif des tâches

| Task | Faiblesse | Résumé |
|------|-----------|--------|
| 1 | F4.a | `Result` : map/flatMap/mapError/getOrElse/getOrNull/onSuccess/onFailure |
| 2 | F4.c | `UpdateError` (erreur métier update) |
| 3 | F4.c | `UpdateRepository.downloadUpdate` → Result + timeout |
| 4 | F4.c | `DownloadAndInstallUpdateUseCase` → Result |
| 5 | F4.b | Settings get/set suspend + Result ; `SettingsViewModel` async |
| 6 | F4.d | `hasPersistedSession` suspend |
| 7 | F6 | `UpdateViewModel` + `UpdateUiState` |
| 8 | F6 | `UpdateDialog` bête |
| 9 | F1 | Registre nav par feature + garde anti-crash |
| 10 | F2 | `AppViewModel` + `SessionState` |
| 11 | F2 | `App.kt` réécrit |
| 12 | F5 | Modules Koin par feature |
| 13 | F7 | Exclusion Kover affinée (VMs comptés) |
| 14 | F3 | Release gated par `verify` |
| 15 | — | Vérif finale + audit à jour |
