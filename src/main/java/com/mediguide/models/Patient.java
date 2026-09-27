package com.mediguide.models;

public class Patient {
    private int patientId;
    private String name;
    private int age;
    private String gender;
    private String phone;
    private String email;
    private String medicalHistoryNotes;
    private String createdDate;

    public Patient() {}

    public Patient(int patientId, String name, int age, String gender, String phone, String email, String medicalHistoryNotes, String createdDate) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.medicalHistoryNotes = medicalHistoryNotes;
        this.createdDate = createdDate;
    }

    public Patient(String name, int age, String gender, String phone, String email, String medicalHistoryNotes, String createdDate) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.medicalHistoryNotes = medicalHistoryNotes;
        this.createdDate = createdDate;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMedicalHistoryNotes() {
        return medicalHistoryNotes;
    }

    public void setMedicalHistoryNotes(String medicalHistoryNotes) {
        this.medicalHistoryNotes = medicalHistoryNotes;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String toString() {
        return String.format("#%03d - %s (%d yrs, %s)", patientId, name, age, gender);
    }
}
