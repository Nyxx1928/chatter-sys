package org.example.chat.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuration for Render.com database connection.
 * Render provides DATABASE_URL in format: postgresql://user:password@host:port/database
 * Spring Boot needs: jdbc:postgresql://host:port/database
 * This configuration handles the conversion.
 */
@Configuration
@Profile("prod")
public class RenderDatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        // If DATABASE_URL is not set, return null to use default configuration
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            return null;
        }

        try {
            // Parse the DATABASE_URL
            URI dbUri = new URI(databaseUrl);
            
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String host = dbUri.getHost();
            int port = dbUri.getPort();
            String database = dbUri.getPath().substring(1); // Remove leading '/'
            
            // Construct JDBC URL
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            
            // Build DataSource
            return DataSourceBuilder
                    .create()
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();
                    
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid DATABASE_URL format", e);
        }
    }
}
