package com.mediguide;

import com.mediguide.engine.DecisionFusionService;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DecisionFusionServiceTest {

    private DecisionFusionService fusionService;
    private Patient testPatient;
    private MedicalRecord highRiskRecord;

    @BeforeEach
    public void setUp() {
        fusionService = new DecisionFusionService();
        testPatient = new Patient(10, "John Doe", 58, "Male", "9876543210", "john@example.com", "Hypertension", "2026-09-30");

        highRiskRecord = new MedicalRecord();
        highRiskRecord.setPatientId(10);
        highRiskRecord.setSystolicBp(180);
        highRiskRecord.setDiastolicBp(110);
        highRiskRecord.setGlucose(210);
        highRiskRecord.setCholesterol(290);
        highRiskRecord.setCreatinine(2.4);
        highRiskRecord.setBmi(33.0);
        highRiskRecord.setFamilyHistory("Heart Disease");
        highRiskRecord.setSymptoms("Polyuria; Polydipsia; Blurred Vision");
    }

    @Test
    public void testHybridDecisionFusionHighRisk() {
        RiskAssessmentResult result = fusionService.assessPatientHybrid(testPatient, highRiskRecord);

        assertNotNull(result, "Risk assessment result should not be null");
        assertEquals("High", result.getOverallRiskLevel(), "Overall risk level for severe vitals should be High");
        assertTrue(result.getConfidenceScore() >= 50.0, "Confidence score should be >= 50%");
        assertNotNull(result.getPrimaryCondition(), "Primary condition should be identified");
        assertNotNull(result.getContributingFactors(), "Contributing factors list should not be empty");
        assertFalse(result.getContributingFactors().isEmpty(), "Should have risk factors");
    }

    @Test
    public void testWeightTuning() {
        DecisionFusionService.setMlWeight(0.7);
        assertEquals(0.7, DecisionFusionService.getMlWeight(), 0.001);
        assertEquals(0.3, DecisionFusionService.getRuleWeight(), 0.001);

        // Reset back
        DecisionFusionService.setMlWeight(0.6);
    }
}
