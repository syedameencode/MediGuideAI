package com.mediguide.ui;

import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;
import com.mediguide.models.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MainLayout — Shell layout for MediGuide AI.
 * Hosts dark navigation sidebar, top navbar, dynamic center container,
 * and conversational AI assistant sidebar.
 */
public class MainLayout extends BorderPane {

    private final User currentUser;
    private final Runnable onLogout;

    private VBox sidebar;
    private HBox topNavbar;
    private StackPane centerContainer;

    private Label currentSectionTitle;
    private final Map<String, Button> navButtons = new HashMap<>();
    private String activeSection;
    private ChatbotPanel chatbotPanel;

    public MainLayout(User currentUser, Runnable onLogout) {
        this.currentUser = currentUser;
        this.onLogout = onLogout;
        initUI();
    }

    private void initUI() {
        // 1. Sidebar Navigation (Role-Based)
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

        // 3. AI Chatbot Sidebar
        chatbotPanel = new ChatbotPanel();
        setRight(chatbotPanel);

        // Initial view based on Role
        boolean isDoctor = "DOCTOR".equalsIgnoreCase(currentUser.getRole());
        if (isDoctor) {
            navigateTo("DIAGNOSIS");
        } else {
            navigateTo("DASHBOARD");
        }
    }

    private VBox createSidebar() {
        VBox box = new VBox(8);
        box.getStyleClass().add("sidebar");

        // Brand Banner
        VBox brandBox = new VBox(4);
        brandBox.setPadding(new Insets(0, 0, 16, 0));

        Label brandBadge = new Label("HEALTH INTELLIGENCE PLATFORM");
        brandBadge.getStyleClass().add("brand-badge");

        Label title = new Label("MediGuide AI");
        title.getStyleClass().add("brand-title");

        Label sub = new Label("Explainable Clinical Decision Support");
        sub.getStyleClass().add("brand-subtitle");

        brandBox.getChildren().addAll(brandBadge, title, sub);
        box.getChildren().add(brandBox);

        boolean isDoctor = "DOCTOR".equalsIgnoreCase(currentUser.getRole());

        if (isDoctor) {
            // Simplified Doctor Portal Navigation
            Label navLabel = new Label("PHYSICIAN PORTAL");
            navLabel.getStyleClass().add("nav-section-label");

            Button vitalsBtn = createNavButton("🩺  AI Patient Analysis", "DIAGNOSIS");
            Button historyBtn = createNavButton("📜  Patient Health Screen", "HISTORY");

            box.getChildren().addAll(navLabel, vitalsBtn, historyBtn);

        } else {
            // Full Clinician / Admin Navigation
            Label navLabel = new Label("CLINICAL NAVIGATION");
            navLabel.getStyleClass().add("nav-section-label");

            Button dashBtn = createNavButton("📊  Health Dashboard", "DASHBOARD");
            Button patientsBtn = createNavButton("👥  Patient Directory", "PATIENTS");
            Button vitalsBtn = createNavButton("⚡  AI Patient Analysis", "DIAGNOSIS");
            Button historyBtn = createNavButton("📜  Patient Health Screen", "HISTORY");

            box.getChildren().addAll(navLabel, dashBtn, patientsBtn, vitalsBtn, historyBtn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User Profile & Logout at bottom of sidebar
        VBox userFooter = new VBox(8);
        userFooter.getStyleClass().add("user-footer-card");

        HBox uHeader = new HBox(8);
        uHeader.setAlignment(Pos.CENTER_LEFT);

        Label uRole = new Label(currentUser.getRole().toUpperCase());
        uRole.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-font-weight: 800; -fx-background-color: rgba(56, 189, 248, 0.15); -fx-padding: 2 6; -fx-background-radius: 4;");

        Label uName = new Label(currentUser.getFullName());
        uName.setStyle("-fx-font-weight: 800; -fx-text-fill: #ffffff; -fx-font-size: 12px;");

        uHeader.getChildren().addAll(uRole, uName);

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.getStyleClass().add("btn-secondary");
        logoutBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 14; -fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-border-color: #334155;");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        userFooter.getChildren().addAll(uHeader, logoutBtn);
        box.getChildren().addAll(spacer, userFooter);

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

        currentSectionTitle = new Label("Health Intelligence Overview");
        currentSectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // User Profile Badge
        HBox userPill = new HBox(8);
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
                currentSectionTitle.setText("Clinical Dashboard & Population Risk Monitoring");
                viewNode = new DashboardView(currentUser, this::navigateTo, this::openDiagnosisForPatient);
                break;
            case "PATIENTS":
                currentSectionTitle.setText("Patient Registry & Demographic Records");
                viewNode = new PatientManagementView(currentUser, this::openDiagnosisForPatient, this::openHistoryForPatient);
                break;
            case "DIAGNOSIS":
                currentSectionTitle.setText("Patient Diagnostic Intake & AI Analysis");
                viewNode = new MedicalDataEntryView(currentUser, null, this::showExplainableResult);
                break;
            case "HISTORY":
                currentSectionTitle.setText("Patient Health Screen & Longitudinal History");
                PatientHistoryView histView = new PatientHistoryView(null);
                histView.setOnDiagnosePatient(this::openDiagnosisForPatient);
                viewNode = histView;
                break;
            default:
                viewNode = new DashboardView(currentUser, this::navigateTo, this::openDiagnosisForPatient);
        }

        centerContainer.getChildren().setAll(viewNode);
    }

    public void openDiagnosisForPatient(Patient patient) {
        currentSectionTitle.setText("AI Patient Analysis for: " + patient.getName());
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
        currentSectionTitle.setText("Patient Health Screen: " + patient.getName());
        PatientHistoryView view = new PatientHistoryView(patient);
        view.setOnDiagnosePatient(this::openDiagnosisForPatient);
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
        Node view = new ExplainableResultView(patient, result, currentUser, () -> navigateTo("DIAGNOSIS"),
                promptText -> chatbotPanel.analyzeContext(promptText));
        centerContainer.getChildren().setAll(view);
    }
}
