package org.bulbaseros.controller.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.PageResponse;
import org.bulbaseros.dto.v2.IncidentResponseV2;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.security.JwtService;
import org.bulbaseros.service.AssignmentService;
import org.bulbaseros.service.AttachmentService;
import org.bulbaseros.service.CommentService;
import org.bulbaseros.service.ExternalContextService;
import org.bulbaseros.service.IncidentService;
import org.bulbaseros.service.IntelligentRecommendationService;
import org.bulbaseros.service.UrbanAssetService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private UrbanAssetService urbanAssetService;

    @MockitoBean
    private ExternalContextService externalContextService;

    @MockitoBean
    private AssignmentService assignmentService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private AttachmentService attachmentService;

    @MockitoBean
    private IntelligentRecommendationService recommendationService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "ciudadano@malaga.es", roles = "CITIZEN")
    @DisplayName("GET /api/v2/incidents debe retornar lista de incidentes con id UUID")
    void getActiveIncidents_shouldReturnListWithUuid() throws Exception {
        UUID publicId = UUID.randomUUID();
        IncidentResponseV2 incident = IncidentResponseV2.builder()
                .id(publicId.toString())
                .title("Farola rota")
                .description("Farola apagada en calle Larios")
                .category("ALUMBRADO")
                .latitude(36.7213)
                .longitude(-4.4214)
                .address("Calle Marques de Larios, 5")
                .status(EstadoIncidencia.REPORTED)
                .priority(PrioridadIncidencia.MEDIUM)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(incidentService.getActiveIncidentsV2()).thenReturn(List.of(incident));

        mockMvc.perform(get("/api/v2/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(publicId.toString()))
                .andExpect(jsonPath("$[0].title").value("Farola rota"))
                .andExpect(jsonPath("$[0].status").value("REPORTED"));
    }

    @Test
    @WithMockUser(username = "ciudadano@malaga.es", roles = "CITIZEN")
    @DisplayName("GET /api/v2/incidents/{id} con UUID valido debe retornar detalle")
    void getIncidentByPublicId_whenExists_shouldReturnIncident() throws Exception {
        UUID publicId = UUID.randomUUID();
        IncidentResponseV2 incident = IncidentResponseV2.builder()
                .id(publicId.toString())
                .title("Bache en asfalto")
                .description("Bache profundo en calzada")
                .category("VIAS_PUBLICAS")
                .latitude(36.7200)
                .longitude(-4.4200)
                .address("Calle Granada, 10")
                .status(EstadoIncidencia.VALIDATED)
                .priority(PrioridadIncidencia.HIGH)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(incidentService.getIncidentByPublicId(publicId)).thenReturn(incident);

        mockMvc.perform(get("/api/v2/incidents/" + publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicId.toString()))
                .andExpect(jsonPath("$.title").value("Bache en asfalto"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @WithMockUser(username = "ciudadano@malaga.es", roles = "CITIZEN")
    @DisplayName("GET /api/v2/incidents/{id} con UUID inexistente debe responder 404")
    void getIncidentByPublicId_whenNotFound_shouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(incidentService.getIncidentByPublicId(nonExistentId))
                .thenThrow(new IncidentNotFoundException("Incidencia no encontrada con identificador publico: " + nonExistentId));

        mockMvc.perform(get("/api/v2/incidents/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ciudadano@malaga.es", roles = "CITIZEN")
    @DisplayName("GET /api/v2/incidents/{id} con formato UUID invalido debe responder 400 Bad Request")
    void getIncidentByPublicId_whenMalformedUuid_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v2/incidents/identificador-no-uuid-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @WithMockUser(username = "ciudadano@malaga.es", roles = "CITIZEN")
    @DisplayName("POST /api/v2/incidents debe crear incidencia y retornar DTO con UUID")
    void createIncident_shouldReturnCreatedIncidentWithUuid() throws Exception {
        UUID publicId = UUID.randomUUID();
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Semaforo apagado",
                "Semaforo en cruce principal no funciona",
                "TRAFICO",
                36.7150,
                -4.4250
        );

        IncidentResponseV2 created = IncidentResponseV2.builder()
                .id(publicId.toString())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(EstadoIncidencia.REPORTED)
                .priority(PrioridadIncidencia.MEDIUM)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(incidentService.createIncidentV2(any(CreateIncidentRequest.class), any())).thenReturn(created);

        mockMvc.perform(post("/api/v2/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(publicId.toString()))
                .andExpect(jsonPath("$.title").value("Semaforo apagado"));
    }

    @Test
    @WithMockUser(username = "ciudadano@malaga.es")
    @DisplayName("GET /api/v2/incidents/my-incidents debe responder 200 con paginacion")
    void getMyIncidents_shouldReturnPageOfIncidents() throws Exception {
        UUID publicId = UUID.randomUUID();
        IncidentResponseV2 incident = IncidentResponseV2.builder()
                .id(publicId.toString())
                .title("Contenedor desbordado")
                .description("Basura acumulada en la acera")
                .category("LIMPIEZA")
                .latitude(36.7100)
                .longitude(-4.4300)
                .address("Calle Marmoles, 1")
                .status(EstadoIncidencia.REPORTED)
                .priority(PrioridadIncidencia.LOW)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        PageResponse<IncidentResponseV2> pageResponse = new PageResponse<>(
                List.of(incident), 1L, 1, 0, 10
        );

        when(incidentService.getMyIncidentsV2(anyInt(), anyInt(), any(), eq("ciudadano@malaga.es")))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v2/incidents/my-incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(publicId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "operador@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("GET /api/v2/incidents/search con OPERATOR debe delegar filtros avanzados y UUID de departamento")
    void searchIncidents_withOperator_shouldDelegateAdvancedFilters() throws Exception {
        UUID departmentId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        IncidentResponseV2 incident = IncidentResponseV2.builder()
                .id(publicId.toString())
                .title("Farola critica")
                .status(EstadoIncidencia.REPORTED)
                .priority(PrioridadIncidencia.CRITICAL)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(incidentService.searchIncidentsV2(any(), any(), any(), any(), any(), any(), any(), any(),
                eq(false), eq("operador@urbanpulse.es")))
                .thenReturn(List.of(incident));

        mockMvc.perform(get("/api/v2/incidents/search")
                        .param("priority", "CRITICAL")
                        .param("district", "Centro")
                        .param("departmentId", departmentId.toString())
                        .param("unassignedDepartment", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(publicId.toString()));

        verify(incidentService).searchIncidentsV2(isNull(), isNull(), isNull(), isNull(), isNull(),
                eq("Centro"), eq(PrioridadIncidencia.CRITICAL), eq(departmentId), eq(false),
                eq("operador@urbanpulse.es"));
    }

    @Test
    @WithMockUser(username = "ciudadano@malaga.es", roles = "CITIZEN")
    @DisplayName("GET /api/v2/incidents/search con CITIZEN debe pasar los parametros y su email al servicio")
    void searchIncidents_withCitizen_shouldPassCitizenEmail() throws Exception {
        UUID departmentId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        IncidentResponseV2 incident = IncidentResponseV2.builder()
                .id(publicId.toString())
                .title("Bache calzada")
                .status(EstadoIncidencia.REPORTED)
                .priority(PrioridadIncidencia.LOW)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(incidentService.searchIncidentsV2(any(), any(), any(), any(), any(), any(), any(), any(),
                eq(false), eq("ciudadano@malaga.es")))
                .thenReturn(List.of(incident));

        mockMvc.perform(get("/api/v2/incidents/search")
                        .param("text", "bache")
                        .param("priority", "HIGH")
                        .param("district", "Centro")
                        .param("departmentId", departmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(publicId.toString()));

        verify(incidentService).searchIncidentsV2(eq("bache"), any(), any(), any(), any(), eq("Centro"),
                eq(PrioridadIncidencia.HIGH), eq(departmentId), eq(false), eq("ciudadano@malaga.es"));
    }

    @Test
    @WithMockUser(username = "operador@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("GET /api/v2/incidents/search con UUID de departamento inexistente debe responder 200 con lista vacia (C-4)")
    void searchIncidents_withUnknownDepartment_shouldReturnEmptyList() throws Exception {
        UUID missingDepartmentId = UUID.randomUUID();
        when(incidentService.searchIncidentsV2(any(), any(), any(), any(), any(), any(), any(),
                eq(missingDepartmentId), eq(false), eq("operador@urbanpulse.es")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v2/incidents/search")
                        .param("departmentId", missingDepartmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "operador@urbanpulse.es", roles = "OPERATOR")
    @DisplayName("GET /api/v2/incidents/search con status invalido debe responder 400 Bad Request")
    void searchIncidents_withInvalidStatus_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v2/incidents/search")
                        .param("status", "ESTADO_INEXISTENTE"))
                .andExpect(status().isBadRequest());
    }
}
