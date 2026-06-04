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
run.bat
```

This will automatically compile the source files on the first run and then launch the game. Output is printed to the console and also saved to `game_log.txt`.

To run with a specific random seed (for reproducible games):

```bat
run.bat 12345
```

If no seed is provided, the default seed `42` is used.

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
