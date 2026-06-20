import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

group = "eu.ejdr"
version = project.findProperty("version")?.toString() ?: "0.1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val ktorVersion = "3.4.2"
val koinBom = "4.1.1"
val coroutinesVersion = "1.10.2"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // JNA : accès à DPAPI (Crypt32 CryptProtectData/CryptUnprotectData) sous Windows pour
    // protéger le mot de passe du KeyStore par un secret lié à l'utilisateur Windows.
    implementation("net.java.dev.jna:jna-platform:5.18.1")

    // Koin (BOM)
    implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koinBom"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-compose")

    // Ktor client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Navigation 3 (back-stack possédé par l'app) + ViewModels retenus par destination.
    implementation("org.jetbrains.androidx.navigation3:navigation3-ui:1.1.1")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Tests
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}

// ───────────────────────────────────────────────────────────────
// Configuration par environnement, résolue AU BUILD (pas au runtime).
//
// On lit `config.defaults.properties` (versionné) puis on le surcharge avec
// `config.local.properties` (non versionné, ignoré par git) si présent. Les
// valeurs sont gelées dans `BuildConfig` → le binaire packagé n'a AUCUNE
// dépendance à l'environnement système de la machine qui le lance.
// Portable desktop / Android / iOS : ces constantes vivront en commonMain.
// ───────────────────────────────────────────────────────────────
val defaultsConfigFile = rootProject.file("config.defaults.properties")
val localConfigFile = rootProject.file("config.local.properties")

fun loadAppConfig(): Properties {
    val props = Properties()
    for (configFile in listOf(defaultsConfigFile, localConfigFile)) {
        if (configFile.exists()) {
            configFile.inputStream().use { stream -> props.load(stream) }
        }
    }
    return props
}

val generateBuildConfig by tasks.registering {
    val appVersion = project.version.toString()
    val appConfig = loadAppConfig()
    val apiUrl = appConfig.getProperty("api.url") ?: "https://ejdr-backend.vyxs.fr"
    val httpLogging = appConfig.getProperty("http.logging")?.toBoolean() ?: false
    // `app.dev` n'est défini que dans `config.local.properties` (non versionné) : faux par
    // défaut → les binaires packagés (prod) restent en mode normal.
    val isDev = appConfig.getProperty("app.dev")?.toBoolean() ?: false
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig")
    outputs.dir(outputDir)
    inputs.property("version", appVersion)
    inputs.property("apiUrl", apiUrl)
    inputs.property("httpLogging", httpLogging)
    inputs.property("isDev", isDev)
    // Re-générer dès qu'un fichier de config change (correction de l'incrémental Gradle).
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
    sourceSets.main {
        kotlin.srcDir(generateBuildConfig)
    }
}

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
// Qualité : detekt (analyse statique) + Kover (couverture)
//
// detekt est retenu plutôt que ktlint : les versions actuelles du plugin ktlint
// embarquent un frontend Kotlin incompatible avec Kotlin 2.2 (échec de parsing).
// detekt analyse le code source pour les vraies anomalies (complexité, code mort,
// constructions risquées) sans imposer un style de mise en forme.
// ───────────────────────────────────────────────────────────────
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    // Ne pas analyser les sources générées / compilées.
    ignoreFailures = false
}

kover {
    reports {
        filters {
            excludes {
                // On compte la LOGIQUE de présentation (ViewModels, RootState) mais pas l'UI
                // Compose pure (composables, thème, navigation), testée via `run`. Kover excluant
                // récursivement par package, on cible les sous-packages UI + quelques classes UI
                // qui cohabitent avec les ViewModels dans les packages de feature.
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
                    // NB : charactersheet.component n'est PAS exclu en bloc — ce package contient
                    // aussi de la LOGIQUE PURE testable (PurseFormat, StatDisplay, StatKeys) qu'une
                    // exclusion récursive masquait. On exclut donc ses composables UI un par un,
                    // classe par classe, ci-dessous (section classes()).
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
                    // Composables UI du package charactersheet.component (exclus nommément à la
                    // place de l'ancienne exclusion de package, qui emportait aussi la logique
                    // pure PurseFormat/StatDisplay/StatKeys — désormais comptée et testée).
                    "eu.ejdr.presentation.features.charactersheet.component.CampagnesTabKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetCardKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetSectionsKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetTabsKt",
                    "eu.ejdr.presentation.features.charactersheet.component.ConfirmDeleteSheetDialogKt",
                    "eu.ejdr.presentation.features.charactersheet.component.CreateCharacterSheetDialogKt",
                    "eu.ejdr.presentation.features.charactersheet.component.SheetCardKt",
                    "eu.ejdr.presentation.features.charactersheet.component.SheetLayoutKt",
                    "eu.ejdr.presentation.features.charactersheet.component.SheetReferenceComponentsKt",
                    // État d'édition couplé à Compose (mutableStateOf) : pas de couverture unitaire utile.
                    "eu.ejdr.presentation.features.charactersheet.component.CharacterSheetFormState",
                )
                // N.B. : les ViewModels (AuthViewModel, UserViewModel, SettingsViewModel),
                // UpdateController et RootState restent COMPTÉS — ils portent de la logique testée.
            }
        }
        verify {
            rule {
                minBound(60) // plancher de couverture : la CI échoue en dessous.
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────
// Alignement local ↔ CI.
//
// La CI lance `detekt`, `build` puis `koverVerify`. Pour qu'une commande
// locale unique reproduise EXACTEMENT ce que vérifie la CI (et éviter qu'un
// problème ne passe inaperçu en dev pour n'apparaître qu'en CI), on rattache
// ces vérifications à la tâche standard `check` et on expose l'alias `verify`.
//
// Désormais `./gradlew verify` (ou `check`) = ce que fait la CI.
// ───────────────────────────────────────────────────────────────
tasks.named("check") {
    dependsOn("detekt", "test", "koverVerify")
}

tasks.register("verify") {
    group = "verification"
    description = "Reproduit localement les vérifications de la CI (detekt + tests + couverture)."
    dependsOn("detekt", "build", "koverVerify")
}

// Utility task to expose the project version for CI
tasks.register("printVersion") {
    doLast {
        println(project.version.toString())
    }
}
