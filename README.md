<p align="center"><img src="docs/images/icon.png" width="110" alt="LUDO-T Online icon"></p>

<h1 align="center">LUDO-T Online</h1>

<p align="center">
A multiplayer take on Ludo with a twist: blockades, mystery cells and special effects.<br>
Play against friends and AI from a JavaFX desktop client, on a server that handles many rooms at once.
</p>

<p align="center">
  <img src="docs/images/lobby.png" width="48%" alt="The lobby: a live list of games as cards">
  <img src="docs/images/room.png" width="48%" alt="A game room">
</p>

## What it is

LUDO-T is a modified Ludo (the rules come from a university coursework brief). This project turns the original
single-player console simulation into a **networked game**:

- a **desktop client** (JavaFX) with a lobby, game rooms, chat and replays,
- a **game server** that owns the rules and the state, so nobody can cheat from the client,
- an optional **PostgreSQL database** that stores every game so it can be replayed later,
- **bots and a stress tool** that act as automatic clients.

### What you can do

- Create a room, take a seat (red, green, yellow or blue), and let AI fill the empty seats.
- Play with people on other computers, or watch a game as a spectator, with room chat and a lobby chat.
- Watch **bot matches** at three speeds, with a "what just happened" panel and a filterable event log
  (Dice, Moves, Captures, Specials, Turns).
- **Reconnect** if your Wi-Fi drops: the window stays put, retries, and gives you your seat back.
- Open **Finished Games** and **replay** any stored game with pause, speed control and a progress bar.
- Hear sounds and see small motion effects (all synthesised in code), each with its own on/off switch.

### The twist

On top of classic Ludo, LUDO-T adds blockades (two or more of your pieces on one cell move together and block
opponents), mystery cells that teleport or hand out effects (energised, sick), a briefing area pieces must escape
from, and a forced break-up of blockades after three sixes in a row.

<p align="center">
  <img src="docs/images/replay.png" width="48%" alt="Replaying a finished game">
  <img src="docs/images/winner.png" width="48%" alt="The winner's card with confetti">
</p>

## Quick start

You need **Java 17 or newer** (`java -version`). The scripts are for macOS and Linux; each has a `.bat`/`.ps1`
twin for Windows that has not been tested.

```bash
git clone <this repository>
cd Ludo-T

./download-tools.sh      # once: JavaFX, JUnit, the JDBC drivers and the analysis tools into lib/ and tools/
./compile.sh             # builds dist/*.jar

./run-server.sh          # terminal 1: the game server (no database needed to try it out)
./run-client.sh          # terminal 2: a game window; run it again for a second player
```

In the window: type a nickname, **Connect**, **Create Game**, take a seat, **Start Game**, then roll and click a
glowing piece. Games are not stored unless you configure a database (below).

### Play with someone else

Run the server on one computer and give the other person the client:

```bash
./run-client.sh --host <the server's IP address> --port 5555
```

Both computers must be able to reach each other on port 5555 (the same Wi-Fi works).
See the [developer guide](docs/DEVELOPER_GUIDE.md#two-machines) for what the second computer needs.

### Store games in a database (optional)

Without a database everything works except **Finished Games** and **Replay**. To switch it on:

```bash
cp config/server.properties.example config/server.properties
# edit it: db.url, db.user, db.password   (this file is git-ignored: never commit it)
./run-dbtool.sh reset    # creates the tables and a finished demo game
./run-server.sh
```

Any PostgreSQL works (the project was built against Supabase). For a fully offline option there is a local H2
database: `./run-h2.sh`, see the [developer guide](docs/DEVELOPER_GUIDE.md#the-database).

## Bots and stress testing

Create a Play room in the client first, then let bots take the seats:

```bash
./run-bots.sh R-0001                       # four bots, one per colour, play the room you created
./run-stress.sh --create --name stressA --seat RED --speed fast --start-game
```

The bots each have a style (Red goes for captures, Green for blockades, Yellow races home, Blue is random). The
stress client floods a room with fast, concurrent and deliberately wrong requests and checks every answer; the
[developer guide](docs/DEVELOPER_GUIDE.md#automatic-clients) has all the options.

## How it works

```
 JavaFX client (many)  <---- TCP ---->  Server  <---- JDBC ---->  PostgreSQL
 (bots and stress tool,                 one actor per room,        games, decisions,
  same protocol)                        queues everywhere          chat
```

- **The server is authoritative.** Clients send requests ("roll", "move piece 2"); the server validates them and
  pushes the result to everyone in the room.
- **One actor per room.** Each room has its own single-thread queue, so thousands of requests to one room are
  handled in order without locks, and a busy room never slows another down. A client that cannot keep up is
  disconnected instead of stalling anyone else.
- **Every push has a sequence number**, so a client notices a gap and asks for a fresh snapshot.
- **Storage is asynchronous and idempotent**: games are written in batches by a background thread, so the
  database is never on the path of a move.
- **Replays re-run the game.** The engine is deterministic: a stored seed plus the recorded decisions replay to
  the same result, and a replay that disagrees with the record stops rather than showing a wrong game.

The code is split into modules that depend only on what they should (the compiler enforces it):

| Module | Role | Depends on |
|---|---|---|
| `core` | The game rules and engine (no I/O) | nothing |
| `protocol` | Messages, DTOs, framing, the shared connection | nothing |
| `server` | Rooms, lobby, connections, replays, database access | `core`, `protocol` |
| `client` | The JavaFX app | `protocol` |
| `bot` | Bots and the stress client | `protocol` |

## Tests

```bash
./test.sh                # about 660 tests across all modules
./run-metrics.sh         # coverage and static analysis (Checkstyle, PMD, SpotBugs)
./run-evidence.sh        # a recorded stress run against a live server and database
```

Highlights: golden game logs that pin the rules, concurrency tests for the room queues, reconnect and replay
tests over real sockets, and a test that replays a game stored in SQL and checks it ends identically.

## Repository layout

```
src/{core,protocol,server,client,bot}/   the five modules
test/                                    tests, one folder per module (+ integration)
test-resources/golden/                   recorded games used as rule tests
db/                                      schema, seed and reset SQL
config/                                  server.properties.example (your real one is git-ignored)
docs/                                    developer guide and images
*.sh / *.bat / *.ps1                     build, run and test scripts (no Maven or Gradle needed)
```

## More documentation

- [Developer guide](docs/DEVELOPER_GUIDE.md): configuration reference, database set-up, the automatic clients,
  metrics, troubleshooting and the game-rule scenarios.

## Status

A finished coursework project that is still being polished. Known limits: no accounts (a nickname is your
identity), plain TCP with Java serialization behind an allow-list (JSON over TLS would be the production choice),
and one server instance owns the database. No licence has been chosen yet.
