package com.mediguide.ui;

import com.mediguide.database.DatabaseManager;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;
import com.mediguide.models.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.Map;

public class MainLayout extends BorderPane {

    private final User currentUser;
    private final Runnable onLogout;

    private VBox sidebar;
    private HBox topNavbar;
    private StackPane centerContainer;

    private Label currentSectionTitle;
    private final Map<String, Button> navButtons = new HashMap<>();
    private String activeSection = "DASHBOARD";

    public MainLayout(User currentUser, Runnable onLogout) {
        this.currentUser = currentUser;
        this.onLogout = onLogout;
        initUI();
    }

    private void initUI() {
        // 1. Sidebar Navigation
        sidebar = createSidebar();
        setLeft(sidebar);

        // 2. Center & Top Header
        BorderPane rightPane = new BorderPane();

        topNavbar = createTopNavbar();
        rightPane.setTop(topNavbar);

        centerContainer = new StackPane();
        centerContainer.setStyle("-fx-background-color: #f8fafc;");
        rightPane.setCenter(centerContainer);

        setCenter(rightPane);

        // Load initial Dashboard
        navigateTo("DASHBOARD");
    }

    private VBox createSidebar() {
        VBox box = new VBox(10);
        box.getStyleClass().add("sidebar");

        // Brand Banner
        VBox brandBox = new VBox(2);
        Label title = new Label("MediGuide AI");
        title.getStyleClass().add("brand-title");

        Label sub = new Label("Explainable Decision Support");
        sub.getStyleClass().add("brand-subtitle");

        brandBox.getChildren().addAll(title, sub);

        // Navigation Sections
        Label navLabel = new Label("CLINICAL NAVIGATION");
        navLabel.getStyleClass().add("nav-section-label");

        Button dashBtn = createNavButton("📊  Dashboard Overview", "DASHBOARD");
        Button patientsBtn = createNavButton("👥  Patients Registry", "PATIENTS");
        Button vitalsBtn = createNavButton("🩺  Enter Clinical Vitals", "DIAGNOSIS");
        Button historyBtn = createNavButton("📜  Patient History & Audit", "HISTORY");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User Profile & Logout at bottom of sidebar
        VBox userFooter = new VBox(8);
        userFooter.setStyle("-fx-background-color: #1e293b; -fx-padding: 12; -fx-background-radius: 8;");

        Label uName = new Label(currentUser.getFullName());
        uName.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-font-size: 12px;");

        Label uRole = new Label(currentUser.getRole() + " ACCESS");
        uRole.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        userFooter.getChildren().addAll(uName, uRole, logoutBtn);

        box.getChildren().addAll(
                brandBox, navLabel, dashBtn, patientsBtn, vitalsBtn, historyBtn,
                spacer, userFooter
        );

        return box;
    }

    private Button createNavButton(String text, String sectionKey) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setOnAction(e -> navigateTo(sectionKey));
        navButtons.put(sectionKey, btn);
        return btn;
    }

    private HBox createTopNavbar() {
        HBox bar = new HBox(16);
        bar.getStyleClass().add("top-navbar");
        bar.setAlignment(Pos.CENTER_LEFT);

        currentSectionTitle = new Label("Dashboard Overview");
        currentSectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // User Pill
        HBox userPill = new HBox(6);
        userPill.setAlignment(Pos.CENTER);
        userPill.getStyleClass().add("user-badge");
        Label userText = new Label("👤 " + currentUser.getFullName() + " [" + currentUser.getRole() + "]");
        userText.getStyleClass().add("user-badge-text");
        userPill.getChildren().add(userText);

        bar.getChildren().addAll(currentSectionTitle, spacer, userPill);
        return bar;
    }

    public void navigateTo(String sectionKey) {
        activeSection = sectionKey;

        // Update active nav button styles
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(sectionKey)) {
                if (!entry.getValue().getStyleClass().contains("nav-button-active")) {
                    entry.getValue().getStyleClass().add("nav-button-active");
                }
            } else {
                entry.getValue().getStyleClass().remove("nav-button-active");
            }
        }

        Node viewNode;
        switch (sectionKey) {
            case "DASHBOARD":
                currentSectionTitle.setText("Clinical Dashboard & Cohort Analytics");
                viewNode = new DashboardView(currentUser, this::navigateTo, this::openDiagnosisForPatient);
                break;
            case "PATIENTS":
                currentSectionTitle.setText("Patient Registry & Clinical Records");
                viewNode = new PatientManagementView(currentUser, this::openDiagnosisForPatient, this::openHistoryForPatient);
                break;
            case "DIAGNOSIS":
                currentSectionTitle.setText("Medical Vitals & Diagnostic Input");
                viewNode = new MedicalDataEntryView(currentUser, null, this::showExplainableResult);
                break;
            case "HISTORY":
                currentSectionTitle.setText("Patient History & Clinical Timeline");
                viewNode = new PatientHistoryView(null);
                break;
            case "DATABASE":
                currentSectionTitle.setText("SQL Database Status & Review 1 Milestones");
                viewNode = new DatabaseInfoView();
                break;
            default:
                viewNode = new DashboardView(currentUser, this::navigateTo, this::openDiagnosisForPatient);
        }

        centerContainer.getChildren().setAll(viewNode);
    }

    public void openDiagnosisForPatient(Patient patient) {
        currentSectionTitle.setText("Medical Vitals Entry for: " + patient.getName());
        Node view = new MedicalDataEntryView(currentUser, patient, this::showExplainableResult);
        centerContainer.getChildren().setAll(view);

        // Highlight Diagnosis tab in sidebar
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            if ("DIAGNOSIS".equals(entry.getKey())) {
                if (!entry.getValue().getStyleClass().contains("nav-button-active")) {
                    entry.getValue().getStyleClass().add("nav-button-active");
                }
            } else {
                entry.getValue().getStyleClass().remove("nav-button-active");
            }
        }
    }

    public void openHistoryForPatient(Patient patient) {
        currentSectionTitle.setText("Clinical History: " + patient.getName());
        Node view = new PatientHistoryView(patient);
        centerContainer.getChildren().setAll(view);

        // Highlight History tab in sidebar
        for (Map.Entry<String, Button> entry : navButtons.entrySet()) {
            if ("HISTORY".equals(entry.getKey())) {
                if (!entry.getValue().getStyleClass().contains("nav-button-active")) {
                    entry.getValue().getStyleClass().add("nav-button-active");
                }
            } else {
                entry.getValue().getStyleClass().remove("nav-button-active");
            }
        }
    }

    public void showExplainableResult(Patient patient, RiskAssessmentResult result) {
        currentSectionTitle.setText("Explainable Decision Support Card: " + patient.getName());
        Node view = new ExplainableResultView(patient, result, currentUser, () -> navigateTo("DIAGNOSIS"));
        centerContainer.getChildren().setAll(view);
    }
}
