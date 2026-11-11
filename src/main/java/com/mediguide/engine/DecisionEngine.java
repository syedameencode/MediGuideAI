package com.mediguide.engine;

import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;

/**
 * DecisionEngine — Orchestrates the hybrid AI decision pipeline.
 *
 * Routes patient assessment through the DecisionFusionService which
 * combines ML predictions with rule-based analysis for explainable results.
 *
 * Falls back to rule-only analysis when ML models are unavailable.
 */
public class DecisionEngine {
    private final DecisionFusionService fusionService = new DecisionFusionService();

    /**
     * Assess a patient using the hybrid ML + Rule decision fusion pipeline.
     * If ML is unavailable, the system degrades gracefully to rule-based only.
     */
    public RiskAssessmentResult assessPatient(Patient patient, MedicalRecord record) {
        return fusionService.assessPatientHybrid(patient, record);
    }
}
