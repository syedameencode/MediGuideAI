package com.mediguide;

import com.mediguide.dao.PatientDAO;
import com.mediguide.dao.UserDAO;
import com.mediguide.database.DatabaseManager;
import com.mediguide.models.Patient;
import com.mediguide.models.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {

    @BeforeAll
    public static void init() {
        DatabaseManager.getInstance();
    }

    @Test
    public void testAdminUserAuthentication() {
        UserDAO userDAO = new UserDAO();
        User admin = userDAO.authenticate("admin", "admin123");
        assertNotNull(admin, "Admin user should exist and authenticate");
        assertEquals("ADMIN", admin.getRole());
        assertTrue(admin.isAdmin());

        User doctor = userDAO.authenticate("doctor", "doctor123");
        assertNotNull(doctor, "Doctor user should exist and authenticate");
        assertEquals("DOCTOR", doctor.getRole());
    }

    @Test
    public void testPatientDaoCrud() {
        PatientDAO dao = new PatientDAO();
        int initialCount = dao.count();
        assertTrue(initialCount > 0, "Seed patients should be populated");

        // Create new patient
        Patient testPatient = new Patient("Test John Doe", 40, "Male", "+1 (555) 999-8888", "john.doe@test.com", "No prior records", "2026-03-21 12:00");
        int newId = dao.create(testPatient);
        assertTrue(newId > 0, "Patient ID should be generated");

        // Fetch back
        Patient fetched = dao.findById(newId);
        assertNotNull(fetched);
        assertEquals("Test John Doe", fetched.getName());
        assertEquals(40, fetched.getAge());

        // Update
        fetched.setName("Test John Doe Updated");
        boolean updated = dao.update(fetched);
        assertTrue(updated);
        Patient reFetched = dao.findById(newId);
        assertEquals("Test John Doe Updated", reFetched.getName());

        // Search
        List<Patient> searchResults = dao.search("Updated");
        assertFalse(searchResults.isEmpty());

        // Cleanup
        boolean deleted = dao.delete(newId);
        assertTrue(deleted);
        assertNull(dao.findById(newId));
    }
}
