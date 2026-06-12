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
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")

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

val generateBuildConfig by tasks.registering {
    val appVersion = project.version.toString()
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig")
    outputs.dir(outputDir)
    inputs.property("version", appVersion)
    doLast {
        val file = outputDir.get().asFile.resolve("eu/ejdr/BuildConfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package eu.ejdr

            internal object BuildConfig {
                const val APP_VERSION = "$appVersion"
                const val GITHUB_REPO = "Yorobro/E-JDR-Frontend"
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
                    "eu.ejdr.di",
                )
                classes(
                    "eu.ejdr.MainKt",
                    "eu.ejdr.presentation.AppKt",
                    "eu.ejdr.presentation.features.auth.AuthNavEntriesKt",
                    "eu.ejdr.presentation.features.user.UserNavEntriesKt",
                    "eu.ejdr.presentation.features.settings.SettingsNavEntriesKt",
                    "eu.ejdr.presentation.navigation.NavActions",
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
