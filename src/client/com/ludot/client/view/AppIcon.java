package com.ludot.client.view;

import com.ludot.protocol.Colour;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

/**
 * The application icon, drawn in code (no image files to ship): a rounded tile in the four board colours with a
 * Ludo pawn on a white disc. It is used for the window, the taskbar and, where the system allows it, the Dock.
 */
public final class AppIcon {

    private static final double DRAWN = 512;
    private static final int[] SIZES = {16, 32, 48, 64, 128, 256, 512};
    // macOS draws app icons inside a transparent margin, about a tenth of the width on each side
    private static final double DOCK_FILL = 0.8;
    private static final Color NAVY = Color.web("#2b3366");

    private AppIcon() {
    }

    /** Gives the window its icon (and the Dock its icon on macOS, with the margin macOS expects). Must run on the FX thread. */
    public static void install(Stage stage) {
        Node tile = tile();
        List<Image> images = new ArrayList<>();
        for (int size : SIZES) {
            images.add(render(tile, size));
        }
        stage.getIcons().setAll(images);
        dock(renderPadded(tile, 512));
    }

    // the Dock icon is not the window icon: it is set through the Taskbar, which not every system offers
    private static void dock(Image large) {
        try {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                Taskbar.getTaskbar().setIconImage(toAwt(large));
            }
        } catch (RuntimeException | LinkageError e) {
            // no Dock or taskbar icon: the window icon is still set
        }
    }

    private static Node tile() {
        Group quadrants = new Group(
                quarter(0, 0, Colour.RED), quarter(DRAWN / 2, 0, Colour.GREEN),
                quarter(0, DRAWN / 2, Colour.BLUE), quarter(DRAWN / 2, DRAWN / 2, Colour.YELLOW));
        Rectangle shape = new Rectangle(DRAWN, DRAWN);
        shape.setArcWidth(DRAWN * 0.42);
        shape.setArcHeight(DRAWN * 0.42);
        quadrants.setClip(shape);

        Circle disc = new Circle(DRAWN / 2, DRAWN / 2, 176, Color.WHITE);
        disc.setEffect(new DropShadow(18, 0, 6, Color.rgb(43, 51, 102, 0.35)));
        double pawnHeight = 250;
        PawnMascot pawn = new PawnMascot(NAVY, pawnHeight);
        pawn.setLayoutX(DRAWN / 2);
        pawn.setLayoutY(DRAWN / 2 - pawnHeight * 0.52);
        return new Group(quadrants, disc, pawn);
    }

    private static Rectangle quarter(double x, double y, Colour colour) {
        Rectangle block = new Rectangle(x, y, DRAWN / 2, DRAWN / 2);
        block.setFill(Theme.strong(colour));
        return block;
    }

    private static Image render(Node tile, int size) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(new Scale(size / DRAWN, size / DRAWN));
        return tile.snapshot(params, new WritableImage(size, size));
    }

    // the tile shrunk into the middle of a transparent square, so it sits in the Dock at the same size as other apps
    private static Image renderPadded(Node tile, int size) {
        double scale = size * DOCK_FILL / DRAWN;
        double margin = size * (1 - DOCK_FILL) / 2;
        Affine fit = new Affine();
        fit.appendTranslation(margin, margin);
        fit.appendScale(scale, scale);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(fit);
        return tile.snapshot(params, new WritableImage(size, size));
    }

    private static BufferedImage toAwt(Image image) {
        PixelReader pixels = image.getPixelReader();
        BufferedImage out = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        return out;
    }
}
