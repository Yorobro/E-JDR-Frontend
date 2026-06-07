# Design System + Bibliothèque de composants + Top bar + Page utilisateur (Design)

**Date:** 2026-06-07
**Statut:** Validé (en attente de relecture finale)
**Spec précédente:** `2026-06-06-ejdr-desktop-clean-arch-design.md`

## Contexte

Le scaffolding E-JDR (Compose Desktop, clean architecture) et la feature Auth sont en place et poussés sur `main`. Les composants de présentation actuels (`AppButton`, `AppTextField`, `LabeledTextField`, `FormError`) s'appuient sur `MaterialTheme` brut, sans système de design centralisé, et il n'existe ni navigation persistante ni page d'accueil réelle (le post-login affiche un placeholder « Bienvenue »).

On veut : (1) un **design system maison** gris/beige pilotant toute l'UI depuis un seul endroit, (2) une **bibliothèque de composants réutilisables** (atoms + molecules) générique et bien documentée, (3) une **top bar présente partout dans la zone connectée**, (4) une **page utilisateur** comme écran d'accueil après connexion. Exigence transverse : toutes les pages et composants doivent **consommer** ces composants réutilisables (pas de style en dur).

Palette validée : neutres gris chauds → beiges, accent **taupe foncé** monochrome. Style des champs : **Outlined**. Boutons : **5 variants** (Primary / Secondary / Text / Danger / Ghost), sans système de tailles. Top bar : **horizontale en haut**, contenu minimal (titre + Déconnexion), uniquement dans la zone connectée.

## Périmètre

Inclus : design system (couleurs/typo/dimens via CompositionLocal), atoms (Text, Button, TextField, NumberField, PasswordField, Checkbox, Icon, Divider/Spacer), molecules (LabeledField, FormError, FieldGroup), organisms (AppTopBar, AppScaffold), migration des écrans auth + accueil, feature `user` (UserPage). Hors périmètre : composants interactifs avancés (Dropdown, Dialog, Snackbar…), dark mode, tests UI Compose, `GetCurrentUser` use case.

## Décisions de design

- **Socle thème :** design system maison via `CompositionLocal` (pas de dépendance au theming Material ; les primitives Material3 restent utilisables comme briques de rendu).
- **Palette :** neutres gris/beige + accent taupe foncé `#5B554C`.
- **Champs :** style Outlined (bordure complète, focus taupe, erreur brun-rouge, désactivé grisé).
- **Boutons :** 5 variants, pas de `size` paramétrable (largeur via `Modifier` à l'appel).
- **Navigation :** zone non-connectée (Login/Register, plein écran) vs zone connectée (rendue dans `AppScaffold` avec `AppTopBar`).
- **Composants bêtes ; pages intelligentes** (règle existante conservée).

## Architecture & fichiers

### 1. Design system — `presentation/shared/theme/`

- `AppColors.kt` — data class immuable des couleurs + `lightColors()` par défaut :
  `background #FAF8F4`, `surface #EFE9E1`, `beige #DDD5C8`, `border #B8AF9D`,
  `muted #8A8275`, `textSecondary #5B554C`, `text #33302B`,
  `primary #5B554C`, `onPrimary #FFFFFF`, `danger #A13D33`, `onDanger #FFFFFF`.
- `AppTypography.kt` — data class : `title`, `subtitle`, `body`, `label`, `caption` (`TextStyle`).
- `AppDimens.kt` — espacements `xs=4, sm=8, md=16, lg=24, xl=32` (dp) ; rayons `radiusSm=6, radiusMd=10` ; `borderWidth=1.5`, `borderWidthFocused=2`.
- `AppTheme.kt` — `CompositionLocal` pour colors/typography/dimens ; objet `AppTheme` exposant `AppTheme.colors/typography/dimens` ; composable racine `AppTheme(content)` fournissant les valeurs par défaut. **Seule source de vérité** du look.

### 2. Atoms — `presentation/shared/component/atomic/`

Tous bêtes, lisent `AppTheme`, exposent `modifier: Modifier = Modifier`.

- `AppText` — `text`, `style: AppTextStyle = Body` (enum Title/Subtitle/Body/Label/Caption), `color: Color? = null`, `maxLines: Int = Int.MAX_VALUE`, `textAlign: TextAlign? = null`.
- `AppButton` (refonte) — `label`, `onClick`, `variant: ButtonVariant = Primary` (Primary/Secondary/Text/Danger/Ghost), `enabled = true`, `loading = false`, `leadingIcon: ImageVector? = null`. Couleurs par variant lues dans le thème ; `loading` → spinner + désactivation.
- `AppTextField` (refonte) — base de tous les champs : `value`, `onValueChange`, `label`, `placeholder: String? = null`, `errorMessage: String? = null`, `enabled = true`, `singleLine = true`, `leadingIcon: ImageVector? = null`, `trailingContent: @Composable (() -> Unit)? = null`, `visualTransformation = VisualTransformation.None`, `keyboardOptions = KeyboardOptions.Default`. Style Outlined taupe/beige + états focus/erreur/désactivé.
- `AppPasswordField` — s'appuie sur `AppTextField` ; masquage + toggle visibilité (œil) dans `trailingContent`.
- `AppNumberField` — s'appuie sur `AppTextField` ; filtre l'entrée via la fonction pure `filterNumericInput(raw, allowDecimal, allowNegative)` ; clavier numérique.
- `AppCheckbox` — `checked`, `onCheckedChange`, `label`, `enabled = true`.
- `AppIcon` — `imageVector`, `contentDescription: String?`, `tint: Color? = null`, `size: Dp` (défaut depuis dimens).
- `AppDivider` + helpers `VerticalSpacer(height)` / `HorizontalSpacer(width)`.

### 3. Molecules — `presentation/shared/component/molecule/`

- `LabeledField` — label (`AppText` Label) + slot `content: @Composable () -> Unit` + `FormError` ; espacement via dimens.
- `FormError` (refonte) — message d'erreur lisant `AppTheme.colors.danger`.
- `FieldGroup` — `Column` espacée pour grouper des champs.

### 4. Organisms — `presentation/shared/component/organism/`

- `AppTopBar` — bête : `title: String`, `onLogout: () -> Unit`, `modifier`. `Row` pleine largeur, fond `surface`, titre à gauche (`AppText` Title), bouton Déconnexion à droite (`AppButton` variant Text/Ghost).
- `AppScaffold` — `topBar: @Composable () -> Unit` + `content: @Composable () -> Unit`. `Column { topBar(); Box(Modifier.weight(1f)) { content() } }`. Garantit la top bar « partout » dans la zone connectée.

### 5. Feature user — `presentation/feature/user/`

- `page/UserPage.kt` — écran d'accueil connecté. Affiche un titre + les infos de l'`User` reçu (email si disponible). Page (peut appeler des use cases plus tard) ; pour l'instant affichage seul. `component/UserProfileCard.kt` extrait seulement si le contenu grossit (YAGNI).

### 6. Navigation — `presentation/navigation/Screen.kt` + `presentation/App.kt`

- `Screen` : `Splash`, `Login`, `Register`, et `User(user: User?)` (remplace `Home` ; porte l'`User` connecté ; `null` si arrivé par auto-login sans profil).
- `App.kt` : envelopper l'UI dans `AppTheme { }` (au lieu de `MaterialTheme`). Zone non-connectée (Login/Register) plein écran. Zone connectée rendue dans `AppScaffold(topBar = { AppTopBar(title = "E-JDR", onLogout = …) }) { UserPage(user) }`. `onLogout` appelle `LogoutUseCase` (existant) puis `screen = Screen.Login`.
- `LoginPage`/`RegisterPage` : `onAuthenticated` transporte l'`User` jusqu'à `App.kt` qui le stocke dans `Screen.User(user)`.

## Migration (consommation des composants)

- `LoginForm`/`RegisterForm` : utiliser `LabeledField` + `AppTextField`/`AppPasswordField`, `AppButton` (variant Primary pour soumettre, Text pour le lien secondaire), `FormError`.
- Placeholder « Bienvenue » supprimé → `UserPage`.
- **Refonte, pas duplication** : les `AppButton`/`AppTextField`/`LabeledTextField`/`FormError` actuels sont réimplémentés sur le thème (mêmes noms quand pertinent : `LabeledTextField` → remplacé par `LabeledField`), pas laissés en double.

## Tests

- Pas de tests UI Compose (cohérent avec la décision du scaffolding).
- `filterNumericInput(...)` (logique pure du `AppNumberField`) testée unitairement (JUnit) : entiers, décimaux, négatifs, caractères parasites, chaîne vide.

## Documentation

- KDoc française complète (rôle + tous les `@param`) sur chaque composant et chaque élément du thème.
- Mise à jour de `presentation/package.md` : section design system + catalogue des composants + navigation zone connectée/non-connectée.

## Conventions

- Conventional Commits, atomiques : `feat(presentation): add design system theme`, `feat(presentation): add reusable atoms`, `feat(presentation): add reusable molecules`, `feat(presentation): add top bar and app scaffold`, `feat(presentation): add user page`, `refactor(presentation): migrate auth screens to design system`, `test(presentation): cover numeric input filtering`, `docs(presentation): document component library`.

## Vérification (end-to-end)

1. `./gradlew build` compile + tests verts (dont `filterNumericInput`).
2. `./gradlew run` : écran Login en palette gris/beige, style Outlined.
3. Connexion (backend lancé) → `UserPage` avec top bar (titre + Déconnexion).
4. Bouton Déconnexion → retour à Login, session effacée.
5. Tous les écrans rendus avec la palette taupe/beige ; aucune couleur en dur (revue visuelle).

## Hors périmètre (plus tard)

Composants interactifs avancés (Dropdown, Select, Dialog, Snackbar, Switch, RadioGroup), dark mode / multi-thème, `GetCurrentUser` use case (profil après auto-login), onglets de navigation multi-pages, tests UI Compose, feature Host.
