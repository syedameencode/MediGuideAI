package com.mediguide.ui;

import com.mediguide.dao.UserDAO;
import com.mediguide.database.DatabaseManager;
import com.mediguide.models.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.function.Consumer;

public class LoginView extends BorderPane {

    private final UserDAO userDAO = new UserDAO();
    private final Consumer<User> onLoginSuccess;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private ToggleGroup roleGroup;
    private RadioButton adminRadio;
    private RadioButton doctorRadio;

    public LoginView(Consumer<User> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        initUI();
    }

    private void initUI() {
        // Root split pane layout
        HBox rootBox = new HBox();
        rootBox.setAlignment(Pos.CENTER);
        rootBox.setStyle("-fx-background-color: #0f172a;");

        // 1. Left Hero Panel (Modern Clinical Branding)
        VBox heroPanel = new VBox(20);
        heroPanel.getStyleClass().add("login-hero");
        heroPanel.setAlignment(Pos.CENTER_LEFT);
        heroPanel.setPrefWidth(480);
        heroPanel.setPadding(new Insets(50));

        Label brandBadge = new Label("CLINICAL DECISION SUPPORT SYSTEM");
        brandBadge.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 20; -fx-font-size: 11;");

        Label brandTitle = new Label("MediGuide AI");
        brandTitle.setStyle("-fx-font-size: 38px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Label brandTagline = new Label("Intelligent Healthcare Diagnosis &\nExplainable Clinical Decision Support");
        brandTagline.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8; -fx-line-spacing: 4;");

        VBox featureList = new VBox(14);
        featureList.setPadding(new Insets(20, 0, 0, 0));
        featureList.getChildren().addAll(
                createFeatureRow("Dual Decision Engine", "Machine learning prediction + rule-based explainability"),
                createFeatureRow("Transparent AI Reasoning", "Explicit evidence factors, risk confidence, and alternatives"),
                createFeatureRow("Human-in-the-Loop Oversight", "Clinician validation, modification, and feedback simulation"),
                createFeatureRow("Persistent SQL Records", "Structured clinical data store for patient vitals and audit trails")
        );

        heroPanel.getChildren().addAll(brandBadge, brandTitle, brandTagline, featureList);
        HBox.setHgrow(heroPanel, Priority.ALWAYS);

        // 2. Right Auth Panel
        VBox rightContainer = new VBox();
        rightContainer.setAlignment(Pos.CENTER);
        rightContainer.setPrefWidth(460);
        rightContainer.setPadding(new Insets(40));
        rightContainer.setStyle("-fx-background-color: #f8fafc;");

        VBox card = new VBox(18);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(380);

        Label welcomeTitle = new Label("Sign In");
        welcomeTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label welcomeSub = new Label("Authenticate with Admin or Doctor role");
        welcomeSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Role Toggle (Admin vs Doctor)
        HBox roleBox = new HBox(20);
        roleBox.setAlignment(Pos.CENTER_LEFT);
        roleGroup = new ToggleGroup();

        adminRadio = new RadioButton("Admin Login");
        adminRadio.setToggleGroup(roleGroup);
        adminRadio.setStyle("-fx-font-weight: 600; -fx-text-fill: #1e293b;");
        adminRadio.setSelected(true);

        doctorRadio = new RadioButton("Doctor Login");
        doctorRadio.setToggleGroup(roleGroup);
        doctorRadio.setStyle("-fx-font-weight: 600; -fx-text-fill: #1e293b;");

        roleBox.getChildren().addAll(adminRadio, doctorRadio);

        // Username Field
        VBox userFieldBox = new VBox(6);
        Label userLabel = new Label("Username / Staff ID");
        userLabel.getStyleClass().add("form-label");
        usernameField = new TextField("admin");
        usernameField.setPromptText("Enter your username");
        userFieldBox.getChildren().addAll(userLabel, usernameField);

        // Password Field
        VBox passFieldBox = new VBox(6);
        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("form-label");
        passwordField = new PasswordField();
        passwordField.setText("admin123");
        passwordField.setPromptText("Enter password");
        passFieldBox.getChildren().addAll(passLabel, passwordField);

        // Error message label
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: 600;");
        errorLabel.setVisible(false);

        // Sign In Button
        Button signInBtn = new Button("Sign In to MediGuide AI");
        signInBtn.getStyleClass().add("btn-primary");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setOnAction(e -> handleLogin());

        // Enter key shortcut
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> handleLogin());

        // Quick Demo Fill Buttons
        VBox quickDemoBox = new VBox(8);
        quickDemoBox.setAlignment(Pos.CENTER);
        quickDemoBox.setPadding(new Insets(10, 0, 0, 0));

        Label quickDemoLabel = new Label("Quick Demo Credentials for Evaluation:");
        quickDemoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: 600;");

        HBox demoBtnRow = new HBox(10);
        demoBtnRow.setAlignment(Pos.CENTER);

        Button fillAdminBtn = new Button("Use Admin (admin/admin123)");
        fillAdminBtn.getStyleClass().add("btn-secondary");
        fillAdminBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
        fillAdminBtn.setOnAction(e -> {
            adminRadio.setSelected(true);
            usernameField.setText("admin");
            passwordField.setText("admin123");
            errorLabel.setVisible(false);
        });

        Button fillDoctorBtn = new Button("Use Doctor (doctor/doctor123)");
        fillDoctorBtn.getStyleClass().add("btn-secondary");
        fillDoctorBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
        fillDoctorBtn.setOnAction(e -> {
            doctorRadio.setSelected(true);
            usernameField.setText("doctor");
            passwordField.setText("doctor123");
            errorLabel.setVisible(false);
        });

        demoBtnRow.getChildren().addAll(fillAdminBtn, fillDoctorBtn);
        quickDemoBox.getChildren().addAll(quickDemoLabel, demoBtnRow);

        card.getChildren().addAll(
                welcomeTitle, welcomeSub, roleBox,
                userFieldBox, passFieldBox, errorLabel,
                signInBtn, quickDemoBox
        );

        rightContainer.getChildren().add(card);
        HBox.setHgrow(rightContainer, Priority.ALWAYS);

        rootBox.getChildren().addAll(heroPanel, rightContainer);
        setCenter(rootBox);
    }

    private HBox createFeatureRow(String title, String desc) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4, javafx.scene.paint.Color.web("#38bdf8"));

        VBox textCol = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9; -fx-font-size: 13px;");
        Label d = new Label(desc);
        d.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        textCol.getChildren().addAll(t, d);

        row.getChildren().addAll(dot, textCol);
        return row;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        User user = userDAO.authenticate(username, password);
        if (user == null) {
            showError("Invalid username or password. Please try again.");
            return;
        }

        // Verify role selection if specified
        boolean requiresAdmin = adminRadio.isSelected();
        if (requiresAdmin && !user.isAdmin()) {
            showError("Account is not authorized as Administrator. Select Doctor login.");
            return;
        }

        errorLabel.setVisible(false);
        if (onLoginSuccess != null) {
            onLoginSuccess.accept(user);
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
