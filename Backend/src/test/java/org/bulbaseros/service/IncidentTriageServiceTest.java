package org.bulbaseros.service;

import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.PriorityCalculationRequest;
import org.bulbaseros.dto.PriorityCalculationResponse;
import org.bulbaseros.dto.ReevaluateIncidentRequest;
import org.bulbaseros.dto.RejectIncidentRequest;
import org.bulbaseros.dto.ReopenIncidentRequest;
import org.bulbaseros.dto.ResolveIncidentRequest;
import org.bulbaseros.dto.SafetyRiskLevel;
import org.bulbaseros.dto.UpdatePriorityRequest;
import org.bulbaseros.dto.ValidateIncidentRequest;
import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.mappers.IncidentMapper;
import org.bulbaseros.repository.CambiosEstadoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.repository.TareasRepository;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyString;
import org.bulbaseros.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link IncidentTriageService} con Mockito.
 * <p>
 * Valida de forma exhaustiva:
 * <ul>
 *   <li>Cálculo de prioridad asistida y actualización manual de prioridad (RF-1, RF-2, RF-3).</li>
 *   <li>Validación y rechazo con desactivación masiva de tareas y control de motivos (RF-4, RF-5, RF-6).</li>
 *   <li>Reevaluación restringida a estados REPORTED o VALIDATED (RF-7).</li>
 *   <li>Inicio de intervención por técnicos asignados activos y transiciones a IN_PROGRESS (RF-13, RF-14).</li>
 *   <li>Resolución con detalle obligatorio para prioridades no menores (RF-15, RF-16).</li>
 *   <li>Cierre definitivo con inactivación de asignaciones y reapertura justificada (RF-17, RF-18, RF-19).</li>
 *   <li>Control de acceso RBAC denegando con 403 Forbidden a perfiles no autorizados.</li>
 *   <li>Inserción obligatoria de auditoría inmutable en {@link CambiosEstadoRepository} en cada transición.</li>
 * </ul>
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class IncidentTriageServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CambiosEstadoRepository cambiosEstadoRepository;

    @Mock
    private TareasRepository tareasRepository;

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private PriorityService priorityService;

    @Mock
    private IncidentMapper incidentMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private IncidentTriageService triageService;

    private Usuario citizen;
    private Usuario operator;
    private Usuario technician;
    private Usuario admin;
    private Incidencia incident;
    private IncidentResponse incidentResponse;

    @BeforeEach
    void setUp() {
        citizen = new Usuario("ciudadano@urbanpulse.es", "pwd", "Juan Ciudadano", RolUsuario.CITIZEN);
        citizen.setId(1L);

        operator = new Usuario("operador@urbanpulse.es", "pwd", "Elena Operadora", RolUsuario.OPERATOR);
        operator.setId(2L);

        technician = new Usuario("tecnico@urbanpulse.es", "pwd", "Carlos Técnico", RolUsuario.TECHNICIAN);
        technician.setId(3L);

        admin = new Usuario("admin@urbanpulse.es", "pwd", "Laura Admin", RolUsuario.ADMIN);
        admin.setId(4L);

        incident = new Incidencia("Bache profundo en calzada", "Bache peligroso", "Vías Públicas", 36.72, -4.42, citizen);
        incident.setId(100L);
        incident.setStatus(EstadoIncidencia.REPORTED);
        incident.setPriority(PrioridadIncidencia.MEDIUM);
        incident.setCreatedAt(OffsetDateTime.now());
        incident.setUpdatedAt(OffsetDateTime.now());

        incidentResponse = new IncidentResponse();
        incidentResponse.setId(100L);
        incidentResponse.setTitle("Bache profundo en calzada");
        incidentResponse.setStatus(EstadoIncidencia.REPORTED);
        incidentResponse.setPriority(PrioridadIncidencia.MEDIUM);
    }

    // =========================================================================
    // 1. calculateAssistedPriority (RF-1, RF-3)
    // =========================================================================

    @Test
    @DisplayName("calculateAssistedPriority: éxito con rol OPERATOR delegando en PriorityService")
    void shouldCalculateAssistedPrioritySuccessfullyForOperator() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(true)
                .safetyRisk(SafetyRiskLevel.HIGH)
                .build();
        PriorityCalculationResponse response = PriorityCalculationResponse.builder()
                .suggestedPriority(PrioridadIncidencia.CRITICAL)
                .priorityReason("Prioridad CRÍTICA sugerida...")
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.existsById(100L)).thenReturn(true);
        when(priorityService.calculateAssistedPriority(request)).thenReturn(response);

        PriorityCalculationResponse result = triageService.calculateAssistedPriority(100L, request, "operador@urbanpulse.es");

        assertNotNull(result);
        assertEquals(PrioridadIncidencia.CRITICAL, result.getSuggestedPriority());
        verify(priorityService).calculateAssistedPriority(request);
    }

    @Test
    @DisplayName("calculateAssistedPriority: éxito con rol ADMIN")
    void shouldCalculateAssistedPrioritySuccessfullyForAdmin() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(false)
                .safetyRisk(SafetyRiskLevel.LOW)
                .build();
        PriorityCalculationResponse response = PriorityCalculationResponse.builder()
                .suggestedPriority(PrioridadIncidencia.MEDIUM)
                .priorityReason("Prioridad MEDIA...")
                .build();

        when(userRepository.findByEmail("admin@urbanpulse.es")).thenReturn(Optional.of(admin));
        when(incidenciaRepository.existsById(100L)).thenReturn(true);
        when(priorityService.calculateAssistedPriority(request)).thenReturn(response);

        PriorityCalculationResponse result = triageService.calculateAssistedPriority(100L, request, "admin@urbanpulse.es");

        assertNotNull(result);
        assertEquals(PrioridadIncidencia.MEDIUM, result.getSuggestedPriority());
    }

    @Test
    @DisplayName("calculateAssistedPriority: lanza 403 Forbidden si el rol es CITIZEN o TECHNICIAN (RF-3)")
    void shouldThrow403InCalculateAssistedPriorityForUnauthorizedRole() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(true)
                .safetyRisk(SafetyRiskLevel.HIGH)
                .build();

        when(userRepository.findByEmail("ciudadano@urbanpulse.es")).thenReturn(Optional.of(citizen));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> triageService.calculateAssistedPriority(100L, request, "ciudadano@urbanpulse.es"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(priorityService);
    }

    @Test
    @DisplayName("calculateAssistedPriority: lanza 404 si la incidencia no existe")
    void shouldThrow404InCalculateAssistedPriorityWhenIncidentNotFound() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(false)
                .safetyRisk(SafetyRiskLevel.NONE)
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.existsById(999L)).thenReturn(false);

        assertThrows(IncidentNotFoundException.class,
                () -> triageService.calculateAssistedPriority(999L, request, "operador@urbanpulse.es"));
    }

    // =========================================================================
    // 2. updatePriority (RF-2, RF-3)
    // =========================================================================

    @Test
    @DisplayName("updatePriority: actualiza prioridad y justificación con OPERATOR exitosamente")
    void shouldUpdatePrioritySuccessfully() {
        UpdatePriorityRequest request = UpdatePriorityRequest.builder()
                .priority(PrioridadIncidencia.HIGH)
                .priorityReason("Afecta vía de alta concurrencia")
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.updatePriority(100L, request, "operador@urbanpulse.es");

        assertNotNull(result);
        assertEquals(PrioridadIncidencia.HIGH, incident.getPriority());
        assertEquals("Afecta vía de alta concurrencia", incident.getPriorityReason());
        verify(incidenciaRepository).save(incident);
        verify(incidentMapper).toDto(incident);
    }

    @Test
    @DisplayName("updatePriority: lanza 403 si el usuario es TECHNICIAN o CITIZEN")
    void shouldThrow403InUpdatePriorityForTechnician() {
        UpdatePriorityRequest request = UpdatePriorityRequest.builder()
                .priority(PrioridadIncidencia.HIGH)
                .build();

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> triageService.updatePriority(100L, request, "tecnico@urbanpulse.es"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(incidenciaRepository);
    }

    @Test
    @DisplayName("updatePriority: lanza IllegalArgumentException si la prioridad es nula")
    void shouldThrowIllegalArgumentExceptionWhenPriorityIsNull() {
        UpdatePriorityRequest request = UpdatePriorityRequest.builder()
                .priority(null)
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        assertThrows(IllegalArgumentException.class,
                () -> triageService.updatePriority(100L, request, "operador@urbanpulse.es"));
    }

    // =========================================================================
    // 3. validateIncident (RF-4)
    // =========================================================================

    @Test
    @DisplayName("validateIncident: transiciona de REPORTED a VALIDATED y registra auditoría con motivo")
    void shouldValidateIncidentSuccessfullyWithReason() {
        ValidateIncidentRequest request = ValidateIncidentRequest.builder()
                .reason("Aviso verificado con servicios de emergencia")
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.validateIncident(100L, request, "operador@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.VALIDATED, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        CambiosEstado cambio = captor.getValue();
        assertEquals(incident, cambio.getIncident());
        assertEquals("REPORTED", cambio.getFromStatus());
        assertEquals("VALIDATED", cambio.getToStatus());
        assertEquals(operator, cambio.getChangedByUser());
        assertEquals("Aviso verificado con servicios de emergencia", cambio.getReason());
        assertNotNull(cambio.getCreatedAt());
    }

    @Test
    @DisplayName("validateIncident: permite validación sin motivo (motivo opcional)")
    void shouldValidateIncidentWithoutReason() {
        ValidateIncidentRequest request = new ValidateIncidentRequest(null);

        when(userRepository.findByEmail("admin@urbanpulse.es")).thenReturn(Optional.of(admin));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.validateIncident(100L, request, "admin@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.VALIDATED, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        assertNull(captor.getValue().getReason());
    }

    @Test
    @DisplayName("validateIncident: lanza IllegalStateException si la incidencia no está en REPORTED")
    void shouldThrowIllegalStateExceptionWhenIncidentIsNotReported() {
        incident.setStatus(EstadoIncidencia.VALIDATED);

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> triageService.validateIncident(100L, new ValidateIncidentRequest(), "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("REPORTED"));
        verifyNoInteractions(cambiosEstadoRepository);
    }

    @Test
    @DisplayName("validateIncident: lanza 403 si el rol no es OPERATOR ni ADMIN")
    void shouldThrow403InValidateIncidentForUnauthorizedUser() {
        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));

        assertThrows(ResponseStatusException.class,
                () -> triageService.validateIncident(100L, new ValidateIncidentRequest(), "tecnico@urbanpulse.es"));
        verifyNoInteractions(cambiosEstadoRepository);
    }

    // =========================================================================
    // 4. rejectIncident (RF-5, RF-6)
    // =========================================================================

    @Test
    @DisplayName("rejectIncident: rechaza correctamente, desactiva asignaciones e inserta auditoría (RF-5)")
    void shouldRejectIncidentSuccessfully() {
        incident.setStatus(EstadoIncidencia.REPORTED);
        RejectIncidentRequest request = RejectIncidentRequest.builder()
                .reason("No corresponde a competencias municipales de Málaga")
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.rejectIncident(100L, request, "operador@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.REJECTED, incident.getStatus());
        verify(assignmentService).deactivateAllActiveAssignments(100L);

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        CambiosEstado cambio = captor.getValue();
        assertEquals("REPORTED", cambio.getFromStatus());
        assertEquals("REJECTED", cambio.getToStatus());
        assertEquals(operator, cambio.getChangedByUser());
        assertEquals("No corresponde a competencias municipales de Málaga", cambio.getReason());
    }

    @Test
    @DisplayName("rejectIncident: lanza IllegalArgumentException si motivo es menor a 5 caracteres (RF-6)")
    void shouldThrowIllegalArgumentExceptionWhenRejectReasonIsTooShort() {
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        RejectIncidentRequest shortReasonRequest = RejectIncidentRequest.builder().reason("No").build();
        assertThrows(IllegalArgumentException.class,
                () -> triageService.rejectIncident(100L, shortReasonRequest, "operador@urbanpulse.es"));

        RejectIncidentRequest blankReasonRequest = RejectIncidentRequest.builder().reason("     ").build();
        assertThrows(IllegalArgumentException.class,
                () -> triageService.rejectIncident(100L, blankReasonRequest, "operador@urbanpulse.es"));

        RejectIncidentRequest nullReasonRequest = RejectIncidentRequest.builder().reason(null).build();
        assertThrows(IllegalArgumentException.class,
                () -> triageService.rejectIncident(100L, nullReasonRequest, "operador@urbanpulse.es"));

        verifyNoInteractions(assignmentService);
        verifyNoInteractions(cambiosEstadoRepository);
    }

    @Test
    @DisplayName("rejectIncident: lanza IllegalStateException si ya está en REJECTED o CLOSED")
    void shouldThrowIllegalStateExceptionWhenRejectingTerminalIncident() {
        incident.setStatus(EstadoIncidencia.REJECTED);
        RejectIncidentRequest request = RejectIncidentRequest.builder().reason("Motivo válido").build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        assertThrows(IllegalStateException.class,
                () -> triageService.rejectIncident(100L, request, "operador@urbanpulse.es"));

        incident.setStatus(EstadoIncidencia.CLOSED);
        assertThrows(IllegalStateException.class,
                () -> triageService.rejectIncident(100L, request, "operador@urbanpulse.es"));

        verifyNoInteractions(assignmentService);
    }

    // =========================================================================
    // 5. reevaluateIncident (RF-7)
    // =========================================================================

    @Test
    @DisplayName("reevaluateIncident: reactiva a REPORTED desde REJECTED con auditoría (RF-7)")
    void shouldReevaluateIncidentToReported() {
        incident.setStatus(EstadoIncidencia.REJECTED);
        ReevaluateIncidentRequest request = ReevaluateIncidentRequest.builder()
                .targetStatus(EstadoIncidencia.REPORTED)
                .reason("Reevaluación tras comprobar foto adicional ciudadana")
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.reevaluateIncident(100L, request, "operador@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.REPORTED, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        CambiosEstado cambio = captor.getValue();
        assertEquals("REJECTED", cambio.getFromStatus());
        assertEquals("REPORTED", cambio.getToStatus());
        assertEquals("Reevaluación tras comprobar foto adicional ciudadana", cambio.getReason());
    }

    @Test
    @DisplayName("reevaluateIncident: reactiva a VALIDATED desde REJECTED con auditoría (RF-7)")
    void shouldReevaluateIncidentToValidated() {
        incident.setStatus(EstadoIncidencia.REJECTED);
        ReevaluateIncidentRequest request = ReevaluateIncidentRequest.builder()
                .targetStatus(EstadoIncidencia.VALIDATED)
                .reason("Reapertura inmediata por confirmación de la policía local")
                .build();

        when(userRepository.findByEmail("admin@urbanpulse.es")).thenReturn(Optional.of(admin));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.reevaluateIncident(100L, request, "admin@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.VALIDATED, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        assertEquals("VALIDATED", captor.getValue().getToStatus());
    }

    @Test
    @DisplayName("reevaluateIncident: lanza IllegalArgumentException si targetStatus no es REPORTED ni VALIDATED")
    void shouldThrowIllegalArgumentExceptionWhenTargetStatusIsInvalid() {
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        ReevaluateIncidentRequest invalidTarget = ReevaluateIncidentRequest.builder()
                .targetStatus(EstadoIncidencia.IN_PROGRESS)
                .reason("Motivo suficientemente largo")
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> triageService.reevaluateIncident(100L, invalidTarget, "operador@urbanpulse.es"));

        verifyNoInteractions(incidenciaRepository);
    }

    @Test
    @DisplayName("reevaluateIncident: lanza IllegalArgumentException si motivo es menor a 5 caracteres")
    void shouldThrowIllegalArgumentExceptionWhenReevaluateReasonIsTooShort() {
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        ReevaluateIncidentRequest shortReason = ReevaluateIncidentRequest.builder()
                .targetStatus(EstadoIncidencia.REPORTED)
                .reason("Ok")
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> triageService.reevaluateIncident(100L, shortReason, "operador@urbanpulse.es"));
    }

    @Test
    @DisplayName("reevaluateIncident: lanza IllegalStateException si la incidencia no está en REJECTED")
    void shouldThrowIllegalStateExceptionWhenIncidentIsNotRejected() {
        incident.setStatus(EstadoIncidencia.VALIDATED);
        ReevaluateIncidentRequest request = ReevaluateIncidentRequest.builder()
                .targetStatus(EstadoIncidencia.REPORTED)
                .reason("Motivo válido de reevaluación")
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> triageService.reevaluateIncident(100L, request, "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("REJECTED"));
    }

    // =========================================================================
    // 6. startIntervention (RF-13, RF-14)
    // =========================================================================

    @Test
    @DisplayName("startIntervention: técnico asignado inicia trabajo desde ASSIGNED a IN_PROGRESS (RF-13)")
    void shouldStartInterventionSuccessfullyFromAssigned() {
        incident.setStatus(EstadoIncidencia.ASSIGNED);

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(true);
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.startIntervention(100L, "tecnico@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.IN_PROGRESS, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        CambiosEstado cambio = captor.getValue();
        assertEquals("ASSIGNED", cambio.getFromStatus());
        assertEquals("IN_PROGRESS", cambio.getToStatus());
        assertEquals(technician, cambio.getChangedByUser());
    }

    @Test
    @DisplayName("startIntervention: técnico asignado inicia trabajo desde REOPENED a IN_PROGRESS (RF-13)")
    void shouldStartInterventionSuccessfullyFromReopened() {
        incident.setStatus(EstadoIncidencia.REOPENED);

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(true);
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.startIntervention(100L, "tecnico@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.IN_PROGRESS, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        assertEquals("REOPENED", captor.getValue().getFromStatus());
    }

    @Test
    @DisplayName("startIntervention: administrador puede iniciar intervención directamente")
    void shouldStartInterventionSuccessfullyByAdmin() {
        incident.setStatus(EstadoIncidencia.ASSIGNED);

        when(userRepository.findByEmail("admin@urbanpulse.es")).thenReturn(Optional.of(admin));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.startIntervention(100L, "admin@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.IN_PROGRESS, incident.getStatus());
        verify(tareasRepository, never()).existsByIncidentIdAndTechnicianIdAndIsActiveTrue(any(), any());
    }

    @Test
    @DisplayName("startIntervention: lanza 403 Forbidden si el técnico no está asignado activamente (RF-14)")
    void shouldThrow403WhenTechnicianIsNotAssigned() {
        incident.setStatus(EstadoIncidencia.ASSIGNED);

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> triageService.startIntervention(100L, "tecnico@urbanpulse.es"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No es un técnico asignado"));
        verifyNoInteractions(cambiosEstadoRepository);
    }

    @Test
    @DisplayName("startIntervention: lanza 403 si el usuario es CITIZEN u OPERATOR")
    void shouldThrow403InStartInterventionForOperatorOrCitizen() {
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> triageService.startIntervention(100L, "operador@urbanpulse.es"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("startIntervention: lanza IllegalStateException si estado previo no es ASSIGNED ni REOPENED")
    void shouldThrowIllegalStateExceptionWhenStartingFromInvalidStatus() {
        incident.setStatus(EstadoIncidencia.VALIDATED);

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> triageService.startIntervention(100L, "tecnico@urbanpulse.es"));
    }

    // =========================================================================
    // 7. resolveIncident (RF-15, RF-16)
    // =========================================================================

    @Test
    @DisplayName("resolveIncident: permite resolución sin notas en incidencias de prioridad LOW (RF-15)")
    void shouldResolveIncidentSuccessfullyForLowPriorityWithoutNotes() {
        incident.setStatus(EstadoIncidencia.IN_PROGRESS);
        incident.setPriority(PrioridadIncidencia.LOW);

        ResolveIncidentRequest request = ResolveIncidentRequest.builder()
                .resolutionNotes("")
                .build();

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(true);
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.resolveIncident(100L, request, "tecnico@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.RESOLVED, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        assertEquals("IN_PROGRESS", captor.getValue().getFromStatus());
        assertEquals("RESOLVED", captor.getValue().getToStatus());
    }

    @ParameterizedTest
    @EnumSource(value = PrioridadIncidencia.class, names = {"MEDIUM", "HIGH", "CRITICAL"})
    @DisplayName("resolveIncident: resolución exitosa con notas >= 10 caracteres para prioridades medias, altas y críticas (RF-15)")
    void shouldResolveIncidentSuccessfullyWithNotesForNonLowPriorities(PrioridadIncidencia priority) {
        incident.setStatus(EstadoIncidencia.IN_PROGRESS);
        incident.setPriority(priority);

        ResolveIncidentRequest request = ResolveIncidentRequest.builder()
                .resolutionNotes("Reparación completa del firme asfáltico y pintado")
                .build();

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(true);
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.resolveIncident(100L, request, "tecnico@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.RESOLVED, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        assertEquals("Reparación completa del firme asfáltico y pintado", captor.getValue().getReason());
    }

    @ParameterizedTest
    @EnumSource(value = PrioridadIncidencia.class, names = {"MEDIUM", "HIGH", "CRITICAL"})
    @DisplayName("resolveIncident: bloquea resolución si descripción < 10 chars para MEDIUM, HIGH o CRITICAL (RF-16)")
    void shouldThrowIllegalArgumentExceptionWhenNotesTooShortForNonLowPriorities(PrioridadIncidencia priority) {
        incident.setStatus(EstadoIncidencia.IN_PROGRESS);
        incident.setPriority(priority);

        ResolveIncidentRequest request = ResolveIncidentRequest.builder()
                .resolutionNotes("Hecho")
                .build();

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> triageService.resolveIncident(100L, request, "tecnico@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("mínimo 10 caracteres"));
        verifyNoInteractions(cambiosEstadoRepository);
    }

    @Test
    @DisplayName("resolveIncident: lanza IllegalStateException si estado previo no es IN_PROGRESS")
    void shouldThrowIllegalStateExceptionWhenResolvingNotInProgressIncident() {
        incident.setStatus(EstadoIncidencia.ASSIGNED);

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> triageService.resolveIncident(100L, new ResolveIncidentRequest("Notas suficientes"), "tecnico@urbanpulse.es"));
    }

    @Test
    @DisplayName("resolveIncident: administrador puede resolver incidencia sin requerir asignación técnica activa previa")
    void shouldResolveIncidentSuccessfullyByAdmin() {
        incident.setStatus(EstadoIncidencia.IN_PROGRESS);
        incident.setPriority(PrioridadIncidencia.HIGH);

        ResolveIncidentRequest request = ResolveIncidentRequest.builder()
                .resolutionNotes("Resolución directa ejecutada por el administrador municipal")
                .build();

        when(userRepository.findByEmail("admin@urbanpulse.es")).thenReturn(Optional.of(admin));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.resolveIncident(100L, request, "admin@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.RESOLVED, incident.getStatus());
        verify(tareasRepository, never()).existsByIncidentIdAndTechnicianIdAndIsActiveTrue(any(), any());
    }

    @Test
    @DisplayName("resolveIncident: lanza 403 Forbidden si el técnico no está asignado activamente")
    void shouldThrow403WhenTechnicianIsNotAssignedOnResolve() {
        incident.setStatus(EstadoIncidencia.IN_PROGRESS);

        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(100L, 3L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> triageService.resolveIncident(100L, new ResolveIncidentRequest("Reparación terminada"), "tecnico@urbanpulse.es"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No es un técnico asignado"));
        verifyNoInteractions(cambiosEstadoRepository);
    }

    // =========================================================================
    // 8. closeIncident (RF-17, RF-19)
    // =========================================================================

    @Test
    @DisplayName("closeIncident: cierra expediente resuelto, desactiva tareas e inserta auditoría (RF-17)")
    void shouldCloseIncidentSuccessfully() {
        incident.setStatus(EstadoIncidencia.RESOLVED);

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.closeIncident(100L, "operador@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.CLOSED, incident.getStatus());
        verify(assignmentService).deactivateAllActiveAssignments(100L);

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        assertEquals("RESOLVED", captor.getValue().getFromStatus());
        assertEquals("CLOSED", captor.getValue().getToStatus());
    }

    @Test
    @DisplayName("closeIncident: lanza 403 si el rol es TECHNICIAN o CITIZEN (RF-19)")
    void shouldThrow403InCloseIncidentForTechnician() {
        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));

        assertThrows(ResponseStatusException.class,
                () -> triageService.closeIncident(100L, "tecnico@urbanpulse.es"));
        verifyNoInteractions(assignmentService);
    }

    @Test
    @DisplayName("closeIncident: lanza IllegalStateException si estado no es RESOLVED")
    void shouldThrowIllegalStateExceptionWhenClosingNonResolvedIncident() {
        incident.setStatus(EstadoIncidencia.IN_PROGRESS);

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        assertThrows(IllegalStateException.class,
                () -> triageService.closeIncident(100L, "operador@urbanpulse.es"));
    }

    // =========================================================================
    // 9. reopenIncident (RF-18, RF-19)
    // =========================================================================

    @Test
    @DisplayName("reopenIncident: reabre expediente resuelto con motivo >= 5 caracteres e inserta auditoría (RF-18)")
    void shouldReopenIncidentSuccessfully() {
        incident.setStatus(EstadoIncidencia.RESOLVED);
        ReopenIncidentRequest request = ReopenIncidentRequest.builder()
                .reason("El bache vuelve a abrirse por lluvias recientes")
                .build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(incident)).thenReturn(incident);
        when(incidentMapper.toDto(incident)).thenReturn(incidentResponse);

        IncidentResponse result = triageService.reopenIncident(100L, request, "operador@urbanpulse.es");

        assertNotNull(result);
        assertEquals(EstadoIncidencia.REOPENED, incident.getStatus());

        ArgumentCaptor<CambiosEstado> captor = ArgumentCaptor.forClass(CambiosEstado.class);
        verify(cambiosEstadoRepository).save(captor.capture());
        CambiosEstado cambio = captor.getValue();
        assertEquals("RESOLVED", cambio.getFromStatus());
        assertEquals("REOPENED", cambio.getToStatus());
        assertEquals("El bache vuelve a abrirse por lluvias recientes", cambio.getReason());
    }

    @Test
    @DisplayName("reopenIncident: lanza IllegalArgumentException si motivo es menor a 5 caracteres")
    void shouldThrowIllegalArgumentExceptionWhenReopenReasonIsTooShort() {
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        ReopenIncidentRequest request = ReopenIncidentRequest.builder().reason("Mal").build();
        assertThrows(IllegalArgumentException.class,
                () -> triageService.reopenIncident(100L, request, "operador@urbanpulse.es"));

        verifyNoInteractions(incidenciaRepository);
    }

    @Test
    @DisplayName("reopenIncident: lanza 403 si el rol es TECHNICIAN o CITIZEN (RF-19)")
    void shouldThrow403InReopenIncidentForCitizen() {
        when(userRepository.findByEmail("ciudadano@urbanpulse.es")).thenReturn(Optional.of(citizen));

        ReopenIncidentRequest request = ReopenIncidentRequest.builder().reason("Motivo justificado").build();
        assertThrows(ResponseStatusException.class,
                () -> triageService.reopenIncident(100L, request, "ciudadano@urbanpulse.es"));
    }

    @Test
    @DisplayName("reopenIncident: lanza IllegalStateException si estado no es RESOLVED")
    void shouldThrowIllegalStateExceptionWhenReopeningNonResolvedIncident() {
        incident.setStatus(EstadoIncidencia.CLOSED);
        ReopenIncidentRequest request = ReopenIncidentRequest.builder().reason("Motivo justificado").build();

        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        assertThrows(IllegalStateException.class,
                () -> triageService.reopenIncident(100L, request, "operador@urbanpulse.es"));
    }
    @Test
    @DisplayName("validateIncident: envia notificacion al ciudadano y tecnicos activos")
    void shouldSendNotificationOnValidateIncident() {
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(incidenciaRepository.save(any(Incidencia.class))).thenReturn(incident);
        when(incidentMapper.toDto(any(Incidencia.class))).thenReturn(new IncidentResponse());

        Tareas activeTask = new Tareas();
        activeTask.setTechnician(technician);
        activeTask.setIsActive(true);
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L)).thenReturn(List.of(activeTask));

        triageService.validateIncident(100L, new ValidateIncidentRequest("OK"), "operador@urbanpulse.es");

        verify(notificationService).createNotification(eq(citizen), eq(incident), eq("STATUS_CHANGE"), anyString(), anyString());
        verify(notificationService).createNotification(eq(technician), eq(incident), eq("STATUS_CHANGE"), anyString(), anyString());
    }
}
