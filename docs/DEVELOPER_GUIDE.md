# LUDO-T Online: developer guide

A multi-user, multi-tier, client-server version of **LUDO-T**, a modified Ludo with mystery cells, effects and
blockades. Several people play together from JavaFX clients, AI players fill the empty seats, anyone else can
watch and chat, and every game is stored in a PostgreSQL database.

It grew out of a single-machine console simulation. Its Clean Architecture is kept, and the rules were checked against the
specification and corrected where the original deviated (listed in [RULES_INTERPRETATION](RULES_INTERPRETATION.md)); the
golden-log tests replay five seeded games and require the output to be byte-identical to the recordings made after
those corrections.

| | |
|---|---|
| **Play mode** | People claim colours and play; AI fills the other seats. Everyone else spectates. |
| **Bot match** | All four seats are AI and everyone watches. The room's host can pause, resume and change the speed for everyone. |
| **Chat** | Every room has its own chat, and the lobby has a global chat. Everyone sees messages in the same order. |
| **Turn timers** | A person has 30 s per phase; then the AI plays for them, and after two misses in a row the seat is "away" until they take it back. |

---

## Three applications, three tiers

```
 ┌──────────────────────┐   TCP :5555    ┌─────────────────────────────┐  JDBC / SSL  ┌─────────────────────┐
 │ JavaFX client (many) │◄──────────────►│ Server                      │◄────────────►│ PostgreSQL          │
 │ bots, stress clients │  Java records  │ rooms, lobby, AI, timers    │  one writer  │ (Supabase, or H2)   │
 └──────────────────────┘                └─────────────────────────────┘              └─────────────────────┘
        depends on: protocol only              depends on: core, protocol                 players, rooms, seats,
                                                                                          every step, chat
```

The **server is authoritative**: it rolls the dice, computes the legal moves and applies them. Clients only send
intentions ("roll", "choose move 2") and draw what the server reports.

| Module | Source | What it is | May use |
|---|---|---|---|
| `core` | `src/core` | The game itself: board, pieces, rules, strategies, events (Clean Architecture entities and use cases) | nothing |
| `protocol` | `src/protocol` | Messages, DTOs, error codes, the framed transport and the shared client connection | nothing |
| `server` | `src/server` | Rooms, lobby, connections, persistence, `ServerMain`, `DbTool` | `core`, `protocol` |
| `client` | `src/client` | The JavaFX GUI | `protocol` (and JavaFX) |
| `bot` | `src/bot` | Network bots and the stress client | `protocol` |

`compile.sh` builds each module with **only its allowed dependencies on the classpath**, so the compiler enforces
the architecture: for example, the client cannot even name a core class, so it cannot contain game logic.

### How concurrency is handled (the short version)

| Mechanism | Where | Why |
|---|---|---|
| One single-thread executor per room (an actor) with a bounded queue (10,000) | `RoomActor`, used by `GameSession` | All requests, timers and AI moves for a room are processed one at a time, in arrival order, so room state needs no locks. When the queue is full the sender gets `SERVER_BUSY`. |
| A second actor for the lobby | `LobbyService` | Room-list updates and lobby chat share one ordering. |
| Blocking I/O, one reader and one writer thread per connection; bounded outbound queue (1,000) | `ConnectionHandler` | A slow client is dropped without ever blocking a room. |
| A window of at most 256 unanswered requests per connection | `ConnectionHandler` | A client that pipelines faster than it can read is slowed by TCP instead of being dropped. |
| Turn numbers on every gameplay request, request ids for duplicates | `GamePlay`, `RoomActor` | Late timers, stale clicks and double-sends are rejected or replayed, never applied twice. |
| Asynchronous batched database writer (`db-writer`) | `AsyncGameRepository` | A database round trip to Supabase takes about 60 ms; a game never waits for it. |
| JavaFX Application Thread rule, `Platform.runLater`, `CompletableFuture` requests | client | The UI never blocks on the network. |

Threads are named (`acceptor`, `conn-reader-N`, `conn-writer-N`, `room-<id>`, `lobby`, `scheduler-N`,
`db-writer`), so they are easy to show in VisualVM or a thread dump.

---

## Requirements

| Tool | Version | Notes |
|------|---------|-------|
| Java JDK | 17 or newer | `java` and `javac` on the `PATH`. The code is compiled with `--release 17`. |
| Internet | once | `download-tools` fetches the JavaFX SDK, the JDBC drivers and the metrics tools |
| PostgreSQL | optional | A free [Supabase](https://supabase.com) project, or the built-in H2 fallback, or no database at all |

There is no Maven or Gradle: shell scripts compile everything.

---

## Quick start (macOS / Linux)

```bash
./download-tools.sh     # once: JavaFX SDK, PostgreSQL + H2 drivers, metrics tools
./compile.sh            # builds every module into dist/<module>.jar
./test.sh               # runs every module's tests (about 550)

./run-server.sh         # terminal 1: the server on port 5555 (games kept in memory)
./run-client.sh         # terminal 2: a GUI client; run it again for a second player
```

In the client: **Connect** (nickname 3-16 letters, digits or `_`; the server address is under *Server settings*) → **Create game** → take a seat → **Start game** →
**Roll** → click a glowing piece. A green dot with "Connected · 12 ms" (the round trip of a ping over the TCP connection) is shown in the lobby and the room; a dot beside each seated person shows whether they are connected, away or offline. **Sign Out** (lobby) returns to the first screen. **Finished Games** (lobby) lists the games stored in the database, newest first, and **Replay** plays one again with pause, three speeds and a progress bar. If the connection drops, the window stays where it is, the badge turns amber ("Reconnecting...") and the client retries for up to 40 s; on success the lobby or room is rebuilt and a held seat is restored. The **Event Log** tab can be filtered (Dice, Moves, Captures, Specials, Turns). Sounds are synthesised in code (no audio files) and the speaker and sparkle buttons in the headers switch sound and motion effects (piece thud, capture shake, dice rattle, winner confetti) on or off; both choices are remembered. With no audio device the game simply stays silent. A second client can join the same room, as a spectator or by taking a seat.

To watch the game with no one playing: create a **Bot match** and join it from a second window.

> **Windows:** every script has a `.bat` twin (`compile.bat`, `test.bat`, `run-server.bat`, `run-client.bat`,
> `run-bot.bat`, `run-bots.bat`, `run-stress.bat`, `run-dbtool.bat`, `run-h2.bat`, `download-tools.bat`). They are
> written to match the shell scripts but were **not run on Windows**; try them before relying on them.
> `run-evidence.sh` is shell-only (use Git Bash or WSL).

---

## Configuration

Copy `config/server.properties.example` to `config/server.properties` (git-ignored, because it holds the database
password). Every key is optional.

| Key | Default | Meaning |
|---|---|---|
| `server.port` | `5555` | TCP port |
| `db.mode` / `db.url` / `db.user` / `db.password` | none | The database. No `db.url` (or `db.mode=none`) keeps games in memory only |
| `turn.timeoutSeconds` | `30` | Time a person has to roll, then to move (0 = no timers) |
| `afk.timeoutsBeforeAfk` | `2` | Consecutive missed turns before the seat is played by the AI until taken back |
| `speed.fastMs` / `normalMs` / `slowMs` | `400` / `1000` / `2000` | Pause between AI actions |
| `room.queueCapacity` | `10000` | Commands one room may have waiting |
| `room.requestMemory` | `1024` | Recent answers remembered per player to replay a repeated request id |
| `room.emptyCloseMs` | `60000` | How long an empty room stays before it closes |
| `connection.outboundCapacity` | `1000` | Messages queued per client before it is dropped as too slow |
| `connection.maxInFlight` | `256` | Unanswered requests per connection before reading pauses |
| `connection.reconnectGraceMs` | `30000` | How long a dropped GUI client keeps its nickname, seat and identity so it can reconnect (0 = off); bots never use it |
| `connection.readTimeoutMs` | `15000` | Silence after which a client is dropped (clients ping every 5 s) |
| `chat.maxLength` / `historySize` / `rateLimitMessages` / `rateLimitWindowMs` | `200` / `30` / `5` / `5000` | Chat limits |
| `log.queueIntervalMs` | `1000` | How often room queue depths are logged while non-zero |

---

## The database

The server stores players, rooms, seats, **every roll and move** (numbered by the engine's step counter, so a
duplicate is impossible at the database level) and chat. Writes go through a queue and a single `db-writer`
thread that writes batches of up to 50 as one transaction; reads are never on the game's critical path.

### Option 1: Supabase (PostgreSQL)

1. Create a project (choose the region nearest you). Note the database password.
2. **Connect → Session pooler → JDBC**: copy host, port, database and user. The *session pooler* works over IPv4;
   the direct connection may be IPv6-only.
3. Put them in `config/server.properties`:
   ```
   db.mode=postgres
   db.url=jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require
   db.user=postgres.<project-ref>
   db.password=<password>
   ```
4. `./run-dbtool.sh reset` creates the tables, seeds two demo people and plays one real finished game into them.

### Option 2: offline H2 fallback (a separate process)

```bash
./run-h2.sh &                       # H2 server on port 9092, in PostgreSQL mode
# config/server.properties:
#   db.mode=h2
#   db.url=jdbc:h2:tcp://localhost:9092/./ludot;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
#   db.user=sa
./run-dbtool.sh reset
```

### DbTool

| Command | Does |
|---|---|
| `./run-dbtool.sh reset` | Drop, create, seed and play the demo game: **the demo's initial state**, in one command |
| `schema` / `seed` / `drop` | Run `db/schema.sql`, `db/seed.sql` or `db/reset.sql` |
| `demo` | Play and store one more finished game |
| `status` | Connection time, round-trip time and row counts |
| `verify [room]` | Check a finished game is stored completely (default: the latest) |

The SQL lives in `db/` and is run over JDBC, so neither `psql` nor the Supabase console is needed. The same SQL
runs unchanged on PostgreSQL and H2. Room numbers continue after the highest stored room when the server restarts.

---

## Automatic clients

**Bots** are separate network clients that play one colour with a per-colour style (Red goes for captures, Green
for blockades, Yellow races home, Blue is random). Create a Play room in the GUI, then:

```bash
./run-bots.sh R-0001                      # four bots, one per colour (they take every seat)
./run-bot.sh --room R-0001 --colour RED --delay 800 --host 192.168.1.20
```

**The stress client** floods a room with asynchronous requests from several threads: real moves, stale and
out-of-turn requests, duplicates, bad indexes, host-only actions from a non-host, snapshots and chat bursts. It
judges every answer against what that request *should* produce, and reports anything else as `UNEXPECTED`.

```bash
./run-stress.sh --create --name stressA --seat RED  --speed fast --start-game --start-at <epochMs>
./run-stress.sh --room R-0002 --name stressB --seat BLUE --start-at <epochMs>
# options: --requests 2000 --threads 4 --rate N --host H --port P --wait <seconds for the game to end>
```

Give both the same `--start-at` (epoch milliseconds, a few seconds ahead) so they start flooding together. The
report shows throughput, latency percentiles, outcome counts, `UNEXPECTED`, whether every update matched its own
state hash, a hash of the chat order (identical on both clients if ordering holds) and whether the final state
equals the server's.

**`./run-evidence.sh`** does all of it in one go for the report and the demo: it resets the database, starts the
server, runs two stress clients against a fresh room while two GUI spectators watch, takes a thread dump, checks
the database holds exactly what was played, and writes everything to `evidence/run-<time>/`.

---

## Two machines

1. On the server machine find its IP address and allow inbound TCP **5555** in the firewall.
2. On the second machine: `./run-client.sh --host <server-ip> --port 5555` (or type it on the connect screen).
3. Both clients now see the same room, with matching state hashes in the bottom status bar.

If the campus network blocks peer-to-peer traffic, use a phone hotspot.

Client options: `--host H --port P --nickname N --connect` (connect straight away), `--join R-0001` (enter a room),
`--seat RED` (take a seat after joining), `--screenshot file.png [--screenshot-after ms]` (save the window and exit).

---

## Tests and metrics

```bash
./compile.sh && ./test.sh
```

| Suite | What it covers |
|---|---|
| `core` (311) | The original unit tests, plus the **golden logs**: seeds 1, 2, 10, 11 and 2069 must reproduce the recorded output byte for byte; phased-turn tests |
| `protocol` (25) | Every message round-trips, oversized / malformed / disallowed input is rejected, state hash |
| `server` (225) | Rooms, validation order, chat, timers and AFK, the **race tests** (seat claim, duplicate ids, join under load, backpressure, chat ordering, rate limit, pause vs AI, timeout vs move), lobby races, real-socket integration, JDBC and the async writer on H2, end-to-end persistence |
| `client` (92) | Board geometry, state model and sequence-gap handling, countdown clock, how a room card and the connection light look, ping round-trip timing, event-log categories, the synthesised sounds and their mapping, confetti physics, reconnect retry rules and stale-connection detection, replay state and wording |
| `bot` (14) | Bot strategies, request mix, statistics |
| `integration` (3) | Four bots finish a game; two stress clients flood one room, all against a live server |

The concurrency tests fire from many threads behind a start gate and most are repeated 5-20 times.

**Metrics** (all five modules): run `./download-tools.sh` once, then `./run-metrics.sh`. It builds every module,
runs all their tests under JaCoCo into one coverage file, runs Checkstyle, PMD, SpotBugs and the cyclomatic-complexity
check over every module's sources, and ends with a per-module table (also saved to `reports/SUMMARY.txt`):

```
Module       Lines  Line cov Branch cov | Checkstyle    PMD  SpotBugs  Complexity
core          1695       93%       82% |         48    132       125           0
server        1807       90%       81% |          7    112         7           0
client        1185       16%       16% |          0     63        23           0
...
```

Reports go to `reports/` (`reports/jacoco/index.html` for coverage, one XML per tool, `tests-<module>.txt` for each
suite). The complexity column counts breaches of `complexity.xml` (cyclomatic complexity 10, NPath 200, 30 statements
per method, class fan-out 35) and is 0 for every module. Client coverage is low because the JavaFX screens are only
exercised by hand. Thresholds live in `checkstyle.xml`, `complexity.xml`, `pmd-ruleset.xml` and `spotbugs-include.xml`.
`run-metrics.bat` still analyses the `core` module only and is untested on Windows.

---

## Project layout

```
Ludo-T/
├── src/
│   ├── core/com/ludot/        # the game: model, rules, command, strategy, engine, events, effects, factory, dice
│   ├── protocol/com/ludot/protocol/   # messages, DTOs, MessageChannel, ServerLink, StateHash
│   ├── server/com/ludot/server/
│   │   ├── app/               # use cases and ports: GameSession (room facade) over RoomActor, Membership, Seating,
│   │   │                      #   GamePlay, TurnDriver, RoomChat, Broadcaster; LobbyService, RequestRouter, ...
│   │   ├── game/              # RoomGame: the only place that touches core types; DTO and event mapping
│   │   ├── adapters/          # net (sockets), jdbc, persistence (async writer), memory
│   │   └── GameServer, ServerMain, DbTool, DemoGameSeeder   # composition roots
│   ├── client/com/ludot/client/   # ClientApp, state/ (RoomModel, ClientState), view/ (screens, BoardView), net/
│   ├── client/resources/          # playful.css (connect screen, lobby, New game dialog) and the Lilita One font (SIL OFL, see fonts/OFL.txt)
│   └── bot/com/ludot/bot/     # BotClient, StressClient, strategies, statistics
├── test/<module>/             # tests mirror the modules; test/integration spans all of them
├── test-resources/golden/     # the five recorded game logs
├── db/                        # schema.sql, seed.sql, reset.sql
├── config/                    # server.properties.example (the real file is git-ignored)
├── lib/                       # JUnit and Mockito (committed); JavaFX SDK, JDBC drivers (downloaded, ignored)
├── out/, dist/, reports/, evidence/   # build and run output (git-ignored)
└── compile.sh test.sh run-*.sh        # and their .bat twins
```

---

## Known limitations

* Reconnecting works within the grace period only (default 30 s). While a player is away their seat is played by the AI; on return they get it back. After a server restart all tokens are gone, so everyone signs in again.
* Replays need the database (without one there is nothing stored to replay). A replay re-plays the game from its seed and stored decisions; if the engine ever disagrees with the record it stops as "diverged" instead of showing a wrong game.
* Identity is a nickname only (no accounts), and the link is plain TCP with Java serialization behind a class
  allow-list and a 1 MB frame cap. JSON over TLS would be the production choice.
* One server instance owns the database (room numbers and writes assume that).
* The GUI has no dark theme.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `JavaFX runtime components are missing` / client will not start | Run `./download-tools.sh`; start with `./run-client.sh`, which passes the JavaFX module path |
| `Address already in use` | Another server is on 5555: stop it or set `server.port` |
| `Cannot use the database` at server start | Check `config/server.properties`; the session pooler details, not the direct connection; or use the H2 fallback; or remove `db.url` to run without storage |
| Hello refused: `INVALID_NICKNAME` | Nicknames are 3-16 letters, digits or `_`, and must be unique among connected people |
| A client "was too slow" and disconnected | The server drops a client whose 1,000-message outbound queue fills; look for `outbound queue is full` in the server log |

---

# The LUDO-T rules and scenario seeds

The core game is still runnable on its own, with no server: `./run.sh <seed>` (or `run.bat <seed>` on Windows).
It plays four AI strategies against each other and prints a narrated log. With no seed it uses `42`. Save the
output with `./run.sh 1 > game_log.txt`.

The seeds below each produce a fully reproducible game that clearly demonstrates a rule. Run one and search
the output for the quoted line. The same seeds work in a **Bot match** (set the seed in *New room…*), and
the golden-log tests pin their output.

### Seed 1: core rules (T-1 to T-11, T-14, T-15, and counter-clockwise movement)

```bash
./run.sh 1
```

| Rule | What to look for in the output |
|------|-------------------------------|
| **T-1** CCW movement | `B1 exits base. Coin toss: Tails -> Counter-Clockwise` |
| **T-1** Approach cell pass | `R1 passes approach cell (pass count: 1)` |
| **T-2** Bonus roll on capture | `Red receives a bonus roll! (T-2)` |
| **T-3** Block formation | `R1 forms a blockade with R2 at cell 26 (T-3)` |
| **T-4** Block movement | `Blockade of 2 moves 2 steps (roll 5 / 2) (T-4)` |
| **T-5** Direction restored on break | `Blockade broken! R1 reverts to counter-clockwise (T-5)` |
| **T-6** Triple-six penalty | `Red rolled three consecutive 6s! Forced block break! (T-6)` |
| **T-7** Home entry rejected | `R1 cannot enter home straight (needs 1 more capture(s)) (T-7)` |
| **T-8** Block-vs-block capture | `Blockade of 2 moves 1 steps (roll 0 / 2) (T-4)` *(roll 0 marks a block-capture move)* |
| **T-9** Capture + return to base | `Red piece R2 lands on square 45, captures Yellow piece Y1, and returns it to the base.` |
| **T-10** Mystery cell spawns | `A mystery cell has spawned in location 13 and will be at this location for the next 4 rounds.` |
| **T-11** Mystery teleport | `B1 lands on mystery cell! Teleporting...` → `B1 teleported to Beta Cell (cell 25)` |
| **T-14** Gamma from counter-clockwise | `Blue piece B1 is moving in a counter-clockwise direction. Teleporting to Beta from Gamma.` |
| **T-10** Mystery cell moves on | `Mystery cell expires.` |
| Home straight entry | `R2 enters the home straight!` |
| Piece reaches home | `R2 reaches Home!` |

### Seed 2: Alpha (Sick) and Beta Briefing (T-12, T-13)

| Rule | What to look for in the output |
|------|-------------------------------|
| **T-12** Alpha → Sick | `Red piece R1 feels sick, and movement speed halves.` |
| **T-13** Beta → Briefing lock | `Blue piece B4 attends briefing and cannot move for four rounds.` |

### Seed 10: Alpha (Energised) (T-12)

| Rule | What to look for in the output |
|------|-------------------------------|
| **T-12** Alpha → Energised | `Red piece R3 feels energized, and movement speed doubles.` |

### Seed 11: Beta Briefing escape via triple-3 (T-13 escape)

The rarest scenario: a piece in briefing is present for three rolls of 3 in a row and escapes early.

| Rule | What to look for in the output |
|------|-------------------------------|
| **T-13** Briefing early escape | `Green piece G2 is movement-restricted and has rolled three consecutively. Teleporting piece G2 to base.` |

Seed `2069` is the demo game that `DbTool reset` stores: a complete game with blockades and mystery cells.

| Seed | Primary scenario |
|------|-----------------|
| `1` | T-1 CCW · T-2 Bonus · T-3/T-4 Block · T-5 Break · T-6 Triple-Six · T-7 Home reject · T-8 Block capture · T-9 Capture · T-10/T-11 Mystery · T-14 Gamma · T-15 Effects |
| `2` | T-12 Alpha Sick · T-13 Beta Briefing |
| `10` | T-12 Alpha Energised |
| `11` | T-13 Beta Briefing escape (triple-3) |
| `2069` | The stored demo game |
