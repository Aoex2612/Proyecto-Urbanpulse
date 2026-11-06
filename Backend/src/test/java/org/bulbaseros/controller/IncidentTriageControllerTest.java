package org.bulbaseros.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bulbaseros.dto.AssignIncidentRequest;
import org.bulbaseros.dto.AssignmentResponse;
import org.bulbaseros.dto.CommentResponse;
import org.bulbaseros.dto.CreateCommentRequest;
import org.bulbaseros.dto.DeactivateAssignmentRequest;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.PriorityCalculationRequest;
import org.bulbaseros.dto.PriorityCalculationResponse;
import org.bulbaseros.dto.ReevaluateIncidentRequest;
import org.bulbaseros.dto.RejectIncidentRequest;
import org.bulbaseros.dto.ReopenIncidentRequest;
import org.bulbaseros.dto.ResolveIncidentRequest;
import org.bulbaseros.dto.SafetyRiskLevel;
import org.bulbaseros.dto.UpdateCommentRequest;
import org.bulbaseros.dto.UpdatePriorityRequest;
import org.bulbaseros.dto.ValidateIncidentRequest;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.service.AssignmentService;
import org.bulbaseros.service.CommentService;
import org.bulbaseros.service.IncidentTriageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración para {@link IncidentTriageController} verificando autorización RBAC,
 * validación de beans (@Valid) y respuestas HTTP estándar con MockMvc.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentTriageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentTriageService incidentTriageService;

    @MockitoBean
    private AssignmentService assignmentService;

    @MockitoBean
    private CommentService commentService;

    // -------------------------------------------------------------------------
    // 1. CÁLCULO ASISTIDO DE PRIORIDAD
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/incidents/{id}/priority/calculate sin token debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnCalculatePriority() throws Exception {
        PriorityCalculationRequest request = new PriorityCalculationRequest(true, SafetyRiskLevel.HIGH);

        mockMvc.perform(post("/api/incidents/1/priority/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("POST /api/incidents/{id}/priority/calculate con rol CITIZEN debe retornar 403 Forbidden")
    void shouldReturn403WhenCitizenOnCalculatePriority() throws Exception {
        PriorityCalculationRequest request = new PriorityCalculationRequest(true, SafetyRiskLevel.HIGH);

        mockMvc.perform(post("/api/incidents/1/priority/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/priority/calculate con campos inválidos debe retornar 400 Bad Request")
    void shouldReturn400WhenInvalidBodyOnCalculatePriority() throws Exception {
        PriorityCalculationRequest invalidRequest = new PriorityCalculationRequest(null, null);

        mockMvc.perform(post("/api/incidents/1/priority/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/priority/calculate con rol OPERATOR debe retornar 200 OK y cálculo")
    void shouldReturn200WhenOperatorOnCalculatePriority() throws Exception {
        PriorityCalculationRequest request = new PriorityCalculationRequest(true, SafetyRiskLevel.HIGH);
        PriorityCalculationResponse response = new PriorityCalculationResponse(
                PrioridadIncidencia.CRITICAL, "Corte de vía y riesgo alto detectado"
        );

        when(incidentTriageService.calculateAssistedPriority(eq(1L), any(), eq("operator@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/priority/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedPriority").value("CRITICAL"))
                .andExpect(jsonPath("$.priorityReason").value("Corte de vía y riesgo alto detectado"));
    }

    // -------------------------------------------------------------------------
    // 2. ACTUALIZACIÓN DEFINITIVA DE PRIORIDAD
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /api/incidents/{id}/priority sin token debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnUpdatePriority() throws Exception {
        UpdatePriorityRequest request = new UpdatePriorityRequest(PrioridadIncidencia.HIGH, "Prioridad asignada");

        mockMvc.perform(put("/api/incidents/1/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("PUT /api/incidents/{id}/priority con rol CITIZEN debe retornar 403 Forbidden")
    void shouldReturn403WhenCitizenOnUpdatePriority() throws Exception {
        UpdatePriorityRequest request = new UpdatePriorityRequest(PrioridadIncidencia.HIGH, "Prioridad asignada");

        mockMvc.perform(put("/api/incidents/1/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("PUT /api/incidents/{id}/priority con prioridad nula debe retornar 400 Bad Request")
    void shouldReturn400WhenPriorityNullOnUpdatePriority() throws Exception {
        UpdatePriorityRequest invalidRequest = new UpdatePriorityRequest(null, "Motivo sin prioridad");

        mockMvc.perform(put("/api/incidents/1/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@urbanpulse.es", roles = "ADMIN")
    @DisplayName("PUT /api/incidents/{id}/priority con rol ADMIN debe retornar 200 OK")
    void shouldReturn200WhenAdminOnUpdatePriority() throws Exception {
        UpdatePriorityRequest request = new UpdatePriorityRequest(PrioridadIncidencia.HIGH, "Prioridad asignada por admin");
        IncidentResponse response = new IncidentResponse();
        response.setId(1L);
        response.setPriority(PrioridadIncidencia.HIGH);

        when(incidentTriageService.updatePriority(eq(1L), any(), eq("admin@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(put("/api/incidents/1/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    // -------------------------------------------------------------------------
    // 3. VALIDACIÓN DE INCIDENCIA
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/incidents/{id}/validate sin autenticación debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnValidate() throws Exception {
        mockMvc.perform(post("/api/incidents/1/validate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("POST /api/incidents/{id}/validate con rol CITIZEN debe retornar 403 Forbidden")
    void shouldReturn403WhenCitizenOnValidate() throws Exception {
        mockMvc.perform(post("/api/incidents/1/validate"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/validate con rol OPERATOR debe retornar 200 OK")
    void shouldReturn200WhenOperatorOnValidate() throws Exception {
        ValidateIncidentRequest request = new ValidateIncidentRequest("Comprobación visual correcta");
        IncidentResponse response = new IncidentResponse();
        response.setId(1L);
        response.setStatus(EstadoIncidencia.VALIDATED);

        when(incidentTriageService.validateIncident(eq(1L), any(), eq("operator@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    // -------------------------------------------------------------------------
    // 4. RECHAZO DE INCIDENCIA
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/incidents/{id}/reject sin autenticación debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnReject() throws Exception {
        RejectIncidentRequest request = new RejectIncidentRequest("Incidencia duplicada");

        mockMvc.perform(post("/api/incidents/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("POST /api/incidents/{id}/reject con rol CITIZEN debe retornar 403 Forbidden")
    void shouldReturn403WhenCitizenOnReject() throws Exception {
        RejectIncidentRequest request = new RejectIncidentRequest("Incidencia duplicada");

        mockMvc.perform(post("/api/incidents/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/reject con motivo menor a 5 caracteres debe retornar 400 Bad Request")
    void shouldReturn400WhenRejectReasonTooShort() throws Exception {
        RejectIncidentRequest invalidRequest = new RejectIncidentRequest("No");

        mockMvc.perform(post("/api/incidents/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/reject con rol OPERATOR debe retornar 200 OK")
    void shouldReturn200WhenOperatorOnReject() throws Exception {
        RejectIncidentRequest request = new RejectIncidentRequest("Incidencia duplicada o fuera de término");
        IncidentResponse response = new IncidentResponse();
        response.setId(1L);
        response.setStatus(EstadoIncidencia.REJECTED);

        when(incidentTriageService.rejectIncident(eq(1L), any(), eq("operator@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // -------------------------------------------------------------------------
    // 5. REEVALUACIÓN DE INCIDENCIA RECHAZADA
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/reevaluate con motivo corto debe retornar 400 Bad Request")
    void shouldReturn400WhenReevaluateReasonTooShort() throws Exception {
        ReevaluateIncidentRequest invalidRequest = new ReevaluateIncidentRequest(EstadoIncidencia.VALIDATED, "Re");

        mockMvc.perform(post("/api/incidents/1/reevaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/reevaluate con rol OPERATOR debe retornar 200 OK")
    void shouldReturn200WhenOperatorOnReevaluate() throws Exception {
        ReevaluateIncidentRequest request = new ReevaluateIncidentRequest(EstadoIncidencia.VALIDATED, "Nuevas pruebas aportadas");
        IncidentResponse response = new IncidentResponse();
        response.setId(1L);
        response.setStatus(EstadoIncidencia.VALIDATED);

        when(incidentTriageService.reevaluateIncident(eq(1L), any(), eq("operator@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/reevaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    // -------------------------------------------------------------------------
    // 6. INICIO DE TRABAJO TÉCNICO
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/incidents/{id}/start-work sin autenticación debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnStartWork() throws Exception {
        mockMvc.perform(post("/api/incidents/1/start-work"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/start-work con rol OPERATOR debe retornar 403 Forbidden")
    void shouldReturn403WhenOperatorOnStartWork() throws Exception {
        mockMvc.perform(post("/api/incidents/1/start-work"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "tech@urbanpulse.es", roles = "TECHNICIAN")
    @DisplayName("POST /api/incidents/{id}/start-work con rol TECHNICIAN debe retornar 200 OK")
    void shouldReturn200WhenTechnicianOnStartWork() throws Exception {
        IncidentResponse response = new IncidentResponse();
        response.setId(1L);
        response.setStatus(EstadoIncidencia.IN_PROGRESS);

        when(incidentTriageService.startIntervention(eq(1L), eq("tech@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/start-work"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // -------------------------------------------------------------------------
    // 7. RESOLUCIÓN DE INCIDENCIA
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/resolve con rol OPERATOR debe retornar 403 Forbidden")
    void shouldReturn403WhenOperatorOnResolve() throws Exception {
        mockMvc.perform(post("/api/incidents/1/resolve"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "tech@urbanpulse.es", roles = "TECHNICIAN")
    @DisplayName("POST /api/incidents/{id}/resolve con rol TECHNICIAN debe retornar 200 OK")
    void shouldReturn200WhenTechnicianOnResolve() throws Exception {
        ResolveIncidentRequest request = new ResolveIncidentRequest("Reparación completa del alumbrado");
        IncidentResponse response = new IncidentResponse();
        response.setId(1L);
        response.setStatus(EstadoIncidencia.RESOLVED);

        when(incidentTriageService.resolveIncident(eq(1L), any(), eq("tech@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    // -------------------------------------------------------------------------
    // 8. CIERRE DE INCIDENCIA
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "tech@urbanpulse.es", roles = "TECHNICIAN")
    @DisplayName("POST /api/incidents/{id}/close con rol TECHNICIAN debe retornar 403 Forbidden")
    void shouldReturn403WhenTechnicianOnClose() throws Exception {
        mockMvc.perform(post("/api/incidents/1/close"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/close con rol OPERATOR debe retornar 200 OK")
    void shouldReturn200WhenOperatorOnClose() throws Exception {
        IncidentResponse response = new IncidentResponse();
        response.setId(1L);
        response.setStatus(EstadoIncidencia.CLOSED);

        when(incidentTriageService.closeIncident(eq(1L), eq("operator@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // -------------------------------------------------------------------------
    // 9. REAPERTURA DE INCIDENCIA
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/reopen con motivo corto debe retornar 400 Bad Request")
    void shouldReturn400WhenReopenReasonTooShort() throws Exception {
        ReopenIncidentRequest invalidRequest = new ReopenIncidentRequest("Mal");

        mockMvc.perform(post("/api/incidents/1/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin@urbanpulse.es", roles = "ADMIN")
    @DisplayName("POST /api/incidents/{id}/reopen con rol ADMIN debe retornar 200 OK")
    void shouldReturn200WhenAdminOnReopen() throws Exception {
        ReopenIncidentRequest request = new ReopenIncidentRequest("La avería persiste tras la intervención");
        IncidentResponse response = new IncidentResponse();
        response.setId(1L);
        response.setStatus(EstadoIncidencia.REOPENED);

        when(incidentTriageService.reopenIncident(eq(1L), any(), eq("admin@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REOPENED"));
    }

    // -------------------------------------------------------------------------
    // 10. ASIGNACIONES MULTIDEPARTAMENTALES Y TÉCNICAS
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("POST /api/incidents/{id}/assignments con rol CITIZEN debe retornar 403 Forbidden")
    void shouldReturn403WhenCitizenOnAssign() throws Exception {
        AssignIncidentRequest request = new AssignIncidentRequest(List.of(1L), List.of(10L));

        mockMvc.perform(post("/api/incidents/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/assignments con departamentos vacíos debe retornar 400 Bad Request")
    void shouldReturn400WhenAssignDepartmentsEmpty() throws Exception {
        AssignIncidentRequest invalidRequest = new AssignIncidentRequest(List.of(), List.of());

        mockMvc.perform(post("/api/incidents/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/assignments con rol OPERATOR debe retornar 200 OK y lista de asignaciones")
    void shouldReturn200WhenOperatorOnAssign() throws Exception {
        AssignIncidentRequest request = new AssignIncidentRequest(List.of(1L), List.of(10L));
        AssignmentResponse assignment = AssignmentResponse.builder()
                .id(100L)
                .departmentId(1L)
                .departmentName("Alumbrado")
                .technicianId(10L)
                .technicianName("Carlos Tecnico")
                .isActive(true)
                .build();

        when(assignmentService.assignDepartmentsAndTechnicians(eq(1L), any(), eq("operator@urbanpulse.es")))
                .thenReturn(List.of(assignment));

        mockMvc.perform(post("/api/incidents/1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].departmentName").value("Alumbrado"))
                .andExpect(jsonPath("$[0].technicianName").value("Carlos Tecnico"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("GET /api/incidents/{id}/assignments autenticado debe retornar 200 OK con asignaciones activas")
    void shouldReturn200WhenAuthenticatedOnGetAssignments() throws Exception {
        AssignmentResponse assignment = AssignmentResponse.builder()
                .id(100L)
                .departmentId(1L)
                .departmentName("Alumbrado")
                .technicianId(10L)
                .technicianName("Carlos Tecnico")
                .isActive(true)
                .assignedAt(OffsetDateTime.now())
                .build();

        when(assignmentService.getActiveAssignments(1L)).thenReturn(List.of(assignment));

        mockMvc.perform(get("/api/incidents/1/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].departmentName").value("Alumbrado"))
                .andExpect(jsonPath("$[0].technicianName").value("Carlos Tecnico"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @DisplayName("GET /api/incidents/{id}/assignments sin token debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnGetAssignments() throws Exception {
        mockMvc.perform(get("/api/incidents/1/assignments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/assignments/deactivate con rol OPERATOR debe retornar 200 OK")
    void shouldReturn200WhenOperatorOnDeactivateAssignments() throws Exception {
        DeactivateAssignmentRequest request = new DeactivateAssignmentRequest(List.of(1L), null);

        when(assignmentService.deactivateAssignments(eq(1L), any(), eq("operator@urbanpulse.es")))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/incidents/1/assignments/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // -------------------------------------------------------------------------
    // 11. MURO DE COMENTARIOS Y NOTAS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /api/incidents/{id}/comments sin token debe retornar 401 Unauthorized")
    void shouldReturn401WhenUnauthenticatedOnGetComments() throws Exception {
        mockMvc.perform(get("/api/incidents/1/comments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/incidents/{id}/comments autenticado debe retornar 200 OK con comentarios")
    void shouldReturn200WhenAuthenticatedOnGetComments() throws Exception {
        CommentResponse comment = CommentResponse.builder()
                .id(50L)
                .incidentId(1L)
                .comment("Aviso público para el ciudadano")
                .isInternal(false)
                .createdAt(OffsetDateTime.now())
                .build();

        when(commentService.getComments(eq(1L), eq("citizen@urbanpulse.es")))
                .thenReturn(List.of(comment));

        mockMvc.perform(get("/api/incidents/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].comment").value("Aviso público para el ciudadano"))
                .andExpect(jsonPath("$[0].isInternal").value(false));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("POST /api/incidents/{id}/comments con rol CITIZEN debe retornar 403 Forbidden")
    void shouldReturn403WhenCitizenOnAddComment() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("Nota no permitida", true);

        mockMvc.perform(post("/api/incidents/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("POST /api/incidents/{id}/comments con comentario en blanco debe retornar 400 Bad Request")
    void shouldReturn400WhenAddCommentBlank() throws Exception {
        CreateCommentRequest invalidRequest = new CreateCommentRequest("   ", true);

        mockMvc.perform(post("/api/incidents/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "tech@urbanpulse.es", roles = "TECHNICIAN")
    @DisplayName("POST /api/incidents/{id}/comments con rol TECHNICIAN debe retornar 201 Created")
    void shouldReturn201WhenTechnicianOnAddComment() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("Pieza sustituida correctamente", true);
        CommentResponse response = CommentResponse.builder()
                .id(51L)
                .incidentId(1L)
                .comment("Pieza sustituida correctamente")
                .isInternal(true)
                .authorName("Carlos Tecnico")
                .createdAt(OffsetDateTime.now())
                .build();

        when(commentService.addComment(eq(1L), any(), eq("tech@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(post("/api/incidents/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(51))
                .andExpect(jsonPath("$.comment").value("Pieza sustituida correctamente"))
                .andExpect(jsonPath("$.isInternal").value(true));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("PUT /api/incidents/{id}/comments/{commentId} con texto en blanco debe retornar 400 Bad Request")
    void shouldReturn400WhenUpdateCommentBlank() throws Exception {
        UpdateCommentRequest invalidRequest = new UpdateCommentRequest("   ");

        mockMvc.perform(put("/api/incidents/1/comments/51")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "tech@urbanpulse.es", roles = "TECHNICIAN")
    @DisplayName("PUT /api/incidents/{id}/comments/{commentId} autenticado debe retornar 200 OK")
    void shouldReturn200WhenUpdateCommentSuccess() throws Exception {
        UpdateCommentRequest request = new UpdateCommentRequest("Texto rectificado");
        CommentResponse response = CommentResponse.builder()
                .id(51L)
                .incidentId(1L)
                .comment("Texto rectificado")
                .isInternal(true)
                .updatedAt(OffsetDateTime.now())
                .build();

        when(commentService.updateComment(eq(1L), eq(51L), any(), eq("tech@urbanpulse.es")))
                .thenReturn(response);

        mockMvc.perform(put("/api/incidents/1/comments/51")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(51))
                .andExpect(jsonPath("$.comment").value("Texto rectificado"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }
}
