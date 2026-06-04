package com.ludot.events;

import com.ludot.events.block.BlockBreakEvent;
import com.ludot.events.block.BlockFormEvent;
import com.ludot.events.block.BlockMoveEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.events.effect.EffectAppliedEvent;
import com.ludot.events.effect.EffectExpiredEvent;
import com.ludot.events.flow.GameStartEvent;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.events.flow.TurnStartEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.events.home.HomeEntryEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.events.move.SkipTurnEvent;
import com.ludot.events.mystery.MysteryExpireEvent;
import com.ludot.events.mystery.MysterySpawnEvent;
import com.ludot.events.mystery.MysteryTickEvent;
import com.ludot.events.mystery.MysteryTeleportEvent;
import com.ludot.GameConstants;
import com.ludot.effects.EnergisedEffect;
import com.ludot.effects.TeleportDestination;
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Converts each event type into a console-ready string via overloaded format() methods.
public class OutputFormatter {

    // game flow — start and turn headers
    public String format(GameStartEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LUDO-T GAME START ===");
        sb.append("\nPlayers: ");
        List<String> playerDescs = e.players().stream()
                .map(p -> cap(p.getColour()) + " (" + strategyName(p.getColour()) + ")")
                .collect(Collectors.toList());
        sb.append(String.join(", ", playerDescs));
        sb.append("\nDetermining first player...");
        return sb.toString();
    }

    public String format(TurnStartEvent e) {
        return "\n--- " + cap(e.player().getColour()) + "'s turn ---";
    }

    // dice
    public String format(DiceRollEvent e) {
        return cap(e.player().getColour()) + " rolls " + e.roll();
    }

    public String format(CoinTossEvent e) {
        String toss = e.direction() == Direction.CLOCKWISE ? "Heads" : "Tails";
        String dir  = e.direction() == Direction.CLOCKWISE ? "Clockwise" : "Counter-Clockwise";
        return e.piece().getName() + " exits base. Coin toss: " + toss + " -> " + dir;
    }

    public String format(TripleSixEvent e) {
        return cap(e.player().getColour()) + " rolled three consecutive 6s! Turn forfeited.";
    }

    // movement
    public String format(MoveEvent e) {
        if (e.from() == null) {
            return e.piece().getName() + " moves from Base to " + e.to().getId();
        }
        return e.piece().getName() + " moves from " + e.from().getId() + " to " + e.to().getId();
    }

    public String format(CaptureEvent e) {
        return e.attacker().getName() + " captures " + e.victim().getName()
                + " at cell " + e.cell().getId() + "!"
                + "\n" + e.victim().getName() + " is sent to base."
                + "\n" + cap(e.attacker().getColour()) + " receives a bonus roll! (T-2)";
    }

    public String format(SkipTurnEvent e) {
        return cap(e.player().getColour()) + " has no valid moves. Turn skipped.";
    }

    // block
    public String format(BlockFormEvent e) {
        List<Piece> block = e.block();
        String names = block.stream().map(Piece::getName).collect(Collectors.joining(", "));
        String first = block.get(0).getName();
        String rest  = block.subList(1, block.size()).stream()
                .map(Piece::getName).collect(Collectors.joining(", "));
        return first + " forms a blockade with " + rest + " at cell " + e.cell().getId() + " (T-3)";
    }

    public String format(BlockMoveEvent e) {
        int size = e.block().size();
        return "Blockade of " + size + " moves " + e.steps()
                + " steps (roll " + e.originalRoll() + " / " + size + ") (T-4)";
    }

    public String format(BlockBreakEvent e) {
        return e.block().stream()
                .map(p -> "Blockade broken! " + p.getName() + " reverts to " + dirStr(p.getDirection()) + " (T-5)")
                .collect(Collectors.joining("\n"));
    }

    // home straight
    public String format(HomeEntryEvent e) {
        return e.piece().getName() + " enters the home straight!";
    }

    public String format(HomeReachEvent e) {
        return e.piece().getName() + " reaches Home!";
    }

    // mystery cell
    public String format(MysterySpawnEvent e) {
        return "Mystery cell appears at cell " + e.cell().getId() + "!";
    }

    public String format(MysteryTickEvent e) {
        return "Mystery cell at cell " + e.cell().getId()
                + " (active for " + e.roundsLeft() + " more rounds)";
    }

    public String format(MysteryExpireEvent e) {
        return "Mystery cell expires.";
    }

    public String format(MysteryTeleportEvent e) {
        String destLabel = destLabel(e.effectName());
        String posStr = e.to() != null ? " (cell " + e.to().getId() + ")" : "";
        StringBuilder sb = new StringBuilder();
        sb.append(e.piece().getName()).append(" lands on mystery cell! Teleporting...");
        sb.append("\n").append(e.piece().getName()).append(" teleported to ").append(destLabel).append(posStr);
        if (e.effectDescription() != null && !e.effectDescription().isBlank()) {
            sb.append("\n").append(e.effectDescription());
        }
        return sb.toString();
    }

    // effects
    public String format(EffectAppliedEvent e) {
        return e.piece().getName() + " is now " + e.effectName() + " for " + e.duration() + " rounds.";
    }

    public String format(EffectExpiredEvent e) {
        return e.piece().getName() + "'s " + e.effectName() + " effect has worn off.";
    }

    // game flow — roll-off and turn order
    public String format(RollOffEvent e) {
        return cap(e.player().getColour()) + " rolls " + e.roll();
    }

    public String format(TurnOrderEvent e) {
        Map<Colour, Integer> rolls = e.rollResults();
        List<Player> order = e.order();
        long distinctValues = rolls.values().stream().distinct().count();
        StringBuilder sb = new StringBuilder();

        if (distinctValues == 1) {
            int rolled = rolls.values().iterator().next();
            sb.append("All players rolled ").append(rolled)
              .append(". Play order determined alphabetically.");
        } else {
            sb.append(cap(e.first().getColour())).append(" goes first!");
            rolls.values().stream().distinct().forEach(rollVal -> {
                List<String> tied = order.stream()
                        .filter(p -> rolls.get(p.getColour()) == rollVal)
                        .map(p -> cap(p.getColour()))
                        .collect(Collectors.toList());
                if (tied.size() >= 2) {
                    sb.append("\n").append(joinNames(tied))
                      .append(" all rolled ").append(rollVal)
                      .append(" and are ordered alphabetically.");
                }
            });
        }

        sb.append("\nThe order of play is ").append(joinNames(
                order.stream().map(p -> cap(p.getColour())).collect(Collectors.toList())
        )).append(".");
        return sb.toString();
    }

    public String format(RoundEndEvent e) {
        List<Player> players = e.players();
        if (players == null || players.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("=== Round ").append(e.roundNumber()).append(" Status ===");
        for (Player p : players) {
            sb.append("\n").append(cap(p.getColour())).append(": ");
            List<String> pieceDescs = p.getPieces().stream()
                    .map(piece -> piece.getName() + "@" + posLabel(piece))
                    .collect(Collectors.toList());
            sb.append(String.join("  ", pieceDescs));
        }
        return sb.toString();
    }

    public String format(WinEvent e) {
        return "=== " + e.winner().getColour().name() + " WINS! ==="
                + "\nGame finished in " + e.totalRounds() + " rounds.";
    }

    // helpers
    private String cap(Colour colour) {
        String s = colour.name();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String dirStr(Direction dir) {
        if (dir == null) return "unknown";
        return dir == Direction.CLOCKWISE ? "Clockwise" : "Counter-Clockwise";
    }

    private String strategyName(Colour colour) {
        return switch (colour) {
            case RED    -> "Aggressive";
            case GREEN  -> "Blocking";
            case YELLOW -> "Winning";
            case BLUE   -> "Random";
        };
    }

    private String destLabel(String enumName) {
        if (enumName == null) return "Unknown";
        return switch (enumName) {
            case "ALPHA"        -> "Alpha Cell";
            case "BETA"         -> "Beta Cell";
            case "GAMMA"        -> "Gamma Cell";
            case "BASE"         -> "Base";
            case "START_CELL"   -> "Start Cell";
            case "APPROACH_CELL"-> "Approach Cell";
            default             -> enumName;
        };
    }

    private String posLabel(Piece piece) {
        if (piece.getPosition() == null) return "Base";
        Cell cell = piece.getPosition();
        if (cell.getCellType() == CellType.HOME) return "Home";
        if (cell.getCellType() == CellType.HOME_STRAIGHT) {
            int slot = (cell.getId() - GameConstants.BOARD_SIZE) % GameConstants.HOME_STRAIGHT_LENGTH + 1;
            return "HS" + slot;
        }
        return String.valueOf(cell.getId());
    }

    private String joinNames(List<String> names) {
        if (names.size() == 1) return names.get(0);
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(i == names.size() - 1 ? ", and " : ", ");
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    // Builds the effect description line for a mystery teleport event.
    // Static so MoveExecutor can call it without holding an OutputFormatter instance.
    public static String buildEffectDescription(Piece piece, TeleportDestination dest, Direction dirBefore) {
        if (dest == null) return "";
        String colour = piece.getColour().name();
        String capColour = Character.toUpperCase(colour.charAt(0)) + colour.substring(1).toLowerCase();
        String name = capColour + " piece " + piece.getName();
        return switch (dest) {
            case ALPHA -> piece.getActiveEffect() instanceof EnergisedEffect
                    ? name + " feels energized, and movement speed doubles."
                    : name + " feels sick, and movement speed halves.";
            case GAMMA -> {
                String oldDir = dirBefore == Direction.CLOCKWISE ? "clockwise" : "counter-clockwise";
                String newDir = piece.getDirection() == Direction.CLOCKWISE ? "clockwise" : "counter-clockwise";
                yield name + ", which was moving " + oldDir + ", has changed to moving " + newDir + ".";
            }
            case BETA          -> name + " attends briefing and cannot move for four rounds.";
            case BASE          -> name + " has been sent back to base.";
            case START_CELL    -> name + " teleported to their start cell.";
            case APPROACH_CELL -> name + " teleported to their approach cell.";
        };
    }
}
