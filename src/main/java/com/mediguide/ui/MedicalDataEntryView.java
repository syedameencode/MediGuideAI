package com.mediguide.ui;

import com.mediguide.dao.MedicalRecordDAO;
import com.mediguide.dao.PatientDAO;
import com.mediguide.engine.DecisionEngine;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;
import com.mediguide.models.User;
import com.mediguide.util.UIAnimationHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * MedicalDataEntryView — AI Analysis Screen.
 * Clinical vitals and biomarker intake with prominent [ ANALYZE PATIENT ] interaction.
 */
public class MedicalDataEntryView extends ScrollPane {

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final DecisionEngine decisionEngine = new DecisionEngine();

    private final User currentUser;
    private final BiConsumer<Patient, RiskAssessmentResult> onAssessmentComplete;
    private Patient preSelectedPatient;

    private ComboBox<Patient> patientComboBox;
    private Label patientInfoChip;

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
    private Button runDiagnosisBtn;

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
        contentBox.setMaxWidth(1060);

        // Header
        VBox headerBox = new VBox(4);
        Label title = new Label("Patient Diagnostic Intake & AI Analysis");
        title.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Input physiological biomarkers, vital signs, and clinical symptomatology for explainable AI evaluation");
        subtitle.getStyleClass().add("page-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // Error Banner
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-background-color: #fef2f2; -fx-border-color: #fecdd3; -fx-border-radius: 8; -fx-padding: 10 16; -fx-background-radius: 8; -fx-font-size: 13px; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // 1. Patient Selection Card
        VBox patientCard = createPatientSelectionCard();

        // 2. Vitals & Laboratory Biomarkers Card
        VBox vitalsCard = createVitalsAndLabsCard();

        // 3. Symptoms & Family History Card
        VBox symptomsCard = createSymptomsAndFamilyCard();

        // 4. Prominent Action Bar with [ ANALYZE PATIENT ]
        HBox actionRow = createActionBar();

        contentBox.getChildren().addAll(headerBox, errorLabel, patientCard, vitalsCard, symptomsCard, actionRow);
        setContent(contentBox);

        // Subtle entrance animation
        UIAnimationHelper.slideInUp(contentBox);
    }

    private VBox createPatientSelectionCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label cardHeader = new Label("1. Select Patient");
        cardHeader.getStyleClass().add("card-header");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        patientInfoChip = new Label();
        patientInfoChip.getStyleClass().add("badge-neutral");
        patientInfoChip.setVisible(false);

        top.getChildren().addAll(cardHeader, sp, patientInfoChip);

        patientComboBox = new ComboBox<>();
        patientComboBox.setMaxWidth(Double.MAX_VALUE);
        List<Patient> patients = patientDAO.findAll();
        patientComboBox.getItems().addAll(patients);

        patientComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    setText(p.getName() + " (#" + p.getPatientId() + ") — Age: " + p.getAge() + ", " + p.getGender() + " | Phone: " + p.getPhone());
                }
            }
        });
        patientComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText("Select registered patient...");
                } else {
                    setText(p.getName() + " (#" + p.getPatientId() + ") — " + p.getAge() + " yrs, " + p.getGender());
                }
            }
        });

        patientComboBox.setOnAction(e -> updatePatientChip());

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

        updatePatientChip();

        Label hint = new Label("Select an existing patient from the database or register a new patient in the Patient Registry.");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(top, patientComboBox, hint);
        return card;
    }

    private void updatePatientChip() {
        Patient p = patientComboBox.getValue();
        if (p != null) {
            patientInfoChip.setText("ID #" + p.getPatientId() + " | " + p.getGender() + ", Age " + p.getAge() + " | " + p.getPhone());
            patientInfoChip.setVisible(true);
        } else {
            patientInfoChip.setVisible(false);
        }
    }

    private VBox createVitalsAndLabsCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label cardHeader = new Label("2. Physiological Biomarkers & Physical Measurements");
        cardHeader.getStyleClass().add("card-header");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label rangeNote = new Label("Values used in ML inference & clinical rule engine");
        rangeNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #0284c7; -fx-font-weight: 600;");
        top.getChildren().addAll(cardHeader, sp, rangeNote);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(16);

        // Column 1: Blood Pressure & Glucose
        systolicBpField = new TextField("138");
        diastolicBpField = new TextField("88");
        glucoseField = new TextField("145.0");

        VBox sysBox = createFieldWithLabel("Systolic Blood Pressure (mmHg)", systolicBpField, "Optimal < 120, Stage 1: 130-139, Stage 2: >= 140");
        VBox diaBox = createFieldWithLabel("Diastolic Blood Pressure (mmHg)", diastolicBpField, "Optimal < 80, Stage 1: 80-89, Stage 2: >= 90");
        VBox gluBox = createFieldWithLabel("Fasting Blood Glucose (mg/dL)", glucoseField, "Normal: 70–99, Pre-diabetic: 100–125, Diabetic: >= 126");

        // Column 2: Cholesterol, Creatinine, BMI
        cholesterolField = new TextField("215.0");
        creatinineField = new TextField("1.2");

        VBox cholBox = createFieldWithLabel("Total Serum Cholesterol (mg/dL)", cholesterolField, "Desirable: < 200, Borderline: 200–239, High: >= 240");
        VBox creatBox = createFieldWithLabel("Serum Creatinine (mg/dL)", creatinineField, "Normal: 0.7–1.3 mg/dL (Female: 0.6–1.1)");

        // Height & Weight for BMI
        heightField = new TextField("172");
        weightField = new TextField("82");
        bmiDisplayLabel = new Label("27.7 kg/m²");
        bmiDisplayLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #0284c7;");

        heightField.textProperty().addListener((obs, o, n) -> updateBmi());
        weightField.textProperty().addListener((obs, o, n) -> updateBmi());

        HBox hwBox = new HBox(12);
        VBox hCol = createFieldWithLabel("Height (cm)", heightField, null);
        VBox wCol = createFieldWithLabel("Weight (kg)", weightField, null);
        HBox.setHgrow(hCol, Priority.ALWAYS);
        HBox.setHgrow(wCol, Priority.ALWAYS);
        hwBox.getChildren().addAll(hCol, wCol);

        VBox bmiBox = new VBox(6);
        Label bmiLbl = new Label("Calculated BMI:");
        bmiLbl.getStyleClass().add("form-label");
        HBox bmiRow = new HBox(8, bmiDisplayLabel, new Label("(Overweight: 25.0–29.9)"));
        bmiRow.setAlignment(Pos.CENTER_LEFT);
        bmiRow.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        bmiBox.getChildren().addAll(bmiLbl, bmiRow);

        grid.add(sysBox, 0, 0);
        grid.add(diaBox, 1, 0);
        grid.add(gluBox, 0, 1);
        grid.add(cholBox, 1, 1);
        grid.add(creatBox, 0, 2);
        grid.add(hwBox, 1, 2);
        grid.add(bmiBox, 1, 3);

        card.getChildren().addAll(top, grid);
        return card;
    }

    private VBox createFieldWithLabel(String labelText, TextField field, String helpText) {
        VBox box = new VBox(5);
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        box.getChildren().addAll(label, field);
        if (helpText != null) {
            Label help = new Label(helpText);
            help.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            box.getChildren().add(help);
        }
        return box;
    }

    private VBox createSymptomsAndFamilyCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label cardHeader = new Label("3. Clinical Symptomatology & Genetic Risk Factors");
        cardHeader.getStyleClass().add("card-header");

        Label sympHeader = new Label("Reported Symptoms (Multi-Condition Indicators):");
        sympHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        FlowPane sympPane = new FlowPane(16, 12);
        thirstCheck = new CheckBox("Excessive thirst (Polydipsia)");
        urinationCheck = new CheckBox("Frequent urination (Polyuria)");
        fatigueCheck = new CheckBox("Chronic fatigue / Lethargy");
        chestPainCheck = new CheckBox("Chest tightness / Angina");
        breathCheck = new CheckBox("Dyspnea (Shortness of breath)");
        edemaCheck = new CheckBox("Peripheral edema (Leg/Ankle swelling)");
        visionCheck = new CheckBox("Blurred vision");

        sympPane.getChildren().addAll(
                thirstCheck, urinationCheck, fatigueCheck,
                chestPainCheck, breathCheck, edemaCheck, visionCheck
        );

        Separator sep = new Separator();

        Label famHeader = new Label("Family Medical History (Documented in First-Degree Relatives):");
        famHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #334155;");

        HBox famBox = new HBox(24);
        famDiabetesCheck = new CheckBox("Diabetes Mellitus");
        famHeartCheck = new CheckBox("Cardiovascular / CAD");
        famKidneyCheck = new CheckBox("Chronic Kidney / Renal");
        famBox.getChildren().addAll(famDiabetesCheck, famHeartCheck, famKidneyCheck);

        card.getChildren().addAll(cardHeader, sympHeader, sympPane, sep, famHeader, famBox);
        return card;
    }

    private HBox createActionBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(10, 0, 30, 0));

        Button quickDiabetic = new Button("Diabetic High Risk");
        quickDiabetic.getStyleClass().add("btn-secondary");
        quickDiabetic.setStyle("-fx-font-size: 11px;");
        quickDiabetic.setOnAction(e -> fillDiabeticSample());

        Button quickCardio = new Button("Cardiac Risk");
        quickCardio.getStyleClass().add("btn-secondary");
        quickCardio.setStyle("-fx-font-size: 11px;");
        quickCardio.setOnAction(e -> fillCardioSample());

        Button quickKidney = new Button("Kidney Risk");
        quickKidney.getStyleClass().add("btn-secondary");
        quickKidney.setStyle("-fx-font-size: 11px;");
        quickKidney.setOnAction(e -> fillKidneySample());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn-secondary");
        clearBtn.setStyle("-fx-font-size: 11px;");
        clearBtn.setOnAction(e -> clearInputs());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // THE PROMINENT [ ANALYZE PATIENT ] INTERACTION
        runDiagnosisBtn = new Button("⚡ ANALYZE PATIENT");
        runDiagnosisBtn.getStyleClass().add("btn-analyze");
        runDiagnosisBtn.setOnAction(e -> {
            UIAnimationHelper.pulseClick(runDiagnosisBtn, this::handleRunDiagnosis);
        });

        bar.getChildren().addAll(
                new Label("Quick Presets:"), quickDiabetic, quickCardio, quickKidney, clearBtn,
                spacer,
                runDiagnosisBtn
        );
        return bar;
    }

    private void updateBmi() {
        try {
            double h = Double.parseDouble(heightField.getText().trim()) / 100.0;
            double w = Double.parseDouble(weightField.getText().trim());
            if (h > 0 && w > 0) {
                double bmi = w / (h * h);
                String cat;
                if (bmi < 18.5) cat = "(Underweight)";
                else if (bmi < 25.0) cat = "(Optimal: 18.5–24.9)";
                else if (bmi < 30.0) cat = "(Overweight: 25.0–29.9)";
                else cat = "(Obese: >= 30.0)";
                bmiDisplayLabel.setText(String.format("%.1f kg/m² %s", bmi, cat));
            }
        } catch (Exception ignored) {
            bmiDisplayLabel.setText("—");
        }
    }

    private void handleRunDiagnosis() {
        Patient selected = patientComboBox.getValue();
        if (selected == null) {
            showError("Please select a patient before analyzing.");
            return;
        }

        try {
            int sys = Integer.parseInt(systolicBpField.getText().trim());
            int dia = Integer.parseInt(diastolicBpField.getText().trim());
            double glu = Double.parseDouble(glucoseField.getText().trim());
            double chol = Double.parseDouble(cholesterolField.getText().trim());
            double creat = Double.parseDouble(creatinineField.getText().trim());

            double bmi = 24.0;
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
            errorLabel.setManaged(false);

            if (onAssessmentComplete != null) {
                onAssessmentComplete.accept(selected, result);
            }

        } catch (NumberFormatException ex) {
            showError("Please enter valid numeric values for blood pressure, glucose, cholesterol, and creatinine.");
        }
    }

    private void fillDiabeticSample() {
        systolicBpField.setText("142");
        diastolicBpField.setText("88");
        glucoseField.setText("172.0");
        cholesterolField.setText("215.0");
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
        hideError();
    }

    private void fillCardioSample() {
        systolicBpField.setText("165");
        diastolicBpField.setText("102");
        glucoseField.setText("105.0");
        cholesterolField.setText("268.0");
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
        hideError();
    }

    private void fillKidneySample() {
        systolicBpField.setText("148");
        diastolicBpField.setText("94");
        glucoseField.setText("118.0");
        cholesterolField.setText("195.0");
        creatinineField.setText("2.2");
        heightField.setText("170");
        weightField.setText("78");
        thirstCheck.setSelected(false);
        urinationCheck.setSelected(false);
        fatigueCheck.setSelected(true);
        chestPainCheck.setSelected(false);
        breathCheck.setSelected(false);
        edemaCheck.setSelected(true);
        visionCheck.setSelected(false);
        famDiabetesCheck.setSelected(false);
        famHeartCheck.setSelected(false);
        famKidneyCheck.setSelected(true);
        updateBmi();
        hideError();
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
        hideError();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
