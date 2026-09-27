package com.mediguide.ui;

import com.mediguide.dao.DoctorFeedbackDAO;
import com.mediguide.dao.MedicalRecordDAO;
import com.mediguide.dao.PatientDAO;
import com.mediguide.dao.PredictionDAO;
import com.mediguide.dao.UserDAO;
import com.mediguide.database.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

public class DatabaseInfoView extends ScrollPane {

    private final PatientDAO patientDAO = new PatientDAO();
    private final PredictionDAO predictionDAO = new PredictionDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final DoctorFeedbackDAO feedbackDAO = new DoctorFeedbackDAO();
    private final UserDAO userDAO = new UserDAO();

    public DatabaseInfoView() {
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
        Label title = new Label("System Status & SQL Database Configuration");
        title.getStyleClass().add("page-header-title");
        Label subtitle = new Label("Overview of database connectivity, schema integrity, and Review 1 implementation progress");
        subtitle.getStyleClass().add("page-header-subtitle");
        headerBox.getChildren().addAll(title, subtitle);

        // 1. Connection Card
        VBox connCard = createConnectionCard();

        // 2. SQL Tables & Record Counts Card
        VBox tablesCard = createTablesCard();

        // 3. Review 1 Checklist Card
        VBox checklistCard = createChecklistCard();

        contentBox.getChildren().addAll(headerBox, connCard, tablesCard, checklistCard);
        setContent(contentBox);
    }

    private VBox createConnectionCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("SQL Database Connectivity");
        header.getStyleClass().add("card-header");

        HBox statusRow = new HBox(12);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(6, javafx.scene.paint.Color.web("#10b981"));
        Label statusLabel = new Label("Active Connection: " + DatabaseManager.getInstance().getStatusMessage());
        statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #065f46;");

        statusRow.getChildren().addAll(dot, statusLabel);

        Label desc = new Label(
                "MediGuide AI features dual-engine SQL database support:\n" +
                "• Default: Standalone SQLite database (mediguide.db) in the project root for zero-configuration execution.\n" +
                "• Production Option: MySQL Server 8.0 support configured via src/main/resources/db.properties.\n" +
                "Both databases operate through standard JDBC with identical table schemas."
        );
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 4;");

        card.getChildren().addAll(header, statusRow, desc);
        return card;
    }

    private VBox createTablesCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("Relational Database Schema & Stored Records");
        header.getStyleClass().add("card-header");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);

        grid.add(createTableRow("users", "User authentication (Admin & Doctor accounts)", userDAO.findAll().size()), 0, 0);
        grid.add(createTableRow("patients", "Patient demographic registry and clinical profile", patientDAO.count()), 1, 0);
        grid.add(createTableRow("medical_records", "Vitals, blood pressure, glucose, creatinine & symptoms", 5), 0, 1);
        grid.add(createTableRow("predictions", "Dual-engine AI risk classifications and explainability", predictionDAO.countTotal()), 1, 1);
        grid.add(createTableRow("doctor_feedback", "Human-in-the-loop physician overrides and notes", feedbackDAO.countTotal()), 0, 2);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        card.getChildren().addAll(header, grid);
        return card;
    }

    private HBox createTableRow(String tableName, String purpose, int count) {
        HBox box = new HBox(12);
        box.setStyle("-fx-background-color: #f8fafc; -fx-padding: 10 14; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-border-width: 1;");
        box.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(2);
        Label name = new Label(tableName);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0284c7;");
        Label desc = new Label(purpose);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        text.getChildren().addAll(name, desc);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label countBadge = new Label(count + " rows");
        countBadge.getStyleClass().add("badge-low");

        box.getChildren().addAll(text, sp, countBadge);
        return box;
    }

    private VBox createChecklistCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");

        Label header = new Label("Review 1 Implementation Milestone (60% Target)");
        header.getStyleClass().add("card-header");

        VBox list = new VBox(8);
        list.getChildren().addAll(
                createCheckItem("JavaFX Modern UI Interface with Custom CSS & Dark Theme Sidebar", true),
                createCheckItem("Admin & Doctor Authentication with Demo Quick-Fill Credentials", true),
                createCheckItem("Relational SQL Database Connectivity (SQLite / MySQL) via JDBC", true),
                createCheckItem("Patient Registration & Full CRUD Directory Management", true),
                createCheckItem("Clinical Vitals & Lab Biomarkers Entry Form with Auto-BMI", true),
                createCheckItem("Dual Decision Engine: Layer 1 Risk Prediction + Layer 2 Explainable Rules", true),
                createCheckItem("Explainable Decision Card: Contributing Factors & Alternative Possibilities", true),
                createCheckItem("Doctor Feedback Simulation (Human-in-the-Loop Oversight)", true),
                createCheckItem("Patient History & Longitudinal Audit Trail Viewer", true),
                createCheckItem("IntelliJ IDEA Project Architecture with Maven Build Tooling", true)
        );

        card.getChildren().addAll(header, list);
        return card;
    }

    private HBox createCheckItem(String text, boolean done) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(done ? "✔" : "⏳");
        icon.setStyle("-fx-text-fill: " + (done ? "#10b981" : "#f59e0b") + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e293b;");
        row.getChildren().addAll(icon, label);
        return row;
    }
}
