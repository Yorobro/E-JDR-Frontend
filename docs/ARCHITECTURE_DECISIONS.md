# Décisions d'architecture — E-JDR Frontend

## Domaine anémique côté frontend

### Décision

Le domaine frontend est **délibérément anémique** : les entités (`User`, `Credentials`, etc.) sont
de pures `data class` sans méthodes ni invariants. La logique d'orchestration vit exclusivement
dans les use cases (`application/usecase/`).

### Pourquoi c'est différent du backend

Le backend (Node API) possède un **domaine riche** : les entités encodent des invariants métier
directement (ex. `Campaign.addPlayer()` lève une erreur si la capacité est atteinte). C'est
intentionnel, car le backend est la **source de vérité** des règles métier.

Le frontend, lui, est un **client** : il orchestre les appels réseau, mappe les réponses et
affiche les erreurs renvoyées par le serveur. Dupliquer les invariants des deux côtés créerait
une source de vérité secondaire qui divergerait inévitablement.

### Conséquences pratiques

**À faire :**
- Appeler le use case, attendre la réponse/erreur du serveur, l'afficher.
- Mapper les erreurs HTTP/domaine en messages lisibles dans la couche `application` ou `presentation`.
- Valider uniquement ce qui relève de l'UX locale : champs vides, format d'email côté saisie.

**À ne pas faire :**
- Encoder une règle métier dans une entité frontend (ex. `Campaign.canAddPlayer(): Boolean`).
- Reproduire les validations serveur dans un use case frontend (ex. "max 4 joueurs" côté client).
- Prendre des décisions métier basées sur l'état local sans confirmation du serveur.

### Exemple concret — future feature Campaign

Mauvaise approche (à éviter) :
```kotlin
// ❌ Invariant dupliqué côté frontend
data class Campaign(val players: List<User>) {
    fun canAddPlayer(): Boolean = players.size < 4
}
```

Bonne approche :
```kotlin
// ✓ Entité anémique
data class Campaign(val id: String, val players: List<User>)

// ✓ Use case orchestre l'appel ; le serveur renvoie l'erreur si la limite est atteinte
class JoinCampaignUseCase(private val repo: CampaignRepository) {
    suspend operator fun invoke(campaignId: String): Result<Campaign, DomainError> =
        repo.joinCampaign(campaignId)
}
```

Le frontend affiche l'erreur retournée (`CampaignFull`, `NotFound`, etc.) sans jamais
décider lui-même si l'action est permise.

## 2026-06-12 — Durcissement post-re-audit

Un re-audit adversarial de l'architecture front a conduit à une série de corrections de
cohérence et de scalabilité (toutes livrées sur `main`). Décisions retenues :

### Contrats des use cases uniformisés
Toutes les frontières use case/repository renvoient désormais `Result<_, DomainError>` et sont
`suspend`, y compris quand l'implémentation est triviale. Fini les retours nus / nullable /
`Boolean` / exceptions qui fuyaient :
- `GetThemeUseCase`/`SetThemeUseCase` + `ThemeRepository` : `suspend` + `Result`, I/O sur
  `Dispatchers.IO` (plus de lecture bloquante au constructeur du ViewModel).
- `CheckUpdateUseCase` → `Result<UpdateInfoDto?, UpdateError>` ; `DownloadAndInstallUpdateUseCase`
  → `Result<Unit, UpdateError>`. Nouvelle erreur domaine `UpdateError` (CheckFailed/DownloadFailed).

### `Result` enrichi
Le type railway `Result` expose maintenant `map`/`flatMap`/`mapError`/`getOrNull`/`getOrElse`/
`onSuccess`/`onFailure` (en plus de `fold`), pour éviter le code verbeux de transformation.

### Navigation distribuée par feature
Le mapping route→écran n'est plus centralisé dans `AppNavDisplay`. Chaque feature expose une
fonction `xxxEntries(actions: NavActions)` (`presentation/features/<feature>/<Feature>NavEntries.kt`)
agrégée dans `AppNavDisplay`. **Rappel** : toute nouvelle `Route` doit être (1) ajoutée à
`appNavConfiguration` via `subclass(...)` dans `Routes.kt` (sinon crash au démarrage) ET (2)
rendue par une entry dans le `*NavEntries.kt` de sa feature.

### DI par feature
Les god-modules `applicationModule`/`infrastructureModule` sont remplacés par un module Koin
**par feature** (`authModule`, `settingsModule`, `updateModule`), sur le modèle de `realtimeModule`.
`infrastructureModule` ne garde que le socle transverse (config, sécurité, HttpClient). Ajouter
une feature = ajouter son module dans `AppKoin`.

### État applicatif global
Le thème et le statut de session sont centralisés dans `RootState` (source de vérité unique),
remplaçant les `mutableStateOf` ad-hoc et le state-lifting manuel de `App.kt`.

### State-holders à la racine ≠ ViewModels androidx
Les holders créés **hors de l'arbre de navigation** (`RootState`, `UpdateController`) sont de
simples classes pilotées par un `CoroutineScope` injecté, PAS des `ViewModel` androidx : à la
racine il n'existe aucun `ViewModelStoreOwner`, donc `viewModel { }`/`koinViewModel` y crasherait
au runtime. Les ViewModels par destination (`AuthViewModel`, etc.) restent de vrais ViewModels
retenus par le décorateur Nav3.

### Intercepteur 401 désambiguïsé
Le refresh silencieux n'efface la session persistée que sur un **401/403** (token réellement
expiré). Sur une panne serveur (5xx) ou réseau, la session est conservée (panne transitoire).

### Couverture honnête
Kover ne compte plus l'UI Compose mais **compte désormais les ViewModels + RootState** (logique
testée). Le plancher de 60 % s'applique à la vraie logique.

### Release sécurisée
Le workflow de release dépend d'une CI verte (`needs: ci`) : un commit dont les tests échouent
ne peut plus produire de binaire publié.

### Divers
`FormState` (inutilisé) supprimé ; helper de cohérence.
