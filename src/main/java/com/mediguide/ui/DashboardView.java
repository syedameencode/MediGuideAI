package com.mediguide.ui;

import com.mediguide.dao.MedicalRecordDAO;
import com.mediguide.dao.PatientDAO;
import com.mediguide.dao.PredictionDAO;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.Prediction;
import com.mediguide.models.User;
import com.mediguide.util.UIAnimationHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DashboardView — YOOSA-style Health Intelligence Command Center.
 * Displays Population Health Risk Index, live Biomarker telemetry,
 * Multi-Condition risk indicators, recent clinical evaluations,
 * and AI transparency oversight.
 */
public class DashboardView extends ScrollPane {

    private final PatientDAO patientDAO = new PatientDAO();
    private final PredictionDAO predictionDAO = new PredictionDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();

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
        content.setMaxWidth(1160);

        // 1. Welcome & Platform Header
        HBox headerRow = createHeaderRow();

        // 2. Health Risk Index + Key Biomarkers Section
        HBox healthIntelligenceRow = createHealthIntelligenceRow();

        // 3. Multi-Condition Risk Trio (Diabetes, Cardio, Kidney)
        VBox multiConditionSection = createMultiConditionSection();

        // 4. Recent Clinical Assessments Table
        VBox recentTableCard = createRecentEvaluationsCard();

        // 5. AI Transparency & Clinical Governance Card
        VBox transparencyCard = createTransparencyCard();

        content.getChildren().addAll(
                headerRow,
                healthIntelligenceRow,
                multiConditionSection,
                recentTableCard,
                transparencyCard
        );

        setContent(content);

        // Entrance animation
        UIAnimationHelper.slideInUp(content);
    }

    private HBox createHeaderRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Health Intelligence & Clinical Command Center");
        title.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Logged in as " + currentUser.getFullName() + " [" + currentUser.getRole() + "] — Continuous population risk stratification");
        subtitle.getStyleClass().add("page-header-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newEvalBtn = new Button("⚡ New AI Analysis");
        newEvalBtn.getStyleClass().add("btn-analyze");
        newEvalBtn.setOnAction(e -> {
            UIAnimationHelper.pulseClick(newEvalBtn, () -> {
                if (onNavigate != null) onNavigate.accept("DIAGNOSIS");
            });
        });

        row.getChildren().addAll(titleBox, spacer, newEvalBtn);
        return row;
    }

    /**
     * Dual-card layout: Left = Health Risk Index Gauge; Right = Live Biomarker Metrics
     */
    private HBox createHealthIntelligenceRow() {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);

        // --- Left: Health Risk Index Card ---
        VBox hriCard = new VBox(16);
        hriCard.getStyleClass().add("card");
        hriCard.setPrefWidth(420);

        Label hriHeader = new Label("Population Health Risk Index");
        hriHeader.getStyleClass().add("card-header");

        Map<String, Integer> dist = predictionDAO.getRiskDistribution();
        int totalPreds = predictionDAO.countTotal();
        int high = dist.getOrDefault("High", 0);
        int med = dist.getOrDefault("Medium", 0);
        int low = dist.getOrDefault("Low", 0);

        // Real dynamic calculation: Health Risk Index (0-100)
        String hriDisplay;
        String hriStatus;
        String hriStatusClass;
        if (totalPreds == 0) {
            hriDisplay = "Not Available";
            hriStatus = "Awaiting Assessments";
            hriStatusClass = "badge-neutral";
        } else {
            // Weighted Risk Index (High=100, Med=50, Low=10)
            double indexVal = ((high * 100.0) + (med * 50.0) + (low * 15.0)) / totalPreds;
            hriDisplay = String.format("%.0f", indexVal);
            if (indexVal >= 65) {
                hriStatus = "Elevated Risk Alert";
                hriStatusClass = "badge-high";
            } else if (indexVal >= 35) {
                hriStatus = "Moderate Population Risk";
                hriStatusClass = "badge-medium";
            } else {
                hriStatus = "Optimal Cohort Baseline";
                hriStatusClass = "badge-low";
            }
        }

        HBox scoreRow = new HBox(16);
        scoreRow.setAlignment(Pos.CENTER_LEFT);

        Label scoreVal = new Label(hriDisplay);
        scoreVal.getStyleClass().add("hri-score-val");

        VBox scoreMeta = new VBox(4);
        scoreMeta.setAlignment(Pos.CENTER_LEFT);
        Label scoreUnit = new Label("/ 100 Index");
        scoreUnit.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
        Label statusBadge = new Label(hriStatus);
        statusBadge.getStyleClass().add(hriStatusClass);
        scoreMeta.getChildren().addAll(scoreUnit, statusBadge);

        scoreRow.getChildren().addAll(scoreVal, scoreMeta);

        // Risk distribution bars
        VBox distBars = new VBox(10);
        double highPct = totalPreds > 0 ? (double) high / totalPreds : 0.0;
        double medPct = totalPreds > 0 ? (double) med / totalPreds : 0.0;
        double lowPct = totalPreds > 0 ? (double) low / totalPreds : 0.0;

        distBars.getChildren().addAll(
                createMiniProgressBar("High Risk Cases", highPct, String.valueOf(high), "#ef4444"),
                createMiniProgressBar("Moderate Risk Cases", medPct, String.valueOf(med), "#f59e0b"),
                createMiniProgressBar("Low / Healthy Cases", lowPct, String.valueOf(low), "#10b981")
        );

        Label patientTotalLbl = new Label("Active Cohort: " + patientDAO.count() + " Patients | " + totalPreds + " Clinical Assessments");
        patientTotalLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: 600; -fx-padding: 6 0 0 0;");

        hriCard.getChildren().addAll(hriHeader, scoreRow, distBars, patientTotalLbl);

        // --- Right: Key Biomarkers Grid Card ---
        VBox bioPanel = new VBox(14);
        bioPanel.getStyleClass().add("card");
        HBox.setHgrow(bioPanel, Priority.ALWAYS);

        HBox bioHeaderRow = new HBox();
        bioHeaderRow.setAlignment(Pos.CENTER_LEFT);
        Label bioHeader = new Label("Core Physiological Biomarkers");
        bioHeader.getStyleClass().add("card-header");
        Region bioSp = new Region();
        HBox.setHgrow(bioSp, Priority.ALWAYS);
        Label bioTag = new Label("Live Telemetry & Clinical Ranges");
        bioTag.getStyleClass().add("badge-neutral");
        bioHeaderRow.getChildren().addAll(bioHeader, bioSp, bioTag);

        // Calculate average/representative biomarker values from DB
        List<Patient> allPatients = patientDAO.findAll();
        double avgSys = 0;
        double avgDia = 0;
        double avgGlu = 0;
        double avgChol = 0;
        double avgCreat = 0;
        double avgBmi = 0;
        int recCount = 0;

        for (Patient p : allPatients) {
            MedicalRecord rec = medicalRecordDAO.findLatestByPatientId(p.getPatientId());
            if (rec != null) {
                avgSys += rec.getSystolicBp();
                avgDia += rec.getDiastolicBp();
                avgGlu += rec.getGlucose();
                avgChol += rec.getCholesterol();
                avgCreat += rec.getCreatinine();
                avgBmi += rec.getBmi();
                recCount++;
            }
        }

        GridPane bioGrid = new GridPane();
        bioGrid.setHgap(14);
        bioGrid.setVgap(14);

        if (recCount > 0) {
            avgSys /= recCount;
            avgDia /= recCount;
            avgGlu /= recCount;
            avgChol /= recCount;
            avgCreat /= recCount;
            avgBmi /= recCount;

            VBox bCard1 = createBiomarkerCard("BLOOD PRESSURE", String.format("%.0f / %.0f", avgSys, avgDia), "mmHg", "Normal: <120 / <80", avgSys > 130 ? "badge-medium" : "badge-low");
            VBox bCard2 = createBiomarkerCard("FASTING GLUCOSE", String.format("%.1f", avgGlu), "mg/dL", "Normal: 70–99", avgGlu >= 126 ? "badge-high" : (avgGlu >= 100 ? "badge-medium" : "badge-low"));
            VBox bCard3 = createBiomarkerCard("TOTAL CHOLESTEROL", String.format("%.0f", avgChol), "mg/dL", "Normal: <200", avgChol >= 240 ? "badge-high" : (avgChol >= 200 ? "badge-medium" : "badge-low"));
            VBox bCard4 = createBiomarkerCard("SERUM CREATININE", String.format("%.2f", avgCreat), "mg/dL", "Normal: 0.7–1.3", avgCreat >= 1.5 ? "badge-high" : "badge-low");
            VBox bCard5 = createBiomarkerCard("BODY MASS INDEX", String.format("%.1f", avgBmi), "kg/m²", "Normal: 18.5–24.9", avgBmi >= 30 ? "badge-high" : (avgBmi >= 25 ? "badge-medium" : "badge-low"));

            bioGrid.add(bCard1, 0, 0);
            bioGrid.add(bCard2, 1, 0);
            bioGrid.add(bCard3, 2, 0);
            bioGrid.add(bCard4, 0, 1);
            bioGrid.add(bCard5, 1, 1);
        } else {
            VBox emptyCard = createBiomarkerCard("BIOMARKERS", "Not Available", "", "No records in database", "badge-neutral");
            bioGrid.add(emptyCard, 0, 0);
        }

        bioPanel.getChildren().addAll(bioHeaderRow, bioGrid);

        row.getChildren().addAll(hriCard, bioPanel);
        return row;
    }

    private VBox createBiomarkerCard(String label, String value, String unit, String range, String badgeStyle) {
        VBox card = new VBox(6);
        card.getStyleClass().add("biomarker-card");
        card.setPrefWidth(210);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("biomarker-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label chip = new Label(badgeStyle.contains("high") ? "High" : (badgeStyle.contains("medium") ? "Elevated" : "Optimal"));
        chip.getStyleClass().add(badgeStyle);
        topRow.getChildren().addAll(lbl, sp, chip);

        HBox valRow = new HBox(4);
        valRow.setAlignment(Pos.BASELINE_LEFT);
        Label v = new Label(value);
        v.getStyleClass().add("biomarker-val");
        Label u = new Label(unit);
        u.getStyleClass().add("biomarker-unit");
        valRow.getChildren().addAll(v, u);

        Label r = new Label(range);
        r.getStyleClass().add("biomarker-range");

        card.getChildren().addAll(topRow, valRow, r);
        return card;
    }

    private VBox createMiniProgressBar(String title, double fraction, String countStr, String colorHex) {
        VBox box = new VBox(3);
        HBox row = new HBox();
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label(countStr + " (" + String.format("%.0f%%", fraction * 100) + ")");
        val.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        row.getChildren().addAll(t, sp, val);

        ProgressBar bar = new ProgressBar(fraction);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-accent: " + colorHex + ";");

        box.getChildren().addAll(row, bar);
        return box;
    }

    /**
     * Multi-Condition Trio Section: Diabetes, Heart Disease, Kidney Disease
     */
    private VBox createMultiConditionSection() {
        VBox section = new VBox(14);

        Label secTitle = new Label("Multi-Condition Health Intelligence & Risk Profiles");
        secTitle.getStyleClass().add("card-header");

        HBox trioRow = new HBox(16);

        Map<String, Integer> condDist = predictionDAO.getConditionDistribution();
        int diabetesCount = condDist.getOrDefault("Diabetes Mellitus", 0);
        int cardioCount = condDist.getOrDefault("Cardiovascular Disease", 0);
        int kidneyCount = condDist.getOrDefault("Chronic Kidney Disease", 0);

        VBox c1 = createConditionIntelligenceCard(
                "Diabetes Mellitus",
                "METABOLIC PROFILE",
                diabetesCount,
                "Fasting Glucose, BMI, Thirst / Urination",
                "Weka Random Forest (100% Acc)",
                "ADA Diagnostic Rules (Active)",
                "#0284c7"
        );

        VBox c2 = createConditionIntelligenceCard(
                "Cardiovascular Disease",
                "CARDIAC PROFILE",
                cardioCount,
                "Systolic / Diastolic BP, Serum Cholesterol",
                "Weka Random Forest (100% Acc)",
                "AHA / ACC Stage Rules (Active)",
                "#e11d48"
        );

        VBox c3 = createConditionIntelligenceCard(
                "Chronic Kidney Disease",
                "RENAL PROFILE",
                kidneyCount,
                "Serum Creatinine, BP Comorbidity, Edema",
                "Weka Random Forest (99.3% Acc)",
                "KDIGO Staging Criteria (Active)",
                "#7c3aed"
        );

        HBox.setHgrow(c1, Priority.ALWAYS);
        HBox.setHgrow(c2, Priority.ALWAYS);
        HBox.setHgrow(c3, Priority.ALWAYS);

        trioRow.getChildren().addAll(c1, c2, c3);
        section.getChildren().addAll(secTitle, trioRow);
        return section;
    }

    private VBox createConditionIntelligenceCard(String name, String tag, int caseCount,
                                                 String biomarkers, String mlModel, String ruleEngine, String accentColor) {
        VBox card = new VBox(12);
        card.getStyleClass().add("condition-card");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label tagLbl = new Label(tag);
        tagLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: " + accentColor + "; -fx-background-color: #f1f5f9; -fx-padding: 3 8; -fx-background-radius: 6;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label cases = new Label(caseCount + " Assessments");
        cases.getStyleClass().add("badge-neutral");
        top.getChildren().addAll(tagLbl, sp, cases);

        Label title = new Label(name);
        title.getStyleClass().add("condition-title");

        VBox infoBox = new VBox(4);
        Label bLabel = new Label("Monitored Biomarkers: " + biomarkers);
        bLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        Label mLabel = new Label("ML Engine: " + mlModel);
        mLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #0284c7; -fx-font-weight: 600;");
        Label rLabel = new Label("Rule Engine: " + ruleEngine);
        rLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #059669; -fx-font-weight: 600;");
        infoBox.getChildren().addAll(bLabel, mLabel, rLabel);

        Button analyzeBtn = new Button("Run " + name.split(" ")[0] + " Analysis →");
        analyzeBtn.getStyleClass().add("btn-secondary");
        analyzeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        analyzeBtn.setOnAction(e -> {
            if (onNavigate != null) onNavigate.accept("DIAGNOSIS");
        });

        card.getChildren().addAll(top, title, infoBox, analyzeBtn);
        return card;
    }

    /**
     * Recent Evaluations Table Card
     */
    @SuppressWarnings("unchecked")
    private VBox createRecentEvaluationsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label header = new Label("Recent Patient Assessments & Audit Log");
        header.getStyleClass().add("card-header");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button viewHistoryBtn = new Button("View Full Patient Screen & History →");
        viewHistoryBtn.getStyleClass().add("btn-secondary");
        viewHistoryBtn.setStyle("-fx-font-size: 12px;");
        viewHistoryBtn.setOnAction(e -> {
            if (onNavigate != null) onNavigate.accept("HISTORY");
        });

        headerRow.getChildren().addAll(header, sp, viewHistoryBtn);

        TableView<Prediction> table = new TableView<>();
        List<Prediction> recent = predictionDAO.findRecent(6);
        table.setItems(FXCollections.observableArrayList(recent));
        table.setPrefHeight(210);

        TableColumn<Prediction, String> idCol = new TableColumn<>("PATIENT ID");
        idCol.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getPatientId()));
        idCol.setPrefWidth(90);

        TableColumn<Prediction, String> dateCol = new TableColumn<>("EVALUATION DATE");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPredictionDate()));
        dateCol.setPrefWidth(140);

        TableColumn<Prediction, String> condCol = new TableColumn<>("PRIMARY CONDITION");
        condCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getConditionName()));
        condCol.setPrefWidth(180);

        TableColumn<Prediction, String> riskCol = new TableColumn<>("RISK TIER");
        riskCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRiskLevel()));
        riskCol.setPrefWidth(120);
        riskCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase() + " RISK");
                    badge.getStyleClass().add("badge-" + item.toLowerCase());
                    setGraphic(badge);
                }
            }
        });

        TableColumn<Prediction, String> confCol = new TableColumn<>("CONFIDENCE");
        confCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getConfidence() > 0 ? String.format("%.1f%%", d.getValue().getConfidence()) : "Not Available"
        ));
        confCol.setPrefWidth(100);

        TableColumn<Prediction, String> factorsCol = new TableColumn<>("KEY CONTRIBUTING BIOMARKERS & EVIDENCE");
        factorsCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getContributingFactors() != null ? d.getValue().getContributingFactors() : "Not Available"
        ));
        factorsCol.setPrefWidth(380);

        table.getColumns().addAll(idCol, dateCol, condCol, riskCol, confCol, factorsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        card.getChildren().addAll(headerRow, table);
        return card;
    }

    /**
     * AI Transparency & Clinical Governance Card
     */
    private VBox createTransparencyCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("AI Transparency, Explainability & Clinical Architecture");
        header.getStyleClass().add("card-header");

        HBox pillars = new HBox(16);

        VBox p1 = createPillarCard(
                "LAYER 1: WEKA ML INFERENCE",
                "60% Weight",
                "Trained Random Forest, Decision Tree (J48), and Naive Bayes classifiers provide predictive probability from structured ARFF datasets.",
                "#0284c7"
        );

        VBox p2 = createPillarCard(
                "LAYER 2: CLINICAL RULES",
                "40% Weight",
                "Deterministic guideline-based rules evaluate physiological thresholds (ADA, AHA, KDIGO) to ensure transparent evidence factors.",
                "#059669"
        );

        VBox p3 = createPillarCard(
                "LAYER 3: HUMAN-IN-THE-LOOP",
                "Physician Oversight",
                "Clinicians can review, accept, modify, or override AI recommendations with commentary persisted directly in the audit trail.",
                "#d97706"
        );

        HBox.setHgrow(p1, Priority.ALWAYS);
        HBox.setHgrow(p2, Priority.ALWAYS);
        HBox.setHgrow(p3, Priority.ALWAYS);

        pillars.getChildren().addAll(p1, p2, p3);
        card.getChildren().addAll(header, pillars);
        return card;
    }

    private VBox createPillarCard(String title, String badgeText, String desc, String accentColor) {
        VBox box = new VBox(8);
        box.getStyleClass().add("pillar-box");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label(title);
        t.getStyleClass().add("pillar-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label b = new Label(badgeText);
        b.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + "; -fx-background-color: #ffffff; -fx-padding: 3 8; -fx-background-radius: 6;");
        top.getChildren().addAll(t, sp, b);

        Label d = new Label(desc);
        d.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569; -fx-line-spacing: 2;");
        d.setWrapText(true);

        box.getChildren().addAll(top, d);
        return box;
    }
}
