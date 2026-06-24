package org.example.chat;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
@EnableScheduling
public class ChatApplication {
    private static final Logger logger = LoggerFactory.getLogger(ChatApplication.class);

    public static void main(String[] args) {
        // Load .env.local file if it exists — but only for local development.
        // In production (Render), environment variables are injected by the platform
        // and .env.local won't exist, so we skip the Dotenv library entirely to
        // avoid unnecessary filesystem scanning overhead on cold starts.
        String activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (activeProfile == null || activeProfile.isBlank() || !activeProfile.equals("prod")) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .filename(".env.local")
                        .ignoreIfMissing()
                        .load();
                
                // Set environment variables from .env.local
                dotenv.entries().forEach(entry -> {
                    System.setProperty(entry.getKey(), entry.getValue());
                    logger.debug("Loaded env var: {} = {}", entry.getKey(), 
                            entry.getKey().contains("KEY") || entry.getKey().contains("PASSWORD") 
                                    ? "***" : entry.getValue());
                });
                
                logger.info("Loaded environment variables from .env.local");
            } catch (Exception e) {
                logger.warn("Could not load .env.local file: {}", e.getMessage());
            }
        } else {
            logger.debug("Skipping .env.local loading (prod profile active)");
        }

        long start = System.currentTimeMillis();
        logger.info("=== Application startup initiated ===");
        SpringApplication.run(ChatApplication.class, args);
        long end = System.currentTimeMillis();
        logger.info("=== Application context loaded in {} ms ===", (end - start));
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
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
