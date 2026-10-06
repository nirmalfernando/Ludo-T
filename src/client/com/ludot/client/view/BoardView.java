package com.ludot.client.view;

import com.ludot.client.view.BoardLayout.Point;
import com.ludot.protocol.BoardDto;
import com.ludot.protocol.BoardGeometry;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Location;
import com.ludot.protocol.MoveOption;
import com.ludot.protocol.MysteryCellDto;
import com.ludot.protocol.PieceDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

/**
 * Draws the board and its sixteen pieces from a {@link BoardDto}. The view computes nothing about the
 * game: it places each piece where the server says it is, animates the move, highlights the pieces the
 * player may move and previews where a hovered piece would land.
 */
public final class BoardView extends Pane {

    private static final double CELL = 40;
    private static final double PIECE_RADIUS = CELL * 0.32;
    private static final double MOVE_MILLIS = 260;

    private final Map<String, PieceNode> pieces = new HashMap<>();
    private final Map<String, Point> positions = new HashMap<>();
    private final Map<Colour, Rectangle> bases = new EnumMap<>(Colour.class);
    private final Circle ghost = new Circle(PIECE_RADIUS, Color.TRANSPARENT);
    private final StackPane mystery = new StackPane();
    private final Label mysteryLabel = new Label();
    private final ScaleTransition mysteryPulse;
    private boolean firstUpdate = true;
    private List<MoveOption> offered = List.of();

    public BoardView() {
        double size = BoardLayout.GRID * CELL;
        setPrefSize(size, size);
        setMinSize(size, size);
        setMaxSize(size, size);
        setStyle("-fx-background-color: #fafafa; -fx-border-color: #90a4ae; -fx-border-width: 2;");
        drawBases();
        drawTrack();
        drawHomeStraights();
        drawCentre();
        ghost.setStroke(Theme.HIGHLIGHT);
        ghost.setStrokeWidth(3);
        ghost.getStrokeDashArray().addAll(6.0, 5.0);
        ghost.setVisible(false);
        ghost.setMouseTransparent(true);
        mystery.getChildren().addAll(new Circle(CELL * 0.42, Color.web("#8e24aa", 0.55)), mysteryLabel);
        mysteryLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12;");
        mystery.setMouseTransparent(true);
        mystery.setVisible(false);
        mysteryPulse = new ScaleTransition(Duration.millis(700), mystery);
        mysteryPulse.setToX(1.15);
        mysteryPulse.setToY(1.15);
        mysteryPulse.setAutoReverse(true);
        mysteryPulse.setCycleCount(Animation.INDEFINITE);
        getChildren().addAll(mystery);
        createPieces();
        getChildren().add(ghost);
    }

    // ------------------------------------------------------------------ static drawing

    private void drawBases() {
        for (Colour colour : Colour.values()) {
            int[] origin = BoardLayout.baseOrigin(colour);
            Rectangle base = new Rectangle(origin[0] * CELL, origin[1] * CELL, 6 * CELL, 6 * CELL);
            base.setFill(Theme.strong(colour));
            bases.put(colour, base);
            Rectangle inner = new Rectangle((origin[0] + 0.7) * CELL, (origin[1] + 0.7) * CELL, 4.6 * CELL, 4.6 * CELL);
            inner.setFill(Color.WHITE);
            inner.setArcWidth(CELL);
            inner.setArcHeight(CELL);
            getChildren().addAll(base, inner);
            for (int i = 0; i < BoardGeometry.PIECES_PER_PLAYER; i++) {
                Point slot = BoardLayout.base(colour, i);
                Circle spot = new Circle(slot.x() * CELL, slot.y() * CELL, PIECE_RADIUS + 5, Theme.tint(colour));
                spot.setStroke(Theme.strong(colour));
                getChildren().add(spot);
            }
        }
    }

    private void drawTrack() {
        for (int id = 0; id < BoardGeometry.TRACK_SIZE; id++) {
            int[] cell = BoardLayout.trackGridCell(id);
            Rectangle square = cellRect(cell, Color.WHITE);
            for (Colour colour : Colour.values()) {
                if (id == BoardGeometry.startCell(colour)) {
                    square.setFill(Theme.tint(colour));
                    Label star = new Label("★");
                    star.setStyle("-fx-text-fill: " + Theme.hex(colour) + "; -fx-font-size: 18;");
                    star.setLayoutX(cell[0] * CELL + 11);
                    star.setLayoutY(cell[1] * CELL + 6);
                    star.setMouseTransparent(true);
                    getChildren().addAll(square, star);
                    square = null;
                    break;
                }
            }
            if (square != null) {
                getChildren().add(square);
            }
        }
    }

    private void drawHomeStraights() {
        for (Colour colour : Colour.values()) {
            for (int step = 0; step < BoardGeometry.HOME_STRAIGHT_LENGTH; step++) {
                Rectangle square = cellRect(BoardLayout.homeGridCell(colour, step),
                        step == BoardGeometry.HOME_STRAIGHT_LENGTH - 1 ? Theme.strong(colour) : Theme.tint(colour));
                getChildren().add(square);
            }
        }
    }

    private void drawCentre() {
        double lo = 6 * CELL;
        double hi = 9 * CELL;
        double mid = 7.5 * CELL;
        getChildren().addAll(
                triangle(Colour.RED, lo, lo, lo, hi, mid, mid),
                triangle(Colour.GREEN, lo, lo, hi, lo, mid, mid),
                triangle(Colour.YELLOW, hi, lo, hi, hi, mid, mid),
                triangle(Colour.BLUE, lo, hi, hi, hi, mid, mid));
    }

    private static Polygon triangle(Colour colour, double... points) {
        Polygon polygon = new Polygon(points);
        polygon.setFill(Theme.strong(colour));
        polygon.setStroke(Color.WHITE);
        return polygon;
    }

    private static Rectangle cellRect(int[] cell, Color fill) {
        Rectangle square = new Rectangle(cell[0] * CELL, cell[1] * CELL, CELL, CELL);
        square.setFill(fill);
        square.setStroke(Theme.GRID_LINE);
        square.setStrokeType(StrokeType.INSIDE);
        return square;
    }

    // ------------------------------------------------------------------ pieces

    private void createPieces() {
        for (Colour colour : Colour.values()) {
            for (int i = 0; i < BoardGeometry.PIECES_PER_PLAYER; i++) {
                PieceNode node = new PieceNode(colour, i, PIECE_RADIUS);
                Point at = BoardLayout.base(colour, i);
                node.setTranslateX(at.x() * CELL);
                node.setTranslateY(at.y() * CELL);
                pieces.put(key(colour, i), node);
                positions.put(key(colour, i), at);
                getChildren().add(node);
            }
        }
    }

    private static String key(Colour colour, int index) {
        return colour + "#" + index;
    }

    /** Moves every piece to where the board says it is, animating pieces that changed place. */
    public void update(BoardDto board, boolean animate) {
        Map<String, List<PieceDto>> shared = new HashMap<>();
        for (PieceDto piece : board.pieces()) {
            shared.computeIfAbsent(spotKey(piece), k -> new ArrayList<>()).add(piece);
        }
        for (PieceDto piece : board.pieces()) {
            List<PieceDto> together = shared.get(spotKey(piece));
            together.sort(Comparator.comparing(PieceDto::colour).thenComparingInt(PieceDto::index));
            Point offset = BoardLayout.stackOffset(together.indexOf(piece), together.size());
            Point base = pointOf(piece);
            moveTo(key(piece.colour(), piece.index()), new Point(base.x() + offset.x(), base.y() + offset.y()),
                    animate && !firstUpdate);
            PieceNode node = pieces.get(key(piece.colour(), piece.index()));
            node.showState(piece);
            node.toFront();
        }
        showMystery(board.mysteryCells());
        firstUpdate = false;
        ghost.toFront();
    }

    // pieces on one track cell, home-straight cell or home share a spot and are fanned out around it
    private static String spotKey(PieceDto piece) {
        return switch (piece.location()) {
            case BASE -> "base" + piece.colour() + piece.index();
            case TRACK, BRIEFING -> "t" + piece.cell();
            case HOME_STRAIGHT -> "h" + piece.colour() + piece.homeStep();
            case HOME -> "H" + piece.colour();
        };
    }

    private static Point pointOf(PieceDto piece) {
        return switch (piece.location()) {
            case BASE -> BoardLayout.base(piece.colour(), piece.index());
            case TRACK, BRIEFING -> BoardLayout.track(piece.cell());
            case HOME_STRAIGHT -> BoardLayout.homeStraight(piece.colour(), piece.homeStep());
            case HOME -> BoardLayout.homeStraight(piece.colour(), BoardGeometry.HOME_STRAIGHT_LENGTH - 1);
        };
    }

    private void moveTo(String pieceKey, Point target, boolean animate) {
        PieceNode node = pieces.get(pieceKey);
        Point previous = positions.put(pieceKey, target);
        double x = target.x() * CELL;
        double y = target.y() * CELL;
        if (!animate || previous == null || previous.equals(target)) {
            node.setTranslateX(x);
            node.setTranslateY(y);
            return;
        }
        TranslateTransition slide = new TranslateTransition(Duration.millis(MOVE_MILLIS), node);
        slide.setToX(x);
        slide.setToY(y);
        slide.play();
    }

    private void showMystery(List<MysteryCellDto> cells) {
        if (cells.isEmpty()) {
            mystery.setVisible(false);
            mysteryPulse.stop();
            return;
        }
        MysteryCellDto cell = cells.get(0);
        Point at = BoardLayout.track(cell.cell());
        mystery.setLayoutX(at.x() * CELL - CELL * 0.42);
        mystery.setLayoutY(at.y() * CELL - CELL * 0.42);
        mysteryLabel.setText("?" + cell.roundsLeft());
        mystery.setVisible(true);
        mysteryPulse.play();
    }

    // ------------------------------------------------------------------ turn highlighting and moves

    /** Marks whose turn it is by glowing that colour's base. */
    public void setActiveColour(Colour active) {
        bases.forEach((colour, base) -> base.setEffect(colour == active ? new DropShadow(26, Theme.HIGHLIGHT) : null));
    }

    /**
     * Highlights the pieces the player can move and calls {@code onChoose} with the option for the piece clicked.
     * Passing no options clears all highlighting.
     */
    public void offerMoves(Colour mine, List<MoveOption> options, Consumer<MoveOption> onChoose) {
        offered = options;
        ghost.setVisible(false);
        for (int i = 0; i < BoardGeometry.PIECES_PER_PLAYER; i++) {
            pieces.get(key(mine, i)).setMovable(false, null, null, null);
        }
        for (MoveOption option : options) {
            for (int pieceIndex : new int[]{option.pieceIndex(), option.partnerPieceIndex()}) {
                if (pieceIndex >= 0) {
                    pieces.get(key(mine, pieceIndex)).setMovable(true, () -> onChoose.accept(option),
                            () -> preview(mine, option), () -> ghost.setVisible(false));
                }
            }
        }
    }

    /** Removes every highlight and preview. */
    public void clearMoves() {
        for (Colour colour : Colour.values()) {
            offerMoves(colour, List.of(), option -> { });
        }
    }

    private void preview(Colour mine, MoveOption option) {
        Point at = switch (option.to()) {
            case TRACK, BRIEFING -> BoardLayout.track(option.toCell());
            case HOME_STRAIGHT -> BoardLayout.homeStraight(mine, option.toCell());
            case HOME -> BoardLayout.homeStraight(mine, BoardGeometry.HOME_STRAIGHT_LENGTH - 1);
            case BASE -> BoardLayout.base(mine, Math.max(0, option.pieceIndex()));
        };
        if (option.to() == Location.BASE) {
            return;
        }
        ghost.setCenterX(at.x() * CELL);
        ghost.setCenterY(at.y() * CELL);
        ghost.setVisible(true);
        ghost.toFront();
    }

    /** The options currently offered, for tests and tooling. */
    List<MoveOption> offeredOptions() {
        return offered;
    }
}
