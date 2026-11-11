package com.mediguide.util;

import javafx.scene.Scene;
import javafx.scene.Node;

import java.util.HashSet;
import java.util.Set;

/**
 * ThemeManager — Global Dark / Light Mode manager for MediGuide AI.
 * Dynamically switches application scenes between Light and Dark themes.
 */
public class ThemeManager {

    public enum Theme {
        LIGHT, DARK
    }

    private static Theme currentTheme = Theme.LIGHT;
    private static final Set<Scene> registeredScenes = new HashSet<>();

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public static void registerScene(Scene scene) {
        if (scene != null) {
            registeredScenes.add(scene);
            applyThemeToScene(scene, currentTheme);
        }
    }

    public static void setTheme(Theme theme) {
        currentTheme = theme;
        for (Scene scene : registeredScenes) {
            applyThemeToScene(scene, theme);
        }
    }

    public static void toggleTheme() {
        setTheme(currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }

    public static void applyThemeToScene(Scene scene, Theme theme) {
        if (scene == null || scene.getRoot() == null) return;
        Node root = scene.getRoot();
        if (theme == Theme.DARK) {
            if (!root.getStyleClass().contains("dark-theme")) {
                root.getStyleClass().add("dark-theme");
            }
            root.getStyleClass().remove("light-theme");
        } else {
            if (!root.getStyleClass().contains("light-theme")) {
                root.getStyleClass().add("light-theme");
            }
            root.getStyleClass().remove("dark-theme");
        }
    }

    public static void applyThemeToNode(Node node) {
        if (node == null) return;
        if (currentTheme == Theme.DARK) {
            if (!node.getStyleClass().contains("dark-theme")) {
                node.getStyleClass().add("dark-theme");
            }
            node.getStyleClass().remove("light-theme");
        } else {
            if (!node.getStyleClass().contains("light-theme")) {
                node.getStyleClass().add("light-theme");
            }
            node.getStyleClass().remove("dark-theme");
        }
    }
}
