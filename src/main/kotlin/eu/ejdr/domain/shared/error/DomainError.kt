package eu.ejdr.domain.shared.error

/**
 * Contrat de base de toutes les erreurs métier du domaine.
 *
 * Volontairement NON `sealed` : les erreurs concrètes vivent par feature dans des
 * sous-packages (`domain/error/entities/<feature>/`), ce qui est incompatible avec
 * une hiérarchie `sealed` (qui exige le même package). L'exhaustivité est garantie
 * au niveau de chaque feature, où l'erreur est un `sealed class` (ex. [eu.ejdr.domain.features.auth.error.AuthError]).
 */
interface DomainError {
    val message: String
}
