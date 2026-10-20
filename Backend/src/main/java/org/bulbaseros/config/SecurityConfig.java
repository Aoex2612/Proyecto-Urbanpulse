package org.bulbaseros.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de seguridad de la aplicación UrbanPulse utilizando Spring Security.
 *
 * <p>Esta clase define la cadena de filtros de seguridad HTTP sin estado (stateless),
 * la política de protección CSRF deshabilitada para APIs REST desacopladas,
 * las reglas de autorización de rutas públicas y protegidas, la configuración CORS
 * para permitir la integración con el cliente frontend y el codificador de contraseñas
 * mediante BCrypt.</p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Constructor por defecto de la clase de configuración de seguridad.
     */
    public SecurityConfig() {
    }

    /**
     * Configura la cadena de filtros de seguridad HTTP (SecurityFilterChain).
     *
     * <p>Aplica una arquitectura sin estado (stateless), desactiva CSRF, asocia la configuración
     * de CORS y define que los endpoints bajo {@code /api/auth/**} sean accesibles sin autenticación,
     * mientras que cualquier otra petición requiera autenticación previa.</p>
     *
     * @param http instancia de {@link HttpSecurity} para configurar la seguridad web.
     * @return la cadena de filtros de seguridad construida {@link SecurityFilterChain}.
     * @throws Exception si ocurre un error durante la configuración de seguridad HTTP.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * Define la configuración de CORS (Cross-Origin Resource Sharing) permitiendo peticiones
     * desde el cliente frontend de desarrollo.
     *
     * <p>Permite el origen {@code http://localhost:5173}, los métodos HTTP GET, POST, PUT, DELETE y OPTIONS,
     * y las cabeceras requeridas como {@code Authorization} y {@code Content-Type}.</p>
     *
     * @return fuente de configuración CORS basada en URLs {@link CorsConfigurationSource}.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Proporciona el codificador de contraseñas para hashing seguro mediante BCrypt.
     *
     * @return instancia de {@link PasswordEncoder} configurada con {@link BCryptPasswordEncoder}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
