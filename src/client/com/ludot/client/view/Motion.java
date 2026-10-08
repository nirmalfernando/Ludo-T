package com.ludot.client.view;

import javafx.animation.Animation;
import javafx.scene.Node;

final class Motion {

    private Motion() {
    }

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
