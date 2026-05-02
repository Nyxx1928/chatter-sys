package org.example.chat.config;

import org.example.chat.security.CustomUserDetailsService;
import org.example.chat.security.JwtAuthenticationFilter;
import org.example.chat.security.JwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for @WebMvcTest slice tests.
 * Provides mock beans for security components that are required by SecurityConfig
 * but not automatically included in the web layer test slice.
 * 
 * This configuration solves the issue where @WebMvcTest tries to load SecurityConfig,
 * which depends on JwtAuthenticationFilter, which in turn depends on JwtUtil and
 * CustomUserDetailsService. Since these are not web layer components, they need to
 * be explicitly provided as mocks for the test context.
 * 
 * Usage: Import this configuration in @WebMvcTest classes:
 * <pre>
 * @WebMvcTest(controllers = YourController.class)
 * @Import(WebMvcTestConfig.class)
 * class YourControllerTest {
 *     // test methods
 * }
 * </pre>
 */
@TestConfiguration
public class WebMvcTestConfig {

    /**
     * Provides a mock JwtUtil bean for tests.
     * The @Primary annotation ensures this bean takes precedence if there are multiple candidates.
     * 
     * @return a mock JwtUtil instance
     */
    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return mock(JwtUtil.class);
    }

    /**
     * Provides a mock CustomUserDetailsService bean for tests.
     * The @Primary annotation ensures this bean takes precedence if there are multiple candidates.
     * 
     * @return a mock CustomUserDetailsService instance
     */
    @Bean
    @Primary
    public CustomUserDetailsService customUserDetailsService() {
        return mock(CustomUserDetailsService.class);
    }

    /**
     * Provides a mock JwtAuthenticationFilter bean for tests.
     * The @Primary annotation ensures this bean takes precedence if there are multiple candidates.
     * 
     * @return a mock JwtAuthenticationFilter instance
     */
    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return mock(JwtAuthenticationFilter.class);
    }
}
