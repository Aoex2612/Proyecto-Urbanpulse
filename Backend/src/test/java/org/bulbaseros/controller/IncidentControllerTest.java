package org.bulbaseros.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.PageResponse;
import org.bulbaseros.dto.ReverseGeocodeResponse;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.security.JwtService;
import org.bulbaseros.service.IncidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas unitarias y de integración para {@link IncidentController}.
 * <p>
 * Verifica los contratos REST, la serialización JSON, la validación de parámetros
 * de consulta y cuerpo, la seguridad perimetral y el manejo de excepciones para los endpoints
 * de consulta paginada de avisos propios ({@code /api/incidents/my-incidents}), geocodificación
 * inversa territorial ({@code /api/incidents/geocode/reverse}), detalle de incidencias y alta.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private IncidentService incidentService;

    @Autowired
    private IncidentController incidentController;

    private IncidentResponse createSampleIncidentResponse(Long id, String title, EstadoIncidencia status) {
        return new IncidentResponse(
                id,
                title,
                "Descripción detallada del aviso urbano",
                "ALUMBRADO",
                36.7213,
                -4.4214,
                status,
                PrioridadIncidencia.MEDIUM,
                1L,
                "Ciudadano Ejemplo",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                5.0,
                "Calle Marqués de Larios, 5",
                "Centro",
                "Centro Histórico"
        );
    }

    // =========================================================================
    // GET /api/incidents/my-incidents
    // =========================================================================

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/incidents/my-incidents con usuario autenticado debe retornar 200 OK y estructura PageResponse")
    void shouldReturn200AndPageResponseForAuthenticatedCitizen() throws Exception {
        IncidentResponse item = createSampleIncidentResponse(101L, "Farola parpadeando", EstadoIncidencia.REPORTED);
        PageResponse<IncidentResponse> pageResponse = new PageResponse<>(
                List.of(item),
                1L,
                1,
                0,
                10
        );

        when(incidentService.getMyIncidents(eq("citizen@urbanpulse.es"), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/incidents/my-incidents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(101))
                .andExpect(jsonPath("$.content[0].title").value("Farola parpadeando"))
                .andExpect(jsonPath("$.content[0].status").value("REPORTED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/incidents/my-incidents con filtro de status debe invocar servicio con dicho estado")
    void shouldReturn200AndFilteredPageResponseWhenStatusProvided() throws Exception {
        IncidentResponse item = createSampleIncidentResponse(102L, "Bache en calzada", EstadoIncidencia.IN_PROGRESS);
        PageResponse<IncidentResponse> pageResponse = new PageResponse<>(
                List.of(item),
                1L,
                1,
                0,
                10
        );

        when(incidentService.getMyIncidents(eq("citizen@urbanpulse.es"), eq(EstadoIncidencia.IN_PROGRESS), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/incidents/my-incidents")
                        .param("status", "IN_PROGRESS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(102))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/incidents/my-incidents con status=TODOS debe interpretar filtro como null")
    void shouldPassNullStatusWhenFilterIsTodos() throws Exception {
        PageResponse<IncidentResponse> emptyPage = new PageResponse<>(List.of(), 0L, 0, 0, 10);
        when(incidentService.getMyIncidents(eq("citizen@urbanpulse.es"), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/incidents/my-incidents")
                        .param("status", "TODOS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/incidents/my-incidents sin token JWT debe retornar 401 Unauthorized")
    void shouldReturn401ForMyIncidentsWithoutJwtToken() throws Exception {
        mockMvc.perform(get("/api/incidents/my-incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es")
    @DisplayName("GET /api/incidents/my-incidents con status inválido debe responder 400 Bad Request")
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/incidents/my-incidents")
                        .param("status", "ESTADO_INEXISTENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("400 BAD_REQUEST"));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es")
    @DisplayName("GET /api/incidents/my-incidents con page negativo o size fuera de rango debe responder 400 Bad Request")
    void shouldReturn400WhenPaginationParamsOutOfBounds() throws Exception {
        mockMvc.perform(get("/api/incidents/my-incidents")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/incidents/my-incidents")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/api/incidents/my-incidents")
                        .param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // =========================================================================
    // GET /api/incidents/geocode/reverse
    // =========================================================================

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/incidents/geocode/reverse con coordenadas válidas dentro de Málaga debe responder 200 OK")
    void shouldReturn200ForValidCoordinatesInMalaga() throws Exception {
        ReverseGeocodeResponse geoResponse = new ReverseGeocodeResponse(
                "Calle Marqués de Larios, 5",
                "Centro",
                "Centro Histórico"
        );

        when(incidentService.reverseGeocode(36.7213, -4.4214)).thenReturn(geoResponse);

        mockMvc.perform(get("/api/incidents/geocode/reverse")
                        .param("latitude", "36.7213")
                        .param("longitude", "-4.4214"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Calle Marqués de Larios, 5"))
                .andExpect(jsonPath("$.district").value("Centro"))
                .andExpect(jsonPath("$.neighborhood").value("Centro Histórico"));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/incidents/geocode/reverse con coordenadas fuera de Málaga o nulas debe responder 400 Bad Request")
    void shouldReturn400ForCoordinatesOutsideMalagaOrNull() throws Exception {
        when(incidentService.reverseGeocode(eq(40.4168), eq(-3.7038)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las coordenadas están fuera de Málaga"));

        when(incidentService.reverseGeocode(isNull(), isNull()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las coordenadas están fuera de Málaga"));

        mockMvc.perform(get("/api/incidents/geocode/reverse")
                        .param("latitude", "40.4168")
                        .param("longitude", "-3.7038"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Las coordenadas están fuera de Málaga"));

        mockMvc.perform(get("/api/incidents/geocode/reverse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/incidents/geocode/reverse sin token JWT debe retornar 401 Unauthorized")
    void shouldReturn401ForReverseGeocodeWithoutJwtToken() throws Exception {
        mockMvc.perform(get("/api/incidents/geocode/reverse")
                        .param("latitude", "36.7213")
                        .param("longitude", "-4.4214"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // =========================================================================
    // GET /api/incidents/{id}
    // =========================================================================

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/incidents/{id} con id inexistente debe retornar 404 Not Found")
    void shouldReturn404WhenIncidentDoesNotExist() throws Exception {
        when(incidentService.getIncidentById(999L))
                .thenThrow(new IncidentNotFoundException("Incidencia no encontrada con id: 999"));

        mockMvc.perform(get("/api/incidents/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada con id: 999"));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("GET /api/incidents/{id} con id existente debe retornar 200 OK con detalle")
    void shouldReturn200WhenIncidentExists() throws Exception {
        IncidentResponse sample = createSampleIncidentResponse(10L, "Alumbrado fundido", EstadoIncidencia.ASSIGNED);
        when(incidentService.getIncidentById(10L)).thenReturn(sample);

        mockMvc.perform(get("/api/incidents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Alumbrado fundido"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    // =========================================================================
    // POST /api/incidents
    // =========================================================================

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = "CITIZEN")
    @DisplayName("POST /api/incidents con coordenadas fuera de límites de Málaga debe responder 400 Bad Request")
    void shouldReturn400WhenCreateIncidentCoordinatesOutsideMalaga() throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Semáforo averiado en Madrid",
                "Semáforo parpadea erráticamente",
                "SENALIZACION",
                40.4168,
                -3.7038,
                null,
                null,
                null,
                null
        );

        when(incidentService.createIncident(any(CreateIncidentRequest.class), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las coordenadas se encuentran fuera del término municipal de Málaga"));

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Las coordenadas se encuentran fuera del término municipal de Málaga"));
    }

    // =========================================================================
    // Pruebas unitarias de invocación directa
    // =========================================================================

    @Test
    @DisplayName("Invocación directa de getMyIncidents debe parsear correctamente estado y delegar en el servicio")
    void shouldDelegateDirectControllerCallForMyIncidents() {
        IncidentService mockService = mock(IncidentService.class);
        IncidentController controller = new IncidentController(mockService);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@urbanpulse.es");

        PageResponse<IncidentResponse> mockPage = new PageResponse<>(List.of(), 0L, 0, 0, 10);
        when(mockService.getMyIncidents(eq("test@urbanpulse.es"), eq(EstadoIncidencia.REPORTED), any(Pageable.class)))
                .thenReturn(mockPage);

        ResponseEntity<PageResponse<IncidentResponse>> response = controller.getMyIncidents(
                "REPORTED",
                0,
                10,
                auth
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(mockService).getMyIncidents(eq("test@urbanpulse.es"), eq(EstadoIncidencia.REPORTED), any(Pageable.class));
    }

    @Test
    @DisplayName("Invocación directa de getMyIncidents con status TODOS debe pasar null al servicio")
    void shouldPassNullStatusOnDirectCallWithTodos() {
        IncidentService mockService = mock(IncidentService.class);
        IncidentController controller = new IncidentController(mockService);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@urbanpulse.es");

        PageResponse<IncidentResponse> mockPage = new PageResponse<>(List.of(), 0L, 0, 0, 10);
        when(mockService.getMyIncidents(eq("test@urbanpulse.es"), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        ResponseEntity<PageResponse<IncidentResponse>> response = controller.getMyIncidents(
                "todos",
                0,
                10,
                auth
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(mockService).getMyIncidents(eq("test@urbanpulse.es"), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Invocación directa de getMyIncidents con parámetros no válidos debe lanzar ResponseStatusException")
    void shouldThrowResponseStatusExceptionOnDirectCallWithInvalidParams() {
        IncidentService mockService = mock(IncidentService.class);
        IncidentController controller = new IncidentController(mockService);
        Authentication auth = mock(Authentication.class);

        assertThrows(ResponseStatusException.class, () -> controller.getMyIncidents(null, -1, 10, auth));
        assertThrows(ResponseStatusException.class, () -> controller.getMyIncidents(null, 0, 0, auth));
        assertThrows(ResponseStatusException.class, () -> controller.getMyIncidents(null, 0, 51, auth));
        assertThrows(ResponseStatusException.class, () -> controller.getMyIncidents("ESTADO_INEXISTENTE", 0, 10, auth));
    }

    @Test
    @DisplayName("Invocación directa de reverseGeocode debe delegar directamente en el servicio")
    void shouldDelegateDirectControllerCallForReverseGeocode() {
        IncidentService mockService = mock(IncidentService.class);
        IncidentController controller = new IncidentController(mockService);

        ReverseGeocodeResponse expected = new ReverseGeocodeResponse("Calle Larios", "Centro", "Centro Histórico");
        when(mockService.reverseGeocode(36.7213, -4.4214)).thenReturn(expected);

        ResponseEntity<ReverseGeocodeResponse> response = controller.reverseGeocode(36.7213, -4.4214);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Calle Larios", response.getBody().getAddress());
    }

    @Test
    @DisplayName("Invocación con token JWT real de CITIZEN a my-incidents y geocode/reverse debe responder 200 OK")
    void shouldAuthorizeRealJwtTokenForCitizenOnBothEndpoints() throws Exception {
        String token = jwtService.generateToken("citizen@urbanpulse.es", "CITIZEN");

        PageResponse<IncidentResponse> pageResponse = new PageResponse<>(List.of(), 0L, 0, 0, 10);
        when(incidentService.getMyIncidents(eq("citizen@urbanpulse.es"), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/incidents/my-incidents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        ReverseGeocodeResponse geoResponse = new ReverseGeocodeResponse("Calle Larios", "Centro", "Centro");
        when(incidentService.reverseGeocode(36.7213, -4.4214)).thenReturn(geoResponse);

        mockMvc.perform(get("/api/incidents/geocode/reverse")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("latitude", "36.7213")
                        .param("longitude", "-4.4214"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Calle Larios"));
    }
}
