package com.mediguide.models;

public class Prediction {
    private int predictionId;
    private int patientId;
    private String conditionName;
    private String riskLevel;          // "Low", "Medium", "High"
    private double confidence;         // Percentage (e.g. 84.5)
    private String contributingFactors; // Semicolon-separated explanation factors
    private String alternativeConditions;
    private String predictionDate;

    // Hybrid Fusion tracking
    private String mlPrediction = "";
    private double mlConfidence = 0;
    private String rulePrediction = "";
    private int ruleScore = 0;
    private String fusionMethod = "Rule-Based Only";

    public Prediction() {}

    public Prediction(int predictionId, int patientId, String conditionName, String riskLevel,
                      double confidence, String contributingFactors, String alternativeConditions,
                      String predictionDate) {
        this.predictionId = predictionId;
        this.patientId = patientId;
        this.conditionName = conditionName;
        this.riskLevel = riskLevel;
        this.confidence = confidence;
        this.contributingFactors = contributingFactors;
        this.alternativeConditions = alternativeConditions;
        this.predictionDate = predictionDate;
    }

    public Prediction(int patientId, String conditionName, String riskLevel,
                      double confidence, String contributingFactors, String alternativeConditions,
                      String predictionDate) {
        this.patientId = patientId;
        this.conditionName = conditionName;
        this.riskLevel = riskLevel;
        this.confidence = confidence;
        this.contributingFactors = contributingFactors;
        this.alternativeConditions = alternativeConditions;
        this.predictionDate = predictionDate;
    }

    public int getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(int predictionId) {
        this.predictionId = predictionId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getContributingFactors() {
        return contributingFactors;
    }

    public void setContributingFactors(String contributingFactors) {
        this.contributingFactors = contributingFactors;
    }

    public String getAlternativeConditions() {
        return alternativeConditions;
    }

    public void setAlternativeConditions(String alternativeConditions) {
        this.alternativeConditions = alternativeConditions;
    }

    public String getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(String predictionDate) {
        this.predictionDate = predictionDate;
    }

    // Hybrid Fusion Accessors
    public String getMlPrediction() { return mlPrediction; }
    public void setMlPrediction(String mlPrediction) { this.mlPrediction = mlPrediction != null ? mlPrediction : ""; }

    public double getMlConfidence() { return mlConfidence; }
    public void setMlConfidence(double mlConfidence) { this.mlConfidence = mlConfidence; }

    public String getRulePrediction() { return rulePrediction; }
    public void setRulePrediction(String rulePrediction) { this.rulePrediction = rulePrediction != null ? rulePrediction : ""; }

    public int getRuleScore() { return ruleScore; }
    public void setRuleScore(int ruleScore) { this.ruleScore = ruleScore; }

    public String getFusionMethod() { return fusionMethod; }
    public void setFusionMethod(String fusionMethod) { this.fusionMethod = fusionMethod != null ? fusionMethod : ""; }
}
