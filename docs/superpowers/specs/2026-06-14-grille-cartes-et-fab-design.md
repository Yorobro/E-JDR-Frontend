# Grille de cartes rectangulaires + bouton flottant (FAB)

**Date** : 2026-06-14
**Statut** : design validé, prêt pour plan d'implémentation
**Repo** : `E-JDR-Frontend` (Kotlin Compose Desktop)
**Branche** : `feat/campaigns`

## Contexte & objectif

Sur les écrans de liste « Campagnes » et « Mes fiches », l'utilisateur veut :
1. Le bouton d'ajout **en bas à droite** (bouton flottant / FAB), au lieu d'un bouton texte en haut.
2. Les cartes affichées en **grille de tuiles rectangulaires (~carrées)** qui s'alignent sur plusieurs colonnes, au lieu d'une liste verticale de cartes pleine largeur.
3. Les tuiles de fiches deviennent **cliquables** → ouvrent un **écran détail de fiche** (nouveau, minimal).

## Périmètre (validé)

- **Écrans concernés** : `CampaignListPage` (Campagnes) et `MyCharacterSheetsPage` (Mes fiches).
- **Hors périmètre** : l'écran détail d'une campagne (`CampaignDetailPage`) garde son style actuel (liste verticale + bouton « Rattacher »).
- **Nouveau** : un écran détail de fiche minimal (pour donner une cible au clic des tuiles de fiches).

## État actuel

Les deux pages ont une structure identique :
```
Column(padding) {
  AppButton("Ajouter …", leadingIcon = Add)   // bouton texte en haut
  FormError(error)
  Box(fillMaxSize) {
    when { loading -> spinner ; vide -> texte ; else -> LazyColumn de cartes fillMaxWidth }
  }
}
```
Les cartes (`CampaignCard`, `CharacterSheetCard`) sont des `Row` pleine largeur (nom à gauche `weight(1f)`, poubelle à droite). `CampaignCard` est cliquable (ouvre le détail) ; `CharacterSheetCard` ne l'est pas (juste poubelle, `onDelete` nullable pour l'usage lecture seule du détail campagne).

## Conception

### 1. Layout des pages (Box plein écran + FAB superposé)

Remplacer le `Column` racine par un **`Box(fillMaxSize)`** qui superpose :
- **le contenu** : un `Column` (ou directement le `when`) portant `FormError` en haut puis l'état (spinner / vide / grille) ;
- **le FAB** : `AppFab(onClick = { showCreate = true }, …)` aligné `Modifier.align(Alignment.BottomEnd).padding(AppTheme.dimens.xl)`.

La grille doit avoir un `contentPadding` bas suffisant pour que la dernière rangée ne soit pas masquée par le FAB.

### 2. La grille

`LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 180.dp))` — autant de colonnes que la largeur de la fenêtre permet. Largeur mini de tuile **180.dp** (valeur de départ, ajustable au rendu). Espacement horizontal et vertical via `Arrangement.spacedBy(AppTheme.dimens.md)`, `contentPadding` via `AppTheme.dimens`.

Import : `androidx.compose.foundation.lazy.grid.LazyVerticalGrid`, `GridCells`, `items`.

### 3. Nouveau composant atomique `AppFab`

`presentation/shared/component/atomic/AppFab.kt` (bête, réutilisable) :
- bouton circulaire, fond `AppTheme.colors.primary`, icône `Add` centrée (tint = couleur de contenu sur primary), légère élévation/ombre, coins = cercle.
- Params : `onClick: () -> Unit`, `contentDescription: String`, `modifier: Modifier = Modifier`. (Icône `Add` par défaut ; pas besoin de la paramétrer pour ce lot — YAGNI.)
- Cohérent avec la DA (réutilise `AppTheme.colors`/`dimens`). Remplace l'usage du `AppButton` texte d'ajout sur les deux écrans.

### 4. Les tuiles (`CampaignCard`, `CharacterSheetCard`)

Transformer chaque carte de `Row` pleine largeur en **tuile à hauteur fixe** :
- conteneur `Box` : `fillMaxWidth` (dans sa cellule) + `height(140.dp)` (valeur de départ, ajustable), `clip(shape)`, `background(surface)`, `border`, coins arrondis `radiusMd` (DA inchangée) ;
- **nom centré** : `AppText(..., maxLines = 2, overflow = Ellipsis)` aligné `Alignment.Center` ;
- **poubelle en coin haut-droite** : `IconButton` aligné `Alignment.TopEnd`, `tint = danger`, `contentDescription` adapté ; son clic ne déclenche pas le clic de la tuile (l'`IconButton` consomme le clic) ;
- **clic tuile** : `Modifier.clickable(onClick)` sur le `Box`, actif seulement si `onClick != null`.

Signatures :
- `CampaignCard(campaign, onClick: () -> Unit, onDelete: () -> Unit, modifier)` — inchangé côté API (déjà cliquable + supprimable). Mise en page → tuile.
- `CharacterSheetCard(sheet, onClick: (() -> Unit)? = null, onDelete: (() -> Unit)?, modifier)` — **ajout d'un `onClick` nullable**. `onClick == null` ⇒ tuile non cliquable (préserve l'usage lecture seule dans `CampaignDetailPage`, qui passe `onDelete = …` sans `onClick`). `onDelete == null` ⇒ pas de poubelle (déjà le cas).

> ⚠️ Vérifier que `CampaignDetailPage` (qui réutilise `CharacterSheetCard` en liste verticale) compile toujours et garde son apparence : il appelle `CharacterSheetCard(sheet, onDelete = …)` sans `onClick`. Avec `onClick` nullable par défaut à `null`, l'appel reste valide. NB : `CampaignDetailPage` n'est PAS converti en grille (hors périmètre) — mais ses cartes deviendront des tuiles ~carrées car elles partagent le composant. **Décision** : on accepte que la carte du détail campagne adopte aussi la forme tuile (cohérence visuelle), affichée dans son `LazyColumn` actuel (une tuile par ligne). Si le rendu y est gênant, on ajustera, mais on ne crée pas de second composant.

### 5. Écran détail de fiche (nouveau, minimal)

Calqué sur `Route.CampaignDetail(id, name)` :
- **Route** : `data class CharacterSheetDetail(val id: String, val name: String) : Route` dans `navigation/Routes.kt`, annotée `@Serializable`.
  - ⚠️ **Piège Nav3 #1** : ajouter `subclass(Route.CharacterSheetDetail::class)` dans `appNavConfiguration` (Routes.kt), sinon crash au démarrage.
- **Page** : `presentation/features/charactersheet/page/CharacterSheetDetailPage(id, name, modifier)` — minimale : titre = `name` (pas de ViewModel, rien à charger pour l'instant). Modèle = l'ancien `CampaignDetailPage` minimal (un `Column` + `AppText` titre).
- **Nav entry** : dans `CharacterSheetNavEntries.kt`, ajouter `entry<Route.CharacterSheetDetail> { key -> … }` enveloppé dans `AppScaffold` + `AppTopBar(title = key.name, onBack = …)`. ⚠️ **Piège Nav3 #2** rappel : l'entry doit exister ici.
- **Câblage du clic** : `MyCharacterSheetsPage` reçoit un param `onOpenSheet: (id: String, name: String) -> Unit` (modèle `CampaignListPage.onOpenCampaign`). `characterSheetEntries(actions)` fournit ce callback en poussant `Route.CharacterSheetDetail(id, name)` sur `actions.backStack`. La tuile fiche passe `onClick = { onOpenSheet(sheet.id, sheet.name) }`.

### 6. Kover

Tous les nouveaux fichiers sont de l'**UI Compose pure** (pages, composant `AppFab`, carte détail, nav entries) → doivent être **exclus de Kover** (convention projet : on compte ViewModels/use cases/repos, pas l'UI). Pas de nouveau ViewModel ⇒ aucun test unitaire à ajouter.
- Vérifier que les patterns d'exclusion Kover dans `build.gradle.kts` couvrent les nouveaux fichiers (`*.page`, `*.component`, `*NavEntriesKt`, et `AppFab`/`AppFabKt` si les atomes ne sont pas déjà exclus globalement). Ajuster si la couverture descend sous 60 %.

## Tests & vérification

- **Pas de test unitaire** : aucun ViewModel/logique ajouté (UI pure).
- `./gradlew verify` (detekt 0 + tests + Kover ≥ 60 %) doit rester **vert**.
- ⚠️ **Validation runtime à la charge de l'utilisateur** (`./gradlew run`, backend up) — non couvert par les tests (pièges Nav3 desktop) :
  1. La fenêtre s'ouvre sans crash (nouvelle Route sérialisable bien enregistrée).
  2. « Mes fiches » : grille de tuiles sur plusieurs colonnes ; FAB en bas à droite ; clic FAB → dialog création ; clic tuile → écran détail fiche (titre = nom) → retour.
  3. « Campagnes » : grille de tuiles ; FAB ; clic tuile → détail campagne (inchangé) ; poubelle → confirmation suppression.
  4. Redimensionner la fenêtre → le nombre de colonnes s'adapte.

## Hors périmètre (YAGNI)

- Pas de contenu réel sur le détail de fiche (juste le nom ; enrichissement ultérieur).
- Pas de conversion de `CampaignDetailPage` en grille (garde son `LazyColumn`).
- Pas de paramétrage avancé du FAB (couleur/icône configurables) : un seul usage « Ajouter ».
- Pas de drag & drop / réordonnancement des tuiles.
