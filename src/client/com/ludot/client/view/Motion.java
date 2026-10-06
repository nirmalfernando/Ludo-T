package com.ludot.client.view;

import javafx.animation.Animation;
import javafx.scene.Node;

/** Keeps decorative animations cheap: they only run while their node is actually on screen. */
final class Motion {

    private Motion() {
    }

    /** Plays the animation while the node is in a scene and stops it as soon as the node leaves. */
    static void whileShowing(Node node, Animation animation) {
        node.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                animation.stop();
            } else {
                animation.play();
            }
        });
        if (node.getScene() != null) {
            animation.play();
        }
    }
}
