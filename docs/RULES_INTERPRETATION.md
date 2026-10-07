# Rules: corrections and interpretations

The rules come from the LUDO-T specification (Assignment 1). The original console game deviated from it in several
places; these were found by an audit and corrected, each with a test that fails on the old behaviour and passes now.
Where the specification is silent, the decision taken is listed in the second table.

## Corrections

| Rule | Old behaviour | Behaviour now | Tests |
|---|---|---|---|
| Rule 9 (approach cell) | A piece turned into the home straight only when it landed exactly on its approach cell | It turns in when it lands on **or passes** the approach cell, and spends the remaining steps on the home straight; overshooting home is not a move | `HomeStraightEntryTest` |
| T-1 / T-7 | Counter-clockwise pieces and the capture requirement were checked on landing only | Counter-clockwise pieces turn in on their **second** approach pass; a piece without the needed capture carries on round the ring | `HomeStraightEntryTest` |
| T-3 | Only the destination cell was checked for a blockade | A piece cannot **jump** a blockade anywhere on its path; it stops on the cell before it. Block moves and the T-6 moves obey it too | `BlockadeTest` |
| T-4 | A block moved by the roll, in the direction of its first piece | A block moves `roll / size` cells in the direction of the member furthest from home | `BlockadeTest` |
| T-5 | A piece could not leave a block on its own; direction was lost on a break | A single piece may leave its block, and goes back to the direction it had when it was placed on X | `TurnAndBlockRulesTest` |
| T-6 | Every piece of the block moved six cells | One piece stays; the others move six, then twelve, ... cells in their original direction | `TurnAndBlockRulesTest` |
| T-8 | Block captures credited the capture count twice | Each attacker earns one capture | `BlockCaptureMoveTest` |
| T-10 | A cell spawned from round 3 and vanished when a piece landed on it | After two rounds, on an empty cell, for four rounds whatever happens, then it reappears elsewhere, never on the same cell twice | `MysteryManagerTest`, `MysteryRuleTest` |
| T-11 | Alpha, Beta and Gamma were not cells of the board | They are cells 7, 25 and 44 (the 9th, 27th and 46th from yellow's approach cell) and the piece is placed there | `RuleEngineApplyTeleportTest` |
| T-13 | The briefing counted rolls, and asking for the valid moves counted them | It counts **rounds**; the three 3s are counted once per roll; asking for moves changes nothing | `AtBriefingStateTest` |
| T-14 | Gamma reversed the direction both ways, with a hidden briefing | Clockwise becomes counter-clockwise; counter-clockwise is teleported on to Beta (with the specified message) | `RuleEngineApplyTeleportTest` |
| T-15 | Landing on Alpha, Beta or Gamma by walking could apply effects | Effects apply only when the piece is teleported there | `RuleEngineApplyTeleportTest` |
| Rule 6 | Leaving base onto a cell with one opponent piece stacked two colours on it | The arriving piece captures the lone opponent | `TurnAndBlockRulesTest` |
| Rule 4 / 7 | An ignored roll of 6 still gave a bonus roll; the count of sixes could carry into the next turn | An ignored roll ends the turn; the count starts again every turn | `TurnAndBlockRulesTest` |
| Rule 7 | Classic let a piece land on its own piece | Not allowed in Classic; if no other piece can move the roll is ignored | `ClassicVariantTest` |
| Classic | Mystery cells, a coin toss and blockade logic leaked into the Classic variant | Classic has none of them | `ClassicVariantTest` |
| Turn order | Players were sorted by their roll-off number | The highest roller starts and play continues clockwise R, G, Y, B; ties for the highest roll roll again | `GameEngineTest` |
| Blue (2.1.4) | Used the global turn counter, so it kept moving the same piece | Cycles B1, B2, B3, B4 on its own counter | `BlueCycleTest` |

"The behaviour is unchanged" therefore holds for the architecture and the rules not listed above; the golden logs
were re-recorded once after these corrections and every game stored in a database before then must be discarded
(`./run-dbtool.sh reset`), because replays of old games would no longer match.

## Decisions where the specification is silent

| Situation | Decision |
|---|---|
| A single piece lands on a cell holding exactly one opponent piece, or a block lands on one | It captures it (Rule 6); a block moving onto a lone opponent captures it without being an equal-size capture |
| Teleported onto a cell holding one opponent piece | That piece is captured |
| Teleported onto a cell holding an opposing block | The teleport is refused and the piece stays where it is, with a message |
| Which piece stays behind on T-6 | The first piece of the block (by name) |
| Blocks and the home straight | A block moves on the ring only; pieces enter the home straight one at a time |
| Effects and blocks | Energised/Sick change a single piece's steps, not a block's |
| Several players tie for the highest roll in the roll-off | Only those players roll again |
| Opposing blockades jam each other so that nobody can ever move again (T-3 forbids jumping) | After **100 rounds without any piece moving** the game is ended and awarded to the player with most pieces home, then most captures |
