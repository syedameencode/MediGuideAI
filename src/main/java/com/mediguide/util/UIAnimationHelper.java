package com.mediguide.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * UIAnimationHelper — Provides subtle, fast JavaFX animations for the
 * MediGuide AI clinical health intelligence interface.
 */
public class UIAnimationHelper {

    /**
     * Subtle fade-in entrance transition (220ms).
     */
    public static void fadeIn(Node node) {
        if (node == null) return;
        node.setOpacity(0.0);
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(Interpolator.EASE_OUT);
        ft.play();
    }

    /**
     * Subtle slide-up and fade-in entrance transition (250ms).
     */
    public static void slideInUp(Node node) {
        if (node == null) return;
        node.setOpacity(0.0);
        node.setTranslateY(12.0);

        FadeTransition ft = new FadeTransition(Duration.millis(250), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(Duration.millis(250), node);
        tt.setFromY(12.0);
        tt.setToY(0.0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(node, ft, tt);
        pt.play();
    }

    /**
     * Subtle pulse scale animation on button click before executing callback (160ms).
     */
    public static void pulseClick(Node node, Runnable onFinished) {
        if (node == null) {
            if (onFinished != null) onFinished.run();
            return;
        }

        ScaleTransition st1 = new ScaleTransition(Duration.millis(80), node);
        st1.setToX(0.96);
        st1.setToY(0.96);

        ScaleTransition st2 = new ScaleTransition(Duration.millis(80), node);
        st2.setToX(1.0);
        st2.setToY(1.0);

        SequentialTransition seq = new SequentialTransition(st1, st2);
        seq.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        seq.play();
    }
}
