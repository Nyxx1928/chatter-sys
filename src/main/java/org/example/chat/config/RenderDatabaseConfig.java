package org.example.chat.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
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
                
                properties.setUrl(jdbcUrl);
                properties.setUsername(username);
                properties.setPassword(password);
                properties.setDriverClassName("org.postgresql.Driver");
                
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid DATABASE_URL format: " + databaseUrl, e);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing DATABASE_URL: " + databaseUrl, e);
            }
        }
        
        return properties;
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
