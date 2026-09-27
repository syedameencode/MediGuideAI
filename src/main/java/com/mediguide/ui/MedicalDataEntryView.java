package com.mediguide.ui;

import com.mediguide.dao.MedicalRecordDAO;
import com.mediguide.dao.PatientDAO;
import com.mediguide.engine.DecisionEngine;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;
import com.mediguide.models.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class MedicalDataEntryView extends ScrollPane {

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final DecisionEngine decisionEngine = new DecisionEngine();

    private final User currentUser;
    private final BiConsumer<Patient, RiskAssessmentResult> onAssessmentComplete;
    private Patient preSelectedPatient;

    private ComboBox<Patient> patientComboBox;
    private TextField systolicBpField;
    private TextField diastolicBpField;
    private TextField glucoseField;
    private TextField cholesterolField;
    private TextField creatinineField;
    private TextField heightField;
    private TextField weightField;
    private Label bmiDisplayLabel;

    private CheckBox thirstCheck;
    private CheckBox urinationCheck;
    private CheckBox fatigueCheck;
    private CheckBox chestPainCheck;
    private CheckBox breathCheck;
    private CheckBox edemaCheck;
    private CheckBox visionCheck;

    private CheckBox famDiabetesCheck;
    private CheckBox famHeartCheck;
    private CheckBox famKidneyCheck;

    private Label errorLabel;

    public MedicalDataEntryView(User currentUser, Patient preSelectedPatient, BiConsumer<Patient, RiskAssessmentResult> onAssessmentComplete) {
        this.currentUser = currentUser;
        this.preSelectedPatient = preSelectedPatient;
        this.onAssessmentComplete = onAssessmentComplete;
        initUI();
    }

    private void initUI() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: #f8fafc;");

        VBox contentBox = new VBox(24);
        contentBox.getStyleClass().add("content-area");
        contentBox.setMaxWidth(1000);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Clinical Vitals & Medical Data Entry");
        title.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Input patient diagnostic vitals, lab biomarkers, and symptomatology for explainable AI evaluation");
        subtitle.getStyleClass().add("page-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Error Banner
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        // 1. Patient Selection Card
        VBox patientCard = createPatientSelectionCard();

        // 2. Vitals & Laboratory Biomarkers Card
        VBox vitalsCard = createVitalsAndLabsCard();

        // 3. Symptoms & Family History Card
        VBox symptomsCard = createSymptomsAndFamilyCard();

        // Action Row
        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(10, 0, 30, 0));

        Button resetBtn = new Button("Clear Inputs");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> clearInputs());

        Button quickFillHighRisk = new Button("Quick Sample: Diabetic High Risk");
        quickFillHighRisk.getStyleClass().add("btn-secondary");
        quickFillHighRisk.setOnAction(e -> fillDiabeticSample());

        Button quickFillCardio = new Button("Quick Sample: Cardiac Risk");
        quickFillCardio.getStyleClass().add("btn-secondary");
        quickFillCardio.setOnAction(e -> fillCardioSample());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button runDiagnosisBtn = new Button("⚡ Generate Explainable AI Assessment");
        runDiagnosisBtn.getStyleClass().add("btn-primary");
        runDiagnosisBtn.setStyle("-fx-font-size: 14px; -fx-padding: 11 24;");
        runDiagnosisBtn.setOnAction(e -> handleRunDiagnosis());

        actionRow.getChildren().addAll(quickFillHighRisk, quickFillCardio, resetBtn, sp, runDiagnosisBtn);

        contentBox.getChildren().addAll(headerBox, errorLabel, patientCard, vitalsCard, symptomsCard, actionRow);
        setContent(contentBox);
    }

    private VBox createPatientSelectionCard() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label cardHeader = new Label("1. Select Patient");
        cardHeader.getStyleClass().add("card-header");

        patientComboBox = new ComboBox<>();
        patientComboBox.setMaxWidth(Double.MAX_VALUE);
        List<Patient> patients = patientDAO.findAll();
        patientComboBox.getItems().addAll(patients);

        if (preSelectedPatient != null) {
            for (Patient p : patients) {
                if (p.getPatientId() == preSelectedPatient.getPatientId()) {
                    patientComboBox.setValue(p);
                    break;
                }
            }
        } else if (!patients.isEmpty()) {
            patientComboBox.setValue(patients.get(0));
        }

        Label hint = new Label("Choose an existing patient from the database or register a new patient in the Patients tab.");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(cardHeader, patientComboBox, hint);
        return card;
    }

    private VBox createVitalsAndLabsCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label cardHeader = new Label("2. Clinical Biomarkers & Physical Measurements");
        cardHeader.getStyleClass().add("card-header");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(14);

        // Row 0: Blood Pressure Systolic & Diastolic
        Label bpSysLabel = new Label("Systolic BP (mmHg)*");
        bpSysLabel.getStyleClass().add("form-label");
        systolicBpField = new TextField("120");
        systolicBpField.setPromptText("e.g. 120");

        Label bpDiaLabel = new Label("Diastolic BP (mmHg)*");
        bpDiaLabel.getStyleClass().add("form-label");
        diastolicBpField = new TextField("80");
        diastolicBpField.setPromptText("e.g. 80");

        grid.add(bpSysLabel, 0, 0);
        grid.add(systolicBpField, 0, 1);
        grid.add(bpDiaLabel, 1, 0);
        grid.add(diastolicBpField, 1, 1);

        // Row 1: Fasting Blood Sugar & Total Cholesterol
        Label gluLabel = new Label("Fasting Blood Sugar / Glucose (mg/dL)*");
        gluLabel.getStyleClass().add("form-label");
        glucoseField = new TextField("95");
        glucoseField.setPromptText("Normal: < 100, Diabetic: >= 126");

        Label cholLabel = new Label("Total Cholesterol (mg/dL)*");
        cholLabel.getStyleClass().add("form-label");
        cholesterolField = new TextField("185");
        cholesterolField.setPromptText("Normal: < 200, High: >= 240");

        grid.add(gluLabel, 0, 2);
        grid.add(glucoseField, 0, 3);
        grid.add(cholLabel, 1, 2);
        grid.add(cholesterolField, 1, 3);

        // Row 2: Serum Creatinine & BMI calculation
        Label creatLabel = new Label("Serum Creatinine (mg/dL)*");
        creatLabel.getStyleClass().add("form-label");
        creatinineField = new TextField("0.9");
        creatinineField.setPromptText("Normal: 0.6 - 1.2");

        VBox bmiBox = new VBox(4);
        Label bmiLabel = new Label("Body Mass Index (BMI)");
        bmiLabel.getStyleClass().add("form-label");
        bmiDisplayLabel = new Label("22.8 kg/m² (Normal weight)");
        bmiDisplayLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0284c7; -fx-padding: 8 0 0 0;");
        bmiBox.getChildren().addAll(bmiLabel, bmiDisplayLabel);

        grid.add(creatLabel, 0, 4);
        grid.add(creatinineField, 0, 5);
        grid.add(bmiBox, 1, 4, 1, 2);

        // Row 3: Height & Weight (auto-compute BMI)
        Label heightLabel = new Label("Height (cm)");
        heightLabel.getStyleClass().add("form-label");
        heightField = new TextField("172");
        heightField.setPromptText("cm");

        Label weightLabel = new Label("Weight (kg)");
        weightLabel.getStyleClass().add("form-label");
        weightField = new TextField("68");
        weightField.setPromptText("kg");

        heightField.textProperty().addListener((obs, o, n) -> updateBmi());
        weightField.textProperty().addListener((obs, o, n) -> updateBmi());

        grid.add(heightLabel, 0, 6);
        grid.add(heightField, 0, 7);
        grid.add(weightLabel, 1, 6);
        grid.add(weightField, 1, 7);

        // Column constraints
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        card.getChildren().addAll(cardHeader, grid);
        return card;
    }

    private void updateBmi() {
        try {
            double h = Double.parseDouble(heightField.getText().trim()) / 100.0;
            double w = Double.parseDouble(weightField.getText().trim());
            if (h > 0 && w > 0) {
                double bmi = w / (h * h);
                String category;
                if (bmi < 18.5) category = "Underweight";
                else if (bmi < 25.0) category = "Normal weight";
                else if (bmi < 30.0) category = "Overweight";
                else category = "Obese";
                bmiDisplayLabel.setText(String.format("%.1f kg/m² (%s)", bmi, category));
            }
        } catch (Exception ignored) {
            bmiDisplayLabel.setText("Enter valid height & weight");
        }
    }

    private VBox createSymptomsAndFamilyCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label cardHeader = new Label("3. Clinical Symptoms & Genetic Family History");
        cardHeader.getStyleClass().add("card-header");

        Label sympHeader = new Label("Reported Patient Symptoms (Select all that apply):");
        sympHeader.setStyle("-fx-font-weight: 600; -fx-text-fill: #334155;");

        FlowPane symptomsFlow = new FlowPane(16, 12);
        thirstCheck = new CheckBox("Excessive thirst (Polydipsia)");
        urinationCheck = new CheckBox("Frequent urination (Polyuria)");
        fatigueCheck = new CheckBox("Chronic unexplained fatigue");
        chestPainCheck = new CheckBox("Exertional chest discomfort / Angina");
        breathCheck = new CheckBox("Shortness of breath (Dyspnea)");
        edemaCheck = new CheckBox("Peripheral edema (Leg/Ankle swelling)");
        visionCheck = new CheckBox("Blurred vision");

        symptomsFlow.getChildren().addAll(
                thirstCheck, urinationCheck, fatigueCheck,
                chestPainCheck, breathCheck, edemaCheck, visionCheck
        );

        Separator sep = new Separator();

        Label famHeader = new Label("Family Medical History (First-degree relatives):");
        famHeader.setStyle("-fx-font-weight: 600; -fx-text-fill: #334155;");

        FlowPane famFlow = new FlowPane(20, 10);
        famDiabetesCheck = new CheckBox("Diabetes Mellitus");
        famHeartCheck = new CheckBox("Premature Coronary / Heart Disease");
        famKidneyCheck = new CheckBox("Chronic Kidney / Renal Disease");

        famFlow.getChildren().addAll(famDiabetesCheck, famHeartCheck, famKidneyCheck);

        card.getChildren().addAll(cardHeader, sympHeader, symptomsFlow, sep, famHeader, famFlow);
        return card;
    }

    private void handleRunDiagnosis() {
        Patient selected = patientComboBox.getValue();
        if (selected == null) {
            showError("Please select a patient.");
            return;
        }

        try {
            int sys = Integer.parseInt(systolicBpField.getText().trim());
            int dia = Integer.parseInt(diastolicBpField.getText().trim());
            double glu = Double.parseDouble(glucoseField.getText().trim());
            double chol = Double.parseDouble(cholesterolField.getText().trim());
            double creat = Double.parseDouble(creatinineField.getText().trim());

            double bmi = 23.5;
            try {
                double h = Double.parseDouble(heightField.getText().trim()) / 100.0;
                double w = Double.parseDouble(weightField.getText().trim());
                if (h > 0 && w > 0) bmi = w / (h * h);
            } catch (Exception ignored) {}

            List<String> sympList = new ArrayList<>();
            if (thirstCheck.isSelected()) sympList.add("Excessive thirst");
            if (urinationCheck.isSelected()) sympList.add("Frequent urination");
            if (fatigueCheck.isSelected()) sympList.add("Chronic fatigue");
            if (chestPainCheck.isSelected()) sympList.add("Chest discomfort");
            if (breathCheck.isSelected()) sympList.add("Shortness of breath");
            if (edemaCheck.isSelected()) sympList.add("Peripheral edema");
            if (visionCheck.isSelected()) sympList.add("Blurred vision");

            String symptomsStr = sympList.isEmpty() ? "None reported" : String.join(", ", sympList);

            List<String> famList = new ArrayList<>();
            if (famDiabetesCheck.isSelected()) famList.add("Diabetes");
            if (famHeartCheck.isSelected()) famList.add("Heart Disease");
            if (famKidneyCheck.isSelected()) famList.add("Kidney Disease");

            String famStr = famList.isEmpty() ? "None" : String.join(", ", famList);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String now = LocalDateTime.now().format(dtf);

            // Persist Medical Record in SQL database
            MedicalRecord record = new MedicalRecord(
                    selected.getPatientId(), symptomsStr, sys, dia, glu, chol, creat, bmi, famStr, now
            );
            medicalRecordDAO.create(record);

            // Execute Explainable Decision Engine
            RiskAssessmentResult result = decisionEngine.assessPatient(selected, record);

            errorLabel.setVisible(false);
            if (onAssessmentComplete != null) {
                onAssessmentComplete.accept(selected, result);
            }

        } catch (NumberFormatException ex) {
            showError("Please enter valid numerical values for blood pressure, glucose, cholesterol, and creatinine.");
        }
    }

    private void fillDiabeticSample() {
        systolicBpField.setText("142");
        diastolicBpField.setText("88");
        glucoseField.setText("172");
        cholesterolField.setText("215");
        creatinineField.setText("1.1");
        heightField.setText("168");
        weightField.setText("85");
        thirstCheck.setSelected(true);
        urinationCheck.setSelected(true);
        fatigueCheck.setSelected(true);
        chestPainCheck.setSelected(false);
        breathCheck.setSelected(false);
        edemaCheck.setSelected(false);
        visionCheck.setSelected(true);
        famDiabetesCheck.setSelected(true);
        famHeartCheck.setSelected(false);
        famKidneyCheck.setSelected(false);
        updateBmi();
        errorLabel.setVisible(false);
    }

    private void fillCardioSample() {
        systolicBpField.setText("165");
        diastolicBpField.setText("102");
        glucoseField.setText("105");
        cholesterolField.setText("268");
        creatinineField.setText("1.2");
        heightField.setText("175");
        weightField.setText("92");
        thirstCheck.setSelected(false);
        urinationCheck.setSelected(false);
        fatigueCheck.setSelected(true);
        chestPainCheck.setSelected(true);
        breathCheck.setSelected(true);
        edemaCheck.setSelected(false);
        visionCheck.setSelected(false);
        famDiabetesCheck.setSelected(false);
        famHeartCheck.setSelected(true);
        famKidneyCheck.setSelected(false);
        updateBmi();
        errorLabel.setVisible(false);
    }

    private void clearInputs() {
        systolicBpField.setText("");
        diastolicBpField.setText("");
        glucoseField.setText("");
        cholesterolField.setText("");
        creatinineField.setText("");
        heightField.setText("");
        weightField.setText("");
        bmiDisplayLabel.setText("—");
        thirstCheck.setSelected(false);
        urinationCheck.setSelected(false);
        fatigueCheck.setSelected(false);
        chestPainCheck.setSelected(false);
        breathCheck.setSelected(false);
        edemaCheck.setSelected(false);
        visionCheck.setSelected(false);
        famDiabetesCheck.setSelected(false);
        famHeartCheck.setSelected(false);
        famKidneyCheck.setSelected(false);
        errorLabel.setVisible(false);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
