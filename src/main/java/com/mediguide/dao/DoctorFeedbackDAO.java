package com.mediguide.dao;

import com.mediguide.database.DatabaseManager;
import com.mediguide.models.DoctorFeedback;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DoctorFeedbackDAO {
    private static final Logger LOGGER = Logger.getLogger(DoctorFeedbackDAO.class.getName());
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public int create(DoctorFeedback feedback) {
        String sql = "INSERT INTO doctor_feedback (prediction_id, doctor_id, doctor_name, doctor_decision, comments, feedback_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, feedback.getPredictionId());
            ps.setInt(2, feedback.getDoctorId());
            ps.setString(3, feedback.getDoctorName());
            ps.setString(4, feedback.getDoctorDecision());
            ps.setString(5, feedback.getComments());
            ps.setString(6, feedback.getFeedbackDate());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    feedback.setFeedbackId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating doctor feedback", e);
        }
        return -1;
    }

    public DoctorFeedback findByPredictionId(int predictionId) {
        String sql = "SELECT * FROM doctor_feedback WHERE prediction_id = ? ORDER BY feedback_id DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, predictionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFeedback(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding feedback for prediction #" + predictionId, e);
        }
        return null;
    }

    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM doctor_feedback";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting doctor feedback", e);
        }
        return 0;
    }

    private DoctorFeedback mapFeedback(ResultSet rs) throws SQLException {
        return new DoctorFeedback(
                rs.getInt("feedback_id"),
                rs.getInt("prediction_id"),
                rs.getInt("doctor_id"),
                rs.getString("doctor_name"),
                rs.getString("doctor_decision"),
                rs.getString("comments"),
                rs.getString("feedback_date")
        );
    }
}
