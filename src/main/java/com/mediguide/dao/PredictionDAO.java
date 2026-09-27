package com.mediguide.dao;

import com.mediguide.database.DatabaseManager;
import com.mediguide.models.Prediction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PredictionDAO {
    private static final Logger LOGGER = Logger.getLogger(PredictionDAO.class.getName());
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public int create(Prediction p) {
        String sql = "INSERT INTO predictions (patient_id, condition_name, risk_level, confidence, contributing_factors, alternative_conditions, prediction_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getPatientId());
            ps.setString(2, p.getConditionName());
            ps.setString(3, p.getRiskLevel());
            ps.setDouble(4, p.getConfidence());
            ps.setString(5, p.getContributingFactors());
            ps.setString(6, p.getAlternativeConditions());
            ps.setString(7, p.getPredictionDate());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    p.setPredictionId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving prediction", e);
        }
        return -1;
    }

    public List<Prediction> findByPatientId(int patientId) {
        List<Prediction> list = new ArrayList<>();
        String sql = "SELECT * FROM predictions WHERE patient_id = ? ORDER BY prediction_id DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPrediction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching predictions for patient #" + patientId, e);
        }
        return list;
    }

    public Prediction findLatestByPatientId(int patientId) {
        String sql = "SELECT * FROM predictions WHERE patient_id = ? ORDER BY prediction_id DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPrediction(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching latest prediction for patient #" + patientId, e);
        }
        return null;
    }

    public List<Prediction> findRecent(int limit) {
        List<Prediction> list = new ArrayList<>();
        String sql = "SELECT * FROM predictions ORDER BY prediction_id DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPrediction(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching recent predictions", e);
        }
        return list;
    }

    public int countHighRisk() {
        String sql = "SELECT COUNT(*) FROM predictions WHERE LOWER(risk_level) = 'high'";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting high risk predictions", e);
        }
        return 0;
    }

    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM predictions";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting predictions", e);
        }
        return 0;
    }

    private Prediction mapPrediction(ResultSet rs) throws SQLException {
        return new Prediction(
                rs.getInt("prediction_id"),
                rs.getInt("patient_id"),
                rs.getString("condition_name"),
                rs.getString("risk_level"),
                rs.getDouble("confidence"),
                rs.getString("contributing_factors"),
                rs.getString("alternative_conditions"),
                rs.getString("prediction_date")
        );
    }
}
