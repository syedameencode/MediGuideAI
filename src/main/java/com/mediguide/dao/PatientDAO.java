package com.mediguide.dao;

import com.mediguide.database.DatabaseManager;
import com.mediguide.models.Patient;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PatientDAO {
    private static final Logger LOGGER = Logger.getLogger(PatientDAO.class.getName());
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public int create(Patient p) {
        String sql = "INSERT INTO patients (name, age, gender, phone, email, medical_history_notes, created_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getPhone());
            ps.setString(5, p.getEmail());
            ps.setString(6, p.getMedicalHistoryNotes());
            ps.setString(7, p.getCreatedDate());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    p.setPatientId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating patient", e);
        }
        return -1;
    }

    public boolean update(Patient p) {
        String sql = "UPDATE patients SET name = ?, age = ?, gender = ?, phone = ?, email = ?, medical_history_notes = ? WHERE patient_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getAge());
            ps.setString(3, p.getGender());
            ps.setString(4, p.getPhone());
            ps.setString(5, p.getEmail());
            ps.setString(6, p.getMedicalHistoryNotes());
            ps.setInt(7, p.getPatientId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating patient #" + p.getPatientId(), e);
        }
        return false;
    }

    public boolean delete(int patientId) {
        String sql = "DELETE FROM patients WHERE patient_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting patient #" + patientId, e);
        }
        return false;
    }

    public Patient findById(int patientId) {
        String sql = "SELECT * FROM patients WHERE patient_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPatient(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding patient #" + patientId, e);
        }
        return null;
    }

    public List<Patient> findAll() {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM patients ORDER BY patient_id DESC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapPatient(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching all patients", e);
        }
        return list;
    }

    public List<Patient> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        List<Patient> list = new ArrayList<>();
        int numericId = -1;
        try {
            numericId = Integer.parseInt(query.trim());
        } catch (NumberFormatException ignored) {}

        String sql = "SELECT * FROM patients WHERE LOWER(name) LIKE ? OR phone LIKE ? OR patient_id = ? ORDER BY patient_id DESC";
        String pattern = "%" + query.trim().toLowerCase() + "%";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, numericId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPatient(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching patients with query: " + query, e);
        }
        return list;
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM patients";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting patients", e);
        }
        return 0;
    }

    private Patient mapPatient(ResultSet rs) throws SQLException {
        return new Patient(
                rs.getInt("patient_id"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getString("gender"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("medical_history_notes"),
                rs.getString("created_date")
        );
    }
}
