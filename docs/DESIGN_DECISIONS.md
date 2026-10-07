# Design decisions and pattern changes

> **Draft notes.** These are the facts and arguments behind each design decision, written so they can be checked
> against the code. The report must be written in your own words; use this as a source, not as text to paste.

## 1. Pattern inventory: what is really in the code

| Pattern | Where | Verdict |
|---|---|---|
| Abstract Factory | `ClassicRuleEngineFactory`, `LudoTRuleEngineFactory` build a whole rule set per variant | Genuine: one family of related rule objects per variant |
| Builder | `GameBuilder` (seed, players, variant, event bus) | Genuine |
| Strategy | `PlayerStrategy` (Red/Green/Yellow/Blue); `RandomProvider` (seeded, fixed for tests) | Genuine. `DecisionSource` is **not** a Strategy: it is a label enum (`HUMAN`, `AI`, `SCRIPTED`) stored with each recorded decision |
| Command | `Move` and its subclasses, executed by `MoveExecutor`; room requests queued to the actor | Partly genuine. Moves are objects with `execute()` (the position change), but the rule handling (captures, mystery cells, direction, events) lives in `MoveExecutor`, which dispatches on the move type with `instanceof`; there is no undo |
| State | `PieceState` (base, on board, home straight, briefing, home); room status; turn phase (`GamePhase`) | Genuine |
| Observer | `EventBus` per game; the room's visitor and the console listener subscribe | Genuine |
| Visitor | `EventVisitor` implemented by `ConsoleOutputListener` (text) and `EventDtoVisitor` (network DTOs) | Genuine: two operations over one closed hierarchy of events |
| Active Object / Actor | one `RoomActor` thread and bounded queue per room | Genuine (architectural, not a GoF pattern) |
| Producer-Consumer | connection outbound queues, room command queues, database write queue | Genuine |
| Write-behind adapter | `AsyncGameRepository` wraps a `BatchWriter` | Called "Decorator" earlier. It does not decorate the same interface to add behaviour; it adapts a synchronous writer to an asynchronous queue. Call it a write-behind adapter |
| Singleton | `Feedback` (client sounds and effects) | The only one left; see 2.6 |

## 2. Patterns that were removed, replaced or never built, with the reason

### 2.1 Board Singleton → one `Board` per game (commit `6a895e2`)
- **Was:** the console game had one global `Board`.
- **Problem:** a server runs many games at once. A shared board means every room shares cells and pieces, so games
  would corrupt each other; tests also leaked state through it.
- **Now:** each game builds its own `Board` and each `Piece` holds a reference to it (dependency injection).
- **Trade-off:** a little more wiring (`GameBuilder` passes the board around); in return rooms are independent and
  tests build a fresh board each time.

### 2.2 Recursive bonus-roll turn → phased engine (commit `fca1515`)
- **Was:** one call played a whole turn, rolling again recursively on a six or a capture, blocking until the end.
- **Problem:** a network player must be asked for each roll and each move; a recursive method cannot pause for a
  human, a timer or an AFK take-over.
- **Now:** `GameEngine.roll()` and `applyMove()` are separate phases (`GamePhase`), and `run()` loops over them for
  AI-only games. The same engine serves the console, AI seats, humans and replays.
- **Trade-off:** the engine now has explicit state (`currentIndex`, `phase`, `stepNumber`), which is more to reason
  about, but every request can be validated against the phase.

### 2.3 Planned shared `ai` module → strategies stay in `core` (ADR-06 no longer matches the code)
- **Planned:** a dependency-free module of strategies choosing among *descriptor records*, used by the server and by
  network bots.
- **Built:** the original `PlayerStrategy` classes run on the server (AI seats and bot matches) because they need the
  live game state; the `bot` module has its own scoring strategies (`BotStrategies`) that choose among the
  `MoveOption` descriptors the server sends. The descriptors exist (`MoveOptionMapper`), the shared module does not.
- **Why:** moving the strategies would change every golden log for no visible gain, and a network bot cannot see the
  rules anyway.
- **Consequence to state honestly:** network bots are an approximation of the specified behaviour (Blue is random);
  the specified behaviour (spec 2.1) is the one in `core`.

### 2.4 One `EventBus` per room, and a second Visitor
- **Why per room:** an event bus shared by all games would deliver one room's events to another room's subscribers.
- **Why a second Visitor (`EventDtoVisitor`):** the same events must become network messages and recorded history.
  A visitor adds that operation without touching the event classes or the console visitor.

### 2.5 Module split with compiler-enforced dependencies
`core` and `protocol` depend on nothing; `server` on both; `client` and `bot` only on `protocol`. Each module is
compiled with only its allowed jars on the class path, so an illegal import does not compile. The cost is more
files and a mapping layer (`DtoMapper`, `MoveOptionMapper`); the gain is that a client cannot depend on the rules or
on server internals, which is the point of a multi-tier design.

### 2.6 Persistence: actor and write-behind instead of writing in the move path
The database is slow and far away (about 60 ms to the hosted database). Writing in the room's thread would make every
move wait for it. Moves go onto a queue and a background thread writes them in batches with retry; the writes are
idempotent. The cost is that a crash can lose the last few queued writes. A replay re-simulates the game from its seed and
the stored decisions and checks every roll and move against the record: if one disagrees (a lost write, or a game
recorded under older rules) it ends as `DIVERGED` instead of showing a wrong game.

### 2.7 `Feedback` singleton (client)
A global access point for sounds and visual effects. It is convenient (any view can call it) but hides a dependency
and makes views harder to test. Accepted because it holds no game state, only UI preferences (sound and effect switches). A cleaner version injects it through the screen constructors.

## 3. Known design debts (stated, not hidden)
- `ClientApp` is large and coordinates many screens; screens could be split further.
- `MoveExecutor` dispatches on the `Move` subclass with `instanceof`; moving the rule handling into the moves
  would make the Command pattern fuller but would change a lot of code.
- Rules are concrete `Base*` classes injected directly; interfaces would make them easier to substitute.
- The core still prints through `ConsoleOutputListener` (kept for the console game and the golden logs).
- Network bots do not implement the exact player behaviour of the specification (see 2.3).

## 4. Statements in the design documents that no longer match the code
These are your report sources, so they were not edited. Check them before copying.
- `07-DECISIONS.md`, **ADR-06**: describes an `ai` module and `MoveOption` descriptors shared by both tiers (see 2.3).
- `07-DECISIONS.md`, **ADR-15**: calls `ScriptedDecisionSource` "the Strategy pattern again"; in the code the
  decision source is a label enum plus a scripted list of decisions.
- Anywhere the persistence adapter is called a **Decorator** (see section 1).
- Anywhere the original rules are called "unchanged": the rule corrections are listed in
  [RULES_INTERPRETATION](RULES_INTERPRETATION.md) and the golden logs were re-recorded once after them.
- `03-TECHNICAL-DESIGN.md`, `04-PROTOCOL.md`, `02-PHASE-PLAN.md`: re-read against the current protocol (reconnect, finished
  games and replay messages were added later) and the current module list.
