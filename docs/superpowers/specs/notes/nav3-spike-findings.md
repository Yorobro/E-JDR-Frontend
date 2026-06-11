# Findings — Spike Navigation 3 (chantier D)

**Date** : 2026-06-11
**But** : valider empiriquement, contre Gradle/compilateur, les versions et l'API
Navigation 3 + ViewModels retenus sur **desktop JVM** avant la réécriture de la
présentation (chantier E). Les docs en ligne donnaient des versions contradictoires
et ne confirmaient pas le nom du decorator de rétention de ViewModel.

## Verdict : ✅ FEU VERT pour Navigation 3

Tout résout et compile sur notre stack (Compose Multiplatform 1.11.1, Kotlin 2.2.20,
JVM desktop). Nav3 est **stable** (1.1.1), pas en alpha — la principale inquiétude de
l'audit (« lib trop jeune ») est levée.

## Versions retenues (vérifiées sur Maven Central, résolues par Gradle)

```kotlin
implementation("org.jetbrains.androidx.navigation3:navigation3-ui:1.1.1")
implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3:2.10.0")
implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
```

- `navigation3-ui:1.1.1` est une **release stable** (versions : …1.0.0-alphaNN, 1.1.0, **1.1.1**).
- `navigation3-runtime` (les API `NavKey`/`entryProvider`/`NavDisplay`) arrive
  **transitivement** via `navigation3-ui` → `androidx.navigation3:navigation3-runtime-desktop:1.1.1`.
  Il n'existe PAS d'artefact `org.jetbrains.androidx.navigation3:navigation3-runtime`
  à déclarer (404 sur Maven Central) — ne pas le mettre dans le build.
- Variantes **desktop** publiées et confirmées : `navigation3-ui-desktop`,
  `lifecycle-viewmodel-navigation3-desktop`, `lifecycle-viewmodel-compose-desktop`.
- `lifecycle-viewmodel-navigation3:2.10.0` est stable (2.11.0-beta01 existe ; on reste
  sur la stable).

## API confirmée (par compilation réelle + javap sur les jars)

| Besoin | API exacte | Package |
|---|---|---|
| Clé de route (typée) | `interface … : NavKey`, `@Serializable` | `androidx.navigation3.runtime.NavKey` |
| Back-stack possédé par l'app | `rememberNavBackStack(SavedStateConfiguration, vararg NavKey): NavBackStack` | `androidx.navigation3.runtime` |
| Config sérialisation back-stack | `SavedStateConfiguration.DEFAULT` (1er arg requis, pas d'overload sans) | `androidx.savedstate.serialization` |
| Affichage | `NavDisplay(backStack, onBack, entryDecorators, entryProvider)` | `androidx.navigation3.ui` |
| DSL d'entrées | `entryProvider { entry<Route> { key -> … } }` | `entryProvider` importé ; `entry` est **membre de `EntryProviderScope`** (PAS un import) |
| **Rétention ViewModel** | `rememberViewModelStoreNavEntryDecorator()` → passé à `entryDecorators = listOf(...)` | `androidx.lifecycle.viewmodel.navigation3` |
| Accès ViewModel | `viewModel { MyVm() }` dans une `entry<>` | `androidx.lifecycle.viewmodel.compose` |

### Pièges rencontrés (corrigés)

1. `entry` n'est PAS importable depuis `androidx.navigation3.runtime.entry` — c'est une
   fonction membre du receiver `EntryProviderScope` du lambda `entryProvider { }`.
2. `rememberNavBackStack` exige un `SavedStateConfiguration` en 1er argument ; utiliser
   `SavedStateConfiguration.DEFAULT` pour le cas simple.

## Conséquences pour le chantier E

- `navigation/Routes.kt` : `sealed interface`/objets `@Serializable : NavKey`.
- `App.kt` possède `rememberNavBackStack(SavedStateConfiguration.DEFAULT, …)` et monte
  `NavDisplay` avec `entryDecorators = listOf(rememberViewModelStoreNavEntryDecorator())`.
- Chaque page intelligente : un `ViewModel` (constructeur = use cases injectés Koin),
  obtenu via `viewModel { … }` dans son `entry<>` → retenu par destination.

> Le code de spike (`eu/ejdr/_spike/Nav3Spike.kt`) a été **supprimé** après ces
> findings ; seules les dépendances validées et ce document subsistent.
