package org.bulbaseros.service;

import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.IncidentStatusResponse;
import org.bulbaseros.dto.PageResponse;
import org.bulbaseros.dto.ReverseGeocodeResponse;
import org.bulbaseros.dto.StatusChangeResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.mappers.IncidentMapper;
import org.bulbaseros.mappers.StatusChangeMapper;
import org.bulbaseros.repository.CambiosEstadoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias con Mockito para la clase de servicio {@link IncidentService}.
 * <p>
 * Verifica el comportamiento de la logica transaccional de incidencias y auditoria:
 * filtrado cartografico de avisos activos, alta asignando metadatos por defecto,
 * actualizacion atomica de estados con persistencia inmutable en {@link CambiosEstado},
 * gestion de excepciones 404 e historial cronologico ordenado.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    /**
     * Mock del repositorio de datos para la entidad {@link Incidencia}.
     */
    @Mock
    private IncidenciaRepository incidenciaRepository;

    /**
     * Mock del repositorio de datos para la entidad {@link CambiosEstado}.
     */
    @Mock
    private CambiosEstadoRepository cambiosEstadoRepository;

    /**
     * Mock del repositorio de datos para la entidad {@link Usuario}.
     */
    @Mock
    private UserRepository userRepository;

    /**
     * Mock del servicio de integracion con datos abiertos de Malaga.
     */
    @Mock
    private MalagaOpenDataService malagaOpenDataService;

    /**
     * Instancia real del mapeador de incidencias.
     */
    @org.mockito.Spy
    private IncidentMapper incidentMapper = new IncidentMapper();

    /**
     * Instancia real del mapeador de cambios de estado.
     */
    @org.mockito.Spy
    private StatusChangeMapper statusChangeMapper = new StatusChangeMapper();

    /**
     * Instancia bajo prueba del servicio de logica de incidencias con mocks inyectados.
     */
    @InjectMocks
    private IncidentService incidentService;

    /**
     * Usuario de prueba con rol ciudadano para asignacion en altas de incidencias.
     */
    private Usuario testCitizen;

    /**
     * Usuario de prueba con rol operador para autorizacion de cambios de estado.
     */
    private Usuario testOperator;

    /**
     * Entidad de prueba de incidencia urbana base.
     */
    private Incidencia testIncident;

    /**
     * Inicializacion de fixtures y objetos de prueba antes de cada test.
     */
    @BeforeEach
    void setUp() {
        testCitizen = new Usuario("ciudadano@urbanpulse.es", "hash123", "Juan Perez", RolUsuario.CITIZEN);
        testCitizen.setId(10L);

        testOperator = new Usuario("operador@urbanpulse.es", "hash456", "Maria Lopez", RolUsuario.OPERATOR);
        testOperator.setId(20L);

        testIncident = new Incidencia(
                "Farola rota",
                "Farola fundida en calle Larios",
                "Alumbrado",
                36.7213,
                -4.4214,
                testCitizen
        );
        testIncident.setId(1L);
        testIncident.setStatus(EstadoIncidencia.REPORTED);
        testIncident.setPriority(PrioridadIncidencia.MEDIUM);
        testIncident.setCreatedAt(OffsetDateTime.now());
        testIncident.setUpdatedAt(OffsetDateTime.now());

        org.mockito.Mockito.lenient().when(malagaOpenDataService.isWithinMalagaBounds(any(), any()))
                .thenAnswer(invocation -> {
                    Double lat = invocation.getArgument(0);
                    Double lon = invocation.getArgument(1);
                    if (lat == null || lon == null) {
                        return false;
                    }
                    return lat >= MalagaOpenDataService.MIN_LATITUDE && lat <= MalagaOpenDataService.MAX_LATITUDE
                            && lon >= MalagaOpenDataService.MIN_LONGITUDE && lon <= MalagaOpenDataService.MAX_LONGITUDE;
                });
    }

    /**
     * Verifica que getActiveIncidents invoque la consulta excluyendo CLOSED y REJECTED
     * y retorne los registros transformados a DTOs IncidentResponse.
     */
    @Test
    @DisplayName("Debería retornar lista de incidencias activas excluyendo CLOSED y REJECTED")
    void shouldReturnActiveIncidentsExcludingClosedAndRejected() {
        Incidencia incident2 = new Incidencia("Bache peligroso", "Bache en asfalto", "Vías Públicas", 36.7190, -4.4200, testCitizen);
        incident2.setId(2L);
        incident2.setStatus(EstadoIncidencia.IN_PROGRESS);

        when(incidenciaRepository.findByStatusNotIn(argThat(statuses ->
                statuses.contains(EstadoIncidencia.CLOSED) && statuses.contains(EstadoIncidencia.REJECTED) && statuses.size() == 2
        ))).thenReturn(List.of(testIncident, incident2));

        List<IncidentResponse> result = incidentService.getActiveIncidents();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Farola rota", result.get(0).getTitle());
        assertEquals(EstadoIncidencia.REPORTED, result.get(0).getStatus());
        assertEquals(2L, result.get(1).getId());
        assertEquals(EstadoIncidencia.IN_PROGRESS, result.get(1).getStatus());

        verify(incidenciaRepository).findByStatusNotIn(any());
    }

    /**
     * Verifica que getIncidentById retorne el DTO IncidentResponse cuando la incidencia existe.
     */
    @Test
    @DisplayName("Debería retornar IncidentResponse cuando la incidencia existe por id")
    void shouldGetIncidentByIdSuccessfully() {
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(testIncident));

        IncidentResponse response = incidentService.getIncidentById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Farola rota", response.getTitle());
        assertEquals("Alumbrado", response.getCategory());
        assertEquals(10L, response.getCitizenId());
        assertEquals("Juan Perez", response.getCitizenName());
        verify(incidenciaRepository).findById(1L);
    }

    /**
     * Verifica que getIncidentById lance IncidentNotFoundException cuando la incidencia no existe.
     */
    @Test
    @DisplayName("Debería lanzar IncidentNotFoundException cuando la incidencia no existe por id")
    void shouldThrowIncidentNotFoundExceptionWhenIncidentDoesNotExistOnGetById() {
        when(incidenciaRepository.findById(99L)).thenReturn(Optional.empty());

        IncidentNotFoundException ex = assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.getIncidentById(99L)
        );

        assertEquals("Incidencia no encontrada con id: 99", ex.getMessage());
        verify(incidenciaRepository).findById(99L);
    }

    /**
     * Verifica que getIncidentById lance IllegalArgumentException si el id es nulo.
     */
    @Test
    @DisplayName("Debería lanzar IllegalArgumentException cuando el id es nulo en getIncidentById")
    void shouldThrowIllegalArgumentExceptionWhenIdIsNullOnGetById() {
        assertThrows(IllegalArgumentException.class, () -> incidentService.getIncidentById(null));
        verify(incidenciaRepository, never()).findById(any());
    }

    /**
     * Verifica la creacion exitosa de una incidencia asignando el usuario autenticado,
     * estado REPORTED, prioridad MEDIUM y marcas temporales actuales.
     */
    @Test
    @DisplayName("Debería crear incidencia asignando usuario autenticado, estado REPORTED y prioridad MEDIUM")
    void shouldCreateIncidentSuccessfully() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Contenedor desbordado",
                "Basura acumulada en la acera",
                "Limpieza",
                36.7200,
                -4.4250
        );
        request.setDistrict("Centro");
        request.setNeighborhood("La Merced");

        when(userRepository.findByEmail("ciudadano@urbanpulse.es")).thenReturn(Optional.of(testCitizen));

        Incidencia savedMock = new Incidencia(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getLatitude(),
                request.getLongitude(),
                testCitizen
        );
        savedMock.setId(100L);
        savedMock.setDistrict("Centro");
        savedMock.setNeighborhood("La Merced");
        savedMock.setStatus(EstadoIncidencia.REPORTED);
        savedMock.setPriority(PrioridadIncidencia.MEDIUM);
        savedMock.setCreatedAt(OffsetDateTime.now());
        savedMock.setUpdatedAt(OffsetDateTime.now());

        ArgumentCaptor<Incidencia> captor = ArgumentCaptor.forClass(Incidencia.class);
        when(incidenciaRepository.save(captor.capture())).thenReturn(savedMock);

        IncidentResponse response = incidentService.createIncident(request, "ciudadano@urbanpulse.es");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Contenedor desbordado", response.getTitle());
        assertEquals("Limpieza", response.getCategory());
        assertEquals(EstadoIncidencia.REPORTED, response.getStatus());
        assertEquals(PrioridadIncidencia.MEDIUM, response.getPriority());
        assertEquals(10L, response.getCitizenId());

        Incidencia captured = captor.getValue();
        assertEquals("Contenedor desbordado", captured.getTitle());
        assertEquals(EstadoIncidencia.REPORTED, captured.getStatus());
        assertEquals(PrioridadIncidencia.MEDIUM, captured.getPriority());
        assertEquals(testCitizen, captured.getCitizen());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());

        verify(userRepository).findByEmail("ciudadano@urbanpulse.es");
        verify(incidenciaRepository).save(any(Incidencia.class));
    }

    /**
     * Verifica que createIncident lance ResponseStatusException 404 si el usuario autenticado no existe en la base de datos.
     */
    @Test
    @DisplayName("Debería lanzar ResponseStatusException 404 si el usuario creador no existe")
    void shouldThrowResponseStatusExceptionWhenUserNotFoundOnCreate() {
        CreateIncidentRequest request = new CreateIncidentRequest("Aviso", "Desc", "Alumbrado", 36.7, -4.4);
        when(userRepository.findByEmail("noexiste@urbanpulse.es")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.createIncident(request, "noexiste@urbanpulse.es")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(incidenciaRepository, never()).save(any());
    }

    /**
     * Verifica que createIncident lance IllegalArgumentException ante argumentos nulos o invalidos.
     */
    @Test
    @DisplayName("Debería lanzar IllegalArgumentException en createIncident con solicitud o email nulo")
    void shouldThrowIllegalArgumentExceptionOnCreateIncidentWithInvalidArgs() {
        CreateIncidentRequest validRequest = new CreateIncidentRequest("Aviso", "Desc", "Alumbrado", 36.7, -4.4);

        assertThrows(IllegalArgumentException.class, () -> incidentService.createIncident(null, "user@test.es"));
        assertThrows(IllegalArgumentException.class, () -> incidentService.createIncident(validRequest, null));
        assertThrows(IllegalArgumentException.class, () -> incidentService.createIncident(validRequest, "   "));
    }

    /**
     * Verifica que updateIncidentStatus actualice el estado de la incidencia y genere
     * atomicamente un registro de auditoria inmutable en CambiosEstado con los datos de transicion.
     */
    @Test
    @DisplayName("Debería actualizar estado de incidencia y registrar transición atómica en CambiosEstado")
    void shouldUpdateIncidentStatusAndCreateAuditChangeAtomically() {
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest(
                EstadoIncidencia.VALIDATED,
                "Incidencia comprobada y validada para derivación"
        );

        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(testOperator));
        when(incidenciaRepository.save(any(Incidencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<CambiosEstado> cambioCaptor = ArgumentCaptor.forClass(CambiosEstado.class);
        when(cambiosEstadoRepository.save(cambioCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentStatusResponse response = incidentService.updateIncidentStatus(1L, request, "operador@urbanpulse.es");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(EstadoIncidencia.VALIDATED, response.getStatus());
        assertEquals("Estado de la incidencia actualizado exitosamente", response.getMessage());

        CambiosEstado capturedCambio = cambioCaptor.getValue();
        assertNotNull(capturedCambio);
        assertEquals(testIncident, capturedCambio.getIncident());
        assertEquals("REPORTED", capturedCambio.getFromStatus());
        assertEquals("VALIDATED", capturedCambio.getToStatus());
        assertEquals("Incidencia comprobada y validada para derivación", capturedCambio.getReason());
        assertEquals(testOperator, capturedCambio.getChangedByUser());
        assertNotNull(capturedCambio.getCreatedAt());

        verify(incidenciaRepository).findById(1L);
        verify(userRepository).findByEmail("operador@urbanpulse.es");
        verify(incidenciaRepository).save(any(Incidencia.class));
        verify(cambiosEstadoRepository).save(any(CambiosEstado.class));
    }

    /**
     * Verifica que updateIncidentStatus lance ResponseStatusException (400 Bad Request)
     * cuando se intente una transición prohibida por la máquina de estados.
     */
    @Test
    @DisplayName("Debería lanzar ResponseStatusException 400 cuando la transición no esté permitida")
    void shouldThrowBadRequestWhenTransitionIsNotAllowedInLifecycle() {
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest(
                EstadoIncidencia.RESOLVED,
                "Intento de salto directo ilegal de REPORTED a RESOLVED"
        );

        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(testOperator));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, request, "operador@urbanpulse.es")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Transición de estado no permitida"));
        verify(incidenciaRepository, never()).save(any());
        verify(cambiosEstadoRepository, never()).save(any());
    }

    /**
     * Verifica que updateIncidentStatus lance IncidentNotFoundException (404) si la incidencia no existe.
     */
    @Test
    @DisplayName("Debería lanzar IncidentNotFoundException cuando la incidencia no existe al actualizar estado")
    void shouldThrowIncidentNotFoundExceptionWhenIncidentDoesNotExistOnUpdateStatus() {
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "Motivo");
        when(incidenciaRepository.findById(999L)).thenReturn(Optional.empty());

        IncidentNotFoundException ex = assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.updateIncidentStatus(999L, request, "operador@urbanpulse.es")
        );

        assertEquals("Incidencia no encontrada con id: 999", ex.getMessage());
        verify(cambiosEstadoRepository, never()).save(any());
        verify(incidenciaRepository, never()).save(any());
    }

    /**
     * Verifica que updateIncidentStatus lance ResponseStatusException si el usuario actor no existe.
     */
    @Test
    @DisplayName("Debería lanzar ResponseStatusException 404 cuando el usuario actor no existe al actualizar estado")
    void shouldThrowResponseStatusExceptionWhenActorUserDoesNotExistOnUpdateStatus() {
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "Motivo");
        when(incidenciaRepository.findById(1L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("desconocido@urbanpulse.es")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.updateIncidentStatus(1L, request, "desconocido@urbanpulse.es")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(cambiosEstadoRepository, never()).save(any());
    }

    /**
     * Verifica que updateIncidentStatus lance IllegalArgumentException ante parametros nulos o invalidos.
     */
    @Test
    @DisplayName("Debería lanzar IllegalArgumentException ante argumentos inválidos en updateIncidentStatus")
    void shouldThrowIllegalArgumentExceptionOnUpdateStatusWithInvalidArgs() {
        UpdateIncidentStatusRequest validReq = new UpdateIncidentStatusRequest(EstadoIncidencia.RESOLVED);
        UpdateIncidentStatusRequest nullStatusReq = new UpdateIncidentStatusRequest();

        assertThrows(IllegalArgumentException.class, () -> incidentService.updateIncidentStatus(null, validReq, "user@test.es"));
        assertThrows(IllegalArgumentException.class, () -> incidentService.updateIncidentStatus(1L, null, "user@test.es"));
        assertThrows(IllegalArgumentException.class, () -> incidentService.updateIncidentStatus(1L, nullStatusReq, "user@test.es"));
        assertThrows(IllegalArgumentException.class, () -> incidentService.updateIncidentStatus(1L, validReq, null));
        assertThrows(IllegalArgumentException.class, () -> incidentService.updateIncidentStatus(1L, validReq, "   "));
    }

    /**
     * Verifica que getIncidentHistory devuelva la lista de transiciones cronologicas ordenadas.
     */
    @Test
    @DisplayName("Debería devolver la lista cronológica de transiciones de estado para una incidencia existente")
    void shouldReturnIncidentHistoryChronologically() {
        when(incidenciaRepository.existsById(1L)).thenReturn(true);

        CambiosEstado cambio1 = new CambiosEstado(testIncident, "REPORTED", "IN_PROGRESS", testOperator, "Inicio de trabajos");
        cambio1.setId(101L);
        cambio1.setCreatedAt(OffsetDateTime.parse("2026-10-25T10:00:00Z"));

        CambiosEstado cambio2 = new CambiosEstado(testIncident, "IN_PROGRESS", "RESOLVED", testOperator, "Reparacion finalizada");
        cambio2.setId(102L);
        cambio2.setCreatedAt(OffsetDateTime.parse("2026-10-25T11:30:00Z"));

        when(cambiosEstadoRepository.findByIncidentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(cambio1, cambio2));

        List<StatusChangeResponse> history = incidentService.getIncidentHistory(1L);

        assertNotNull(history);
        assertEquals(2, history.size());

        assertEquals(101L, history.get(0).getId());
        assertEquals(1L, history.get(0).getIncidentId());
        assertEquals("REPORTED", history.get(0).getFromStatus());
        assertEquals("IN_PROGRESS", history.get(0).getToStatus());
        assertEquals(20L, history.get(0).getChangedByUserId());
        assertEquals("Maria Lopez", history.get(0).getChangedByUserName());
        assertEquals("operador@urbanpulse.es", history.get(0).getChangedByUserEmail());
        assertEquals("Inicio de trabajos", history.get(0).getReason());

        assertEquals(102L, history.get(1).getId());
        assertEquals("IN_PROGRESS", history.get(1).getFromStatus());
        assertEquals("RESOLVED", history.get(1).getToStatus());
        assertEquals("Reparacion finalizada", history.get(1).getReason());

        verify(incidenciaRepository).existsById(1L);
        verify(cambiosEstadoRepository).findByIncidentIdOrderByCreatedAtAsc(1L);
    }

    /**
     * Verifica que getIncidentHistory lance IncidentNotFoundException (404) si la incidencia no existe.
     */
    @Test
    @DisplayName("Debería lanzar IncidentNotFoundException cuando la incidencia no existe al solicitar historial")
    void shouldThrowIncidentNotFoundExceptionWhenIncidentDoesNotExistOnGetHistory() {
        when(incidenciaRepository.existsById(999L)).thenReturn(false);

        IncidentNotFoundException ex = assertThrows(
                IncidentNotFoundException.class,
                () -> incidentService.getIncidentHistory(999L)
        );

        assertEquals("Incidencia no encontrada con id: 999", ex.getMessage());
        verify(incidenciaRepository).existsById(999L);
        verify(cambiosEstadoRepository, never()).findByIncidentIdOrderByCreatedAtAsc(any());
    }

    /**
     * Verifica que getIncidentHistory lance IllegalArgumentException si el identificador es nulo.
     */
    @Test
    @DisplayName("Debería lanzar IllegalArgumentException cuando el id es nulo en getIncidentHistory")
    void shouldThrowIllegalArgumentExceptionWhenIdIsNullOnGetHistory() {
        assertThrows(IllegalArgumentException.class, () -> incidentService.getIncidentHistory(null));
        verify(incidenciaRepository, never()).existsById(any());
        verify(cambiosEstadoRepository, never()).findByIncidentIdOrderByCreatedAtAsc(any());
    }

    /**
     * Verifica que createIncident autocompleta direccion, barrio y distrito consultando
     * MalagaOpenDataService cuando no se suministran en la peticion de creacion.
     */
    @Test
    @DisplayName("Debería crear incidencia autocompletando dirección, barrio y distrito desde MalagaOpenDataService")
    void shouldCreateIncidentAutocompletingTerritorialDataFromOpenDataService() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Semáforo averiado",
                "Semáforo parpadea en ámbar continuamente",
                "Tráfico",
                36.7213,
                -4.4214
        );

        when(userRepository.findByEmail("ciudadano@urbanpulse.es")).thenReturn(Optional.of(testCitizen));
        when(malagaOpenDataService.resolveLocation(36.7213, -4.4214))
                .thenReturn(new ReverseGeocodeResponse("Calle Marqués de Larios, 4", "Centro", "Centro Histórico"));

        ArgumentCaptor<Incidencia> captor = ArgumentCaptor.forClass(Incidencia.class);
        when(incidenciaRepository.save(captor.capture())).thenAnswer(inv -> {
            Incidencia inc = inv.getArgument(0);
            inc.setId(101L);
            return inc;
        });

        IncidentResponse response = incidentService.createIncident(request, "ciudadano@urbanpulse.es");

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Calle Marqués de Larios, 4", response.getAddress());
        assertEquals("Centro", response.getDistrict());
        assertEquals("Centro Histórico", response.getNeighborhood());
        assertEquals(EstadoIncidencia.REPORTED, response.getStatus());
        assertEquals(PrioridadIncidencia.MEDIUM, response.getPriority());

        Incidencia captured = captor.getValue();
        assertEquals("Calle Marqués de Larios, 4", captured.getAddress());
        assertEquals("Centro", captured.getDistrict());
        assertEquals("Centro Histórico", captured.getNeighborhood());
        assertEquals(EstadoIncidencia.REPORTED, captured.getStatus());
        assertEquals(PrioridadIncidencia.MEDIUM, captured.getPriority());
        assertEquals(testCitizen, captured.getCitizen());

        verify(malagaOpenDataService).resolveLocation(36.7213, -4.4214);
        verify(incidenciaRepository).save(any(Incidencia.class));
    }

    /**
     * Verifica que createIncident aplique degradacion controlada manteniendo nulos los datos
     * territoriales cuando MalagaOpenDataService retorna campos nulos.
     */
    @Test
    @DisplayName("Debería crear incidencia exitosamente ante degradación controlada de MalagaOpenDataService")
    void shouldCreateIncidentSuccessfullyWithControlledDegradationWhenOpenDataReturnsNullFields() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Papelera rota",
                "Papelera arrancada en acera",
                "Limpieza",
                36.7213,
                -4.4214
        );

        when(userRepository.findByEmail("ciudadano@urbanpulse.es")).thenReturn(Optional.of(testCitizen));
        when(malagaOpenDataService.resolveLocation(36.7213, -4.4214))
                .thenReturn(new ReverseGeocodeResponse(null, null, null));

        ArgumentCaptor<Incidencia> captor = ArgumentCaptor.forClass(Incidencia.class);
        when(incidenciaRepository.save(captor.capture())).thenAnswer(inv -> {
            Incidencia inc = inv.getArgument(0);
            inc.setId(102L);
            return inc;
        });

        IncidentResponse response = incidentService.createIncident(request, "ciudadano@urbanpulse.es");

        assertNotNull(response);
        assertEquals(102L, response.getId());
        assertNull(response.getAddress());
        assertNull(response.getDistrict());
        assertNull(response.getNeighborhood());
        assertEquals(EstadoIncidencia.REPORTED, response.getStatus());
        assertEquals(PrioridadIncidencia.MEDIUM, response.getPriority());

        Incidencia captured = captor.getValue();
        assertNull(captured.getAddress());
        assertNull(captured.getDistrict());
        assertNull(captured.getNeighborhood());

        verify(malagaOpenDataService).resolveLocation(36.7213, -4.4214);
        verify(incidenciaRepository).save(any(Incidencia.class));
    }

    /**
     * Verifica que createIncident rechace peticiones con HTTP 400 Bad Request si las coordenadas
     * aportadas se situan fuera de los limites del termino municipal de Malaga.
     */
    @Test
    @DisplayName("Debería rechazar createIncident con 400 Bad Request si las coordenadas están fuera de Málaga")
    void shouldRejectCreateIncidentWhenCoordinatesAreOutsideMalagaBounds() {
        // Coordenadas en Madrid (fuera del termino municipal de Malaga)
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Bache en carretera",
                "Bache pronunciado",
                "Vías Públicas",
                40.4168,
                -3.7038
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.createIncident(request, "ciudadano@urbanpulse.es")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("fuera del término municipal de Málaga"));
        verify(userRepository, never()).findByEmail(any());
        verify(incidenciaRepository, never()).save(any());
    }

    /**
     * Verifica que createIncident rechace peticiones con HTTP 400 Bad Request si alguna coordenada es nula.
     */
    @Test
    @DisplayName("Debería rechazar createIncident con 400 Bad Request si alguna coordenada es nula")
    void shouldRejectCreateIncidentWhenCoordinatesAreNull() {
        CreateIncidentRequest requestWithNullLat = new CreateIncidentRequest(
                "Aviso", "Desc", "Alumbrado", null, -4.4214
        );
        CreateIncidentRequest requestWithNullLon = new CreateIncidentRequest(
                "Aviso", "Desc", "Alumbrado", 36.7213, null
        );

        ResponseStatusException ex1 = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.createIncident(requestWithNullLat, "ciudadano@urbanpulse.es")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.createIncident(requestWithNullLon, "ciudadano@urbanpulse.es")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());

        verify(incidenciaRepository, never()).save(any());
    }

    /**
     * Verifica que getMyIncidents sin filtro de estado consulte findByCitizenId aplicando
     * por defecto ordenacion descendente por createdAt y retorne el PageResponse correspondiente.
     */
    @Test
    @DisplayName("Debería retornar lista paginada de mis incidencias sin filtro de estado con ordenación createdAt DESC")
    void shouldGetMyIncidentsWithoutStatusFilter() {
        when(userRepository.findByEmail("ciudadano@urbanpulse.es")).thenReturn(Optional.of(testCitizen));

        Pageable requestPageable = PageRequest.of(0, 10);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        Page<Incidencia> pageMock = new PageImpl<>(List.of(testIncident), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
        when(incidenciaRepository.findByCitizenId(eq(10L), pageableCaptor.capture())).thenReturn(pageMock);

        PageResponse<IncidentResponse> response = incidentService.getMyIncidents("ciudadano@urbanpulse.es", null, requestPageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(testIncident.getId(), response.getContent().get(0).getId());

        Pageable capturedPageable = pageableCaptor.getValue();
        Sort.Order order = capturedPageable.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());

        verify(incidenciaRepository).findByCitizenId(eq(10L), any(Pageable.class));
        verify(incidenciaRepository, never()).findByCitizenIdAndStatus(any(), any(), any());
    }

    /**
     * Verifica que getMyIncidents con filtro de estado consulte findByCitizenIdAndStatus
     * retornando unicamente las incidencias que coincidan con dicho estado.
     */
    @Test
    @DisplayName("Debería retornar lista paginada de mis incidencias filtrando por estado específico")
    void shouldGetMyIncidentsWithStatusFilter() {
        when(userRepository.findByEmail("ciudadano@urbanpulse.es")).thenReturn(Optional.of(testCitizen));

        Pageable requestPageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Incidencia> pageMock = new PageImpl<>(List.of(testIncident), requestPageable, 1);

        when(incidenciaRepository.findByCitizenIdAndStatus(eq(10L), eq(EstadoIncidencia.REPORTED), any(Pageable.class)))
                .thenReturn(pageMock);

        PageResponse<IncidentResponse> response = incidentService.getMyIncidents(
                "ciudadano@urbanpulse.es",
                EstadoIncidencia.REPORTED,
                requestPageable
        );

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(EstadoIncidencia.REPORTED, response.getContent().get(0).getStatus());

        verify(incidenciaRepository).findByCitizenIdAndStatus(eq(10L), eq(EstadoIncidencia.REPORTED), any(Pageable.class));
        verify(incidenciaRepository, never()).findByCitizenId(any(), any());
    }

    /**
     * Verifica que getMyIncidents lance ResponseStatusException 404 cuando el correo electronico
     * suministrado no corresponde a ningun usuario en el repositorio.
     */
    @Test
    @DisplayName("Debería lanzar ResponseStatusException 404 en getMyIncidents cuando el usuario no existe")
    void shouldThrowNotFoundWhenUserDoesNotExistInGetMyIncidents() {
        when(userRepository.findByEmail("inexistente@urbanpulse.es")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.getMyIncidents("inexistente@urbanpulse.es", null, PageRequest.of(0, 10))
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Usuario no encontrado", ex.getReason());
        verify(incidenciaRepository, never()).findByCitizenId(any(), any());
        verify(incidenciaRepository, never()).findByCitizenIdAndStatus(any(), any(), any());
    }

    /**
     * Verifica que getMyIncidents lance IllegalArgumentException cuando el correo del usuario es nulo o vacio.
     */
    @Test
    @DisplayName("Debería lanzar IllegalArgumentException en getMyIncidents con email nulo o vacío")
    void shouldThrowIllegalArgumentExceptionWhenUserEmailIsBlankInGetMyIncidents() {
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.getMyIncidents(null, null, PageRequest.of(0, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.getMyIncidents("   ", null, PageRequest.of(0, 10)));
        verify(userRepository, never()).findByEmail(any());
    }

    /**
     * Verifica que getMyIncidents aplique paginacion y ordenacion por defecto (createdAt DESC) si pageable es nulo.
     */
    @Test
    @DisplayName("Debería aplicar paginación por defecto si pageable es nulo en getMyIncidents")
    void shouldApplyDefaultPaginationWhenPageableIsNullInGetMyIncidents() {
        when(userRepository.findByEmail("ciudadano@urbanpulse.es")).thenReturn(Optional.of(testCitizen));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        Page<Incidencia> pageMock = new PageImpl<>(List.of(testIncident));
        when(incidenciaRepository.findByCitizenId(eq(10L), captor.capture())).thenReturn(pageMock);

        PageResponse<IncidentResponse> response = incidentService.getMyIncidents("ciudadano@urbanpulse.es", null, null);

        assertNotNull(response);
        Pageable effective = captor.getValue();
        assertEquals(0, effective.getPageNumber());
        assertEquals(10, effective.getPageSize());
        Sort.Order order = effective.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    /**
     * Verifica que reverseGeocode delegue en MalagaOpenDataService y retorne el DTO territorial
     * cuando las coordenadas se encuentran dentro del termino municipal de Malaga.
     */
    @Test
    @DisplayName("Debería resolver geocodificación inversa exitosamente para coordenadas válidas en Málaga")
    void shouldReverseGeocodeSuccessfullyForValidCoordinates() {
        Double lat = 36.7213;
        Double lon = -4.4214;
        ReverseGeocodeResponse mockResponse = new ReverseGeocodeResponse("Calle Marqués de Larios, 4", "Centro", "Centro Histórico");

        when(malagaOpenDataService.resolveLocation(lat, lon)).thenReturn(mockResponse);

        ReverseGeocodeResponse response = incidentService.reverseGeocode(lat, lon);

        assertNotNull(response);
        assertEquals("Calle Marqués de Larios, 4", response.getAddress());
        assertEquals("Centro", response.getDistrict());
        assertEquals("Centro Histórico", response.getNeighborhood());
        verify(malagaOpenDataService).resolveLocation(lat, lon);
    }

    /**
     * Verifica que reverseGeocode rechace con HTTP 400 Bad Request cuando las coordenadas
     * se encuentran fuera del termino municipal de Malaga.
     */
    @Test
    @DisplayName("Debería rechazar reverseGeocode con 400 Bad Request si las coordenadas están fuera de Málaga")
    void shouldRejectReverseGeocodeWhenCoordinatesAreOutsideMalagaBounds() {
        Double lat = 40.4168;
        Double lon = -3.7038;

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.reverseGeocode(lat, lon)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Las coordenadas están fuera de Málaga", ex.getReason());
        verify(malagaOpenDataService, never()).resolveLocation(any(), any());
    }

    /**
     * Verifica que reverseGeocode rechace con HTTP 400 Bad Request si alguna coordenada es nula.
     */
    @Test
    @DisplayName("Debería rechazar reverseGeocode con 400 Bad Request si alguna coordenada es nula")
    void shouldRejectReverseGeocodeWhenCoordinatesAreNull() {
        ResponseStatusException ex1 = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.reverseGeocode(null, -4.4214)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(
                ResponseStatusException.class,
                () -> incidentService.reverseGeocode(36.7213, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());

        verify(malagaOpenDataService, never()).resolveLocation(any(), any());
    }
}
