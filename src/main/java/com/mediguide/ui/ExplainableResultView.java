package com.mediguide.ui;

import com.mediguide.dao.DoctorFeedbackDAO;
import com.mediguide.dao.PredictionDAO;
import com.mediguide.models.DoctorFeedback;
import com.mediguide.models.Patient;
import com.mediguide.models.Prediction;
import com.mediguide.models.RiskAssessmentResult;
import com.mediguide.models.User;
import com.mediguide.util.PdfReportGenerator;
import com.mediguide.util.UIAnimationHelper;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * ExplainableResultView — Explainable Decision Card Screen.
 * Displays Overall Risk, ML Result, Rule Result, Hybrid Result,
 * Contributing Factors, Alternative Possibilities, and Recommendations
 * with strict backend-driven values and "Not Available" fallback.
 */
public class ExplainableResultView extends ScrollPane {

    private final Patient patient;
    private final RiskAssessmentResult result;
    private final User currentUser;
    private final Runnable onBackAction;
    private final Consumer<String> onAnalyzeWithGemini;

    private final PredictionDAO predictionDAO = new PredictionDAO();
    private final DoctorFeedbackDAO feedbackDAO = new DoctorFeedbackDAO();
    private int savedPredictionId = -1;
    private Label pdfStatusLabel;

    public ExplainableResultView(Patient patient, RiskAssessmentResult result, User currentUser, Runnable onBackAction, Consumer<String> onAnalyzeWithGemini) {
        this.patient = patient;
        this.result = result;
        this.currentUser = currentUser;
        this.onBackAction = onBackAction;
        this.onAnalyzeWithGemini = onAnalyzeWithGemini;

        savePredictionToDatabase();
        initUI();
    }

    private void savePredictionToDatabase() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String now = LocalDateTime.now().format(dtf);

        String factorsCombined = (result.getContributingFactors() != null && !result.getContributingFactors().isEmpty())
                ? String.join("; ", result.getContributingFactors())
                : "Not Available";

        String alternativesCombined = (result.getAlternativePossibilities() != null && !result.getAlternativePossibilities().isEmpty())
                ? String.join("; ", result.getAlternativePossibilities())
                : "Not Available";

        Prediction p = new Prediction(
                patient.getPatientId(),
                result.getPrimaryCondition() != null ? result.getPrimaryCondition() : "General Screening",
                result.getOverallRiskLevel() != null ? result.getOverallRiskLevel() : "Unknown",
                result.getConfidenceScore(),
                factorsCombined,
                alternativesCombined,
                now
        );
        p.setFusionMethod(result.getFusionStrategy() != null ? result.getFusionStrategy() : "Weighted Hybrid");
        p.setMlPrediction(result.getMlPrediction() != null ? result.getMlPrediction() : "Not Available");
        p.setRulePrediction(result.getRulePrediction() != null ? result.getRulePrediction() : "Not Available");

        savedPredictionId = predictionDAO.create(p);
    }

    private void initUI() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");

        VBox contentBox = new VBox(24);
        contentBox.getStyleClass().add("content-area");
        contentBox.setMaxWidth(1100);

        pdfStatusLabel = new Label();
        pdfStatusLabel.setVisible(false);
        pdfStatusLabel.setManaged(false);

        // Top Navigation Bar
        HBox topBar = createTopBar();

        // 1. Primary Diagnosis & Overall Risk Hero Card
        VBox heroCard = createPrimaryDiagnosisCard();

        // 2. Decision Engine Triad: ML Result | Rule Result | Hybrid Result
        HBox engineTriad = createEngineTriadRow();

        // 3. Contributing Evidence Checklist
        VBox factorsCard = createContributingFactorsCard();

        // 4. Clinical Recommendations & Differential Possibilities (Dual Column)
        HBox dualClinicalBox = new HBox(20);
        VBox altCard = createAlternativesCard();
        VBox recCard = createRecommendationsCard();
        HBox.setHgrow(altCard, Priority.ALWAYS);
        HBox.setHgrow(recCard, Priority.ALWAYS);
        dualClinicalBox.getChildren().addAll(altCard, recCard);

        // 5. Multi-Condition Cross-Analysis Section
        VBox multiConditionSection = createMultiConditionSection();

        // 6. Doctor Feedback & Clinical Override (Human-in-the-Loop)
        VBox feedbackCard = createDoctorFeedbackCard();

        contentBox.getChildren().addAll(
                topBar,
                pdfStatusLabel,
                heroCard,
                engineTriad,
                factorsCard,
                dualClinicalBox,
                multiConditionSection,
                feedbackCard
        );

        setContent(contentBox);

        // Entrance animation
        UIAnimationHelper.slideInUp(contentBox);
    }

    private HBox createTopBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Analysis Intake");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> {
            if (onBackAction != null) onBackAction.run();
        });

        VBox titleBox = new VBox(2);
        Label title = new Label("Explainable Decision Support Card");
        title.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Comprehensive transparent clinical reasoning for " + patient.getName() + " (#" + patient.getPatientId() + ")");
        subtitle.getStyleClass().add("page-header-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button pdfBtn = new Button("📄 Export PDF Clinical Report");
        pdfBtn.setStyle("-fx-background-color: #0e7490; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 20; -fx-cursor: hand;");
        pdfBtn.setOnAction(e -> exportPdfReport());

        Button geminiBtn = new Button("✨ AI Clinical Summary");
        geminiBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 9 18; -fx-background-radius: 20; -fx-cursor: hand;");
        geminiBtn.setOnAction(e -> {
            String context = "Analyze these patient results at a glance and provide a concise 2-3 sentence clinical summary: " +
                    "Patient: " + patient.getName() + " (Age " + patient.getAge() + "). " +
                    "Primary Condition: " + (result.getPrimaryCondition() != null ? result.getPrimaryCondition() : "N/A") + ". " +
                    "Overall Risk: " + (result.getOverallRiskLevel() != null ? result.getOverallRiskLevel() : "N/A") + ". " +
                    "Key Evidence: " + (result.getContributingFactors() != null ? String.join(", ", result.getContributingFactors()) : "None") + ".";
            if (onAnalyzeWithGemini != null) {
                onAnalyzeWithGemini.accept(context);
            }
        });

        bar.getChildren().addAll(backBtn, titleBox, spacer, geminiBtn, pdfBtn);
        return bar;
    }

    private void exportPdfReport() {
        try {
            File pdf = PdfReportGenerator.generateReport(patient, null, result, "reports");
            pdfStatusLabel.setText("✅ PDF Clinical Report generated successfully: " + pdf.getAbsolutePath());
            pdfStatusLabel.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #166534; -fx-border-color: #86efac; -fx-padding: 10 16; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 12px;");
            pdfStatusLabel.setManaged(true);
            pdfStatusLabel.setVisible(true);
        } catch (Exception ex) {
            pdfStatusLabel.setText("❌ PDF Generation failed: " + ex.getMessage());
            pdfStatusLabel.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #991b1b; -fx-border-color: #fca5a5; -fx-padding: 10 16; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 12px;");
            pdfStatusLabel.setManaged(true);
            pdfStatusLabel.setVisible(true);
        }
    }

    /**
     * Hero Card displaying Primary Condition & Overall Risk
     */
    private VBox createPrimaryDiagnosisCard() {
        VBox card = new VBox(16);
        String riskStr = result.getOverallRiskLevel() != null ? result.getOverallRiskLevel() : "Low";
        String level = riskStr.toLowerCase();
        card.getStyleClass().addAll("card", "decision-card-" + level);

        HBox headerRow = new HBox(20);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox condCol = new VBox(6);
        Label flag = new Label("PRIMARY CLINICAL DIAGNOSTIC DETERMINATION");
        flag.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.8px;");

        Label primaryLabel = new Label(result.getPrimaryCondition() != null ? result.getPrimaryCondition() : "Not Available");
        primaryLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        Label patientSub = new Label("Patient: " + patient.getName() + " (#" + patient.getPatientId() + ") | Age: " + patient.getAge() + " | Gender: " + patient.getGender());
        patientSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-font-weight: 500;");

        condCol.getChildren().addAll(flag, primaryLabel, patientSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Overall Risk Badge
        VBox riskCol = new VBox(6);
        riskCol.setAlignment(Pos.CENTER_RIGHT);

        Label overallRiskBadge = new Label(result.getOverallRiskLevel() != null ? result.getOverallRiskLevel().toUpperCase() + " RISK" : "Not Available");
        overallRiskBadge.getStyleClass().add("badge-" + level);
        overallRiskBadge.setStyle("-fx-font-size: 16px; -fx-padding: 8 22;");

        Label confLabel = new Label("Confidence: " + (result.getConfidenceScore() > 0 ? String.format("%.1f%%", result.getConfidenceScore()) : "Not Available"));
        confLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0284c7;");

        riskCol.getChildren().addAll(overallRiskBadge, confLabel);

        headerRow.getChildren().addAll(condCol, spacer, riskCol);

        // Disclaimer
        Label disclaimer = new Label("⚠ Clinical Support Notice: " + (result.getDisclaimer() != null ? result.getDisclaimer() : "Not Available"));
        disclaimer.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 8 0 0 0;");
        disclaimer.setWrapText(true);

        card.getChildren().addAll(headerRow, disclaimer);
        return card;
    }

    /**
     * Decision Engine Triad: ML Result | Rule Result | Hybrid Result
     */
    private HBox createEngineTriadRow() {
        HBox row = new HBox(16);

        // 1. ML Result Card
        VBox mlCard = new VBox(10);
        mlCard.getStyleClass().add("card");
        HBox.setHgrow(mlCard, Priority.ALWAYS);

        Label mlHeader = new Label("1. ML RESULT (Weka 3.8.6)");
        mlHeader.getStyleClass().add("pillar-title");

        String mlPred = (result.getMlPrediction() != null && !result.getMlPrediction().isEmpty() && !result.getMlPrediction().equalsIgnoreCase("Unavailable"))
                ? result.getMlPrediction()
                : "Not Available";
        Label mlPredLbl = new Label(mlPred);
        mlPredLbl.getStyleClass().add("pillar-value");

        String mlAlgo = result.getMlAlgorithm() != null && !result.getMlAlgorithm().isEmpty() ? result.getMlAlgorithm() : "Not Available";
        String mlConf = result.getMlConfidence() > 0 ? String.format("%.1f%%", result.getMlConfidence()) : "Not Available";

        Label mlMeta = new Label("Model: " + mlAlgo + "\nProbability: " + mlConf + "\nWeight: 60% in decision fusion");
        mlMeta.getStyleClass().add("pillar-sub");
        mlCard.getChildren().addAll(mlHeader, mlPredLbl, mlMeta);

        // 2. Rule Result Card
        VBox ruleCard = new VBox(10);
        ruleCard.getStyleClass().add("card");
        HBox.setHgrow(ruleCard, Priority.ALWAYS);

        Label ruleHeader = new Label("2. RULE RESULT (Clinical Logic)");
        ruleHeader.getStyleClass().add("pillar-title");

        String rulePred = result.getRuleRiskLevel() != null && !result.getRuleRiskLevel().isEmpty()
                ? result.getRuleRiskLevel()
                : "Not Available";
        Label rulePredLbl = new Label(rulePred);
        rulePredLbl.getStyleClass().add("pillar-value");

        String ruleScoreStr = result.getRuleScore() > 0 ? result.getRuleScore() + " / 100" : (result.getRuleRiskLevel() != null ? "Calculated" : "Not Available");
        Label ruleMeta = new Label("Diagnostic Score: " + ruleScoreStr + "\nGuideline Rules: ADA / AHA / KDIGO\nWeight: 40% in decision fusion");
        ruleMeta.getStyleClass().add("pillar-sub");
        ruleCard.getChildren().addAll(ruleHeader, rulePredLbl, ruleMeta);

        // 3. Hybrid Result Card
        VBox hybridCard = new VBox(10);
        hybridCard.getStyleClass().add("card");
        HBox.setHgrow(hybridCard, Priority.ALWAYS);

        Label hybridHeader = new Label("3. HYBRID RESULT (Decision Fusion)");
        hybridHeader.getStyleClass().add("pillar-title");

        String hybridPred = result.getOverallRiskLevel() != null ? result.getOverallRiskLevel() : "Not Available";
        Label hybridPredLbl = new Label(hybridPred);
        hybridPredLbl.getStyleClass().add("pillar-value");

        String fusionStr = result.getFusionStrategy() != null ? result.getFusionStrategy() : "Not Available";
        String confStr = result.getConfidenceScore() > 0 ? String.format("%.1f%%", result.getConfidenceScore()) : "Not Available";

        Label hybridMeta = new Label("Fusion: " + fusionStr + "\nCalibrated Confidence: " + confStr + "\nState: Validated Hybrid Output");
        hybridMeta.getStyleClass().add("pillar-sub");
        hybridCard.getChildren().addAll(hybridHeader, hybridPredLbl, hybridMeta);

        row.getChildren().addAll(mlCard, ruleCard, hybridCard);
        return row;
    }

    /**
     * Contributing Factors Checklist
     */
    private VBox createContributingFactorsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("Contributing Diagnostic Factors & Clinical Evidence");
        header.getStyleClass().add("card-header");

        Label sub = new Label("Explicit biomarkers, vital thresholds, and clinical symptoms that contributed to this determination:");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox factorsList = new VBox(8);
        List<String> factors = result.getContributingFactors();

        if (factors == null || factors.isEmpty()) {
            Label emptyLbl = new Label("Not Available");
            emptyLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            factorsList.getChildren().add(emptyLbl);
        } else {
            for (String factor : factors) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("factor-item");

                Label check = new Label("✔");
                check.setStyle("-fx-text-fill: #0284c7; -fx-font-weight: bold; -fx-font-size: 13px;");

                Label fText = new Label(factor);
                fText.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e293b; -fx-font-weight: 500;");
                fText.setWrapText(true);

                row.getChildren().addAll(check, fText);
                factorsList.getChildren().add(row);
            }
        }

        card.getChildren().addAll(header, sub, factorsList);
        return card;
    }

    /**
     * Alternative Possibilities (Differential Diagnoses)
     */
    private VBox createAlternativesCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label header = new Label("Alternative Possibilities (Differential Hypotheses)");
        header.getStyleClass().add("card-header");

        Label sub = new Label("Secondary etiologies considered by clinical rule evaluation:");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox list = new VBox(8);
        List<String> alts = result.getAlternativePossibilities();

        if (alts == null || alts.isEmpty()) {
            Label emptyLbl = new Label("Not Available");
            emptyLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            list.getChildren().add(emptyLbl);
        } else {
            for (String alt : alts) {
                Label chip = new Label("• " + alt);
                chip.getStyleClass().add("alternative-chip");
                chip.setWrapText(true);
                chip.setMaxWidth(Double.MAX_VALUE);
                list.getChildren().add(chip);
            }
        }

        card.getChildren().addAll(header, sub, list);
        return card;
    }

    /**
     * Clinical Recommendations
     */
    private VBox createRecommendationsCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label header = new Label("Clinical Recommendations & Protocol Plan");
        header.getStyleClass().add("card-header");

        Label sub = new Label("Guideline-directed clinical next steps and follow-up guidance:");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox list = new VBox(8);
        List<String> recs = result.getRecommendations();

        if (recs == null || recs.isEmpty()) {
            Label emptyLbl = new Label("Not Available");
            emptyLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            list.getChildren().add(emptyLbl);
        } else {
            int step = 1;
            for (String rec : recs) {
                HBox rBox = new HBox(10);
                rBox.setAlignment(Pos.CENTER_LEFT);
                rBox.getStyleClass().add("recommendation-box");

                Label num = new Label(String.valueOf(step++));
                num.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 12;");

                Label rText = new Label(rec);
                rText.setStyle("-fx-font-size: 12px; -fx-text-fill: #166534; -fx-font-weight: 500;");
                rText.setWrapText(true);

                rBox.getChildren().addAll(num, rText);
                list.getChildren().add(rBox);
            }
        }

        card.getChildren().addAll(header, sub, list);
        return card;
    }

    /**
     * Multi-Condition Cross-Analysis Section
     */
    private VBox createMultiConditionSection() {
        VBox section = new VBox(14);
        section.getStyleClass().add("card");

        Label header = new Label("Multi-Condition Comparative Cross-Analysis");
        header.getStyleClass().add("card-header");

        Label sub = new Label("Simultaneous evaluation across all three monitored medical conditions:");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        HBox cardsRow = new HBox(16);
        List<RiskAssessmentResult.ConditionEvaluation> evaluations = result.getConditionEvaluations();

        if (evaluations == null || evaluations.isEmpty()) {
            Label empty = new Label("Not Available");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            section.getChildren().addAll(header, sub, empty);
            return section;
        }

        for (RiskAssessmentResult.ConditionEvaluation eval : evaluations) {
            VBox cCard = new VBox(10);
            cCard.setStyle("-fx-background-color: #f8fafc; -fx-padding: 16; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");
            HBox.setHgrow(cCard, Priority.ALWAYS);

            HBox rTop = new HBox();
            rTop.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(eval.getConditionName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label badge = new Label(eval.getRiskLevel().toUpperCase() + " RISK");
            badge.getStyleClass().add("badge-" + eval.getRiskLevel().toLowerCase());
            rTop.getChildren().addAll(name, sp, badge);

            ProgressBar pBar = new ProgressBar(eval.getScore() / 100.0);
            pBar.setMaxWidth(Double.MAX_VALUE);
            String pColor = "#10b981";
            if ("High".equalsIgnoreCase(eval.getRiskLevel())) pColor = "#ef4444";
            else if ("Medium".equalsIgnoreCase(eval.getRiskLevel())) pColor = "#f59e0b";
            pBar.setStyle("-fx-accent: " + pColor + ";");

            Label scoreSummary = new Label(eval.getSummary() != null ? eval.getSummary() : "Score: " + eval.getScore());
            scoreSummary.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            scoreSummary.setWrapText(true);

            cCard.getChildren().addAll(rTop, pBar, scoreSummary);
            cardsRow.getChildren().add(cCard);
        }

        section.getChildren().addAll(header, sub, cardsRow);
        return section;
    }

    /**
     * Doctor Feedback & Clinical Override (Human-in-the-Loop AI)
     */
    private VBox createDoctorFeedbackCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("Clinician Oversight & Doctor Feedback Simulation (Human-in-the-Loop)");
        header.getStyleClass().add("card-header");

        Label sub = new Label("Authorized Physician: " + currentUser.getFullName() + " — Review the automated decision support above and record your clinical decision in the audit log:");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Radio Decision Group
        HBox radioBox = new HBox(28);
        radioBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup decisionGroup = new ToggleGroup();

        RadioButton acceptRadio = new RadioButton("Accept AI Recommendation");
        acceptRadio.setToggleGroup(decisionGroup);
        acceptRadio.setStyle("-fx-font-weight: 700; -fx-text-fill: #059669;");
        acceptRadio.setSelected(true);

        RadioButton modifyRadio = new RadioButton("Modify Risk Tier / Diagnosis");
        modifyRadio.setToggleGroup(decisionGroup);
        modifyRadio.setStyle("-fx-font-weight: 700; -fx-text-fill: #d97706;");

        RadioButton rejectRadio = new RadioButton("Reject Recommendation");
        rejectRadio.setToggleGroup(decisionGroup);
        rejectRadio.setStyle("-fx-font-weight: 700; -fx-text-fill: #dc2626;");

        radioBox.getChildren().addAll(acceptRadio, modifyRadio, rejectRadio);

        // Notes Text Area
        VBox notesBox = new VBox(6);
        Label notesLabel = new Label("Physician Clinical Notes & Rationale:");
        notesLabel.getStyleClass().add("form-label");
        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Enter clinical observations, diagnostic rationale, or medication orders...");
        commentsArea.setPrefRowCount(3);
        notesBox.getChildren().addAll(notesLabel, commentsArea);

        // Action Row
        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button submitFeedbackBtn = new Button("Record Doctor Decision to Database");
        submitFeedbackBtn.getStyleClass().add("btn-success");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        statusLabel.setVisible(false);

        submitFeedbackBtn.setOnAction(e -> {
            String decision = "Accepted";
            if (modifyRadio.isSelected()) decision = "Modified";
            if (rejectRadio.isSelected()) decision = "Rejected";

            String comments = commentsArea.getText().trim();
            if (comments.isEmpty()) {
                comments = decision + " based on review of biomarkers and clinical presentation.";
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String now = LocalDateTime.now().format(dtf);

            DoctorFeedback feedback = new DoctorFeedback(
                    savedPredictionId,
                    currentUser.getUserId(),
                    currentUser.getFullName(),
                    decision,
                    comments,
                    now
            );

            int fbId = feedbackDAO.create(feedback);
            if (fbId > 0) {
                statusLabel.setText("✔ Decision saved to database (Feedback #" + fbId + ")");
                statusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                submitFeedbackBtn.setDisable(true);
            } else {
                statusLabel.setText("Failed to save feedback to database.");
                statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
            statusLabel.setVisible(true);
        });

        actionRow.getChildren().addAll(submitFeedbackBtn, statusLabel);
        card.getChildren().addAll(header, sub, radioBox, notesBox, actionRow);
        return card;
    }
}
