package com.mediguide.ui;

import com.mediguide.dao.PatientDAO;
import com.mediguide.models.Patient;
import com.mediguide.models.User;
import com.mediguide.util.UIAnimationHelper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PatientManagementView extends VBox {

    private final PatientDAO patientDAO = new PatientDAO();
    private final User currentUser;
    private final Consumer<Patient> onDiagnosePatient;
    private final Consumer<Patient> onViewHistory;

    private TableView<Patient> tableView;
    private ObservableList<Patient> patientList;
    private TextField searchField;
    private Label countLabel;

    public PatientManagementView(User currentUser, Consumer<Patient> onDiagnosePatient, Consumer<Patient> onViewHistory) {
        this.currentUser = currentUser;
        this.onDiagnosePatient = onDiagnosePatient;
        this.onViewHistory = onViewHistory;
        initUI();
    }

    private void initUI() {
        setSpacing(20);
        getStyleClass().add("content-area");

        // Header & Actions
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleCol = new VBox(2);
        Label title = new Label("Patient Directory & Registry");
        title.getStyleClass().add("page-header-title");
        countLabel = new Label("Managing registered patient records in SQL database");
        countLabel.getStyleClass().add("page-header-subtitle");
        titleCol.getChildren().addAll(title, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addPatientBtn = new Button("+ Register New Patient");
        addPatientBtn.getStyleClass().add("btn-primary");
        addPatientBtn.setOnAction(e -> showAddPatientDialog());

        headerRow.getChildren().addAll(titleCol, spacer, addPatientBtn);

        // Filter / Search Toolbar
        HBox searchToolbar = new HBox(12);
        searchToolbar.setAlignment(Pos.CENTER_LEFT);
        searchToolbar.getStyleClass().add("card");
        searchToolbar.setPadding(new Insets(12, 16, 12, 16));

        Label searchIcon = new Label("🔍 Search Directory:");
        searchIcon.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");

        searchField = new TextField();
        searchField.setPromptText("Filter by patient name, phone number, or ID...");
        searchField.setPrefWidth(380);
        searchField.textProperty().addListener((obs, o, n) -> handleSearch(n));

        Button refreshBtn = new Button("Refresh List");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> refreshData());

        searchToolbar.getChildren().addAll(searchIcon, searchField, refreshBtn);

        // Table
        tableView = createPatientTable();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getChildren().addAll(headerRow, searchToolbar, tableView);
        refreshData();

        UIAnimationHelper.slideInUp(this);
    }

    @SuppressWarnings("unchecked")
    private TableView<Patient> createPatientTable() {
        TableView<Patient> table = new TableView<>();
        patientList = FXCollections.observableArrayList();
        table.setItems(patientList);

        TableColumn<Patient, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getPatientId()));
        idCol.setPrefWidth(60);

        TableColumn<Patient, String> nameCol = new TableColumn<>("PATIENT NAME");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(180);

        TableColumn<Patient, Number> ageCol = new TableColumn<>("AGE");
        ageCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getAge()));
        ageCol.setPrefWidth(60);

        TableColumn<Patient, String> genderCol = new TableColumn<>("GENDER");
        genderCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getGender()));
        genderCol.setPrefWidth(80);

        TableColumn<Patient, String> phoneCol = new TableColumn<>("PHONE");
        phoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        phoneCol.setPrefWidth(140);

        TableColumn<Patient, String> emailCol = new TableColumn<>("EMAIL");
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        emailCol.setPrefWidth(180);

        TableColumn<Patient, String> dateCol = new TableColumn<>("REGISTERED");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedDate()));
        dateCol.setPrefWidth(130);

        // Actions Column
        TableColumn<Patient, Void> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setPrefWidth(280);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button diagnoseBtn = new Button("⚡ Analyze");
            private final Button historyBtn = new Button("Patient Screen");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(6, diagnoseBtn, historyBtn, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER_LEFT);
                diagnoseBtn.getStyleClass().add("btn-primary");
                diagnoseBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12;");
                diagnoseBtn.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    if (onDiagnosePatient != null) onDiagnosePatient.accept(p);
                });

                historyBtn.getStyleClass().add("btn-secondary");
                historyBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12;");
                historyBtn.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    if (onViewHistory != null) onViewHistory.accept(p);
                });

                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 12;");
                editBtn.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    showEditPatientDialog(p);
                });

                deleteBtn.getStyleClass().add("btn-danger");
                deleteBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8; -fx-background-radius: 12;");
                deleteBtn.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    handleDeletePatient(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, ageCol, genderCol, phoneCol, emailCol, dateCol, actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    private void refreshData() {
        List<Patient> list = patientDAO.findAll();
        patientList.setAll(list);
        countLabel.setText("Currently managing " + list.size() + " registered patient records in SQL database");
    }

    private void handleSearch(String query) {
        List<Patient> filtered = patientDAO.search(query);
        patientList.setAll(filtered);
    }

    private void showAddPatientDialog() {
        Dialog<Patient> dialog = new Dialog<>();
        dialog.setTitle("Register New Patient");
        dialog.setHeaderText("Add Patient Record to MediGuide AI Database");

        ButtonType saveBtnType = new ButtonType("Register Patient", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");
        TextField ageField = new TextField();
        ageField.setPromptText("Age (years)");
        ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList("Female", "Male", "Other"));
        genderBox.setValue("Female");
        TextField phoneField = new TextField();
        phoneField.setPromptText("10-digit Mobile Number (e.g. 9876543210)");
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String digitsOnly = newVal.replaceAll("[^\\d]", "");
                if (digitsOnly.length() > 10) {
                    digitsOnly = digitsOnly.substring(0, 10);
                }
                if (!newVal.equals(digitsOnly)) {
                    phoneField.setText(digitsOnly);
                }
            }
        });

        TextField emailField = new TextField();
        emailField.setPromptText("patient@example.com");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Known conditions, family medical history, allergies...");
        notesArea.setPrefRowCount(3);

        grid.add(new Label("Full Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Age:*"), 0, 1);
        grid.add(ageField, 1, 1);
        grid.add(new Label("Gender:*"), 0, 2);
        grid.add(genderBox, 1, 2);
        grid.add(new Label("Phone (10 Digits):*"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Email (.com):*"), 0, 4);
        grid.add(emailField, 1, 4);
        grid.add(new Label("Medical Notes:"), 0, 5);
        grid.add(notesArea, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                try {
                    String name = nameField.getText().trim();
                    int age = Integer.parseInt(ageField.getText().trim());
                    String gender = genderBox.getValue();
                    String phone = phoneField.getText().trim();
                    String email = emailField.getText().trim();
                    String notes = notesArea.getText().trim();

                    if (name.isEmpty()) {
                        showErrorAlert("Validation Error", "Patient name cannot be empty.");
                        return null;
                    }
                    if (phone.length() != 10) {
                        showErrorAlert("Validation Error", "Mobile number must be exactly 10 digits.");
                        return null;
                    }
                    if (!email.isEmpty() && (!email.toLowerCase().endsWith(".com") || !email.contains("@"))) {
                        showErrorAlert("Validation Error", "Email address must contain '@' and end with .com");
                        return null;
                    }

                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    String now = LocalDateTime.now().format(dtf);

                    return new Patient(name, age, gender, phone, email, notes, now);
                } catch (Exception ex) {
                    showErrorAlert("Validation Error", "Please enter valid numeric age.");
                    return null;
                }
            }
            return null;
        });

        Optional<Patient> result = dialog.showAndWait();
        result.ifPresent(patient -> {
            int newId = patientDAO.create(patient);
            if (newId > 0) {
                refreshData();
            }
        });
    }

    private void showEditPatientDialog(Patient patient) {
        Dialog<Patient> dialog = new Dialog<>();
        dialog.setTitle("Edit Patient #" + patient.getPatientId());
        dialog.setHeaderText("Update Patient Information");

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField nameField = new TextField(patient.getName());
        TextField ageField = new TextField(String.valueOf(patient.getAge()));
        ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList("Female", "Male", "Other"));
        genderBox.setValue(patient.getGender());
        TextField phoneField = new TextField(patient.getPhone());
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String digitsOnly = newVal.replaceAll("[^\\d]", "");
                if (digitsOnly.length() > 10) {
                    digitsOnly = digitsOnly.substring(0, 10);
                }
                if (!newVal.equals(digitsOnly)) {
                    phoneField.setText(digitsOnly);
                }
            }
        });

        TextField emailField = new TextField(patient.getEmail());
        TextArea notesArea = new TextArea(patient.getMedicalHistoryNotes());
        notesArea.setPrefRowCount(3);

        grid.add(new Label("Full Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Age:*"), 0, 1);
        grid.add(ageField, 1, 1);
        grid.add(new Label("Gender:*"), 0, 2);
        grid.add(genderBox, 1, 2);
        grid.add(new Label("Phone (10 Digits):*"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Email (.com):*"), 0, 4);
        grid.add(emailField, 1, 4);
        grid.add(new Label("Medical Notes:"), 0, 5);
        grid.add(notesArea, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                try {
                    String name = nameField.getText().trim();
                    int age = Integer.parseInt(ageField.getText().trim());
                    String phone = phoneField.getText().trim();
                    String email = emailField.getText().trim();

                    if (name.isEmpty()) {
                        showErrorAlert("Validation Error", "Patient name cannot be empty.");
                        return null;
                    }
                    if (phone.length() != 10) {
                        showErrorAlert("Validation Error", "Mobile number must be exactly 10 digits.");
                        return null;
                    }
                    if (!email.isEmpty() && (!email.toLowerCase().endsWith(".com") || !email.contains("@"))) {
                        showErrorAlert("Validation Error", "Email address must contain '@' and end with .com");
                        return null;
                    }

                    patient.setName(name);
                    patient.setAge(age);
                    patient.setGender(genderBox.getValue());
                    patient.setPhone(phone);
                    patient.setEmail(email);
                    patient.setMedicalHistoryNotes(notesArea.getText().trim());
                    return patient;
                } catch (Exception ex) {
                    showErrorAlert("Validation Error", "Please enter valid numeric age.");
                    return null;
                }
            }
            return null;
        });

        Optional<Patient> res = dialog.showAndWait();
        res.ifPresent(p -> {
            patientDAO.update(p);
            refreshData();
        });
    }

    private void handleDeletePatient(Patient patient) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Patient: " + patient.getName() + " (#" + patient.getPatientId() + ")?");
        alert.setContentText("This will permanently remove the patient, all recorded vitals, predictions, and doctor feedback from the database.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            patientDAO.delete(patient.getPatientId());
            refreshData();
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
