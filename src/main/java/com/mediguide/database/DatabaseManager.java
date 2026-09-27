package com.mediguide.database;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;
    private Connection connection;
    private String databaseType = "sqlite"; // "sqlite" or "mysql"
    private String statusMessage = "Not connected";

    private DatabaseManager() {
        initConnection();
        initTables();
        seedInitialData();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initConnection() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load db.properties, using SQLite defaults", e);
        }

        String preferredType = props.getProperty("db.type", "sqlite").trim().toLowerCase();

        if ("mysql".equalsIgnoreCase(preferredType)) {
            try {
                String host = props.getProperty("db.mysql.host", "localhost");
                String port = props.getProperty("db.mysql.port", "3306");
                String dbName = props.getProperty("db.mysql.database", "mediguide_ai");
                String user = props.getProperty("db.mysql.username", "root");
                String pass = props.getProperty("db.mysql.password", "");

                // First ensure database exists
                String serverUrl = "jdbc:mysql://" + host + ":" + port + "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
                try (Connection tempConn = DriverManager.getConnection(serverUrl, user, pass);
                     Statement stmt = tempConn.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
                }

                String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
                connection = DriverManager.getConnection(dbUrl, user, pass);
                databaseType = "mysql";
                statusMessage = "Connected to MySQL (" + host + ":" + port + "/" + dbName + ")";
                LOGGER.info("Successfully connected to MySQL database.");
                return;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to connect to MySQL database. Falling back to SQLite.", e);
                // Fall back to SQLite below
            }
        }

        // SQLite connection (default and reliable fallback)
        try {
            String dbFile = props.getProperty("db.sqlite.file", "mediguide.db");
            String sqliteUrl = "jdbc:sqlite:" + dbFile;
            connection = DriverManager.getConnection(sqliteUrl);
            databaseType = "sqlite";
            statusMessage = "Connected to SQL Database (SQLite: " + dbFile + ")";
            LOGGER.info("Connected to SQLite database: " + sqliteUrl);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite database", e);
            statusMessage = "Error connecting to database: " + e.getMessage();
        }
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initConnection();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking database connection status", e);
        }
        return connection;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    private void initTables() {
        Connection conn = getConnection();
        if (conn == null) return;

        boolean isMysql = "mysql".equalsIgnoreCase(databaseType);
        String autoInc = isMysql ? "AUTO_INCREMENT" : "AUTOINCREMENT";

        try (Statement stmt = conn.createStatement()) {
            // 1. Users table (Admin & Doctor accounts)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY " + autoInc + ", " +
                    "username VARCHAR(50) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL, " +
                    "created_at VARCHAR(30) NOT NULL)");

            // 2. Patients table
            stmt.execute("CREATE TABLE IF NOT EXISTS patients (" +
                    "patient_id INTEGER PRIMARY KEY " + autoInc + ", " +
                    "name VARCHAR(100) NOT NULL, " +
                    "age INTEGER NOT NULL, " +
                    "gender VARCHAR(20) NOT NULL, " +
                    "phone VARCHAR(30), " +
                    "email VARCHAR(100), " +
                    "medical_history_notes TEXT, " +
                    "created_date VARCHAR(30) NOT NULL)");

            // 3. Medical Records table
            stmt.execute("CREATE TABLE IF NOT EXISTS medical_records (" +
                    "record_id INTEGER PRIMARY KEY " + autoInc + ", " +
                    "patient_id INTEGER NOT NULL, " +
                    "symptoms TEXT, " +
                    "blood_pressure_systolic INTEGER NOT NULL, " +
                    "blood_pressure_diastolic INTEGER NOT NULL, " +
                    "glucose DOUBLE NOT NULL, " +
                    "cholesterol DOUBLE NOT NULL, " +
                    "creatinine DOUBLE NOT NULL, " +
                    "bmi DOUBLE NOT NULL, " +
                    "family_history VARCHAR(255), " +
                    "recorded_date VARCHAR(30) NOT NULL, " +
                    "FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE)");

            // 4. Predictions table
            stmt.execute("CREATE TABLE IF NOT EXISTS predictions (" +
                    "prediction_id INTEGER PRIMARY KEY " + autoInc + ", " +
                    "patient_id INTEGER NOT NULL, " +
                    "condition_name VARCHAR(100) NOT NULL, " +
                    "risk_level VARCHAR(20) NOT NULL, " +
                    "confidence DOUBLE NOT NULL, " +
                    "contributing_factors TEXT, " +
                    "alternative_conditions TEXT, " +
                    "prediction_date VARCHAR(30) NOT NULL, " +
                    "FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE)");

            // 5. Doctor Feedback table
            stmt.execute("CREATE TABLE IF NOT EXISTS doctor_feedback (" +
                    "feedback_id INTEGER PRIMARY KEY " + autoInc + ", " +
                    "prediction_id INTEGER NOT NULL, " +
                    "doctor_id INTEGER, " +
                    "doctor_name VARCHAR(100), " +
                    "doctor_decision VARCHAR(20) NOT NULL, " +
                    "comments TEXT, " +
                    "feedback_date VARCHAR(30) NOT NULL, " +
                    "FOREIGN KEY (prediction_id) REFERENCES predictions(prediction_id) ON DELETE CASCADE)");

            LOGGER.info("Database tables verified/created successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating database tables", e);
        }
    }

    private void seedInitialData() {
        Connection conn = getConnection();
        if (conn == null) return;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String now = LocalDateTime.now().format(dtf);

        try {
            // Seed default users if table is empty
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    LOGGER.info("Seeding initial administrator and clinician users...");
                    String insertUser = "INSERT INTO users (username, password, full_name, role, created_at) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertUser)) {
                        // Admin account
                        ps.setString(1, "admin");
                        ps.setString(2, "admin123");
                        ps.setString(3, "Clinical Administrator");
                        ps.setString(4, "ADMIN");
                        ps.setString(5, now);
                        ps.executeUpdate();

                        // Doctor account
                        ps.setString(1, "doctor");
                        ps.setString(2, "doctor123");
                        ps.setString(3, "Dr. Sarah Mitchell, MD");
                        ps.setString(4, "DOCTOR");
                        ps.setString(5, now);
                        ps.executeUpdate();
                    }
                }
            }

            // Seed sample patients if table is empty
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM patients")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    LOGGER.info("Seeding sample patient cohort and initial clinical assessments...");
                    seedSamplePatients(conn, now);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error during data seeding", e);
        }
    }

    private void seedSamplePatients(Connection conn, String dateStr) throws SQLException {
        // Patient 1: Diabetic Risk Profile
        int p1 = insertPatient(conn, "Eleanor Vance", 58, "Female", "+1 (555) 234-5678", "eleanor.vance@example.com", "Type 2 diabetes family history, sedentary lifestyle", "2026-03-01 10:15");
        insertMedicalRecord(conn, p1, "Frequent urination, Excessive thirst, Chronic fatigue", 138, 88, 168.0, 215.0, 1.1, 29.4, "Diabetes Mellitus", "2026-03-01 10:30");
        int pred1 = insertPrediction(conn, p1, "Diabetes Mellitus", "High", 87.5,
                "Fasting blood glucose elevated: 168 mg/dL; Reported frequent thirst & polyuria; Family history indicator; BMI: 29.4 kg/m²",
                "Metabolic syndrome, Hyperglycemic pre-state", "2026-03-01 10:35");
        insertDoctorFeedback(conn, pred1, 2, "Dr. Sarah Mitchell, MD", "Accepted", "Clinical presentation aligns with high diabetic risk. Order HbA1c screening and lifestyle counseling.", "2026-03-01 11:00");

        // Patient 2: Cardiovascular Risk Profile
        int p2 = insertPatient(conn, "Marcus Sterling", 62, "Male", "+1 (555) 876-5432", "marcus.s@example.com", "Smoker, mild angina episodes, elevated lipid profile", "2026-03-05 14:20");
        insertMedicalRecord(conn, p2, "Chest tightness on exertion, Shortness of breath", 158, 96, 115.0, 265.0, 1.2, 31.2, "Hypertension, Coronary Artery Disease", "2026-03-05 14:40");
        int pred2 = insertPrediction(conn, p2, "Cardiovascular Disease", "High", 91.0,
                "Stage 2 Hypertension: 158/96 mmHg; High serum cholesterol: 265 mg/dL; Exertional dyspnea & chest tightness; Age > 60 factor",
                "Hypertensive heart disease, Atherosclerosis", "2026-03-05 14:45");
        insertDoctorFeedback(conn, pred2, 2, "Dr. Sarah Mitchell, MD", "Accepted", "Urgent cardiology referral and initiation of ACE inhibitor therapy.", "2026-03-05 15:10");

        // Patient 3: Chronic Kidney Disease Profile
        int p3 = insertPatient(conn, "Arthur Pendelton", 67, "Male", "+1 (555) 345-9876", "arthur.p@example.com", "Longstanding hypertension for 12 years", "2026-03-10 09:10");
        insertMedicalRecord(conn, p3, "Lower extremity edema, Morning puffiness, Fatigue", 144, 92, 122.0, 195.0, 1.9, 27.1, "Renal dysfunction, Hypertension", "2026-03-10 09:30");
        int pred3 = insertPrediction(conn, p3, "Chronic Kidney Disease", "High", 84.0,
                "Serum creatinine significantly elevated: 1.9 mg/dL; Persistent Stage 1/2 hypertension; Peripheral edema reported",
                "Nephrosclerosis, Diabetic nephropathy early phase", "2026-03-10 09:40");
        insertDoctorFeedback(conn, pred3, 2, "Dr. Sarah Mitchell, MD", "Accepted", "Order eGFR and 24-hr urine protein analysis. Nephrology consult required.", "2026-03-10 10:05");

        // Patient 4: Moderate Borderline Risk Profile
        int p4 = insertPatient(conn, "Sophia Chen", 44, "Female", "+1 (555) 765-4321", "sophia.chen@example.com", "Occasional headaches, annual physical checkup", "2026-03-14 11:30");
        insertMedicalRecord(conn, p4, "Mild fatigue, Afternoon headaches", 132, 84, 108.0, 210.0, 0.9, 25.8, "None reported", "2026-03-14 11:45");
        insertPrediction(conn, p4, "Cardiovascular Disease", "Medium", 62.0,
                "Elevated systolic BP: 132 mmHg (Pre-hypertension); Borderline cholesterol: 210 mg/dL; Mild fasting glucose: 108 mg/dL",
                "Essential pre-hypertension, Stress-related elevation", "2026-03-14 11:50");

        // Patient 5: Low Risk / Healthy Baseline
        int p5 = insertPatient(conn, "David Kim", 29, "Male", "+1 (555) 432-1098", "david.kim@example.com", "Marathon runner, non-smoker, no chronic conditions", "2026-03-18 16:00");
        insertMedicalRecord(conn, p5, "None / Routine screening", 118, 76, 88.0, 165.0, 0.8, 22.4, "None", "2026-03-18 16:15");
        insertPrediction(conn, p5, "Multi-Condition Screening", "Low", 95.0,
                "Optimal blood pressure: 118/76 mmHg; Normal fasting glucose: 88 mg/dL; Healthy lipid and creatinine markers",
                "No clinical conditions indicated", "2026-03-18 16:20");
    }

    private int insertPatient(Connection conn, String name, int age, String gender, String phone, String email, String notes, String date) throws SQLException {
        String sql = "INSERT INTO patients (name, age, gender, phone, email, medical_history_notes, created_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setInt(2, age);
            ps.setString(3, gender);
            ps.setString(4, phone);
            ps.setString(5, email);
            ps.setString(6, notes);
            ps.setString(7, date);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private void insertMedicalRecord(Connection conn, int patientId, String symptoms, int sys, int dia, double glu, double chol, double creat, double bmi, String famHist, String date) throws SQLException {
        String sql = "INSERT INTO medical_records (patient_id, symptoms, blood_pressure_systolic, blood_pressure_diastolic, glucose, cholesterol, creatinine, bmi, family_history, recorded_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, symptoms);
            ps.setInt(3, sys);
            ps.setInt(4, dia);
            ps.setDouble(5, glu);
            ps.setDouble(6, chol);
            ps.setDouble(7, creat);
            ps.setDouble(8, bmi);
            ps.setString(9, famHist);
            ps.setString(10, date);
            ps.executeUpdate();
        }
    }

    private int insertPrediction(Connection conn, int patientId, String condition, String riskLevel, double confidence, String factors, String alt, String date) throws SQLException {
        String sql = "INSERT INTO predictions (patient_id, condition_name, risk_level, confidence, contributing_factors, alternative_conditions, prediction_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setString(2, condition);
            ps.setString(3, riskLevel);
            ps.setDouble(4, confidence);
            ps.setString(5, factors);
            ps.setString(6, alt);
            ps.setString(7, date);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private void insertDoctorFeedback(Connection conn, int predictionId, int docId, String docName, String decision, String comments, String date) throws SQLException {
        String sql = "INSERT INTO doctor_feedback (prediction_id, doctor_id, doctor_name, doctor_decision, comments, feedback_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, predictionId);
            ps.setInt(2, docId);
            ps.setString(3, docName);
            ps.setString(4, decision);
            ps.setString(5, comments);
            ps.setString(6, date);
            ps.executeUpdate();
        }
    }
}
