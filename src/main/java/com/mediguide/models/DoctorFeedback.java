package com.mediguide.models;

public class DoctorFeedback {
    private int feedbackId;
    private int predictionId;
    private int doctorId;
    private String doctorName;
    private String doctorDecision; // "Accept", "Modify", "Reject"
    private String comments;
    private String feedbackDate;

    public DoctorFeedback() {}

    public DoctorFeedback(int feedbackId, int predictionId, int doctorId, String doctorName,
                          String doctorDecision, String comments, String feedbackDate) {
        this.feedbackId = feedbackId;
        this.predictionId = predictionId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.doctorDecision = doctorDecision;
        this.comments = comments;
        this.feedbackDate = feedbackDate;
    }

    public DoctorFeedback(int predictionId, int doctorId, String doctorName,
                          String doctorDecision, String comments, String feedbackDate) {
        this.predictionId = predictionId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.doctorDecision = doctorDecision;
        this.comments = comments;
        this.feedbackDate = feedbackDate;
    }

    public int getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(int feedbackId) {
        this.feedbackId = feedbackId;
    }

    public int getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(int predictionId) {
        this.predictionId = predictionId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorDecision() {
        return doctorDecision;
    }

    public void setDoctorDecision(String doctorDecision) {
        this.doctorDecision = doctorDecision;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getFeedbackDate() {
        return feedbackDate;
    }

    public void setFeedbackDate(String feedbackDate) {
        this.feedbackDate = feedbackDate;
    }
}
