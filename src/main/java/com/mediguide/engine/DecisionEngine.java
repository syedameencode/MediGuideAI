package com.mediguide.engine;

import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;

import java.util.ArrayList;
import java.util.List;

public class DecisionEngine {
    private final RuleEngine ruleEngine = new RuleEngine();
    private final ExplanationGenerator explanationGenerator = new ExplanationGenerator();

    public RiskAssessmentResult assessPatient(Patient patient, MedicalRecord record) {
        RiskAssessmentResult result = new RiskAssessmentResult();

        // 1. Evaluate individual clinical domains
        RuleEngine.ConditionScore diabetes = ruleEngine.evaluateDiabetes(patient, record);
        RuleEngine.ConditionScore cardio = ruleEngine.evaluateCardiovascular(patient, record);
        RuleEngine.ConditionScore kidney = ruleEngine.evaluateKidneyDisease(patient, record);

        String dRisk = RiskCalculator.scoreToRiskLevel(diabetes.score);
        String cRisk = RiskCalculator.scoreToRiskLevel(cardio.score);
        String kRisk = RiskCalculator.scoreToRiskLevel(kidney.score);

        result.addConditionEvaluation(new RiskAssessmentResult.ConditionEvaluation(
                "Diabetes Mellitus", dRisk, diabetes.score,
                "Fasting Glucose: " + record.getGlucose() + " mg/dL | BMI: " + String.format("%.1f", record.getBmi())));

        result.addConditionEvaluation(new RiskAssessmentResult.ConditionEvaluation(
                "Cardiovascular Disease", cRisk, cardio.score,
                "Blood Pressure: " + record.getBloodPressureString() + " | Cholesterol: " + record.getCholesterol() + " mg/dL"));

        result.addConditionEvaluation(new RiskAssessmentResult.ConditionEvaluation(
                "Chronic Kidney Disease", kRisk, kidney.score,
                "Serum Creatinine: " + record.getCreatinine() + " mg/dL"));

        // 2. Determine Primary Threat Condition (Highest Score)
        RuleEngine.ConditionScore maxCondition = diabetes;
        if (cardio.score > maxCondition.score) {
            maxCondition = cardio;
        }
        if (kidney.score > maxCondition.score) {
            maxCondition = kidney;
        }

        result.setPrimaryCondition(maxCondition.conditionName);
        result.setOverallRiskLevel(RiskCalculator.scoreToRiskLevel(maxCondition.score));

        // 3. Collect all relevant contributing factors across conditions
        List<String> allFactors = new ArrayList<>(maxCondition.factors);
        // Include notable factors from other conditions if any
        if (maxCondition != diabetes && diabetes.score >= 35) {
            allFactors.addAll(diabetes.factors);
        }
        if (maxCondition != cardio && cardio.score >= 35) {
            allFactors.addAll(cardio.factors);
        }
        if (maxCondition != kidney && kidney.score >= 35) {
            allFactors.addAll(kidney.factors);
        }

        if (allFactors.isEmpty()) {
            allFactors.add("All evaluated biomarkers (Glucose, Blood Pressure, Cholesterol, Creatinine) within normal physiological baseline.");
        }

        for (String factor : allFactors) {
            result.addContributingFactor(factor);
        }

        // 4. Calculate calibrated confidence score
        double confidence = RiskCalculator.calculateConfidence(maxCondition.score, allFactors.size());
        result.setConfidenceScore(confidence);

        // 5. Generate transparent alternatives & clinical recommendations
        List<String> alternatives = explanationGenerator.generateAlternativeConditions(maxCondition.conditionName, record);
        for (String alt : alternatives) {
            result.addAlternativePossibility(alt);
        }

        List<String> recommendations = explanationGenerator.generateRecommendations(
                maxCondition.conditionName, result.getOverallRiskLevel(), record);
        for (String rec : recommendations) {
            result.addRecommendation(rec);
        }

        return result;
    }
}
