package org.bulbaseros.service;

import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.IncidentStatusResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
}
