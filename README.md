# MediGuide AI — Intelligent Healthcare Diagnosis & Explainable Decision Support System
### Review 1 Implementation (60% Milestone Active)

MediGuide AI is a clinical decision-support and explainable AI healthcare prototype built in **Java** (JavaFX 21), powered by an **SQL Database** (JDBC SQLite / MySQL), and designed following the **Model-View-Controller (MVC)** architectural pattern.

---

## 🌟 Key Features (Review 1 / 60% Scope)

1. **Modern Clinical User Interface**:
   - High-contrast clinical theme with deep navy slate sidebar (`#0f172a`), clean card surfaces, and color-coded risk alerts.
   - Smooth navigation: Dashboard, Patient Directory, Clinical Vitals Entry, Patient History, Database & System Status.

2. **Role-Based Authentication**:
   - **Admin Login**: Full access to patient records, database status, and clinical management.
   - **Doctor Login**: Clinician portal with diagnostic evaluation and human-in-the-loop decision recording.
   - **Quick Demo Fill**: One-click buttons to instantly populate `admin/admin123` or `doctor/doctor123` during evaluations.

3. **Relational SQL Database (Zero-Config + MySQL Support)**:
   - Built-in automatic SQLite database (`mediguide.db`) in the project root — **runs instantly anywhere with zero passwords or server setup needed**.
   - Fully compatible with local MySQL Server 8.0 via `src/main/resources/db.properties`.
   - Automatic table schema creation (`users`, `patients`, `medical_records`, `predictions`, `doctor_feedback`).
   - Automatically pre-seeds a realistic clinical cohort with 5 diverse patients (diabetic, cardiovascular, chronic kidney, borderline, and healthy baseline).

4. **Patient Management (Full CRUD & Live Search)**:
   - **Create**: Add new patients with demographic and baseline notes.
   - **Read**: Live search filter by patient name, phone, or ID.
   - **Update**: Modal dialog to edit patient information.
   - **Delete**: Safely delete patient and cascading medical history with confirmation.
   - **Direct Diagnosis**: Jump straight into vital evaluation for any selected patient.

5. **Dual Decision Engine (Review 1 Signature Feature)**:
   - **Layer 1 (Intelligent Prediction)**: Multi-condition clinical risk scoring for **Diabetes Mellitus**, **Cardiovascular Disease**, and **Chronic Kidney Disease (CKD)**.
   - **Layer 2 (Explainable Rule Engine)**: Generates transparent, human-readable evidence checklists answering **"Why did the system reach this conclusion?"** with biomarker measurements vs. clinical thresholds.
   - **System Confidence Score**: Calibrated confidence meter (68% - 96%).
   - **Differential Possibilities**: Suggests alternative diagnoses (e.g. Metabolic Syndrome, Renovascular HTN).
   - **Clinical Recommendations**: Guidelines on laboratory confirmatory tests (HbA1c, eGFR, lipid profile).

6. **Human-in-the-Loop Doctor Feedback Simulation**:
   - Clinicians can **Accept**, **Modify**, or **Reject** the AI decision.
   - Enter physician notes or override rationale and persist directly to the SQL database.

7. **Patient Longitudinal History & Audit Trail**:
   - View chronological history of vitals, blood pressure, blood glucose, creatinine, past AI predictions, and clinician decisions for any patient.

---

## 🚀 How to Run in IntelliJ IDEA

1. Open **IntelliJ IDEA**.
2. Click **File → Open...** and select the folder:
   ```
   c:\Users\anubh\OneDrive\Desktop\MediGuideAI
   ```
   *(Select `pom.xml` when prompted to open as a Maven project).*
3. Wait for IntelliJ IDEA to sync Maven dependencies.
4. Run the application in one of two ways:
   - **Option A (Recommended)**: Select **`Run MediGuideAI`** from the top-right Run configurations dropdown and click the green **▶ Play** button.
   - **Option B**: Navigate to `src/main/java/com/mediguide/AppLauncher.java`, right-click, and select **Run 'AppLauncher.main()'**.

---

## 💻 How to Run from Terminal / Command Line

Using the included Maven Wrapper:

```powershell
# In PowerShell / Command Prompt inside the project directory:
.\mvnw.cmd javafx:run
```

Or run automated tests:
```powershell
.\mvnw.cmd test
```

---

## 🔑 Demo Credentials

| Role | Username | Password | Purpose |
|------|----------|----------|---------|
| **System Administrator** | `admin` | `admin123` | Full administrative & patient management access |
| **Medical Clinician** | `doctor` | `doctor123` | Clinical vitals entry, diagnosis & feedback simulation |

*(Click the "Use Admin" or "Use Doctor" buttons on the login screen to auto-fill instantly).*

---

## 🗄️ Relational Database Schema

- `users`: User authentication, credentials, and access roles (`ADMIN`, `DOCTOR`).
- `patients`: Patient demographics (`patient_id`, `name`, `age`, `gender`, `phone`, `email`, `medical_history_notes`, `created_date`).
- `medical_records`: Clinical vitals (`record_id`, `patient_id`, `symptoms`, `systolic_bp`, `diastolic_bp`, `glucose`, `cholesterol`, `creatinine`, `bmi`, `family_history`, `recorded_date`).
- `predictions`: AI evaluations (`prediction_id`, `patient_id`, `condition_name`, `risk_level`, `confidence`, `contributing_factors`, `alternative_conditions`, `prediction_date`).
- `doctor_feedback`: Clinician oversight (`feedback_id`, `prediction_id`, `doctor_id`, `doctor_name`, `doctor_decision`, `comments`, `feedback_date`).

---

## 📂 Project Architecture

```
MediGuideAI/
├── pom.xml                                 # Maven build & JavaFX / JDBC dependencies
├── mvnw & mvnw.cmd                         # Maven wrapper
├── mediguide.db                            # SQLite relational database file
├── .run/
│   └── Run_MediGuideAI.run.xml             # IntelliJ IDEA 1-click Run configuration
└── src/
    ├── main/
    │   ├── resources/
    │   │   ├── db.properties               # Database configuration (SQLite / MySQL)
    │   │   ├── schema.sql                  # DDL table creation script
    │   │   └── styles.css                  # Custom healthcare design system
    │   └── java/com/mediguide/
    │       ├── MainApp.java                # JavaFX Application entry point
    │       ├── AppLauncher.java            # Standard Java launcher for IntelliJ IDEA
    │       ├── models/                     # Domain Entities
    │       │   ├── User.java
    │       │   ├── Patient.java
    │       │   ├── MedicalRecord.java
    │       │   ├── Prediction.java
    │       │   ├── DoctorFeedback.java
    │       │   └── RiskAssessmentResult.java
    │       ├── database/
    │       │   └── DatabaseManager.java    # Dual-mode JDBC connection manager & seeder
    │       ├── dao/                        # Data Access Objects (CRUD)
    │       │   ├── UserDAO.java
    │       │   ├── PatientDAO.java
    │       │   ├── MedicalRecordDAO.java
    │       │   ├── PredictionDAO.java
    │       │   └── DoctorFeedbackDAO.java
    │       ├── engine/                     # Explainable Decision Support Engine
    │       │   ├── RuleEngine.java         # Clinical thresholds & biomarker weighting
    │       │   ├── RiskCalculator.java     # Score to risk level & confidence mapping
    │       │   ├── ExplanationGenerator.java # XAI reasoning, alternatives & recommendations
    │       │   └── DecisionEngine.java     # Dual-engine coordinator
    │       └── ui/                         # JavaFX Modern Views
    │           ├── LoginView.java          # Split login with Admin/Doctor auth & quick fill
    │           ├── MainLayout.java         # Sidebar & persistent navbar layout
    │           ├── DashboardView.java      # Executive clinical analytics & metrics
    │           ├── PatientManagementView.java # Patient registry & live search
    │           ├── MedicalDataEntryView.java  # Vitals entry, auto-BMI, symptoms checklist
    │           ├── ExplainableResultView.java # The Review 1 Signature Feature (XAI Decision Card)
    │           ├── PatientHistoryView.java    # Longitudinal audit trail & records
    │           └── DatabaseInfoView.java      # DB status & Review 1 milestone tracker
    └── test/java/com/mediguide/
        ├── DecisionEngineTest.java         # Unit tests verifying XAI calculations
        └── DatabaseManagerTest.java        # Unit tests verifying SQL CRUD operations
```
