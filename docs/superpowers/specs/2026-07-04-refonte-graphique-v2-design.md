# Refonte graphique native v2 — retrait de Material 3 (Design)

**Date :** 2026-07-04
**Statut :** Validé (en attente de relecture du spec)
**Branche :** `feat/refonte-graphique-v2` (depuis `origin/main` @ `e6a75ae`, release 1.26.0)
**Spec précédente pertinente :** `2026-06-07-design-system-components-navbar-design.md` (design system initial)

## Contexte

Le front E-JDR est une application **Kotlin Multiplatform + Compose Multiplatform** (cibles Desktop et Android), architecture clean, navigation **Nav3**. Une refonte « premium-grimoire » est déjà mergée dans `main` : design system maison (`AppTheme` via `CompositionLocal`), 3 thèmes (**Parchemin** clair chaleureux, **Taupe** clair minimaliste, **Grimoire** sombre premium), fonts custom (Fraunces / Inter / JetBrains Mono), discipline « zéro couleur/dimension en dur » verrouillée par `LocalAppColors.error()`.

Aujourd'hui, tout l'**aspect** visuel est déjà maison, mais la **mécanique** de rendu s'appuie encore sur **Material 3** (`compose.material3`) : `Surface`, `OutlinedTextField`, `AlertDialog`, `DropdownMenu`, `NavigationBar`, `Snackbar`, ripple, ombres. L'objectif de cette refonte est de **retirer complètement Material 3** et de tout redessiner sur `compose.foundation`, pour un rendu **moderne, professionnel et 100 % à nous**, sans changer la charte.

Demande utilisateur (validée en brainstorming) : « on refait tout nous-mêmes, on n'utilise rien, on fait natif, rendu très moderne et professionnel, on garde les mêmes thèmes, sur une branche à part ».

## Principe directeur : intensité visuelle riche / sobre

La charte (3 thèmes) est **conservée à l'identique**. Ce qui change, c'est le **traitement** appliqué :

- **Traitement RICHE** (ornements dorés, filets intérieurs, reliefs, boutons ciselés à dégradé) — réservé aux **4 écrans « vitrine »** : **Splash, Auth (connexion/inscription), Accueil/Profil, Fiche de perso (détail)**.
- **Traitement SOBRE soigné** (dégradés/ombres discrets, pas d'ornement) — partout ailleurs : **listes** (campagnes/fiches/groupes), **référentiels** (« Mes éléments »), **réglages**, **dialogues**.

Le riche est un **accent rare** → il garde son impact. Le sobre reste soigné (moderne, pas plat/pauvre).

## Décisions de design

- **Retrait total de Material 3.** `compose.material3` supprimé des dépendances. Fondation UI = `compose.foundation` + `compose.ui` uniquement.
- **Champs de texte maison** sur `BasicTextField` (fourni par `compose.foundation`) : curseur, sélection, focus, erreur, placeholder, œil du mot de passe — tout à nous.
- **Icônes maison** : jeu `AppIcons` d'`ImageVector` tracés à la main. `materialIconsExtended` retiré.
- **Migration progressive**, écran par écran : l'app compile et tourne à chaque étape.
- **API publique des composants conservée** quand c'est possible (mêmes noms) pour minimiser l'impact sur les écrans.
- **Navigation** : Nav3 et routes inchangées ; seule la chrome change (voir plus bas).
- **Animations** : niveau **équilibré**, via `AppMotion`, respecte le réglage « mouvement réduit » existant.
- **Périmètre visuel seulement** : on ne change pas ce que fait l'app (aucune nouvelle feature, aucun changement de charte/couleurs, ni backend).

## Architecture technique

### Retrait de Material 3 → socle de primitives maison

Nouveau paquet `presentation/shared/component/base/` réimplémentant sur `foundation` ce que Material fournissait :

| Brique Material retirée | Remplacement maison (sur `foundation`) |
|---|---|
| `Surface` | `AppSurface` — `Box` + background / shape / border / ombre depuis le thème |
| `OutlinedTextField` | `AppTextFieldCore` — sur `BasicTextField` (curseur, focus, erreur, placeholder) |
| `AlertDialog` | `AppDialogCore` — `Popup` (foundation) + scrim + carte animée |
| `DropdownMenu` | `AppDropdownCore` — `Popup` + positionnement maison |
| `NavigationBar` (Android) | `AppBottomBar` — `Row` maison |
| `Snackbar` | finalisé sur `Box` / `Popup` (déjà semi-maison) |
| ripple Material | `AppInteractionSource` — feedback press/hover maison (press-scale + overlay), depuis `AppMotion` |

Les primitives `base/` sont **bêtes, mécaniques, testées** (logique pure : positionnement de popup, mapping d'état de focus/erreur, filtre numérique conservé).

### Migration progressive

On construit d'abord le socle `base/` + le thème étendu (rien de visible), en gardant l'app compilable. Puis on migre les écrans un par un. À aucun moment l'app n'est cassée durablement. `verifyDesktop` reste **vert** à chaque étape.

## Design system étendu — `presentation/shared/theme/`

Les **3 palettes existantes et leurs valeurs hex sont conservées** (pas de nouvelle charte). On **enrichit** les tokens :

- **`AppColors`** — nouveaux rôles pour le traitement riche : `accentGradientTop` / `accentGradientBottom` (boutons ciselés), `ornament` (doré des filets / flourish), `hairline` (trait dégradé des séparateurs). Dérivés par thème depuis `AppPalette` (ex. Grimoire : gradient `#E4C06B → #C9A24B`, `ornament #C9A24B`).
- **`AppElevation`** (nouveau) — Material ne fournissant plus l'ombre, jeu d'ombres maison (`sm` / `md` / `lg` : offset, blur, couleur) appliqué par `AppSurface`.
- **`AppDimens`** — inchangé (déjà complet).
- **`AppMotion`** — inchangé ; pilote toutes les animations maison.
- **`AppTreatment`** (nouveau) — enum `Rich` / `Plain` fournie via `CompositionLocal`. Un écran vitrine pose `ProvideTreatment(Rich) { … }` ; par défaut `Plain`. Les composants adaptent leur rendu sans dupliquer d'écran.

La règle « aucune couleur / dimension en dur » reste **verrouillée** (`LocalAppColors.error()` conservés). Aucun `Color(0x…)` hors du paquet `theme/`.

## Bibliothèque de composants (réécriture sur foundation)

**a) Socle `base/`** : `AppSurface`, `AppTextFieldCore`, `AppDialogCore`, `AppDropdownCore`, `AppBottomBar`, `AppInteractionSource`.

**b) Composants existants réhabillés** (mêmes noms/API quand possible, réimplémentés sur le socle, sensibles à `AppTreatment`) :

- **Atoms** : `AppText`, `AppButton` (5 variants ; Primary **ciselé/dégradé** en zone riche, **plat** en zone sobre), `AppTextField` / `AppPasswordField` / `AppNumberField` (sur `AppTextFieldCore`), `AppCheckbox`, `AppDropdown` (sur `AppDropdownCore`), `AppTabs`, `AppIcon`, `AppBadge`, `AppDivider`, `AppFab`, `SkeletonBox`.
- **Ornements (zone riche)** : `AppCornerFlourish` (`◆ ───── ◆`), `AppBrandMark` (sceau), `AppHairline` (séparateur dégradé doré) — finalisés.
- **Molecules** : `EmptyState`, `LabeledField`, `FormError`, `RemoteChangeBanner`, `SkeletonGrid` / `SkeletonList`.
- **Organisms** : `AppCard` (filet doré intérieur si `Rich`), `AppTopBar`, `AppBottomBar`, `AppScaffold`, `PageHeader` (flourish si `Rich`), `AppDialog`, `AppSnackbar`.

### Navigation (chrome uniquement — Nav3 inchangé)

- **Desktop — `MainTopBar`** : icônes **seules**, **libellé au survol** (tooltip maison via `Popup`), onglet actif en doré.
- **Android — `AppBottomBar`** : icônes **seules**, **libellé affiché sur l'onglet actif uniquement** (pastille dorée).
- Mêmes icônes desktop/mobile issues du jeu `AppIcons`.

## Icônes maison — `AppIcons`

Jeu d'`ImageVector` tracés à la main (trait fin, style manuscrit léger) : destinations (profil, campagnes, fiches, groupes, éléments, réglages) et actions (`+`, éditer, supprimer, retour, œil, etc.). `materialIconsExtended` **retiré** du build. Un écran de galerie interne (dev-only) permet de vérifier tous les tracés.

## Ordre de migration des écrans

Chaque étape compile et tourne (desktop **et** Android) avant de passer à la suivante :

1. **Socle + thème** (`base/`, tokens, `AppTreatment`) — rien de visible.
2. **Splash** (riche) — sceau `AppBrandMark` animé.
3. **Auth** (riche) — carte encadrée grimoire, champs maison.
4. **Accueil / Profil** (riche) — cf. maquette validée (en-tête orné, carte ciselée, boutons en relief).
5. **Listes** (sobre) — campagnes / fiches / groupes : en-tête + grille de cartes.
6. **Fiche de perso — détail** (riche) — pièce maîtresse : en-tête orné + corps lisible pour l'édition.
7. **Référentiels / « Mes éléments »** (sobre).
8. **Réglages** (sobre) — dont le sélecteur de thème (3 variantes).
9. **Dialogues** (sobre) — créer / copier / supprimer, sur `AppDialogCore`.
10. **Navigation** — top bar desktop (tooltip) + bottom bar Android (actif nommé).

## Animations (niveau équilibré)

Via `AppMotion`, respecte « mouvement réduit » : transitions d'écran (fondu / slide léger), press-scale cartes & boutons, hover desktop, apparition des listes (stagger léger), dialogues (fade + scale), tooltip de nav. Rien de tape-à-l'œil.

## Périmètre

**Inclus :** retrait `material3` + `materialIconsExtended`, socle `base/` maison, thème étendu (`AppColors` enrichi, `AppElevation`, `AppTreatment`), réécriture des composants, jeu d'icônes `AppIcons`, migration des 10 écrans (desktop **et** Android), animations équilibrées.

**Hors périmètre :** nouvelles fonctionnalités, changement de charte / couleurs, nouveaux écrans, refonte backend. On ne change pas **ce que fait** l'app, seulement son **apparence**.

## Tests

- Logique pure testée (JUnit / desktopTest) : `filterNumericInput` conservé, positionnement de popup (`AppDropdownCore` / `AppDialogCore`), mapping d'état de focus/erreur, résolution `AppTreatment`.
- Pas de tests UI Compose (cohérent avec l'existant ; ces paquets restent exclus de Kover).

## Vérification (end-to-end)

1. `./gradlew verifyDesktop` (detekt + desktopJar + koverVerify) **vert** à chaque étape.
2. `./gradlew run` (desktop) démarre ; `./gradlew assembleDebug` (Android) produit l'APK.
3. Revue visuelle des 10 écrans dans les **3 thèmes** (Parchemin / Taupe / Grimoire).
4. **Grep de contrôle** : plus aucune référence à `androidx.compose.material3` ni à `materialIconsExtended` dans le code (`src/`).
5. Navigation : desktop tooltip au survol OK ; Android libellé sur l'onglet actif OK.

## Conventions

Conventional Commits, atomiques et par étape, ex. :
`build(front): retirer material3 et materialIconsExtended`,
`feat(theme): tokens ornement/gradient/elevation + AppTreatment`,
`feat(component): socle base sur foundation (AppSurface, TextFieldCore, DialogCore…)`,
`feat(icons): jeu d'icônes maison AppIcons`,
`refactor(component): réimplémenter les composants sur le socle maison`,
`refactor(feature): migrer l'écran <x> vers le rendu natif`,
`feat(nav): top bar tooltip (desktop) + bottom bar actif nommé (android)`.

## Hors périmètre (plus tard)

Dark/light auto selon l'OS, nouveaux thèmes, tests UI Compose, refonte de l'ergonomie/flux (au-delà de l'habillage), nouvelles features.
