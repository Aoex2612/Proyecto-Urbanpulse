package org.bulbaseros.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para verificar la configuración de seguridad y CORS en {@link SecurityConfig}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    /**
     * Verifica que el bean de codificación de contraseñas sea provisto y funcione con BCrypt.
     */
    @Test
    @DisplayName("Debería codificar y verificar contraseñas correctamente usando BCrypt")
    void shouldEncodeAndMatchPasswordsWithBCrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder, "El codificador de contraseñas no debe ser nulo");

        String rawPassword = "SecurePassword123";
        String encodedPassword = encoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertTrue(encoder.matches(rawPassword, encodedPassword), "La contraseña original debe coincidir con el hash");
    }

    /**
     * Verifica que la configuración CORS autorice el origen frontend y los métodos y cabeceras requeridos.
     */
    @Test
    @DisplayName("Debería configurar CORS con origen localhost:5173, métodos y cabeceras adecuados")
    void shouldConfigureCorsCorrectly() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source, "La fuente de configuración CORS no debe ser nula");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config, "La configuración CORS no debe ser nula para la ruta evaluada");

        assertEquals(List.of("http://localhost:5173"), config.getAllowedOrigins());
        assertTrue(config.getAllowedMethods().containsAll(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")));
        assertTrue(config.getAllowedHeaders().containsAll(List.of("Authorization", "Content-Type")));
        assertEquals(Boolean.TRUE, config.getAllowCredentials());
    }
}
