# Couche `domain`

Cœur métier de l'application : modélise les concepts et règles de l'E-JDR,
indépendamment de toute technologie (UI, réseau, persistance).

## Rôle

- Définir les **entités métier** et les **erreurs de domaine**.
- Servir de langage commun, stable, partagé par toutes les autres couches.

## Règle des entités pures

Les entités (`entities/`) sont des **conteneurs de données purs** : aucune méthode
métier, aucun comportement. La logique applicative vit dans les use cases de la
couche `application`, jamais dans les entités elles-mêmes.

## Organisation

- `entities/` — entités métier (ex. `entities/auth/User`, `Credentials`).
- `error/` — contrat des erreurs : `DomainError` (interface, non `sealed`).
- `error/entities/<feature>/` — erreurs concrètes par feature, en `sealed class`
  (ex. `error/entities/auth/AuthError`), pour une exhaustivité garantie au niveau
  de chaque feature.

## Dépendances

`domain` est la couche **la plus interne** : elle ne dépend de **rien** (ni
framework, ni couche `application`/`infrastructure`/`presentation`). Ce sont les
couches externes qui dépendent du domaine, jamais l'inverse.
