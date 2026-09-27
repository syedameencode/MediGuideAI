package com.mediguide.ui;

import com.mediguide.dao.DoctorFeedbackDAO;
import com.mediguide.dao.MedicalRecordDAO;
import com.mediguide.dao.PatientDAO;
import com.mediguide.dao.PredictionDAO;
import com.mediguide.models.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class PatientHistoryView extends ScrollPane {

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final PredictionDAO predictionDAO = new PredictionDAO();
    private final DoctorFeedbackDAO feedbackDAO = new DoctorFeedbackDAO();

    private ComboBox<Patient> patientSelector;
    private VBox patientCard;
    private TableView<MedicalRecord> recordsTable;
    private TableView<Prediction> predictionsTable;
    private VBox feedbackSection;

    private Patient initialPatient;

    public PatientHistoryView(Patient initialPatient) {
        this.initialPatient = initialPatient;
        initUI();
    }

    private void initUI() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");

        VBox contentBox = new VBox(24);
        contentBox.getStyleClass().add("content-area");
        contentBox.setMaxWidth(1100);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Patient Clinical History & Longitudinal Audit Trail");
        title.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Review past vital measurements, AI diagnostic determinations, and doctor feedback");
        subtitle.getStyleClass().add("page-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Patient Selector Row
        HBox selectorRow = new HBox(16);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        selectorRow.getStyleClass().add("card");
        selectorRow.setPadding(new Insets(14, 20, 14, 20));

        Label selLabel = new Label("Select Patient:");
        selLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

        patientSelector = new ComboBox<>();
        patientSelector.setPrefWidth(360);
        List<Patient> allPatients = patientDAO.findAll();
        patientSelector.getItems().addAll(allPatients);

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

        selectorRow.getChildren().addAll(selLabel, patientSelector);

        // Patient Info Profile Card
        patientCard = new VBox(8);
        patientCard.getStyleClass().add("card");

        // Section 1: Past Medical Vitals Table
        VBox vitalsSection = new VBox(10);
        Label vitalsHeader = new Label("Recorded Clinical Vitals & Lab Biomarkers");
        vitalsHeader.getStyleClass().add("card-header");
        recordsTable = createRecordsTable();
        recordsTable.setPrefHeight(180);
        vitalsSection.getChildren().addAll(vitalsHeader, recordsTable);

        // Section 2: AI Diagnostic Predictions Table
        VBox predictionsSection = new VBox(10);
        Label predHeader = new Label("Intelligent Decision Engine Evaluations & Risk Assessments");
        predHeader.getStyleClass().add("card-header");
        predictionsTable = createPredictionsTable();
        predictionsTable.setPrefHeight(180);
        predictionsSection.getChildren().addAll(predHeader, predictionsTable);

        // Section 3: Doctor Feedback Log
        feedbackSection = new VBox(10);
        Label fbHeader = new Label("Clinician Feedback & Decision Log");
        fbHeader.getStyleClass().add("card-header");
        feedbackSection.getChildren().add(fbHeader);

        contentBox.getChildren().addAll(
                headerBox, selectorRow, patientCard,
                vitalsSection, predictionsSection, feedbackSection
        );

        setContent(contentBox);

        if (patientSelector.getValue() != null) {
            updateHistory(patientSelector.getValue());
        }
    }

    private void updateHistory(Patient patient) {
        if (patient == null) return;

        // Update Patient Profile Card
        patientCard.getChildren().clear();
        Label cardTitle = new Label("Patient Profile: " + patient.getName() + " (#" + patient.getPatientId() + ")");
        cardTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        HBox metaRow = new HBox(24);
        metaRow.getChildren().addAll(
                new Label("Age: " + patient.getAge() + " yrs"),
                new Label("Gender: " + patient.getGender()),
                new Label("Phone: " + patient.getPhone()),
                new Label("Email: " + patient.getEmail()),
                new Label("Registered: " + patient.getCreatedDate())
        );
        metaRow.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        Label notesLabel = new Label("Medical Notes: " + (patient.getMedicalHistoryNotes().isEmpty() ? "None recorded" : patient.getMedicalHistoryNotes()));
        notesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");

        patientCard.getChildren().addAll(cardTitle, metaRow, notesLabel);

        // Update Vitals Table
        List<MedicalRecord> records = medicalRecordDAO.findByPatientId(patient.getPatientId());
        recordsTable.setItems(FXCollections.observableArrayList(records));

        // Update Predictions Table
        List<Prediction> predictions = predictionDAO.findByPatientId(patient.getPatientId());
        predictionsTable.setItems(FXCollections.observableArrayList(predictions));

        // Update Doctor Feedback Section
        feedbackSection.getChildren().removeIf(node -> node instanceof HBox || node instanceof Label && !((Label) node).getText().contains("Clinician Feedback"));

        if (predictions.isEmpty()) {
            Label noneLbl = new Label("No assessments or clinician feedbacks recorded yet for this patient.");
            noneLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            feedbackSection.getChildren().add(noneLbl);
        } else {
            boolean hasFeedback = false;
            for (Prediction p : predictions) {
                DoctorFeedback fb = feedbackDAO.findByPredictionId(p.getPredictionId());
                if (fb != null) {
                    hasFeedback = true;
                    HBox fbCard = new HBox(16);
                    fbCard.setStyle("-fx-background-color: #ffffff; -fx-padding: 14 18; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1;");
                    fbCard.setAlignment(Pos.CENTER_LEFT);

                    Label decBadge = new Label(fb.getDoctorDecision().toUpperCase());
                    String styleClass = "badge-low";
                    if ("Modified".equalsIgnoreCase(fb.getDoctorDecision())) styleClass = "badge-medium";
                    if ("Rejected".equalsIgnoreCase(fb.getDoctorDecision())) styleClass = "badge-high";
                    decBadge.getStyleClass().add(styleClass);

                    VBox textCol = new VBox(3);
                    Label docTitle = new Label("Clinician: " + fb.getDoctorName() + " | Date: " + fb.getFeedbackDate());
                    docTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1e293b;");

                    Label comments = new Label("\"" + fb.getComments() + "\"");
                    comments.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
                    comments.setWrapText(true);

                    textCol.getChildren().addAll(docTitle, comments);
                    fbCard.getChildren().addAll(decBadge, textCol);
                    feedbackSection.getChildren().add(fbCard);
                }
            }
            if (!hasFeedback) {
                Label pendingLbl = new Label("No clinician overrides submitted yet for existing predictions.");
                pendingLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
                feedbackSection.getChildren().add(pendingLbl);
            }
        }
    }

    private TableView<MedicalRecord> createRecordsTable() {
        TableView<MedicalRecord> table = new TableView<>();

        TableColumn<MedicalRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRecordedDate()));
        dateCol.setPrefWidth(120);

        TableColumn<MedicalRecord, String> bpCol = new TableColumn<>("Blood Pressure");
        bpCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBloodPressureString()));
        bpCol.setPrefWidth(120);

        TableColumn<MedicalRecord, String> gluCol = new TableColumn<>("Glucose (mg/dL)");
        gluCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getGlucose())));
        gluCol.setPrefWidth(110);

        TableColumn<MedicalRecord, String> cholCol = new TableColumn<>("Cholesterol");
        cholCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCholesterol())));
        cholCol.setPrefWidth(100);

        TableColumn<MedicalRecord, String> creatCol = new TableColumn<>("Creatinine");
        creatCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCreatinine())));
        creatCol.setPrefWidth(90);

        TableColumn<MedicalRecord, String> bmiCol = new TableColumn<>("BMI");
        bmiCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f", d.getValue().getBmi())));
        bmiCol.setPrefWidth(80);

        TableColumn<MedicalRecord, String> sympCol = new TableColumn<>("Symptoms");
        sympCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSymptoms()));
        sympCol.setPrefWidth(220);

        table.getColumns().addAll(dateCol, bpCol, gluCol, cholCol, creatCol, bmiCol, sympCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private TableView<Prediction> createPredictionsTable() {
        TableView<Prediction> table = new TableView<>();

        TableColumn<Prediction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPredictionDate()));
        dateCol.setPrefWidth(120);

        TableColumn<Prediction, String> condCol = new TableColumn<>("Condition Evaluated");
        condCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getConditionName()));
        condCol.setPrefWidth(170);

        TableColumn<Prediction, String> riskCol = new TableColumn<>("Risk Classification");
        riskCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRiskLevel()));
        riskCol.setPrefWidth(130);
        riskCol.setCellFactory(column -> new TableCell<>() {
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

        TableColumn<Prediction, String> confCol = new TableColumn<>("Confidence");
        confCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f%%", d.getValue().getConfidence())));
        confCol.setPrefWidth(100);

        TableColumn<Prediction, String> factorsCol = new TableColumn<>("Key Explainable Factors");
        factorsCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getContributingFactors()));
        factorsCol.setPrefWidth(350);

        table.getColumns().addAll(dateCol, condCol, riskCol, confCol, factorsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }
}
