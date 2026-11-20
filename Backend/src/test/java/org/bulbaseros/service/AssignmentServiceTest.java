package org.bulbaseros.service;

import org.bulbaseros.dto.AssignIncidentRequest;
import org.bulbaseros.dto.AssignmentResponse;
import org.bulbaseros.dto.DeactivateAssignmentRequest;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.mappers.AssignmentMapper;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.TareasRepository;
import org.bulbaseros.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link AssignmentService} con Mockito.
 * <p>
 * Verifica la asignación multidepartamental y técnica (RF-8, RF-9), la comprobación
 * estricta de pertenencia de técnicos a departamentos activos (RF-10), la desactivación lógica (RF-11),
 * la reversión automática de estado a {@link EstadoIncidencia#VALIDATED} ante desasignación total (RF-12)
 * y la inactivación total de asignaciones (RF-17).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private TareasRepository tareasRepository;

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private AssignmentMapper assignmentMapper;

    @InjectMocks
    private AssignmentService assignmentService;

    private Incidencia incident;
    private Departamento deptVias;
    private Departamento deptAlumbrado;
    private Usuario operator;
    private Usuario techVias;
    private Usuario techAlumbrado;
    private Usuario citizen;

    @BeforeEach
    void setUp() {
        deptVias = new Departamento(1L, "Vías Públicas", "Mantenimiento de calzadas");
        deptAlumbrado = new Departamento(2L, "Alumbrado", "Mantenimiento de farolas");

        operator = new Usuario("operador@urbanpulse.es", "hash", "Operador Juan", RolUsuario.OPERATOR);
        operator.setId(10L);

        techVias = new Usuario("tech.vias@urbanpulse.es", "hash", "Técnico Pedro", RolUsuario.TECHNICIAN, deptVias);
        techVias.setId(20L);

        techAlumbrado = new Usuario("tech.alumbrado@urbanpulse.es", "hash", "Técnico Luis", RolUsuario.TECHNICIAN, deptAlumbrado);
        techAlumbrado.setId(21L);

        citizen = new Usuario("ciudadano@urbanpulse.es", "hash", "Ciudadano Ana", RolUsuario.CITIZEN);
        citizen.setId(30L);

        incident = new Incidencia("Socavón en calle", "Bache profundo", "Vías Públicas", 36.72, -4.42, citizen);
        incident.setId(100L);
        incident.setStatus(EstadoIncidencia.VALIDATED);
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe asignar departamentos y técnicos y transicionar VALIDATED a ASSIGNED")
    void shouldAssignDepartmentsAndTechniciansAndTransitionToAssigned() {
        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L))
                .technicianIds(List.of(20L))
                .build();

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(departamentoRepository.findAllById(List.of(1L))).thenReturn(List.of(deptVias));
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L)).thenReturn(new ArrayList<>());
        when(userRepository.findById(20L)).thenReturn(Optional.of(techVias));

        Tareas createdTask = new Tareas(incident, deptVias, techVias, operator);
        createdTask.setId(1L);
        when(assignmentMapper.toEntity(incident, deptVias, techVias, operator)).thenReturn(createdTask);

        AssignmentResponse responseDto = AssignmentResponse.builder()
                .id(1L)
                .departmentId(1L)
                .departmentName("Vías Públicas")
                .technicianId(20L)
                .technicianName("Técnico Pedro")
                .isActive(true)
                .build();

        when(incidenciaRepository.existsById(100L)).thenReturn(true);
        when(assignmentMapper.toDtoList(anyList())).thenReturn(List.of(responseDto));

        List<AssignmentResponse> results = assignmentService.assignDepartmentsAndTechnicians(100L, request, "operador@urbanpulse.es");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(EstadoIncidencia.ASSIGNED, incident.getStatus());
        verify(incidenciaRepository).save(incident);
        verify(tareasRepository).save(createdTask);
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe transicionar REOPENED a ASSIGNED")
    void shouldTransitionReopenedToAssigned() {
        incident.setStatus(EstadoIncidencia.REOPENED);

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L))
                .build();

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(departamentoRepository.findAllById(List.of(1L))).thenReturn(List.of(deptVias));
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L)).thenReturn(new ArrayList<>());

        Tareas createdTask = new Tareas(incident, deptVias, null, operator);
        when(assignmentMapper.toEntity(incident, deptVias, null, operator)).thenReturn(createdTask);

        when(incidenciaRepository.existsById(100L)).thenReturn(true);
        when(assignmentMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());

        assignmentService.assignDepartmentsAndTechnicians(100L, request, "operador@urbanpulse.es");

        assertEquals(EstadoIncidencia.ASSIGNED, incident.getStatus());
        verify(incidenciaRepository).save(incident);
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe completar tarea departamental existente sin técnico")
    void shouldCompleteExistingUnassignedDepartmentTask() {
        incident.setStatus(EstadoIncidencia.ASSIGNED);

        Tareas existingDeptTask = new Tareas(incident, deptVias, null, operator);
        existingDeptTask.setId(5L);
        existingDeptTask.setIsActive(true);

        List<Tareas> activeList = new ArrayList<>(List.of(existingDeptTask));
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(departamentoRepository.findAllById(List.of(1L))).thenReturn(List.of(deptVias));
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L)).thenReturn(activeList);
        when(userRepository.findById(20L)).thenReturn(Optional.of(techVias));

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L))
                .technicianIds(List.of(20L))
                .build();

        when(incidenciaRepository.existsById(100L)).thenReturn(true);
        when(assignmentMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());

        assignmentService.assignDepartmentsAndTechnicians(100L, request, "operador@urbanpulse.es");

        assertEquals(techVias, existingDeptTask.getTechnician());
        verify(tareasRepository).save(existingDeptTask);
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe lanzar IncidentNotFoundException si la incidencia no existe")
    void shouldThrowExceptionWhenIncidentNotFound() {
        when(incidenciaRepository.findById(999L)).thenReturn(Optional.empty());

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L))
                .build();

        assertThrows(IncidentNotFoundException.class,
                () -> assignmentService.assignDepartmentsAndTechnicians(999L, request, "operador@urbanpulse.es"));
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe lanzar IllegalArgumentException si el estado no es asignable")
    void shouldThrowExceptionWhenStatusNotAssignable() {
        incident.setStatus(EstadoIncidencia.REPORTED);
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> assignmentService.assignDepartmentsAndTechnicians(100L, request, "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("no admite nuevas asignaciones"));
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe lanzar IllegalArgumentException si el usuario no existe")
    void shouldThrowExceptionWhenUserNotFound() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("desconocido@urbanpulse.es")).thenReturn(Optional.empty());

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> assignmentService.assignDepartmentsAndTechnicians(100L, request, "desconocido@urbanpulse.es"));
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe lanzar IllegalArgumentException si departmentIds es nulo o vacío")
    void shouldThrowExceptionWhenDepartmentIdsEmpty() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(Collections.emptyList())
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> assignmentService.assignDepartmentsAndTechnicians(100L, request, "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("Debe indicarse al menos un departamento"));
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe lanzar IllegalArgumentException si algún departamento no existe")
    void shouldThrowExceptionWhenDepartmentNotFound() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(departamentoRepository.findAllById(List.of(1L, 99L))).thenReturn(List.of(deptVias));

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L, 99L))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> assignmentService.assignDepartmentsAndTechnicians(100L, request, "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("no existen"));
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe lanzar IllegalArgumentException si usuario no tiene rol TECHNICIAN")
    void shouldThrowExceptionWhenUserNotTechnicianRole() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(departamentoRepository.findAllById(List.of(1L))).thenReturn(List.of(deptVias));
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L)).thenReturn(new ArrayList<>());
        when(userRepository.findById(30L)).thenReturn(Optional.of(citizen));

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L))
                .technicianIds(List.of(30L))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> assignmentService.assignDepartmentsAndTechnicians(100L, request, "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("no posee el rol de técnico"));
    }

    @Test
    @DisplayName("assignDepartmentsAndTechnicians debe lanzar IllegalArgumentException si técnico pertenece a departamento no activo (RF-10)")
    void shouldThrowExceptionWhenTechnicianDepartmentDoesNotMatchActiveDepartments() {
        // Solicitamos asignar departamento Vías (1L), pero el técnico pertenece a Alumbrado (2L)
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(departamentoRepository.findAllById(List.of(1L))).thenReturn(List.of(deptVias));
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L)).thenReturn(new ArrayList<>());
        when(userRepository.findById(21L)).thenReturn(Optional.of(techAlumbrado)); // Alumbrado, pero sólo Vías está activa

        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L))
                .technicianIds(List.of(21L))
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> assignmentService.assignDepartmentsAndTechnicians(100L, request, "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("no pertenece a ningún departamento activo"));
    }

    @Test
    @DisplayName("deactivateAssignments debe desactivar asignaciones por departamento y técnico")
    void shouldDeactivateAssignmentsMatchingCriteria() {
        incident.setStatus(EstadoIncidencia.ASSIGNED);

        Tareas task1 = new Tareas(incident, deptVias, techVias, operator);
        task1.setIsActive(true);

        Tareas task2 = new Tareas(incident, deptAlumbrado, techAlumbrado, operator);
        task2.setIsActive(true);

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L))
                .thenReturn(List.of(task1, task2)) // primera llamada
                .thenReturn(List.of(task2));        // llamada posterior tras desactivar task1

        DeactivateAssignmentRequest request = DeactivateAssignmentRequest.builder()
                .departmentIds(List.of(1L))
                .build();

        when(assignmentMapper.toDtoList(List.of(task2))).thenReturn(Collections.emptyList());

        assignmentService.deactivateAssignments(100L, request, "operador@urbanpulse.es");

        assertFalse(task1.getIsActive());
        verify(tareasRepository).saveAll(List.of(task1));
        // Al restar task2 activo, el estado debe mantenerse ASSIGNED
        assertEquals(EstadoIncidencia.ASSIGNED, incident.getStatus());
    }

    @Test
    @DisplayName("deactivateAssignments debe revertir estado a VALIDATED si no restan asignaciones activas (RF-12)")
    void shouldRevertToValidatedWhenNoActiveAssignmentsRemain() {
        incident.setStatus(EstadoIncidencia.ASSIGNED);

        Tareas task1 = new Tareas(incident, deptVias, techVias, operator);
        task1.setIsActive(true);

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L))
                .thenReturn(List.of(task1))
                .thenReturn(Collections.emptyList()); // Sin tareas restantes

        DeactivateAssignmentRequest request = DeactivateAssignmentRequest.builder()
                .technicianIds(List.of(20L))
                .build();

        when(assignmentMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        assignmentService.deactivateAssignments(100L, request, "operador@urbanpulse.es");

        assertFalse(task1.getIsActive());
        assertEquals(EstadoIncidencia.VALIDATED, incident.getStatus());
        verify(incidenciaRepository).save(incident);
    }

    @Test
    @DisplayName("deactivateAssignments debe lanzar IllegalArgumentException si no se especifican criterios")
    void shouldThrowExceptionWhenNoDeactivateCriteriaProvided() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        DeactivateAssignmentRequest request = DeactivateAssignmentRequest.builder().build();

        assertThrows(IllegalArgumentException.class,
                () -> assignmentService.deactivateAssignments(100L, request, "operador@urbanpulse.es"));
    }

    @Test
    @DisplayName("deactivateAllActiveAssignments debe desactivar todas las asignaciones activas (RF-17)")
    void shouldDeactivateAllActiveAssignments() {
        Tareas task1 = new Tareas(incident, deptVias, techVias, operator);
        task1.setIsActive(true);
        Tareas task2 = new Tareas(incident, deptAlumbrado, null, operator);
        task2.setIsActive(true);

        when(incidenciaRepository.existsById(100L)).thenReturn(true);
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L)).thenReturn(List.of(task1, task2));

        assignmentService.deactivateAllActiveAssignments(100L);

        assertFalse(task1.getIsActive());
        assertFalse(task2.getIsActive());
        verify(tareasRepository).saveAll(List.of(task1, task2));
        verify(tareasRepository).flush();
    }

    @Test
    @DisplayName("deactivateAllActiveAssignments debe lanzar IncidentNotFoundException si la incidencia no existe")
    void shouldThrowExceptionWhenIncidentNotFoundInDeactivateAll() {
        when(incidenciaRepository.existsById(999L)).thenReturn(false);

        assertThrows(IncidentNotFoundException.class,
                () -> assignmentService.deactivateAllActiveAssignments(999L));
    }

    @Test
    @DisplayName("getActiveAssignments debe devolver la lista mapeada de asignaciones activas")
    void shouldReturnActiveAssignments() {
        Tareas task1 = new Tareas(incident, deptVias, techVias, operator);
        task1.setIsActive(true);

        when(incidenciaRepository.existsById(100L)).thenReturn(true);
        when(tareasRepository.findByIncidentIdAndIsActiveTrue(100L)).thenReturn(List.of(task1));

        AssignmentResponse dto = AssignmentResponse.builder().id(1L).departmentName("Vías Públicas").build();
        when(assignmentMapper.toDtoList(List.of(task1))).thenReturn(List.of(dto));

        List<AssignmentResponse> results = assignmentService.getActiveAssignments(100L);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Vías Públicas", results.get(0).getDepartmentName());
    }
}
