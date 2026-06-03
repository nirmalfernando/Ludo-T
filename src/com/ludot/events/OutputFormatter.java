package com.ludot.events;

import com.ludot.events.block.BlockBreakEvent;
import com.ludot.events.block.BlockFormEvent;
import com.ludot.events.block.BlockMoveEvent;
import com.ludot.events.dice.CoinTossEvent;
import com.ludot.events.dice.DiceRollEvent;
import com.ludot.events.dice.TripleSixEvent;
import com.ludot.events.effect.EffectAppliedEvent;
import com.ludot.events.effect.EffectExpiredEvent;
import com.ludot.events.flow.RollOffEvent;
import com.ludot.events.flow.RoundEndEvent;
import com.ludot.events.flow.TurnOrderEvent;
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
import com.ludot.model.Cell;
import com.ludot.model.CellType;
import com.ludot.model.Colour;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import com.ludot.model.Direction;
import com.ludot.model.Piece;
import com.ludot.model.Player;
import java.util.List;

// Converts each event type into a console-ready string via overloaded format() methods.
public class OutputFormatter {

    // dice
    public String format(DiceRollEvent e) {
        return cap(e.player().getColour()) + " player rolled " + e.roll() + ".";
    }

    public String format(CoinTossEvent e) {
        return ""; // direction assignment is an internal detail; suppressed from console output
    }

    public String format(TripleSixEvent e) {
        return cap(e.player().getColour()) + " player rolled triple six - turn forfeited.";
    }

    // movement
    public String format(MoveEvent e) {
        if (e.from() == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(cap(e.piece().getColour())).append(" player moves piece ")
              .append(e.piece().getName()).append(" to the starting point.");
            if (e.player() != null) {
                sb.append("\n").append(boardCount(e.player()));
            }
            return sb.toString();
        }
        return cap(e.piece().getColour()) + " moves piece " + e.piece().getName()
            + " from location " + e.from().getId() + " to " + e.to().getId()
            + " by " + e.steps() + " units in " + dirStr(e.piece().getDirection()) + " direction.";
    }

    public String format(CaptureEvent e) {
        StringBuilder sb = new StringBuilder();
        sb.append(cap(e.attacker().getColour())).append(" piece ").append(e.attacker().getName())
          .append(" lands on square ").append(e.cell().getId()).append(", captures ")
          .append(cap(e.victim().getColour())).append(" piece ").append(e.victim().getName())
          .append(", and returns it to the base.");
        if (e.victimPlayer() != null) {
            sb.append("\n").append(boardCount(e.victimPlayer()));
        }
        return sb.toString();
    }

    public String format(SkipTurnEvent e) {
        return cap(e.player().getColour()) + " player has no valid moves - turn skipped.";
    }

    // block
    public String format(BlockFormEvent e) {
        return "A block has formed at location " + e.cell().getId() + ".";
    }

    public String format(BlockMoveEvent e) {
        return "Block moves from location " + cellId(e.from()) + " to " + cellId(e.to()) + ".";
    }

    public String format(BlockBreakEvent e) {
        return "The block at location " + e.cell().getId() + " has been broken.";
    }

    // home straight
    public String format(HomeEntryEvent e) {
        return cap(e.piece().getColour()) + " piece " + e.piece().getName() + " has entered the home straight.";
    }

    public String format(HomeReachEvent e) {
        return cap(e.piece().getColour()) + " piece " + e.piece().getName() + " has reached home!";
    }

    // mystery cell
    public String format(MysterySpawnEvent e) {
        return "A mystery cell has spawned in location " + e.cell().getId()
            + " and will be at this location for the next " + e.roundCount() + " rounds.";
    }

    public String format(MysteryTickEvent e) {
        return "The mystery cell is at " + e.cell().getId()
            + " and will be at that location for the next " + e.roundsLeft() + " values.";
    }

    public String format(MysteryExpireEvent e) {
        return "The mystery cell at location " + e.cell().getId() + " has expired.";
    }

    public String format(MysteryTeleportEvent e) {
        String dest = cap(e.effectName());
        StringBuilder sb = new StringBuilder();
        sb.append(cap(e.piece().getColour()))
          .append(" player lands on a mystery cell and is teleported to ").append(dest).append(".");
        sb.append("\n").append(cap(e.piece().getColour())).append(" piece ")
          .append(e.piece().getName()).append(" teleported to ").append(dest).append(".");
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
        return e.piece().getName() + " " + e.effectName() + " effect expired.";
    }

    // game flow — roll-off and turn order
    public String format(RollOffEvent e) {
        return cap(e.player().getColour()) + " rolls " + e.roll() + ".";
    }

    public String format(TurnOrderEvent e) {
        Map<Colour, Integer> rolls = e.rollResults();
        List<Player> order = e.order();
        long distinctValues = rolls.values().stream().distinct().count();
        StringBuilder sb = new StringBuilder();

        if (distinctValues == 1) {
            // every player rolled the same value
            int rolled = rolls.values().iterator().next();
            sb.append("All players rolled ").append(rolled)
              .append(". Play order is determined alphabetically.");
        } else {
            // at least one unique highest roll
            sb.append(cap(e.first().getColour()))
              .append(" player has the highest roll and will begin the game.");

            // find and describe any tied groups (size >= 2)
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

    // game flow — prints a position summary table for every player
    public String format(RoundEndEvent e) {
        List<Player> players = e.players();
        if (players == null || players.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int pi = 0; pi < players.size(); pi++) {
            Player p = players.get(pi);
            sb.append(boardCount(p)).append("\n");
            sb.append("============================\n");
            sb.append("Location of pieces ").append(cap(p.getColour())).append("\n");
            sb.append("============================\n");
            List<Piece> pieces = p.getPieces();
            for (int i = 0; i < pieces.size(); i++) {
                Piece piece = pieces.get(i);
                String loc = piece.getPosition() != null ? posLabel(piece.getPosition()) : "Base";
                sb.append("Piece ").append(i + 1).append(" -> ").append(loc);
                if (i < pieces.size() - 1) sb.append("\n");
            }
            if (pi < players.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    public String format(WinEvent e) {
        return cap(e.winner().getColour()) + " wins in " + e.totalRounds() + " rounds!";
    }

    // helpers
    private String cap(Colour colour) {
        String s = colour.name();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(w.charAt(0)));
                sb.append(w.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private String dirStr(Direction dir) {
        if (dir == null) return "unknown";
        return dir == Direction.CLOCKWISE ? "clockwise" : "counter-clockwise";
    }

    private String boardCount(Player player) {
        long onBoard = player.getPieces().stream().filter(p -> p.getPosition() != null).count();
        long total = player.getPieces().size();
        long onBase = total - onBoard;
        return cap(player.getColour()) + " player now has " + onBoard + "/" + total
            + " on pieces on the board and " + onBase + "/" + total + " pieces on the base.";
    }

    // "A", "A and B", "A, B, and C" etc.
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

    private String cellId(Cell cell) {
        return cell != null ? String.valueOf(cell.getId()) : "base";
    }

    // human-readable position label — hides internal IDs for home-straight cells
    private String posLabel(Cell cell) {
        if (cell.getCellType() == CellType.HOME) {
            return cap(cell.getColour()) + " Home";
        }
        if (cell.getCellType() == CellType.HOME_STRAIGHT) {
            int slot = (cell.getId() - 52) % 5 + 1; // 1..4 counting from entry
            return cap(cell.getColour()) + " Home Str. " + slot;
        }
        return String.valueOf(cell.getId());
    }
}
