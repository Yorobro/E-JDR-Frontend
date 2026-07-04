# Fiche de personnage — blocs délimités, types affinés & value object Purse

## Contexte

La fiche détaillée vient d'être ajoutée (21 champs, écran détail éditable, migration V006 appliquée localement — **rien n'est commité ni poussé**). À l'usage, deux besoins :

1. **Présentation fidèle à la fiche papier** : chaque section dans un **carré de délimitation** (carte bordée), avec une disposition en colonnes/paires plutôt qu'une longue pile verticale.
2. **Modèle plus juste** : certains champs ont un type inadapté (niveau/âge en texte), le sexe doit être contraint (M/F/NB), la **monnaie** doit devenir un vrai **value object** (PO/PA/PC avec règles de conversion), et **Compétences** — exclu jusqu'ici — doit être ajouté.

Comme la fiche n'est ni mergée ni poussée, on **refait la migration V006** avec les bons types (pas de V007).

## Décisions (validées avec l'utilisateur)

- Code en **anglais** (entités, VO) ; libellés UI en **français**.
- **Aucun lien dérivé** entre champs (pas de « PV = 10 + vigueur »). Tout optionnel sauf `name`.
- Stockage : **une colonne par champ** sur `character_sheets` (y compris la bourse, 1-1 avec la fiche).

## Modèle de données

### Champs (en plus de id / ownerId / name / createdAt)

**Bloc Identité**
| Champ | Type domaine | Colonne SQL | Notes |
|---|---|---|---|
| formation | `string \| null` | `formation VARCHAR(255)` | |
| niveau | `number \| null` | `niveau INT` | entier ≥ 0 (était texte) |
| peuple | `string \| null` | `peuple VARCHAR(255)` | |
| sexe | `Sex \| null` | `sexe VARCHAR(10)` | enum **M / F / NB**, validé back (VARCHAR(10) pour marge) |
| tailleEtPoids | `string \| null` | `taille_et_poids VARCHAR(255)` | texte |
| age | `number \| null` | `age INT` | entier ≥ 0 (était texte) |
| apparence | `string \| null` | `apparence TEXT` | texte long |

**Bloc Caractéristiques** — entiers ≥ 0 : `dexterite`, `intelligence`, `perception`, `social`, `vigueur` (INT NULL chacun).

**Bloc Combat** — entiers ≥ 0 : `pointsDeVie`, `pointsDeMagie`, `protection` (INT NULL chacun).

**Bloc Bourse (value object `Purse`)** — `purse_gold`, `purse_silver`, `purse_copper` (INT NULL). 1-1 avec la fiche via les colonnes (le « lien » = la ligne). Si les 3 colonnes sont `null` → pas de bourse ; sinon les `null` comptent comme 0.

**Champs texte** (chacun son carré) : `armures`, `armes`, `competences` (**nouveau**), `equipement`, `sortsEtMiracles`, `notes` — tous `TEXT NULL`.

### Value object `Purse` (backend)

`src/domain/features/character-sheet/value-objects/Purse.ts` — immuable, comme `CharacterSheetName` :

- Champs `gold`, `silver`, `copper` : entiers ≥ 0.
- Factory `Purse.create({ gold, silver, copper })` → valide (entiers, ≥ 0) sinon `InvalidPurseError` (→ 400). Valeurs absentes traitées comme 0.
- Constantes : `SILVER_PER_GOLD = 100`, `COPPER_PER_SILVER = 100` (donc 1 gold = 10 000 copper).
- `totalInCopper(): number` — total en cuivre.
- `normalized(): Purse` — recombine (ex. `{0,150,0}` → `{1,50,0}`). Utilisé **à l'affichage en lecture**.
- `equals(other)`.

### Value object / enum `Sex` (backend)

`src/domain/features/character-sheet/value-objects/Sex.ts` — `Sex.create(raw)` accepte `"M" | "F" | "NB"` (normalisé majuscule) sinon `InvalidSexError` (→ 400). Expose `value`.

### Entité `CharacterSheet`

`CharacterSheetDetails` ajuste : `niveau`/`age` → `number | null`, `sexe` → `Sex | null`, ajoute `competences: string | null` et `purse: Purse | null`. `withDetails` et le getter `details` inchangés en principe.

## Contrat API (JSON)

- `GET /character-sheets/:id` et `PUT` renvoient la fiche complète. La bourse voyage **imbriquée** : `"purse": { "gold": 1, "silver": 50, "copper": 0 } | null`. `sexe` = `"M" | "F" | "NB" | null`. `niveau`/`age` = `number | null`.
- `PUT /character-sheets/:id` (corps souple) : `name` requis ; `sexe` validé via `Sex.create` ; `purse` validé via `Purse.create` ; erreurs `INVALID_CHARACTER_SHEET_NAME`/`INVALID_SEX`/`INVALID_PURSE` → 400 (réutilise `InvalidInputError`, pas de nouveau code mapper si possible — sinon ajout au mapper).
- Liste (`GET /character-sheets`) reste **nom seul** (inchangée).

## Disposition visuelle (carrés de délimitation)

Composant réutilisable `SheetCard(title) { content }` : fond `surface`, bordure `border`, coins `radiusMd`, titre en haut. Chaque bloc = un `SheetCard`.

Agencement de la page détail (haut → bas) :

```
┌──────────────── Identité ────────────────┐
│ Nom · Formation · Niveau(int)            │
│ Peuple · Sexe(M/F/NB) · Taille/poids · Âge(int) │
│ Apparence (texte long, pleine largeur)   │
└──────────────────────────────────────────┘
┌─ Caractéristiques ─┐ ┌─── Combat ───┐ ┌─── Bourse ───┐
│ Dextérité          │ │ Points de vie│ │ PO  PA  PC   │
│ Intelligence       │ │ Points de magie│ (normalisé   │
│ Perception         │ │ Protection   │ │  en lecture) │
│ Social · Vigueur   │ │              │ │              │
└────────────────────┘ └──────────────┘ └──────────────┘
┌─ Armures ─┐ ┌─ Armes ─┐          (paire 50/50)
┌─ Compétences ─┐ ┌─ Équipement ─┐  (paire 50/50)
┌─ Sorts & Miracles ─┐ ┌─ Notes ─┐ (paire 50/50)
```

- Rangée **Caractéristiques / Combat / Bourse** : 3 colonnes côte à côte, **repli en pile** sur fenêtre étroite (`ResponsiveColumns` existant, seuil ~720 dp).
- Champs texte : **3 rangées de 2 carrés** (50/50) — Armures·Armes, Compétences·Équipement, Sorts&Miracles·Notes — repli en pile si étroit.
- **Même grille en lecture et en édition**. En édition : cellules → champs ; `sexe` → menu déroulant ; bourse → 3 champs entiers.

## Architecture / découpage

### Backend
- VO `Purse` + `InvalidPurseError` ; VO `Sex` + `InvalidSexError`.
- Refaire `db/migrations/V006__add_character_sheet_details.sql` avec les bons types (niveau/age INT, sexe VARCHAR(10), competences TEXT, purse_gold/silver/copper INT).
- `CharacterSheetDetails` (types ajustés + purse + sexe + competences) ; DAO `CharacterSheetWriteRow`/`CharacterSheetRow` + listes de colonnes ; Mapper `toRow`/`toDomain` reconstruisent `Purse`/`Sex`.
- `CharacterSheetDetail` (DTO lecture) : `niveau`/`age` number, `sexe` string, `competences`, `purse: { gold, silver, copper } | null`. `UpdateCharacterSheetCommand` idem en entrée.
- Use case Update : `Sex.create` + `Purse.create` (catch DomainError → `InvalidInputError`).
- Controller : coercion du corps (purse objet → command), réponse imbriquée.
- Tests : VO `Purse` (conversion, normalisation, rejets ≥0/entier), VO `Sex`, MAJ entité/DAO(V006)/intégration routes.

### Frontend (anémique)
- `CharacterSheet` : `niveau: Int?`, `age: Int?`, `sexe: String?`, `competences: String?`, `purse: Purse?` où `data class Purse(gold: Int, silver: Int, copper: Int)`.
- DTO `CharacterSheetDto` + `UpdateCharacterSheetRequestDto` : champs ajustés + `purse: PurseDto? = null` (imbriqué). Mapper étendu.
- Atomes/molécules : `SheetCard` (carré bordé) ; `AppDropdown` (nouvel atome menu déroulant, pour le sexe) ; petite fonction utilitaire de **normalisation Purse** pour l'affichage lecture.
- `CharacterSheetFormState` : niveau/age/purse en String (saisie), `sexe` sélection ; `toCharacterSheet()` reconstruit les types. Sections réorganisées (Identité / Caractéristiques / Combat / Bourse / paires texte) dans des `SheetCard`.
- Tests : VM/HTTP repo (cas purse/sexe, régression liste nom-seul conservée).

## Vérification de bout en bout
1. Back : `lint` + `test` verts ; `test:db` (Docker) valide la nouvelle V006 + colonnes purse.
2. Front : `./gradlew verify` vert (compile + detekt + Kover + tests).
3. Runtime : refaire la migration (`migrate:up` après reset/redo V006), `./gradlew run` → ouvrir une fiche → vérifier les **carrés**, la disposition en paires, le repli responsive ; **Modifier** → sexe en menu, bourse 3 champs → **Enregistrer** → rouvrir → valeurs persistées et bourse **normalisée** en lecture.

## Risques
- **Refaire V006** : la base locale a déjà l'ancienne V006 appliquée → il faudra `migrate:down` (ou drop des colonnes/recréation) avant de réappliquer la nouvelle. À gérer dans le plan.
- **Changement de type** niveau/age : l'ancienne colonne était VARCHAR ; la base est quasi vide (données de test) → conversion/drop sans risque.
- **detekt LongMethod** : la page va grossir → garder l'extraction en sections/cartes.
- **Contrat purse imbriqué** : front et back doivent s'accorder sur la forme `purse: {gold,silver,copper}` et le `null`.
