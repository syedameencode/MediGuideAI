package com.mediguide.engine;

import com.mediguide.ml.WekaMLService;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;

import java.util.ArrayList;
import java.util.List;

/**
 * DecisionFusionService — Hybrid AI Decision Fusion Engine.
 *
 * Combines ML predictions with rule-based analysis using a transparent,
 * configurable weighted fusion strategy:
 *
 *   ML_WEIGHT (default 0.60) + RULE_WEIGHT (default 0.40) = Hybrid Result
 *
 * These weights are prototype system parameters — NOT clinically validated.
 * The fusion strategy is fully explainable and displayed to the user.
 */
public class DecisionFusionService {

    // Configurable fusion weights (prototype parameters)
    private static double mlWeight = 0.60;
    private static double ruleWeight = 0.40;

    public static double getMlWeight() { return mlWeight; }
    public static double getRuleWeight() { return ruleWeight; }
    public static void setMlWeight(double weight) {
        mlWeight = weight;
        ruleWeight = 1.0 - weight;
    }

    private final RuleEngine ruleEngine = new RuleEngine();
    private final ExplanationGenerator explanationGenerator = new ExplanationGenerator();
    private final WekaMLService mlService = WekaMLService.getInstance();

    /**
     * Perform hybrid decision fusion for a specific condition.
     * Returns ML prediction, rule prediction, and fused hybrid result.
     */
    public ConditionFusionResult fuseForCondition(String condition, Patient patient, MedicalRecord record) {
        ConditionFusionResult result = new ConditionFusionResult();
        result.conditionName = condition;

        // 1. Rule-based analysis
        RuleEngine.ConditionScore ruleScore;
        switch (condition) {
            case "Diabetes Mellitus":
                ruleScore = ruleEngine.evaluateDiabetes(patient, record);
                break;
            case "Cardiovascular Disease":
                ruleScore = ruleEngine.evaluateCardiovascular(patient, record);
                break;
            case "Chronic Kidney Disease":
                ruleScore = ruleEngine.evaluateKidneyDisease(patient, record);
                break;
            default:
                ruleScore = new RuleEngine.ConditionScore(condition, 5, new ArrayList<>());
        }

        result.ruleScore = ruleScore.score;
        result.ruleRiskLevel = RiskCalculator.scoreToRiskLevel(ruleScore.score);
        result.ruleFactors = ruleScore.factors;

        // 2. ML prediction (if available)
        boolean hasFamilyHistory = record.getFamilyHistory() != null &&
                !record.getFamilyHistory().equalsIgnoreCase("None");

        String[] mlResult = mlService.predict(
                condition, patient.getAge(), record.getBmi(), record.getGlucose(),
                record.getSystolicBp(), record.getDiastolicBp(),
                record.getCholesterol(), record.getCreatinine(),
                hasFamilyHistory, record.getSymptoms() != null ? record.getSymptoms() : ""
        );

        if (mlResult != null) {
            result.mlAvailable = true;
            result.mlPrediction = mlResult[0];
            result.mlConfidence = Double.parseDouble(mlResult[1]);
            result.mlAlgorithm = mlService.getBestAlgorithm(condition);
        } else {
            result.mlAvailable = false;
            result.mlPrediction = "Unavailable";
            result.mlConfidence = 0;
            result.mlAlgorithm = "N/A";
        }

        // 3. Hybrid decision fusion
        if (result.mlAvailable) {
            double mlNumeric = riskLevelToScore(result.mlPrediction);
            double ruleNumeric = ruleScore.score;

            // Weighted fusion
            double hybridScore = (mlNumeric * mlWeight) + (ruleNumeric * ruleWeight);
            result.hybridScore = hybridScore;
            result.hybridRiskLevel = RiskCalculator.scoreToRiskLevel((int) Math.round(hybridScore));
            result.fusionMethod = String.format("Weighted Hybrid (ML: %.0f%% + Rule: %.0f%%)",
                    mlWeight * 100, ruleWeight * 100);
        } else {
            // Fallback: rule-only
            result.hybridScore = ruleScore.score;
            result.hybridRiskLevel = result.ruleRiskLevel;
            result.fusionMethod = "Rule-Based Only (ML model unavailable)";
        }

        // 4. Confidence
        result.confidence = RiskCalculator.calculateConfidence(
                (int) Math.round(result.hybridScore), result.ruleFactors.size());

        return result;
    }

    /**
     * Perform full multi-condition hybrid analysis for a patient.
     */
    public RiskAssessmentResult assessPatientHybrid(Patient patient, MedicalRecord record) {
        RiskAssessmentResult assessment = new RiskAssessmentResult();

        String[] conditions = {"Diabetes Mellitus", "Cardiovascular Disease", "Chronic Kidney Disease"};
        ConditionFusionResult maxResult = null;

        for (String condition : conditions) {
            ConditionFusionResult fusion = fuseForCondition(condition, patient, record);

            // Build condition evaluation
            String summary;
            if (fusion.mlAvailable) {
                summary = String.format("ML: %s (%.0f%%) | Rules: %s (Score: %d) | Hybrid: %s",
                        fusion.mlPrediction, fusion.mlConfidence,
                        fusion.ruleRiskLevel, fusion.ruleScore,
                        fusion.hybridRiskLevel);
            } else {
                summary = String.format("Rules: %s (Score: %d) | ML: Unavailable",
                        fusion.ruleRiskLevel, fusion.ruleScore);
            }

            RiskAssessmentResult.ConditionEvaluation eval = new RiskAssessmentResult.ConditionEvaluation(
                    condition, fusion.hybridRiskLevel, (int) Math.round(fusion.hybridScore), summary
            );
            assessment.addConditionEvaluation(eval);

            if (maxResult == null || fusion.hybridScore > maxResult.hybridScore) {
                maxResult = fusion;
            }
        }

        if (maxResult != null) {
            assessment.setPrimaryCondition(maxResult.conditionName);
            assessment.setOverallRiskLevel(maxResult.hybridRiskLevel);
            assessment.setConfidenceScore(maxResult.confidence);
            assessment.setFusionMethod(maxResult.fusionMethod);
            assessment.setMlAvailable(maxResult.mlAvailable);
            assessment.setMlPrediction(maxResult.mlPrediction);
            assessment.setMlConfidence(maxResult.mlConfidence);
            assessment.setMlAlgorithm(maxResult.mlAlgorithm);
            assessment.setRuleRiskLevel(maxResult.ruleRiskLevel);
            assessment.setRuleScore(maxResult.ruleScore);

            // Contributing factors
            for (String factor : maxResult.ruleFactors) {
                assessment.addContributingFactor(factor);
            }
            if (assessment.getContributingFactors().isEmpty()) {
                assessment.addContributingFactor(
                        "All evaluated biomarkers within normal physiological baseline.");
            }

            // Alternatives and recommendations
            List<String> alternatives = explanationGenerator.generateAlternativeConditions(
                    maxResult.conditionName, record);
            for (String alt : alternatives) {
                assessment.addAlternativePossibility(alt);
            }

            List<String> recommendations = explanationGenerator.generateRecommendations(
                    maxResult.conditionName, maxResult.hybridRiskLevel, record);
            for (String rec : recommendations) {
                assessment.addRecommendation(rec);
            }

            // Also collect notable factors from other conditions
            for (String condition : conditions) {
                if (!condition.equals(maxResult.conditionName)) {
                    ConditionFusionResult other = fuseForCondition(condition, patient, record);
                    if (other.hybridScore >= 35) {
                        for (String factor : other.ruleFactors) {
                            if (!assessment.getContributingFactors().contains(factor)) {
                                assessment.addContributingFactor(factor);
                            }
                        }
                    }
                }
            }
        }

        return assessment;
    }

    /**
     * Convert risk level string to numeric score for fusion calculation.
     */
    private double riskLevelToScore(String riskLevel) {
        switch (riskLevel.toLowerCase()) {
            case "high": return 80;
            case "medium": return 50;
            case "low": return 20;
            default: return 30;
        }
    }

    /**
     * Stores intermediate fusion results for a single condition.
     */
    public static class ConditionFusionResult {
        public String conditionName;
        // ML
        public boolean mlAvailable;
        public String mlPrediction;
        public double mlConfidence;
        public String mlAlgorithm;
        // Rule
        public int ruleScore;
        public String ruleRiskLevel;
        public List<String> ruleFactors = new ArrayList<>();
        // Hybrid
        public double hybridScore;
        public String hybridRiskLevel;
        public String fusionMethod;
        public double confidence;
    }
}
