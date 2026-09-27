package com.mediguide.models;

public class MedicalRecord {
    private int recordId;
    private int patientId;
    private String symptoms;
    private int systolicBp;
    private int diastolicBp;
    private double glucose;        // Fasting blood sugar (mg/dL)
    private double cholesterol;    // Total cholesterol (mg/dL)
    private double creatinine;     // Serum creatinine (mg/dL)
    private double bmi;            // Body Mass Index (kg/m²)
    private String familyHistory;  // e.g., "Diabetes, Heart Disease"
    private String recordedDate;

    public MedicalRecord() {}

    public MedicalRecord(int recordId, int patientId, String symptoms, int systolicBp, int diastolicBp,
                         double glucose, double cholesterol, double creatinine, double bmi,
                         String familyHistory, String recordedDate) {
        this.recordId = recordId;
        this.patientId = patientId;
        this.symptoms = symptoms;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.glucose = glucose;
        this.cholesterol = cholesterol;
        this.creatinine = creatinine;
        this.bmi = bmi;
        this.familyHistory = familyHistory;
        this.recordedDate = recordedDate;
    }

    public MedicalRecord(int patientId, String symptoms, int systolicBp, int diastolicBp,
                         double glucose, double cholesterol, double creatinine, double bmi,
                         String familyHistory, String recordedDate) {
        this.patientId = patientId;
        this.symptoms = symptoms;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.glucose = glucose;
        this.cholesterol = cholesterol;
        this.creatinine = creatinine;
        this.bmi = bmi;
        this.familyHistory = familyHistory;
        this.recordedDate = recordedDate;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public int getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(int systolicBp) {
        this.systolicBp = systolicBp;
    }

    public int getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(int diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public double getGlucose() {
        return glucose;
    }

    public void setGlucose(double glucose) {
        this.glucose = glucose;
    }

    public double getCholesterol() {
        return cholesterol;
    }

    public void setCholesterol(double cholesterol) {
        this.cholesterol = cholesterol;
    }

    public double getCreatinine() {
        return creatinine;
    }

    public void setCreatinine(double creatinine) {
        this.creatinine = creatinine;
    }

    public double getBmi() {
        return bmi;
    }

    public void setBmi(double bmi) {
        this.bmi = bmi;
    }

    public String getFamilyHistory() {
        return familyHistory;
    }

    public void setFamilyHistory(String familyHistory) {
        this.familyHistory = familyHistory;
    }

    public String getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(String recordedDate) {
        this.recordedDate = recordedDate;
    }

    public String getBloodPressureString() {
        return systolicBp + "/" + diastolicBp + " mmHg";
    }
}
