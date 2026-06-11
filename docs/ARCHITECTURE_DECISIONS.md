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
