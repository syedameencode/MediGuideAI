package com.mediguide;

import com.mediguide.engine.DecisionEngine;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DecisionEngineTest {

    private DecisionEngine engine;

    @BeforeEach
    public void setup() {
        engine = new DecisionEngine();
    }

    @Test
    public void testHighRiskDiabetesEvaluation() {
        Patient patient = new Patient(1, "Test Patient", 55, "Male", "123", "test@example.com", "Sedentary", "2026-01-01");
        // High glucose (175), high BMI (32), thirsty and frequent urination symptoms, diabetes family history
        MedicalRecord record = new MedicalRecord(1, 1, "Excessive thirst, Frequent urination, Fatigue",
                125, 80, 175.0, 190.0, 1.0, 32.0, "Diabetes", "2026-01-01");

        RiskAssessmentResult result = engine.assessPatient(patient, record);

        assertNotNull(result);
        assertEquals("Diabetes Mellitus", result.getPrimaryCondition());
        assertEquals("High", result.getOverallRiskLevel());
        assertTrue(result.getConfidenceScore() >= 80.0, "Confidence should be >= 80%");
        assertTrue(result.getContributingFactors().size() >= 3, "Should explain at least 3 factors");
        assertFalse(result.getAlternativePossibilities().isEmpty(), "Should provide alternative conditions");
    }

    @Test
    public void testHighRiskCardiovascularEvaluation() {
        Patient patient = new Patient(2, "Cardio Patient", 64, "Male", "456", "cardio@example.com", "Smoker", "2026-01-01");
        // High BP (165/102), High cholesterol (270), Chest pain symptoms
        MedicalRecord record = new MedicalRecord(2, 2, "Chest tightness, Shortness of breath",
                165, 102, 95.0, 270.0, 1.1, 27.5, "Heart Disease", "2026-01-01");

        RiskAssessmentResult result = engine.assessPatient(patient, record);

        assertNotNull(result);
        assertEquals("Cardiovascular Disease", result.getPrimaryCondition());
        assertEquals("High", result.getOverallRiskLevel());
        assertTrue(result.getContributingFactors().stream().anyMatch(f -> f.contains("Hypertension") || f.contains("cholesterol")));
    }

    @Test
    public void testLowRiskHealthyPatient() {
        Patient patient = new Patient(3, "Healthy Patient", 25, "Female", "789", "healthy@example.com", "Athlete", "2026-01-01");
        // Normal parameters: BP 115/75, Glucose 85, Chol 160, Creatinine 0.8, BMI 21.5
        MedicalRecord record = new MedicalRecord(3, 3, "None",
                115, 75, 85.0, 160.0, 0.8, 21.5, "None", "2026-01-01");

        RiskAssessmentResult result = engine.assessPatient(patient, record);

        assertNotNull(result);
        assertEquals("Low", result.getOverallRiskLevel());
    }
}
