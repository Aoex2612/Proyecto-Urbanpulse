package org.bulbaseros.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bulbaseros.dto.IncidentStatusResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de seguridad y control de acceso RBAC para el controlador de incidencias {@link IncidentController}.
 * <p>
 * Verifica el cumplimiento estricto de las reglas perimetrales de autorización sobre el endpoint
 * de triaje {@code PATCH /api/incidents/{id}/status}:
 * <ul>
 *   <li>Petición no autenticada responde HTTP 401 Unauthorized con formato {@code ErrorResponse} (RF-1).</li>
 *   <li>Petición autenticada con rol {@code CITIZEN} responde HTTP 403 Forbidden con formato {@code ErrorResponse} (RF-5).</li>
 *   <li>Petición autenticada con rol {@code OPERATOR} responde HTTP 200 OK con confirmación {@code IncidentStatusResponse} (RF-6).</li>
 *   <li>Petición autenticada con rol {@code ADMIN} responde HTTP 200 OK con confirmación {@code IncidentStatusResponse} (RF-6).</li>
 * </ul>
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private IncidentController incidentController;

    /**
     * Verifica que una petición sin cabecera de autenticación responda HTTP 401 Unauthorized
     * serializando el DTO estructurado ErrorResponse.
     */
    @Test
    @DisplayName("Petición sin token a PATCH /api/incidents/{id}/status debe responder 401 Unauthorized")
    void shouldRejectUnauthenticatedRequestWith401() throws Exception {
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "Iniciando análisis");

        mockMvc.perform(patch("/api/incidents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token de autenticación ausente o inválido"))
                .andExpect(jsonPath("$.path").value("/api/incidents/1/status"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Verifica que un usuario autenticado con rol CITIZEN reciba HTTP 403 Forbidden
     * y el cuerpo estandarizado ErrorResponse al intentar triar una incidencia.
     */
    @Test
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("Petición con rol CITIZEN a PATCH /api/incidents/{id}/status debe responder 403 Forbidden")
    void shouldRejectCitizenUserWith403() throws Exception {
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.VALIDATED, "Intento no autorizado");

        mockMvc.perform(patch("/api/incidents/42/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: permisos insuficientes"))
                .andExpect(jsonPath("$.path").value("/api/incidents/42/status"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Verifica que un usuario autenticado con rol OPERATOR reciba HTTP 200 OK
     * y la confirmación en el DTO IncidentStatusResponse.
     */
    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Petición con rol OPERATOR a PATCH /api/incidents/{id}/status debe responder 200 OK")
    void shouldAuthorizeOperatorUserWith200() throws Exception {
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "En revisión operativa");

        mockMvc.perform(patch("/api/incidents/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.message").value("Estado de la incidencia actualizado exitosamente"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    /**
     * Verifica que un usuario autenticado con rol ADMIN reciba HTTP 200 OK
     * y la confirmación en el DTO IncidentStatusResponse.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Petición con rol ADMIN a PATCH /api/incidents/{id}/status debe responder 200 OK")
    void shouldAuthorizeAdminUserWith200() throws Exception {
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.RESOLVED, "Incidencia resuelta por admin");

        mockMvc.perform(patch("/api/incidents/200/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.message").value("Estado de la incidencia actualizado exitosamente"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    /**
     * Verifica la autorización mediante cabecera Bearer con un token JWT real generado para un CITIZEN.
     */
    @Test
    @DisplayName("Petición con token JWT real de rol CITIZEN debe responder 403 Forbidden")
    void shouldRejectRealJwtTokenWithCitizenRoleWith403() throws Exception {
        String token = jwtService.generateToken("citizen@urbanpulse.es", "CITIZEN");
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.VALIDATED);

        mockMvc.perform(patch("/api/incidents/10/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: permisos insuficientes"));
    }

    /**
     * Verifica la autorización mediante cabecera Bearer con un token JWT real generado para un OPERATOR.
     */
    @Test
    @DisplayName("Petición con token JWT real de rol OPERATOR debe responder 200 OK")
    void shouldAuthorizeRealJwtTokenWithOperatorRoleWith200() throws Exception {
        String token = jwtService.generateToken("operator@urbanpulse.es", "OPERATOR");
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.ASSIGNED);

        mockMvc.perform(patch("/api/incidents/10/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.message").value("Estado de la incidencia actualizado exitosamente"));
    }

    /**
     * Verifica la autorización mediante cabecera Bearer con un token JWT real generado para un ADMIN.
     */
    @Test
    @DisplayName("Petición con token JWT real de rol ADMIN debe responder 200 OK")
    void shouldAuthorizeRealJwtTokenWithAdminRoleWith200() throws Exception {
        String token = jwtService.generateToken("admin@urbanpulse.es", "ADMIN");
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.CLOSED);

        mockMvc.perform(patch("/api/incidents/55/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.message").value("Estado de la incidencia actualizado exitosamente"));
    }

    /**
     * Verifica que una petición de OPERATOR con payload inválido (estado nulo) responda HTTP 400 Bad Request.
     */
    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Petición con estado nulo debe retornar HTTP 400 Bad Request por validación Bean Validation")
    void shouldReturn400WhenStatusIsNull() throws Exception {
        mockMvc.perform(patch("/api/incidents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.status").value("El estado de la incidencia es obligatorio"));
    }

    /**
     * Verifica la ejecución unitaria directa del controlador sin pasar por la capa de red.
     */
    @Test
    @DisplayName("Invocación unitaria directa de IncidentController debe retornar 200 OK con cuerpo esperado")
    void shouldReturn200OnDirectControllerInvocation() {
        IncidentController controller = new IncidentController();
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest(EstadoIncidencia.RESOLVED, "Resuelto");

        ResponseEntity<IncidentStatusResponse> response = controller.updateIncidentStatus(77L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(77L, response.getBody().getId());
        assertEquals(EstadoIncidencia.RESOLVED, response.getBody().getStatus());
        assertEquals("Estado de la incidencia actualizado exitosamente", response.getBody().getMessage());
        assertNotNull(response.getBody().getUpdatedAt());
    }
}
