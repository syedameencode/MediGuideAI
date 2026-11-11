package com.mediguide.ui;

import com.mediguide.dao.DoctorFeedbackDAO;
import com.mediguide.dao.MedicalRecordDAO;
import com.mediguide.dao.PatientDAO;
import com.mediguide.dao.PredictionDAO;
import com.mediguide.models.*;
import com.mediguide.util.PdfReportGenerator;
import com.mediguide.util.UIAnimationHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * PatientHistoryView — Comprehensive Patient Screen & Health Intelligence Profile.
 * Displays:
 * 1. Patient information (demographics, contact, clinical notes)
 * 2. Medical information & latest Biomarkers (BP, Glucose, Cholesterol, Creatinine, BMI)
 * 3. Diagnostic Risk Profile
 * 4. Longitudinal Health Trends
 * 5. Assessment History
 * 6. Doctor Feedback Log
 */
public class PatientHistoryView extends ScrollPane {

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final PredictionDAO predictionDAO = new PredictionDAO();
    private final DoctorFeedbackDAO feedbackDAO = new DoctorFeedbackDAO();

    private ComboBox<Patient> patientSelector;
    private VBox patientCard;
    private VBox biomarkersCard;
    private VBox riskProfileCard;
    private VBox trendChartSection;
    private TableView<MedicalRecord> recordsTable;
    private TableView<Prediction> predictionsTable;
    private VBox feedbackSection;
    private Label pdfStatusLabel;

    private Patient initialPatient;
    private Consumer<Patient> onDiagnosePatient;

    public PatientHistoryView(Patient initialPatient) {
        this.initialPatient = initialPatient;
        initUI();
    }

    public void setOnDiagnosePatient(Consumer<Patient> onDiagnosePatient) {
        this.onDiagnosePatient = onDiagnosePatient;
    }

    private void initUI() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");

        VBox contentBox = new VBox(24);
        contentBox.getStyleClass().add("content-area");
        contentBox.setMaxWidth(1140);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Patient Health Intelligence Profile & Longitudinal History");
        title.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Comprehensive longitudinal clinical records, physiological biomarkers, risk trends, and doctor feedback");
        subtitle.getStyleClass().add("page-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        pdfStatusLabel = new Label();
        pdfStatusLabel.setVisible(false);
        pdfStatusLabel.setManaged(false);

        // Patient Selector Row
        HBox selectorRow = createPatientSelectorRow();

        // 1. Patient Demographics Profile Card
        patientCard = new VBox(14);
        patientCard.getStyleClass().add("card");

        // 2. Risk Profile & Multi-Condition Status Card
        riskProfileCard = new VBox(14);
        riskProfileCard.getStyleClass().add("card");

        // 3. Latest Biomarkers Card
        biomarkersCard = new VBox(14);
        biomarkersCard.getStyleClass().add("card");

        // 4. Longitudinal Risk Trend Line Chart
        trendChartSection = new VBox(12);
        trendChartSection.getStyleClass().add("card");

        // 5. Recorded Clinical Vitals Table Card
        VBox vitalsSection = new VBox(12);
        vitalsSection.getStyleClass().add("card");
        Label vitalsHeader = new Label("Recorded Clinical Vitals & Lab Biomarkers History");
        vitalsHeader.getStyleClass().add("card-header");
        recordsTable = createRecordsTable();
        recordsTable.setPrefHeight(190);
        vitalsSection.getChildren().addAll(vitalsHeader, recordsTable);

        // 6. AI Diagnostic Assessments Table Card
        VBox predictionsSection = new VBox(12);
        predictionsSection.getStyleClass().add("card");
        Label predHeader = new Label("Assessment History (Hybrid Decision Engine Log)");
        predHeader.getStyleClass().add("card-header");
        predictionsTable = createPredictionsTable();
        predictionsTable.setPrefHeight(190);
        predictionsSection.getChildren().addAll(predHeader, predictionsTable);

        // 7. Doctor Feedback Log Card
        feedbackSection = new VBox(12);
        feedbackSection.getStyleClass().add("card");
        Label fbHeader = new Label("Doctor Feedback & Clinical Override Log (Human-in-the-Loop)");
        fbHeader.getStyleClass().add("card-header");
        feedbackSection.getChildren().add(fbHeader);

        contentBox.getChildren().addAll(
                headerBox,
                pdfStatusLabel,
                selectorRow,
                patientCard,
                riskProfileCard,
                biomarkersCard,
                trendChartSection,
                vitalsSection,
                predictionsSection,
                feedbackSection
        );

        setContent(contentBox);

        if (patientSelector.getValue() != null) {
            updateHistory(patientSelector.getValue());
        }

        UIAnimationHelper.slideInUp(contentBox);
    }

    private HBox createPatientSelectorRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        row.setPadding(new Insets(14, 20, 14, 20));

        Label selLabel = new Label("Select Patient:");
        selLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

        patientSelector = new ComboBox<>();
        patientSelector.setPrefWidth(380);
        List<Patient> allPatients = patientDAO.findAll();
        patientSelector.getItems().addAll(allPatients);

        patientSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    setText(p.getName() + " (#" + p.getPatientId() + ") — Age: " + p.getAge() + ", " + p.getGender());
                }
            }
        });
        patientSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText("Select patient...");
                } else {
                    setText(p.getName() + " (#" + p.getPatientId() + ") — Age: " + p.getAge() + ", " + p.getGender());
                }
            }
        });

        if (initialPatient != null) {
            for (Patient p : allPatients) {
                if (p.getPatientId() == initialPatient.getPatientId()) {
                    patientSelector.setValue(p);
                    break;
                }
            }
        } else if (!allPatients.isEmpty()) {
            patientSelector.setValue(allPatients.get(0));
        }

        patientSelector.setOnAction(e -> updateHistory(patientSelector.getValue()));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button diagnoseBtn = new Button("⚡ Run New AI Analysis for this Patient");
        diagnoseBtn.getStyleClass().add("btn-analyze");
        diagnoseBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8 18;");
        diagnoseBtn.setOnAction(e -> {
            Patient p = patientSelector.getValue();
            if (p != null && onDiagnosePatient != null) {
                onDiagnosePatient.accept(p);
            }
        });

        row.getChildren().addAll(selLabel, patientSelector, sp, diagnoseBtn);
        return row;
    }

    private void updateHistory(Patient patient) {
        if (patient == null) return;

        // 1. Update Patient Information Card
        updatePatientInfoCard(patient);

        // 2. Update Latest Biomarkers Card
        MedicalRecord latestRecord = medicalRecordDAO.findLatestByPatientId(patient.getPatientId());
        updateBiomarkersCard(latestRecord);

        // 3. Update Risk Profile Card
        Prediction latestPrediction = predictionDAO.findLatestByPatientId(patient.getPatientId());
        updateRiskProfileCard(latestPrediction);

        // 4. Update Tables & Trend Chart
        List<MedicalRecord> records = medicalRecordDAO.findByPatientId(patient.getPatientId());
        recordsTable.setItems(FXCollections.observableArrayList(records));

        List<Prediction> predictions = predictionDAO.findByPatientId(patient.getPatientId());
        predictionsTable.setItems(FXCollections.observableArrayList(predictions));

        updateTrendChart(patient, predictions);

        // 5. Update Doctor Feedback Section
        updateDoctorFeedbackSection(predictions);
    }

    private void updatePatientInfoCard(Patient patient) {
        patientCard.getChildren().clear();

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Initials avatar
        String initials = "";
        String[] parts = patient.getName().split(" ");
        for (String pt : parts) {
            if (!pt.isEmpty()) initials += pt.charAt(0);
            if (initials.length() >= 2) break;
        }
        Label avatar = new Label(initials.toUpperCase());
        avatar.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 16px; -fx-min-width: 48px; -fx-min-height: 48px; -fx-alignment: center; -fx-background-radius: 24;");

        VBox nameCol = new VBox(2);
        Label cardTitle = new Label(patient.getName() + " (#" + patient.getPatientId() + ")");
        cardTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        Label regLabel = new Label("Registered: " + (patient.getCreatedDate() != null ? patient.getCreatedDate() : "Not Available"));
        regLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        nameCol.getChildren().addAll(cardTitle, regLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button exportPdfBtn = new Button("📄 Export PDF Report");
        exportPdfBtn.getStyleClass().add("btn-secondary");
        exportPdfBtn.setOnAction(e -> exportPatientPdfReport(patient));

        topRow.getChildren().addAll(avatar, nameCol, spacer, exportPdfBtn);

        // Demographics Meta Grid
        HBox metaRow = new HBox(32);
        metaRow.setStyle("-fx-padding: 8 0;");
        metaRow.getChildren().addAll(
                createMetaItem("AGE", patient.getAge() + " years"),
                createMetaItem("GENDER", patient.getGender()),
                createMetaItem("PHONE", patient.getPhone() != null && !patient.getPhone().isEmpty() ? patient.getPhone() : "Not Available"),
                createMetaItem("EMAIL", patient.getEmail() != null && !patient.getEmail().isEmpty() ? patient.getEmail() : "Not Available")
        );

        Label notesHeader = new Label("Documented Medical History & Background Notes:");
        notesHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748b;");
        String notesStr = (patient.getMedicalHistoryNotes() != null && !patient.getMedicalHistoryNotes().trim().isEmpty())
                ? patient.getMedicalHistoryNotes()
                : "Not Available";
        Label notesLabel = new Label(notesStr);
        notesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-background-color: #f8fafc; -fx-padding: 10 14; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1;");
        notesLabel.setWrapText(true);

        patientCard.getChildren().addAll(topRow, metaRow, notesHeader, notesLabel);
    }

    private VBox createMetaItem(String label, String value) {
        VBox box = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #94a3b8; -fx-letter-spacing: 0.5px;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
        box.getChildren().addAll(l, v);
        return box;
    }

    private void updateBiomarkersCard(MedicalRecord record) {
        biomarkersCard.getChildren().clear();

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label header = new Label("Latest Physiological Biomarkers & Vitals");
        header.getStyleClass().add("card-header");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label dateTag = new Label(record != null ? "Recorded: " + record.getRecordedDate() : "No Records Available");
        dateTag.getStyleClass().add("badge-neutral");
        top.getChildren().addAll(header, sp, dateTag);

        biomarkersCard.getChildren().add(top);

        if (record == null) {
            Label noBio = new Label("Not Available — No physiological vitals recorded for this patient yet.");
            noBio.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            biomarkersCard.getChildren().add(noBio);
            return;
        }

        GridPane bioGrid = new GridPane();
        bioGrid.setHgap(14);
        bioGrid.setVgap(14);

        int sys = record.getSystolicBp();
        int dia = record.getDiastolicBp();
        double glu = record.getGlucose();
        double chol = record.getCholesterol();
        double creat = record.getCreatinine();
        double bmi = record.getBmi();

        VBox b1 = createBiomarkerWidget("BLOOD PRESSURE", sys + " / " + dia, "mmHg", "Normal: <120 / <80", sys >= 140 ? "badge-high" : (sys >= 130 ? "badge-medium" : "badge-low"));
        VBox b2 = createBiomarkerWidget("FASTING GLUCOSE", String.format("%.1f", glu), "mg/dL", "Normal: 70–99", glu >= 126 ? "badge-high" : (glu >= 100 ? "badge-medium" : "badge-low"));
        VBox b3 = createBiomarkerWidget("TOTAL CHOLESTEROL", String.format("%.0f", chol), "mg/dL", "Normal: <200", chol >= 240 ? "badge-high" : (chol >= 200 ? "badge-medium" : "badge-low"));
        VBox b4 = createBiomarkerWidget("SERUM CREATININE", String.format("%.2f", creat), "mg/dL", "Normal: 0.7–1.3", creat >= 1.5 ? "badge-high" : "badge-low");
        VBox b5 = createBiomarkerWidget("BODY MASS INDEX", String.format("%.1f", bmi), "kg/m²", "Normal: 18.5–24.9", bmi >= 30 ? "badge-high" : (bmi >= 25 ? "badge-medium" : "badge-low"));

        bioGrid.add(b1, 0, 0);
        bioGrid.add(b2, 1, 0);
        bioGrid.add(b3, 2, 0);
        bioGrid.add(b4, 3, 0);
        bioGrid.add(b5, 4, 0);

        HBox sympRow = new HBox(8);
        sympRow.setAlignment(Pos.CENTER_LEFT);
        Label sLbl = new Label("Reported Symptoms: ");
        sLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #475569;");
        String sympStr = record.getSymptoms() != null && !record.getSymptoms().isEmpty() ? record.getSymptoms() : "None reported";
        Label sVal = new Label(sympStr);
        sVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f172a;");
        sympRow.getChildren().addAll(sLbl, sVal);

        HBox famRow = new HBox(8);
        famRow.setAlignment(Pos.CENTER_LEFT);
        Label fLbl = new Label("Family History: ");
        fLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #475569;");
        String famStr = record.getFamilyHistory() != null && !record.getFamilyHistory().isEmpty() ? record.getFamilyHistory() : "None";
        Label fVal = new Label(famStr);
        fVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f172a;");
        famRow.getChildren().addAll(fLbl, fVal);

        biomarkersCard.getChildren().addAll(bioGrid, sympRow, famRow);
    }

    private VBox createBiomarkerWidget(String label, String value, String unit, String range, String badgeClass) {
        VBox card = new VBox(4);
        card.getStyleClass().add("biomarker-card");
        card.setPrefWidth(190);

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.getStyleClass().add("biomarker-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label chip = new Label(badgeClass.contains("high") ? "High" : (badgeClass.contains("medium") ? "Elevated" : "Optimal"));
        chip.getStyleClass().add(badgeClass);
        top.getChildren().addAll(l, sp, chip);

        HBox vRow = new HBox(4);
        vRow.setAlignment(Pos.BASELINE_LEFT);
        Label v = new Label(value);
        v.getStyleClass().add("biomarker-val");
        Label u = new Label(unit);
        u.getStyleClass().add("biomarker-unit");
        vRow.getChildren().addAll(v, u);

        Label r = new Label(range);
        r.getStyleClass().add("biomarker-range");

        card.getChildren().addAll(top, vRow, r);
        return card;
    }

    private void updateRiskProfileCard(Prediction pred) {
        riskProfileCard.getChildren().clear();

        Label header = new Label("Diagnostic Risk Profile");
        header.getStyleClass().add("card-header");

        if (pred == null) {
            Label noPred = new Label("Not Available — No diagnostic AI assessments recorded for this patient yet.");
            noPred.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            riskProfileCard.getChildren().addAll(header, noPred);
            return;
        }

        HBox row = new HBox(24);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox condCol = new VBox(4);
        Label cLabel = new Label("DIAGNOSED PRIMARY CONDITION");
        cLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
        Label cVal = new Label(pred.getConditionName());
        cVal.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        condCol.getChildren().addAll(cLabel, cVal);

        VBox riskCol = new VBox(4);
        Label rLabel = new Label("RISK STRATIFICATION");
        rLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
        Label badge = new Label(pred.getRiskLevel().toUpperCase() + " RISK");
        badge.getStyleClass().add("badge-" + pred.getRiskLevel().toLowerCase());
        riskCol.getChildren().addAll(rLabel, badge);

        VBox confCol = new VBox(4);
        Label cfLabel = new Label("CONFIDENCE");
        cfLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
        Label cfVal = new Label(pred.getConfidence() > 0 ? String.format("%.1f%%", pred.getConfidence()) : "Not Available");
        cfVal.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0284c7;");
        confCol.getChildren().addAll(cfLabel, cfVal);

        VBox dateCol = new VBox(4);
        Label dLabel = new Label("ASSESSMENT DATE");
        dLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b; -fx-letter-spacing: 0.5px;");
        Label dVal = new Label(pred.getPredictionDate() != null ? pred.getPredictionDate() : "Not Available");
        dVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        dateCol.getChildren().addAll(dLabel, dVal);

        row.getChildren().addAll(condCol, riskCol, confCol, dateCol);
        riskProfileCard.getChildren().addAll(header, row);
    }

    private void updateTrendChart(Patient patient, List<Prediction> predictions) {
        trendChartSection.getChildren().clear();

        Label header = new Label("Longitudinal Health & Risk Trends");
        header.getStyleClass().add("card-header");

        if (predictions == null || predictions.isEmpty()) {
            Label noTrend = new Label("Not Available — Trend chart requires at least one recorded assessment.");
            noTrend.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            trendChartSection.getChildren().addAll(header, noTrend);
            return;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Assessment Timeline");
        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel("AI Confidence Score (%)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setPrefHeight(200);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Confidence %");

        // Display in chronological order
        for (int i = predictions.size() - 1; i >= 0; i--) {
            Prediction p = predictions.get(i);
            String label = p.getPredictionDate() != null ? p.getPredictionDate().split(" ")[0] : ("#" + (predictions.size() - i));
            series.getData().add(new XYChart.Data<>(label, p.getConfidence()));
        }

        lineChart.getData().add(series);
        trendChartSection.getChildren().addAll(header, lineChart);
    }

    private void updateDoctorFeedbackSection(List<Prediction> predictions) {
        feedbackSection.getChildren().removeIf(node -> node instanceof HBox || (node instanceof Label && !((Label) node).getText().contains("Doctor Feedback")));

        if (predictions == null || predictions.isEmpty()) {
            Label noneLbl = new Label("Not Available — No assessments or clinician feedbacks recorded yet for this patient.");
            noneLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            feedbackSection.getChildren().add(noneLbl);
            return;
        }

        boolean hasFeedback = false;
        for (Prediction p : predictions) {
            DoctorFeedback fb = feedbackDAO.findByPredictionId(p.getPredictionId());
            if (fb != null) {
                hasFeedback = true;
                HBox fbCard = new HBox(16);
                fbCard.setStyle("-fx-background-color: #f8fafc; -fx-padding: 14 18; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");
                fbCard.setAlignment(Pos.CENTER_LEFT);

                Label decBadge = new Label(fb.getDoctorDecision().toUpperCase());
                String styleClass = "badge-low";
                if ("Modified".equalsIgnoreCase(fb.getDoctorDecision())) styleClass = "badge-medium";
                if ("Rejected".equalsIgnoreCase(fb.getDoctorDecision())) styleClass = "badge-high";
                decBadge.getStyleClass().add(styleClass);

                VBox textCol = new VBox(3);
                Label docTitle = new Label("Physician: " + fb.getDoctorName() + " | Assessment #" + p.getPredictionId() + " (" + p.getConditionName() + ") | Date: " + fb.getFeedbackDate());
                docTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1e293b;");

                String commentStr = (fb.getComments() != null && !fb.getComments().isEmpty()) ? fb.getComments() : "Not Available";
                Label comments = new Label("\"" + commentStr + "\"");
                comments.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
                comments.setWrapText(true);

                textCol.getChildren().addAll(docTitle, comments);
                fbCard.getChildren().addAll(decBadge, textCol);
                feedbackSection.getChildren().add(fbCard);
            }
        }

        if (!hasFeedback) {
            Label pendingLbl = new Label("No clinician overrides submitted yet for existing predictions.");
            pendingLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            feedbackSection.getChildren().add(pendingLbl);
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<MedicalRecord> createRecordsTable() {
        TableView<MedicalRecord> table = new TableView<>();

        TableColumn<MedicalRecord, String> dateCol = new TableColumn<>("RECORDED DATE");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRecordedDate()));
        dateCol.setPrefWidth(140);

        TableColumn<MedicalRecord, String> bpCol = new TableColumn<>("BLOOD PRESSURE");
        bpCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBloodPressureString()));
        bpCol.setPrefWidth(130);

        TableColumn<MedicalRecord, String> gluCol = new TableColumn<>("GLUCOSE (mg/dL)");
        gluCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f", d.getValue().getGlucose())));
        gluCol.setPrefWidth(130);

        TableColumn<MedicalRecord, String> cholCol = new TableColumn<>("CHOLESTEROL (mg/dL)");
        cholCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.0f", d.getValue().getCholesterol())));
        cholCol.setPrefWidth(140);

        TableColumn<MedicalRecord, String> creatCol = new TableColumn<>("CREATININE (mg/dL)");
        creatCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f", d.getValue().getCreatinine())));
        creatCol.setPrefWidth(140);

        TableColumn<MedicalRecord, String> bmiCol = new TableColumn<>("BMI");
        bmiCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f", d.getValue().getBmi())));
        bmiCol.setPrefWidth(80);

        TableColumn<MedicalRecord, String> sympCol = new TableColumn<>("SYMPTOMS REPORTED");
        sympCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSymptoms()));
        sympCol.setPrefWidth(260);

        table.getColumns().addAll(dateCol, bpCol, gluCol, cholCol, creatCol, bmiCol, sympCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<Prediction> createPredictionsTable() {
        TableView<Prediction> table = new TableView<>();

        TableColumn<Prediction, String> dateCol = new TableColumn<>("DATE");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPredictionDate()));
        dateCol.setPrefWidth(130);

        TableColumn<Prediction, String> condCol = new TableColumn<>("PRIMARY CONDITION");
        condCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getConditionName()));
        condCol.setPrefWidth(170);

        TableColumn<Prediction, String> riskCol = new TableColumn<>("RISK TIER");
        riskCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRiskLevel()));
        riskCol.setPrefWidth(110);
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

        TableColumn<Prediction, String> mlCol = new TableColumn<>("ML PREDICTION");
        mlCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMlPrediction() != null && !d.getValue().getMlPrediction().isEmpty() ? d.getValue().getMlPrediction() : "Not Available"
        ));
        mlCol.setPrefWidth(120);

        TableColumn<Prediction, String> confCol = new TableColumn<>("CONFIDENCE");
        confCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getConfidence() > 0 ? String.format("%.1f%%", d.getValue().getConfidence()) : "Not Available"
        ));
        confCol.setPrefWidth(100);

        TableColumn<Prediction, String> factorsCol = new TableColumn<>("EVIDENCE FACTORS");
        factorsCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getContributingFactors() != null ? d.getValue().getContributingFactors() : "Not Available"
        ));
        factorsCol.setPrefWidth(300);

        table.getColumns().addAll(dateCol, condCol, riskCol, mlCol, confCol, factorsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private void exportPatientPdfReport(Patient patient) {
        try {
            MedicalRecord latestRec = medicalRecordDAO.findLatestByPatientId(patient.getPatientId());
            Prediction latestPred = predictionDAO.findLatestByPatientId(patient.getPatientId());

            RiskAssessmentResult mockAssessment = new RiskAssessmentResult();
            if (latestPred != null) {
                mockAssessment.setPrimaryCondition(latestPred.getConditionName());
                mockAssessment.setOverallRiskLevel(latestPred.getRiskLevel());
                mockAssessment.setConfidenceScore(latestPred.getConfidence());
                if (latestPred.getContributingFactors() != null) {
                    for (String f : latestPred.getContributingFactors().split(";")) {
                        mockAssessment.addContributingFactor(f.trim());
                    }
                }
            } else {
                mockAssessment.setPrimaryCondition("Multi-Condition Baseline");
                mockAssessment.setOverallRiskLevel("Low");
                mockAssessment.setConfidenceScore(90.0);
                mockAssessment.addContributingFactor("Baseline clinical screening.");
            }

            File pdf = PdfReportGenerator.generateReport(patient, latestRec, mockAssessment, "reports");
            pdfStatusLabel.setText("✅ Patient PDF Clinical Report exported successfully: " + pdf.getAbsolutePath());
            pdfStatusLabel.setStyle("-fx-background-color: #f0fdf4; -fx-text-fill: #166534; -fx-border-color: #86efac; -fx-padding: 10 16; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 12px;");
            pdfStatusLabel.setManaged(true);
            pdfStatusLabel.setVisible(true);
        } catch (Exception ex) {
            pdfStatusLabel.setText("❌ Failed to generate patient PDF report: " + ex.getMessage());
            pdfStatusLabel.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #991b1b; -fx-border-color: #fca5a5; -fx-padding: 10 16; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 12px;");
            pdfStatusLabel.setManaged(true);
            pdfStatusLabel.setVisible(true);
        }
    }
}
