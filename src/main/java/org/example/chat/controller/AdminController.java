package org.example.chat.controller;

import org.example.chat.repository.PendingRegistrationRepository;
import org.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller for development/testing operations.
 * WARNING: This should be disabled in production!
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;

    @Value("${app.admin.enabled:false}")
    private boolean adminEnabled;

    public AdminController(
            UserRepository userRepository,
            PendingRegistrationRepository pendingRegistrationRepository) {
        this.userRepository = userRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
    }

    /**
     * Cleans up all unverified users and pending registrations.
     * Only works if app.admin.enabled=true
     */
    @DeleteMapping("/cleanup-test-data")
    @Transactional
    public ResponseEntity<?> cleanupTestData() {
        if (!adminEnabled) {
            logger.warn("Admin endpoint called but admin is disabled");
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Admin endpoints are disabled"));
        }

        logger.info("Cleaning up test data...");

        long unverifiedUsers = userRepository.countByEmailVerifiedFalse();
        long pendingRegistrations = pendingRegistrationRepository.count();

        pendingRegistrationRepository.deleteAll();
        userRepository.deleteUnverifiedUsersAndDependents();

        logger.info("Cleanup complete: {} unverified users deleted, {} pending registrations deleted",
                unverifiedUsers, pendingRegistrations);

        return ResponseEntity.ok(Map.of(
                "message", "Cleanup successful",
                "unverifiedUsersDeleted", unverifiedUsers,
                "pendingRegistrationsDeleted", pendingRegistrations
        ));
    }

    /**
     * Shows current test data status.
     */
    @GetMapping("/test-data-status")
    public ResponseEntity<?> getTestDataStatus() {
        if (!adminEnabled) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Admin endpoints are disabled"));
        }

        long totalUsers = userRepository.count();
        long verifiedUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEmailVerified()))
                .count();
        long unverifiedUsers = totalUsers - verifiedUsers;
        long pendingRegistrations = pendingRegistrationRepository.count();

        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "verifiedUsers", verifiedUsers,
                "unverifiedUsers", unverifiedUsers,
                "pendingRegistrations", pendingRegistrations
        ));
    }
}
