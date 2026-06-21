# KMP Multiplatform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Journal d'exécution (2026-06-21) :** Étape 2 terminée. Tasks 1–11 faites, `verifyDesktop` vert après chacune.
> **Déviation Task 10 :** le plan demandait de remonter *tous* les modules + `AppKoin` en commonMain, mais `SettingsModule`/`UpdateModule`/`FriendGroupModule` et `AppKoin` référencent des symboles desktop-only (`ThemeFileRepository`, `WindowsSystemLauncher`, `ActiveGroupFileRepository`, `ActiveGroupState`, `java.io.File`). Résolu en bifurquant : les 8 modules feature remontent en commonMain en ne gardant que les bindings communs (interfaces + use cases + repos HTTP) ; les 4 bindings platform-specific (`ThemeRepository`, `SystemLauncherService`, `ActiveGroupRepository`, `ActiveGroupState`) sont déplacés dans `InfrastructureModuleDesktop` ; `AppKoin` reste en desktopMain comme composition root. `UpdateController`, `UpdateDialog` et `ActiveGroupState` restent en desktopMain (hors scope Étape 2, notés "should be commonMain" pour plus tard).

**Goal:** Migrer le frontend E-JDR de `kotlin("jvm")` pur vers Kotlin Multiplatform (Desktop + Android) en 3 étapes séquentielles sans jamais casser le desktop.

**Architecture:** Mono-module KMP avec sourcesets `commonMain` / `desktopMain` / `androidMain`. Le code existant migre d'abord intégralement dans `desktopMain`, puis remonte progressivement en `commonMain`. Les 6 interfaces platform-specific sont bifurquées via Koin (pas d'`expect/actual`, les interfaces existent déjà). L'UI Android (pages + navigation) est nouvelle mais s'appuie sur les ViewModels et composants atomiques partagés.

**Tech Stack:** Kotlin 2.2.20, Compose Multiplatform 1.8.x, Navigation3 1.1.1, Koin 4.1.1, Ktor 3.4.2, AGP 8.x, minSdk 26, compileSdk 35, Jetpack Security Crypto pour EncryptedSharedPreferences.

## Global Constraints

- `./gradlew verifyDesktop` doit passer à la fin de chaque tâche de l'Étape 1 et 2
- Jamais de `Color(0xFF...)` ni de valeur `.dp` en dur dans les composants — utiliser `AppTheme.colors.*` et `AppTheme.dimens.*`
- Pas de refacto des pages desktop existantes (elles restent telles quelles)
- Pas de nouveaux features métier
- minSdk Android : 26 (Android 8.0) — EncryptedSharedPreferences requiert API 23+, Android Keystore API 18+
- compileSdk Android : 35
- Package Android : `eu.ejdr`
- Toute navigation : `backStack.add(Route.X)` pour empiler, `backStack.removeLastOrNull()` pour revenir

---

# ÉTAPE 1 — Migrer le build Gradle

---

### Task 1: Migrer build.gradle.kts vers kotlin("multiplatform")

**Files:**
- Modify: `build.gradle.kts` (réécriture complète)
- Modify: `settings.gradle.kts` (ajout plugin AGP)
- Create: `src/desktopMain/kotlin/eu/ejdr/.gitkeep` (placeholder sourceset)
- Create: `src/commonMain/kotlin/eu/ejdr/.gitkeep` (placeholder sourceset)
- Create: `src/androidMain/kotlin/eu/ejdr/.gitkeep` (placeholder sourceset)

**Interfaces:**
- Produit: task Gradle `verifyDesktop` qui remplace `verify` pour le desktop
- Produit: task Gradle `packageExe` / `packageMsi` toujours fonctionnelle

- [ ] **Step 1: Créer les dossiers de sourcesets**

```powershell
New-Item -ItemType Directory -Force "src/desktopMain/kotlin/eu/ejdr"
New-Item -ItemType Directory -Force "src/commonMain/kotlin/eu/ejdr"
New-Item -ItemType Directory -Force "src/androidMain/kotlin/eu/ejdr"
New-Item -ItemType File -Force "src/desktopMain/kotlin/eu/ejdr/.gitkeep"
New-Item -ItemType File -Force "src/commonMain/kotlin/eu/ejdr/.gitkeep"
New-Item -ItemType File -Force "src/androidMain/kotlin/eu/ejdr/.gitkeep"
```

- [ ] **Step 2: Mettre à jour settings.gradle.kts**

Remplacer le contenu de `settings.gradle.kts` par :

```kotlin
rootProject.name = "ejdr-frontend"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
```

- [ ] **Step 3: Réécrire build.gradle.kts**

Remplacer le contenu entier de `build.gradle.kts` par :

```kotlin
import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("com.android.application") version "8.10.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

group = "eu.ejdr"
version = project.findProperty("version")?.toString() ?: "0.1.0"

val ktorVersion = "3.4.2"
val koinBom = "4.1.1"
val coroutinesVersion = "1.10.2"
val nav3Version = "1.1.1"
val lifecycleVersion = "2.10.0"

// ───────────────────────────────────────────────────────────────
// Configuration par environnement, résolue AU BUILD.
// ───────────────────────────────────────────────────────────────
val defaultsConfigFile = rootProject.file("config.defaults.properties")
val localConfigFile = rootProject.file("config.local.properties")

fun loadAppConfig(): Properties {
    val props = Properties()
    for (configFile in listOf(defaultsConfigFile, localConfigFile)) {
        if (configFile.exists()) configFile.inputStream().use { stream -> props.load(stream) }
    }
    return props
}

val generateBuildConfig by tasks.registering {
    val appVersion = project.version.toString()
    val appConfig = loadAppConfig()
    val apiUrl = appConfig.getProperty("api.url") ?: "https://ejdr-backend.vyxs.fr"
    val httpLogging = appConfig.getProperty("http.logging")?.toBoolean() ?: false
    val isDev = appConfig.getProperty("app.dev")?.toBoolean() ?: false
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig")
    outputs.dir(outputDir)
    inputs.property("version", appVersion)
    inputs.property("apiUrl", apiUrl)
    inputs.property("httpLogging", httpLogging)
    inputs.property("isDev", isDev)
    inputs.files(defaultsConfigFile, localConfigFile).optional()
    doLast {
        val file = outputDir.get().asFile.resolve("eu/ejdr/BuildConfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package eu.ejdr

            internal object BuildConfig {
                const val APP_VERSION = "$appVersion"
                const val GITHUB_REPO = "Yorobro/E-JDR-Frontend"
                const val API_URL = "$apiUrl"
                const val HTTP_LOGGING = $httpLogging
                const val IS_DEV = $isDev
            }
            """.trimIndent()
        )
    }
}

kotlin {
    jvmToolchain(21)

    jvm("desktop")

    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "21" }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

                // Koin
                implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koinBom"))
                implementation("io.insert-koin:koin-core")
                implementation("io.insert-koin:koin-compose")

                // Ktor (core uniquement — moteur dans chaque sourceset)
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-client-logging:$ktorVersion")

                // Navigation 3
                implementation("org.jetbrains.androidx.navigation3:navigation3-ui:$nav3Version")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3:$lifecycleVersion")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
                // JNA : DPAPI Windows
                implementation("net.java.dev.jna:jna-platform:5.18.1")
                // Ktor CIO (JVM)
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation("androidx.core:core-ktx:1.16.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.junit.jupiter:junit-jupiter:5.11.4")
                implementation("io.mockk:mockk:1.14.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
                implementation("io.ktor:ktor-client-mock:$ktorVersion")
            }
        }
    }
}

android {
    namespace = "eu.ejdr"
    compileSdk = 35

    defaultConfig {
        applicationId = "eu.ejdr"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = project.version.toString()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// ───────────────────────────────────────────────────────────────
// Desktop application packaging
// ───────────────────────────────────────────────────────────────
compose.desktop {
    application {
        mainClass = "eu.ejdr.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "E-JDR"
            packageVersion = project.version.toString()
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Qualité : detekt + Kover
// ───────────────────────────────────────────────────────────────
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    ignoreFailures = false
}

kover {
    reports {
        filters {
            excludes {
                packages(
                    "eu.ejdr.presentation.shared.component",
                    "eu.ejdr.presentation.shared.theme",
                    "eu.ejdr.presentation.shared.di",
                    "eu.ejdr.presentation.navigation",
                    "eu.ejdr.presentation.features.auth.page",
                    "eu.ejdr.presentation.features.auth.component",
                    "eu.ejdr.presentation.features.settings.page",
                    "eu.ejdr.presentation.features.settings.component",
                    "eu.ejdr.presentation.features.user.page",
                    "eu.ejdr.presentation.features.user.component",
                    "eu.ejdr.presentation.features.campaign.page",
                    "eu.ejdr.presentation.features.campaign.component",
                    "eu.ejdr.presentation.features.session.page",
                    "eu.ejdr.presentation.features.session.component",
                    "eu.ejdr.presentation.features.reference.page",
                    "eu.ejdr.presentation.features.reference.component",
                    "eu.ejdr.presentation.features.charactersheet.page",
                    "eu.ejdr.presentation.features.friendgroup.page",
                    "eu.ejdr.presentation.features.friendgroup.component",
                    "eu.ejdr.infrastructure.file",
                    "eu.ejdr.di",
                )
                classes(
                    "eu.ejdr.MainKt",
                    "eu.ejdr.presentation.AppKt",
                    "eu.ejdr.presentation.features.auth.AuthNavEntriesKt",
                    "eu.ejdr.presentation.features.user.UserNavEntriesKt",
                    "eu.ejdr.presentation.features.settings.SettingsNavEntriesKt",
                    "eu.ejdr.presentation.features.campaign.CampaignNavEntriesKt",
                    "eu.ejdr.presentation.features.session.SessionNavEntriesKt",
                    "eu.ejdr.presentation.features.reference.ReferenceNavEntriesKt",
                    "eu.ejdr.presentation.features.charactersheet.CharacterSheetNavEntriesKt",
                    "eu.ejdr.presentation.features.friendgroup.FriendGroupNavEntriesKt",
                    "eu.ejdr.presentation.navigation.NavActions",
                    "eu.ejdr.presentation.features.charactersheet.component.CampagnesTabKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCardKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetSectionsKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetTabsKt",
                    "eu.ejdr.presentation.features.charactersheet.component.ConfirmDeleteSheetDialogKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CreateCharacterSheetDialogKt",
                    "eu.ejdr.presentation.features.charactersheet.component.SheetCardKt",
                    "eu.ejdr.presentation.features.charactersheet.component.SheetLayoutKt",
                    "eu.ejdr.presentation.features.charactersheet.component.SheetReferenceComponentsKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetFormState",
                )
            }
        }
        verify {
            rule { minBound(60) }
        }
    }
}

tasks.named("check") {
    dependsOn("detekt", "desktopTest", "koverVerify")
}

tasks.register("verifyDesktop") {
    group = "verification"
    description = "Reproduit localement les vérifications CI pour la target desktop."
    dependsOn("detekt", "desktopJar", "koverVerify")
}

tasks.register("printVersion") {
    doLast { println(project.version.toString()) }
}
```

- [ ] **Step 4: Déplacer tout le code source dans desktopMain**

```powershell
# Déplacer le code source
$src = "src\main\kotlin\eu\ejdr"
$dst = "src\desktopMain\kotlin\eu\ejdr"
Get-ChildItem -Path $src -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring((Resolve-Path $src).Path.Length + 1)
    $target = Join-Path $dst $relative
    New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
    Move-Item $_.FullName $target
}

# Déplacer les tests
$srcTest = "src\test\kotlin\eu\ejdr"
$dstTest = "src\desktopTest\kotlin\eu\ejdr"
New-Item -ItemType Directory -Force $dstTest | Out-Null
Get-ChildItem -Path $srcTest -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring((Resolve-Path $srcTest).Path.Length + 1)
    $target = Join-Path $dstTest $relative
    New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
    Move-Item $_.FullName $target
}
```

- [ ] **Step 5: Vérifier que le build compile**

```powershell
.\gradlew desktopJar
```

Attendu : `BUILD SUCCESSFUL`. Si erreur sur une dépendance manquante dans `desktopMain`, vérifier que la dépendance est bien dans `desktopMain` (pas `commonMain`) dans le `build.gradle.kts`.

- [ ] **Step 6: Lancer les tests desktop**

```powershell
.\gradlew desktopTest
```

Attendu : tous les tests passent (même résultat qu'avant la migration).

- [ ] **Step 7: Commit**

```powershell
git add build.gradle.kts settings.gradle.kts src/desktopMain src/commonMain src/androidMain
git commit -m "build: migrate to kotlin multiplatform (desktop + android targets)"
```

---

### Task 2: Créer le AndroidManifest.xml et MainActivity stub

**Files:**
- Create: `src/androidMain/AndroidManifest.xml`
- Create: `src/androidMain/kotlin/eu/ejdr/MainActivity.kt`

**Interfaces:**
- Produit: APK compilable (même vide) via `./gradlew assembleDebug`

- [ ] **Step 1: Créer AndroidManifest.xml**

Créer `src/androidMain/AndroidManifest.xml` :

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="E-JDR"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

- [ ] **Step 2: Créer MainActivity stub**

Créer `src/androidMain/kotlin/eu/ejdr/MainActivity.kt` :

```kotlin
package eu.ejdr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("E-JDR Android — stub")
        }
    }
}
```

- [ ] **Step 3: Vérifier que l'APK compile**

```powershell
.\gradlew assembleDebug
```

Attendu : `BUILD SUCCESSFUL`, APK généré dans `build/outputs/apk/debug/`.

- [ ] **Step 4: Vérifier que desktop tient toujours**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add src/androidMain/AndroidManifest.xml src/androidMain/kotlin/eu/ejdr/MainActivity.kt
git commit -m "feat(android): add AndroidManifest and MainActivity stub"
```

---

# ÉTAPE 2 — Remonter le code commun en commonMain

> À chaque tâche, `./gradlew verifyDesktop` doit rester vert.

---

### Task 3: Remonter domain/ et application/ en commonMain

**Files:**
- Move: `src/desktopMain/kotlin/eu/ejdr/domain/` → `src/commonMain/kotlin/eu/ejdr/domain/`
- Move: `src/desktopMain/kotlin/eu/ejdr/application/` → `src/commonMain/kotlin/eu/ejdr/application/`

**Interfaces:**
- Produit: `domain/` et `application/` disponibles dans tous les sourcesets

- [ ] **Step 1: Déplacer domain/**

```powershell
$base = "src"
Move-Item "$base\desktopMain\kotlin\eu\ejdr\domain" "$base\commonMain\kotlin\eu\ejdr\domain"
```

- [ ] **Step 2: Déplacer application/**

```powershell
Move-Item "src\desktopMain\kotlin\eu\ejdr\application" "src\commonMain\kotlin\eu\ejdr\application"
```

- [ ] **Step 3: Vérifier**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`, tous les tests passent.

- [ ] **Step 4: Commit**

```powershell
git add src/commonMain src/desktopMain
git commit -m "refactor: move domain and application to commonMain"
```

---

### Task 4: Remonter infrastructure/http/ en commonMain

**Files:**
- Move: `src/desktopMain/kotlin/eu/ejdr/infrastructure/http/` → `src/commonMain/kotlin/eu/ejdr/infrastructure/http/`
- Modify: `src/commonMain/kotlin/eu/ejdr/infrastructure/http/KtorClientFactory.kt` (import SecureCookiesStorage via interface)

**Interfaces:**
- Produit: `KtorClientFactory`, tous les `*HttpRepository`, `*HttpMapper`, `*Dtos` disponibles en commonMain

- [ ] **Step 1: Déplacer infrastructure/http/**

```powershell
New-Item -ItemType Directory -Force "src\commonMain\kotlin\eu\ejdr\infrastructure"
Move-Item "src\desktopMain\kotlin\eu\ejdr\infrastructure\http" "src\commonMain\kotlin\eu\ejdr\infrastructure\http"
```

- [ ] **Step 2: Vérifier que KtorClientFactory compile en commonMain**

`KtorClientFactory` prend en paramètre `SecureCookiesStorage` qui est encore dans `desktopMain`. Il faut le remplacer par l'interface `SessionPersistence` + `CookiesStorage`. Modifier `src/commonMain/kotlin/eu/ejdr/infrastructure/http/KtorClientFactory.kt` :

```kotlin
package eu.ejdr.infrastructure.http

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.infrastructure.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json

private val RefreshRetryKey = AttributeKey<Unit>("RefreshRetry")

class KtorClientFactory(
    private val config: AppConfig,
    private val cookiesStorage: CookiesStorage,
    private val sessionPersistence: SessionPersistence,
    private val engineFactory: HttpClientEngineFactory<*>,
) {
    fun create(): HttpClient {
        val client = HttpClient(engineFactory) {
            expectSuccess = false
            install(HttpCookies) { storage = cookiesStorage }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            if (config.enableHttpLogging) {
                install(Logging) { level = LogLevel.INFO }
            }
        }

        client.plugin(HttpSend).intercept { request ->
            val call = execute(request)
            val isRetry = request.attributes.contains(RefreshRetryKey)
            val requestPath = request.url.build().encodedPath
            val isAuthRoute = requestPath.startsWith("/auth/")

            if (call.response.status != HttpStatusCode.Unauthorized || isRetry || isAuthRoute) {
                return@intercept call
            }

            val refreshCall = execute(
                HttpRequestBuilder().apply {
                    method = HttpMethod.Post
                    url { takeFrom("${config.baseUrl}/auth/refresh") }
                    attributes.put(RefreshRetryKey, Unit)
                },
            )

            if (!refreshCall.response.status.isSuccess()) {
                val refreshStatus = refreshCall.response.status
                if (refreshStatus == HttpStatusCode.Unauthorized ||
                    refreshStatus == HttpStatusCode.Forbidden
                ) {
                    sessionPersistence.clearPersisted()
                }
                return@intercept call
            }

            request.attributes.put(RefreshRetryKey, Unit)
            execute(request)
        }

        return client
    }
}
```

- [ ] **Step 3: Mettre à jour InfrastructureModule dans desktopMain**

Modifier `src/desktopMain/kotlin/eu/ejdr/di/InfrastructureModule.kt` pour passer l'`engineFactory` CIO et les nouvelles interfaces :

```kotlin
package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.file.DesktopFileSaver
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.DpapiSecretProtector
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.PlaintextSecretProtector
import eu.ejdr.infrastructure.security.SecretProtector
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.dsl.module

val infrastructureModule = module {
    single { AppConfig.load() }
    single<SecretProtector> {
        if (System.getProperty("os.name").orEmpty().startsWith("Windows")) DpapiSecretProtector()
        else PlaintextSecretProtector()
    }
    single { KeyStoreProvider(get<AppConfig>().dataDir, get<SecretProtector>()) }
    single { CookieCipher(get()) }
    single {
        SecureCookiesStorage(
            get<AppConfig>().dataDir,
            get(),
            get<AppConfig>().baseUrl,
            AcceptAllCookiesStorage(),
        )
    }
    single<SessionPersistence> { get<SecureCookiesStorage>() }
    single<HttpClient> {
        KtorClientFactory(
            config = get(),
            cookiesStorage = get<SecureCookiesStorage>(),
            sessionPersistence = get<SessionPersistence>(),
            engineFactory = CIO,
        ).create()
    }
    single<FileSaver> { DesktopFileSaver() }
}
```

- [ ] **Step 4: Vérifier**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add src/commonMain src/desktopMain
git commit -m "refactor: move infrastructure/http to commonMain, decouple KtorClientFactory engine"
```

---

### Task 5: Remonter infrastructure/config et interfaces security en commonMain

**Files:**
- Move: `src/desktopMain/kotlin/eu/ejdr/infrastructure/config/AppConfig.kt` → `src/commonMain/kotlin/eu/ejdr/infrastructure/config/AppConfig.kt`
- Modify: `src/commonMain/kotlin/eu/ejdr/infrastructure/config/AppConfig.kt` (retirer `System.getenv("APPDATA")`, ne garder que `baseUrl` + `httpLogging`)
- Create: `src/desktopMain/kotlin/eu/ejdr/infrastructure/config/AppConfigDesktop.kt` (provideDataDir desktop)
- Move: `src/desktopMain/kotlin/eu/ejdr/infrastructure/security/SecretProtector.kt` → `src/commonMain/kotlin/eu/ejdr/infrastructure/security/SecretProtector.kt`
- Move: `src/desktopMain/kotlin/eu/ejdr/infrastructure/security/CookieCipher.kt` → `src/commonMain/kotlin/eu/ejdr/infrastructure/security/CookieCipher.kt`
- Move: `src/desktopMain/kotlin/eu/ejdr/infrastructure/security/SecureCookiesStorage.kt` → `src/commonMain/kotlin/eu/ejdr/infrastructure/security/SecureCookiesStorage.kt`
- Modify: `src/commonMain/kotlin/eu/ejdr/infrastructure/security/SecureCookiesStorage.kt` (remplacer `java.io.File` par interface `SessionStorage`)
- Create: `src/desktopMain/kotlin/eu/ejdr/infrastructure/security/FileSessionStorage.kt`

**Interfaces:**
- Produit: `AppConfig(baseUrl, enableHttpLogging)` en commonMain — `dataDir` retiré
- Produit: `interface SessionStorage` en commonMain (lire/écrire/effacer le token persisté)
- Produit: `FileSessionStorage` en desktopMain (implémentation java.io.File)

- [ ] **Step 1: Créer l'interface SessionStorage en commonMain**

Créer `src/commonMain/kotlin/eu/ejdr/infrastructure/security/SessionStorage.kt` :

```kotlin
package eu.ejdr.infrastructure.security

interface SessionStorage {
    fun load(): String?
    fun save(value: String)
    fun clear()
    fun exists(): Boolean
}
```

- [ ] **Step 2: Créer FileSessionStorage en desktopMain**

Créer `src/desktopMain/kotlin/eu/ejdr/infrastructure/security/FileSessionStorage.kt` :

```kotlin
package eu.ejdr.infrastructure.security

import java.io.File

class FileSessionStorage(
    dataDir: File,
    private val cipher: CookieCipher,
) : SessionStorage {

    private val storeFile = File(dataDir, "secure-cookies.enc")

    override fun load(): String? {
        if (!storeFile.exists()) return null
        return runCatching { cipher.decrypt(storeFile.readText()) }.getOrNull()
    }

    override fun save(value: String) {
        storeFile.writeText(cipher.encrypt(value))
    }

    override fun clear() {
        if (storeFile.exists()) storeFile.delete()
    }

    override fun exists(): Boolean = storeFile.exists()
}
```

- [ ] **Step 3: Déplacer et adapter SecureCookiesStorage en commonMain**

Déplacer `src/desktopMain/kotlin/eu/ejdr/infrastructure/security/SecureCookiesStorage.kt` vers `src/commonMain/kotlin/eu/ejdr/infrastructure/security/SecureCookiesStorage.kt` et remplacer `java.io.File` par `SessionStorage` :

```kotlin
package eu.ejdr.infrastructure.security

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.http.isSecure

class SecureCookiesStorage(
    private val sessionStorage: SessionStorage,
    backendUrl: String,
    private val delegate: CookiesStorage = AcceptAllCookiesStorage(),
) : CookiesStorage, SessionPersistence {

    private val refreshTokenName = "refresh_token"
    private val backend = Url(backendUrl)
    private var pendingRefreshToken: String? = sessionStorage.load()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        delegate.addCookie(requestUrl, cookie)
        if (cookie.name == refreshTokenName) {
            sessionStorage.save(cookie.value)
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> {
        if (pendingRefreshToken != null && requestUrl.host == backend.host) {
            val value = pendingRefreshToken
            pendingRefreshToken = null
            if (value != null) {
                delegate.addCookie(requestUrl, restoredCookie(value))
            }
        }
        return delegate.get(requestUrl)
    }

    override fun close() = delegate.close()

    override fun hasPersistedSession(): Boolean = sessionStorage.exists()

    override fun clearPersisted() = sessionStorage.clear()

    private fun restoredCookie(value: String): Cookie = Cookie(
        name = refreshTokenName,
        value = value,
        domain = backend.host,
        path = "/",
        secure = backend.protocol.isSecure(),
        httpOnly = true,
    )
}
```

- [ ] **Step 4: Déplacer SecretProtector et CookieCipher en commonMain**

```powershell
Move-Item "src\desktopMain\kotlin\eu\ejdr\infrastructure\security\SecretProtector.kt" "src\commonMain\kotlin\eu\ejdr\infrastructure\security\SecretProtector.kt"
Move-Item "src\desktopMain\kotlin\eu\ejdr\infrastructure\security\CookieCipher.kt" "src\commonMain\kotlin\eu\ejdr\infrastructure\security\CookieCipher.kt"
```

Note : `CookieCipher` utilise `javax.crypto` et `java.util.Base64` qui sont disponibles en JVM mais pas KMP pur. On garde `CookieCipher` dans `desktopMain` pour l'instant (il n'est utilisé que par `FileSessionStorage` côté desktop). Annuler le move de CookieCipher :

```powershell
# CookieCipher reste dans desktopMain — utilise javax.crypto (JVM only)
# Ne déplacer que SecretProtector
```

Créer `src/commonMain/kotlin/eu/ejdr/infrastructure/security/SecretProtector.kt` (copie de l'interface uniquement, si ce n'est pas déjà une interface) :

```kotlin
package eu.ejdr.infrastructure.security

interface SecretProtector {
    fun protect(data: ByteArray): ByteArray
    fun reveal(data: ByteArray): ByteArray
}
```

Supprimer l'ancien fichier de desktopMain s'il reste.

- [ ] **Step 5: Séparer AppConfig**

Modifier `src/desktopMain/kotlin/eu/ejdr/infrastructure/config/AppConfig.kt` → devenir `src/commonMain/kotlin/eu/ejdr/infrastructure/config/AppConfig.kt` :

```kotlin
package eu.ejdr.infrastructure.config

import eu.ejdr.BuildConfig

data class AppConfig(
    val baseUrl: String,
    val enableHttpLogging: Boolean,
)

fun loadAppConfig(): AppConfig = AppConfig(
    baseUrl = BuildConfig.API_URL,
    enableHttpLogging = BuildConfig.HTTP_LOGGING,
)
```

Créer `src/desktopMain/kotlin/eu/ejdr/infrastructure/config/AppConfigDesktop.kt` :

```kotlin
package eu.ejdr.infrastructure.config

import java.io.File

fun provideDataDir(): File {
    val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
    return File(appData, "E-JDR").apply { mkdirs() }
}
```

- [ ] **Step 6: Mettre à jour InfrastructureModule desktop**

Modifier `src/desktopMain/kotlin/eu/ejdr/di/InfrastructureModule.kt` :

```kotlin
package eu.ejdr.di

import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.config.loadAppConfig
import eu.ejdr.infrastructure.config.provideDataDir
import eu.ejdr.infrastructure.file.DesktopFileSaver
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.security.CookieCipher
import eu.ejdr.infrastructure.security.DpapiSecretProtector
import eu.ejdr.infrastructure.security.FileSessionStorage
import eu.ejdr.infrastructure.security.KeyStoreProvider
import eu.ejdr.infrastructure.security.PlaintextSecretProtector
import eu.ejdr.infrastructure.security.SecretProtector
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.dsl.module

val infrastructureModule = module {
    single { loadAppConfig() }
    single { provideDataDir() }
    single<SecretProtector> {
        if (System.getProperty("os.name").orEmpty().startsWith("Windows")) DpapiSecretProtector()
        else PlaintextSecretProtector()
    }
    single { KeyStoreProvider(get(), get<SecretProtector>()) }
    single { CookieCipher(get()) }
    single { FileSessionStorage(get(), get<CookieCipher>()) }
    single {
        SecureCookiesStorage(
            sessionStorage = get<FileSessionStorage>(),
            backendUrl = get<AppConfig>().baseUrl,
            delegate = AcceptAllCookiesStorage(),
        )
    }
    single<SessionPersistence> { get<SecureCookiesStorage>() }
    single<HttpClient> {
        KtorClientFactory(
            config = get(),
            cookiesStorage = get<SecureCookiesStorage>(),
            sessionPersistence = get<SessionPersistence>(),
            engineFactory = CIO,
        ).create()
    }
    single<FileSaver> { DesktopFileSaver() }
}
```

- [ ] **Step 7: Vérifier**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```powershell
git add src/commonMain src/desktopMain
git commit -m "refactor: move AppConfig, SecretProtector, SecureCookiesStorage to commonMain"
```

---

### Task 6: Remonter le thème en commonMain (AppTheme, AppColors, AppDimens, AppTypography)

**Files:**
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/shared/theme/AppColors.kt` → `src/commonMain/`
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/shared/theme/AppDimens.kt` → `src/commonMain/`
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/shared/theme/AppTypography.kt` → `src/commonMain/`
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/shared/theme/AppTheme.kt` → `src/commonMain/`

**Interfaces:**
- Produit: `AppTheme`, `AppColors`, `AppDimens`, `AppTypography`, `lightColors()`, `darkColors()` disponibles en commonMain

- [ ] **Step 1: Déplacer les fichiers thème**

```powershell
$themeBase = "src\desktopMain\kotlin\eu\ejdr\presentation\shared\theme"
$themeDst  = "src\commonMain\kotlin\eu\ejdr\presentation\shared\theme"
New-Item -ItemType Directory -Force $themeDst | Out-Null
Move-Item "$themeBase\AppColors.kt"     "$themeDst\AppColors.kt"
Move-Item "$themeBase\AppDimens.kt"     "$themeDst\AppDimens.kt"
Move-Item "$themeBase\AppTypography.kt" "$themeDst\AppTypography.kt"
Move-Item "$themeBase\AppTheme.kt"      "$themeDst\AppTheme.kt"
```

- [ ] **Step 2: Vérifier**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add src/commonMain src/desktopMain
git commit -m "refactor: move AppTheme and design tokens to commonMain"
```

---

### Task 7: Remonter presentation/shared/component/ et presentation/shared/di/ en commonMain

**Files:**
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/shared/component/` → `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/`
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/shared/di/` → `src/commonMain/kotlin/eu/ejdr/presentation/shared/di/`

**Interfaces:**
- Produit: tous les composants atomiques/molecules et `koinViewModel` disponibles en commonMain

- [ ] **Step 1: Déplacer shared/component/ et shared/di/**

```powershell
$base = "src\desktopMain\kotlin\eu\ejdr\presentation\shared"
$dst  = "src\commonMain\kotlin\eu\ejdr\presentation\shared"
New-Item -ItemType Directory -Force $dst | Out-Null
Move-Item "$base\component" "$dst\component"
Move-Item "$base\di"        "$dst\di"
```

- [ ] **Step 2: Vérifier**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add src/commonMain src/desktopMain
git commit -m "refactor: move shared components and koinViewModel to commonMain"
```

---

### Task 8: Remonter ViewModels, Routes, RootState, SessionStatus en commonMain

**Files:**
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/navigation/Routes.kt` → `src/commonMain/`
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/navigation/NavActions.kt` → `src/commonMain/`
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/RootState.kt` → `src/commonMain/`
- Move: `src/desktopMain/kotlin/eu/ejdr/presentation/SessionStatus.kt` → `src/commonMain/` (ou extraire de RootState.kt si inline)
- Move: tous les `*ViewModel.kt` de `src/desktopMain/.../features/*/` → `src/commonMain/.../features/*/`
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/navigation/NavItems.kt`

**Interfaces:**
- Produit: `NavItem`, `appNavItems` — logique de visibilité navbar partagée
- Produit: `SessionStatus` enum disponible en commonMain
- Produit: tous les ViewModels disponibles en commonMain

- [ ] **Step 1: Déplacer Routes.kt et NavActions.kt**

```powershell
$navBase = "src\desktopMain\kotlin\eu\ejdr\presentation\navigation"
$navDst  = "src\commonMain\kotlin\eu\ejdr\presentation\navigation"
New-Item -ItemType Directory -Force $navDst | Out-Null
Move-Item "$navBase\Routes.kt"    "$navDst\Routes.kt"
Move-Item "$navBase\NavActions.kt" "$navDst\NavActions.kt"
```

- [ ] **Step 2: Déplacer RootState.kt (qui contient SessionStatus)**

```powershell
$presDst = "src\commonMain\kotlin\eu\ejdr\presentation"
New-Item -ItemType Directory -Force $presDst | Out-Null
Move-Item "src\desktopMain\kotlin\eu\ejdr\presentation\RootState.kt" "$presDst\RootState.kt"
```

- [ ] **Step 3: Déplacer tous les ViewModels**

```powershell
$features = @("auth","settings","user","campaign","session","charactersheet","reference","friendgroup")
foreach ($f in $features) {
    $src = "src\desktopMain\kotlin\eu\ejdr\presentation\features\$f"
    $dst = "src\commonMain\kotlin\eu\ejdr\presentation\features\$f"
    New-Item -ItemType Directory -Force $dst | Out-Null
    Get-ChildItem "$src\*ViewModel.kt" -ErrorAction SilentlyContinue | ForEach-Object {
        Move-Item $_.FullName "$dst\$($_.Name)"
    }
    # Aussi les ActiveGroupViewModel si présent
    Get-ChildItem "$src\Active*ViewModel.kt" -ErrorAction SilentlyContinue | ForEach-Object {
        Move-Item $_.FullName "$dst\$($_.Name)"
    }
}
```

- [ ] **Step 4: Créer NavItems.kt en commonMain**

Créer `src/commonMain/kotlin/eu/ejdr/presentation/navigation/NavItems.kt` :

```kotlin
package eu.ejdr.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import eu.ejdr.presentation.SessionStatus

data class NavItem(
    val route: Route,
    val label: String,
    val icon: ImageVector,
    val isVisible: (sessionStatus: SessionStatus, activeGroupId: String?) -> Boolean,
)

val appNavItems: List<NavItem> = listOf(
    NavItem(
        route = Route.Home,
        label = "Accueil",
        icon = Icons.Default.Home,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Campaigns,
        label = "Campagnes",
        icon = Icons.Default.LibraryBooks,
        isVisible = { s, g -> s == SessionStatus.Authenticated && g != null },
    ),
    NavItem(
        route = Route.CharacterSheets,
        label = "Fiches",
        icon = Icons.Default.Person,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.ReferenceHub,
        label = "Références",
        icon = Icons.Default.LibraryBooks,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Groups,
        label = "Groupes",
        icon = Icons.Default.Groups,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
    NavItem(
        route = Route.Settings,
        label = "Paramètres",
        icon = Icons.Default.Settings,
        isVisible = { s, _ -> s == SessionStatus.Authenticated },
    ),
)
```

- [ ] **Step 5: Vérifier**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`, tous les tests passent.

- [ ] **Step 6: Commit**

```powershell
git add src/commonMain src/desktopMain
git commit -m "refactor: move ViewModels, Routes, RootState, NavItems to commonMain"
```

---

### Task 9: Remonter les feature components (sans les pages) en commonMain

**Files:**
- Move: `src/desktopMain/.../features/*/component/` → `src/commonMain/.../features/*/component/` pour chaque feature

**Interfaces:**
- Produit: composants sans layout (cards, dialogs, chips) disponibles en commonMain

- [ ] **Step 1: Déplacer les component/ de chaque feature**

```powershell
$features = @("auth","settings","user","campaign","session","charactersheet","reference","friendgroup")
foreach ($f in $features) {
    $src = "src\desktopMain\kotlin\eu\ejdr\presentation\features\$f\component"
    $dst = "src\commonMain\kotlin\eu\ejdr\presentation\features\$f\component"
    if (Test-Path $src) {
        New-Item -ItemType Directory -Force $dst | Out-Null
        Get-ChildItem "$src\*.kt" | ForEach-Object {
            Move-Item $_.FullName "$dst\$($_.Name)"
        }
    }
}
```

- [ ] **Step 2: Vérifier**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`. Si un composant utilise `java.awt.*` ou `Dispatchers.Swing`, le laisser dans `desktopMain` et ne pas le déplacer.

- [ ] **Step 3: Commit**

```powershell
git add src/commonMain src/desktopMain
git commit -m "refactor: move feature components to commonMain"
```

---

### Task 10: Remonter les modules Koin communs en commonMain

**Files:**
- Move: `src/desktopMain/kotlin/eu/ejdr/di/AppKoin.kt` → `src/commonMain/kotlin/eu/ejdr/di/AppKoin.kt`
- Move: `src/desktopMain/kotlin/eu/ejdr/di/AuthModule.kt` → `src/commonMain/kotlin/eu/ejdr/di/AuthModule.kt`
- Move: tous les `*Module.kt` sauf `InfrastructureModule.kt` → `src/commonMain/kotlin/eu/ejdr/di/`
- Rename: `src/desktopMain/kotlin/eu/ejdr/di/InfrastructureModule.kt` → `InfrastructureModuleDesktop.kt`

**Interfaces:**
- Produit: modules Koin feature disponibles en commonMain
- Produit: `InfrastructureModuleDesktop` clair dans desktopMain

- [ ] **Step 1: Déplacer les modules feature en commonMain**

```powershell
$diSrc = "src\desktopMain\kotlin\eu\ejdr\di"
$diDst = "src\commonMain\kotlin\eu\ejdr\di"
New-Item -ItemType Directory -Force $diDst | Out-Null

$modules = @("AppKoin.kt","AuthModule.kt","SettingsModule.kt","UpdateModule.kt",
             "CampaignModule.kt","SessionModule.kt","CharacterSheetModule.kt",
             "ReferenceModule.kt","FriendGroupModule.kt")
foreach ($m in $modules) {
    if (Test-Path "$diSrc\$m") {
        Move-Item "$diSrc\$m" "$diDst\$m"
    }
}
```

- [ ] **Step 2: Renommer InfrastructureModule en InfrastructureModuleDesktop**

```powershell
Rename-Item "src\desktopMain\kotlin\eu\ejdr\di\InfrastructureModule.kt" "InfrastructureModuleDesktop.kt"
```

Mettre à jour le nom de la variable dans le fichier — changer `val infrastructureModule` en gardant le même nom (AppKoin.kt l'importe par nom de variable, pas par nom de fichier, donc pas de changement nécessaire dans AppKoin.kt).

- [ ] **Step 3: Vérifier**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git add src/commonMain src/desktopMain
git commit -m "refactor: move Koin feature modules to commonMain, rename InfrastructureModuleDesktop"
```

---

### Task 11: Vérification finale Étape 2

- [ ] **Step 1: Lancer la suite complète**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`, tous les tests passent, couverture ≥ 60%.

- [ ] **Step 2: Vérifier que desktopMain ne contient que du code platform-specific**

Les seuls fichiers restant dans `src/desktopMain/` doivent être :
- `main.kt`
- `infrastructure/security/` : `DpapiSecretProtector`, `PlaintextSecretProtector`, `KeyStoreProvider`, `CookieCipher`, `FileSessionStorage`
- `infrastructure/file/DesktopFileSaver`
- `infrastructure/system/WindowsSystemLauncher`
- `infrastructure/settings/ThemeFileRepository`, `ActiveGroupFileRepository`
- `infrastructure/config/AppConfigDesktop`
- `di/InfrastructureModuleDesktop`
- `presentation/App.kt`
- `presentation/navigation/AppNavDisplay.kt`, `NavDecoratorDesktop.kt`
- `presentation/features/*/page/*.kt` (pages desktop)
- `presentation/features/*/*NavEntries.kt`

- [ ] **Step 3: Commit de marquage**

```powershell
git commit --allow-empty -m "chore: etape 2 complete — commonMain finalisé, desktop intact"
```

---

# ÉTAPE 3 — Implémenter Android

---

### Task 12: Infrastructure Android (Keystore, SharedPreferences, OkHttp)

**Files:**
- Create: `src/androidMain/kotlin/eu/ejdr/infrastructure/security/AndroidSessionStorage.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/infrastructure/config/AppConfigAndroid.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/infrastructure/settings/AndroidThemeRepository.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/infrastructure/settings/AndroidActiveGroupRepository.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/infrastructure/file/AndroidFileSaver.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/infrastructure/system/AndroidUpdateLauncher.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/di/InfrastructureModuleAndroid.kt`

**Interfaces:**
- Consomme: `SessionStorage` (interface de Task 5), `ThemeRepository`, `ActiveGroupRepository`, `FileSaver`, `SystemLauncherService`
- Produit: toutes les implémentations Android branchées via Koin

- [ ] **Step 1: Créer AndroidSessionStorage**

Créer `src/androidMain/kotlin/eu/ejdr/infrastructure/security/AndroidSessionStorage.kt` :

```kotlin
package eu.ejdr.infrastructure.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AndroidSessionStorage(context: Context) : SessionStorage {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ejdr_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val key = "refresh_token"

    override fun load(): String? = prefs.getString(key, null)

    override fun save(value: String) = prefs.edit().putString(key, value).apply()

    override fun clear() = prefs.edit().remove(key).apply()

    override fun exists(): Boolean = prefs.contains(key)
}
```

- [ ] **Step 2: Créer AppConfigAndroid**

Créer `src/androidMain/kotlin/eu/ejdr/infrastructure/config/AppConfigAndroid.kt` :

```kotlin
package eu.ejdr.infrastructure.config

import android.content.Context
import java.io.File

fun provideDataDir(context: Context): File = context.filesDir
```

- [ ] **Step 3: Créer AndroidThemeRepository**

Créer `src/androidMain/kotlin/eu/ejdr/infrastructure/settings/AndroidThemeRepository.kt` :

```kotlin
package eu.ejdr.infrastructure.settings

import android.content.Context
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.domain.features.settings.error.SettingsError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidThemeRepository(context: Context) : ThemeRepository {

    private val prefs = context.getSharedPreferences("ejdr_settings", Context.MODE_PRIVATE)
    private val themeKey = "theme"

    override suspend fun getTheme(): ThemeVariant = withContext(Dispatchers.IO) {
        prefs.getString(themeKey, null)
            ?.let { runCatching { ThemeVariant.valueOf(it) }.getOrNull() }
            ?: ThemeVariant.LIGHT
    }

    override suspend fun setTheme(theme: ThemeVariant): Result<Unit, SettingsError> =
        withContext(Dispatchers.IO) {
            val written = runCatching {
                prefs.edit().putString(themeKey, theme.name).commit()
            }.getOrDefault(false)
            if (written) Result.Success(Unit) else Result.Failure(SettingsError.ThemePersistenceFailed)
        }
}
```

- [ ] **Step 4: Créer AndroidActiveGroupRepository**

Créer `src/androidMain/kotlin/eu/ejdr/infrastructure/settings/AndroidActiveGroupRepository.kt` :

```kotlin
package eu.ejdr.infrastructure.settings

import android.content.Context
import eu.ejdr.application.features.friendgroup.abstraction.repository.ActiveGroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidActiveGroupRepository(context: Context) : ActiveGroupRepository {

    private val prefs = context.getSharedPreferences("ejdr_settings", Context.MODE_PRIVATE)
    private val key = "activeGroupId"

    override suspend fun getActiveGroupId(): String? = withContext(Dispatchers.IO) {
        prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    override suspend fun setActiveGroupId(id: String?) = withContext(Dispatchers.IO) {
        if (id != null) prefs.edit().putString(key, id).apply()
        else prefs.edit().remove(key).apply()
    }
}
```

- [ ] **Step 5: Créer AndroidFileSaver**

Créer `src/androidMain/kotlin/eu/ejdr/infrastructure/file/AndroidFileSaver.kt` :

```kotlin
package eu.ejdr.infrastructure.file

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidFileSaver(private val context: Context) : FileSaver {

    override suspend fun save(suggestedName: String, bytes: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(context.cacheDir, suggestedName)
                file.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, suggestedName).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            }.getOrDefault(false)
        }
}
```

- [ ] **Step 6: Créer AndroidUpdateLauncher**

Créer `src/androidMain/kotlin/eu/ejdr/infrastructure/system/AndroidUpdateLauncher.kt` :

```kotlin
package eu.ejdr.infrastructure.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService

class AndroidUpdateLauncher(private val context: Context) : SystemLauncherService {

    override fun openUpdateDestination() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            // Fallback si Play Store absent : ouvrir le navigateur
            val webIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
```

- [ ] **Step 7: Créer InfrastructureModuleAndroid**

Créer `src/androidMain/kotlin/eu/ejdr/di/InfrastructureModuleAndroid.kt` :

```kotlin
package eu.ejdr.di

import android.content.Context
import eu.ejdr.application.features.auth.abstraction.service.SessionPersistence
import eu.ejdr.application.features.charactersheet.abstraction.service.FileSaver
import eu.ejdr.application.features.friendgroup.abstraction.repository.ActiveGroupRepository
import eu.ejdr.application.features.settings.abstraction.repository.ThemeRepository
import eu.ejdr.application.features.update.abstraction.service.SystemLauncherService
import eu.ejdr.infrastructure.config.AppConfig
import eu.ejdr.infrastructure.config.loadAppConfig
import eu.ejdr.infrastructure.file.AndroidFileSaver
import eu.ejdr.infrastructure.http.KtorClientFactory
import eu.ejdr.infrastructure.security.AndroidSessionStorage
import eu.ejdr.infrastructure.security.SecureCookiesStorage
import eu.ejdr.infrastructure.settings.AndroidActiveGroupRepository
import eu.ejdr.infrastructure.settings.AndroidThemeRepository
import eu.ejdr.infrastructure.system.AndroidUpdateLauncher
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val infrastructureModule = module {
    single { loadAppConfig() }
    single { androidContext() }
    single { AndroidSessionStorage(get<Context>()) }
    single {
        SecureCookiesStorage(
            sessionStorage = get<AndroidSessionStorage>(),
            backendUrl = get<AppConfig>().baseUrl,
            delegate = AcceptAllCookiesStorage(),
        )
    }
    single<SessionPersistence> { get<SecureCookiesStorage>() }
    single<HttpClient> {
        KtorClientFactory(
            config = get(),
            cookiesStorage = get<SecureCookiesStorage>(),
            sessionPersistence = get<SessionPersistence>(),
            engineFactory = OkHttp,
        ).create()
    }
    single<ThemeRepository> { AndroidThemeRepository(get<Context>()) }
    single<ActiveGroupRepository> { AndroidActiveGroupRepository(get<Context>()) }
    single<FileSaver> { AndroidFileSaver(get<Context>()) }
    single<SystemLauncherService> { AndroidUpdateLauncher(get<Context>()) }
}
```

- [ ] **Step 8: Ajouter FileProvider dans AndroidManifest.xml**

Modifier `src/androidMain/AndroidManifest.xml` pour ajouter le FileProvider (nécessaire pour AndroidFileSaver) :

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="E-JDR"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
```

Créer `src/androidMain/res/xml/file_paths.xml` :

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="shared_files" path="." />
</paths>
```

- [ ] **Step 9: Vérifier que l'APK compile**

```powershell
.\gradlew assembleDebug
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

```powershell
git add src/androidMain
git commit -m "feat(android): add infrastructure implementations (Keystore, SharedPrefs, OkHttp)"
```

---

### Task 13: App.kt Android + AppNavDisplay Android (bottom bar)

**Files:**
- Create: `src/androidMain/kotlin/eu/ejdr/presentation/App.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/presentation/navigation/NavDecoratorAndroid.kt`
- Modify: `src/androidMain/kotlin/eu/ejdr/MainActivity.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/di/AppKoinAndroid.kt`

**Interfaces:**
- Consomme: `appNavItems` (NavItems.kt commonMain), `RootState` (commonMain), `SessionStatus` (commonMain)
- Produit: entry point Android fonctionnel avec navigation bottom bar

- [ ] **Step 1: Créer AppNavDisplay Android**

Créer `src/androidMain/kotlin/eu/ejdr/presentation/navigation/AppNavDisplay.kt` :

```kotlin
package eu.ejdr.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.SessionStatus
import eu.ejdr.presentation.features.auth.authEntries
import eu.ejdr.presentation.features.campaign.campaignEntries
import eu.ejdr.presentation.features.charactersheet.characterSheetEntries
import eu.ejdr.presentation.features.friendgroup.friendGroupEntries
import eu.ejdr.presentation.features.reference.referenceEntries
import eu.ejdr.presentation.features.session.sessionEntries
import eu.ejdr.presentation.features.settings.settingsEntries
import eu.ejdr.presentation.features.user.userEntries
import eu.ejdr.presentation.shared.theme.AppTheme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    sessionStatus: StateFlow<SessionStatus>,
    activeGroupId: StateFlow<String?>,
    onLogout: () -> Unit,
    onThemeChange: (ThemeVariant) -> Unit,
    resetTo: (Route) -> Unit,
) {
    val actions = NavActions(backStack, onLogout, onThemeChange, resetTo)
    val status by sessionStatus.collectAsStateWithLifecycle()
    val groupId by activeGroupId.collectAsStateWithLifecycle()

    val visibleItems = appNavItems.filter { it.isVisible(status, groupId) }
    val currentRoute = backStack.lastOrNull()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(rememberEjdrViewModelStoreNavEntryDecorator()),
                entryProvider = entryProvider {
                    entry<Route.Splash> { SplashScreen() }
                    authEntries(actions)
                    userEntries(actions)
                    settingsEntries(actions)
                    campaignEntries(actions)
                    sessionEntries(actions)
                    referenceEntries(actions)
                    characterSheetEntries(actions)
                    friendGroupEntries(actions)
                },
            )
        }

        if (status == SessionStatus.Authenticated && visibleItems.isNotEmpty()) {
            NavigationBar(Modifier.fillMaxWidth()) {
                visibleItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute?.let { it::class == item.route::class } ?: false,
                        onClick = {
                            if (currentRoute?.let { it::class != item.route::class } != false) {
                                backStack.add(item.route)
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(AppTheme.colors.background),
        Alignment.Center,
    ) { CircularProgressIndicator(color = AppTheme.colors.primary) }
}
```

- [ ] **Step 2: Créer NavDecoratorAndroid.kt**

Créer `src/androidMain/kotlin/eu/ejdr/presentation/navigation/NavDecoratorAndroid.kt` :

```kotlin
package eu.ejdr.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator

@Composable
fun rememberEjdrViewModelStoreNavEntryDecorator(): NavEntryDecorator =
    rememberViewModelStoreNavEntryDecorator()
```

- [ ] **Step 3: Créer App.kt Android**

Créer `src/androidMain/kotlin/eu/ejdr/presentation/App.kt` :

```kotlin
package eu.ejdr.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import eu.ejdr.application.features.auth.abstraction.usecase.LogoutUseCase
import eu.ejdr.application.features.auth.abstraction.usecase.RestoreSessionUseCase
import eu.ejdr.application.features.settings.abstraction.usecase.GetThemeUseCase
import eu.ejdr.application.features.update.abstraction.usecase.CheckUpdateUseCase
import eu.ejdr.application.features.update.dto.UpdateInfoDto
import eu.ejdr.application.shared.getOrNull
import eu.ejdr.domain.features.settings.entities.ThemeVariant
import eu.ejdr.presentation.features.update.UpdateController
import eu.ejdr.presentation.navigation.AppNavDisplay
import eu.ejdr.presentation.navigation.Route
import eu.ejdr.presentation.navigation.appNavConfiguration
import eu.ejdr.presentation.shared.component.organism.UpdateDialog
import eu.ejdr.presentation.shared.theme.AppTheme
import eu.ejdr.presentation.shared.theme.darkColors
import eu.ejdr.presentation.shared.theme.lightColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val getTheme = koinInject<GetThemeUseCase>()
    val restoreSession = koinInject<RestoreSessionUseCase>()
    val rootState = remember { RootState(scope, getTheme, restoreSession) }
    val themeVariant by rootState.theme.collectAsStateWithLifecycle()

    AppTheme(
        colors = when (themeVariant) {
            ThemeVariant.LIGHT -> lightColors()
            ThemeVariant.DARK -> darkColors()
        },
    ) {
        val logout = koinInject<LogoutUseCase>()
        val checkUpdate = koinInject<CheckUpdateUseCase>()

        val backStack = rememberNavBackStack(appNavConfiguration, Route.Splash)
        var updateInfo by remember { mutableStateOf<UpdateInfoDto?>(null) }
        val sessionStatus by rootState.sessionStatus.collectAsStateWithLifecycle()

        fun resetTo(route: Route) {
            backStack.clear()
            backStack.add(route)
        }

        LaunchedEffect(Unit) {
            launch { updateInfo = checkUpdate().getOrNull() }
            rootState.restoreSession()
        }

        LaunchedEffect(sessionStatus) {
            when (sessionStatus) {
                SessionStatus.Authenticated -> resetTo(Route.Home)
                SessionStatus.Unauthenticated -> resetTo(Route.Login)
                SessionStatus.Unknown -> Unit
            }
        }

        // activeGroupId est géré par le ViewModel friendGroup — on expose un flow vide ici
        // et le ViewModel le mettra à jour une fois injecté.
        val activeGroupId = remember { MutableStateFlow<String?>(null) }

        AppNavDisplay(
            backStack = backStack,
            sessionStatus = rootState.sessionStatus,
            activeGroupId = activeGroupId,
            onLogout = { scope.launch { logout(); resetTo(Route.Login) } },
            onThemeChange = rootState::setTheme,
            resetTo = ::resetTo,
        )

        updateInfo?.let { info ->
            UpdateDialog(
                info = info,
                onOpenReleasePage = { updateInfo = null },
                onDismiss = { updateInfo = null },
            )
        }
    }
}
```

Note : `UpdateDialog` Android n'affiche pas le bouton "Télécharger et installer" — uniquement "Voir sur le Play Store" (délégué à `SystemLauncherService`). Adapter le composable `UpdateDialog` en commonMain pour accepter un callback `onOpenStore: (() -> Unit)?` optionnel au lieu du download handler.

- [ ] **Step 4: Mettre à jour MainActivity**

Modifier `src/androidMain/kotlin/eu/ejdr/MainActivity.kt` :

```kotlin
package eu.ejdr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import eu.ejdr.presentation.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}
```

- [ ] **Step 5: Créer AppKoinAndroid.kt**

Créer `src/androidMain/kotlin/eu/ejdr/di/AppKoinAndroid.kt` :

```kotlin
package eu.ejdr.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

fun initKoinAndroid(app: Application) = startKoin {
    androidContext(app)
    modules(
        infrastructureModule,   // InfrastructureModuleAndroid
        authModule,
        settingsModule,
        updateModule,
        campaignModule,
        sessionModule,
        characterSheetModule,
        referenceModule,
        friendGroupModule,
    )
}
```

Créer `src/androidMain/kotlin/eu/ejdr/EjdrApplication.kt` :

```kotlin
package eu.ejdr

import android.app.Application
import eu.ejdr.di.initKoinAndroid

class EjdrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoinAndroid(this)
    }
}
```

Mettre à jour `AndroidManifest.xml` pour référencer l'Application :

```xml
<application
    android:name=".EjdrApplication"
    ...>
```

Ajouter également `koin-android` dans `androidMain` dependencies de `build.gradle.kts` :

```kotlin
val androidMain by getting {
    dependencies {
        // ... existant ...
        implementation("io.insert-koin:koin-android:$koinBom")
    }
}
```

- [ ] **Step 6: Vérifier que l'APK compile**

```powershell
.\gradlew assembleDebug
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```powershell
git add src/androidMain
git commit -m "feat(android): add App composable, AppNavDisplay with bottom bar, MainActivity wired"
```

---

### Task 14: Pages Android — Auth (Login, Register)

**Files:**
- Create: `src/androidMain/kotlin/eu/ejdr/presentation/features/auth/page/LoginPage.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/presentation/features/auth/page/RegisterPage.kt`
- Create: `src/androidMain/kotlin/eu/ejdr/presentation/features/auth/AuthNavEntries.kt`

**Interfaces:**
- Consomme: `AuthViewModel` (commonMain), callbacks `onAuthenticated`, `onGoToRegister`, `onGoToLogin`
- Produit: pages Login et Register Android (layout mobile : champs full-width, boutons larges)

- [ ] **Step 1: Créer LoginPage Android**

Créer `src/androidMain/kotlin/eu/ejdr/presentation/features/auth/page/LoginPage.kt` :

```kotlin
package eu.ejdr.presentation.features.auth.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.presentation.features.auth.AuthViewModel
import eu.ejdr.presentation.shared.component.atom.AppButton
import eu.ejdr.presentation.shared.component.atom.AppTextField
import eu.ejdr.presentation.shared.component.atom.AppTextButton
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

@Composable
fun LoginPage(
    onAuthenticated: () -> Unit,
    onGoToRegister: () -> Unit,
) {
    val vm = koinViewModel { AuthViewModel(get(), get()) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(AppTheme.dimens.md),
        verticalArrangement = Arrangement.Center,
    ) {
        AppTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AppTheme.dimens.sm))
        AppTextField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = "Mot de passe",
            isPassword = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AppTheme.dimens.md))
        AppButton(
            text = "Se connecter",
            onClick = { vm.login(onAuthenticated) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading,
        )
        Spacer(Modifier.height(AppTheme.dimens.sm))
        AppTextButton(
            text = "Créer un compte",
            onClick = onGoToRegister,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { error ->
            Spacer(Modifier.height(AppTheme.dimens.sm))
            // AppErrorText est un composant atom existant en commonMain
            AppErrorText(text = error)
        }
    }
}
```

- [ ] **Step 2: Créer RegisterPage Android**

Créer `src/androidMain/kotlin/eu/ejdr/presentation/features/auth/page/RegisterPage.kt` :

```kotlin
package eu.ejdr.presentation.features.auth.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.presentation.features.auth.AuthViewModel
import eu.ejdr.presentation.shared.component.atom.AppButton
import eu.ejdr.presentation.shared.component.atom.AppTextField
import eu.ejdr.presentation.shared.component.atom.AppTextButton
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

@Composable
fun RegisterPage(
    onAuthenticated: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    val vm = koinViewModel { AuthViewModel(get(), get()) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(AppTheme.dimens.md),
        verticalArrangement = Arrangement.Center,
    ) {
        AppTextField(
            value = state.pseudo,
            onValueChange = vm::onPseudoChange,
            label = "Pseudo",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AppTheme.dimens.sm))
        AppTextField(
            value = state.email,
            onValueChange = vm::onEmailChange,
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AppTheme.dimens.sm))
        AppTextField(
            value = state.password,
            onValueChange = vm::onPasswordChange,
            label = "Mot de passe",
            isPassword = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(AppTheme.dimens.md))
        AppButton(
            text = "Créer le compte",
            onClick = { vm.register(onAuthenticated) },
            modifier = Modifier.fillMaxWidth(),
            isLoading = state.isLoading,
        )
        Spacer(Modifier.height(AppTheme.dimens.sm))
        AppTextButton(
            text = "J'ai déjà un compte",
            onClick = onGoToLogin,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { error ->
            Spacer(Modifier.height(AppTheme.dimens.sm))
            AppErrorText(text = error)
        }
    }
}
```

- [ ] **Step 3: Créer AuthNavEntries Android**

Créer `src/androidMain/kotlin/eu/ejdr/presentation/features/auth/AuthNavEntries.kt` :

```kotlin
package eu.ejdr.presentation.features.auth

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.auth.page.LoginPage
import eu.ejdr.presentation.features.auth.page.RegisterPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route

fun EntryProviderScope<Any>.authEntries(actions: NavActions) {
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

- [ ] **Step 4: Vérifier compilation**

```powershell
.\gradlew assembleDebug
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add src/androidMain
git commit -m "feat(android): add Login and Register pages"
```

---

### Task 15: Pages Android — les features restantes (une par feature)

> Répéter le même pattern pour chaque feature. Chaque page Android est un `LazyColumn` ou `Column` full-width avec les mêmes ViewModels que desktop.

**Files à créer pour chaque feature** (même structure que Task 14) :
- `src/androidMain/.../features/campaign/page/CampaignListPage.kt`
- `src/androidMain/.../features/campaign/page/CampaignDetailPage.kt`
- `src/androidMain/.../features/campaign/CampaignNavEntries.kt`
- `src/androidMain/.../features/session/page/SessionDetailPage.kt`
- `src/androidMain/.../features/session/SessionNavEntries.kt`
- `src/androidMain/.../features/charactersheet/page/MyCharacterSheetsPage.kt`
- `src/androidMain/.../features/charactersheet/page/CharacterSheetDetailPage.kt`
- `src/androidMain/.../features/charactersheet/CharacterSheetNavEntries.kt`
- `src/androidMain/.../features/reference/page/ReferenceHubPage.kt`
- `src/androidMain/.../features/reference/page/ReferenceListPage.kt`
- `src/androidMain/.../features/reference/ReferenceNavEntries.kt`
- `src/androidMain/.../features/friendgroup/page/GroupListPage.kt`
- `src/androidMain/.../features/friendgroup/page/GroupDetailPage.kt`
- `src/androidMain/.../features/friendgroup/page/InvitationsPage.kt`
- `src/androidMain/.../features/friendgroup/FriendGroupNavEntries.kt`
- `src/androidMain/.../features/settings/page/SettingsPage.kt`
- `src/androidMain/.../features/settings/SettingsNavEntries.kt`
- `src/androidMain/.../features/user/page/UserPage.kt`
- `src/androidMain/.../features/user/UserNavEntries.kt`

**Pattern à suivre pour chaque page Android :**

```kotlin
// Exemple : CampaignListPage Android
package eu.ejdr.presentation.features.campaign.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.ejdr.presentation.features.campaign.CampaignListViewModel
import eu.ejdr.presentation.features.campaign.component.CampaignCard        // commonMain
import eu.ejdr.presentation.features.campaign.component.CreateCampaignDialog // commonMain
import eu.ejdr.presentation.shared.component.organism.AppFab
import eu.ejdr.presentation.shared.di.koinViewModel
import eu.ejdr.presentation.shared.theme.AppTheme

@Composable
fun CampaignListPage(
    onOpenCampaign: (id: String, name: String) -> Unit,
) {
    val vm = koinViewModel { CampaignListViewModel(get(), get(), get(), get()) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(AppTheme.dimens.md),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.sm),
        ) {
            items(state.campaigns) { campaign ->
                CampaignCard(
                    campaign = campaign,
                    onClick = { onOpenCampaign(campaign.id, campaign.name) },
                    onDelete = { vm.delete(campaign.id) },
                )
            }
        }
        AppFab(
            onClick = { showCreate = true },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(AppTheme.dimens.md),
        )
    }

    if (showCreate) {
        CreateCampaignDialog(
            onConfirm = { name -> vm.create(name); showCreate = false },
            onDismiss = { showCreate = false },
        )
    }
}
```

**Pattern NavEntries Android (identique au desktop) :**

```kotlin
// CampaignNavEntries.kt (androidMain)
package eu.ejdr.presentation.features.campaign

import androidx.navigation3.runtime.EntryProviderScope
import eu.ejdr.presentation.features.campaign.page.CampaignListPage
import eu.ejdr.presentation.features.campaign.page.CampaignDetailPage
import eu.ejdr.presentation.navigation.NavActions
import eu.ejdr.presentation.navigation.Route

fun EntryProviderScope<Any>.campaignEntries(actions: NavActions) {
    entry<Route.Campaigns> {
        CampaignListPage(
            onOpenCampaign = { id, name -> actions.backStack.add(Route.CampaignDetail(id, name)) },
        )
    }
    entry<Route.CampaignDetail> { key ->
        CampaignDetailPage(
            campaignId = key.id,
            campaignName = key.name,
            onBack = { actions.backStack.removeLastOrNull() },
            onOpenSession = { id, title -> actions.backStack.add(Route.SessionDetail(id, title)) },
        )
    }
}
```

- [ ] **Step 1: Implémenter les NavEntries et pages Android pour chaque feature selon le pattern ci-dessus**

Répéter pour : `campaign`, `session`, `charactersheet`, `reference`, `friendgroup`, `settings`, `user`.

Pour chaque feature :
1. Créer la/les page(s) Android dans `page/` (layout `LazyColumn` / `Column` full-width)
2. Créer le `*NavEntries.kt` Android (identique au desktop en structure, importe les pages Android)

- [ ] **Step 2: Vérifier compilation après chaque feature**

```powershell
.\gradlew assembleDebug
```

Attendu : `BUILD SUCCESSFUL` après chaque feature.

- [ ] **Step 3: Commit par feature**

```powershell
git add src/androidMain
git commit -m "feat(android): add <feature> pages and nav entries"
```

---

### Task 16: Vérification finale Étape 3

- [ ] **Step 1: Vérifier desktop inchangé**

```powershell
.\gradlew verifyDesktop
```

Attendu : `BUILD SUCCESSFUL`, tous les tests passent.

- [ ] **Step 2: Vérifier APK debug**

```powershell
.\gradlew assembleDebug
```

Attendu : `BUILD SUCCESSFUL`.

- [ ] **Step 3: Installer et tester manuellement sur émulateur**

```powershell
.\gradlew installDebug
```

Parcours minimal à valider :
1. Splash → Login (champs visibles, clavier qui monte)
2. Login réussi → Home → Bottom bar visible avec les items corrects
3. Navigation Campagnes → liste → détail → retour
4. Export PDF fiche → Share sheet Android s'ouvre
5. Notification mise à jour → bouton Play Store s'ouvre dans le navigateur
6. Logout → retour Login, bottom bar disparaît

- [ ] **Step 4: Commit final**

```powershell
git commit --allow-empty -m "chore: etape 3 complete — Android target fonctionnel"
```
