package org.bulbaseros.config;

import jakarta.servlet.Filter;
import org.bulbaseros.security.CustomAccessDeniedHandler;
import org.bulbaseros.security.JwtAuthenticationEntryPoint;
import org.bulbaseros.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración y unitarias para verificar la configuración de seguridad,
 * reglas de autorización RBAC, filtros perimetrales y CORS en {@link SecurityConfig}.
 *
 * @author UrbanPulse Team
 * @version 1.1
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private JwtAuthenticationFilter injectedJwtFilter;

    @Autowired
    private JwtAuthenticationEntryPoint injectedEntryPoint;

    @Autowired
    private CustomAccessDeniedHandler injectedAccessDeniedHandler;

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

    /**
     * Verifica que el constructor por defecto inicialice instancias no nulas de las dependencias de seguridad.
     */
    @Test
    @DisplayName("Debería inicializar dependencias por defecto al invocar constructor sin argumentos")
    void shouldInstantiateWithDefaultDependencies() {
        SecurityConfig config = new SecurityConfig();
        assertNotNull(config.getJwtAuthenticationFilter());
        assertNotNull(config.getJwtAuthenticationEntryPoint());
        assertNotNull(config.getCustomAccessDeniedHandler());
    }

    /**
     * Verifica que el constructor parametrizado asigne exactamente las dependencias suministradas.
     */
    @Test
    @DisplayName("Debería asignar dependencias explícitas al invocar constructor parametrizado")
    void shouldInstantiateWithExplicitDependencies() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();
        CustomAccessDeniedHandler accessDeniedHandler = new CustomAccessDeniedHandler();

        SecurityConfig config = new SecurityConfig(filter, entryPoint, accessDeniedHandler);
        assertSame(filter, config.getJwtAuthenticationFilter());
        assertSame(entryPoint, config.getJwtAuthenticationEntryPoint());
        assertSame(accessDeniedHandler, config.getCustomAccessDeniedHandler());
    }

    /**
     * Verifica que el SecurityFilterChain registre el filtro JwtAuthenticationFilter.
     */
    @Test
    @DisplayName("Debería contener el filtro JwtAuthenticationFilter registrado en la cadena de filtros")
    void shouldRegisterJwtAuthenticationFilterInFilterChain() {
        assertNotNull(securityFilterChain, "SecurityFilterChain no debe ser nulo");
        List<Filter> filters = securityFilterChain.getFilters();
        boolean containsJwtFilter = filters.stream().anyMatch(f -> f instanceof JwtAuthenticationFilter);
        assertTrue(containsJwtFilter, "SecurityFilterChain debe incluir una instancia de JwtAuthenticationFilter");
    }

    /**
     * Verifica que los beans inyectados en el contexto coincidan con los asociados en SecurityConfig.
     */
    @Test
    @DisplayName("Debería integrar los beans de seguridad inyectados por el contenedor de Spring")
    void shouldIntegrateInjectedSecurityBeans() {
        assertSame(injectedJwtFilter, securityConfig.getJwtAuthenticationFilter());
        assertSame(injectedEntryPoint, securityConfig.getJwtAuthenticationEntryPoint());
        assertSame(injectedAccessDeniedHandler, securityConfig.getCustomAccessDeniedHandler());
    }

    /**
     * Verifica que los endpoints de autenticación (/api/auth/**) y consola H2 sean públicos y no requieran token.
     */
    @Test
    @DisplayName("Rutas públicas /api/auth/** y /h2-console/** no deben requerir autenticación previa")
    void shouldAllowPublicAuthEndpoints() throws Exception {
        // Petición a /api/auth/register con payload vacío llega al controlador y retorna 400 (Bad Request), no 401
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Petición a /h2-console es permitida sin 401 ni 403
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    /**
     * Verifica que una petición no autenticada a PATCH /api/incidents/1/status responda 401 Unauthorized
     * con el DTO ErrorResponse estructurado.
     */
    @Test
    @DisplayName("Petición sin autenticación a PATCH /api/incidents/*/status debe responder 401 Unauthorized")
    void shouldRejectUnauthenticatedPatchIncidentStatus() throws Exception {
        mockMvc.perform(patch("/api/incidents/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token de autenticación ausente o inválido"))
                .andExpect(jsonPath("$.path").value("/api/incidents/100/status"));
    }

    /**
     * Verifica que un usuario con rol CITIZEN reciba 403 Forbidden estructurado al intentar
     * actualizar el estado de una incidencia en PATCH /api/incidents/{id}/status.
     */
    @Test
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("Usuario con rol CITIZEN en PATCH /api/incidents/*/status debe recibir 403 Forbidden")
    void shouldRejectCitizenRoleOnPatchIncidentStatus() throws Exception {
        mockMvc.perform(patch("/api/incidents/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: permisos insuficientes"))
                .andExpect(jsonPath("$.path").value("/api/incidents/100/status"));
    }

    /**
     * Verifica que un usuario con rol OPERATOR sea autorizado para PATCH /api/incidents/{id}/status (no 401 ni 403).
     */
    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Usuario con rol OPERATOR debe ser autorizado en PATCH /api/incidents/*/status")
    void shouldAuthorizeOperatorRoleOnPatchIncidentStatus() throws Exception {
        mockMvc.perform(patch("/api/incidents/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    /**
     * Verifica que un usuario con rol ADMIN sea autorizado para PATCH /api/incidents/{id}/status (no 401 ni 403).
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Usuario con rol ADMIN debe ser autorizado en PATCH /api/incidents/*/status")
    void shouldAuthorizeAdminRoleOnPatchIncidentStatus() throws Exception {
        mockMvc.perform(patch("/api/incidents/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }

    /**
     * Verifica que recursos no públicos requieran autenticación y respondan 401 si no se suministra token.
     */
    @Test
    @DisplayName("Cualquier otra ruta protegida no autenticada debe responder 401 Unauthorized")
    void shouldRejectUnauthenticatedAccessToGeneralProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    /**
     * Verifica que recursos generales autenticados no emitan 401 ni 403 para usuarios autenticados.
     */
    @Test
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("Cualquier otra ruta protegida con usuario autenticado supera la barrera perimetral")
    void shouldAllowAuthenticatedAccessToGeneralProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }
}
