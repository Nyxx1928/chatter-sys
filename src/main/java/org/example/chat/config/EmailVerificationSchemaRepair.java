package org.example.chat.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Repairs auth/email-verification schema drift for databases created before
 * email verification fields were introduced.
 */
@Component
public class EmailVerificationSchemaRepair {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationSchemaRepair.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public EmailVerificationSchemaRepair(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void repairSchemaIfNeeded() {
        try (Connection connection = dataSource.getConnection()) {
            repairUsersTable(connection);
            repairVerificationTokensTable(connection);
        } catch (SQLException e) {
            logger.warn("Skipping email verification schema repair because metadata could not be inspected.", e);
        }
    }

    private void repairUsersTable(Connection connection) throws SQLException {
        if (!tableExists(connection, "users")) {
            return;
        }

        if (!columnExists(connection, "users", "email_verified")) {
            logger.info("Repairing users.email_verified column for legacy database.");
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN email_verified BOOLEAN");
        }

        jdbcTemplate.execute("UPDATE users SET email_verified = FALSE WHERE email_verified IS NULL");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL");
    }

    private void repairVerificationTokensTable(Connection connection) throws SQLException {
        if (!tableExists(connection, "verification_tokens")) {
            return;
        }

        if (!columnExists(connection, "verification_tokens", "used")) {
            logger.info("Repairing verification_tokens.used column for legacy database.");
            jdbcTemplate.execute("ALTER TABLE verification_tokens ADD COLUMN used BOOLEAN");
        }

        if (!columnExists(connection, "verification_tokens", "created_at")) {
            logger.info("Repairing verification_tokens.created_at column for legacy database.");
            jdbcTemplate.execute("ALTER TABLE verification_tokens ADD COLUMN created_at TIMESTAMP");
        }

        jdbcTemplate.execute("UPDATE verification_tokens SET used = FALSE WHERE used IS NULL");
        jdbcTemplate.execute("UPDATE verification_tokens SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
        jdbcTemplate.execute("ALTER TABLE verification_tokens ALTER COLUMN used SET DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE verification_tokens ALTER COLUMN used SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE verification_tokens ALTER COLUMN created_at SET NOT NULL");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();

        try (ResultSet resultSet = metadata.getTables(null, null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String currentTableName = resultSet.getString("TABLE_NAME");
                if (tableName.equalsIgnoreCase(currentTableName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String normalizedTableName = tableName.toLowerCase(Locale.ROOT);
        String normalizedColumnName = columnName.toLowerCase(Locale.ROOT);

        try (ResultSet resultSet = metadata.getColumns(null, null, normalizedTableName, normalizedColumnName)) {
            if (resultSet.next()) {
                return true;
            }
        }

        try (ResultSet resultSet = metadata.getColumns(null, null, tableName.toUpperCase(Locale.ROOT),
                columnName.toUpperCase(Locale.ROOT))) {
            return resultSet.next();
        }
    }
}
