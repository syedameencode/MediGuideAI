package com.mediguide;

import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;
import com.mediguide.util.PdfReportGenerator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class PdfReportGeneratorTest {

    @Test
    public void testGenerateReport() throws Exception {
        Patient patient = new Patient(99, "Jane Smith", 45, "Female", "5551234567", "jane@example.com", "Family history of diabetes", "2026-09-30");
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(99);
        record.setSystolicBp(130);
        record.setDiastolicBp(85);
        record.setGlucose(145);
        record.setCholesterol(190);
        record.setCreatinine(0.9);
        record.setBmi(28.4);
        record.setFamilyHistory("Diabetes");

        RiskAssessmentResult result = new RiskAssessmentResult();
        result.setPrimaryCondition("Type 2 Diabetes Mellitus");
        result.setOverallRiskLevel("Medium");
        result.setConfidenceScore(82.5);
        result.setFusionStrategy("Hybrid ML (60%) + Rule (40%)");
        result.setMlPrediction("Medium");
        result.setRulePrediction("Medium");
        result.setTopRiskFactors(Arrays.asList("Elevated Fasting Glucose: 145 mg/dL", "Overweight BMI: 28.4 kg/m²"));
        result.setRecommendations(Arrays.asList("Fasting blood glucose re-check in 14 days", "Dietary lifestyle modifications"));

        File pdf = PdfReportGenerator.generateReport(patient, record, result, "target/test-reports");

        assertNotNull(pdf, "Generated PDF file should not be null");
        assertTrue(pdf.exists(), "Generated PDF file must exist on disk");
        assertTrue(pdf.length() > 0, "Generated PDF file must not be empty");
    }
}
