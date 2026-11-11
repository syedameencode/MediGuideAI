package com.mediguide;

import com.mediguide.database.DatabaseManager;
import com.mediguide.models.User;
import com.mediguide.ui.LoginView;
import com.mediguide.ui.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // Initialize database and tables
        DatabaseManager.getInstance();

        // Initialize Weka ML Pipeline on background thread so JavaFX UI opens instantly
        new Thread(() -> {
            com.mediguide.ml.WekaMLService.getInstance().initialize();
        }, "Weka-Init-Thread").start();

        primaryStage.setTitle("MediGuide AI — Intelligent Healthcare Diagnosis & Explainable Decision Support");
        primaryStage.setMinWidth(1080);
        primaryStage.setMinHeight(700);

        showLoginScreen();
        primaryStage.show();
    }

    public void showLoginScreen() {
        LoginView loginView = new LoginView(this::handleLoginSuccess);
        Scene scene = new Scene(loginView, 1140, 740);

        // Attach global CSS
        String css = getClass().getResource("/styles.css") != null
                ? getClass().getResource("/styles.css").toExternalForm()
                : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }

        com.mediguide.util.ThemeManager.registerScene(scene);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    private void handleLoginSuccess(User user) {
        MainLayout mainLayout = new MainLayout(user, this::showLoginScreen);
        Scene scene = new Scene(mainLayout, 1180, 780);

        String css = getClass().getResource("/styles.css") != null
                ? getClass().getResource("/styles.css").toExternalForm()
                : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }

        com.mediguide.util.ThemeManager.registerScene(scene);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
