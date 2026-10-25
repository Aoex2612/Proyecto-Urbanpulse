package org.bulbaseros.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.IncidentStatusResponse;
import org.bulbaseros.dto.StatusChangeResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.security.JwtService;
import org.bulbaseros.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de seguridad, contratos REST y control de acceso RBAC para {@link IncidentController}.
 * <p>
 * Verifica el cumplimiento estricto de las reglas perimetrales de autorizacion sobre todos los endpoints
 * del recurso de incidencias y auditoria transversal:
 * <ul>
 *   <li>Peticiones no autenticadas (ANONYMOUS) responden HTTP 401 Unauthorized con formato {@code ErrorResponse}.</li>
 *   <li>Peticiones de {@code CITIZEN} a endpoints operativos o de auditoria responden HTTP 403 Forbidden.</li>
 *   <li>Peticiones de {@code OPERATOR} a cambio de estado responden HTTP 200 OK, pero a auditoria responden 403 Forbidden.</li>
 *   <li>Peticiones de {@code ADMIN} responden HTTP 200 OK tanto a cambios de estado como al historial de auditoria.</li>
 *   <li>Peticiones sobre incidencias inexistentes en historial de auditoria responden HTTP 404 Not Found.</li>
 *   <li>Peticiones autenticadas de alta de incidencia responden HTTP 201 Created.</li>
 *   <li>Peticiones autenticadas de lectura de incidencias activas responden HTTP 200 OK.</li>
 * </ul>
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentControllerSecurityTest {

    /**
     * Cliente de pruebas MockMvc para ejecutar peticiones HTTP simuladas contra el contexto web.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Serializador y deserializador JSON para componer y validar cuerpos de peticion y respuesta.
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Servicio de generacion y validacion de tokens JWT para pruebas con cabeceras Bearer reales.
     */
    @Autowired
    private JwtService jwtService;

    /**
     * Mock del servicio de incidencias para aislar la capa web y de seguridad de la base de datos relacional.
     */
    @MockitoBean
    private IncidentService incidentService;

    /**
     * Instancia inyectada del controlador para pruebas de invocacion unitaria directa.
     */
    @Autowired
    private IncidentController incidentController;

    /**
     * Configuracion de comportamiento inicial de los metodos simulados de {@link IncidentService}.
     */
    @BeforeEach
    void setUp() {
        IncidentResponse sampleIncident = new IncidentResponse(
                1L,
                "Bache en calle Larios",
                "Pavimento deteriorado",
                "Vias Publicas",
                36.7213,
                -4.4214,
                EstadoIncidencia.REPORTED,
                PrioridadIncidencia.MEDIUM,
                10L,
                "Ciudadano Ejemplar",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(incidentService.getActiveIncidents()).thenReturn(List.of(sampleIncident));
        when(incidentService.getIncidentById(1L)).thenReturn(sampleIncident);
        when(incidentService.createIncident(any(CreateIncidentRequest.class), any()))
                .thenReturn(sampleIncident);

        when(incidentService.updateIncidentStatus(anyLong(), any(UpdateIncidentStatusRequest.class), any()))
                .thenAnswer(invocation -> {
                    Long id = invocation.getArgument(0);
                    UpdateIncidentStatusRequest req = invocation.getArgument(1);
                    return new IncidentStatusResponse(
                            id,
                            req.getStatus(),
                            "Estado de la incidencia actualizado exitosamente",
                            OffsetDateTime.now()
                    );
                });

        StatusChangeResponse change = new StatusChangeResponse(
                1L,
                1L,
                "REPORTED",
                "IN_PROGRESS",
                2L,
                "Operador Central",
                "operator@urbanpulse.es",
                "Revision inicial",
                OffsetDateTime.now()
        );
        when(incidentService.getIncidentHistory(1L)).thenReturn(List.of(change));
        when(incidentService.getIncidentHistory(999L))
                .thenThrow(new IncidentNotFoundException("Incidencia no encontrada con id: 999"));
    }

    /**
     * Verifica que una peticion anonima a GET /api/incidents responda HTTP 401 Unauthorized
     * serializando el DTO estructurado ErrorResponse.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @DisplayName("Peticion anonima a GET /api/incidents debe responder 401 Unauthorized")
    void shouldRejectAnonymousGetIncidentsWith401() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token de autenticación ausente o inválido"))
                .andExpect(jsonPath("$.path").value("/api/incidents"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * Verifica que una peticion anonima a POST /api/incidents responda HTTP 401 Unauthorized.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @DisplayName("Peticion anonima a POST /api/incidents debe responder 401 Unauthorized")
    void shouldRejectAnonymousPostIncidentsWith401() throws Exception {
        CreateIncidentRequest payload = new CreateIncidentRequest(
                "Farola rota", "Sin iluminacion", "Alumbrado", 36.72, -4.42
        );

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token de autenticación ausente o inválido"))
                .andExpect(jsonPath("$.path").value("/api/incidents"));
    }

    /**
     * Verifica que una peticion anonima a GET /api/incidents/{id}/history responda HTTP 401 Unauthorized.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @DisplayName("Peticion anonima a GET /api/incidents/{id}/history debe responder 401 Unauthorized")
    void shouldRejectAnonymousGetIncidentHistoryWith401() throws Exception {
        mockMvc.perform(get("/api/incidents/1/history"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Token de autenticación ausente o inválido"))
                .andExpect(jsonPath("$.path").value("/api/incidents/1/history"));
    }

    /**
     * Verifica que una peticion sin cabecera de autenticacion a PATCH responda HTTP 401 Unauthorized
     * serializando el DTO estructurado ErrorResponse.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @DisplayName("Peticion sin token a PATCH /api/incidents/{id}/status debe responder 401 Unauthorized")
    void shouldRejectUnauthenticatedRequestWith401() throws Exception {
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "Iniciando analisis");

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
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("Peticion con rol CITIZEN a PATCH /api/incidents/{id}/status debe responder 403 Forbidden")
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
     * y la confirmacion en el DTO IncidentStatusResponse al actualizar estado.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Peticion con rol OPERATOR a PATCH /api/incidents/{id}/status debe responder 200 OK")
    void shouldAuthorizeOperatorUserWith200() throws Exception {
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "En revision operativa");

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
     * y la confirmacion en el DTO IncidentStatusResponse al actualizar estado.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Peticion con rol ADMIN a PATCH /api/incidents/{id}/status debe responder 200 OK")
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
     * Verifica que un usuario con rol TECHNICIAN reciba HTTP 403 Forbidden al intentar actualizar estado.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("Peticion con rol TECHNICIAN a PATCH /api/incidents/{id}/status debe responder 403 Forbidden")
    void shouldRejectTechnicianUserWith403OnPatchStatus() throws Exception {
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "Tecnico no autorizado");

        mockMvc.perform(patch("/api/incidents/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: permisos insuficientes"));
    }

    /**
     * Verifica que un usuario con rol ANALYST reciba HTTP 403 Forbidden al intentar actualizar estado.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("Peticion con rol ANALYST a PATCH /api/incidents/{id}/status debe responder 403 Forbidden")
    void shouldRejectAnalystUserWith403OnPatchStatus() throws Exception {
        UpdateIncidentStatusRequest payload = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "Analista no autorizado");

        mockMvc.perform(patch("/api/incidents/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: permisos insuficientes"));
    }

    /**
     * Verifica que un usuario con rol CITIZEN reciba HTTP 403 Forbidden al consultar el historial de auditoria.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("Peticion con rol CITIZEN a GET /api/incidents/{id}/history debe responder 403 Forbidden")
    void shouldRejectCitizenUserWith403OnGetHistory() throws Exception {
        mockMvc.perform(get("/api/incidents/1/history"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: permisos insuficientes"))
                .andExpect(jsonPath("$.path").value("/api/incidents/1/history"));
    }

    /**
     * Verifica que un usuario con rol OPERATOR reciba HTTP 403 Forbidden al consultar el historial de auditoria.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Peticion con rol OPERATOR a GET /api/incidents/{id}/history debe responder 403 Forbidden")
    void shouldRejectOperatorUserWith403OnGetHistory() throws Exception {
        mockMvc.perform(get("/api/incidents/1/history"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: permisos insuficientes"))
                .andExpect(jsonPath("$.path").value("/api/incidents/1/history"));
    }

    /**
     * Verifica que un usuario con rol TECHNICIAN reciba HTTP 403 Forbidden al consultar el historial de auditoria.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("Peticion con rol TECHNICIAN a GET /api/incidents/{id}/history debe responder 403 Forbidden")
    void shouldRejectTechnicianUserWith403OnGetHistory() throws Exception {
        mockMvc.perform(get("/api/incidents/1/history"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    /**
     * Verifica que un usuario con rol ANALYST reciba HTTP 403 Forbidden al consultar el historial de auditoria.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("Peticion con rol ANALYST a GET /api/incidents/{id}/history debe responder 403 Forbidden")
    void shouldRejectAnalystUserWith403OnGetHistory() throws Exception {
        mockMvc.perform(get("/api/incidents/1/history"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    /**
     * Verifica que un usuario con rol ADMIN reciba HTTP 200 OK y la lista de transiciones de auditoria.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Peticion con rol ADMIN a GET /api/incidents/{id}/history debe responder 200 OK")
    void shouldAuthorizeAdminUserWith200OnGetHistory() throws Exception {
        mockMvc.perform(get("/api/incidents/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].incidentId").value(1))
                .andExpect(jsonPath("$[0].fromStatus").value("REPORTED"))
                .andExpect(jsonPath("$[0].toStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].changedByUserName").value("Operador Central"))
                .andExpect(jsonPath("$[0].reason").value("Revision inicial"));
    }

    /**
     * Verifica que un usuario con rol ADMIN al consultar una incidencia inexistente reciba HTTP 404 Not Found.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Peticion con rol ADMIN a GET /api/incidents/{id}/history sobre id inexistente responde 404 Not Found")
    void shouldReturn404WhenAdminRequestsHistoryForNonExistentIncident() throws Exception {
        mockMvc.perform(get("/api/incidents/999/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada con id: 999"))
                .andExpect(jsonPath("$.path").value("/api/incidents/999/history"));
    }

    /**
     * Verifica que un usuario con rol CITIZEN pueda registrar una nueva incidencia respondiendo HTTP 201 Created.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("Peticion con rol CITIZEN a POST /api/incidents responde 201 Created")
    void shouldAuthorizeCitizenRoleOnPostIncidentsWith201() throws Exception {
        CreateIncidentRequest payload = new CreateIncidentRequest(
                "Farola fundida",
                "Falta de iluminacion en la via publica",
                "Alumbrado",
                36.7213,
                -4.4214
        );

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Bache en calle Larios"))
                .andExpect(jsonPath("$.category").value("Vias Publicas"))
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    /**
     * Verifica que un usuario con rol CITIZEN pueda consultar las incidencias activas respondiendo HTTP 200 OK.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("Peticion con rol CITIZEN a GET /api/incidents responde 200 OK")
    void shouldAuthorizeCitizenRoleOnGetIncidentsWith200() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Bache en calle Larios"))
                .andExpect(jsonPath("$[0].category").value("Vias Publicas"));
    }

    /**
     * Verifica que un usuario con rol CITIZEN pueda consultar el detalle de una incidencia respondiendo HTTP 200 OK.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "CITIZEN")
    @DisplayName("Peticion con rol CITIZEN a GET /api/incidents/{id} responde 200 OK")
    void shouldAuthorizeCitizenRoleOnGetIncidentByIdWith200() throws Exception {
        mockMvc.perform(get("/api/incidents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Bache en calle Larios"));
    }

    /**
     * Verifica la autorizacion mediante cabecera Bearer con un token JWT real generado para un CITIZEN.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @DisplayName("Peticion con token JWT real de rol CITIZEN debe responder 403 Forbidden en PATCH")
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
     * Verifica la autorizacion mediante cabecera Bearer con un token JWT real generado para un OPERATOR.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @DisplayName("Peticion con token JWT real de rol OPERATOR debe responder 200 OK en PATCH")
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
     * Verifica la autorizacion mediante cabecera Bearer con un token JWT real generado para un ADMIN.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @DisplayName("Peticion con token JWT real de rol ADMIN debe responder 200 OK en PATCH y GET history")
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

        mockMvc.perform(get("/api/incidents/1/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    /**
     * Verifica que una peticion de OPERATOR con payload invalido (estado nulo) responda HTTP 400 Bad Request.
     *
     * @throws Exception si ocurre un fallo durante la ejecucion de la peticion.
     */
    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("Peticion con estado nulo debe retornar HTTP 400 Bad Request por validacion Bean Validation")
    void shouldReturn400WhenStatusIsNull() throws Exception {
        mockMvc.perform(patch("/api/incidents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.status").value("El estado de la incidencia es obligatorio"));
    }

    /**
     * Verifica la ejecucion unitaria directa del controlador sin pasar por la capa de red.
     */
    @Test
    @DisplayName("Invocacion unitaria directa de IncidentController debe retornar 200 OK con cuerpo esperado")
    void shouldReturn200OnDirectControllerInvocation() {
        IncidentService mockService = mock(IncidentService.class);
        IncidentController controller = new IncidentController(mockService);
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest(EstadoIncidencia.RESOLVED, "Resuelto");
        IncidentStatusResponse expectedResponse = new IncidentStatusResponse(
                77L,
                EstadoIncidencia.RESOLVED,
                "Estado de la incidencia actualizado exitosamente",
                OffsetDateTime.now()
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("operator@urbanpulse.es");
        when(mockService.updateIncidentStatus(eq(77L), eq(request), eq("operator@urbanpulse.es")))
                .thenReturn(expectedResponse);

        ResponseEntity<IncidentStatusResponse> response = controller.updateIncidentStatus(77L, request, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(77L, response.getBody().getId());
        assertEquals(EstadoIncidencia.RESOLVED, response.getBody().getStatus());
        assertEquals("Estado de la incidencia actualizado exitosamente", response.getBody().getMessage());
        assertNotNull(response.getBody().getUpdatedAt());
    }
}
