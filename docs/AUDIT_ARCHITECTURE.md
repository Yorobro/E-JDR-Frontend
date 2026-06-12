# Rapport d'audit — Architecture du Frontend E-JDR

> **Périmètre** : `E-JDR-Frontend/` — application **desktop** Kotlin (JVM 21) + Compose for
> Desktop, Ktor client, Koin, Navigation 3.
> **Objectif** : juger l'application au regard des critères d'une bonne application
> (scalabilité, cohérence, maintenabilité), et lister les axes d'amélioration.
> **Méthode** : lecture exhaustive des 4 couches (domain / application / infrastructure /
> presentation), de l'outillage (tests, detekt, Kover, CI/CD) et vérification directe des
> points critiques.
> **Date** : 2026-06-12.

---

## 0. Verdict en une phrase

Architecture **clean/hexagonale rigoureuse et déjà très saine** : séparation des couches
nette, ports/adaptateurs corrects, type railway `Result`, sécurité soignée, WebSocket prêt,
CI verte sans violation detekt. **Excellente base pour 3-6 features.** Les limites de
scalabilité sont **connues et localisées** (navigation centralisée, état global ad-hoc,
modules DI à plat, quelques incohérences de signatures entre features) — toutes corrigeables
sans refonte.

**Note globale : 8/10** — « production-grade pour un MVP, à durcir avant la montée en charge ».

---

## 1. Les critères d'une bonne application (grille de notation)

| # | Critère | Définition | Note |
|---|---------|-----------|:----:|
| C1 | **Séparation des responsabilités** | Couches étanches, dépendances unidirectionnelles vers l'intérieur | 9/10 |
| C2 | **Cohérence interne** | Mêmes patterns d'une feature à l'autre, conventions uniformes | 7/10 |
| C3 | **Testabilité** | Logique isolée du framework, seams, injection par constructeur | 9/10 |
| C4 | **Scalabilité structurelle** | Ajouter une feature/un écran = effort prévisible et borné | 7/10 |
| C5 | **Gestion d'erreurs** | Erreurs métier typées, pas d'exception qui fuit, messages maîtrisés | 8/10 |
| C6 | **Sécurité** | Secrets non en clair, crypto correcte, surface d'attaque réduite | 8/10 |
| C7 | **Design system / UI** | Atomic design, tokens centralisés, thème, composants réutilisables | 8/10 |
| C8 | **Gestion de l'asynchrone** | Concurrence structurée, annulation propre, pas de fuite de coroutine | 9/10 |
| C9 | **Découplage / DI** | Composition root claire, dépendances explicites | 7/10 |
| C10 | **Outillage & qualité** | Lint, couverture, CI/CD, conventions de commit, docs | 8/10 |
| C11 | **Portabilité** | Pas de couplage dur à un OS/environnement | 5/10 |
| C12 | **Documentation** | ADR, docs de package, README à jour | 9/10 |

---

## 2. Forces structurantes (ce qui est excellent)

### 2.1 — Clean architecture réellement appliquée (C1, C3)
Quatre couches sous `eu.ejdr`, dépendances `presentation → application → domain` et
`infrastructure → application/domain`. La convention transverse **`shared/` vs
`features/<feature>/`** est appliquée dans chaque couche : ajouter une feature = créer le
même dossier partout, structure **prévisible et auto-documentée**.
Aucune fuite de type infra (Ktor `HttpClient`/`HttpResponse`/`HttpStatusCode`) vers les
ports : seuls des types domaine (`User`, `AuthError`, `UpdateInfoDto`) remontent.

### 2.2 — Type `Result<T, E : DomainError>` railway-oriented (C5)
`application/shared/Result.kt` : `Success`/`Failure` scellés, variance `out`, contrainte
`E : DomainError` qui **force chaque feature à modéliser ses erreurs**. Aucune exception ne
traverse vers la présentation. Les erreurs sont des `sealed class` par feature
(`AuthError` : 7 variantes avec message utilisateur ; `Unknown(detail)` isole le détail
technique de la fuite serveur).

### 2.3 — Concurrence structurée et annulation propre (C8)
Helper `runCatchingCancellable` (re-`throw` de `CancellationException`) utilisé
systématiquement (`AuthHttpRepository`, `KtorRealtimeConnection`). Les ViewModels lancent
dans `viewModelScope` → pas de fuite de coroutine.

### 2.4 — Sécurité soignée (C6)
- `refresh_token` **seul** persisté, chiffré **AES-GCM** (IV aléatoire 96 bits par
  chiffrement, tag 128 bits, IV préfixé) ; `access_token` reste en mémoire.
- Mot de passe du KeyStore JCEKS **dérivé** d'attributs locaux (jamais en dur).
- Dégradation gracieuse : coffre illisible → reset + reconnexion (pas de crash).
- Intercepteur 401 transverse (refresh silencieux + rejeu) dans `KtorClientFactory`.

### 2.5 — Couche temps réel prête sans dette (C4)
`RealtimeConnection` (port) + `KtorRealtimeConnection` (machine à états avec backoff
exponentiel + jitter) + `RealtimeTransport` (seam testable) + `KtorWebSocketTransport`
(auth-on-connect). Réutilise **à dessein** un canal distinct de l'intercepteur REST. Aucune
feature ne le consomme encore : c'est une fondation, pas du code mort.

### 2.6 — Design system cohérent (C7)
Atomic design (`atomic`/`molecule`/`organism`), thème via `CompositionLocal`
(`AppTheme.colors/typography/dimens`, `staticCompositionLocalOf`), light/dark, composants
atomiques réellement « bêtes ». Tokens centralisés (pas de valeurs en dur dans les
composants).

### 2.7 — Outillage et qualité (C10, C12)
detekt **0 violation**, Kover (plancher 60 %), alias `./gradlew verify` = exactement la CI,
Conventional Commits (commitlint + husky), semantic-release + build natif Windows.
**Documentation exemplaire** : un ADR (domaine anémique côté front justifié) + un
`package.md` par couche. ViewModels testés **sans Compose** (MockK, `runTest`, assertions
StateFlow/Channel) — la logique de présentation est couverte.

---

## 3. Faiblesses & risques de scalabilité (ce qui limite)

> Classées par sévérité. Aucune n'est bloquante aujourd'hui ; toutes deviennent gênantes en
> grandissant.

### 🔴 Important

**F1 — Navigation centralisée : `AppNavDisplay` deviendra un goulot (C4)**
`presentation/navigation/AppNavDisplay.kt` concentre **tout** le mapping route→écran dans un
seul `entryProvider`. Ajouter une route impose **3 à 4 modifications coordonnées** :
`Route` (`Routes.kt:22`), `subclass(...)` dans `appNavConfiguration` (`Routes.kt:55` — oubli =
**crash au démarrage**), `entry<…>` dans `AppNavDisplay`, et parfois l'orchestration dans
`App.kt`. À 15-20 écrans ce fichier devient volumineux et conflictogène.
→ *Mitigation* : enregistrement des entries **par feature** (chaque feature expose son/ses
`entry<>` agrégés), et idéalement une `Route` qui s'auto-enregistre dans la config de
sérialisation.
*Nuance positive* : les routes sont déjà des `NavKey` `@Serializable` et le code anticipe
explicitement `Campaign(id)` via `data class` — **le passage de paramètres typés est prévu,
pas bloqué**, juste pas encore exercé.

**F2 — État global géré en ad-hoc dans `App.kt` (C4, C9)**
Le thème vit dans un `mutableStateOf` au sommet de `App.kt` ; l'utilisateur courant n'a pas
de point d'ancrage global. À mesure que des états transverses apparaîtront (profil, préfs,
session, notifications), le « state lifting » manuel ne tiendra pas.
→ *Mitigation* : un `AppStateHolder`/ViewModel racine (ou store) pour session + thème + user.

**F3 — `release.yml` ne dépend pas de la CI (C10)**
Le workflow de release peut tagger une version et builder les binaires **sans avoir rejoué
les tests**. Un commit cassé sur `main` peut produire une release cassée.
→ *Mitigation* : faire dépendre le job release d'une CI verte (ou n'autoriser `main` que via
PR à CI verte).

### 🟠 Moyen

**F4 — Incohérences de signatures entre features (C2)** — *le point le plus visible*
- `Result` n'expose **que `fold`** : pas de `map`/`flatMap`/`getOrElse`/`mapError`. Toute
  transformation devient verbeuse. (`application/shared/Result.kt:42`)
- **Settings** : `GetThemeUseCase` renvoie `ThemeVariant` **nu** alors que `SetThemeUseCase`
  renvoie `Result<Unit, SettingsError>` — deux conventions opposées dans **la même feature**.
- `ThemeRepository.setTheme()` renvoie `Boolean` au lieu d'un `Result`.
- **Update** : `downloadUpdate()` renvoie `java.io.File` et `DownloadAndInstallUpdateUseCase`
  ne renvoie pas de `Result` → une exception peut **s'échapper** vers la présentation, en
  rupture avec le pattern railway du reste de l'app.
- `AuthRepository.hasPersistedSession()` est **non-`suspend`** alors que tout le reste l'est.
→ *Mitigation* : aligner toutes les frontières use case/repo sur `Result<_, DomainError>` +
`suspend`, même quand l'implémentation est triviale (contrat stable face à l'évolution).

**F5 — Modules DI à plat (C9)**
`ApplicationModule` (10 `single`) et `InfrastructureModule` listent tous les bindings sans
découpage par feature. Aujourd'hui lisible ; au-delà de ~5-6 features, ces fichiers
deviennent des « god-modules ».
→ *Mitigation* : un module Koin **par feature** (`authModule`, `settingsModule`, …) agrégés
dans `AppKoin`. `RealtimeModule` montre déjà la bonne voie (1er module par-feature).

**F6 — `UpdateDialog` mélange état et vue (C2, C7)**
`organism/UpdateDialog.kt` embarque une mini machine à états de téléchargement directement
dans le composable, là où le reste de l'app utilise un ViewModel. Casse le pattern
« composant bête ».
→ *Mitigation* : un `UpdateViewModel` comme les autres features.

**F7 — Couverture mesurée vs réelle (C10)**
`presentation` est exclu de Kover : le plancher de 60 % s'applique **après** exclusion. Les
ViewModels (de la logique, pas de l'UI) sont **testés mais non comptés**. La métrique
« 60 % » sous-estime ou surestime selon la lecture — à clarifier.
→ *Mitigation* : sortir les ViewModels de l'exclusion (ce ne sont pas de l'UI Compose), ou
documenter explicitement la convention.

### 🟡 Mineur / à surveiller

- **F8 — Portabilité OS (C11)** : `WindowsSystemLauncher` (cmd/start) est codé en dur et
  enregistré inconditionnellement. Bloquant si macOS/Linux un jour.
  → factory par OS, ou `Desktop.getDesktop().open()`.
- **F9 — Sécurité, durcissement** : dérivation du mot de passe KeyStore en **SHA-256 brut**
  (préférer PBKDF2/Argon2) ; l'intercepteur 401 renvoie le **401 d'origine** quand le refresh
  échoue pour une raison réseau → la présentation voit `SessionExpired` au lieu de `Network`.
- **F10 — `FormState` défini mais inutilisé** : le conteneur générique de formulaire existe
  (et est documenté) mais aucun ViewModel ne s'en sert (chacun a son `*UiState`). À adopter
  ou à retirer pour éviter la confusion.
- **F11 — Pas de tests UI Compose** ni de route guard centralisé (acceptable au stade MVP,
  à prévoir avec les premières routes protégées).
- **F12 — `UpdateHttpRepository`** : pas de timeout de téléchargement, pas de contrôle
  d'espace disque (hang possible).

---

## 4. Tableau récapitulatif Force / Faiblesse par couche

| Couche | Forces | Faiblesses |
|--------|--------|-----------|
| **Domain** | Entités pures (anémie **assumée** par ADR) ; `SemanticVersion` = vrai VO ; erreurs `sealed` bien hiérarchisées | Pas de VO `Email`/`Password` ; pas de validation au constructeur (cohérent avec l'ADR « invariants côté serveur ») |
| **Application** | Use cases d'orchestration pure ; **aucun** appel use-case→use-case ; logique partagée via services ; ports sans fuite infra | `Result` limité à `fold` ; incohérences Settings (get/set) et Update (pas de `Result`) ; `hasPersistedSession` non-suspend |
| **Infrastructure** | Client Ktor factorisé ; mappers DTO↔domaine étanches ; crypto AES-GCM correcte ; WebSocket machine à états + backoff testable | Intercepteur 401 ambigu (réseau vs session) ; `WindowsSystemLauncher` non portable ; dérivation KeyStore SHA-256 brut ; pas de timeout download |
| **Presentation** | Smart/dumb net ; un ViewModel par feature (StateFlow + Channel events) cohérent ; atomic design + thème CompositionLocal | Navigation centralisée (F1) ; état global ad-hoc (F2) ; `UpdateDialog` smart ; `FormState` inutilisé |
| **Outillage** | detekt 0 violation ; `verify` = CI ; semantic-release ; docs/ADR exemplaires | Release sans CI (F3) ; presentation hors Kover (F7) ; pas de tests UI |

---

## 5. Plan d'action priorisé (pour « super scalable et cohérent »)

### Avant la prochaine grosse feature (rapide, fort impact cohérence)
1. **Uniformiser les contrats** (F4) : tout port/use case renvoie `Result<_, DomainError>` et
   est `suspend` ; enrichir `Result` de `map`/`flatMap`/`getOrElse`/`mapError`. *(petit, très
   rentable — supprime la dette de cohérence la plus visible)*
2. **Faire dépendre la release d'une CI verte** (F3). *(config CI, faible effort)*
3. **Découper la DI par feature** (F5), en suivant `RealtimeModule`.

### Avant de dépasser ~6 écrans (scalabilité structurelle)
4. **Décentraliser la navigation** (F1) : chaque feature contribue ses `entry<>` + son
   enregistrement de sérialisation ; introduire les `Route(data class)` paramétrées dès le
   premier écran de détail (campagne/perso).
5. **Introduire un état applicatif global** (F2) : ViewModel/store racine pour session, user,
   thème.
6. **`UpdateViewModel`** (F6) pour réaligner `UpdateDialog` sur le pattern.

### Durcissement (quand le besoin se confirme)
7. Portabilité OS du `SystemLauncher` (F8) si cible multi-OS.
8. PBKDF2/Argon2 + désambiguïser l'intercepteur 401 (F9) ; timeout download (F12).
9. Adopter ou retirer `FormState` (F10) ; premiers tests UI Compose + route guard (F11).

---

## 6. Conclusion

Le frontend E-JDR est une **application desktop de très bonne facture** : les fondations
(clean architecture, railway errors, concurrence structurée, sécurité, WebSocket, outillage,
docs) sont **solides et cohérentes**. Ce n'est pas un prototype — c'est une base
**production-grade pour son périmètre actuel** (auth + settings + update).

Pour atteindre l'objectif « **super scalable et cohérent** », l'effort ne porte pas sur une
refonte mais sur **trois leviers ciblés** : (1) **uniformiser les contrats** entre features
(la seule dette de cohérence réelle), (2) **décentraliser navigation et DI** par feature, et
(3) **introduire un état applicatif global**. Aucun de ces chantiers n'est lourd ; menés
avant la montée en nombre d'écrans, ils font passer la note de cohérence/scalabilité de
7/10 à 9/10.
