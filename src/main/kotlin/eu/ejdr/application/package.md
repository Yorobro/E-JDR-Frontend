# Couche Application

La couche **application** orchestre les cas d'usage métier de l'application. Elle
fait le lien entre la présentation et le domaine, sans contenir de détails
techniques (HTTP, persistance, UI). Elle s'appuie uniquement sur des abstractions
(ports) implémentées par l'infrastructure.

## Organisation : `shared/` + `features/`

Le code est rangé en deux familles :

- `shared/` — éléments transverses, non liés à une feature (ex. `shared/Result.kt`,
  le type de retour railway-oriented commun à tous les use cases).
- `features/<feature>/` — tout ce qui appartient à une fonctionnalité (`auth`,
  `settings`, `update`, …).

Chaque `features/<feature>/` regroupe :

- `abstraction/repository/` — ports d'accès aux données (ex. `AuthRepository`)
- `abstraction/service/` — contrats des services réutilisables (ex. `SessionService`)
- `abstraction/usecase/` — contrats des use cases (`fun interface` à `operator invoke`)
- `service/` — implémentations des services
- `usecase/` — implémentations des use cases

### Ajouter une feature applicative

Créer `features/<feature>/` avec le sous-dossier `abstraction/` (ports) et les
implémentations (`usecase/`, `service/`). Déclarer les use cases en
`fun interface … { suspend operator fun invoke(…): Result<…> }`, puis enregistrer
l'implémentation dans `di/ApplicationModule.kt`. Les ports d'infrastructure
nécessaires (repositories) sont liés dans `di/InfrastructureModule.kt`.

## Règles

- **Un use case = orchestration uniquement.** Il combine repositories et services,
  mais **n'appelle jamais un autre use case**.
- **Un service = logique réutilisable.** Il peut utiliser des repositories et
  d'autres services, et il est partagé entre plusieurs use cases (ex. la
  restauration de session est centralisée dans `SessionService`).
- **Retour `Result<T, DomainError>`.** Les use cases renvoient toujours un `Result` ;
  aucune exception ne remonte jusqu'à la présentation.

## Sens de dépendance

`application → domain` uniquement. La couche application dépend du domaine (entités,
erreurs), jamais de l'infrastructure ni de la présentation. L'infrastructure
implémente les ports définis ici par inversion de dépendance.
