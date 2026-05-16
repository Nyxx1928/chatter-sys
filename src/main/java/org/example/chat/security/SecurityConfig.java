package org.example.chat.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for the chat application.
 * Configures HTTP security, JWT authentication, and CORS settings.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Extra origins injected from the CORS_ALLOWED_ORIGINS environment variable.
     * Comma-separated list, e.g.:
     *   https://chatter-sys.vercel.app,https://abc123.ngrok-free.app
     *
     * Defaults to empty string (no extra origins) when the variable is not set.
     */
    @Value("${cors.allowed-origins:}")
    private String corsAllowedOriginsEnv;

    @Value("${app.admin.enabled:false}")
    private boolean adminEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the security filter chain.
     * - Permits authentication endpoints without authentication
     * - Requires authentication for all other endpoints
     * - Adds JWT authentication filter
     * - Configures CORS
     * - Enables CSRF protection for REST endpoints (excludes WebSocket which uses JWT)
     * - Sets session management to stateless
     * - Configures custom authentication entry point to return 401 instead of 403
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — JWT tokens in Authorization header are immune to CSRF
                .csrf(csrf -> csrf.disable())

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> {
                    var authRules = auth
                            // Permit authentication and verification endpoints
                            .requestMatchers("/api/auth/register", "/api/auth/login",
                                    "/api/auth/verify-email", "/api/auth/resend-verification").permitAll()

                            // Permit health checks (Render)
                            .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()

                            // Permit WebSocket endpoint (authentication handled by STOMP interceptor)
                            .requestMatchers("/ws/**").permitAll();

                    // Permit admin endpoints only if enabled (for development)
                    if (adminEnabled) {
                        authRules.requestMatchers("/api/admin/**").permitAll();
                    }

                    authRules.anyRequest().authenticated();
                })

                // Configure exception handling to return 401 for unauthenticated requests
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint()))

                // Set session management to stateless (no sessions, using JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Custom authentication entry point that returns 401 Unauthorized instead of
     * 403 Forbidden when authentication is required but not provided.
     *
     * @return the custom AuthenticationEntryPoint
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
        };
    }

    /**
     * Builds the list of allowed CORS origins.
     *
     * Always includes localhost:3000 for local development.
     * Additional origins are read from the CORS_ALLOWED_ORIGINS env var so that
     * production (Vercel) and temporary testing URLs (ngrok) can be added without
     * changing code.
     */
    List<String> buildAllowedOrigins() {
        List<String> origins = new ArrayList<>(List.of("http://localhost:3000"));
        if (corsAllowedOriginsEnv != null && !corsAllowedOriginsEnv.isBlank()) {
            Arrays.stream(corsAllowedOriginsEnv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(origins::add);
        }
        return origins;
    }

    /**
     * Configures CORS to allow requests from the frontend origin.
     * Allows all HTTP methods and headers for simplicity in this learning project.
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(buildAllowedOrigins());

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Expose Authorization header to frontend
        configuration.setExposedHeaders(List.of("Authorization"));

        // Apply CORS configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Provides the password encoder bean for password hashing.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides the authentication manager bean.
     *
     * @param authenticationConfiguration the authentication configuration
     * @return the authentication manager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
