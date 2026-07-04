# Fiche — blocs délimités, types affinés & value object Purse — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Affiner le modèle de la fiche (niveau/âge en entiers, sexe enum M/F/NB, monnaie = value object Purse PO/PA/PC, ajout Compétences) et présenter l'écran détail en blocs bordés disposés comme la fiche papier.

**Architecture:** Backend clean/hexagonal (refait la migration V006, ajoute les VO `Purse`/`Sex`, étend entité/DAO/mapper/use cases/controller). Frontend anémique Compose (DTO/mapper/repo inchangés en structure ; refonte UI en cartes `SheetCard` + grille responsive + atome `AppDropdown`). Contrat JSON : `purse` imbriqué `{gold,silver,copper}|null`, `sexe` string M/F/NB, niveau/age number.

**Tech Stack:** Node 22 / TS / Express / MySQL (umzug, Vitest, Testcontainers) ; Kotlin 2.2.20 / Compose MP / Koin / Ktor / kotlinx.serialization / detekt / Kover.

**Préalable au runtime :** la migration V006 actuelle est déjà appliquée en base locale. Avant de réappliquer la nouvelle V006 : `npm run migrate:down` (annule l'ancienne V006), puis `npm run migrate:up`. Les commits utilisent un **sujet en minuscules** (commitlint).

---

## Convention de champs (référence)

21 → 24 champs détaillés. Types finaux :
- **Texte court** (VARCHAR 255) : formation, peuple, tailleEtPoids
- **Entiers** (INT) : niveau, age, dexterite, intelligence, perception, social, vigueur, pointsDeVie, pointsDeMagie, protection, purse_gold, purse_silver, purse_copper
- **Sexe** (VARCHAR 10, enum M/F/NB)
- **Texte long** (TEXT) : apparence, armures, armes, competences, equipement, sortsEtMiracles, notes

---

# PARTIE A — BACKEND

## Task A1 — Value object `Purse` + erreur

**Files:**
- Create: `src/domain/features/character-sheet/errors/InvalidPurseError.ts`
- Create: `src/domain/features/character-sheet/value-objects/Purse.ts`
- Test: `tests/domain/Purse.test.ts`

- [ ] **Step 1 — Écrire le test (échoue)** `tests/domain/Purse.test.ts`

```ts
import { describe, it, expect } from "vitest";
import { Purse } from "@domain/features/character-sheet/value-objects/Purse";
import { InvalidPurseError } from "@domain/features/character-sheet/errors/InvalidPurseError";

describe("Purse (value object)", () => {
  it("crée une bourse valide et expose ses pièces", () => {
    const p = Purse.create({ gold: 1, silver: 50, copper: 30 });
    expect(p.gold).toBe(1);
    expect(p.silver).toBe(50);
    expect(p.copper).toBe(30);
  });

  it("traite les valeurs absentes comme 0", () => {
    const p = Purse.create({});
    expect(p.gold).toBe(0);
    expect(p.silver).toBe(0);
    expect(p.copper).toBe(0);
  });

  it("totalInCopper applique 1 gold = 100 silver = 10000 copper", () => {
    expect(Purse.create({ gold: 1 }).totalInCopper()).toBe(10000);
    expect(Purse.create({ silver: 1 }).totalInCopper()).toBe(100);
    expect(Purse.create({ gold: 1, silver: 50, copper: 30 }).totalInCopper()).toBe(15030);
  });

  it("normalized recombine en forme canonique", () => {
    const n = Purse.create({ gold: 0, silver: 150, copper: 0 }).normalized();
    expect(n.gold).toBe(1);
    expect(n.silver).toBe(50);
    expect(n.copper).toBe(0);
  });

  it("rejette un entier négatif", () => {
    expect(() => Purse.create({ gold: -1 })).toThrow(InvalidPurseError);
  });

  it("rejette une valeur non entière", () => {
    expect(() => Purse.create({ silver: 1.5 })).toThrow(InvalidPurseError);
  });
});
```

- [ ] **Step 2 — Lancer le test (échoue)** : `npx vitest run tests/domain/Purse.test.ts` → FAIL (module introuvable).

- [ ] **Step 3 — Implémenter l'erreur** `src/domain/features/character-sheet/errors/InvalidPurseError.ts`

```ts
import { DomainError } from "@domain/shared/errors/DomainError";

/**
 * Erreur domaine levée lorsqu'une bourse (or/argent/cuivre) viole ses règles
 * (valeur négative ou non entière). Émise par {@link Purse} lors de sa construction.
 */
export class InvalidPurseError extends DomainError {
  /**
   * @param reason - La raison précise de l'invalidité.
   */
  constructor(reason: string) {
    super("INVALID_PURSE", `La bourse est invalide : ${reason}.`);
  }
}
```

- [ ] **Step 4 — Implémenter le VO** `src/domain/features/character-sheet/value-objects/Purse.ts`

```ts
import { InvalidPurseError } from "@domain/features/character-sheet/errors/InvalidPurseError";

/** Pièces non négatives composant une bourse. */
export interface PurseCoins {
  readonly gold?: number;
  readonly silver?: number;
  readonly copper?: number;
}

/**
 * Value object représentant la **bourse** d'un personnage : pièces d'or (gold), d'argent
 * (silver) et de cuivre (copper). Immuable. Règles : 1 gold = 100 silver, 1 silver = 100 copper
 * (donc 1 gold = 10 000 copper). Chaque montant est un entier ≥ 0.
 */
export class Purse {
  /** Pièces d'argent par pièce d'or. */
  public static readonly SILVER_PER_GOLD = 100;
  /** Pièces de cuivre par pièce d'argent. */
  public static readonly COPPER_PER_SILVER = 100;

  private constructor(
    public readonly gold: number,
    public readonly silver: number,
    public readonly copper: number,
  ) {}

  /**
   * Crée une bourse après validation. Les montants absents valent 0.
   *
   * @param coins - Montants bruts (or/argent/cuivre).
   * @throws {InvalidPurseError} Si un montant est négatif ou non entier.
   */
  public static create(coins: PurseCoins): Purse {
    const gold = Purse.validate(coins.gold ?? 0, "or");
    const silver = Purse.validate(coins.silver ?? 0, "argent");
    const copper = Purse.validate(coins.copper ?? 0, "cuivre");
    return new Purse(gold, silver, copper);
  }

  private static validate(value: number, label: string): number {
    if (!Number.isInteger(value) || value < 0) {
      throw new InvalidPurseError(`le montant de ${label} doit être un entier positif ou nul`);
    }
    return value;
  }

  /** @returns La valeur totale de la bourse, exprimée en pièces de cuivre. */
  public totalInCopper(): number {
    return (
      this.gold * Purse.SILVER_PER_GOLD * Purse.COPPER_PER_SILVER +
      this.silver * Purse.COPPER_PER_SILVER +
      this.copper
    );
  }

  /** @returns Une bourse équivalente sous forme canonique (cuivre/argent recombinés). */
  public normalized(): Purse {
    let total = this.totalInCopper();
    const gold = Math.floor(total / (Purse.SILVER_PER_GOLD * Purse.COPPER_PER_SILVER));
    total -= gold * Purse.SILVER_PER_GOLD * Purse.COPPER_PER_SILVER;
    const silver = Math.floor(total / Purse.COPPER_PER_SILVER);
    const copper = total - silver * Purse.COPPER_PER_SILVER;
    return new Purse(gold, silver, copper);
  }

  /**
   * @param other - Une autre bourse.
   * @returns `true` si les deux ont la même valeur totale.
   */
  public equals(other: Purse): boolean {
    return this.totalInCopper() === other.totalInCopper();
  }
}
```

- [ ] **Step 5 — Lancer le test (passe)** : `npx vitest run tests/domain/Purse.test.ts` → PASS.

- [ ] **Step 6 — Commit**

```bash
git add src/domain/features/character-sheet/value-objects/Purse.ts src/domain/features/character-sheet/errors/InvalidPurseError.ts tests/domain/Purse.test.ts
git commit -m "feat: add purse value object with gold/silver/copper rules"
```

---

## Task A2 — Value object `Sex` + erreur

**Files:**
- Create: `src/domain/features/character-sheet/errors/InvalidSexError.ts`
- Create: `src/domain/features/character-sheet/value-objects/Sex.ts`
- Test: `tests/domain/Sex.test.ts`

- [ ] **Step 1 — Écrire le test** `tests/domain/Sex.test.ts`

```ts
import { describe, it, expect } from "vitest";
import { Sex } from "@domain/features/character-sheet/value-objects/Sex";
import { InvalidSexError } from "@domain/features/character-sheet/errors/InvalidSexError";

describe("Sex (value object)", () => {
  it("accepte M, F, NB", () => {
    expect(Sex.create("M").value).toBe("M");
    expect(Sex.create("F").value).toBe("F");
    expect(Sex.create("NB").value).toBe("NB");
  });

  it("normalise la casse et les espaces", () => {
    expect(Sex.create(" m ").value).toBe("M");
    expect(Sex.create("nb").value).toBe("NB");
  });

  it("rejette une valeur hors M/F/NB", () => {
    expect(() => Sex.create("X")).toThrow(InvalidSexError);
  });
});
```

- [ ] **Step 2 — Lancer (échoue)** : `npx vitest run tests/domain/Sex.test.ts` → FAIL.

- [ ] **Step 3 — Erreur** `src/domain/features/character-sheet/errors/InvalidSexError.ts`

```ts
import { DomainError } from "@domain/shared/errors/DomainError";

/** Erreur domaine levée lorsqu'un sexe n'est pas l'un de M/F/NB. Émise par {@link Sex}. */
export class InvalidSexError extends DomainError {
  constructor(reason: string) {
    super("INVALID_SEX", `Le sexe est invalide : ${reason}.`);
  }
}
```

- [ ] **Step 4 — VO** `src/domain/features/character-sheet/value-objects/Sex.ts`

```ts
import { InvalidSexError } from "@domain/features/character-sheet/errors/InvalidSexError";

/** Valeurs autorisées pour le sexe d'un personnage. */
export type SexValue = "M" | "F" | "NB";

const ALLOWED: readonly SexValue[] = ["M", "F", "NB"];

/**
 * Value object représentant le **sexe** d'un personnage, contraint à M, F ou NB. Immuable.
 * Normalise la casse et les espaces de bord.
 */
export class Sex {
  private constructor(public readonly value: SexValue) {}

  /**
   * @param raw - Valeur brute saisie.
   * @throws {InvalidSexError} Si la valeur normalisée n'est pas M/F/NB.
   */
  public static create(raw: string): Sex {
    const normalized = (raw ?? "").trim().toUpperCase();
    if (!ALLOWED.includes(normalized as SexValue)) {
      throw new InvalidSexError(`valeur « ${raw} » non autorisée (attendu M, F ou NB)`);
    }
    return new Sex(normalized as SexValue);
  }
}
```

- [ ] **Step 5 — Lancer (passe)** : `npx vitest run tests/domain/Sex.test.ts` → PASS.

- [ ] **Step 6 — Commit**

```bash
git add src/domain/features/character-sheet/value-objects/Sex.ts src/domain/features/character-sheet/errors/InvalidSexError.ts tests/domain/Sex.test.ts
git commit -m "feat: add sex value object constrained to m/f/nb"
```

---

## Task A3 — Étendre l'entité `CharacterSheet` (types + purse + sex + competences)

**Files:**
- Modify: `src/domain/features/character-sheet/entities/CharacterSheet.ts`
- Test: `tests/domain/CharacterSheet.test.ts`

Le `CharacterSheetDetails` actuel a `niveau: string|null`, `age: string|null`, `sexe: string|null`, `monnaie: number|null`. On change : `niveau: number|null`, `age: number|null`, `sexe: Sex|null`, on retire `monnaie`, on ajoute `purse: Purse|null` et `competences: string|null`. Le reste des champs (formation, peuple, tailleEtPoids, apparence, 5 stats, pointsDeVie, pointsDeMagie, protection, armes, armures, equipement, sortsEtMiracles, notes) inchangé.

- [ ] **Step 1 — Mettre à jour le test** `tests/domain/CharacterSheet.test.ts` : remplacer le test `create accepte des champs détaillés optionnels` et ajouter purse/sex/competences.

```ts
  it("create accepte des champs détaillés optionnels", () => {
    const sheet = CharacterSheet.create({
      id: "sheet-1",
      ownerId: "user-1",
      name: CharacterSheetName.create("Aragorn"),
      createdAt: new Date("2026-01-01T00:00:00Z"),
      peuple: "Dúnedain",
      niveau: 5,
      age: 87,
      sexe: Sex.create("M"),
      vigueur: 6,
      competences: "Pistage",
      purse: Purse.create({ gold: 2 }),
    });
    expect(sheet.details.peuple).toBe("Dúnedain");
    expect(sheet.details.niveau).toBe(5);
    expect(sheet.details.age).toBe(87);
    expect(sheet.details.sexe?.value).toBe("M");
    expect(sheet.details.vigueur).toBe(6);
    expect(sheet.details.competences).toBe("Pistage");
    expect(sheet.details.purse?.gold).toBe(2);
    expect(sheet.details.formation).toBeNull();
  });
```

Ajouter en tête du fichier : `import { Sex } from "@domain/features/character-sheet/value-objects/Sex";` et `import { Purse } from "@domain/features/character-sheet/value-objects/Purse";`.

- [ ] **Step 2 — Lancer (échoue)** : `npx vitest run tests/domain/CharacterSheet.test.ts` → FAIL (type/champ).

- [ ] **Step 3 — Modifier l'entité** : dans `CharacterSheet.ts`, ajouter les imports `Sex` et `Purse`, puis dans `CharacterSheetDetails` :
  - remplacer `readonly niveau: string | null;` par `readonly niveau: number | null;`
  - remplacer `readonly age: string | null;` par `readonly age: number | null;`
  - remplacer `readonly sexe: string | null;` par `readonly sexe: Sex | null;`
  - supprimer `readonly monnaie: number | null;`
  - ajouter `readonly competences: string | null;` (à côté des textes longs)
  - ajouter `readonly purse: Purse | null;`

Et dans `EMPTY_DETAILS` : `niveau: null, age: null, sexe: null` restent, retirer `monnaie: null`, ajouter `competences: null, purse: null`.

- [ ] **Step 4 — Lancer (passe)** : `npx vitest run tests/domain/CharacterSheet.test.ts` → PASS.

- [ ] **Step 5 — Commit**

```bash
git add src/domain/features/character-sheet/entities/CharacterSheet.ts tests/domain/CharacterSheet.test.ts
git commit -m "feat: model niveau/age as integers, sexe as vo, add purse and competences"
```

---

## Task A4 — Refaire la migration V006

**Files:**
- Modify: `db/migrations/V006__add_character_sheet_details.sql`

**Préalable :** la V006 actuelle est appliquée en base locale. L'agent exécutant devra (hors plan de code) faire `npm run migrate:down` une fois pour annuler l'ancienne V006 AVANT d'appliquer la nouvelle. La V006 n'a pas de down (forward-only) → si `migrate:down` échoue, drop manuel des 21 colonnes ou recréation de la base de dev.

- [ ] **Step 1 — Réécrire le fichier** `db/migrations/V006__add_character_sheet_details.sql`

```sql
-- Migration V006 — Ajout des champs détaillés d'une fiche de personnage.
--
-- Sections :
--   * Identité   : formation, niveau (int), peuple, sexe (M/F/NB), taille/poids, age (int), apparence.
--   * Caractéristiques (int) : dextérité, intelligence, perception, social, vigueur.
--   * Combat (int)           : points de vie, points de magie, protection.
--   * Bourse (int)           : purse_gold, purse_silver, purse_copper (value object Purse).
--   * Textes longs           : armures, armes, competences, equipement, sorts & miracles, notes.
--
-- Tous les champs sont NULLables (saisie souple ; seul le nom est requis). Aucune règle métier
-- portée par le schéma : la validation (sexe M/F/NB, bourse ≥ 0, etc.) vit dans le domaine.
--
-- ⚠️ Numéro V006 à coordonner avec l'équipe avant merge (cf. db/MIGRATION.md).

ALTER TABLE character_sheets
    -- Identité
    ADD COLUMN formation        VARCHAR(255) NULL,
    ADD COLUMN niveau           INT NULL,
    ADD COLUMN peuple           VARCHAR(255) NULL,
    ADD COLUMN sexe             VARCHAR(10) NULL,
    ADD COLUMN taille_et_poids  VARCHAR(255) NULL,
    ADD COLUMN age              INT NULL,
    ADD COLUMN apparence        TEXT NULL,
    -- Caractéristiques
    ADD COLUMN dexterite        INT NULL,
    ADD COLUMN intelligence     INT NULL,
    ADD COLUMN perception       INT NULL,
    ADD COLUMN social           INT NULL,
    ADD COLUMN vigueur          INT NULL,
    -- Combat
    ADD COLUMN points_de_vie    INT NULL,
    ADD COLUMN points_de_magie  INT NULL,
    ADD COLUMN protection       INT NULL,
    -- Bourse (value object Purse)
    ADD COLUMN purse_gold       INT NULL,
    ADD COLUMN purse_silver     INT NULL,
    ADD COLUMN purse_copper     INT NULL,
    -- Textes longs
    ADD COLUMN armures          TEXT NULL,
    ADD COLUMN armes            TEXT NULL,
    ADD COLUMN competences      TEXT NULL,
    ADD COLUMN equipement       TEXT NULL,
    ADD COLUMN sorts_et_miracles TEXT NULL,
    ADD COLUMN notes            TEXT NULL;
```

- [ ] **Step 2 — Commit**

```bash
git add db/migrations/V006__add_character_sheet_details.sql
git commit -m "feat: redo v006 with integer niveau/age, sexe, competences and purse columns"
```

---

## Task A5 — DAO : colonnes & write-row

**Files:**
- Modify: `src/infrastructure/persistence/mysql/features/character-sheet/dao/CharacterSheetDao.ts`

Le DAO actuel a `CharacterSheetRow`/`CharacterSheetWriteRow` avec les anciens types (niveau/age string, monnaie). On ajuste : `niveau`/`age` deviennent `number|null`, `sexe` reste `string|null` (le mapper traduit vers/depuis le VO), retirer `monnaie`, ajouter `competences`, `purse_gold`, `purse_silver`, `purse_copper`. Mettre à jour les deux interfaces ET la constante `DETAIL_COLUMNS`.

- [ ] **Step 1 — Modifier `CharacterSheetRow`** : changer `niveau?: string | null` → `niveau?: number | null` ; `age?: string | null` → `age?: number | null` ; retirer `monnaie?` ; ajouter (optionnels, comme les autres) :
```ts
  competences?: string | null;
  purse_gold?: number | null;
  purse_silver?: number | null;
  purse_copper?: number | null;
```

- [ ] **Step 2 — Modifier `CharacterSheetWriteRow`** : `niveau: number | null` ; `age: number | null` ; retirer `monnaie` ; ajouter `competences: string | null; purse_gold: number | null; purse_silver: number | null; purse_copper: number | null;`.

- [ ] **Step 3 — Mettre à jour `DETAIL_COLUMNS`** (ordre stable) pour refléter EXACTEMENT les colonnes V006 :
```ts
const DETAIL_COLUMNS = [
  "formation", "niveau", "peuple", "sexe", "taille_et_poids", "age", "apparence",
  "dexterite", "intelligence", "perception", "social", "vigueur",
  "points_de_vie", "points_de_magie", "protection",
  "purse_gold", "purse_silver", "purse_copper",
  "armures", "armes", "competences", "equipement", "sorts_et_miracles", "notes",
] as const;
```

- [ ] **Step 4 — Vérifier le typecheck** : `npx tsc -p tsconfig.json --noEmit` → des erreurs attendues dans le Mapper (Task A6) ; le DAO seul doit être cohérent. (On commitera après A6.)

---

## Task A6 — Mapper : Purse/Sex ↔ colonnes

**Files:**
- Modify: `src/infrastructure/persistence/mysql/features/character-sheet/mappers/CharacterSheetMapper.ts`

- [ ] **Step 1 — `toDomain`** : ajouter les imports `Sex` et `Purse`. Remplacer le mapping de `niveau`/`age`/`sexe`, retirer `monnaie`, ajouter `competences` et `purse` :
```ts
      niveau: row.niveau ?? null,
      age: row.age ?? null,
      sexe: row.sexe != null ? Sex.create(row.sexe) : null,
      competences: row.competences ?? null,
      purse: buildPurse(row),
```
où `buildPurse` est une fonction locale au mapper :
```ts
/** Reconstruit la bourse : null si les 3 colonnes sont absentes, sinon Purse (null → 0). */
function buildPurse(row: CharacterSheetRow): Purse | null {
  if (row.purse_gold == null && row.purse_silver == null && row.purse_copper == null) {
    return null;
  }
  return Purse.create({
    gold: row.purse_gold ?? 0,
    silver: row.purse_silver ?? 0,
    copper: row.purse_copper ?? 0,
  });
}
```

- [ ] **Step 2 — `toRow`** : à partir de `sheet.details`, écrire :
```ts
      niveau: d.niveau,
      age: d.age,
      sexe: d.sexe?.value ?? null,
      competences: d.competences,
      purse_gold: d.purse?.gold ?? null,
      purse_silver: d.purse?.silver ?? null,
      purse_copper: d.purse?.copper ?? null,
```
(retirer la ligne `monnaie`).

- [ ] **Step 3 — Typecheck** : `npx tsc -p tsconfig.json --noEmit` → le DAO+Mapper doivent compiler (restera des erreurs dans use cases/controller/tests, corrigées plus loin).

- [ ] **Step 4 — Commit** (DAO + Mapper ensemble)

```bash
git add src/infrastructure/persistence/mysql/features/character-sheet/dao/CharacterSheetDao.ts src/infrastructure/persistence/mysql/features/character-sheet/mappers/CharacterSheetMapper.ts
git commit -m "feat: map purse/sex columns and integer niveau/age in character sheet dao"
```

---

## Task A7 — DTO de lecture `CharacterSheetDetail` + commande Update

**Files:**
- Modify: `src/application/features/character-sheet/abstractions/usecases/CharacterSheetDetail.ts`
- Modify: `src/application/features/character-sheet/commands/UpdateCharacterSheetCommand.ts`

- [ ] **Step 1 — `CharacterSheetDetail`** : `niveau`/`age` → `number | null` ; `sexe` → `string | null` ; retirer `monnaie` ; ajouter `competences: string | null` et `purse: PurseView | null` où :
```ts
/** Représentation publique de la bourse (pièces brutes, non normalisées). */
export interface PurseView {
  readonly gold: number;
  readonly silver: number;
  readonly copper: number;
}
```

- [ ] **Step 2 — `UpdateCharacterSheetCommand`** : `niveau?`/`age?` → `number | null` ; `sexe?` → `string | null` ; retirer `monnaie?` ; ajouter `competences?: string | null` et `purse?: { gold?: number | null; silver?: number | null; copper?: number | null } | null`.

- [ ] **Step 3 — Commit**

```bash
git add src/application/features/character-sheet/abstractions/usecases/CharacterSheetDetail.ts src/application/features/character-sheet/commands/UpdateCharacterSheetCommand.ts
git commit -m "feat: add purse/competences and integer types to character sheet detail dto"
```

---

## Task A8 — Projection `toCharacterSheetDetail`

**Files:**
- Modify: `src/application/features/character-sheet/usecases/toCharacterSheetDetail.ts`

Aujourd'hui ce helper fait `{ id, ownerId, name, createdAt, ...sheet.details }`. Mais `details` contient maintenant `sexe: Sex|null` et `purse: Purse|null` (objets domaine) — il faut les projeter en formes publiques (`sexe.value`, `purse → {gold,silver,copper}`).

- [ ] **Step 1 — Réécrire** la projection :
```ts
import { CharacterSheet } from "@domain/features/character-sheet/entities/CharacterSheet";
import { CharacterSheetDetail } from "@application/features/character-sheet/abstractions/usecases/CharacterSheetDetail";

/** Projette une entité vers son DTO de lecture complet (VO → formes publiques). */
export function toCharacterSheetDetail(sheet: CharacterSheet): CharacterSheetDetail {
  const d = sheet.details;
  return {
    id: sheet.id,
    ownerId: sheet.ownerId,
    name: sheet.name.value,
    createdAt: sheet.createdAt,
    formation: d.formation,
    niveau: d.niveau,
    peuple: d.peuple,
    sexe: d.sexe?.value ?? null,
    tailleEtPoids: d.tailleEtPoids,
    age: d.age,
    apparence: d.apparence,
    dexterite: d.dexterite,
    intelligence: d.intelligence,
    perception: d.perception,
    social: d.social,
    vigueur: d.vigueur,
    pointsDeVie: d.pointsDeVie,
    pointsDeMagie: d.pointsDeMagie,
    protection: d.protection,
    competences: d.competences,
    purse: d.purse != null
      ? { gold: d.purse.gold, silver: d.purse.silver, copper: d.purse.copper }
      : null,
    armures: d.armures,
    armes: d.armes,
    equipement: d.equipement,
    sortsEtMiracles: d.sortsEtMiracles,
    notes: d.notes,
  };
}
```

- [ ] **Step 2 — Commit**

```bash
git add src/application/features/character-sheet/usecases/toCharacterSheetDetail.ts
git commit -m "feat: project sex and purse value objects in detail dto"
```

---

## Task A9 — Use case Update : valider Sex & Purse

**Files:**
- Modify: `src/application/features/character-sheet/usecases/UpdateCharacterSheetUseCaseImpl.ts`
- Test: `tests/application/UpdateCharacterSheetUseCase.test.ts`

Le `detailsFrom` actuel construit des `CharacterSheetDetails` avec `shortText/longText/nonNegativeInt`. Il faut : niveau/age via `nonNegativeInt`, `sexe` via `Sex.create` (catch DomainError → InvalidInputError), `purse` via `Purse.create`, ajouter `competences` (longText). Comme `Sex.create`/`Purse.create` peuvent throw, on les construit AVANT le `withDetails` dans le corps `execute` (comme le nom), pas dans le helper pur.

- [ ] **Step 1 — Mettre à jour le test** : ajouter des cas purse/sexe/competences et le rejet sexe/purse invalides.

```ts
  it("met à jour sexe (VO), purse et competences", async () => {
    txRepos.characterSheets.seed(buildTestCharacterSheet("s-1", "owner-1", "Aragorn"));
    const result = await useCase.execute({
      characterSheetId: "s-1",
      ownerId: "owner-1",
      name: "Aragorn",
      niveau: 5,
      age: 87,
      sexe: "m",
      competences: "Pistage, Survie",
      purse: { gold: 1, silver: 150, copper: 0 },
    });
    expect(result.isSuccess).toBe(true);
    expect(result.value.niveau).toBe(5);
    expect(result.value.age).toBe(87);
    expect(result.value.sexe).toBe("M");
    expect(result.value.competences).toBe("Pistage, Survie");
    expect(result.value.purse).toEqual({ gold: 1, silver: 150, copper: 0 });
  });

  it("échoue avec InvalidInputError si le sexe est invalide", async () => {
    txRepos.characterSheets.seed(buildTestCharacterSheet("s-1", "owner-1"));
    const result = await useCase.execute({
      characterSheetId: "s-1", ownerId: "owner-1", name: "X", sexe: "Z",
    });
    expect(result.error).toBeInstanceOf(InvalidInputError);
  });

  it("échoue avec InvalidInputError si la bourse est négative", async () => {
    txRepos.characterSheets.seed(buildTestCharacterSheet("s-1", "owner-1"));
    const result = await useCase.execute({
      characterSheetId: "s-1", ownerId: "owner-1", name: "X", purse: { gold: -1 },
    });
    expect(result.error).toBeInstanceOf(InvalidInputError);
  });
```
(le cas existant `met à jour le nom et les champs détaillés` qui utilisait `vigueur`/`notes` reste valide ; supprimer toute référence à `monnaie` s'il y en a.)

- [ ] **Step 2 — Lancer (échoue)** : `npx vitest run tests/application/UpdateCharacterSheetUseCase.test.ts` → FAIL.

- [ ] **Step 3 — Modifier l'impl** : ajouter imports `Sex`, `Purse`. Dans `execute`, après la validation du nom, ajouter la construction validée de sexe et purse :
```ts
    let sexe: Sex | null = null;
    let purse: Purse | null = null;
    try {
      if (command.sexe != null && command.sexe !== "") {
        sexe = Sex.create(command.sexe);
      }
      if (command.purse != null) {
        purse = Purse.create({
          gold: command.purse.gold ?? 0,
          silver: command.purse.silver ?? 0,
          copper: command.purse.copper ?? 0,
        });
      }
    } catch (error) {
      if (error instanceof DomainError) {
        return Result.failure(new InvalidInputError(error.code, error.message));
      }
      throw error;
    }

    const updated = sheet.withDetails({ name, sexe, purse, ...this.detailsFrom(command) });
```
Modifier `detailsFrom` pour : `niveau: nonNegativeInt(command.niveau)`, `age: nonNegativeInt(command.age)`, ajouter `competences: longText(command.competences)`, retirer `monnaie`, et NE PAS y mettre `sexe`/`purse` (gérés au-dessus). Garder les autres champs identiques. (Le type de retour de `detailsFrom` devient `Omit<CharacterSheetDetails, "sexe" | "purse">` — ou laisser `detailsFrom` retourner tous les champs sauf sexe/purse et les fusionner ; le plus simple : `detailsFrom` retourne un `Partial<CharacterSheetDetails>` couvrant tout sauf sexe/purse.)

- [ ] **Step 4 — Lancer (passe)** : `npx vitest run tests/application/UpdateCharacterSheetUseCase.test.ts` → PASS.

- [ ] **Step 5 — Mettre à jour le test Get** `tests/application/GetCharacterSheetUseCase.test.ts` : le cas existant seed avec `{ peuple, vigueur }` reste OK (types inchangés pour ceux-là) ; vérifier qu'aucune assertion ne touche `monnaie`. Lancer : `npx vitest run tests/application/GetCharacterSheetUseCase.test.ts` → PASS.

- [ ] **Step 6 — Commit**

```bash
git add src/application/features/character-sheet/usecases/UpdateCharacterSheetUseCaseImpl.ts tests/application/UpdateCharacterSheetUseCase.test.ts
git commit -m "feat: validate sex and purse in character sheet update use case"
```

---

## Task A10 — Controller : coercion du corps (purse objet, sexe, ints)

**Files:**
- Modify: `src/presentation/http/features/character-sheet/controllers/CharacterSheetController.ts`

Le `UpdateCharacterSheetBody` et `toUpdateCommand` actuels traitent niveau/age comme du texte et `monnaie` comme un nombre. Ajuster : niveau/age via `num()`, `sexe` via `text()`, retirer `monnaie`, ajouter `competences` (text), et `purse` (objet imbriqué).

- [ ] **Step 1 — `UpdateCharacterSheetBody`** : retirer `monnaie?`, ajouter `competences?: unknown;` et `purse?: unknown;`. (niveau/age/sexe restent `unknown`.)

- [ ] **Step 2 — `toUpdateCommand`** : changer `niveau`/`age` pour `num(...)`, garder `sexe: text(body.sexe)`, retirer la ligne `monnaie`, ajouter `competences: text(body.competences)`, et la bourse :
```ts
      competences: text(body.competences),
      purse: toPurseCommand(body.purse),
```
avec une fonction locale au fichier :
```ts
/** Extrait la bourse du corps : objet {gold,silver,copper} numérique, sinon null. */
function toPurseCommand(
  value: unknown,
): { gold: number | null; silver: number | null; copper: number | null } | null {
  if (value == null || typeof value !== "object") {
    return null;
  }
  const v = value as Record<string, unknown>;
  const num = (x: unknown): number | null => (typeof x === "number" ? x : null);
  return { gold: num(v.gold), silver: num(v.silver), copper: num(v.copper) };
}
```
(Le `toResponse` actuel `{ ...detail, createdAt: ISO }` fonctionne tel quel : `purse` est déjà un objet plat sérialisable et `sexe` une string.)

- [ ] **Step 3 — Typecheck** : `npx tsc -p tsconfig.json --noEmit` → 0 erreur attendue côté src (les tests restent à ajuster en A11).

- [ ] **Step 4 — Commit**

```bash
git add src/presentation/http/features/character-sheet/controllers/CharacterSheetController.ts
git commit -m "feat: accept purse object, sexe and integer fields in update controller"
```

---

## Task A11 — Tests fakes/DAO(db)/intégration

**Files:**
- Modify: `tests/application/fakes.ts`
- Modify: `tests/db/CharacterSheetDao.test.ts`
- Modify: `tests/presentation/characterSheetRoutes.integration.test.ts`

- [ ] **Step 1 — `fakes.ts`** : `buildTestCharacterSheet` prend `details: Partial<CharacterSheetDetails>` — comme les types ont changé (sexe: Sex, purse: Purse), les appels existants passant `{ peuple, vigueur }` restent valides. Vérifier qu'aucun appel ne passe `monnaie` ou `niveau`/`age` en string. (Aucune modif de code si les appels n'utilisent que peuple/vigueur ; sinon ajuster.)

- [ ] **Step 2 — `CharacterSheetDao.test.ts`** : le helper `sheetRow` liste les colonnes détaillées à null. Mettre à jour `sheetRow` pour : retirer `monnaie` si présent, ajouter `competences: null, purse_gold: null, purse_silver: null, purse_copper: null`, et `niveau`/`age` à `null` (déjà null). Ajouter au test V006 round-trip des assertions purse :
```ts
      // dans l'insert détaillé :
      niveau: 5,
      age: 87,
      sexe: "M",
      competences: "Pistage",
      purse_gold: 1,
      purse_silver: 50,
      purse_copper: 0,
      // après findById :
      expect(inserted!.niveau).toBe(5);
      expect(inserted!.sexe).toBe("M");
      expect(inserted!.competences).toBe("Pistage");
      expect(inserted!.purse_gold).toBe(1);
```

- [ ] **Step 3 — `characterSheetRoutes.integration.test.ts`** : dans le test `PUT … GET reflète les changements`, ajouter sexe/purse/competences au corps et aux assertions :
```ts
      const put = await agent.put(`/character-sheets/${created.body.id}`).send({
        name: "Strider", peuple: "Rôdeur", niveau: 5, age: 87, sexe: "M",
        competences: "Pistage", vigueur: 7, notes: "Garde du Nord",
        purse: { gold: 1, silver: 50, copper: 0 },
      });
      expect(put.status).toBe(200);
      expect(put.body.sexe).toBe("M");
      expect(put.body.niveau).toBe(5);
      expect(put.body.purse).toEqual({ gold: 1, silver: 50, copper: 0 });
      // ... GET reflète
      expect(res.body.competences).toBe("Pistage");
```
Ajouter un test de rejet :
```ts
    it("PUT avec un sexe invalide renvoie 400", async () => {
      const agent = await authenticate("p@test.com");
      const created = await agent.post("/character-sheets").send({ name: "A" });
      const res = await agent.put(`/character-sheets/${created.body.id}`).send({ name: "A", sexe: "Z" });
      expect(res.status).toBe(400);
      expect(res.body.code).toBe("INVALID_SEX");
    });
```

- [ ] **Step 4 — Vérifier le mapper d'erreur** : `CharacterSheetHttpMapper.statusFor` a un `default → 400`, donc `INVALID_SEX`/`INVALID_PURSE` (portés par `InvalidInputError`) tombent à 400. Aucune modif nécessaire. (Confirmer en lisant le mapper.)

- [ ] **Step 5 — Lancer les tests non-DB** : `npm run lint && npm run test` → vert.

- [ ] **Step 6 — Commit**

```bash
git add tests/
git commit -m "test: cover purse, sexe and competences across character sheet tests"
```

---

## Task A12 — Doc contrat API

**Files:**
- Modify: `docs/CONTRAT_API.md`

- [ ] **Step 1 — Mettre à jour** la section GET/:id et PUT : ajouter `competences`, remplacer `monnaie` par `purse: { gold, silver, copper } | null`, noter `sexe` ∈ {M,F,NB}, `niveau`/`age` entiers. Ajouter les codes `INVALID_SEX` (400), `INVALID_PURSE` (400).

- [ ] **Step 2 — Commit**

```bash
git add docs/CONTRAT_API.md
git commit -m "docs: document purse, sexe enum and competences in character sheet api"
```

---

# PARTIE B — FRONTEND

## Task B1 — Domaine `CharacterSheet` + `Purse`

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/domain/features/charactersheet/entities/CharacterSheet.kt`
- Create: `src/main/kotlin/eu/ejdr/domain/features/charactersheet/entities/Purse.kt`

- [ ] **Step 1 — Créer `Purse.kt`**

```kotlin
package eu.ejdr.domain.features.charactersheet.entities

/**
 * Bourse d'un personnage (domaine front anémique) : pièces d'or, d'argent et de cuivre.
 *
 * @property gold Pièces d'or.
 * @property silver Pièces d'argent.
 * @property copper Pièces de cuivre.
 */
data class Purse(
    val gold: Int,
    val silver: Int,
    val copper: Int,
)
```

- [ ] **Step 2 — Modifier `CharacterSheet.kt`** : changer `niveau`/`age` en `Int? = null`, `sexe` reste `String? = null`, retirer `monnaie`, ajouter `competences: String? = null` et `purse: Purse? = null`. Mettre à jour la KDoc des propriétés concernées.

- [ ] **Step 3 — Compile** : `.\gradlew.bat compileKotlin --console=plain` → erreurs attendues dans DTO/mapper/FormState (corrigées ensuite). (Pas de commit isolé ; commit avec B2.)

---

## Task B2 — DTO + mapper

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/infrastructure/http/features/charactersheet/dto/CharacterSheetDtos.kt`
- Modify: `src/main/kotlin/eu/ejdr/infrastructure/http/features/charactersheet/CharacterSheetHttpMapper.kt`

- [ ] **Step 1 — `CharacterSheetDtos.kt`** : ajouter le DTO bourse et ajuster les deux DTO :
```kotlin
/** Bourse JSON (pièces brutes). */
@Serializable
data class PurseDto(val gold: Int = 0, val silver: Int = 0, val copper: Int = 0)
```
Dans `CharacterSheetDto` : `niveau`/`age` → `Int? = null`, `sexe: String? = null` (déjà), retirer `monnaie`, ajouter `competences: String? = null` et `purse: PurseDto? = null`.
Dans `UpdateCharacterSheetRequestDto` : idem (`niveau: Int? = null`, `age: Int? = null`, `competences: String? = null`, `purse: PurseDto? = null`, retirer `monnaie`).

- [ ] **Step 2 — `CharacterSheetHttpMapper.kt`** : dans `toCharacterSheet`, ajuster niveau/age (Int), ajouter `competences = dto.competences`, `purse = dto.purse?.let { Purse(it.gold, it.silver, it.copper) }`, retirer `monnaie`. Dans `toUpdateRequest`, ajouter `competences = sheet.competences`, `purse = sheet.purse?.let { PurseDto(it.gold, it.silver, it.copper) }`, retirer `monnaie`. Importer `Purse` et `PurseDto`.

- [ ] **Step 3 — Commit** (domaine + DTO + mapper)

```bash
git add src/main/kotlin/eu/ejdr/domain/features/charactersheet/entities/ src/main/kotlin/eu/ejdr/infrastructure/http/features/charactersheet/
git commit -m "feat: model purse, competences and integer niveau/age on front character sheet"
```

---

## Task B3 — Atome `AppDropdown`

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/shared/component/atomic/AppDropdown.kt`

- [ ] **Step 1 — Créer l'atome** (menu déroulant DA, basé sur `ExposedDropdownMenuBox` material3) :

```kotlin
package eu.ejdr.presentation.shared.component.atomic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupProperties

/**
 * Atome menu déroulant du design system (choix parmi une liste fermée).
 *
 * Composant bête : affiche la valeur courante et propose [options] au clic. Réutilisable
 * (ex. sexe M/F/NB). La valeur peut être `null` (rien de sélectionné).
 *
 * @param value Valeur sélectionnée, ou `null`.
 * @param options Options proposées.
 * @param onSelect Callback de sélection.
 * @param label Libellé du champ.
 * @param modifier Modifier Compose appliqué au champ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdown(
    value: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = true),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}
```

**Note d'exécution :** vérifier au premier compile que `menuAnchor()` / `ExposedDropdownMenu` sont disponibles dans Compose MP 1.11.1 ; si l'API exige des arguments (`menuAnchor(type, enabled)`), adapter d'après l'erreur de compilation. C'est le point le plus susceptible d'ajustement.

- [ ] **Step 2 — Compile** : `.\gradlew.bat compileKotlin --console=plain` → BUILD SUCCESSFUL (l'atome seul).

- [ ] **Step 3 — Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/shared/component/atomic/AppDropdown.kt
git commit -m "feat(design-system): add AppDropdown atom for closed-list choices"
```

---

## Task B4 — `SheetCard` (carré de délimitation) + util normalisation Purse

**Files:**
- Create: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/component/SheetCard.kt`
- Create: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/component/PurseFormat.kt`

- [ ] **Step 1 — `SheetCard.kt`** (carte bordée + titre)

```kotlin
package eu.ejdr.presentation.features.charactersheet.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Carré de délimitation d'une section de fiche (composant bête).
 *
 * Carte à fond `surface`, bordure et coins arrondis, avec un titre en haut. Reproduit les
 * cadres de la fiche papier.
 *
 * @param title Titre de la section.
 * @param modifier Modifier Compose appliqué à la carte.
 * @param content Contenu de la section.
 */
@Composable
fun SheetCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(AppTheme.dimens.radiusMd)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTheme.colors.surface)
            .border(BorderStroke(AppTheme.dimens.borderWidth, AppTheme.colors.border), shape)
            .padding(AppTheme.dimens.md),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
    ) {
        AppText(text = title, style = AppTextStyle.Subtitle)
        content()
    }
}
```

- [ ] **Step 2 — `PurseFormat.kt`** (normalisation d'affichage)

```kotlin
package eu.ejdr.presentation.features.charactersheet.component

import eu.ejdr.domain.features.charactersheet.entities.Purse

private const val SILVER_PER_GOLD = 100
private const val COPPER_PER_SILVER = 100

/** Total de la bourse en pièces de cuivre. */
private fun Purse.totalInCopper(): Int =
    gold * SILVER_PER_GOLD * COPPER_PER_SILVER + silver * COPPER_PER_SILVER + copper

/**
 * Représente une bourse sous forme normalisée lisible (ex. « 1 PO · 50 PA · 0 PC »).
 * Recombine les pièces selon 1 PO = 100 PA = 10 000 PC.
 */
fun Purse.formatNormalized(): String {
    var total = totalInCopper()
    val g = total / (SILVER_PER_GOLD * COPPER_PER_SILVER)
    total -= g * SILVER_PER_GOLD * COPPER_PER_SILVER
    val s = total / COPPER_PER_SILVER
    val c = total - s * COPPER_PER_SILVER
    return "$g PO · $s PA · $c PC"
}
```

- [ ] **Step 3 — Compile** : `.\gradlew.bat compileKotlin --console=plain` → SUCCESS.

- [ ] **Step 4 — Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/charactersheet/component/SheetCard.kt src/main/kotlin/eu/ejdr/presentation/features/charactersheet/component/PurseFormat.kt
git commit -m "feat(charactersheet): add bordered SheetCard and purse normalization helper"
```

---

## Task B5 — `CharacterSheetFormState` : nouveaux champs

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/component/CharacterSheetFormState.kt`

- [ ] **Step 1 — Remplacer** le champ `monnaie` et ajouter sexe(sélection)/competences/purse. Concrètement : retirer `var monnaie`, ajouter :
```kotlin
    var competences by mutableStateOf(source.competences.orEmpty())
    var purseGold by mutableStateOf(source.purse?.gold.toFieldText())
    var purseSilver by mutableStateOf(source.purse?.silver.toFieldText())
    var purseCopper by mutableStateOf(source.purse?.copper.toFieldText())
```
`sexe` reste un `var sexe by mutableStateOf(source.sexe.orEmpty())` (la valeur "M"/"F"/"NB" ou "").
`niveau`/`age` : aujourd'hui `source.niveau.orEmpty()` ne compile plus (Int?), remplacer par `source.niveau.toFieldText()` et `source.age.toFieldText()`.

- [ ] **Step 2 — `toCharacterSheet()`** : `niveau = niveau.toNullableInt()`, `age = age.toNullableInt()`, `sexe = sexe.toNullableText()`, ajouter `competences = competences.toNullableText()`, retirer `monnaie`, et construire la bourse :
```kotlin
        purse = buildPurse(),
```
avec une méthode privée dans la classe :
```kotlin
    /** Construit la bourse depuis les 3 champs ; null si les 3 sont vides. */
    private fun buildPurse(): Purse? {
        val g = purseGold.toNullableInt()
        val s = purseSilver.toNullableInt()
        val c = purseCopper.toNullableInt()
        return if (g == null && s == null && c == null) null
        else Purse(gold = g ?: 0, silver = s ?: 0, copper = c ?: 0)
    }
```
Importer `Purse`.

- [ ] **Step 3 — Compile** : erreurs attendues dans les sections/page (B6). Pas de commit isolé.

---

## Task B6 — Sections en cartes + disposition fidèle

**Files:**
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/component/CharacterSheetSections.kt`
- Modify: `src/main/kotlin/eu/ejdr/presentation/features/charactersheet/page/CharacterSheetDetailPage.kt`

Réorganiser : chaque section dans un `SheetCard`. Identité (Nom/Formation/Niveau ; Peuple/Sexe(dropdown)/Taille-poids/Âge ; Apparence) ; rangée responsive 3 colonnes Caractéristiques / Combat / Bourse ; puis 3 paires 50/50 (Armures·Armes, Compétences·Équipement, Sorts&Miracles·Notes). Le bloc Bourse : en édition 3 `NumberCell` (PO/PA/PC) ; en lecture, `Purse.formatNormalized()` (ou « — » si null).

- [ ] **Step 1 — `CharacterSheetSections.kt`** : adapter `IdentiteSection` pour que `Niveau`/`Âge` soient des `NumberCell` (Int) et `Sexe` un dropdown. Ajouter un `SexCell` :
```kotlin
@Composable
fun SexCell(editing: Boolean, value: String, readValue: String?, onChange: (String) -> Unit) {
    if (editing) {
        AppDropdown(
            value = value.ifBlank { null },
            options = listOf("M", "F", "NB"),
            onSelect = onChange,
            label = "Sexe",
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        // réutilise l'affichage lecture libellé/valeur
        ReadCellPublic("Sexe", readValue)
    }
}
```
(exposer un `ReadCellPublic` ou réutiliser `TextCell` en lecture — au choix de l'implémenteur, en gardant `ReadCell` privé ou en le rendant interne).
Créer les sections `CombatSection` (pointsDeVie/pointsDeMagie/protection) et `PurseSection` :
```kotlin
@Composable
fun PurseSection(sheet: CharacterSheet, editing: Boolean, form: CharacterSheetFormState) {
    if (editing) {
        NumberCell("Or (PO)", true, form.purseGold, null) { form.purseGold = it }
        NumberCell("Argent (PA)", true, form.purseSilver, null) { form.purseSilver = it }
        NumberCell("Cuivre (PC)", true, form.purseCopper, null) { form.purseCopper = it }
    } else {
        AppText(
            text = sheet.purse?.formatNormalized() ?: "—",
            style = AppTextStyle.Body,
        )
    }
}
```
Adapter `CaracteristiquesSection` pour ne contenir QUE les 5 stats (Combat et Bourse deviennent leurs propres sections). Ajouter `competences` dans les `LongTextSection`.

- [ ] **Step 2 — `CharacterSheetDetailPage.kt`** : remplacer le corps de `CharacterSheetDetailContent` pour envelopper chaque section dans `SheetCard(...)` et disposer la rangée Caractéristiques/Combat/Bourse via `ResponsiveColumns`, puis les 3 paires de texte via `ResponsiveColumns` à 2 colonnes. Exemple de structure :
```kotlin
        SheetCard("Identité") { IdentiteSection(sheet, isEditing, form) }
        ResponsiveColumns(columns = listOf(
            { SheetCard("Caractéristiques") { CaracteristiquesSection(sheet, isEditing, form) } },
            { SheetCard("Combat") { CombatSection(sheet, isEditing, form) } },
            { SheetCard("Bourse") { PurseSection(sheet, isEditing, form) } },
        ))
        ResponsiveColumns(columns = listOf(
            { SheetCard("Armures") { LongTextBody(isEditing, form.armures, sheet.armures) { form.armures = it } } },
            { SheetCard("Armes") { LongTextBody(isEditing, form.armes, sheet.armes) { form.armes = it } } },
        ))
        ResponsiveColumns(columns = listOf(
            { SheetCard("Compétences") { LongTextBody(isEditing, form.competences, sheet.competences) { form.competences = it } } },
            { SheetCard("Équipement") { LongTextBody(isEditing, form.equipement, sheet.equipement) { form.equipement = it } } },
        ))
        ResponsiveColumns(columns = listOf(
            { SheetCard("Sorts & Miracles") { LongTextBody(isEditing, form.sortsEtMiracles, sheet.sortsEtMiracles) { form.sortsEtMiracles = it } } },
            { SheetCard("Notes") { LongTextBody(isEditing, form.notes, sheet.notes) { form.notes = it } } },
        ))
```
où `LongTextBody` est le contenu d'une zone texte sans titre (le titre est porté par le `SheetCard`) — extraire depuis l'actuel `LongTextSection` la partie « champ ou texte » sans le `Section(title)`. Garder chaque sous-composable court pour rester sous detekt LongMethod (80).

Comme `SheetCard` porte déjà le titre, les sections internes ne doivent PLUS appeler `Section(title)` (sinon double titre) — adapter `IdentiteSection`/`CaracteristiquesSection`/`CombatSection`/`PurseSection` pour ne contenir que les cellules (sans le wrapper `Section`).

- [ ] **Step 3 — Compile** : `.\gradlew.bat compileKotlin --console=plain` → SUCCESS (ajuster les erreurs API dropdown/menuAnchor si besoin).

- [ ] **Step 4 — Commit**

```bash
git add src/main/kotlin/eu/ejdr/presentation/features/charactersheet/
git commit -m "feat(charactersheet): lay out detail in bordered cards with sex dropdown and purse"
```

---

## Task B7 — Tests front + verify

**Files:**
- Modify: `src/test/kotlin/eu/ejdr/infrastructure/http/features/charactersheet/CharacterSheetHttpRepositoryTest.kt`

- [ ] **Step 1 — Ajouter un test purse/sexe** dans `getById success` ou un nouveau cas : le JSON inclut `"sexe":"M","niveau":5,"competences":"Pistage","purse":{"gold":1,"silver":50,"copper":0}` et on asserte `result.value.sexe == "M"`, `result.value.niveau == 5`, `result.value.purse == Purse(1,50,0)`, `result.value.competences == "Pistage"`. Garder le test de régression liste nom-seul (les nouveaux champs DTO ont des défauts → la liste désérialise toujours).

```kotlin
    @Test
    fun `getById maps sexe niveau purse competences`() = runTest {
        val body = """
            {"id":"s-1","ownerId":"u-1","name":"Aragorn","createdAt":"2026-06-13T10:00:00.000Z",
             "sexe":"M","niveau":5,"competences":"Pistage","purse":{"gold":1,"silver":50,"copper":0}}
        """.trimIndent()
        val result = repository(clientReturning(HttpStatusCode.OK, body)).getById("s-1")
        assertIs<Result.Success<CharacterSheet>>(result)
        assertEquals("M", result.value.sexe)
        assertEquals(5, result.value.niveau)
        assertEquals("Pistage", result.value.competences)
        assertEquals(Purse(1, 50, 0), result.value.purse)
    }
```
Importer `Purse`. Vérifier que le test `update success` existant n'utilise pas `monnaie`.

- [ ] **Step 2 — `./gradlew verify`** : `.\gradlew.bat verify --console=plain` → BUILD SUCCESSFUL (compile + detekt + Kover + tests). Surveiller detekt LongMethod sur la page : si dépassement, extraire davantage de sous-composables.

- [ ] **Step 3 — Commit**

```bash
git add src/test/
git commit -m "test(charactersheet): cover purse, sexe and competences in http repo"
```

---

# Vérification finale (manuelle)

- [ ] **Backend tests DB (Docker requis)** : `npm run test:db` → vert (valide la nouvelle V006 + colonnes purse). Si Docker indisponible, à faire par l'utilisateur.
- [ ] **Migration runtime** : `npm run migrate:down` (annule l'ancienne V006) puis `npm run migrate:up` (applique la nouvelle). Vérifier `SHOW COLUMNS FROM character_sheets` → 28 colonnes (4 base + 24 détaillées).
- [ ] **Runtime front** : `./gradlew run` (backend up :3000) → ouvrir une fiche : sections en **carrés**, rangée Caractéristiques/Combat/Bourse en 3 colonnes, textes en 3 paires 50/50, repli en pile si fenêtre étroite. **Modifier** → Sexe en menu déroulant (M/F/NB), Bourse 3 champs PO/PA/PC → **Enregistrer** → rouvrir : valeurs persistées, **bourse normalisée** en lecture (ex. 150 PA → « 1 PO · 50 PA · 0 PC »).

---

## Auto-review (couverture spec)

- VO Purse (règle 100/100, normalized) → A1 ✅ ; VO Sex (M/F/NB) → A2 ✅ ; entité types + competences + purse → A3 ✅ ; migration V006 refaite → A4 ✅ ; DAO/Mapper colonnes → A5/A6 ✅ ; DTO/commande → A7 ✅ ; projection → A8 ✅ ; use case validation → A9 ✅ ; controller coercion → A10 ✅ ; tests back → A11 ✅ ; doc → A12 ✅. Front : domaine+Purse → B1 ✅ ; DTO/mapper → B2 ✅ ; AppDropdown → B3 ✅ ; SheetCard+normalisation → B4 ✅ ; FormState → B5 ✅ ; disposition cartes + paires 50/50 → B6 ✅ ; tests+verify → B7 ✅. Stockage 3 colonnes purse sur character_sheets → A4/A5/A6 ✅. Contrat purse imbriqué → A7/A8/A10 + B2 ✅.
