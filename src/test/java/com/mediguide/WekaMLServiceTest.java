package com.mediguide;

import com.mediguide.ml.DatasetGenerator;
import com.mediguide.ml.WekaMLService;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.ModelEvaluationResult;
import com.mediguide.models.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WekaMLServiceTest {

    private static WekaMLService mlService;

    @BeforeAll
    public static void setUp() {
        DatasetGenerator.ensureDatasetsExist();
        mlService = WekaMLService.getInstance();
        mlService.initialize();
    }

    @Test
    public void testDatasetGeneration() {
        assertTrue(DatasetGenerator.getDatasetPath("Diabetes Mellitus") != null, "ARFF Datasets path should exist");
    }

    @Test
    public void testModelEvaluationResults() {
        List<ModelEvaluationResult> results = mlService.getAllEvaluationResults();
        assertNotNull(results, "Evaluation results should not be null");
        assertFalse(results.isEmpty(), "Evaluation results should contain entries for algorithms and conditions");

        for (ModelEvaluationResult res : results) {
            assertTrue(res.getAccuracy() >= 0 && res.getAccuracy() <= 100, "Accuracy must be percentage between 0 and 100");
            assertTrue(res.getPrecision() >= 0 && res.getPrecision() <= 100.0, "Precision must be between 0 and 100");
            assertTrue(res.getRecall() >= 0 && res.getRecall() <= 100.0, "Recall must be between 0 and 100");
            assertTrue(res.getF1Score() >= 0 && res.getF1Score() <= 100.0, "F1 score must be between 0 and 100");
        }
    }

    @Test
    public void testMLPredictionHighCardioRisk() {
        Patient patient = new Patient(1, "Test High Risk", 65, "Male", "1234567890", "test@hospital.org", "History of CAD", "2026-09-30");

        MedicalRecord record = new MedicalRecord();
        record.setPatientId(1);
        record.setSystolicBp(175);
        record.setDiastolicBp(105);
        record.setCholesterol(280);
        record.setGlucose(130);
        record.setCreatinine(1.1);
        record.setBmi(32.5);
        record.setFamilyHistory("Heart Disease");
        record.setSymptoms("Chest Pain; Shortness of Breath");

        String[] pred = mlService.predict("Cardiovascular Disease", patient.getAge(), record.getBmi(), record.getGlucose(),
                record.getSystolicBp(), record.getDiastolicBp(), record.getCholesterol(), record.getCreatinine(),
                true, record.getSymptoms());

        assertNotNull(pred, "Prediction array should not be null");
        assertEquals(2, pred.length, "Prediction array length should be 2");
        assertNotNull(pred[0], "Risk level should not be null");
        assertTrue(Double.parseDouble(pred[1]) > 0, "Confidence should be greater than 0");
    }
}
