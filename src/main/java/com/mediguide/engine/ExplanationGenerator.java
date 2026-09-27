package com.mediguide.engine;

import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;

import java.util.ArrayList;
import java.util.List;

public class ExplanationGenerator {

    public List<String> generateAlternativeConditions(String primaryCondition, MedicalRecord record) {
        List<String> alternatives = new ArrayList<>();

        if ("Diabetes Mellitus".equalsIgnoreCase(primaryCondition)) {
            alternatives.add("Metabolic Syndrome (impaired fasting glucose with dyslipidemia)");
            alternatives.add("Thyroid-related metabolic dysfunction (hyperthyroidism/hypothyroidism)");
            if (record.getSystolicBp() >= 130) {
                alternatives.add("Concurrent Hypertensive Cardiovascular Stress");
            }
        } else if ("Cardiovascular Disease".equalsIgnoreCase(primaryCondition)) {
            alternatives.add("Secondary Hypertension (renovascular etiology)");
            alternatives.add("Dyslipidemia with early atheroma plaque formation");
            alternatives.add("Anxiety or panic-associated transient cardiovascular distress");
        } else if ("Chronic Kidney Disease".equalsIgnoreCase(primaryCondition)) {
            alternatives.add("Hypertensive Nephrosclerosis");
            alternatives.add("Prerenal azotemia (dehydration or diuretic-induced)");
            alternatives.add("Diabetic Microalbuminuria / Early Glomerular Sclerosis");
        } else {
            alternatives.add("General fatigue / lifestyle and dietary stress factors");
            alternatives.add("Seasonal or non-pathological vital fluctuations");
        }

        return alternatives;
    }

    public List<String> generateRecommendations(String primaryCondition, String riskLevel, MedicalRecord record) {
        List<String> recs = new ArrayList<>();

        if ("High".equalsIgnoreCase(riskLevel)) {
            recs.add("Immediate clinical physician review recommended within 24-48 hours.");
            if ("Diabetes Mellitus".equalsIgnoreCase(primaryCondition)) {
                recs.add("Order confirmatory HbA1c laboratory test and 2-hour Oral Glucose Tolerance Test (OGTT).");
                recs.add("Initiate medical nutrition therapy and blood glucose home-monitoring protocol.");
            } else if ("Cardiovascular Disease".equalsIgnoreCase(primaryCondition)) {
                recs.add("Obtain a 12-lead ECG and complete fasting lipid subfraction profile.");
                recs.add("Urgent cardiology assessment and anti-hypertensive regimen review.");
            } else if ("Chronic Kidney Disease".equalsIgnoreCase(primaryCondition)) {
                recs.add("Quantify estimated GFR (eGFR) and order spot urine albumin-to-creatinine ratio (ACR).");
                recs.add("Schedule renal ultrasonography and nephrology consultation.");
            }
        } else if ("Medium".equalsIgnoreCase(riskLevel)) {
            recs.add("Schedule routine follow-up evaluation in 2 to 4 weeks.");
            recs.add("Reinforce lifestyle modifications: low sodium/sugar dietary guidance and aerobic exercise.");
            recs.add("Repeat basic metabolic panel at next clinical visit.");
        } else {
            recs.add("Continue routine annual wellness exams and preventive healthcare.");
            recs.add("Maintain balanced physical activity and balanced nutritional habits.");
        }

        return recs;
    }
}
