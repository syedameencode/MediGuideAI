package com.mediguide.models;

/**
 * Stores evaluation metrics for a trained Weka ML model.
 * Values come from actual model evaluation — never hardcoded.
 */
public class ModelEvaluationResult {
    private int resultId;
    private String algorithm;        // e.g. "Decision Tree", "Random Forest", "Naive Bayes"
    private String conditionName;    // e.g. "Diabetes Mellitus"
    private double accuracy;
    private double precision;
    private double recall;
    private double f1Score;
    private String trainingDate;

    public ModelEvaluationResult() {}

    public ModelEvaluationResult(String algorithm, String conditionName,
                                  double accuracy, double precision,
                                  double recall, double f1Score, String trainingDate) {
        this.algorithm = algorithm;
        this.conditionName = conditionName;
        this.accuracy = accuracy;
        this.precision = precision;
        this.recall = recall;
        this.f1Score = f1Score;
        this.trainingDate = trainingDate;
    }

    public int getResultId() { return resultId; }
    public void setResultId(int resultId) { this.resultId = resultId; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getConditionName() { return conditionName; }
    public void setConditionName(String conditionName) { this.conditionName = conditionName; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public double getPrecision() { return precision; }
    public void setPrecision(double precision) { this.precision = precision; }

    public double getRecall() { return recall; }
    public void setRecall(double recall) { this.recall = recall; }

    public double getF1Score() { return f1Score; }
    public void setF1Score(double f1Score) { this.f1Score = f1Score; }

    public String getTrainingDate() { return trainingDate; }
    public void setTrainingDate(String trainingDate) { this.trainingDate = trainingDate; }
}
