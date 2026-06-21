# Design : Migration Compose Multiplatform (Desktop + Android)

**Date** : 2026-06-20
**Statut** : Approuvé

---

## Contexte

Le frontend E-JDR est aujourd'hui une application Kotlin Desktop pure (`kotlin("jvm")`, Compose Desktop 1.11, JVM 21). L'objectif est de le rendre multiplateforme — Desktop Windows + Android mobile — en adoptant Kotlin Multiplatform (KMP) et Compose Multiplatform, sans régression sur le desktop et sans changer le backend.

---

## Décisions clés

| Question | Décision |
|---|---|
| Structure Gradle | Mono-module KMP (`kotlin("multiplatform")` dans un seul `build.gradle.kts`) |
| Navigation Android | Navigation3 identique au desktop, même `sealed interface Route` |
| Navigation UI | Sidebar desktop + Bottom bar Android, logique de visibilité partagée en `commonMain` |
| Sécurité Android | Android Keystore + EncryptedSharedPreferences (remplace DPAPI + JCEKS) |
| Mise à jour Android | Afficher la notif de nouvelle version, ouvrir le Play Store (pas d'auto-install) |
| Thème | `AppTheme` partagé à 100%, restructuré pour qu'un futur changement visuel soit centralisé |
| Stratégie migration | Approche B : 3 étapes séquentielles, desktop fonctionnel à chaque fin d'étape |

---

## Architecture cible : structure des sourcesets

```
src/
├── commonMain/kotlin/eu/ejdr/
│   ├── domain/                          ← inchangé, 0 modification
│   ├── application/                     ← inchangé, 0 modification
│   ├── infrastructure/
│   │   ├── http/                        ← inchangé (Ktor multiplatform)
│   │   ├── config/AppConfig.kt          ← baseUrl + httpLogging (commun)
│   │   └── security/
│   │       ├── SecretProtector.kt       ← interface (déjà existe)
│   │       ├── CookieCipher.kt          ← AES-GCM (javax.crypto = commun KMP)
│   │       └── SecureCookiesStorage.kt  ← logique commune
│   ├── presentation/
│   │   ├── shared/
│   │   │   ├── component/               ← atoms/molecules partagés
│   │   │   └── theme/                   ← AppTheme, AppColors, AppTypography, AppSpacing
│   │   ├── navigation/
│   │   │   ├── Routes.kt                ← sealed interface Route (commun)
│   │   │   └── NavItems.kt              ← logique de visibilité navbar (commun)
│   │   └── features/*/
│   │       ├── *ViewModel.kt            ← ViewModels (communs)
│   │       └── component/               ← composants sans layout (communs)
│   └── di/                              ← modules Koin communs
│
├── desktopMain/kotlin/eu/ejdr/
│   ├── main.kt
│   ├── infrastructure/
│   │   ├── security/
│   │   │   ├── DpapiSecretProtector.kt
│   │   │   ├── KeyStoreProvider.kt
│   │   │   └── SecureCookiesStorageDesktop.kt
│   │   ├── file/DesktopFileSaver.kt
│   │   ├── system/WindowsSystemLauncher.kt
│   │   ├── settings/ThemeFileRepository.kt
│   │   ├── settings/ActiveGroupFileRepository.kt
│   │   └── config/AppConfigDesktop.kt   ← provideDataDir() → APPDATA/E-JDR
│   ├── di/InfrastructureModuleDesktop.kt
│   └── presentation/
│       ├── App.kt                       ← entry composable desktop
│       ├── navigation/AppNavDisplay.kt  ← sidebar desktop
│       └── features/*/page/             ← layouts desktop (inchangés)
│
└── androidMain/kotlin/eu/ejdr/
    ├── MainActivity.kt
    ├── infrastructure/
    │   ├── security/
    │   │   ├── AndroidSecretProtector.kt       ← Android Keystore
    │   │   └── EncryptedPrefsStorage.kt        ← EncryptedSharedPreferences
    │   ├── file/AndroidFileSaver.kt            ← CreateDocument intent
    │   ├── system/AndroidUpdateLauncher.kt     ← Intent ACTION_VIEW Play Store
    │   ├── settings/AndroidThemeRepository.kt ← SharedPreferences
    │   ├── settings/AndroidActiveGroupRepository.kt
    │   └── config/AppConfigAndroid.kt          ← provideDataDir() → context.filesDir
    ├── di/InfrastructureModuleAndroid.kt
    └── presentation/
        ├── App.kt                              ← entry composable Android
        ├── navigation/AppNavDisplay.kt         ← bottom bar Android
        └── features/*/page/                    ← layouts Android (nouveaux)
```

---

## Thème et système de design

### Principe

Un seul endroit pour changer l'esthétique complète. Toutes les valeurs visuelles sont centralisées, les composants n'ont jamais de `Color(0xFF...)` ou `16.dp` en dur.

### Fichiers cibles dans `commonMain/presentation/shared/theme/`

**`AppColors.kt`** — couleurs nommées sémantiquement :
```kotlin
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val surface: Color,
    val onSurface: Color,
    val background: Color,
    val onBackground: Color,
    val error: Color,
    val onError: Color,
    // ... toutes les couleurs métier
)

fun lightColors(): AppColors = AppColors(primary = Color(0xFF...), ...)
fun darkColors(): AppColors = AppColors(primary = Color(0xFF...), ...)
```

**`AppTypography.kt`** — toutes les tailles et familles de police centralisées.

**`AppSpacing.kt`** — espacements nommés (les composants utilisent `AppSpacing.md` au lieu de `16.dp`) :
```kotlin
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
```

**`AppShapes.kt`** — rayons de coins centralisés.

**`AppTheme.kt`** — inchangé structurellement, wrapping `MaterialTheme`.

### Navbar partagée

La logique de visibilité des items vit en `commonMain/presentation/navigation/NavItems.kt` :

```kotlin
data class NavItem(
    val route: Route,
    val label: String,
    val icon: ImageVector,
    val isVisible: (sessionStatus: SessionStatus, activeGroupId: String?) -> Boolean
)

val appNavItems: List<NavItem> = listOf(
    NavItem(Route.Home, "Accueil", Icons.Home,
        isVisible = { s, _ -> s == SessionStatus.Authenticated }),
    NavItem(Route.Campaigns, "Campagnes", Icons.Campaign,
        isVisible = { s, g -> s == SessionStatus.Authenticated && g != null }),
    // ...
)
```

- **Desktop** : `desktopMain/AppNavDisplay` consomme `appNavItems` → rend une sidebar
- **Android** : `androidMain/AppNavDisplay` consomme `appNavItems` → rend une `NavigationBar`

Même règles de visibilité, deux rendus.

---

## Infrastructure platform-specific

### 6 interfaces bifurquées

| Interface | desktopMain | androidMain |
|---|---|---|
| `SecretProtector` | `DpapiSecretProtector` (DPAPI + JNA) | `AndroidSecretProtector` (Android Keystore) |
| `SessionPersistence` | `SecureCookiesStorage` (java.io.File + JCEKS + AES-GCM) | `EncryptedPrefsStorage` (EncryptedSharedPreferences) |
| `FileSaver` | `DesktopFileSaver` (java.awt.FileDialog) | `AndroidFileSaver` (CreateDocument intent) |
| `SystemLauncherService` | `WindowsSystemLauncher` (ouvre navigateur release GitHub) | `AndroidUpdateLauncher` (Intent Play Store) |
| `ThemeRepository` | `ThemeFileRepository` (java.util.Properties) | `AndroidThemeRepository` (SharedPreferences) |
| `ActiveGroupRepository` | `ActiveGroupFileRepository` (java.util.Properties) | `AndroidActiveGroupRepository` (SharedPreferences) |

### Moteur Ktor par plateforme

| Sourceset | Moteur |
|---|---|
| `desktopMain` | `ktor-client-cio` (JVM, inchangé) |
| `androidMain` | `ktor-client-okhttp` (Android) |

### Dépendances Gradle par sourceset

| Dépendance | commonMain | desktopMain | androidMain |
|---|---|---|---|
| `ktor-client-core` | ✓ | | |
| `ktor-client-cio` | | ✓ | |
| `ktor-client-okhttp` | | | ✓ |
| `koin-core` | ✓ | | |
| `koin-compose` | ✓ | | |
| `jna-platform` (DPAPI) | | ✓ | |
| `androidx.security:security-crypto` | | | ✓ |
| `navigation3` | ✓ | | |
| `compose.desktop.currentOs` | | ✓ | |

---

## Plan de migration : Approche B (3 étapes séquentielles)

### Étape 1 — Migrer le build (1-2 jours)

**Objectif** : passer en KMP sans toucher une ligne de code métier. Desktop identique à aujourd'hui.

Actions :
- `build.gradle.kts` : `kotlin("jvm")` → `kotlin("multiplatform")` + `com.android.application`
- Déplacer tout le code existant dans `desktopMain/` (déplacement de fichiers, pas de refacto)
- Créer `androidMain/` avec un `MainActivity.kt` minimal
- `commonMain/` vide pour l'instant
- Configurer `android {}` bloc (minSdk, compileSdk, etc.)

**Critère de succès** : `./gradlew verifyDesktop` passe, le `.exe` se produit, zéro régression.

---

### Étape 2 — Remonter le code commun en `commonMain` (3-5 jours)

**Objectif** : tout ce qui n'est pas platform-specific remonte. Desktop toujours fonctionnel.

Ordre de remontée :
1. `domain/` entier
2. `application/` entier
3. `infrastructure/http/` entier
4. `presentation/shared/theme/` → restructuration (`AppColors`, `AppSpacing`, `AppShapes`, `AppTypography`)
5. `presentation/shared/component/` (atoms/molecules)
6. ViewModels + Routes
7. `NavItems.kt` (logique navbar partagée)
8. Interfaces platform-specific déplacées en `commonMain`, implémentations restent dans `desktopMain/`
9. Modules Koin communs extraits, `InfrastructureModuleDesktop` garde les bindings platform-specific

**Critère de succès** : `./gradlew verifyDesktop` passe, comportement desktop identique.

---

### Étape 3 — Implémenter Android (1-2 semaines)

**Objectif** : app Android fonctionnelle avec toutes les features.

Ordre :
1. `InfrastructureModuleAndroid` : Keystore, OkHttp, EncryptedPrefs, SharedPreferences
2. `MainActivity` + `App.kt` Android
3. `AppNavDisplay` Android (bottom bar avec `appNavItems`)
4. Pages Android feature par feature :
   - Auth (Login, Register)
   - Home
   - Campaigns + CampaignDetail
   - Sessions
   - CharacterSheets + CharacterSheetDetail
   - References
   - FriendGroups + GroupDetail + Invitations
   - Settings

**Critère de succès** : APK installable, login → navigation → toutes les features fonctionnelles.

---

## Ce qui est hors scope

- Refacto des pages desktop existantes (elles restent telles quelles)
- Changement du backend
- Nouveaux features métier
- CI Android (ajouté après l'étape 3)
- Support iOS
- Rotation d'écran Android (portrait uniquement dans un premier temps)
