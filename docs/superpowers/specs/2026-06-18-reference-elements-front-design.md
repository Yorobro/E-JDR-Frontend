# Design — Éléments de référence (Front)

## Context

Le **backend** (branche `feat/reference-elements`) a remplacé les champs texte libres de la fiche
(`formation`, `peuple`, `armes`, `armures`, `competences`, `equipement`) par des **catalogues
d'éléments créés et possédés par l'utilisateur** :
- **N‑1** : la fiche porte `formationId` / `peupleId` (id de référence, nullable).
- **N‑N** : armes/armures/compétences/équipements sont liés via `/character-sheets/:id/:type`.
- Catalogue CRUD sous `/reference/:type` (`type` ∈ formations|peoples|armes|armures|competences|equipements).

Ce front doit (1) **s'aligner** sur ce nouveau contrat (sinon les fiches ne se chargent plus) et
(2) offrir l'UI : dropdowns formation/peuple, sélection N‑N des armes/etc. sur la fiche, et des
**écrans de gestion** du catalogue. Front Kotlin/Compose, domaine **anémique**, Koin par feature,
Nav3, Result railway.

Décisions validées :
- **Écrans dédiés** « Mes éléments » : hub sur l'accueil → liste des 6 types → liste d'un type (créer/supprimer).
- **N‑N via dialog de sélection** (cases à cocher du catalogue, clone de `LinkCharacterDialog`) ;
  éléments liés affichés en cartes avec retrait. Pas de composant multi-select à inventer.
- **N‑1** : l'écran détail de fiche charge les catalogues formations/peuples pour peupler les
  dropdowns et résoudre id→nom **côté front** (le back ne renvoie que l'id ; aucune re-touche back).
- **Structure générique** : un écran/ViewModel de gestion **paramétré par type** (route Nav3 portant
  le type), à l'image du controller générique du back.

---

## 1. Data layer — `features/reference/`

### Domaine — `domain/features/reference/`
- `entities/ReferenceItem.kt` : `data class ReferenceItem(id, name, createdAt)` (anémique).
- `error/ReferenceError.kt` : sealed (`InvalidName`, `NotFound`, `NameAlreadyUsed`, `AccessDenied`,
  `Network`, `Unknown(detail)`), clone de `CampaignError`.
- `entities/ReferenceType.kt` : enum `ReferenceType { FORMATION, PEUPLE, ARME, ARMURE, COMPETENCE, EQUIPEMENT }`
  avec `slug` (le segment d'URL : `formations`, `peoples`, …) et `label` FR (« Formations », …).
  Source unique de vérité du mapping type↔slug↔libellé, côté front.

### Application — `application/features/reference/`
- `abstraction/repository/ReferenceRepository.kt` :
  - Catalogue : `list(type)`, `create(type, name)`, `delete(type, itemId)`.
  - Liaison N‑N : `listLinked(sheetId, type)`, `link(sheetId, type, itemId)`, `unlink(sheetId, type, itemId)`.
  - Tous → `Result<…, ReferenceError>`. Le `type` est un `ReferenceType` (le repo HTTP utilise son `slug`).
- `abstraction/usecase/` + `usecase/*Impl` : `ListReferenceItemsUseCase`, `CreateReferenceItemUseCase`,
  `DeleteReferenceItemUseCase`, `ListSheetReferencesUseCase`, `LinkSheetReferenceUseCase`,
  `UnlinkSheetReferenceUseCase` (délégations triviales, regroupées dans `ReferenceUseCaseImpls.kt`).

### Infrastructure — `infrastructure/http/features/reference/`
- `dto/ReferenceDtos.kt` : `ReferenceItemDto(id, name, createdAt)`, `CreateReferenceRequestDto(name)`,
  `ReferenceListResponseDto(items)`, `LinkReferenceRequestDto(itemId)`.
- `ReferenceHttpMapper.kt` : `toItem(dto)` + `toError(status, code, message)` (codes
  `INVALID_REFERENCE_NAME`/`REFERENCE_NAME_ALREADY_USED`/`REFERENCE_ITEM_NOT_FOUND`/`CHARACTER_SHEET_ACCESS_DENIED`).
- `ReferenceHttpRepository.kt` : appelle `/reference/{slug}` et `/character-sheets/{id}/{slug}`,
  `runCatchingCancellable { … }.getOrElse { Result.Failure(ReferenceError.Network) }`. Clone de `CampaignHttpRepository`.

### DI — `di/ReferenceModule.kt`
- `single<ReferenceRepository> { ReferenceHttpRepository(get(), get()) }` + les 6 use cases.
- Ajouter `referenceModule` à `di/AppKoin.kt`.

---

## 2. Alignement du modèle fiche (obligatoire — sinon la fiche ne charge plus)

- `domain/features/charactersheet/entities/CharacterSheet.kt` : remplacer `formation`/`peuple`
  (String?) par `formationId`/`peupleId` (String?) ; **retirer** `armes`/`armures`/`competences`/`equipement`.
- `dto/CharacterSheetDtos.kt` : idem sur `CharacterSheetDto` et `UpdateCharacterSheetRequestDto`
  (`formationId`/`peupleId`, drop des 4 champs N‑N).
- `CharacterSheetHttpMapper.kt` : refléter le renommage / la suppression.
- `component/CharacterSheetFormState.kt` : `formationId`/`peupleId` (String mutable), retrait des
  4 champs N‑N ; `toCharacterSheet()` adapté.
- `component/CharacterSheetSections.kt` : la cellule Formation/Peuple devient un `AppDropdown`
  (options = noms du catalogue, sélection → id) ; les sections Armes/Armures (onglet Combat) et
  Compétences/Équipement (onglet Inventaire) passent du `LongTextBody` à une **section N‑N**
  (liste de cartes + bouton « Ajouter » ouvrant le dialog de sélection).

> Les ViewModels/use cases de fiche **n'ont pas besoin** de connaître les noms : la résolution
> id→nom se fait dans la couche présentation à partir des catalogues chargés.

---

## 3. UI fiche — détail

`CharacterSheetDetailViewModel` (présentation) :
- Charge en plus : les **catalogues** `formations` et `peoples` (pour les dropdowns) et les
  **listes liées** des 4 types N‑N de la fiche (`listLinked(sheetId, type)`).
- Expose : `formations: List<ReferenceItem>`, `peoples: List<ReferenceItem>`, et pour chaque type
  N‑N la liste liée + le catalogue (pour le dialog). Actions `linkRef(type, itemId)` /
  `unlinkRef(type, itemId)` (rechargent la liste liée). La sauvegarde de la fiche (formationId/peupleId
  + champs scalaires) reste via l'update existant.
- Détail UI :
  - **Formation/Peuple** : `AppDropdown` (valeur = nom résolu depuis l'id ; `onSelect` → met à jour
    l'id dans le form-state ; inclut une option « — » pour vider).
  - **Armes/Armures/Compétences/Équipements** : par type, une liste de cartes (clone léger de
    `CharacterSheetCard` : nom + croix de retrait) + bouton « Ajouter » → dialog de sélection.
- `component/ReferencePickerDialog.kt` (nouveau, clone de `LinkCharacterDialog`) : liste les éléments
  du catalogue **non encore liés**, sélection → `link`. Si le catalogue est vide, message + lien
  implicite « créez-en dans Mes éléments ».
- `component/LinkedReferenceList.kt` (nouveau) : cartes des éléments liés avec retrait.

---

## 4. UI gestion — « Mes éléments »

- `navigation/Routes.kt` : `data object ReferenceHub : Route` et
  `data class ReferenceList(val type: String) : Route` (type = slug, sérialisable). Enregistrer dans `appNavConfiguration`.
- `presentation/features/reference/ReferenceNavEntries.kt` : `entry<Route.ReferenceHub>` (hub) et
  `entry<Route.ReferenceList>` (liste générique), agrégés dans `AppNavDisplay`.
- `page/ReferenceHubPage.kt` : grille de 6 tuiles (une par `ReferenceType.label`) → ouvre `ReferenceList(type.slug)`.
- `ReferenceListViewModel.kt` (générique, prend un `ReferenceType`) : `items`, `isLoading`, `error` ;
  `load/create/delete` (clone de `CampaignListViewModel`).
- `page/ReferenceListPage.kt` (générique) : grille de cartes + `AppFab` (créer) + `CreateReferenceDialog`
  + `ConfirmDeleteDialog`. Réutilise les atomes/organismes existants.
- Accès : un bouton « Mes éléments » sur l'accueil (`UserPage` / `AppTopBar`) → `Route.ReferenceHub`.

---

## 5. Tests + couverture (Kover ≥ 60 %)

- **Comptés** : `ReferenceHttpRepository` + `ReferenceHttpMapper` (clone `CampaignHttpRepositoryTest`),
  les use case impls, `ReferenceListViewModel`, et les ajouts à `CharacterSheetDetailViewModel`
  (chargement catalogues + link/unlink).
- **Exclus de Kover** (UI, comme l'existant) : `presentation.features.reference.page`/`.component`,
  `ReferenceNavEntriesKt`. Ajouter ces exclusions dans `build.gradle.kts`.

---

## Risques / points d'attention
- **Rupture du modèle fiche** : si on ne change pas DTO/entité/mapper/form-state ensemble, le
  chargement de fiche casse. À faire en un lot cohérent.
- **Résolution id→nom** : un id pointant un élément supprimé (back met à null en N‑1) → le dropdown
  affichera « — » ; gérer l'id absent du catalogue sans planter.
- **`gradlew verify`** doit rester vert (detekt + tests + kover + build). Les nouveaux écrans UI
  exclus de Kover ; la logique (repo/mapper/use cases/VM) couverte.
- Pas de date-picker ni de multi-select natif : on reste dans le design system existant
  (`AppDropdown`, `AppDialog`, `AppCheckbox`, `AppFab`, cartes).

## Vérification
- `./gradlew verify` vert (detekt + tests + koverVerify ≥ 60 % + build).
- **Bout en bout** (back local lancé) : créer une formation dans « Mes éléments » → l'affecter via le
  dropdown de la fiche → recharger la fiche, le dropdown montre le bon nom ; ajouter une arme via le
  dialog → elle apparaît en carte → la retirer ; vérifier qu'un catalogue vide affiche l'état vide.
