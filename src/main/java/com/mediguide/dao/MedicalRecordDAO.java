package com.mediguide.dao;

import com.mediguide.database.DatabaseManager;
import com.mediguide.models.MedicalRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MedicalRecordDAO {
    private static final Logger LOGGER = Logger.getLogger(MedicalRecordDAO.class.getName());
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public int create(MedicalRecord record) {
        String sql = "INSERT INTO medical_records (patient_id, symptoms, blood_pressure_systolic, blood_pressure_diastolic, glucose, cholesterol, creatinine, bmi, family_history, recorded_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getPatientId());
            ps.setString(2, record.getSymptoms());
            ps.setInt(3, record.getSystolicBp());
            ps.setInt(4, record.getDiastolicBp());
            ps.setDouble(5, record.getGlucose());
            ps.setDouble(6, record.getCholesterol());
            ps.setDouble(7, record.getCreatinine());
            ps.setDouble(8, record.getBmi());
            ps.setString(9, record.getFamilyHistory());
            ps.setString(10, record.getRecordedDate());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    record.setRecordId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting medical record", e);
        }
        return -1;
    }

    public List<MedicalRecord> findByPatientId(int patientId) {
        List<MedicalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM medical_records WHERE patient_id = ? ORDER BY record_id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRecord(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching medical records for patient #" + patientId, e);
        }
        return list;
    }

    public MedicalRecord findLatestByPatientId(int patientId) {
        String sql = "SELECT * FROM medical_records WHERE patient_id = ? ORDER BY record_id DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching latest medical record for patient #" + patientId, e);
        }
        return null;
    }

    private MedicalRecord mapRecord(ResultSet rs) throws SQLException {
        return new MedicalRecord(
                rs.getInt("record_id"),
                rs.getInt("patient_id"),
                rs.getString("symptoms"),
                rs.getInt("blood_pressure_systolic"),
                rs.getInt("blood_pressure_diastolic"),
                rs.getDouble("glucose"),
                rs.getDouble("cholesterol"),
                rs.getDouble("creatinine"),
                rs.getDouble("bmi"),
                rs.getString("family_history"),
                rs.getString("recorded_date")
        );
    }
}
