# LUDO-T

A Java implementation of a modified Ludo board game featuring custom rules, player strategies, and a software metrics pipeline.

---

## Requirements

| Tool | Version | Notes |
|------|---------|-------|
| Java JDK | 17 or higher | Must be on your system `PATH` |
| Git | Any | To clone the repository |

No Maven, Gradle, or IDE installation is required.

---

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd Ludo-T
```

### 2. Run the game

```bat
compile.bat
```

```bat
run.bat
```

This will automatically compile the source files on the first run and then launch the game. Output is printed to the console and also saved to `game_log.txt`.

To run with a specific random seed (for reproducible games):

```bat
run.bat 12345
```

If no seed is provided, the default seed `42` is used.

---

## Scenario Seeds

Each seed below produces a fully reproducible game that clearly demonstrates a specific LUDO-T rule.
Run any of them with `run.bat <seed>` and search the output for the quoted line to jump straight to the scenario.

---

### Seed 1 — Core rules (T-2, T-3, T-4, T-5, T-6, T-7, T-8, T-9, T-10, T-11, T-14, T-15 + CCW movement)

This is the primary demonstration seed. A single game run contains examples of almost every rule.

```bat
run.bat 1
```

| Rule | What to look for in the output |
|------|-------------------------------|
| **T-1** CCW movement | `R1 exits base. Coin toss: Tails -> Counter-Clockwise` |
| **T-1** Approach cell pass | `R1 passes approach cell (pass count: 1)` |
| **T-2** Bonus roll on capture | `Yellow receives a bonus roll! (T-2)` |
| **T-3** Block formation | `G1 forms a blockade with G2 at cell 39 (T-3)` |
| **T-4** Block movement | `Blockade of 2 moves 2 steps (roll 5 / 2) (T-4)` |
| **T-5** Direction restored on break | `Blockade broken! G1 reverts to counter-clockwise (T-5)` |
| **T-6** Triple-six penalty | `Green rolled three consecutive 6s! Forced block break! (T-6)` |
| **T-7** Home entry rejected | `G4 cannot enter home straight (needs 1 more capture(s)) (T-7)` |
| **T-8** Block-vs-block capture | `Blockade of 3 moves 1 steps (roll 0 / 3) (T-4)` *(roll=0 marks a block-capture move)* |
| **T-9** Capture + return to base | `Yellow piece Y4 lands on square 2, captures Red piece R1, and returns it to the base.` |
| **T-10** Mystery cell spawns | `A mystery cell has spawned in location 32 and will be at this location for the next 4 rounds.` |
| **T-11** Mystery teleport | `Y1 lands on mystery cell! Teleporting...` → `Y1 teleported to Start Cell (cell 0)` |
| **T-14** Gamma — direction reversal | `Y1 teleported to Gamma Cell (cell 33)` → `Yellow piece Y1, which was moving clockwise, has changed to moving counter-clockwise.` |
| **T-15** Mystery cell expiry | `Mystery cell expires.` |
| Home straight entry | `Y4 enters the home straight!` |
| Piece reaches home | `Y4 reaches Home!` |

---

### Seed 2 — Alpha (Sick) + Beta Briefing (T-12 Sick, T-13)

```bat
run.bat 2
```

| Rule | What to look for in the output |
|------|-------------------------------|
| **T-12** Alpha → Sick effect | `R1 teleported to Alpha Cell (cell 20)` → `Red piece R1 feels sick, and movement speed halves.` |
| **T-13** Beta → Briefing lock | `G1 teleported to Beta Cell (cell 45)` → `Green piece G1 attends briefing and cannot move for four rounds.` |

---

### Seed 3 — Alpha (Energised) (T-12 Energised)

```bat
run.bat 3
```

| Rule | What to look for in the output |
|------|-------------------------------|
| **T-12** Alpha → Energised effect | `Y4 teleported to Alpha Cell (cell 10)` → `Yellow piece Y4 feels energized, and movement speed doubles.` |

---

### Seed 2069 — Beta Briefing escape via triple-3 (T-13 escape)

This is the rarest scenario: a piece in Briefing state rolls three consecutive 3s and escapes early.

```bat
run.bat 2069
```

| Rule | What to look for in the output |
|------|-------------------------------|
| **T-13** Briefing early escape | `Red piece R3 is movement-restricted and has rolled three consecutively. Teleporting piece R3 to base.` |

---

### Quick reference

| Seed | Primary scenario |
|------|-----------------|
| `1` | T-1 CCW · T-2 Bonus · T-3/T-4 Block · T-5 Break · T-6 Triple-Six · T-7 Home reject · T-8 Block capture · T-9 Capture · T-10/T-11 Mystery · T-14 Gamma · T-15 Expiry |
| `2` | T-12 Alpha Sick · T-13 Beta Briefing |
| `3` | T-12 Alpha Energised |
| `2069` | T-13 Beta Briefing escape (triple-3) |

---

## Running the Tests

```bat
test.bat
```

This compiles all test sources and runs the full JUnit 5 test suite (250 tests). No setup needed.

---

## Software Metrics

The project includes a metrics pipeline that runs four analysis tools plus a dedicated cyclomatic complexity check.

### Step 1 — Download the tool JARs (one-time setup)

```bat
download-tools.bat
```

This downloads the following tools into the `tools/` folder:

| Tool | Version | Purpose |
|------|---------|---------|
| JaCoCo | 0.8.11 | Code coverage (line + branch) |
| Checkstyle | 10.14.0 | Code style enforcement |
| PMD | 7.3.0 | Static analysis |
| SpotBugs | 4.8.6 | Bug pattern detection |

The `tools/` folder is git-ignored so the JARs are not committed to the repository.

### Step 2 — Run the metrics

```bat
run-metrics.bat
```

This compiles the project, runs all 250 tests under JaCoCo coverage instrumentation, then runs all four analysis tools. Reports are saved to the `reports/` folder.

| Report | Location |
|--------|----------|
| JaCoCo coverage | `reports/jacoco/index.html` (open in browser) |
| Checkstyle | `reports/checkstyle/checkstyle-src.xml` |
| PMD | `reports/pmd/pmd-result.xml` |
| SpotBugs | `reports/spotbugs/spotbugs-result.xml` |
| Cyclomatic Complexity | `reports/complexity/complexity-result.xml` |

---

## Project Structure

```
Ludo-T/
├── src/                        # Production source code
│   └── com/ludot/
│       ├── Main.java           # Entry point
│       ├── engine/             # Game loop, turn management, move execution
│       ├── model/              # Board, Cell, Piece, Player, GameState
│       ├── rules/              # Rule engine, move/capture/block/dice rules
│       ├── command/            # Move command objects (Command pattern)
│       ├── strategy/           # Player AI strategies
│       ├── events/             # Event bus, event types, visitor (Observer + Visitor)
│       ├── effects/            # Piece effects (Energised, Sick, NoEffect)
│       ├── factory/            # Builders and factories (Factory Method)
│       └── dice/               # Random number providers
├── test/                       # JUnit 5 test sources (mirrors src/ structure)
├── lib/                        # Runtime JARs (JUnit 5, Mockito — committed)
├── tools/                      # Metrics tool JARs (downloaded, git-ignored)
├── reports/                    # Generated reports (git-ignored)
├── out/                        # Compiled classes (git-ignored)
├── checkstyle.xml              # Checkstyle style rules
├── complexity.xml              # Cyclomatic complexity thresholds
├── pmd-ruleset.xml             # PMD static analysis rules
├── spotbugs-include.xml        # SpotBugs filter config
├── compile.bat                 # Compile sources (used by run.bat / test.bat)
├── run.bat                     # Compile and run the game
├── test.bat                    # Compile and run all tests
├── download-tools.bat          # Download metrics tool JARs (run once)
├── run-metrics.bat             # Run full metrics pipeline
└── game_log.txt                # Last game output (git-ignored)
```

---

## Configuring the Metrics Thresholds

| File | What it controls |
|------|-----------------|
| `checkstyle.xml` | Code style rules (naming, braces, whitespace, imports) |
| `complexity.xml` | Cyclomatic complexity max (default 10), NPath max (default 200), fan-out max (default 35) |
| `pmd-ruleset.xml` | Static analysis rules (best practices, design, error-prone, performance) |
| `spotbugs-include.xml` | SpotBugs bug categories to include |

---

## Unix / macOS

Shell script equivalents are provided for all batch files:

| Windows | Unix/macOS |
|---------|-----------|
| `download-tools.bat` | `./download-tools.sh` |
| `run-metrics.bat` | `./run-metrics.sh` |

Make the scripts executable after cloning:

```bash
chmod +x download-tools.sh run-metrics.sh
```
