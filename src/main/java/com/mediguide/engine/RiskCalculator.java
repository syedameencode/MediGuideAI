package com.mediguide.engine;

public class RiskCalculator {

    public static String scoreToRiskLevel(int score) {
        if (score >= 65) {
            return "High";
        } else if (score >= 35) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    public static double calculateConfidence(int score, int evidenceFactorCount) {
        // Base confidence around 70-80% for AI/rule hybrid
        double base = 75.0;

        // More contributing factors increase certainty in clinical prediction
        double factorBonus = Math.min(18.0, evidenceFactorCount * 4.5);

        // Extreme scores (very high or very low) have higher certainty
        double extremityBonus = 0;
        if (score >= 75 || score <= 20) {
            extremityBonus = 5.0;
        } else if (score >= 65 || score <= 30) {
            extremityBonus = 2.5;
        }

        double confidence = base + factorBonus + extremityBonus;
        // Clamp between 68% and 96%
        return Math.min(96.0, Math.max(68.0, confidence));
    }
}
