package com.ludot.client.view;

import com.ludot.protocol.Speed;
import java.util.function.Consumer;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/** Slow | Normal | Fast as one rounded segmented control. */
final class SpeedPicker extends HBox {

    private static final Speed[] ORDER = {Speed.SLOW, Speed.NORMAL, Speed.FAST};

    private final ToggleGroup group = new ToggleGroup();
    private boolean selecting;

    /** @param onPick called when the person picks a speed (not when {@link #select} sets one from outside) */
    SpeedPicker(Consumer<Speed> onPick) {
        for (int i = 0; i < ORDER.length; i++) {
            ToggleButton segment = new ToggleButton(label(ORDER[i]));
            segment.setUserData(ORDER[i]);
            segment.setToggleGroup(group);
            segment.getStyleClass().add("segment");
            segment.getStyleClass().add(i == 0 ? "segment-first" : i == ORDER.length - 1 ? "segment-last" : "segment-middle");
            Toggles.keepSelected(segment);
            getChildren().add(segment);
        }
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (!selecting && now != null) {
                onPick.accept((Speed) now.getUserData());
            }
        });
    }

    /** Shows this speed as the chosen one without reporting it as a pick. */
    void select(Speed speed) {
        selecting = true;
        group.getToggles().stream().filter(toggle -> toggle.getUserData() == speed)
                .findFirst().ifPresent(toggle -> toggle.setSelected(true));
        selecting = false;
    }

    private static String label(Speed speed) {
        return speed.name().charAt(0) + speed.name().substring(1).toLowerCase();
    }
}
