package com.mediguide.ai;

import com.mediguide.dao.PatientDAO;
import com.mediguide.dao.MedicalRecordDAO;
import com.mediguide.dao.PredictionDAO;
import com.mediguide.models.Patient;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Prediction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MediGuideChatEngine — Offline, rule-based AI assistant for MediGuide.
 * Handles patient registry operations, medical guidance, navigation help,
 * and contextual analysis without requiring any external API.
 */
public class MediGuideChatEngine {

    private final PatientDAO patientDAO = new PatientDAO();
    private final MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
    private final PredictionDAO predictionDAO = new PredictionDAO();

    // Conversation memory for multi-step interactions
    private String pendingAction = null;
    private final Map<String, String> pendingData = new HashMap<>();

    /**
     * Process a user message and return a response string.
     */
    public String processMessage(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Please type a message so I can help you.";
        }

        String input = userMessage.trim();
        String lower = input.toLowerCase();

        // --- Handle pending multi-step conversations ---
        if (pendingAction != null) {
            return handlePendingAction(input, lower);
        }

        // --- Greeting ---
        if (matchesAny(lower, "hello", "hi", "hey", "good morning", "good afternoon", "good evening", "assalam", "salam")) {
            return "Hello! 👋 I'm MediGuide AI Assistant. Here's what I can help you with:\n\n"
                    + "• Register new patient — type 'new patient' or 'register patient'\n"
                    + "• Search patients — type 'search <name>' or 'find patient'\n"
                    + "• List all patients — type 'list patients' or 'show all patients'\n"
                    + "• Patient count — type 'how many patients'\n"
                    + "• View patient details — type 'patient #ID' (e.g. 'patient #1')\n"
                    + "• Navigation help — type 'how to enter vitals' or 'where to see reports'\n"
                    + "• Medical info — ask about BP, glucose, BMI, cholesterol, etc.\n\n"
                    + "How can I assist you today?";
        }

        // --- Help / Commands ---
        if (matchesAny(lower, "help", "commands", "what can you do", "options", "menu")) {
            return getHelpText();
        }

        // --- Register New Patient (multi-step) ---
        if (matchesAny(lower, "new patient", "register patient", "add patient", "create patient",
                "new registry", "new patient registry", "register new patient", "add new patient")) {
            pendingAction = "REGISTER_NAME";
            pendingData.clear();
            return "📝 Let's register a new patient!\n\nPlease enter the patient's full name:";
        }

        // --- List All Patients ---
        if (matchesAny(lower, "list patients", "show patients", "all patients", "show all patients",
                "patient list", "show registry", "view patients", "list all")) {
            return handleListPatients();
        }

        // --- Patient Count ---
        if (matchesAny(lower, "how many patients", "patient count", "total patients", "count patients",
                "number of patients")) {
            int count = patientDAO.count();
            return "📊 There are currently " + count + " patients registered in the MediGuide database.";
        }

        // --- View Patient by ID ---
        Pattern idPattern = Pattern.compile("(?:patient|view|show|get|details|info)\\s*#?(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher idMatcher = idPattern.matcher(input);
        if (idMatcher.find()) {
            int patientId = Integer.parseInt(idMatcher.group(1));
            return handleViewPatient(patientId);
        }

        // Also match just "#<number>" or "id <number>"
        Pattern hashPattern = Pattern.compile("^#(\\d+)$");
        Matcher hashMatcher = hashPattern.matcher(input.trim());
        if (hashMatcher.find()) {
            int patientId = Integer.parseInt(hashMatcher.group(1));
            return handleViewPatient(patientId);
        }

        // --- Search Patients ---
        if (matchesAny(lower, "search patient", "find patient", "search", "find", "lookup patient")) {
            // Check if there's a name/query after the keyword
            String query = extractAfter(lower, "search patient", "find patient", "search", "find", "lookup patient");
            if (query != null && !query.isEmpty()) {
                return handleSearchPatients(query);
            } else {
                pendingAction = "SEARCH_QUERY";
                return "🔍 Who would you like to search for? Enter a patient name, phone number, or ID:";
            }
        }

        // --- Delete Patient ---
        if (lower.startsWith("delete patient") || lower.startsWith("remove patient")) {
            String afterKeyword = extractAfter(lower, "delete patient", "remove patient");
            if (afterKeyword != null && !afterKeyword.isEmpty()) {
                try {
                    int delId = Integer.parseInt(afterKeyword.replaceAll("[^0-9]", ""));
                    return handleDeletePatient(delId);
                } catch (NumberFormatException e) {
                    // Search by name instead
                    List<Patient> results = patientDAO.search(afterKeyword);
                    if (results.isEmpty()) {
                        return "❌ No patient found matching '" + afterKeyword + "'. Use 'list patients' to see all records.";
                    } else if (results.size() == 1) {
                        return handleDeletePatient(results.get(0).getPatientId());
                    } else {
                        StringBuilder sb = new StringBuilder("Multiple patients match that name. Please specify the ID:\n\n");
                        for (Patient p : results) {
                            sb.append("  • #").append(p.getPatientId()).append(" — ").append(p.getName())
                                    .append(" (").append(p.getAge()).append(" yrs, ").append(p.getGender()).append(")\n");
                        }
                        sb.append("\nType 'delete patient #ID' to remove a specific patient.");
                        return sb.toString();
                    }
                }
            }
            pendingAction = "DELETE_QUERY";
            return "🗑️ Which patient would you like to delete? Enter the patient ID or name:";
        }

        // --- Navigation Guidance ---
        if (matchesAny(lower, "enter vitals", "how to enter vitals", "input vitals", "clinical vitals",
                "diagnostic vitals", "how to diagnose", "where to enter data")) {
            return "🩺 To enter patient vitals and run AI analysis:\n\n"
                    + "1. Click '⚡ AI Patient Analysis' in the left sidebar\n"
                    + "2. Select an existing patient from the dropdown, or register a new one first\n"
                    + "3. Fill in the clinical biomarkers (BP, glucose, cholesterol, BMI, creatinine)\n"
                    + "4. Enter any symptoms the patient is presenting\n"
                    + "5. Click 'ANALYZE PATIENT' to get an AI risk assessment\n\n"
                    + "The system will generate an Explainable Decision Support Card with risk levels and contributing factors.";
        }

        if (matchesAny(lower, "reports", "view reports", "clinical reports", "where to see reports",
                "patient history", "see history", "health screen")) {
            return "📜 To view patient clinical reports and history:\n\n"
                    + "1. Click '📜 Patient Health Screen' in the left sidebar\n"
                    + "2. Search for the patient by name or ID\n"
                    + "3. You'll see their complete medical timeline including:\n"
                    + "   — Recorded vitals history\n"
                    + "   — AI predictions & risk assessments\n"
                    + "   — Doctor feedback & clinical decisions\n\n"
                    + "You can also access this from the Patient Directory by clicking 'Patient Screen' on any patient row.";
        }

        if (matchesAny(lower, "dashboard", "overview", "statistics", "analytics")) {
            return "📊 The Health Dashboard provides:\n\n"
                    + "• Total patient count and record statistics\n"
                    + "• Risk distribution across the patient population\n"
                    + "• Recent predictions and alerts\n"
                    + "• Quick-access cards for common tasks\n\n"
                    + "Click '📊 Health Dashboard' in the left sidebar to view it.";
        }

        if (matchesAny(lower, "patient directory", "patient registry", "manage patients",
                "where to manage patients", "register")) {
            return "👥 The Patient Directory is where you manage all patient records:\n\n"
                    + "• Click '👥 Patient Directory' in the left sidebar\n"
                    + "• Register new patients with '+ Register New Patient' button\n"
                    + "• Search, edit, or delete existing patient records\n"
                    + "• Click 'Analyze' to jump to AI diagnosis for any patient\n"
                    + "• Click 'Patient Screen' to view their full medical history\n\n"
                    + "Or just tell me 'register patient' and I'll help you right here!";
        }

        // --- Medical Information ---
        if (matchesAny(lower, "blood pressure", "bp", "hypertension", "systolic", "diastolic")) {
            return "💉 Blood Pressure (BP) Reference Guide:\n\n"
                    + "• Normal: < 120/80 mmHg\n"
                    + "• Elevated: 120-129 / < 80 mmHg\n"
                    + "• High BP Stage 1: 130-139 / 80-89 mmHg\n"
                    + "• High BP Stage 2: ≥ 140 / ≥ 90 mmHg\n"
                    + "• Hypertensive Crisis: > 180 / > 120 mmHg\n\n"
                    + "In MediGuide, enter both systolic and diastolic values in the 'Diagnostic Vitals Input' section.";
        }

        if (matchesAny(lower, "glucose", "blood sugar", "diabetes", "fasting glucose", "sugar level")) {
            return "🍬 Fasting Blood Glucose Reference:\n\n"
                    + "• Normal: 70-99 mg/dL\n"
                    + "• Pre-diabetes: 100-125 mg/dL\n"
                    + "• Diabetes (indicative): ≥ 126 mg/dL\n\n"
                    + "Elevated glucose combined with symptoms like frequent urination, excessive thirst, and fatigue strongly suggests diabetic risk. "
                    + "MediGuide's AI engine uses glucose as a key biomarker for diabetes risk assessment.";
        }

        if (matchesAny(lower, "cholesterol", "lipid", "hdl", "ldl", "triglyceride")) {
            return "🫀 Total Cholesterol Reference:\n\n"
                    + "• Desirable: < 200 mg/dL\n"
                    + "• Borderline High: 200-239 mg/dL\n"
                    + "• High: ≥ 240 mg/dL\n\n"
                    + "High cholesterol is a major risk factor for cardiovascular disease. "
                    + "MediGuide factors this into heart disease risk predictions.";
        }

        if (matchesAny(lower, "bmi", "body mass index", "weight", "obesity", "overweight")) {
            return "⚖️ BMI (Body Mass Index) Categories:\n\n"
                    + "• Underweight: < 18.5\n"
                    + "• Normal: 18.5 - 24.9\n"
                    + "• Overweight: 25.0 - 29.9\n"
                    + "• Obese Class I: 30.0 - 34.9\n"
                    + "• Obese Class II: 35.0 - 39.9\n"
                    + "• Obese Class III: ≥ 40.0\n\n"
                    + "BMI = weight(kg) / height(m)². It is a contributing factor in diabetes, cardiovascular, and kidney disease risk.";
        }

        if (matchesAny(lower, "creatinine", "kidney", "renal", "ckd", "kidney disease", "egfr")) {
            return "🧪 Serum Creatinine Reference:\n\n"
                    + "• Normal (Male): 0.7-1.3 mg/dL\n"
                    + "• Normal (Female): 0.6-1.1 mg/dL\n"
                    + "• Mildly elevated: 1.3-1.9 mg/dL\n"
                    + "• Significantly elevated: ≥ 2.0 mg/dL\n\n"
                    + "Elevated creatinine indicates potential kidney dysfunction. "
                    + "MediGuide uses this as a primary marker for Chronic Kidney Disease (CKD) risk assessment.";
        }

        // --- About / Info ---
        if (matchesAny(lower, "about", "what is mediguide", "about mediguide", "what is this app",
                "tell me about this", "about this app")) {
            return "🏥 MediGuide AI — Explainable Clinical Decision Support System\n\n"
                    + "MediGuide AI is a health intelligence platform that helps clinicians:\n\n"
                    + "• Register and manage patient records in a SQL database\n"
                    + "• Input clinical vitals and biomarkers\n"
                    + "• Run AI-powered risk assessments for conditions like:\n"
                    + "  — Diabetes Mellitus\n"
                    + "  — Cardiovascular Disease\n"
                    + "  — Chronic Kidney Disease\n"
                    + "• View explainable results with contributing factors\n"
                    + "• Track patient history and doctor feedback over time\n\n"
                    + "The system combines rule-based clinical logic with machine learning for accurate, transparent predictions.";
        }

        // --- Thank you ---
        if (matchesAny(lower, "thanks", "thank you", "thx", "ty", "great", "awesome", "perfect")) {
            return "You're welcome! 😊 Feel free to ask me anything else. I'm here to help with patient management and clinical guidance.";
        }

        // --- Bye ---
        if (matchesAny(lower, "bye", "goodbye", "see you", "exit", "quit")) {
            return "Goodbye! 👋 Stay healthy. I'll be right here if you need assistance.";
        }

        // --- Fallback: try to search as patient name ---
        List<Patient> fallbackSearch = patientDAO.search(input);
        if (!fallbackSearch.isEmpty()) {
            StringBuilder sb = new StringBuilder("🔍 I found " + fallbackSearch.size() + " patient(s) matching '" + input + "':\n\n");
            for (Patient p : fallbackSearch) {
                sb.append("  • #").append(p.getPatientId()).append(" — ").append(p.getName())
                        .append(" (").append(p.getAge()).append(" yrs, ").append(p.getGender()).append(")")
                        .append(" | 📞 ").append(p.getPhone() != null ? p.getPhone() : "N/A")
                        .append("\n");
            }
            sb.append("\nType 'patient #ID' for full details, or 'help' to see all commands.");
            return sb.toString();
        }

        // --- True Fallback ---
        return "🤔 I didn't quite understand that. Here are some things you can try:\n\n"
                + "• 'register patient' — Add a new patient to the registry\n"
                + "• 'list patients' — View all registered patients\n"
                + "• 'search <name>' — Find a patient by name\n"
                + "• 'patient #1' — View details of patient with ID 1\n"
                + "• 'how to enter vitals' — Navigation guidance\n"
                + "• 'blood pressure' — Medical reference info\n"
                + "• 'help' — See all available commands\n\n"
                + "You can also just type a patient's name and I'll search for them!";
    }

    /**
     * Process context-based analysis requests (from ExplainableResultView).
     */
    public String processAnalysis(String promptContext) {
        if (promptContext == null || promptContext.isEmpty()) {
            return "No analysis data was provided. Please run an AI analysis on a patient first.";
        }

        // The promptContext already contains pre-formatted analysis data
        // Provide an intelligent summary based on keyword detection
        String lower = promptContext.toLowerCase();

        StringBuilder response = new StringBuilder("📋 AI Analysis Summary:\n\n");

        // Detect risk level
        if (lower.contains("high risk") || lower.contains("risk_level: high") || lower.contains("risk level: high")) {
            response.append("⚠️ HIGH RISK detected. Immediate clinical attention recommended.\n\n");
        } else if (lower.contains("medium risk") || lower.contains("moderate")) {
            response.append("⚡ MODERATE RISK detected. Monitoring and follow-up recommended.\n\n");
        } else if (lower.contains("low risk")) {
            response.append("✅ LOW RISK detected. Continue routine monitoring.\n\n");
        }

        // Detect conditions
        if (lower.contains("diabetes")) {
            response.append("🍬 Diabetes Indicators:\n")
                    .append("  — Monitor fasting glucose levels regularly\n")
                    .append("  — Consider HbA1c screening if glucose > 126 mg/dL\n")
                    .append("  — Lifestyle modifications: diet, exercise, weight management\n\n");
        }
        if (lower.contains("cardiovascular") || lower.contains("heart") || lower.contains("hypertension")) {
            response.append("🫀 Cardiovascular Indicators:\n")
                    .append("  — Monitor blood pressure trends\n")
                    .append("  — Lipid panel review recommended if cholesterol > 200 mg/dL\n")
                    .append("  — Consider cardiology referral for high-risk patients\n\n");
        }
        if (lower.contains("kidney") || lower.contains("renal") || lower.contains("ckd") || lower.contains("creatinine")) {
            response.append("🧪 Kidney Function Indicators:\n")
                    .append("  — Track serum creatinine and eGFR trends\n")
                    .append("  — 24-hour urine protein analysis for elevated creatinine\n")
                    .append("  — Nephrology consult if creatinine > 1.5 mg/dL\n\n");
        }

        response.append("📌 Recommendation: Review the Explainable Decision Card for detailed contributing factors and confidence scores. "
                + "Use 'Patient Health Screen' to compare with historical records.");

        return response.toString();
    }

    // ---------- Multi-step interaction handlers ----------

    private String handlePendingAction(String input, String lower) {
        if (matchesAny(lower, "cancel", "stop", "nevermind", "never mind", "abort", "exit")) {
            pendingAction = null;
            pendingData.clear();
            return "✅ Action cancelled. How else can I help you?";
        }

        switch (pendingAction) {
            case "REGISTER_NAME":
                pendingData.put("name", input);
                pendingAction = "REGISTER_AGE";
                return "Got it! Patient name: " + input + "\n\nNow enter the patient's age (number):";

            case "REGISTER_AGE":
                try {
                    int age = Integer.parseInt(input.trim());
                    if (age < 0 || age > 150) {
                        return "Please enter a valid age between 0 and 150:";
                    }
                    pendingData.put("age", String.valueOf(age));
                    pendingAction = "REGISTER_GENDER";
                    return "Age: " + age + " ✓\n\nEnter gender (Male / Female / Other):";
                } catch (NumberFormatException e) {
                    return "⚠️ That doesn't look like a valid age. Please enter a number (e.g., 45):";
                }

            case "REGISTER_GENDER":
                String gender = input.trim();
                if (!gender.equalsIgnoreCase("Male") && !gender.equalsIgnoreCase("Female") && !gender.equalsIgnoreCase("Other")) {
                    return "Please enter 'Male', 'Female', or 'Other':";
                }
                pendingData.put("gender", capitalize(gender));
                pendingAction = "REGISTER_PHONE";
                return "Gender: " + capitalize(gender) + " ✓\n\nEnter 10-digit phone number:";

            case "REGISTER_PHONE":
                String digits = input.replaceAll("[^0-9]", "");
                if (digits.length() != 10) {
                    return "⚠️ Phone number must be exactly 10 digits. Please re-enter:";
                }
                pendingData.put("phone", digits);
                pendingAction = "REGISTER_EMAIL";
                return "Phone: " + digits + " ✓\n\nEnter email address (or type 'skip' to skip):";

            case "REGISTER_EMAIL":
                String email = input.trim();
                if (lower.equals("skip") || lower.equals("none") || lower.equals("na") || lower.equals("n/a")) {
                    email = "";
                } else if (!email.contains("@") || !email.contains(".")) {
                    return "⚠️ That doesn't look like a valid email. Enter a valid email or type 'skip':";
                }
                pendingData.put("email", email);
                pendingAction = "REGISTER_NOTES";
                return "Email: " + (email.isEmpty() ? "(skipped)" : email) + " ✓\n\nEnter any medical history notes (or type 'skip'):";

            case "REGISTER_NOTES":
                String notes = input.trim();
                if (lower.equals("skip") || lower.equals("none") || lower.equals("na") || lower.equals("n/a")) {
                    notes = "";
                }
                pendingData.put("notes", notes);
                pendingAction = null;
                return registerPatient();

            case "SEARCH_QUERY":
                pendingAction = null;
                return handleSearchPatients(input);

            case "DELETE_QUERY":
                pendingAction = null;
                try {
                    int delId = Integer.parseInt(input.replaceAll("[^0-9]", ""));
                    return handleDeletePatient(delId);
                } catch (NumberFormatException e) {
                    List<Patient> results = patientDAO.search(input);
                    if (results.isEmpty()) {
                        return "❌ No patient found matching '" + input + "'.";
                    } else if (results.size() == 1) {
                        return handleDeletePatient(results.get(0).getPatientId());
                    } else {
                        StringBuilder sb = new StringBuilder("Multiple matches found. Specify the ID:\n\n");
                        for (Patient p : results) {
                            sb.append("  • #").append(p.getPatientId()).append(" — ").append(p.getName()).append("\n");
                        }
                        sb.append("\nType 'delete patient #ID' to remove a specific patient.");
                        return sb.toString();
                    }
                }

            default:
                pendingAction = null;
                pendingData.clear();
                return "Something went wrong with the conversation flow. Let's start fresh. How can I help?";
        }
    }

    // ---------- Action handlers ----------

    private String registerPatient() {
        try {
            String name = pendingData.get("name");
            int age = Integer.parseInt(pendingData.get("age"));
            String gender = pendingData.get("gender");
            String phone = pendingData.get("phone");
            String email = pendingData.getOrDefault("email", "");
            String notes = pendingData.getOrDefault("notes", "");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String now = LocalDateTime.now().format(dtf);

            Patient patient = new Patient(name, age, gender, phone, email, notes, now);
            int newId = patientDAO.create(patient);
            pendingData.clear();

            if (newId > 0) {
                return "✅ Patient registered successfully!\n\n"
                        + "📋 Patient Record:\n"
                        + "  • ID: #" + newId + "\n"
                        + "  • Name: " + name + "\n"
                        + "  • Age: " + age + " years\n"
                        + "  • Gender: " + gender + "\n"
                        + "  • Phone: " + phone + "\n"
                        + "  • Email: " + (email.isEmpty() ? "N/A" : email) + "\n"
                        + "  • Notes: " + (notes.isEmpty() ? "None" : notes) + "\n"
                        + "  • Registered: " + now + "\n\n"
                        + "You can now go to '⚡ AI Patient Analysis' to run diagnostics on this patient, "
                        + "or visit '👥 Patient Directory' to manage their record.";
            } else {
                return "❌ Failed to register the patient. Please try again or use the '+ Register New Patient' button in the Patient Directory.";
            }
        } catch (Exception e) {
            pendingData.clear();
            return "❌ Error while registering patient: " + e.getMessage() + "\nPlease try again.";
        }
    }

    private String handleListPatients() {
        List<Patient> patients = patientDAO.findAll();
        if (patients.isEmpty()) {
            return "📋 No patients found in the registry. Type 'register patient' to add one!";
        }

        StringBuilder sb = new StringBuilder("📋 Patient Registry (" + patients.size() + " patients):\n\n");
        int shown = 0;
        for (Patient p : patients) {
            sb.append("  #").append(String.format("%03d", p.getPatientId()))
                    .append(" | ").append(p.getName())
                    .append(" | ").append(p.getAge()).append(" yrs")
                    .append(" | ").append(p.getGender())
                    .append(" | 📞 ").append(p.getPhone() != null ? p.getPhone() : "N/A")
                    .append("\n");
            shown++;
            if (shown >= 15) {
                sb.append("  ... and ").append(patients.size() - 15).append(" more.\n");
                break;
            }
        }
        sb.append("\nType 'patient #ID' for full details, or 'search <name>' to filter.");
        return sb.toString();
    }

    private String handleSearchPatients(String query) {
        List<Patient> results = patientDAO.search(query.trim());
        if (results.isEmpty()) {
            return "🔍 No patients found matching '" + query.trim() + "'.\n\n"
                    + "Try a different search term, or type 'list patients' to see all records.";
        }

        StringBuilder sb = new StringBuilder("🔍 Found " + results.size() + " patient(s) matching '" + query.trim() + "':\n\n");
        for (Patient p : results) {
            sb.append("  • #").append(p.getPatientId()).append(" — ").append(p.getName())
                    .append(" (").append(p.getAge()).append(" yrs, ").append(p.getGender()).append(")")
                    .append(" | 📞 ").append(p.getPhone() != null ? p.getPhone() : "N/A")
                    .append("\n");
        }
        sb.append("\nType 'patient #ID' for full details.");
        return sb.toString();
    }

    private String handleViewPatient(int patientId) {
        Patient p = patientDAO.findById(patientId);
        if (p == null) {
            return "❌ No patient found with ID #" + patientId + ". Use 'list patients' to see available records.";
        }

        StringBuilder sb = new StringBuilder("📋 Patient Details — #" + p.getPatientId() + "\n\n");
        sb.append("  👤 Name: ").append(p.getName()).append("\n");
        sb.append("  🎂 Age: ").append(p.getAge()).append(" years\n");
        sb.append("  ⚧ Gender: ").append(p.getGender()).append("\n");
        sb.append("  📞 Phone: ").append(p.getPhone() != null ? p.getPhone() : "N/A").append("\n");
        sb.append("  ✉️ Email: ").append(p.getEmail() != null && !p.getEmail().isEmpty() ? p.getEmail() : "N/A").append("\n");
        sb.append("  📝 Notes: ").append(p.getMedicalHistoryNotes() != null && !p.getMedicalHistoryNotes().isEmpty() ? p.getMedicalHistoryNotes() : "None").append("\n");
        sb.append("  📅 Registered: ").append(p.getCreatedDate()).append("\n");

        // Try to get latest medical record
        List<MedicalRecord> records = medicalRecordDAO.findByPatientId(patientId);
        if (!records.isEmpty()) {
            MedicalRecord latest = records.get(0);
            sb.append("\n🩺 Latest Vitals (").append(latest.getRecordedDate()).append("):\n");
            sb.append("  • BP: ").append(latest.getBloodPressureString()).append("\n");
            sb.append("  • Glucose: ").append(latest.getGlucose()).append(" mg/dL\n");
            sb.append("  • Cholesterol: ").append(latest.getCholesterol()).append(" mg/dL\n");
            sb.append("  • Creatinine: ").append(latest.getCreatinine()).append(" mg/dL\n");
            sb.append("  • BMI: ").append(latest.getBmi()).append(" kg/m²\n");
            if (latest.getSymptoms() != null && !latest.getSymptoms().isEmpty()) {
                sb.append("  • Symptoms: ").append(latest.getSymptoms()).append("\n");
            }
        }

        // Try to get latest prediction
        List<Prediction> predictions = predictionDAO.findByPatientId(patientId);
        if (!predictions.isEmpty()) {
            Prediction latestPred = predictions.get(0);
            sb.append("\n🤖 Latest AI Assessment:\n");
            sb.append("  • Condition: ").append(latestPred.getConditionName()).append("\n");
            sb.append("  • Risk Level: ").append(latestPred.getRiskLevel()).append("\n");
            sb.append("  • Confidence: ").append(String.format("%.1f%%", latestPred.getConfidence())).append("\n");
        }

        sb.append("\n💡 Tip: Go to '⚡ AI Patient Analysis' to run a new diagnosis, or '📜 Patient Health Screen' for full history.");
        return sb.toString();
    }

    private String handleDeletePatient(int patientId) {
        Patient p = patientDAO.findById(patientId);
        if (p == null) {
            return "❌ No patient found with ID #" + patientId + ".";
        }

        boolean deleted = patientDAO.delete(patientId);
        if (deleted) {
            return "🗑️ Patient #" + patientId + " (" + p.getName() + ") has been removed from the registry.\n\n"
                    + "⚠️ All associated medical records, predictions, and feedback have also been deleted.";
        } else {
            return "❌ Failed to delete patient #" + patientId + ". Please try from the Patient Directory instead.";
        }
    }

    // ---------- Utility methods ----------

    private String getHelpText() {
        return "🏥 MediGuide AI Assistant — Available Commands:\n\n"
                + "📝 PATIENT REGISTRY:\n"
                + "  • 'register patient' — Add a new patient step-by-step\n"
                + "  • 'list patients' — View all registered patients\n"
                + "  • 'search <name>' — Find patients by name/phone/ID\n"
                + "  • 'patient #1' — View full details for patient ID 1\n"
                + "  • 'how many patients' — Get total patient count\n"
                + "  • 'delete patient #1' — Remove a patient record\n\n"
                + "🧭 NAVIGATION:\n"
                + "  • 'how to enter vitals' — Guide to diagnostic input\n"
                + "  • 'reports' — How to view clinical reports\n"
                + "  • 'dashboard' — Dashboard information\n"
                + "  • 'patient directory' — Patient management guide\n\n"
                + "💊 MEDICAL REFERENCES:\n"
                + "  • 'blood pressure' — BP normal ranges\n"
                + "  • 'glucose' — Blood sugar reference\n"
                + "  • 'cholesterol' — Lipid panel info\n"
                + "  • 'bmi' — BMI categories\n"
                + "  • 'creatinine' — Kidney function markers\n\n"
                + "ℹ️ OTHER:\n"
                + "  • 'about' — About MediGuide AI\n"
                + "  • 'help' — Show this command list\n\n"
                + "💡 You can also just type a patient's name and I'll search for them!";
    }

    private boolean matchesAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractAfter(String input, String... keywords) {
        for (String keyword : keywords) {
            int idx = input.indexOf(keyword);
            if (idx >= 0) {
                String after = input.substring(idx + keyword.length()).trim();
                if (!after.isEmpty()) {
                    return after;
                }
            }
        }
        return null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
