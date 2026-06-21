# Détail de fiche réactif (canal `sheet:`) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rendre le détail d'une fiche de personnage réactif en temps réel via le canal WebSocket `sheet:{id}` — quand une fiche est mise à jour, les autres appareils qui l'affichent rechargent (ou affichent un bandeau si l'utilisateur édite).

**Architecture:** Le backend (déjà doté de la plomberie temps réel) notifie sur `sheet:{id}` à l'update et autorise l'abonnement à ce canal pour les membres du groupe. Le frontend (KMP) ajoute un service central `RealtimeSubscriptions` (envoi de frames subscribe/unsubscribe + re-souscription à la reconnexion) et rend `CharacterSheetDetailViewModel` réactif au bus d'invalidation.

**Tech Stack :** Backend Node 22 / TypeScript / Express / Vitest / `Result<T,E>` / path aliases (@application/@infrastructure/@domain/@presentation). Frontend Kotlin Multiplatform / Compose / Koin / Ktor / kotlinx.serialization / kotlin.test + StandardTestDispatcher.

## Global Constraints

- **Backend repo :** `C:\Users\yomdr\Documents\ProjetDev\Equipe\E-JDR\E-JDR-Backend`, branche `develop`. Commits respectent commitlint (sujet en minuscule). Husky (Prettier) pre-commit : si bloqué, `npx prettier --write <fichiers>` puis re-commit.
- **Frontend repo :** `C:\Users\yomdr\Documents\ProjetDev\Equipe\E-JDR\E-JDR-Frontend`, branche `feat/sheet-detail-realtime` (déjà créée depuis `main`). Commitlint **strict** : le sujet ne doit PAS commencer par une majuscule (ex. « ajouter le… » pas « Ajouter le… »). ESLint custom `ejdr/file-size` : max 500 lignes/fichier. `./gradlew.bat verifyDesktop` = detekt + build + tests desktop. Tests en `src/desktopTest/`.
- **Protocole WS serveur (figé, NE PAS modifier) :** client → serveur `{"type":"subscribe","channel":"sheet:X"}` / `{"type":"unsubscribe","channel":"sheet:X"}` ; serveur → client `{"type":"subscribed","channel":"..."}`, `{"type":"error","channel":"...","message":"..."}`, et invalidation `{"type":"invalidate","channel":"sheet:X","resource":"...","scopeId":"X"}`.
- **Resource string :** l'invalidation de détail utilise `resource = "character-sheet-detail"` (distinct de `"character-sheets"` utilisé par la liste).
- **Règle d'autorisation d'abonnement `sheet:{id}` :** membre du groupe de la fiche (`groupAccess.requireMember(userId, sheet.groupId)`). La modification reste réservée (propriétaire/MJ) — inchangée.

---

# PARTIE A — BACKEND (E-JDR-Backend, branche develop)

### Task 1: Port `SheetGroupLookup` + adaptateur sur le repo

**Files:**
- Create: `src/application/features/realtime/abstractions/SheetGroupLookup.ts`
- Create: `src/infrastructure/realtime/CharacterSheetGroupLookup.ts`
- Test: `tests/infrastructure/realtime/CharacterSheetGroupLookup.test.ts`

**Interfaces:**
- Produces: `interface SheetGroupLookup { groupIdOf(sheetId: string): Promise<string | null> }` ; classe `CharacterSheetGroupLookup` qui l'implémente à partir d'un `Pick<CharacterSheetRepository, "findById">`.

- [ ] **Step 1: Écrire le port (pas de test, simple interface)**

`src/application/features/realtime/abstractions/SheetGroupLookup.ts` :
```ts
/**
 * Résout le groupe d'une fiche à partir de son identifiant. Interface ségrégée :
 * l'autorisateur d'abonnement `sheet:` n'a besoin que de cela, pas de tout le repo.
 */
export interface SheetGroupLookup {
  /** Renvoie le groupId de la fiche, ou null si la fiche n'existe pas. */
  groupIdOf(sheetId: string): Promise<string | null>;
}
```

- [ ] **Step 2: Écrire le test de l'adaptateur**

`tests/infrastructure/realtime/CharacterSheetGroupLookup.test.ts` :
```ts
import { describe, it, expect } from "vitest";
import { CharacterSheetGroupLookup } from "@infrastructure/realtime/CharacterSheetGroupLookup";

describe("CharacterSheetGroupLookup", () => {
  it("renvoie le groupId d'une fiche existante", async () => {
    const repo = { findById: async () => ({ groupId: "g-1" }) as never };
    const lookup = new CharacterSheetGroupLookup(repo);
    expect(await lookup.groupIdOf("s-1")).toBe("g-1");
  });

  it("renvoie null si la fiche n'existe pas", async () => {
    const repo = { findById: async () => null };
    const lookup = new CharacterSheetGroupLookup(repo);
    expect(await lookup.groupIdOf("absent")).toBeNull();
  });
});
```

- [ ] **Step 3: Lancer le test → échec (module introuvable)**

Run: `npm test -- CharacterSheetGroupLookup`
Expected: FAIL (`Cannot find module .../CharacterSheetGroupLookup`)

- [ ] **Step 4: Écrire l'adaptateur**

`src/infrastructure/realtime/CharacterSheetGroupLookup.ts` :
```ts
import { CharacterSheetRepository } from "@application/features/character-sheet/abstractions/repositories/CharacterSheetRepository";
import { SheetGroupLookup } from "@application/features/realtime/abstractions/SheetGroupLookup";

/**
 * Implémente {@link SheetGroupLookup} en lisant la fiche via le repo character-sheet.
 * Ne dépend que de `findById` (interface ségrégée).
 */
export class CharacterSheetGroupLookup implements SheetGroupLookup {
  constructor(private readonly repo: Pick<CharacterSheetRepository, "findById">) {}

  public async groupIdOf(sheetId: string): Promise<string | null> {
    const sheet = await this.repo.findById(sheetId);
    return sheet?.groupId ?? null;
  }
}
```

- [ ] **Step 5: Lancer le test → succès**

Run: `npm test -- CharacterSheetGroupLookup`
Expected: PASS (2 tests)

- [ ] **Step 6: Commit**

```bash
git add src/application/features/realtime/abstractions/SheetGroupLookup.ts src/infrastructure/realtime/CharacterSheetGroupLookup.ts tests/infrastructure/realtime/CharacterSheetGroupLookup.test.ts
git commit -m "feat(realtime): port SheetGroupLookup pour autoriser l'abonnement sheet"
```

---

### Task 2: Autoriser l'abonnement au canal `sheet:` dans l'authorizer

**Files:**
- Modify: `src/infrastructure/realtime/WebSocketServer.ts` (interface `ChannelAuthorizerDeps` + cas `sheet:` de `RealtimeChannelAuthorizer.canSubscribe`)
- Modify: `src/infrastructure/realtime/buildRealtimeServer.ts` (ajouter le param `sheetGroupLookup` + le passer à l'authorizer)
- Modify: `src/main.ts:366-371` (passer `services.characterSheetRepository` enveloppé)
- Test: `tests/infrastructure/realtime/RealtimeChannelAuthorizer.test.ts`

**Interfaces:**
- Consumes: `SheetGroupLookup` (Task 1), `CharacterSheetGroupLookup` (Task 1), `GroupAccessService.requireMember` (existant).
- Produces: `ChannelAuthorizerDeps` gagne `sheetGroupLookup: SheetGroupLookup` ; `buildRealtimeServer(app, tokenProvider, groupAccessService, hub, sheetGroupLookup)`.

- [ ] **Step 1: Écrire les tests de l'autorisation `sheet:`**

`tests/infrastructure/realtime/RealtimeChannelAuthorizer.test.ts` (créer ; si un fichier de test de l'authorizer existe déjà, AJOUTER ces cas dans un `describe("sheet:")`) :
```ts
import { describe, it, expect } from "vitest";
import { RealtimeChannelAuthorizer } from "@infrastructure/realtime/WebSocketServer";
import { Result } from "@domain/shared/Result";

const ok = () => Result.success(undefined as never);
const ko = () => Result.failure({ message: "non membre" } as never);

describe("RealtimeChannelAuthorizer — sheet:", () => {
  it("autorise un membre du groupe de la fiche", async () => {
    const authorizer = new RealtimeChannelAuthorizer({
      groupAccess: { requireMember: async () => ok() },
      sheetGroupLookup: { groupIdOf: async () => "g-1" },
    });
    expect(await authorizer.canSubscribe("u-1", "sheet:s-1")).toBe(true);
  });

  it("refuse un non-membre du groupe de la fiche", async () => {
    const authorizer = new RealtimeChannelAuthorizer({
      groupAccess: { requireMember: async () => ko() },
      sheetGroupLookup: { groupIdOf: async () => "g-1" },
    });
    expect(await authorizer.canSubscribe("u-1", "sheet:s-1")).toBe(false);
  });

  it("refuse si la fiche n'existe pas (groupIdOf → null)", async () => {
    const authorizer = new RealtimeChannelAuthorizer({
      groupAccess: { requireMember: async () => ok() },
      sheetGroupLookup: { groupIdOf: async () => null },
    });
    expect(await authorizer.canSubscribe("u-1", "sheet:absent")).toBe(false);
  });
});
```
> Note : adapter `Result.success/failure` à l'API réelle du projet — vérifier le chemin et la forme dans `src/domain/shared/Result.ts` avant d'écrire (le test utilise `.isSuccess`). Si la forme diffère, ajuster `ok`/`ko` pour produire un objet dont `.isSuccess` vaut `true`/`false`.

- [ ] **Step 2: Lancer le test → échec**

Run: `npm test -- RealtimeChannelAuthorizer`
Expected: FAIL (le cas `sheet:` renvoie `false` en dur ⇒ le test « autorise un membre » échoue ; et `sheetGroupLookup` n'est pas dans `ChannelAuthorizerDeps` ⇒ erreur TS de compilation du test).

- [ ] **Step 3: Étendre `ChannelAuthorizerDeps` et le cas `sheet:`**

Dans `src/infrastructure/realtime/WebSocketServer.ts` :

Ajouter l'import en tête :
```ts
import { SheetGroupLookup } from "@application/features/realtime/abstractions/SheetGroupLookup";
```

Étendre l'interface :
```ts
export interface ChannelAuthorizerDeps {
  groupAccess: Pick<GroupAccessService, "requireMember">;
  sheetGroupLookup: SheetGroupLookup;
}
```

Remplacer le cas `sheet:` (la ligne `// sheet: affiné au Lot 3.` + `return false;`) par :
```ts
    if (parsed.kind === "sheet") {
      const groupId = await this.deps.sheetGroupLookup.groupIdOf(parsed.id);
      if (groupId === null) {
        return false;
      }
      const result = await this.deps.groupAccess.requireMember(userId, groupId);
      return result.isSuccess;
    }
    return false;
```

- [ ] **Step 4: Lancer le test → succès**

Run: `npm test -- RealtimeChannelAuthorizer`
Expected: PASS (3 cas sheet: + les cas user:/group: existants inchangés)

- [ ] **Step 5: Brancher `sheetGroupLookup` dans `buildRealtimeServer`**

Dans `src/infrastructure/realtime/buildRealtimeServer.ts` :

Ajouter l'import :
```ts
import { SheetGroupLookup } from "@application/features/realtime/abstractions/SheetGroupLookup";
```

Ajouter le paramètre et le passer à l'authorizer :
```ts
export function buildRealtimeServer(
  app: Application,
  tokenProvider: TokenProviderService,
  groupAccessService: GroupAccessService,
  hub: RealtimeHub,
  sheetGroupLookup: SheetGroupLookup,
): http.Server {
  const authorizer = new RealtimeChannelAuthorizer({
    groupAccess: groupAccessService,
    sheetGroupLookup,
  });

  const httpServer = http.createServer(app);
  attachWebSocketServer(httpServer, { hub, tokenProvider, authorizer });
  return httpServer;
}
```
(Mettre à jour le JSDoc : ajouter `@param sheetGroupLookup - Résout le groupe d'une fiche pour autoriser les abonnements sheet:.`)

- [ ] **Step 6: Brancher le lookup dans `main.ts`**

Dans `src/main.ts`, ajouter l'import :
```ts
import { CharacterSheetGroupLookup } from "@infrastructure/realtime/CharacterSheetGroupLookup";
```

Modifier l'appel `buildRealtimeServer` (lignes 366-371) :
```ts
  const httpServer = buildRealtimeServer(
    app,
    services.tokenProvider,
    groupAccessService,
    realtimeHub,
    new CharacterSheetGroupLookup(services.characterSheetRepository),
  );
```

- [ ] **Step 7: Build + tests complets**

Run: `npm run build && npm test`
Expected: build OK, suite verte (aucune régression). Si Prettier bloque au commit, `npx prettier --write` sur les fichiers modifiés.

- [ ] **Step 8: Commit**

```bash
git add src/infrastructure/realtime/WebSocketServer.ts src/infrastructure/realtime/buildRealtimeServer.ts src/main.ts tests/infrastructure/realtime/RealtimeChannelAuthorizer.test.ts
git commit -m "feat(realtime): autoriser l'abonnement au canal sheet pour les membres du groupe"
```

---

### Task 3: Notifier sur `UpdateCharacterSheetUseCase`

**Files:**
- Modify: `src/application/features/character-sheet/usecases/UpdateCharacterSheetUseCaseImpl.ts` (ajouter dep `realtimeNotifier` + appel après succès)
- Modify: `src/presentation/http/features/character-sheet/buildCharacterSheetControllers.ts` (passer `deps.realtimeNotifier` au constructeur de Update)
- Test: `tests/application/features/character-sheet/UpdateCharacterSheetUseCase.test.ts` (le fichier de test existant de ce use case — y ajouter les cas notifier ; sinon le créer)

**Interfaces:**
- Consumes: `RealtimeNotifier.notifySheetChanged(sheetId, resource)` (existant), `FakeRealtimeNotifier` (existant dans `tests/application/serviceFakes.ts`).
- Produces: `UpdateCharacterSheetUseCaseImpl` ctor gagne un dernier paramètre `realtimeNotifier: RealtimeNotifier`.

- [ ] **Step 1: Lire le test existant et le use case**

Lire `tests/application/features/character-sheet/UpdateCharacterSheetUseCase*.test.ts` (nom exact à confirmer) et `UpdateCharacterSheetUseCaseImpl.ts` pour connaître l'ordre exact des params du constructeur et le helper de construction du use case dans le test. Repérer le `FakeRealtimeNotifier` dans `tests/application/serviceFakes.ts` (méthode pour vérifier les appels, ex. `sheetChangedFor` / liste capturée — mirrorer le pattern utilisé par les tests de Create/Delete).

- [ ] **Step 2: Écrire le test « notifie sur update réussi »**

Ajouter dans le fichier de test du use case (adapter les noms du helper/fake au code réel) :
```ts
it("notifie le canal sheet après une mise à jour réussie", async () => {
  const notifier = new FakeRealtimeNotifier();
  const useCase = buildUpdateUseCase({ realtimeNotifier: notifier }); // helper local du fichier de test
  const result = await useCase.execute(validUpdateCommand);            // commande qui réussit
  expect(result.isSuccess).toBe(true);
  expect(notifier.sheetChanges).toContainEqual({
    sheetId: validUpdateCommand.characterSheetId,
    resource: "character-sheet-detail",
  });
});

it("ne notifie pas si la mise à jour échoue", async () => {
  const notifier = new FakeRealtimeNotifier();
  const useCase = buildUpdateUseCase({ realtimeNotifier: notifier });
  await useCase.execute(forbiddenUpdateCommand);  // commande refusée (non-éditeur) ou fiche absente
  expect(notifier.sheetChanges).toHaveLength(0);
});
```
> Note : `notifier.sheetChanges` est illustratif — utiliser la vraie API de capture de `FakeRealtimeNotifier`. Si le fake ne capture pas encore `notifySheetChanged`, l'étendre dans `serviceFakes.ts` pour enregistrer `{sheetId, resource}` (mirrorer la capture de `notifyUserChanged`).

- [ ] **Step 3: Lancer le test → échec**

Run: `npm test -- UpdateCharacterSheet`
Expected: FAIL (le use case ne prend pas encore `realtimeNotifier` / n'appelle pas `notifySheetChanged`).

- [ ] **Step 4: Ajouter la dépendance et l'appel**

Dans `UpdateCharacterSheetUseCaseImpl.ts` :
- Ajouter `import { RealtimeNotifier } from "@application/features/realtime/abstractions/RealtimeNotifier";` (vérifier le chemin exact via les imports de Create/Delete).
- Ajouter `private readonly realtimeNotifier: RealtimeNotifier,` en **dernier paramètre** du constructeur.
- Juste avant le `return Result.success(...)` final (après la persistance réussie dans l'`unitOfWork`), ajouter :
```ts
    this.realtimeNotifier.notifySheetChanged(command.characterSheetId, "character-sheet-detail");
```
(Vérifier le nom exact du champ id dans la commande : `command.characterSheetId` ou `command.id` — l'aligner sur le code réel.)

- [ ] **Step 5: Brancher le notifier dans le controller builder**

Dans `src/presentation/http/features/character-sheet/buildCharacterSheetControllers.ts`, à la construction de `new UpdateCharacterSheetUseCaseImpl(...)`, ajouter `deps.realtimeNotifier` comme **dernier argument** (mirrorer Create/Delete qui le passent déjà).

- [ ] **Step 6: Lancer les tests → succès**

Run: `npm test -- UpdateCharacterSheet`
Expected: PASS (notifie sur succès, ne notifie pas sur échec).

- [ ] **Step 7: Build + suite complète**

Run: `npm run build && npm test`
Expected: tout vert.

- [ ] **Step 8: Commit**

```bash
git add src/application/features/character-sheet/usecases/UpdateCharacterSheetUseCaseImpl.ts src/presentation/http/features/character-sheet/buildCharacterSheetControllers.ts tests/application/features/character-sheet/ tests/application/serviceFakes.ts
git commit -m "feat(realtime): notifier le canal sheet à la mise à jour d'une fiche"
```

---

# PARTIE B — FRONTEND (E-JDR-Frontend, branche feat/sheet-detail-realtime)

> Contexte technique vérifié : le `send` actuel de `KtorWebSocketTransport` encode `RealtimeEnvelopeDto{type, payload}` — ce qui produirait `{"type":"subscribe","payload":"…"}`, INCOMPATIBLE avec le serveur qui attend `{"type":"subscribe","channel":"sheet:X"}` à plat. La Task 4 ajoute donc un canal d'envoi de **frames de contrôle bruts**. Le hook `onReconnected` de `KtorRealtimeConnection` existe (aujourd'hui vide) et sera câblé en Task 5.

### Task 4: Envoi de frames de contrôle bruts (subscribe/unsubscribe)

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/application/features/realtime/abstraction/RealtimeConnection.kt` (ajouter `suspend fun sendRaw(text: String)`)
- Modify: `src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/RealtimeTransport.kt` (ajouter `suspend fun sendRaw(text: String)`)
- Modify: `src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/KtorWebSocketTransport.kt` (implémenter `sendRaw`)
- Modify: `src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/KtorRealtimeConnection.kt` (déléguer `sendRaw`)
- Test: `src/desktopTest/kotlin/eu/ejdr/infrastructure/realtime/KtorWebSocketTransportSendRawTest.kt` (si testable sans vrai socket ; sinon couvert indirectement via Task 5 — voir note)

**Interfaces:**
- Produces: `RealtimeConnection.sendRaw(text: String)` et `RealtimeTransport.sendRaw(text: String)` — envoient le texte brut tel quel sur le socket (frame texte), sans enveloppe.

- [ ] **Step 1: Ajouter `sendRaw` au port `RealtimeConnection`**

Dans `RealtimeConnection.kt`, ajouter dans l'interface :
```kotlin
    /**
     * Envoie un message de contrôle **brut** (texte JSON déjà sérialisé) tel quel, sans
     * enveloppe. Utilisé pour les frames subscribe/unsubscribe dont le format ({type, channel})
     * diffère de l'enveloppe métier {type, payload}.
     */
    suspend fun sendRaw(text: String)
```

- [ ] **Step 2: Ajouter `sendRaw` au `RealtimeTransport`**

Dans `RealtimeTransport.kt`, ajouter :
```kotlin
    /** Envoie un texte brut (déjà sérialisé) sur la session courante. */
    suspend fun sendRaw(text: String)
```

- [ ] **Step 3: Implémenter `sendRaw` dans `KtorWebSocketTransport`**

Dans `KtorWebSocketTransport.kt`, ajouter après `send(...)` :
```kotlin
    override suspend fun sendRaw(text: String) {
        val ws = session ?: return // pas de session ⇒ no-op toléré
        ws.send(text)
    }
```
(Note : `import io.ktor.websocket.send` est déjà présent. On choisit le no-op silencieux plutôt que `error(...)` car un envoi sur connexion absente — ex. reconnexion en cours — ne doit pas faire planter l'appelant.)

- [ ] **Step 4: Déléguer `sendRaw` dans `KtorRealtimeConnection`**

Dans `KtorRealtimeConnection.kt`, ajouter sous `send(...)` :
```kotlin
    override suspend fun sendRaw(text: String) {
        transport.sendRaw(text)
    }
```

- [ ] **Step 5: Compiler (les implémenteurs de `RealtimeConnection`/`RealtimeTransport` dans les tests devront aussi déclarer `sendRaw`)**

Run: `./gradlew.bat compileKotlinDesktop`
Expected: peut échouer sur les fakes de test existants (`FakeConnection`, `NoopConnection`, `ScriptedTransport`) qui n'implémentent pas `sendRaw`. Les corriger en ajoutant `override suspend fun sendRaw(text: String) = Unit` à chacun :
- `src/desktopTest/.../RealtimeCoordinatorTest.kt` → `FakeConnection`
- `src/desktopTest/.../RootStateTest.kt` → `NoopConnection`
- `src/desktopTest/.../KtorRealtimeConnectionTest.kt` → `ScriptedTransport` (transport : `override suspend fun sendRaw(text: String) = Unit`)

- [ ] **Step 6: Compiler + tests existants → verts**

Run: `./gradlew.bat compileTestKotlinDesktop && ./gradlew.bat desktopTest`
Expected: compile OK, tests existants verts (aucune régression).

- [ ] **Step 7: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/application/features/realtime/abstraction/RealtimeConnection.kt src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/RealtimeTransport.kt src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/KtorWebSocketTransport.kt src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/KtorRealtimeConnection.kt src/desktopTest/kotlin/eu/ejdr/infrastructure/realtime/ src/desktopTest/kotlin/eu/ejdr/presentation/RootStateTest.kt
git commit -m "feat(realtime): envoi de frames de contrôle bruts (sendRaw) pour subscribe"
```

---

### Task 5: Service `RealtimeSubscriptions` (registre + re-souscription)

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/application/features/realtime/abstraction/RealtimeSubscriptions.kt` (interface)
- Create: `src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/DefaultRealtimeSubscriptions.kt` (impl)
- Modify: `src/commonMain/kotlin/eu/ejdr/di/RealtimeModule.kt` (fournir le service + câbler `onReconnected`)
- Test: `src/desktopTest/kotlin/eu/ejdr/infrastructure/realtime/DefaultRealtimeSubscriptionsTest.kt`

**Interfaces:**
- Consumes: `RealtimeConnection.sendRaw(text)` (Task 4).
- Produces:
  - `interface RealtimeSubscriptions { fun subscribe(channel: String); fun unsubscribe(channel: String); suspend fun resubscribeAll() }`
  - `class DefaultRealtimeSubscriptions(connection, scope)` — envoie `{"type":"subscribe","channel":c}` / `{"type":"unsubscribe","channel":c}` ; `resubscribeAll()` réémet `subscribe` pour tous les canaux du set.

- [ ] **Step 1: Écrire l'interface**

`RealtimeSubscriptions.kt` :
```kotlin
package eu.ejdr.application.features.realtime.abstraction

/**
 * Registre central des abonnements temps réel actifs. Le ViewModel déclare les canaux
 * voulus ; le service envoie les frames subscribe/unsubscribe et réémet tous les abonnements
 * après une reconnexion (le serveur perd les abonnements à la coupure du socket).
 */
interface RealtimeSubscriptions {
    /** Demande l'abonnement à un canal (ex. "sheet:X"). Idempotent. */
    fun subscribe(channel: String)

    /** Annule l'abonnement à un canal. Idempotent. */
    fun unsubscribe(channel: String)

    /** Réémet un `subscribe` pour tous les canaux encore voulus (après reconnexion). */
    suspend fun resubscribeAll()
}
```

- [ ] **Step 2: Écrire le test**

`src/desktopTest/kotlin/eu/ejdr/infrastructure/realtime/DefaultRealtimeSubscriptionsTest.kt` :
```kotlin
package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.ConnectionState
import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.features.realtime.abstraction.RealtimeMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRealtimeSubscriptionsTest {

    private class RecordingConnection : RealtimeConnection {
        val sent = mutableListOf<String>()
        override val state: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)
        override val incoming = MutableSharedFlow<RealtimeMessage>()
        override suspend fun connect() = Unit
        override suspend fun send(message: RealtimeMessage) = Unit
        override suspend fun sendRaw(text: String) { sent.add(text) }
        override suspend fun disconnect() = Unit
    }

    @Test
    fun `subscribe envoie un frame subscribe à plat`() = runTest {
        val conn = RecordingConnection()
        val subs = DefaultRealtimeSubscriptions(conn, this)
        subs.subscribe("sheet:s-1")
        advanceUntilIdle()
        assertEquals(listOf("""{"type":"subscribe","channel":"sheet:s-1"}"""), conn.sent)
    }

    @Test
    fun `unsubscribe envoie un frame unsubscribe et retire du set`() = runTest {
        val conn = RecordingConnection()
        val subs = DefaultRealtimeSubscriptions(conn, this)
        subs.subscribe("sheet:s-1")
        subs.unsubscribe("sheet:s-1")
        advanceUntilIdle()
        assertEquals(
            listOf(
                """{"type":"subscribe","channel":"sheet:s-1"}""",
                """{"type":"unsubscribe","channel":"sheet:s-1"}""",
            ),
            conn.sent,
        )
        conn.sent.clear()
        subs.resubscribeAll()
        advanceUntilIdle()
        assertTrue(conn.sent.isEmpty(), "un canal désabonné ne doit pas être réémis")
    }

    @Test
    fun `resubscribeAll réémet tous les canaux encore voulus`() = runTest {
        val conn = RecordingConnection()
        val subs = DefaultRealtimeSubscriptions(conn, this)
        subs.subscribe("sheet:a")
        subs.subscribe("sheet:b")
        advanceUntilIdle()
        conn.sent.clear()
        subs.resubscribeAll()
        advanceUntilIdle()
        assertEquals(
            setOf(
                """{"type":"subscribe","channel":"sheet:a"}""",
                """{"type":"subscribe","channel":"sheet:b"}""",
            ),
            conn.sent.toSet(),
        )
    }
}
```

- [ ] **Step 3: Lancer le test → échec**

Run: `./gradlew.bat desktopTest --tests "*DefaultRealtimeSubscriptionsTest*"`
Expected: FAIL (compilation : `DefaultRealtimeSubscriptions` n'existe pas).

- [ ] **Step 4: Écrire l'implémentation**

`src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/DefaultRealtimeSubscriptions.kt` :
```kotlin
package eu.ejdr.infrastructure.realtime

import eu.ejdr.application.features.realtime.abstraction.RealtimeConnection
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implémentation par défaut : maintient l'ensemble des canaux voulus et envoie les frames
 * de contrôle via [RealtimeConnection.sendRaw]. Les envois sont lancés sur [scope] (les
 * appels publics sont non-suspendants pour rester simples côté ViewModel).
 *
 * @property connection Connexion temps réel (envoi des frames).
 * @property scope Portée portant les envois asynchrones.
 */
class DefaultRealtimeSubscriptions(
    private val connection: RealtimeConnection,
    private val scope: CoroutineScope,
) : RealtimeSubscriptions {

    private val mutex = Mutex()
    private val channels = mutableSetOf<String>()

    override fun subscribe(channel: String) {
        scope.launch {
            val added = mutex.withLock { channels.add(channel) }
            if (added) connection.sendRaw(frame("subscribe", channel))
        }
    }

    override fun unsubscribe(channel: String) {
        scope.launch {
            val removed = mutex.withLock { channels.remove(channel) }
            if (removed) connection.sendRaw(frame("unsubscribe", channel))
        }
    }

    override suspend fun resubscribeAll() {
        val snapshot = mutex.withLock { channels.toList() }
        for (channel in snapshot) {
            connection.sendRaw(frame("subscribe", channel))
        }
    }

    private fun frame(type: String, channel: String): String =
        """{"type":"$type","channel":"$channel"}"""
}
```
> Note : le `frame(...)` construit le JSON à la main (les valeurs `type`/`channel` sont des constantes/identifiants sans caractères à échapper). C'est volontaire pour garantir le format exact attendu par le serveur.

- [ ] **Step 5: Lancer le test → succès**

Run: `./gradlew.bat desktopTest --tests "*DefaultRealtimeSubscriptionsTest*"`
Expected: PASS (3 tests).

- [ ] **Step 6: Câbler le service + `onReconnected` dans `RealtimeModule`**

Dans `src/commonMain/kotlin/eu/ejdr/di/RealtimeModule.kt` :

Ajouter les imports :
```kotlin
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.infrastructure.realtime.DefaultRealtimeSubscriptions
import kotlinx.coroutines.runBlocking
```

Remplacer la définition de `RealtimeConnection` pour câbler `onReconnected`, et ajouter le service. Le service dépend de la connexion et la connexion (via onReconnected) dépend du service ⇒ casser le cycle en résolvant le service paresseusement dans le hook :
```kotlin
    single<RealtimeConnection> {
        KtorRealtimeConnection(
            scope = get(),
            transport = get(),
            onReconnected = { get<RealtimeSubscriptions>().resubscribeAll() },
        )
    }

    single<RealtimeSubscriptions> {
        DefaultRealtimeSubscriptions(connection = get(), scope = get())
    }
```
> Note : `onReconnected` est `suspend () -> Unit` et la lambda appelle `resubscribeAll()` (suspend) — le `get<RealtimeSubscriptions>()` est résolu à l'exécution du hook (après construction), ce qui évite le cycle de dépendances à la construction. Vérifier dans Koin que `get<...>()` dans la lambda compile (sinon capturer un `lazy { get<RealtimeSubscriptions>() }`).

- [ ] **Step 7: verifyDesktop**

Run: `./gradlew.bat verifyDesktop`
Expected: detekt + build + tests verts.

- [ ] **Step 8: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/application/features/realtime/abstraction/RealtimeSubscriptions.kt src/commonMain/kotlin/eu/ejdr/infrastructure/realtime/DefaultRealtimeSubscriptions.kt src/commonMain/kotlin/eu/ejdr/di/RealtimeModule.kt src/desktopTest/kotlin/eu/ejdr/infrastructure/realtime/DefaultRealtimeSubscriptionsTest.kt
git commit -m "feat(realtime): service d'abonnement central avec re-souscription à la reconnexion"
```

---

### Task 6: `CharacterSheetDetailViewModel` réactif

**Files:**
- Modify: `src/commonMain/kotlin/eu/ejdr/presentation/features/charactersheet/CharacterSheetDetailViewModel.kt`
- Test: `src/desktopTest/kotlin/eu/ejdr/presentation/features/charactersheet/CharacterSheetDetailRealtimeTest.kt`

**Interfaces:**
- Consumes: `InvalidationBus` (existant), `RealtimeSubscriptions` (Task 5).
- Produces: ctor du VM gagne, **en derniers paramètres**, `invalidationBus: InvalidationBus` et `subscriptions: RealtimeSubscriptions`. Nouveaux membres : `sheetChangedRemotely: StateFlow<Boolean>`, `fun reloadFromRemote()`, `fun dismissRemoteChange()`. Override `onCleared()`.

- [ ] **Step 1: Écrire les tests réactifs**

`src/desktopTest/kotlin/eu/ejdr/presentation/features/charactersheet/CharacterSheetDetailRealtimeTest.kt`. Construire le VM avec des fakes minimalistes des use cases (mirrorer le helper du `CharacterSheetDetailViewModelTest` existant s'il y en a un — lire d'abord pour réutiliser ses fonctions interfaces). Squelette :
```kotlin
package eu.ejdr.presentation.features.charactersheet

import eu.ejdr.application.features.realtime.abstraction.Invalidation
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterSheetDetailRealtimeTest {

    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class RecordingSubscriptions : RealtimeSubscriptions {
        val subscribed = mutableListOf<String>()
        val unsubscribed = mutableListOf<String>()
        override fun subscribe(channel: String) { subscribed.add(channel) }
        override fun unsubscribe(channel: String) { unsubscribed.add(channel) }
        override suspend fun resubscribeAll() = Unit
    }

    // buildVm(...) : helper local construisant CharacterSheetDetailViewModel avec des fakes
    // de tous les use cases (getById renvoie un nombre d'appels traçable), le bus et subs fournis.
    // Réutiliser/mirrorer le helper du test existant du VM détail.

    @Test
    fun `s'abonne au canal sheet à l'init`() = runTest {
        val subs = RecordingSubscriptions()
        buildVm(sheetId = "s-1", subscriptions = subs)
        advanceUntilIdle()
        assertEquals(listOf("sheet:s-1"), subs.subscribed)
    }

    @Test
    fun `recharge sur invalidation character-sheet-detail hors édition`() = runTest {
        val bus = InMemoryInvalidationBus()
        var loadCount = 0
        val vm = buildVm(sheetId = "s-1", bus = bus, onGetById = { loadCount++ })
        advanceUntilIdle()
        val before = loadCount
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "s-1"))
        advanceUntilIdle()
        assertTrue(loadCount > before, "doit recharger")
    }

    @Test
    fun `lève le bandeau sans recharger pendant l'édition`() = runTest {
        val bus = InMemoryInvalidationBus()
        var loadCount = 0
        val vm = buildVm(sheetId = "s-1", bus = bus, onGetById = { loadCount++ })
        advanceUntilIdle()
        vm.startEdit()
        val before = loadCount
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "s-1"))
        advanceUntilIdle()
        assertEquals(before, loadCount, "ne doit PAS recharger en édition")
        assertTrue(vm.sheetChangedRemotely.value)
    }

    @Test
    fun `ignore une invalidation d'un autre scopeId ou resource`() = runTest {
        val bus = InMemoryInvalidationBus()
        var loadCount = 0
        val vm = buildVm(sheetId = "s-1", bus = bus, onGetById = { loadCount++ })
        advanceUntilIdle()
        val before = loadCount
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "autre"))
        bus.emit(Invalidation(resource = "character-sheets", scopeId = "s-1"))
        advanceUntilIdle()
        assertEquals(before, loadCount)
    }

    @Test
    fun `reloadFromRemote recharge, baisse le flag et sort de l'édition`() = runTest {
        val bus = InMemoryInvalidationBus()
        val vm = buildVm(sheetId = "s-1", bus = bus)
        advanceUntilIdle()
        vm.startEdit()
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "s-1"))
        advanceUntilIdle()
        vm.reloadFromRemote()
        advanceUntilIdle()
        assertEquals(false, vm.sheetChangedRemotely.value)
        assertEquals(false, vm.isEditing.value)
    }

    @Test
    fun `dismissRemoteChange baisse le flag sans recharger`() = runTest {
        val bus = InMemoryInvalidationBus()
        var loadCount = 0
        val vm = buildVm(sheetId = "s-1", bus = bus, onGetById = { loadCount++ })
        advanceUntilIdle()
        vm.startEdit()
        bus.emit(Invalidation(resource = "character-sheet-detail", scopeId = "s-1"))
        advanceUntilIdle()
        val before = loadCount
        vm.dismissRemoteChange()
        advanceUntilIdle()
        assertEquals(false, vm.sheetChangedRemotely.value)
        assertEquals(before, loadCount)
    }
}
```
> Note : écrire `buildVm(...)` en lisant le test existant du VM détail (s'il existe) pour réutiliser ses fakes de use cases. `onGetById` est un compteur branché sur le fake `GetCharacterSheetUseCase`. Le désabonnement à `onCleared()` n'est pas trivial à déclencher en test unitaire (onCleared est `protected`) — le couvrir au mieux : si un utilitaire de test androidx permet d'appeler `onCleared`, l'utiliser ; sinon, documenter que le désabonnement est vérifié en validation runtime (Task 8) et tester seulement que `subscribe` a lieu à l'init.

- [ ] **Step 2: Lancer → échec**

Run: `./gradlew.bat desktopTest --tests "*CharacterSheetDetailRealtimeTest*"`
Expected: FAIL (ctor du VM n'accepte pas encore `invalidationBus`/`subscriptions`).

- [ ] **Step 3: Modifier le ViewModel**

Dans `CharacterSheetDetailViewModel.kt` :

Ajouter les imports :
```kotlin
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
```

Ajouter au constructeur, **en derniers paramètres** :
```kotlin
    private val invalidationBus: InvalidationBus,
    private val subscriptions: RealtimeSubscriptions,
```

Ajouter l'état (près des autres `_…` StateFlow) :
```kotlin
    private val _sheetChangedRemotely = MutableStateFlow(false)
    val sheetChangedRemotely: StateFlow<Boolean> = _sheetChangedRemotely.asStateFlow()
```

Dans le bloc `init { ... }`, après les launches existants, ajouter l'abonnement + la collecte :
```kotlin
        subscriptions.subscribe("sheet:$sheetId")
        viewModelScope.launch {
            invalidationBus.events.collect { invalidation ->
                if (invalidation.resource == "character-sheet-detail" && invalidation.scopeId == sheetId) {
                    if (_isEditing.value) {
                        _sheetChangedRemotely.value = true
                    } else {
                        load()
                    }
                }
            }
        }
```

Ajouter les actions et l'override (après `cancelEdit()` par exemple) :
```kotlin
    /** Recharge depuis le serveur (perd les modifs en cours) et sort de l'édition. */
    fun reloadFromRemote() {
        _sheetChangedRemotely.value = false
        _isEditing.value = false
        load()
    }

    /** Ignore la modification distante : garde la saisie en cours, baisse le bandeau. */
    fun dismissRemoteChange() {
        _sheetChangedRemotely.value = false
    }

    override fun onCleared() {
        subscriptions.unsubscribe("sheet:$sheetId")
        super.onCleared()
    }
```

- [ ] **Step 4: Lancer → succès**

Run: `./gradlew.bat desktopTest --tests "*CharacterSheetDetailRealtimeTest*"`
Expected: PASS.

- [ ] **Step 5: Corriger l'instanciation du VM dans les tests existants**

Si `CharacterSheetDetailViewModelTest` existe, son helper de construction casse (params manquants). Ajouter `invalidationBus = InMemoryInvalidationBus()` et `subscriptions = <fake noop>` (un objet implémentant `RealtimeSubscriptions` avec corps vides) à son helper.

Run: `./gradlew.bat desktopTest --tests "*CharacterSheetDetail*"`
Expected: tous verts.

- [ ] **Step 6: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/features/charactersheet/CharacterSheetDetailViewModel.kt src/desktopTest/kotlin/eu/ejdr/presentation/features/charactersheet/
git commit -m "feat(realtime): rendre le détail de fiche réactif aux invalidations sheet"
```

---

### Task 7: Bandeau « modifiée ailleurs » + branchement dans les 2 pages

**Files:**
- Create: `src/commonMain/kotlin/eu/ejdr/presentation/shared/component/molecule/RemoteChangeBanner.kt`
- Modify: `src/androidMain/kotlin/eu/ejdr/presentation/features/charactersheet/page/CharacterSheetDetailPage.kt`
- Modify: `src/desktopMain/kotlin/eu/ejdr/presentation/features/charactersheet/page/CharacterSheetDetailPage.kt`

**Interfaces:**
- Consumes: `CharacterSheetDetailViewModel.sheetChangedRemotely` + `reloadFromRemote()` + `dismissRemoteChange()` (Task 6) ; `InvalidationBus` et `RealtimeSubscriptions` du DI (Task 5).
- Produces: composable `RemoteChangeBanner(onReload: () -> Unit, onDismiss: () -> Unit, modifier)`.

- [ ] **Step 1: Créer le composant bandeau (molecule)**

`RemoteChangeBanner.kt` (s'inspirer du style de `FormError.kt` ; utiliser `AppText`, `AppButton`, couleurs `AppTheme.colors.surface`/`primary`) :
```kotlin
package eu.ejdr.presentation.shared.component.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import eu.ejdr.presentation.shared.component.atomic.AppButton
import eu.ejdr.presentation.shared.component.atomic.AppText
import eu.ejdr.presentation.shared.component.atomic.AppTextStyle
import eu.ejdr.presentation.shared.theme.AppTheme

/**
 * Bandeau inline signalant qu'une mise à jour distante de la fiche est arrivée pendant
 * l'édition. L'utilisateur choisit : recharger (perd ses modifs) ou ignorer (garde sa saisie).
 *
 * @param onReload Recharge la fiche depuis le serveur.
 * @param onDismiss Ferme le bandeau en gardant la saisie en cours.
 * @param modifier Modifier Compose appliqué au bandeau.
 */
@Composable
fun RemoteChangeBanner(
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimens.sm))
            .background(AppTheme.colors.surface)
            .padding(AppTheme.dimens.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = "Cette fiche a été modifiée ailleurs.",
            style = AppTextStyle.Body,
            color = AppTheme.colors.text,
            modifier = Modifier.weight(1f),
        )
        AppButton(text = "Recharger", onClick = onReload)
        AppButton(text = "Ignorer", onClick = onDismiss)
    }
}
```
> Note : vérifier les noms réels — `AppTheme.dimens.sm/md`, `AppTheme.colors.text`, signature de `AppButton` (peut-être `AppButton(onClick, text)` ou un slot). Lire `AppButton.kt` et un usage existant avant d'écrire, et aligner. Si `AppButton` n'a pas de variante « secondaire » pour « Ignorer », utiliser ce qui existe (un `TextButton`-like) ; sinon deux `AppButton` suffisent.

- [ ] **Step 2: Brancher dans la page Android**

Dans `src/androidMain/.../page/CharacterSheetDetailPage.kt` :
- Injecter les 2 deps dans le `koinViewModel { CharacterSheetDetailViewModel(... ) }` : ajouter `invalidationBus = get<InvalidationBus>(), subscriptions = get<RealtimeSubscriptions>(),` (imports correspondants).
- Collecter l'état : `val sheetChangedRemotely by viewModel.sheetChangedRemotely.collectAsStateWithLifecycle()`.
- Dans le `Column` du contenu, **juste sous le titre / au-dessus du contenu de la fiche**, ajouter :
```kotlin
        if (sheetChangedRemotely) {
            RemoteChangeBanner(
                onReload = viewModel::reloadFromRemote,
                onDismiss = viewModel::dismissRemoteChange,
            )
        }
```
- Ajouter les imports : `RemoteChangeBanner`, `InvalidationBus`, `RealtimeSubscriptions`.

- [ ] **Step 3: Brancher dans la page Desktop**

Idem dans `src/desktopMain/.../page/CharacterSheetDetailPage.kt` (même 3 modifications : injection des 2 deps, collecte de l'état, affichage du bandeau au même endroit).

- [ ] **Step 4: verifyDesktop**

Run: `./gradlew.bat verifyDesktop`
Expected: detekt + build + tests verts. (La page Android n'est pas compilée par verifyDesktop ; étape 5 la couvre.)

- [ ] **Step 5: Compiler la cible Android**

Run: `./gradlew.bat compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL (la page Android branche bien les nouvelles deps + le bandeau).

- [ ] **Step 6: Commit**

```bash
git add src/commonMain/kotlin/eu/ejdr/presentation/shared/component/molecule/RemoteChangeBanner.kt src/androidMain/kotlin/eu/ejdr/presentation/features/charactersheet/page/CharacterSheetDetailPage.kt src/desktopMain/kotlin/eu/ejdr/presentation/features/charactersheet/page/CharacterSheetDetailPage.kt
git commit -m "feat(realtime): bandeau de modification distante sur le détail de fiche"
```

---

### Task 8: Validation d'intégration (back déployé dev + front desktop+APK)

**Files:** aucun (validation runtime).

- [ ] **Step 1: Déployer le back sur dev**

Merger/pousser la branche back `develop` avec les Tasks 1-3, puis redéployer `ejdr-backend-dev` sur Vertex (la branche `develop` est l'env dev). Vérifier dans les logs au démarrage : `Serveur démarré (HTTP + WebSocket /ws)`.

- [ ] **Step 2: Lancer le desktop (pointe dev) + rebuild/réinstaller l'APK**

- Desktop : `./gradlew.bat run` (config.local pointe dev).
- Android : `./gradlew.bat assembleDebug` puis `adb install -r build/outputs/apk/debug/ejdr-frontend-debug.apk`.

- [ ] **Step 3: Test croisé**

Même compte, même groupe actif, **même fiche ouverte** sur desktop et mobile.
- Sur le téléphone : éditer un champ + enregistrer.
- Desktop (fiche ouverte, pas en édition) : la valeur doit se mettre à jour **toute seule**.
- Desktop **en édition** quand l'update arrive : le **bandeau** « modifiée ailleurs » apparaît ; « Recharger » charge la version distante, « Ignorer » garde la saisie.
- Vérifier l'autorisation : un membre du groupe reçoit les updates ; (optionnel) un non-membre ne peut pas s'abonner (logs serveur : `{type:"error"}`).

- [ ] **Step 4: Vérifier via logs serveur dev**

Après une édition, les logs `ejdr-backend-dev` doivent montrer la requête `PUT` réussie ; le rechargement (`GET`) côté clients abonnés suit. (Rappel : les logs Ktor client n'apparaissent pas dans adb logcat — valider via les logs serveur Vertex.)

- [ ] **Step 5: Si OK — finaliser**

Ouvrir la PR front `feat/sheet-detail-realtime` → `main` ; PR back `develop` (ou la stratégie habituelle). Mettre à jour la mémoire projet (feature livrée, ce qui reste : canal `group:` pour la liste).

---

## Notes transverses

- **Ordre conseillé :** Backend (1→2→3) d'abord et déployé dev, puis Frontend (4→5→6→7), puis validation (8). Le front a besoin du back pour le test runtime, mais peut être développé/compilé/testé en parallèle (les tests front sont unitaires, sans serveur).
- **Deux repos = deux suites de commits.** Les commits back vont sur `develop` (E-JDR-Backend) ; les commits front sur `feat/sheet-detail-realtime` (E-JDR-Frontend).
- **Hors périmètre (rappel) :** canal `group:` (liste « Mes fiches » réactive entre membres) = feature suivante ; elle réutilisera `RealtimeSubscriptions` (abonnement à `group:{groupe actif}`, réabonnement au changement de groupe actif).
