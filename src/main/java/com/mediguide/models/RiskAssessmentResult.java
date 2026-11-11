package com.mediguide.models;

import java.util.ArrayList;
import java.util.List;

public class RiskAssessmentResult {
    public static class ConditionEvaluation {
        private String conditionName;
        private String riskLevel; // "Low", "Medium", "High"
        private int score;        // 0 to 100
        private String summary;

        public ConditionEvaluation(String conditionName, String riskLevel, int score, String summary) {
            this.conditionName = conditionName;
            this.riskLevel = riskLevel;
            this.score = score;
            this.summary = summary;
        }

        public String getConditionName() {
            return conditionName;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public int getScore() {
            return score;
        }

        public String getSummary() {
            return summary;
        }
    }

    private String primaryCondition;
    private String overallRiskLevel; // "Low", "Medium", "High"
    private double confidenceScore;  // Percentage (e.g. 84.5)
    private List<ConditionEvaluation> conditionEvaluations = new ArrayList<>();
    private List<String> contributingFactors = new ArrayList<>();
    private List<String> alternativePossibilities = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private String disclaimer = "Notice: This system is an educational and research clinical decision-support prototype. " +
            "It does not replace professional medical evaluation, laboratory confirmation, or doctor judgment.";

    // Hybrid Fusion fields
    private boolean mlAvailable;
    private String mlPrediction = "N/A";
    private double mlConfidence;
    private String mlAlgorithm = "N/A";
    private String ruleRiskLevel = "N/A";
    private int ruleScore;
    private String fusionMethod = "Rule-Based Only";

    public RiskAssessmentResult() {}

    public String getPrimaryCondition() {
        return primaryCondition;
    }

    public void setPrimaryCondition(String primaryCondition) {
        this.primaryCondition = primaryCondition;
    }

    public String getOverallRiskLevel() {
        return overallRiskLevel;
    }

    public void setOverallRiskLevel(String overallRiskLevel) {
        this.overallRiskLevel = overallRiskLevel;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public List<ConditionEvaluation> getConditionEvaluations() {
        return conditionEvaluations;
    }

    public void addConditionEvaluation(ConditionEvaluation eval) {
        this.conditionEvaluations.add(eval);
    }

    public List<String> getContributingFactors() {
        return contributingFactors;
    }

    public void addContributingFactor(String factor) {
        this.contributingFactors.add(factor);
    }

    public List<String> getAlternativePossibilities() {
        return alternativePossibilities;
    }

    public void addAlternativePossibility(String alt) {
        this.alternativePossibilities.add(alt);
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void addRecommendation(String rec) {
        this.recommendations.add(rec);
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    // ML / Hybrid Fusion Accessors
    public boolean isMlAvailable() { return mlAvailable; }
    public void setMlAvailable(boolean mlAvailable) { this.mlAvailable = mlAvailable; }

    public String getMlPrediction() { return mlPrediction; }
    public void setMlPrediction(String mlPrediction) { this.mlPrediction = mlPrediction; }

    public double getMlConfidence() { return mlConfidence; }
    public void setMlConfidence(double mlConfidence) { this.mlConfidence = mlConfidence; }

    public String getMlAlgorithm() { return mlAlgorithm; }
    public void setMlAlgorithm(String mlAlgorithm) { this.mlAlgorithm = mlAlgorithm; }

    public String getRuleRiskLevel() { return ruleRiskLevel; }
    public void setRuleRiskLevel(String ruleRiskLevel) { this.ruleRiskLevel = ruleRiskLevel; }

    public int getRuleScore() { return ruleScore; }
    public void setRuleScore(int ruleScore) { this.ruleScore = ruleScore; }

    public String getFusionMethod() { return fusionMethod; }
    public void setFusionMethod(String fusionMethod) { this.fusionMethod = fusionMethod; }

    // Alias accessors for backward/PDF compatibility
    public List<String> getTopRiskFactors() { return contributingFactors; }
    public void setTopRiskFactors(List<String> list) { this.contributingFactors = list != null ? list : new ArrayList<>(); }

    public List<String> getExplanations() { return contributingFactors; }

    public void setContributingFactors(List<String> list) { this.contributingFactors = list != null ? list : new ArrayList<>(); }
    public void setRecommendations(List<String> list) { this.recommendations = list != null ? list : new ArrayList<>(); }

    public String getFusionStrategy() { return fusionMethod; }
    public void setFusionStrategy(String fusionStrategy) { this.fusionMethod = fusionStrategy; }

    public String getRulePrediction() { return ruleRiskLevel; }
    public void setRulePrediction(String rulePrediction) { this.ruleRiskLevel = rulePrediction; }
}
