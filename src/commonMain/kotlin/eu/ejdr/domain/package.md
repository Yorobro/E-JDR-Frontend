# Couche `domain`

Cœur métier de l'application : modélise les concepts et règles de l'E-JDR,
indépendamment de toute technologie (UI, réseau, persistance).

## Rôle

- Définir les **entités métier** et les **erreurs de domaine**.
- Servir de langage commun, stable, partagé par toutes les autres couches.

## Règle des entités pures

Les entités sont des **conteneurs de données purs** : aucune méthode métier, aucun
comportement. La logique applicative vit dans les use cases de la couche
`application`, jamais dans les entités elles-mêmes.

## Organisation

Le code est rangé en deux familles : `shared/` (transverse, non lié à une feature)
et `features/<feature>/` (tout ce qui appartient à une fonctionnalité).

- `shared/error/` — contrat transverse des erreurs : `DomainError` (interface, non
  `sealed`), implémenté par chaque erreur de feature.
- `features/<feature>/entities/` — entités métier de la feature
  (ex. `features/auth/entities/User`, `Credentials`).
- `features/<feature>/error/` — erreurs concrètes de la feature, en `sealed class`
  (ex. `features/auth/error/AuthError`), pour une exhaustivité garantie au niveau
  de chaque feature.

### Ajouter une feature au domaine

Créer `features/<feature>/` puis, selon les besoins, `entities/` (data classes
pures) et `error/<Feature>Error.kt` (`sealed class … : DomainError`). Ne rien
mettre dans `shared/` qui soit propre à une seule feature.

## Dépendances

`domain` est la couche **la plus interne** : elle ne dépend de **rien** (ni
framework, ni couche `application`/`infrastructure`/`presentation`). Ce sont les
couches externes qui dépendent du domaine, jamais l'inverse.
