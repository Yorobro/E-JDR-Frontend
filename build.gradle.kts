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
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generateBuildConfig)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.components.resources)

                // Icônes Lucide (Compose Multiplatform) — jeu d'icônes du design system, mappé
                // sous les mêmes noms dans AppIcons. Seules les icônes référencées sont conservées.
                implementation("com.composables:icons-lucide-cmp:2.2.1")

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
                // WebSockets (temps réel) — artefact multiplateforme, moteur fourni par chaque sourceset
                implementation("io.ktor:ktor-client-websockets:$ktorVersion")

                // Navigation 3
                implementation("org.jetbrains.androidx.navigation3:navigation3-ui:$nav3Version")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3:$lifecycleVersion")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
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
                // Koin Android (androidContext) — version alignée sur le BOM commun
                implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koinBom"))
                implementation("io.insert-koin:koin-android")
                // Dispatchers.Main sur Android (requis par RootState / ActiveGroupState au runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
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
    compileSdk = 36

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
            // Racine des ressources d'installation passées à jpackage (--resource-dir). Les fichiers
            // de `packaging/windows/` sont injectés côté Windows : on y place « E-JDR.iss », un
            // template Inno Setup qui REMPLACE celui par défaut de jpackage et ajoute une case
            // « Lancer E-JDR » précochée en fin de 1re installation (cf. packaging/windows/E-JDR.iss).
            // Sous-dossiers reconnus : windows / macos / linux / common.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("packaging"))
            windows {
                // Crée le raccourci dans le menu Démarrer → l'application devient
                // recherchable via la loupe Windows (l'installeur .exe / Inno Setup l'honore).
                menu = true
                menuGroup = "E-JDR"
                // UUID FIXE : ne JAMAIS le modifier. Il identifie le produit pour que les
                // futures versions remplacent proprement l'ancienne (au lieu d'installer
                // un doublon), ce qui est indispensable avec l'auto-update.
                upgradeUuid = "68252833-3dec-4fee-bb7d-b87f6b0ff26d"
            }
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
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCardKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetSectionsKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetTabsKt",
                    "eu.ejdr.presentation.features.charactersheet.component.ConfirmDeleteSheetDialogKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CopyCharacterSheetDialogKt",
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
