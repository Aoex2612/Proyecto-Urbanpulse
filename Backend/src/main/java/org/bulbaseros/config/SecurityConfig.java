package org.bulbaseros.config;

import org.bulbaseros.security.CustomAccessDeniedHandler;
import org.bulbaseros.security.JwtAuthenticationEntryPoint;
import org.bulbaseros.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de seguridad de la aplicación UrbanPulse utilizando Spring Security.
 *
 * <p>Esta clase define la cadena de filtros de seguridad HTTP sin estado (stateless),
 * la política de protección CSRF deshabilitada para APIs REST desacopladas,
 * las reglas de autorización de rutas públicas y protegidas con RBAC (Role-Based Access Control),
 * la gestión centralizada de excepciones de seguridad (401 Unauthorized y 403 Forbidden),
 * la integración perimetral del filtro {@link JwtAuthenticationFilter} antes de
 * {@link UsernamePasswordAuthenticationFilter}, la configuración CORS para permitir la
 * integración con el cliente frontend y el codificador de contraseñas mediante BCrypt.</p>
 *
 * @author UrbanPulse Team
 * @version 1.3
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * Constructor por defecto de la clase de configuración de seguridad.
     * <p>Inicializa las dependencias de seguridad con instancias predeterminadas para permitir
     * instanciación directa en contextos desacoplados de prueba.</p>
     */
    public SecurityConfig() {
        this(null, null, null);
    }

    /**
     * Constructor principal con inyección de dependencias para los componentes de autenticación,
     * autorización y manejo de excepciones de seguridad.
     *
     * @param jwtAuthenticationFilter    filtro perimetral para inspección y validación de tokens JWT.
     * @param jwtAuthenticationEntryPoint manejador de respuestas estructuradas para errores 401 Unauthorized.
     * @param customAccessDeniedHandler   manejador de respuestas estructuradas para errores 403 Forbidden.
     */
    @Autowired
    public SecurityConfig(
            @Autowired(required = false) JwtAuthenticationFilter jwtAuthenticationFilter,
            @Autowired(required = false) JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            @Autowired(required = false) CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.jwtAuthenticationFilter = (jwtAuthenticationFilter != null)
                ? jwtAuthenticationFilter
                : new JwtAuthenticationFilter();
        this.jwtAuthenticationEntryPoint = (jwtAuthenticationEntryPoint != null)
                ? jwtAuthenticationEntryPoint
                : new JwtAuthenticationEntryPoint();
        this.customAccessDeniedHandler = (customAccessDeniedHandler != null)
                ? customAccessDeniedHandler
                : new CustomAccessDeniedHandler();
    }

    /**
     * Configura la cadena de filtros de seguridad HTTP (SecurityFilterChain).
     *
     * <p>Aplica una arquitectura sin estado (stateless), desactiva CSRF, asocia la configuración
     * de CORS y define las siguientes reglas de autorización:</p>
     * <ul>
     *   <li>Rutas públicas {@code /api/auth/**} y {@code /h2-console/**}: permitidas sin autenticación ({@code permitAll()}).</li>
     *   <li>Consulta cronológica de auditoría {@code GET /api/incidents/{id}/history}: restringida a usuarios con rol {@code ADMIN}.</li>
     *   <li>Operaciones de triaje y estado {@code PATCH /api/incidents/{id}/status}: restringidas a usuarios con rol {@code OPERATOR} o {@code ADMIN}.</li>
     *   <li>Seguimiento de avisos propios {@code GET /api/incidents/my-incidents} y geocodificación inversa {@code GET /api/incidents/geocode/reverse}: requieren autenticación ({@code authenticated()}).</li>
     *   <li>Rutas generales de incidencias {@code /api/incidents/**}: requiere autenticación previa ({@code authenticated()}).</li>
     *   <li>Cualquier otra petición: requiere autenticación previa ({@code authenticated()}).</li>
     * </ul>
     * <p>Asimismo, asocia {@link JwtAuthenticationEntryPoint} para el manejo de fallos 401,
     * {@link CustomAccessDeniedHandler} para denegaciones de acceso 403, e inserta
     * {@link JwtAuthenticationFilter} antes de {@link UsernamePasswordAuthenticationFilter}.</p>
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
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/h2-console/**", "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/incidents/*/history").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/incidents/*/status").hasAnyRole("OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/incidents/my-incidents").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/incidents/geocode/reverse").authenticated()
                        .requestMatchers("/api/incidents/**").authenticated()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
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

    /**
     * Obtiene el filtro de autenticación JWT registrado en la configuración.
     *
     * @return instancia de {@link JwtAuthenticationFilter}.
     */
    public JwtAuthenticationFilter getJwtAuthenticationFilter() {
        return jwtAuthenticationFilter;
    }

    /**
     * Obtiene el manejador de punto de entrada para excepciones de autenticación (401).
     *
     * @return instancia de {@link JwtAuthenticationEntryPoint}.
     */
    public JwtAuthenticationEntryPoint getJwtAuthenticationEntryPoint() {
        return jwtAuthenticationEntryPoint;
    }

    /**
     * Obtiene el manejador de acceso denegado para excepciones de autorización (403).
     *
     * @return instancia de {@link CustomAccessDeniedHandler}.
     */
    public CustomAccessDeniedHandler getCustomAccessDeniedHandler() {
        return customAccessDeniedHandler;
    }
}
