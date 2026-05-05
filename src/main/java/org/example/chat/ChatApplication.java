package org.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
public class ChatApplication {
    private static final Logger logger = LoggerFactory.getLogger(ChatApplication.class);

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        logger.info("=== Application startup initiated ===");
        SpringApplication.run(ChatApplication.class, args);
        long end = System.currentTimeMillis();
        logger.info("=== Application context loaded in {} ms ===", (end - start));
    }

    @Bean
    public CommandLineRunner startupDiagnostics(DataSource dataSource) {
        return args -> {
            logger.info("=== Running custom startup diagnostics ===");
            try (Connection conn = dataSource.getConnection()) {
                logger.info("Database connection successful: {}", conn.getMetaData().getURL());
            } catch (SQLException e) {
                logger.error("Database connection failed", e);
            }
        };
    }
}
