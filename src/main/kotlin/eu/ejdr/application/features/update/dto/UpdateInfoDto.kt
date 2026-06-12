package eu.ejdr.application.features.update.dto

/**
 * Objet de transport (DTO) **applicatif** décrivant une mise à jour disponible.
 *
 * C'est le type de retour du port [eu.ejdr.application.features.update.abstraction.repository.UpdateRepository] :
 * il vit donc dans la couche application (consommé par le use case `CheckUpdate` et la
 * présentation), et non dans `infrastructure/`. Étant une donnée concrète et non un
 * contrat, il est rangé hors de `abstraction/` (réservé aux interfaces). Le JSON réseau
 * brut de GitHub est, lui, porté par `GitHubReleaseDto` côté infrastructure, qui est
 * *mappé* vers ce type — ce qui préserve la règle de dépendances (application ne dépend
 * pas d'infrastructure).
 *
 * @property version Tag de la release la plus récente (ex. `"v1.2.3"`).
 * @property releaseUrl URL de la page de release (repli si aucun installeur n'est joint).
 * @property downloadUrl URL de l'installeur à télécharger, ou `null` si absent.
 */
data class UpdateInfoDto(val version: String, val releaseUrl: String, val downloadUrl: String?)
