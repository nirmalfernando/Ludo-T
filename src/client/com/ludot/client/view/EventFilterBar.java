package com.ludot.client.view;

import java.util.EnumSet;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

// the chosen set is published as a property so the log can re-filter itself
final class EventFilterBar extends FlowPane {

    private final Set<EventCategory> selected = EnumSet.noneOf(EventCategory.class);
    private final ObjectProperty<Set<EventCategory>> filter = new SimpleObjectProperty<>(Set.of());
    private final ToggleButton all = new ToggleButton("All");

    EventFilterBar() {
        super(6, 6);
        all.getStyleClass().add("filter-chip");
        all.setSelected(true);
        all.setOnAction(event -> clear());
        all.setTooltip(new Tooltip("Everything, including history from before you joined (which has no type)"));
        getChildren().add(all);
        for (EventCategory category : EventCategory.values()) {
            ToggleButton chip = new ToggleButton(category.label());
            chip.getStyleClass().add("filter-chip");
            chip.setOnAction(event -> toggle(category, chip.isSelected()));
            getChildren().add(chip);
        }
    }

    ObjectProperty<Set<EventCategory>> filterProperty() {
        return filter;
    }

    private void toggle(EventCategory category, boolean on) {
        if (on) {
            selected.add(category);
        } else {
            selected.remove(category);
        }
        publish();
    }

    private void clear() {
        selected.clear();
        for (var node : getChildren()) {
            if (node != all) {
                ((ToggleButton) node).setSelected(false);
            }
        }
        publish();
    }

    // "All" shows exactly when nothing else is chosen
    private void publish() {
        all.setSelected(selected.isEmpty());
        filter.set(Set.copyOf(selected));
    }
}
