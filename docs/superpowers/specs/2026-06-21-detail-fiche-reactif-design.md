# Design — Détail de fiche réactif (canal `sheet:`)

**Date :** 2026-06-21
**Repos concernés :** E-JDR-Backend (Node/TS), E-JDR-Frontend (Kotlin Multiplatform)
**Statut :** validé, prêt pour plan d'implémentation

## Contexte

Le temps réel par invalidation WebSocket est en place et mergé sur `main` (front KMP desktop+Android) et déployé en dev (back). Le pilote **« Mes fiches »** (liste) est réactif : à la création/suppression d'une fiche, la liste des autres appareils **du même utilisateur** se rafraîchit, via le canal `user:{id}` (auto-abonné au login).

Limite constatée au test runtime : **le détail d'une fiche ouverte n'est pas réactif**. Quand quelqu'un édite et enregistre une fiche, les autres qui regardent la même fiche ne voient pas la mise à jour.

Cette feature rend le **détail** de fiche réactif via le **canal `sheet:{id}` dédié**, et pose au passage le **protocole d'abonnement dynamique** (subscribe/unsubscribe) côté front, qui n'existe pas encore et resservira pour toutes les futures features temps réel.

## Décisions de cadrage

| Sujet | Décision |
| --- | --- |
| **Canal** | `sheet:{id}` dédié, abonnement dynamique (subscribe à l'ouverture de la fiche, unsubscribe à la fermeture). |
| **Autorisation d'abonnement** | Tout **membre du groupe** de la fiche peut s'abonner et voir les mises à jour (= même règle que la lecture REST `requireMember(userId, sheet.groupId)`). La **modification** reste réservée (propriétaire ou MJ) — inchangé. |
| **UX édition en cours** | Si une invalidation arrive pendant l'édition (`isEditing == true`) : **ne pas recharger**, afficher un **bandeau inline** « Cette fiche a été modifiée ailleurs — recharger ? » (boutons Recharger / ignorer). Jamais d'écrasement silencieux de la saisie. |
| **Périmètre** | **Détail seul.** Le rafraîchissement de la **liste** « Mes fiches » entre membres (canal `group:`) est une **feature suivante**, hors périmètre ici. |
| **Service d'abonnement front** | Service central `RealtimeSubscriptions` (registre d'abonnements actifs) qui réémet tous les abonnements à la reconnexion WS. Pas d'appel direct `connection.send` depuis le ViewModel. |
| **Accès fiche pour l'autorisation back** | Petit port dédié `SheetGroupLookup` (interface ségrégée, une méthode `groupIdOf(sheetId)`), implémenté par le `CharacterSheetRepository`. |
| **Désabonnement front** | Via `onCleared()` du ViewModel (cycle de vie de la destination Navigation3). |
| **Bandeau** | Inline, en haut du contenu de la fiche (pousse le contenu, difficile à rater). |

## Flux complet

```
[Appareil A] édite + save fiche X
        │  PUT /character-sheets/X  (REST, inchangé)
        ▼
[BACK] UpdateCharacterSheetUseCase réussit
        │  realtimeNotifier.notifySheetChanged(X, "character-sheet-detail")   ← AJOUT
        ▼
[HUB] publie {type:"invalidate", channel:"sheet:X", resource:"character-sheet-detail", scopeId:"X"}
        │  → à tous les sockets abonnés au canal sheet:X
        ▼
[Appareil B] (fiche X ouverte ⇒ abonné sheet:X)
        │  RealtimeCoordinator → InvalidationBus → Invalidation("character-sheet-detail","X")
        ▼
[CharacterSheetDetailViewModel(X)]
        ├─ si !isEditing → load()  (recharge la fiche via getById)
        └─ si isEditing  → sheetChangedRemotely = true  (bandeau, n'écrase rien)
```

Abonnement : ouverture de la fiche X ⇒ `RealtimeSubscriptions.subscribe("sheet:X")` ⇒ `{type:"subscribe",channel:"sheet:X"}`. Fermeture ⇒ `unsubscribe`. Reconnexion WS ⇒ réémission de tous les abonnements actifs.

## Backend (E-JDR-Backend)

La plomberie temps réel existe déjà : `notifySheetChanged` (port + impl `WsRealtimeNotifier`), builders de canaux (`sheetChannel`/`parseChannel`), protocole serveur subscribe/unsubscribe (`handleMessage` dans `WebSocketServer.ts`), hub pub/sub. **Aucun de ces éléments ne change.**

### 4.1 Notifier sur update

- `UpdateCharacterSheetUseCaseImpl` : ajouter `realtimeNotifier: RealtimeNotifier` au constructeur. Après succès de la persistance (et seulement en cas de succès), appeler `realtimeNotifier.notifySheetChanged(sheetId, "character-sheet-detail")`.
- `buildCharacterSheetControllers.ts` : passer `deps.realtimeNotifier` au constructeur de `UpdateCharacterSheetUseCaseImpl` (la dep est déjà dans `CharacterSheetControllerDeps`, déjà passée à Create/Delete, manquante pour Update).

### 4.2 Autoriser l'abonnement `sheet:`

`RealtimeChannelAuthorizer.canSubscribe` (dans `WebSocketServer.ts`) renvoie aujourd'hui `false` en dur pour le cas `sheet:`. Nouveau comportement :

- Pour `sheet:{id}` : résoudre le `groupId` de la fiche via le nouveau port `SheetGroupLookup.groupIdOf(id)`. Si `null` (fiche absente) → refus. Sinon `groupAccess.requireMember(userId, groupId)` → autorisé ssi succès.
- Le port `SheetGroupLookup` (nouvelle abstraction dans la feature character-sheet ou realtime) :
  ```ts
  interface SheetGroupLookup {
    groupIdOf(sheetId: string): Promise<string | null>;
  }
  ```
  Implémenté par le `CharacterSheetRepository` (via `findById(sheetId)?.groupId ?? null`), ou par un adaptateur fin qui l'enveloppe. Injecté dans `RealtimeChannelAuthorizer` (en plus du `groupAccess` déjà présent).
- Wiring : la construction de `RealtimeChannelAuthorizer` (dans `buildRealtimeServer`/`main.ts`) reçoit ce port. **Point de vérification du plan** : confirmer que le `CharacterSheetRepository` (ou un équivalent) est accessible au point de construction de l'authorizer ; si la dépendance n'y est pas encore passée, l'ajouter à la signature de `buildRealtimeServer`.

### 4.3 Inchangé

Protocole serveur (subscribe/unsubscribe/subscribed/error), hub, `notifySheetChanged`, builders de canaux.

### Tests back (TDD)

- `UpdateCharacterSheetUseCaseImpl` appelle `notifySheetChanged(id, "character-sheet-detail")` après succès ; ne l'appelle PAS en cas d'échec (fiche absente, accès refusé, validation KO).
- `RealtimeChannelAuthorizer.canSubscribe` pour `sheet:` : membre → autorisé ; non-membre → refusé ; fiche inexistante (`groupIdOf → null`) → refusé. Les cas `user:`/`group:` restent inchangés (non-régression).
- `SheetGroupLookup` impl : renvoie le groupId d'une fiche existante, `null` sinon.

## Frontend (E-JDR-Frontend, KMP)

### 5.1 Nouveau service `RealtimeSubscriptions` (commonMain)

Registre central d'abonnements, singleton fourni par `realtimeModule`.

```kotlin
interface RealtimeSubscriptions {
    fun subscribe(channel: String)     // ex. "sheet:X"
    fun unsubscribe(channel: String)
}
```

Implémentation :
- Maintient un `Set<String>` thread-safe des canaux voulus.
- `subscribe(c)` : ajoute au set + `connection.send(RealtimeMessage("subscribe", payload="""{"channel":"$c"}"""))` (format aligné sur ce que parse le serveur : `{type, channel}`).
- `unsubscribe(c)` : retire du set + envoie `{type:"unsubscribe",channel:c}`.
- **Re-souscription** : branchée sur le hook `onReconnected` de `KtorRealtimeConnection` (présent, aujourd'hui vide) → réémet `subscribe` pour tous les canaux du set.
- Les envois passent par le `RealtimeConnection` existant ; un `send` sur connexion fermée échoue silencieusement (toléré).

> Note protocole : le serveur attend des frames JSON `{type, channel}`. Le `KtorWebSocketTransport.send` encode aujourd'hui un `RealtimeEnvelopeDto {type, payload}`. **À vérifier/aligner à l'implémentation** : il faut que le frame envoyé soit bien `{"type":"subscribe","channel":"sheet:X"}` à plat, pas `{"type":"subscribe","payload":"..."}`. Si l'enveloppe actuelle ne correspond pas, ajouter un encodage dédié pour les messages de contrôle (subscribe/unsubscribe) — c'est un point d'attention du plan, pas une inconnue de design (le contrat serveur est figé : `{type, channel}`).

Ajustement DI : câbler `onReconnected` du `KtorRealtimeConnection` (créé dans `realtimeModule`) pour déclencher la réémission via `RealtimeSubscriptions`.

### 5.2 `CharacterSheetDetailViewModel` réactif (commonMain)

Ajout au constructeur : `invalidationBus: InvalidationBus`, `subscriptions: RealtimeSubscriptions`.

- **init** : `subscriptions.subscribe("sheet:$sheetId")` ; collecte `invalidationBus.events`.
- **Sur invalidation** `resource == "character-sheet-detail" && scopeId == sheetId` :
  - `!isEditing` → `load()` ;
  - `isEditing` → `_sheetChangedRemotely.value = true`.
- **Nouvel état** : `sheetChangedRemotely: StateFlow<Boolean>`.
- **Nouvelles actions** :
  - `reloadFromRemote()` : `load()` + `_isEditing = false` + `_sheetChangedRemotely = false` (on sort de l'édition car le formulaire doit refléter les données fraîches).
  - `dismissRemoteChange()` : `_sheetChangedRemotely = false` (garde la saisie ; un `save` ultérieur écrasera — « ma version gagne », assumé).
- **onCleared()** (override) : `subscriptions.unsubscribe("sheet:$sheetId")`.

Filtre strict : invalidation pour un autre `scopeId` ou une autre `resource` → ignorée.

### 5.3 Pages (desktop + android)

- Injecter `invalidationBus = get()` et `subscriptions = get()` dans le `koinViewModel { CharacterSheetDetailViewModel(...) }` des **deux** pages.
- Afficher le bandeau inline en haut du contenu quand `sheetChangedRemotely == true` : texte « Cette fiche a été modifiée ailleurs », bouton **Recharger** (`reloadFromRemote()`), croix/ignorer (`dismissRemoteChange()`). Réutiliser un composant bandeau existant du design system s'il y en a un ; sinon créer un petit atomique cohérent (couleur d'accent/info du thème).

### Tests front (TDD)

- `RealtimeSubscriptions` : `subscribe` envoie un frame `{type:"subscribe",channel:c}` ; `unsubscribe` envoie `{type:"unsubscribe",channel:c}` ; la réémission (onReconnected) renvoie `subscribe` pour tout le set ; un canal désabonné n'est plus réémis.
- `CharacterSheetDetailViewModel` : s'abonne à `sheet:$id` à l'init ; se désabonne à `onCleared` ; recharge sur invalidation `character-sheet-detail`/`$id` hors édition ; lève `sheetChangedRemotely` (sans recharger) en édition ; `reloadFromRemote` recharge + baisse le flag + sort de l'édition ; `dismissRemoteChange` baisse le flag sans recharger ; ignore les invalidations d'un autre scopeId/resource.

## Gestion d'erreur & cas limites

- **Abonnement refusé** (non-membre / fiche supprimée) : serveur répond `{type:"error",channel:...}` → ignoré côté front (pas d'invalidation reçue ⇒ pas de rechargement auto ; la fiche reste lisible via REST). Dégradation propre.
- **WS coupé pendant que la fiche est ouverte** : à la reconnexion, `RealtimeSubscriptions` réémet `subscribe sheet:{id}`. Pas de trou.
- **Publication best-effort** : `notifySheetChanged` est déjà dans un try/catch (`WsRealtimeNotifier`) ; un échec du hub ne casse jamais l'update REST.
- **Invalidation d'une autre fiche** (`scopeId != sheetId`) : ignorée (filtre VM).
- **`onCleared` après déconnexion** : `unsubscribe` sur socket fermé = no-op toléré (le serveur a déjà nettoyé l'abonnement à la fermeture du socket).
- **Édition + reload** : `reloadFromRemote` sort de l'édition pour éviter que le formulaire garde l'ancienne saisie par-dessus les nouvelles données.

## Hors périmètre (features suivantes)

- Canal `group:` : rafraîchissement de la **liste** « Mes fiches » entre membres (nom modifié, création/suppression par un autre membre). Réutilisera `RealtimeSubscriptions` (abonnement à `group:{groupe actif}`, réabonnement au changement de groupe actif).
- Réactivité des autres écrans (invitations de groupe, sessions, etc.).

## Découpage en unités

- **Back** : (a) port `SheetGroupLookup` + impl ; (b) `canSubscribe` cas `sheet:` ; (c) notifier sur `UpdateCharacterSheetUseCase` + wiring controller. Chaque unité testable isolément.
- **Front** : (a) `RealtimeSubscriptions` (service + re-souscription) ; (b) VM détail réactif + états/actions ; (c) bandeau UI dans les 2 pages. Chaque unité testable isolément (le VM avec un faux `RealtimeSubscriptions` et un `InMemoryInvalidationBus`).
