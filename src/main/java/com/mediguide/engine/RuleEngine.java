package com.mediguide.engine;

import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;

import java.util.ArrayList;
import java.util.List;

public class RuleEngine {

    public static class ConditionScore {
        public final String conditionName;
        public final int score;
        public final List<String> factors;

        public ConditionScore(String conditionName, int score, List<String> factors) {
            this.conditionName = conditionName;
            this.score = Math.min(100, Math.max(0, score));
            this.factors = factors;
        }
    }

    public ConditionScore evaluateDiabetes(Patient patient, MedicalRecord record) {
        int score = 5;
        List<String> factors = new ArrayList<>();
        String symptoms = record.getSymptoms() != null ? record.getSymptoms().toLowerCase() : "";
        String famHist = record.getFamilyHistory() != null ? record.getFamilyHistory().toLowerCase() : "";

        // Fasting blood glucose
        double glu = record.getGlucose();
        if (glu >= 180) {
            score += 55;
            factors.add("Severe fasting hyperglycemia: " + glu + " mg/dL (Normal < 100 mg/dL)");
        } else if (glu >= 126) {
            score += 40;
            factors.add("Elevated fasting glucose above diagnostic threshold: " + glu + " mg/dL (>= 126 mg/dL)");
        } else if (glu >= 100) {
            score += 20;
            factors.add("Impaired fasting glucose / pre-diabetes range: " + glu + " mg/dL (100-125 mg/dL)");
        }

        // BMI assessment
        double bmi = record.getBmi();
        if (bmi >= 30.0) {
            score += 20;
            factors.add("Obese BMI index: " + String.format("%.1f", bmi) + " kg/m² (>= 30.0)");
        } else if (bmi >= 25.0) {
            score += 10;
            factors.add("Overweight BMI index: " + String.format("%.1f", bmi) + " kg/m² (25.0-29.9)");
        }

        // Symptoms
        if (symptoms.contains("thirst")) {
            score += 15;
            factors.add("Reported polydipsia (excessive thirst)");
        }
        if (symptoms.contains("urination")) {
            score += 15;
            factors.add("Reported polyuria (frequent urination)");
        }
        if (symptoms.contains("fatigue")) {
            score += 8;
            factors.add("Reported chronic fatigue / lethargy");
        }
        if (symptoms.contains("vision")) {
            score += 10;
            factors.add("Reported blurred vision");
        }

        // Family history
        if (famHist.contains("diabet")) {
            score += 15;
            factors.add("Documented family history of diabetes mellitus");
        }

        // Age factor
        if (patient.getAge() >= 45) {
            score += 8;
            factors.add("Age-related metabolic risk factor (" + patient.getAge() + " years)");
        }

        return new ConditionScore("Diabetes Mellitus", score, factors);
    }

    public ConditionScore evaluateCardiovascular(Patient patient, MedicalRecord record) {
        int score = 5;
        List<String> factors = new ArrayList<>();
        String symptoms = record.getSymptoms() != null ? record.getSymptoms().toLowerCase() : "";
        String famHist = record.getFamilyHistory() != null ? record.getFamilyHistory().toLowerCase() : "";

        // Blood pressure
        int sys = record.getSystolicBp();
        int dia = record.getDiastolicBp();

        if (sys >= 180 || dia >= 120) {
            score += 65;
            factors.add("Hypertensive crisis level BP: " + record.getBloodPressureString());
        } else if (sys >= 140 || dia >= 90) {
            score += 45;
            factors.add("Stage 2 Hypertension: " + record.getBloodPressureString() + " (>= 140/90 mmHg)");
        } else if (sys >= 130 || dia >= 80) {
            score += 25;
            factors.add("Stage 1 Hypertension: " + record.getBloodPressureString() + " (130-139 / 80-89 mmHg)");
        } else if (sys >= 120 && dia < 80) {
            score += 12;
            factors.add("Elevated blood pressure: " + record.getBloodPressureString());
        }

        // Cholesterol
        double chol = record.getCholesterol();
        if (chol >= 240) {
            score += 35;
            factors.add("High serum cholesterol: " + chol + " mg/dL (>= 240 mg/dL)");
        } else if (chol >= 200) {
            score += 18;
            factors.add("Borderline high serum cholesterol: " + chol + " mg/dL (200-239 mg/dL)");
        }

        // Symptoms
        if (symptoms.contains("chest")) {
            score += 30;
            factors.add("Reported exertional chest discomfort / angina-like symptoms");
        }
        if (symptoms.contains("breath") || symptoms.contains("shortness")) {
            score += 20;
            factors.add("Reported dyspnea (shortness of breath)");
        }

        // Family history & Age
        if (famHist.contains("heart") || famHist.contains("coronary") || famHist.contains("cardio") || famHist.contains("hypertension")) {
            score += 15;
            factors.add("Positive family history of premature cardiovascular disease");
        }
        if (patient.getAge() >= 55) {
            score += 10;
            factors.add("Age > 55 cardiovascular risk factor (" + patient.getAge() + " years)");
        }

        return new ConditionScore("Cardiovascular Disease", score, factors);
    }

    public ConditionScore evaluateKidneyDisease(Patient patient, MedicalRecord record) {
        int score = 5;
        List<String> factors = new ArrayList<>();
        String symptoms = record.getSymptoms() != null ? record.getSymptoms().toLowerCase() : "";
        String famHist = record.getFamilyHistory() != null ? record.getFamilyHistory().toLowerCase() : "";

        // Serum creatinine
        double creat = record.getCreatinine();
        boolean isFemale = "female".equalsIgnoreCase(patient.getGender());
        double upperNormal = isFemale ? 1.1 : 1.3;

        if (creat >= 2.0) {
            score += 65;
            factors.add("Markedly elevated serum creatinine: " + creat + " mg/dL (Severe impairment)");
        } else if (creat >= 1.5) {
            score += 45;
            factors.add("Significantly elevated serum creatinine: " + creat + " mg/dL (Upper normal: " + upperNormal + " mg/dL)");
        } else if (creat > upperNormal) {
            score += 25;
            factors.add("Borderline elevated serum creatinine: " + creat + " mg/dL");
        }

        // Comorbidity multipliers: Hypertension & Diabetes
        if (record.getSystolicBp() >= 140 || record.getDiastolicBp() >= 90) {
            score += 15;
            factors.add("Hypertension comorbidity accelerating nephron degradation");
        }
        if (record.getGlucose() >= 126) {
            score += 15;
            factors.add("Diabetic comorbidity risk (diabetic nephropathy correlation)");
        }

        // Symptoms
        if (symptoms.contains("edema") || symptoms.contains("swelling") || symptoms.contains("feet") || symptoms.contains("legs")) {
            score += 20;
            factors.add("Peripheral edema / swelling (fluid retention indicator)");
        }
        if (symptoms.contains("puffiness")) {
            score += 15;
            factors.add("Facial or periorbital morning puffiness");
        }

        if (famHist.contains("kidney") || famHist.contains("renal")) {
            score += 15;
            factors.add("Positive family history of renal dysfunction");
        }

        return new ConditionScore("Chronic Kidney Disease", score, factors);
    }
}
