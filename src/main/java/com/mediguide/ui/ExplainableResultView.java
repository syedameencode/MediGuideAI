package com.mediguide.ui;

import com.mediguide.dao.DoctorFeedbackDAO;
import com.mediguide.dao.PredictionDAO;
import com.mediguide.models.DoctorFeedback;
import com.mediguide.models.Patient;
import com.mediguide.models.Prediction;
import com.mediguide.models.RiskAssessmentResult;
import com.mediguide.models.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExplainableResultView extends ScrollPane {

    private final Patient patient;
    private final RiskAssessmentResult result;
    private final User currentUser;
    private final Runnable onBackAction;

    private final PredictionDAO predictionDAO = new PredictionDAO();
    private final DoctorFeedbackDAO feedbackDAO = new DoctorFeedbackDAO();
    private int savedPredictionId = -1;

    public ExplainableResultView(Patient patient, RiskAssessmentResult result, User currentUser, Runnable onBackAction) {
        this.patient = patient;
        this.result = result;
        this.currentUser = currentUser;
        this.onBackAction = onBackAction;

        savePredictionToDatabase();
        initUI();
    }

    private void savePredictionToDatabase() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String now = LocalDateTime.now().format(dtf);

        String factorsCombined = String.join("; ", result.getContributingFactors());
        String alternativesCombined = String.join("; ", result.getAlternativePossibilities());

        Prediction p = new Prediction(
                patient.getPatientId(),
                result.getPrimaryCondition(),
                result.getOverallRiskLevel(),
                result.getConfidenceScore(),
                factorsCombined,
                alternativesCombined,
                now
        );

        savedPredictionId = predictionDAO.create(p);
    }

    private void initUI() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");

        VBox contentBox = new VBox(24);
        contentBox.getStyleClass().add("content-area");
        contentBox.setMaxWidth(1100);

        // Top Navigation Bar
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Clinical Entry");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> {
            if (onBackAction != null) onBackAction.run();
        });

        VBox titleBox = new VBox(2);
        Label title = new Label("Intelligent Decision Support & Explainable AI Card");
        title.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Multi-Condition Evaluation & Transparent Reasoning for Patient: " + patient.getName() + " (#" + patient.getPatientId() + ")");
        subtitle.getStyleClass().add("page-header-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        topBar.getChildren().addAll(backBtn, titleBox);

        // Disclaimer Banner
        HBox disclaimerBanner = new HBox(12);
        disclaimerBanner.getStyleClass().add("disclaimer-banner");
        disclaimerBanner.setAlignment(Pos.CENTER_LEFT);
        Label warnIcon = new Label("⚠");
        warnIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #d97706;");
        Label disclaimerText = new Label(result.getDisclaimer());
        disclaimerText.getStyleClass().add("disclaimer-text");
        disclaimerText.setWrapText(true);
        disclaimerBanner.getChildren().addAll(warnIcon, disclaimerText);

        // 1. Primary Diagnosis & Confidence Hero Card
        VBox heroCard = createPrimaryDiagnosisCard();

        // 2. Multi-Condition Analysis Row (Review 1 Unique Feature: Page 13 in PDF)
        VBox multiConditionSection = createMultiConditionSection();

        // 3. Explainable AI Factor Checklist ("Why did the system reach this conclusion?")
        VBox xaiCard = createExplainabilityCard();

        // 4. Clinical Recommendations & Differential Hypotheses
        HBox dualClinicalBox = new HBox(20);
        VBox altCard = createAlternativesCard();
        VBox recCard = createRecommendationsCard();
        HBox.setHgrow(altCard, Priority.ALWAYS);
        HBox.setHgrow(recCard, Priority.ALWAYS);
        dualClinicalBox.getChildren().addAll(altCard, recCard);

        // 5. Human-in-the-Loop Doctor Feedback Simulation (Page 13 in PDF)
        VBox feedbackCard = createDoctorFeedbackCard();

        contentBox.getChildren().addAll(
                topBar, disclaimerBanner, heroCard,
                multiConditionSection, xaiCard, dualClinicalBox,
                feedbackCard
        );

        setContent(contentBox);
    }

    private VBox createPrimaryDiagnosisCard() {
        VBox card = new VBox(16);
        String level = result.getOverallRiskLevel().toLowerCase();
        card.getStyleClass().addAll("card", "decision-card-" + level);

        HBox headerRow = new HBox(20);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox condCol = new VBox(4);
        Label flag = new Label("PRIMARY CLINICAL RISK CLASSIFICATION");
        flag.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        Label primaryLabel = new Label(result.getPrimaryCondition());
        primaryLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label patientSub = new Label("Patient: " + patient.getName() + " | Age: " + patient.getAge() + " | Gender: " + patient.getGender());
        patientSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        condCol.getChildren().addAll(flag, primaryLabel, patientSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Risk Level Badge
        Label riskBadge = new Label(result.getOverallRiskLevel().toUpperCase() + " RISK");
        riskBadge.getStyleClass().add("badge-" + level);
        riskBadge.setStyle("-fx-font-size: 15px; -fx-padding: 8 18;");

        // Confidence Indicator
        VBox confBox = new VBox(2);
        confBox.setAlignment(Pos.CENTER_RIGHT);
        Label confLabel = new Label(String.format("%.1f%%", result.getConfidenceScore()));
        confLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0284c7;");
        Label confSub = new Label("System Confidence");
        confSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        confBox.getChildren().addAll(confLabel, confSub);

        headerRow.getChildren().addAll(condCol, spacer, riskBadge, confBox);
        card.getChildren().add(headerRow);
        return card;
    }

    private VBox createMultiConditionSection() {
        VBox section = new VBox(12);

        Label secTitle = new Label("Multi-Condition Cross-Analysis");
        secTitle.getStyleClass().add("card-header");

        HBox cardsRow = new HBox(16);
        for (RiskAssessmentResult.ConditionEvaluation eval : result.getConditionEvaluations()) {
            VBox conditionCard = new VBox(10);
            conditionCard.getStyleClass().add("card");
            conditionCard.setPrefWidth(330);
            HBox.setHgrow(conditionCard, Priority.ALWAYS);

            HBox rowTop = new HBox();
            rowTop.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(eval.getConditionName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label badge = new Label(eval.getRiskLevel().toUpperCase());
            badge.getStyleClass().add("badge-" + eval.getRiskLevel().toLowerCase());

            rowTop.getChildren().addAll(name, sp, badge);

            // Score Progress Bar
            ProgressBar pBar = new ProgressBar(eval.getScore() / 100.0);
            pBar.setMaxWidth(Double.MAX_VALUE);
            if ("High".equalsIgnoreCase(eval.getRiskLevel())) {
                pBar.setStyle("-fx-accent: #ef4444;");
            } else if ("Medium".equalsIgnoreCase(eval.getRiskLevel())) {
                pBar.setStyle("-fx-accent: #f59e0b;");
            } else {
                pBar.setStyle("-fx-accent: #10b981;");
            }

            Label scoreText = new Label("Risk Score: " + eval.getScore() + " / 100");
            scoreText.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

            Label summary = new Label(eval.getSummary());
            summary.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
            summary.setWrapText(true);

            conditionCard.getChildren().addAll(rowTop, pBar, scoreText, summary);
            cardsRow.getChildren().add(conditionCard);
        }

        section.getChildren().addAll(secTitle, cardsRow);
        return section;
    }

    private VBox createExplainabilityCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("Explainable Decision Card — Contributing Biomarkers & Clinical Rules");
        header.getStyleClass().add("card-header");

        Label whyPrompt = new Label("Why did the system reach this conclusion?");
        whyPrompt.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0284c7;");

        VBox factorList = new VBox(10);
        for (String factor : result.getContributingFactors()) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #f8fafc; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-radius: 6; -fx-border-width: 1;");

            Label check = new Label("✔");
            check.setStyle("-fx-text-fill: #0284c7; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label factorText = new Label(factor);
            factorText.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");
            factorText.setWrapText(true);

            row.getChildren().addAll(check, factorText);
            factorList.getChildren().add(row);
        }

        card.getChildren().addAll(header, whyPrompt, factorList);
        return card;
    }

    private VBox createAlternativesCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label header = new Label("Alternative Possibilities Considered");
        header.getStyleClass().add("card-header");

        VBox list = new VBox(8);
        for (String alt : result.getAlternativePossibilities()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Circle bullet = new Circle(3, Color.web("#64748b"));
            Label altText = new Label(alt);
            altText.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
            altText.setWrapText(true);
            row.getChildren().addAll(bullet, altText);
            list.getChildren().add(row);
        }

        card.getChildren().addAll(header, list);
        return card;
    }

    private VBox createRecommendationsCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label header = new Label("Clinical Next Steps & Guidance");
        header.getStyleClass().add("card-header");

        VBox list = new VBox(8);
        for (String rec : result.getRecommendations()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Circle bullet = new Circle(3, Color.web("#0284c7"));
            Label recText = new Label(rec);
            recText.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
            recText.setWrapText(true);
            row.getChildren().addAll(bullet, recText);
            list.getChildren().add(row);
        }

        card.getChildren().addAll(header, list);
        return card;
    }

    private VBox createDoctorFeedbackCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("Doctor Feedback & Clinical Override Simulation (Human-in-the-Loop AI)");
        header.getStyleClass().add("card-header");

        Label sub = new Label("As an authorized clinician (" + currentUser.getFullName() + "), review the automated decision support above and record your decision.");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Radio Decision Group
        HBox radioBox = new HBox(24);
        radioBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup decisionGroup = new ToggleGroup();

        RadioButton acceptRadio = new RadioButton("Accept AI Recommendation");
        acceptRadio.setToggleGroup(decisionGroup);
        acceptRadio.setStyle("-fx-font-weight: 600; -fx-text-fill: #059669;");
        acceptRadio.setSelected(true);

        RadioButton modifyRadio = new RadioButton("Modify Risk Tier / Diagnosis");
        modifyRadio.setToggleGroup(decisionGroup);
        modifyRadio.setStyle("-fx-font-weight: 600; -fx-text-fill: #d97706;");

        RadioButton rejectRadio = new RadioButton("Reject Recommendation");
        rejectRadio.setToggleGroup(decisionGroup);
        rejectRadio.setStyle("-fx-font-weight: 600; -fx-text-fill: #dc2626;");

        radioBox.getChildren().addAll(acceptRadio, modifyRadio, rejectRadio);

        // Notes Text Area
        VBox notesBox = new VBox(6);
        Label notesLabel = new Label("Physician Clinical Notes / Override Rationale:");
        notesLabel.getStyleClass().add("form-label");
        TextArea commentsArea = new TextArea();
        commentsArea.setPromptText("Enter clinical observations, diagnostic rationale, or therapeutic prescription plan...");
        commentsArea.setPrefRowCount(3);
        notesBox.getChildren().addAll(notesLabel, commentsArea);

        // Submit Button & Confirmation Status
        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button submitFeedbackBtn = new Button("Record Doctor Decision to SQL Database");
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
