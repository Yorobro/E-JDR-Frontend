# Câblage GET /me (profil courant + exercice de l'intercepteur 401) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Appeler la nouvelle route protégée backend `GET /me` depuis la page Home, ce qui donne enfin un cas réel à l'intercepteur 401 (refresh silencieux) déjà câblé dans `KtorClientFactory`.

**Architecture:** Clean architecture existante — port `AuthRepository.me()` + impl HTTP, `GetCurrentUserUseCase` (fun interface), `UserPage` devient un composant intelligent qui charge le profil au montage. Aucune modification de l'intercepteur 401 : `/me` n'est pas une route `/auth/`, le refresh silencieux s'applique automatiquement.

**Tech Stack:** Kotlin 2.2, Compose for Desktop, Ktor Client (CIO + MockEngine en test), Koin, JUnit5 + MockK.

**Spec :** `E-JDR-Backend/docs/superpowers/specs/2026-06-10-protected-route-me-and-dao-testcontainers-design.md` (volet 3)

**Pré-requis :** le backend expose `GET /me` (plan backend `2026-06-10-protected-route-me-and-dao-testcontainers.md`). Les tâches 1–2 sont réalisables sans backend (MockEngine) ; seule la vérification manuelle finale en a besoin.

**Conventions du repo à respecter partout :**
- KDoc en français sur chaque classe/fonction publique.
- `Result<T, E : DomainError>` : aucune exception ne traverse les couches.
- Use case = `fun interface` invocable, impl suffixée `Impl`, orchestration pure.
- Vérification locale : `.\gradlew verify` (detekt + build + tests + koverVerify).
- Commits : Conventional Commits, validés par commitlint.

---

### Task 1: Port `AuthRepository.me()` + implémentation HTTP

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/application/auth/abstraction/repository/AuthRepository.kt`
- Modify: `src/main/kotlin/eu/ejdr/infrastructure/http/auth/AuthHttpRepository.kt`
- Test: `src/test/kotlin/eu/ejdr/infrastructure/http/auth/AuthHttpRepositoryTest.kt`

- [ ] **Step 1: Écrire les tests qui échouent**

Dans `src/test/kotlin/eu/ejdr/infrastructure/http/auth/AuthHttpRepositoryTest.kt`, ajouter ces tests à la fin de la classe (les helpers `repository(...)`, `clientReturning(...)` existent déjà) :

```kotlin
    @Test
    fun `me success maps body to User`() =
        runTest {
            val repo =
                repository(
                    clientReturning(
                        HttpStatusCode.OK,
                        """{"userId":"u-1","email":"user@test.com","createdAt":"2026-06-10T08:00:00.000Z"}""",
                    ),
                )

            val result = repo.me()

            assertIs<Result.Success<*>>(result)
            assertEquals("user@test.com", (result.value as eu.ejdr.domain.entities.auth.User).email)
        }

    @Test
    fun `me 401 maps to SessionExpired`() =
        runTest {
            val repo =
                repository(
                    clientReturning(
                        HttpStatusCode.Unauthorized,
                        """{"code":"UNAUTHENTICATED","message":"Authentification requise."}""",
                    ),
                )

            val result = repo.me()

            assertIs<Result.Failure<*>>(result)
            assertEquals(AuthError.SessionExpired, result.error)
        }

    @Test
    fun `me network failure maps to Network`() =
        runTest {
            val engine = MockEngine { throw java.io.IOException("connexion refusée") }
            val client =
                HttpClient(engine) {
                    install(HttpCookies) { storage = cookiesStorage }
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            },
                        )
                    }
                }

            val result = repository(client).me()

            assertIs<Result.Failure<*>>(result)
            assertEquals(AuthError.Network, result.error)
        }
```

Note : le test `me success` inclut volontairement `createdAt` dans le JSON — champ absent du DTO, ignoré grâce à `ignoreUnknownKeys = true` (même config que `KtorClientFactory`). C'est le contrat réel du backend.

- [ ] **Step 2: Vérifier que la compilation échoue**

Run: `.\gradlew test --tests "eu.ejdr.infrastructure.http.auth.AuthHttpRepositoryTest"`
Expected: FAIL — `unresolved reference: me`

- [ ] **Step 3: Ajouter `me()` au port**

Dans `src/main/kotlin/eu/ejdr/application/auth/abstraction/repository/AuthRepository.kt`, ajouter après `refresh()` :

```kotlin
    /**
     * Récupère le profil de l'utilisateur courant auprès du serveur (`GET /me`).
     *
     * Première route protégée : un 401 éventuel est d'abord traité par l'intercepteur
     * de rafraîchissement silencieux du client HTTP ; s'il parvient jusqu'ici, la
     * session est réellement expirée.
     *
     * @return l'[User] courant, ou une [AuthError] ([AuthError.SessionExpired] si la
     * session n'est plus valide)
     */
    suspend fun me(): Result<User, AuthError>
```

- [ ] **Step 4: Implémenter dans `AuthHttpRepository`**

Dans `src/main/kotlin/eu/ejdr/infrastructure/http/auth/AuthHttpRepository.kt` :

1. Ajouter l'import :

```kotlin
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
```

2. Ajouter la méthode après `refresh()` :

```kotlin
    /**
     * Récupère le profil courant via `GET /me` (route protégée).
     *
     * Si un 401 arrive ici, c'est que l'intercepteur de refresh silencieux a déjà
     * échoué (il a alors effacé la session persistée) : on traduit en
     * [AuthError.SessionExpired] pour que la présentation ramène à la connexion.
     *
     * @return [Result.Success] avec l'[User] courant, sinon une [AuthError].
     */
    override suspend fun me(): Result<User, AuthError> =
        runCatching {
            val response = client.get("${config.baseUrl}/me")
            when {
                response.status.isSuccess() ->
                    Result.Success(mapper.toUser(response.body<AuthResponseDto>()))

                response.status == HttpStatusCode.Unauthorized ->
                    Result.Failure(AuthError.SessionExpired)

                else -> {
                    val err = runCatching { response.body<ApiErrorDto>() }.getOrNull()
                    Result.Failure(mapper.toAuthError(response.status, err?.code, err?.message))
                }
            }
        }.getOrElse { Result.Failure(AuthError.Network) }
```

Note : la réponse backend contient `createdAt`, absent d'`AuthResponseDto` — ignoré par `ignoreUnknownKeys`. Pas de nouveau DTO (YAGNI).

- [ ] **Step 5: Vérifier que les tests passent**

Run: `.\gradlew test --tests "eu.ejdr.infrastructure.http.auth.AuthHttpRepositoryTest"`
Expected: PASS (tests existants + 3 nouveaux)

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/eu/ejdr/application/auth/abstraction/repository/AuthRepository.kt src/main/kotlin/eu/ejdr/infrastructure/http/auth/AuthHttpRepository.kt src/test/kotlin/eu/ejdr/infrastructure/http/auth/AuthHttpRepositoryTest.kt
git commit -m "feat(auth): add me() to AuthRepository for protected GET /me"
```

---

### Task 2: `GetCurrentUserUseCase` + enregistrement Koin

**Files:**
- Create: `src/main/kotlin/eu/ejdr/application/auth/abstraction/usecase/GetCurrentUserUseCase.kt`
- Create: `src/main/kotlin/eu/ejdr/application/auth/usecase/GetCurrentUserUseCaseImpl.kt`
- Modify: `src/main/kotlin/eu/ejdr/di/ApplicationModule.kt`
- Test: `src/test/kotlin/eu/ejdr/application/auth/usecase/GetCurrentUserUseCaseImplTest.kt`

- [ ] **Step 1: Écrire le test qui échoue**

Créer `src/test/kotlin/eu/ejdr/application/auth/usecase/GetCurrentUserUseCaseImplTest.kt` :

```kotlin
package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetCurrentUserUseCaseImplTest {

    private val repository = mockk<AuthRepository>()
    private val useCase = GetCurrentUserUseCaseImpl(repository)

    @Test
    fun `returns user on success`() = runTest {
        coEvery { repository.me() } returns Result.Success(User("1", "a@b.c"))

        val result = useCase()

        assertIs<Result.Success<User>>(result)
        assertEquals("a@b.c", result.value.email)
    }

    @Test
    fun `propagates SessionExpired failure`() = runTest {
        coEvery { repository.me() } returns Result.Failure(AuthError.SessionExpired)

        val result = useCase()

        assertIs<Result.Failure<AuthError>>(result)
        assertEquals(AuthError.SessionExpired, result.error)
    }
}
```

- [ ] **Step 2: Vérifier que la compilation échoue**

Run: `.\gradlew test --tests "eu.ejdr.application.auth.usecase.GetCurrentUserUseCaseImplTest"`
Expected: FAIL — `unresolved reference: GetCurrentUserUseCaseImpl`

- [ ] **Step 3: Créer l'interface du use case**

`src/main/kotlin/eu/ejdr/application/auth/abstraction/usecase/GetCurrentUserUseCase.kt` :

```kotlin
package eu.ejdr.application.auth.abstraction.usecase

import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Use case de consultation du profil de l'utilisateur courant (`GET /me`).
 *
 * S'invoque comme une fonction : `getCurrentUserUseCase()`.
 *
 * @return l'[User] courant, ou une [AuthError] ([AuthError.SessionExpired] si la
 * session n'est plus valide côté serveur)
 */
fun interface GetCurrentUserUseCase {
    suspend operator fun invoke(): Result<User, AuthError>
}
```

- [ ] **Step 4: Créer l'implémentation**

`src/main/kotlin/eu/ejdr/application/auth/usecase/GetCurrentUserUseCaseImpl.kt` :

```kotlin
package eu.ejdr.application.auth.usecase

import eu.ejdr.application.auth.abstraction.repository.AuthRepository
import eu.ejdr.application.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError

/**
 * Implémentation de [GetCurrentUserUseCase].
 *
 * Orchestration pure : délègue au [AuthRepository] et renvoie son résultat tel quel.
 * Un use case ne contient pas de logique réutilisable et n'appelle jamais un autre
 * use case.
 */
class GetCurrentUserUseCaseImpl(
    private val authRepository: AuthRepository,
) : GetCurrentUserUseCase {
    override suspend fun invoke(): Result<User, AuthError> = authRepository.me()
}
```

- [ ] **Step 5: Enregistrer dans Koin**

Dans `src/main/kotlin/eu/ejdr/di/ApplicationModule.kt` :

1. Ajouter les imports :

```kotlin
import eu.ejdr.application.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.auth.usecase.GetCurrentUserUseCaseImpl
```

2. Ajouter dans le bloc `module { }`, après la ligne `single<LogoutUseCase> { ... }` :

```kotlin
    single<GetCurrentUserUseCase> { GetCurrentUserUseCaseImpl(get()) }
```

- [ ] **Step 6: Vérifier que les tests passent**

Run: `.\gradlew test --tests "eu.ejdr.application.auth.usecase.GetCurrentUserUseCaseImplTest"`
Expected: PASS (2 tests)

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/eu/ejdr/application/auth src/main/kotlin/eu/ejdr/di/ApplicationModule.kt src/test/kotlin/eu/ejdr/application/auth/usecase/GetCurrentUserUseCaseImplTest.kt
git commit -m "feat(auth): add GetCurrentUserUseCase wired through Koin"
```

---

### Task 3: `UserPage` intelligente + câblage `App.kt`

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/feature/user/page/UserPage.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/App.kt`

Rappel : la présentation est exclue de Kover (UI Compose testée manuellement) — pas de test automatisé ici, la vérification est `verify` + test manuel en Task 4.

- [ ] **Step 1: Rendre `UserPage` intelligente**

Remplacer intégralement `src/main/kotlin/eu/ejdr/presentation/feature/user/page/UserPage.kt` par :

```kotlin
package eu.ejdr.presentation.feature.user.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import eu.ejdr.application.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.common.Result
import eu.ejdr.domain.entities.auth.User
import eu.ejdr.domain.error.entities.auth.AuthError
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Page d'accueil affichée une fois l'utilisateur connecté (composant intelligent).
 *
 * Au montage, rafraîchit le profil via [GetCurrentUserUseCase] (`GET /me`) — première
 * route protégée : si l'access token a expiré, l'intercepteur du client HTTP tente un
 * refresh silencieux de façon transparente. Une [AuthError.SessionExpired] résiduelle
 * signifie que la session n'est plus restaurable : [onSessionExpired] est invoqué.
 * Sur une erreur réseau, le profil déjà connu (fourni par [user]) reste affiché.
 *
 * @param user Profil déjà connu (issu du login ou de l'auto-login), ou `null`.
 * @param onSessionExpired Callback de retour à l'écran de connexion.
 * @param modifier Modifier Compose appliqué à la page.
 */
@Composable
fun UserPage(
    user: User?,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val getCurrentUser = koinInject<GetCurrentUserUseCase>()
    var profile by remember { mutableStateOf(user) }

    LaunchedEffect(Unit) {
        when (val result = getCurrentUser()) {
            is Result.Success -> profile = result.value
            is Result.Failure ->
                if (result.error == AuthError.SessionExpired) {
                    onSessionExpired()
                }
            // Autres erreurs (réseau...) : on conserve le profil déjà connu.
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(AppTheme.dimens.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm, Alignment.CenterVertically),
    ) {
        AppText(text = "Bienvenue sur E-JDR", style = AppTextStyle.Title)
        profile?.let { current ->
            AppText(
                text = "Connecté en tant que ${current.email}",
                style = AppTextStyle.Body,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}
```

- [ ] **Step 2: Câbler le callback dans `App.kt`**

Dans `src/main/kotlin/eu/ejdr/presentation/App.kt`, branche `is Screen.Home`, remplacer :

```kotlin
                UserPage(user = current.user)
```

par :

```kotlin
                UserPage(
                    user = current.user,
                    onSessionExpired = { screen = Screen.Login },
                )
```

- [ ] **Step 3: Vérifier la compilation et la qualité**

Run: `.\gradlew verify`
Expected: PASS (detekt + build + tests + koverVerify — la présentation est hors couverture)

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation
git commit -m "feat(home): load current user profile from protected GET /me"
```

---

### Task 4: Vérification de bout en bout

- [ ] **Step 1: Vérification complète locale**

```bash
.\gradlew verify
```

Expected: PASS.

- [ ] **Step 2: Test manuel contre le backend local**

Pré-requis : backend lancé (`npm run serve` dans E-JDR-Backend, MySQL local démarré).

```powershell
$env:EJDR_API_URL = "http://localhost:3000"
.\gradlew run
```

Scénario à dérouler :
1. S'inscrire ou se connecter → arriver sur Home.
2. Vérifier l'affichage « Connecté en tant que \<email\> » (profil venu de `GET /me`).
3. **Exercice réel de l'intercepteur 401** : côté backend, mettre `JWT_ACCESS_EXPIRES_IN=5s` dans `.env`, relancer le backend, se reconnecter dans l'app, attendre ~10 s, puis naviguer Settings → retour Home (redéclenche `GET /me`). Attendu : le profil s'affiche toujours (401 → refresh silencieux → rejeu transparent). Avec `EJDR_HTTP_LOG=true`, la séquence `401 → POST /auth/refresh → 200` est visible dans les logs.
4. Remettre `JWT_ACCESS_EXPIRES_IN=15m` dans le `.env` backend après le test.
