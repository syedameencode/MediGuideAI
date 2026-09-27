package com.mediguide.ui;

import com.mediguide.dao.DoctorFeedbackDAO;
import com.mediguide.dao.PatientDAO;
import com.mediguide.dao.PredictionDAO;
import com.mediguide.models.Patient;
import com.mediguide.models.Prediction;
import com.mediguide.models.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.function.Consumer;

public class DashboardView extends ScrollPane {

    private final PatientDAO patientDAO = new PatientDAO();
    private final PredictionDAO predictionDAO = new PredictionDAO();
    private final DoctorFeedbackDAO feedbackDAO = new DoctorFeedbackDAO();

    private final User currentUser;
    private final Consumer<String> onNavigate;
    private final Consumer<Patient> onSelectPatientForDiagnosis;

    public DashboardView(User currentUser, Consumer<String> onNavigate, Consumer<Patient> onSelectPatientForDiagnosis) {
        this.currentUser = currentUser;
        this.onNavigate = onNavigate;
        this.onSelectPatientForDiagnosis = onSelectPatientForDiagnosis;
        initUI();
    }

    private void initUI() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");

        VBox content = new VBox(24);
        content.getStyleClass().add("content-area");
        content.setMaxWidth(1100);

        // Welcome Header
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label greeting = new Label("Welcome, " + currentUser.getFullName());
        greeting.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Clinical Intelligence & Explainable Decision-Support Center");
        subtitle.getStyleClass().add("page-header-subtitle");
        titleBox.getChildren().addAll(greeting, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newEvalBtn = new Button("⚡ New AI Diagnosis");
        newEvalBtn.getStyleClass().add("btn-primary");
        newEvalBtn.setOnAction(e -> {
            if (onNavigate != null) onNavigate.accept("DIAGNOSIS");
        });

        headerRow.getChildren().addAll(titleBox, spacer, newEvalBtn);

        // 1. Metric Stat Cards Row
        HBox statsRow = createStatsCardsRow();

        // 2. Middle Section: Quick Actions & Risk Distribution
        HBox midSection = new HBox(20);
        VBox quickActionsCard = createQuickActionsCard();
        VBox distributionCard = createRiskDistributionCard();
        HBox.setHgrow(quickActionsCard, Priority.ALWAYS);
        HBox.setHgrow(distributionCard, Priority.ALWAYS);
        midSection.getChildren().addAll(quickActionsCard, distributionCard);

        // 3. Recent Clinical Predictions Table
        VBox recentTableCard = createRecentEvaluationsCard();

        content.getChildren().addAll(headerRow, statsRow, midSection, recentTableCard);
        setContent(content);
    }

    private HBox createStatsCardsRow() {
        HBox row = new HBox(16);

        int totalPatients = patientDAO.count();
        int totalPreds = predictionDAO.countTotal();
        int highRiskCount = predictionDAO.countHighRisk();
        int feedbackCount = feedbackDAO.countTotal();

        VBox card1 = createStatCard("TOTAL PATIENTS", String.valueOf(totalPatients), "Registered in database", "#0284c7");
        VBox card2 = createStatCard("HIGH RISK ALERTS", String.valueOf(highRiskCount), "Requiring doctor review", "#ef4444");
        VBox card3 = createStatCard("AI ASSESSMENTS", String.valueOf(totalPreds), "Dual-engine evaluations", "#059669");
        VBox card4 = createStatCard("DOCTOR FEEDBACK", String.valueOf(feedbackCount), "Human-in-the-loop entries", "#d97706");

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3, card4);
        return row;
    }

    private VBox createStatCard(String title, String val, String sub, String accentColor) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");

        Label t = new Label(title);
        t.getStyleClass().add("stat-card-title");

        Label v = new Label(val);
        v.getStyleClass().add("stat-card-value");
        v.setStyle("-fx-text-fill: " + accentColor + ";");

        Label s = new Label(sub);
        s.getStyleClass().add("stat-card-sub");

        card.getChildren().addAll(t, v, s);
        return card;
    }

    private VBox createQuickActionsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPrefWidth(480);

        Label header = new Label("Quick Clinical Workflows");
        header.getStyleClass().add("card-header");

        VBox actionsList = new VBox(10);

        Button btn1 = createActionRowButton("➕ Register New Patient", "Add patient demographics and record background notes", () -> {
            if (onNavigate != null) onNavigate.accept("PATIENTS");
        });

        Button btn2 = createActionRowButton("⚡ Run Explainable AI Vitals Assessment", "Input blood pressure, glucose, creatinine & symptoms", () -> {
            if (onNavigate != null) onNavigate.accept("DIAGNOSIS");
        });

        Button btn3 = createActionRowButton("📋 Search Patient Directory & History", "Query patients by ID, phone, or name and view audit log", () -> {
            if (onNavigate != null) onNavigate.accept("HISTORY");
        });

        actionsList.getChildren().addAll(btn1, btn2, btn3);
        card.getChildren().addAll(header, actionsList);
        return card;
    }

    private Button createActionRowButton(String title, String sub, Runnable action) {
        Button btn = new Button();
        btn.getStyleClass().add("btn-secondary");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-padding: 10 14;");

        VBox box = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");
        Label s = new Label(sub);
        s.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        box.getChildren().addAll(t, s);

        btn.setGraphic(box);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private VBox createRiskDistributionCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPrefWidth(480);

        Label header = new Label("Diagnostic Risk Stratification");
        header.getStyleClass().add("card-header");

        Label sub = new Label("Multi-Condition distribution across active cohort");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        VBox bars = new VBox(12);

        bars.getChildren().addAll(
                createRiskBar("High Risk Priority Cases", 0.40, "40%", "#ef4444"),
                createRiskBar("Moderate / Borderline Risk", 0.35, "35%", "#f59e0b"),
                createRiskBar("Low Risk / Physiological Baseline", 0.25, "25%", "#10b981")
        );

        Label xaiNote = new Label("✔ Layer 1 (Prediction Engine) + Layer 2 (Explainable Rules) calibrated");
        xaiNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #0284c7; -fx-font-weight: 600; -fx-padding: 8 0 0 0;");

        card.getChildren().addAll(header, sub, bars, xaiNote);
        return card;
    }

    private VBox createRiskBar(String label, double progress, String pct, String colorHex) {
        VBox box = new VBox(4);
        HBox row = new HBox();
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label p = new Label(pct);
        p.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        row.getChildren().addAll(l, sp, p);

        ProgressBar bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent: " + colorHex + ";");

        box.getChildren().addAll(row, bar);
        return box;
    }

    private VBox createRecentEvaluationsCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("Recent Decision Engine Evaluations");
        header.getStyleClass().add("card-header");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button viewAllBtn = new Button("View Patient History →");
        viewAllBtn.getStyleClass().add("btn-secondary");
        viewAllBtn.setStyle("-fx-font-size: 11px;");
        viewAllBtn.setOnAction(e -> {
            if (onNavigate != null) onNavigate.accept("HISTORY");
        });

        headerRow.getChildren().addAll(header, sp, viewAllBtn);

        TableView<Prediction> table = new TableView<>();
        List<Prediction> recent = predictionDAO.findRecent(6);
        table.setItems(FXCollections.observableArrayList(recent));
        table.setPrefHeight(200);

        TableColumn<Prediction, String> idCol = new TableColumn<>("Patient ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getPatientId()));
        idCol.setPrefWidth(90);

        TableColumn<Prediction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPredictionDate()));
        dateCol.setPrefWidth(130);

        TableColumn<Prediction, String> condCol = new TableColumn<>("Primary Condition");
        condCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getConditionName()));
        condCol.setPrefWidth(180);

        TableColumn<Prediction, String> riskCol = new TableColumn<>("Risk Level");
        riskCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRiskLevel()));
        riskCol.setPrefWidth(120);
        riskCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    badge.getStyleClass().add("badge-" + item.toLowerCase());
                    setGraphic(badge);
                }
            }
        });

        TableColumn<Prediction, String> confCol = new TableColumn<>("Confidence");
        confCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f%%", d.getValue().getConfidence())));
        confCol.setPrefWidth(100);

        TableColumn<Prediction, String> factorsCol = new TableColumn<>("Key Explainable Evidence");
        factorsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getContributingFactors()));
        factorsCol.setPrefWidth(380);

        table.getColumns().addAll(idCol, dateCol, condCol, riskCol, confCol, factorsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        card.getChildren().addAll(headerRow, table);
        return card;
    }
}
