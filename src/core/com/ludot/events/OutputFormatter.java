package com.ludot.events;

import com.ludot.events.block.BlockBreakEvent;
import com.ludot.events.block.BlockFormEvent;
import com.ludot.events.block.BlockMoveEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.events.effect.EffectAppliedEvent;
import com.ludot.events.effect.EffectExpiredEvent;
import com.ludot.events.flow.BriefingEscapeEvent;
import com.ludot.events.flow.GameStartEvent;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.TurnOrderEvent;
import com.ludot.events.flow.TurnStartEvent;
import com.ludot.events.flow.WinEvent;
import com.ludot.events.home.HomeEntryEvent;
import com.ludot.events.home.HomeEntryRejectedEvent;
import com.ludot.events.home.HomeReachEvent;
import com.ludot.events.move.ApproachPassEvent;
import com.ludot.events.move.CaptureEvent;
import com.ludot.events.move.MoveEvent;
import com.ludot.events.move.PieceBlockedEvent;
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
import java.util.stream.Collectors;

// Converts each event type into a console-ready string via overloaded format() methods.
public class OutputFormatter {

    private static final String SEP = "====================";

    // game flow — start and turn headers
    public String format(GameStartEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LUDO-T GAME START ===");
        sb.append("\nPlayers: ");
        List<String> playerDescs = e.players().stream()
                .map(p -> cap(p.getColour()) + " (" + strategyName(p.getColour()) + ")")
                .collect(Collectors.toList());
        sb.append(String.join(", ", playerDescs));
        for (Player p : e.players()) {
            List<String> names = p.getPieces().stream()
                    .map(Piece::getName)
                    .collect(Collectors.toList());
            if (!names.isEmpty()) {
                sb.append("\nThe ").append(cap(p.getColour()))
                  .append(" player has four (04) pieces named ")
                  .append(joinNames(names)).append(".");
            }
        }
        sb.append("\nDetermining first player...");
        return sb.toString();
    }

    public String format(TurnStartEvent e) {
        return "\n--- " + cap(e.player().getColour()) + "'s turn ---";
    }

    public String format(DiceRollEvent e) {
        return cap(e.player().getColour()) + " player rolled " + e.roll() + ".";
    }

    public String format(CoinTossEvent e) {
        String toss = e.direction() == Direction.CLOCKWISE ? "Heads" : "Tails";
        String dir  = e.direction() == Direction.CLOCKWISE ? "Clockwise" : "Counter-Clockwise";
        return e.piece().getName() + " exits base. Coin toss: " + toss + " -> " + dir;
    }

    public String format(TripleSixEvent e) {
        if (e.hasBlock()) {
            return cap(e.player().getColour()) + " rolled three consecutive 6s! Forced block break! (T-6)";
        }
        return cap(e.player().getColour()) + " rolled three consecutive 6s! Turn forfeited.";
    }

    public String format(MoveEvent e) {
        StringBuilder sb = new StringBuilder();
        String colour = cap(e.piece().getColour());
        if (e.from() == null) {
            sb.append(colour).append(" player moves piece ").append(e.piece().getName())
              .append(" to the starting point.");
        } else {
            sb.append(colour).append(" player moves piece ").append(e.piece().getName())
              .append(" from location ").append(e.from().getId())
              .append(" to ").append(e.to().getId())
              .append(" by ").append(e.steps()).append(" units in ")
              .append(dirStr(e.piece().getDirection())).append(" direction.");
        }
        if (e.player() != null) {
            int onBoard = e.player().getPiecesOnBoard().size();
            int atBase  = e.player().getPiecesAtBase().size();
            int total   = e.player().getPieces().size();
            sb.append("\n").append(colour)
              .append(" player now has ").append(onBoard).append("/").append(total)
              .append(" pieces on the board and ").append(atBase).append("/").append(total)
              .append(" pieces on the base.");
        }
        return sb.toString();
    }

    // O11: "[Color X] piece [Name] lands on square L1, captures [Color Y] piece [Name], and returns it to the base."
    public String format(CaptureEvent e) {
        return cap(e.attacker().getColour()) + " piece " + e.attacker().getName()
                + " lands on square " + e.cell().getId() + ", captures "
                + cap(e.victim().getColour()) + " piece " + e.victim().getName()
                + ", and returns it to the base."
                + "\n" + cap(e.attacker().getColour()) + " receives a bonus roll! (T-2)";
    }

    // O10: "[Color] player does not have other pieces available. Ignoring the throw."
    public String format(SkipTurnEvent e) {
        return cap(e.player().getColour()) + " player does not have other pieces available. Ignoring the throw.";
    }

    // O9: "[Color] piece [Name] is blocked from moving from L1 to L2 by [Color'] piece [Name']."
    public String format(PieceBlockedEvent e) {
        String fromStr = e.from() == null ? "Base" : String.valueOf(e.from().getId());
        String toStr   = String.valueOf(e.intendedDest().getId());
        if (e.blockingPieces().isEmpty()) {
            return cap(e.blocked().getColour()) + " piece " + e.blocked().getName()
                    + " is blocked from moving from " + fromStr + " to " + toStr + ".";
        }
        Piece blocker = e.blockingPieces().get(0);
        return cap(e.blocked().getColour()) + " piece " + e.blocked().getName()
                + " is blocked from moving from " + fromStr + " to " + toStr
                + " by " + cap(blocker.getColour()) + " piece " + blocker.getName() + ".";
    }

    public String format(ApproachPassEvent e) {
        return e.piece().getName() + " passes approach cell (pass count: " + e.passCount() + ")";
    }

    // block
    public String format(BlockFormEvent e) {
        List<Piece> block = e.block();
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

    public String format(HomeEntryRejectedEvent e) {
        return e.piece().getName() + " cannot enter home straight (needs "
                + e.capturesNeeded() + " more capture(s)) (T-7)";
    }

    public String format(MysterySpawnEvent e) {
        return "A mystery cell has spawned in location " + e.cell().getId()
                + " and will be at this location for the next " + e.roundCount() + " rounds.";
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

    // O15: beta escape
    public String format(BriefingEscapeEvent e) {
        String name = cap(e.piece().getColour()) + " piece " + e.piece().getName();
        return name + " is movement-restricted and has rolled three consecutively. Teleporting piece "
                + e.piece().getName() + " to base.";
    }

    // game flow — roll-off and turn order
    public String format(RollOffEvent e) {
        return cap(e.player().getColour()) + " rolls " + e.roll();
    }

    // O3/O4: first-player announcement and play order
    public String format(TurnOrderEvent e) {
        List<Player> order = e.order();
        return cap(e.first().getColour()) + " player has the highest roll and will begin the game."
                + "\nThe order of a single round is "
                + joinNames(order.stream().map(p -> cap(p.getColour())).collect(Collectors.toList())) + ".";
    }

    // O12/O17: round-end status table with optional mystery cell line
    public String format(RoundEndEvent e) {
        List<Player> players = e.players();
        if (players == null || players.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("=== Round ").append(e.roundNumber()).append(" Status ===");
        for (Player p : players) {
            sb.append("\n").append(SEP);
            sb.append("\nLocation of pieces ").append(cap(p.getColour()));
            sb.append("\n").append(SEP);
            for (Piece piece : p.getPieces()) {
                sb.append("\nPiece ").append(piece.getName()).append(" -> ").append(posLabel(piece));
            }
        }
        if (e.mysteryCell() != null) {
            sb.append("\n").append(SEP);
            sb.append("\nThe mystery cell is at ").append(e.mysteryCell().getId())
              .append(" and will be at that location for the next ")
              .append(e.mysteryRoundsLeft()).append(" rounds.");
        }
        return sb.toString();
    }

    // O16: "[Color X] player wins!!!"
    public String format(WinEvent e) {
        return cap(e.winner().getColour()) + " player wins!!!"
                + "\nGame finished in " + e.totalRounds() + " rounds.";
    }

    // helpers
    private String cap(Colour colour) {
        String s = colour.name();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String dirStr(Direction dir) {
        if (dir == null) return "unknown";
        return dir == Direction.CLOCKWISE ? "clockwise" : "counter-clockwise";
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
            case "BLOCKED"      -> "nowhere (blocked)";
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

    public static String buildEffectDescription(Piece piece, TeleportDestination dest, Direction dirBefore) {
        if (dest == null) return "";
        String name = pieceLabel(piece);
        return switch (dest) {
            case ALPHA         -> describeAlpha(piece, name);
            case GAMMA         -> dirBefore == Direction.COUNTER_CLOCKWISE
                    ? name + " is moving in a counter-clockwise direction. Teleporting to Beta from Gamma.\n"
                            + name + " attends briefing and cannot move for four rounds."
                    : name + ", which was moving " + directionWord(dirBefore)
                            + ", has changed to moving " + directionWord(piece.getDirection()) + ".";
            case BETA          -> name + " attends briefing and cannot move for four rounds.";
            case BASE          -> name + " has been sent back to base.";
            case START_CELL    -> name + " teleported to their start cell.";
            case APPROACH_CELL -> name + " teleported to their approach cell.";
        };
    }

    // "Red piece R1": the colour in sentence case followed by the piece's name
    private static String pieceLabel(Piece piece) {
        String colour = piece.getColour().name();
        return Character.toUpperCase(colour.charAt(0)) + colour.substring(1).toLowerCase() + " piece " + piece.getName();
    }

    private static String describeAlpha(Piece piece, String name) {
        return piece.getActiveEffect() instanceof EnergisedEffect
                ? name + " feels energized, and movement speed doubles."
                : name + " feels sick, and movement speed halves.";
    }

    private static String directionWord(Direction direction) {
        return direction == Direction.CLOCKWISE ? "clockwise" : "counter-clockwise";
    }
}
