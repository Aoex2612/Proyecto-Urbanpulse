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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de negocio encargado de la gestión de asignaciones departamentales y técnicas de incidencias.
 * <p>
 * Coordina la vinculación multidepartamental (RF-8), la validación estricta de adscripción de técnicos
 * a departamentos activos (RF-9, RF-10), la desactivación lógica explícita (RF-11), la reversión automática
 * a estado validado cuando no restan áreas activas (RF-12) y la inactivación total para soporte de cierre o rechazo (RF-17).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class AssignmentService {

    private final TareasRepository tareasRepository;
    private final DepartamentoRepository departamentoRepository;
    private final UserRepository userRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final AssignmentMapper assignmentMapper;

    /**
     * Construye una nueva instancia de {@link AssignmentService} con sus dependencias requeridas.
     *
     * @param tareasRepository       Repositorio de persistencia de tareas y asignaciones.
     * @param departamentoRepository Repositorio de consulta de departamentos municipales.
     * @param userRepository         Repositorio de usuarios del sistema.
     * @param incidenciaRepository   Repositorio de gestión de incidencias urbanas.
     * @param assignmentMapper       Mapeador de conversión entre entidades {@link Tareas} y DTOs {@link AssignmentResponse}.
     */
    public AssignmentService(TareasRepository tareasRepository,
                             DepartamentoRepository departamentoRepository,
                             UserRepository userRepository,
                             IncidenciaRepository incidenciaRepository,
                             AssignmentMapper assignmentMapper) {
        this.tareasRepository = tareasRepository;
        this.departamentoRepository = departamentoRepository;
        this.userRepository = userRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.assignmentMapper = assignmentMapper;
    }

    /**
     * Asigna uno o varios departamentos municipales y técnicos específicos a una incidencia urbana.
     * <p>
     * Valida que la incidencia se encuentre en un estado asignable ({@code VALIDATED}, {@code ASSIGNED} o {@code REOPENED}),
     * que todos los técnicos pertenezcan al rol {@link RolUsuario#TECHNICIAN} y estén adscritos a uno de los
     * departamentos activos de la incidencia. Si la incidencia se encontraba en {@code VALIDATED} o {@code REOPENED},
     * transiciona automáticamente su estado a {@code ASSIGNED}.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con las listas de identificadores de departamentos y técnicos solicitados.
     * @param userEmail  Dirección de correo electrónico del usuario operador o administrador que autoriza la asignación.
     * @return Lista actualizada de asignaciones activas ({@link AssignmentResponse}) asociadas a la incidencia.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalArgumentException  si el estado no es asignable, faltan datos, los departamentos no existen,
     *                                   o algún técnico no tiene rol técnico o no pertenece a los departamentos activos.
     */
    @Transactional
    public List<AssignmentResponse> assignDepartmentsAndTechnicians(Long incidentId, AssignIncidentRequest request, String userEmail) {
        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        EstadoIncidencia currentStatus = incident.getStatus();
        if (currentStatus != EstadoIncidencia.VALIDATED
                && currentStatus != EstadoIncidencia.ASSIGNED
                && currentStatus != EstadoIncidencia.REOPENED) {
            throw new IllegalArgumentException("La incidencia en estado " + currentStatus + " no admite nuevas asignaciones");
        }

        Usuario assignedBy = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado: " + userEmail));

        if (request == null || request.getDepartmentIds() == null || request.getDepartmentIds().isEmpty()) {
            throw new IllegalArgumentException("Debe indicarse al menos un departamento para la asignación");
        }

        List<Departamento> requestedDepartments = departamentoRepository.findAllById(request.getDepartmentIds());
        if (requestedDepartments.size() != request.getDepartmentIds().size()) {
            throw new IllegalArgumentException("Uno o más departamentos indicados no existen en el sistema");
        }

        Map<Long, Departamento> departmentMap = requestedDepartments.stream()
                .collect(Collectors.toMap(Departamento::getId, Function.identity()));

        List<Tareas> currentActiveTasks = tareasRepository.findByIncidentIdAndIsActiveTrue(incidentId);
        Set<Long> activeDepartmentIds = currentActiveTasks.stream()
                .map(t -> t.getDepartment().getId())
                .collect(Collectors.toSet());
        activeDepartmentIds.addAll(request.getDepartmentIds());

        List<Usuario> techniciansToAssign = new ArrayList<>();
        if (request.getTechnicianIds() != null && !request.getTechnicianIds().isEmpty()) {
            for (Long techId : request.getTechnicianIds()) {
                Usuario tech = userRepository.findById(techId)
                        .orElseThrow(() -> new IllegalArgumentException("Técnico no encontrado con id: " + techId));

                if (tech.getRole() != RolUsuario.TECHNICIAN) {
                    throw new IllegalArgumentException("El usuario " + tech.getFullName() + " no posee el rol de técnico");
                }

                if (tech.getDepartment() == null || !activeDepartmentIds.contains(tech.getDepartment().getId())) {
                    throw new IllegalArgumentException("El técnico " + tech.getFullName()
                            + " no pertenece a ningún departamento activo asignado a la incidencia");
                }

                techniciansToAssign.add(tech);
            }
        }

        Set<Long> handledDepartmentIds = new HashSet<>();

        // Asignar los técnicos solicitados
        for (Usuario tech : techniciansToAssign) {
            boolean alreadyAssigned = currentActiveTasks.stream()
                    .anyMatch(t -> t.getTechnician() != null && t.getTechnician().getId().equals(tech.getId()));

            if (!alreadyAssigned) {
                Long techDeptId = tech.getDepartment().getId();
                // Buscar si existe una tarea departamental activa sin técnico que podamos completar
                Tareas unassignedTask = currentActiveTasks.stream()
                        .filter(t -> t.getDepartment().getId().equals(techDeptId) && t.getTechnician() == null)
                        .findFirst()
                        .orElse(null);

                if (unassignedTask != null) {
                    unassignedTask.setTechnician(tech);
                    unassignedTask.setAssignedBy(assignedBy);
                    unassignedTask.setAssignedAt(OffsetDateTime.now());
                    tareasRepository.save(unassignedTask);
                } else {
                    Departamento dept = departmentMap.getOrDefault(techDeptId, tech.getDepartment());
                    Tareas newTask = assignmentMapper.toEntity(incident, dept, tech, assignedBy);
                    tareasRepository.save(newTask);
                    currentActiveTasks.add(newTask);
                }
            }
            handledDepartmentIds.add(tech.getDepartment().getId());
        }

        // Para los departamentos solicitados que no tengan asignación activa, crear registro departamental
        for (Long deptId : request.getDepartmentIds()) {
            boolean deptHasActive = currentActiveTasks.stream()
                    .anyMatch(t -> t.getDepartment().getId().equals(deptId));

            if (!deptHasActive && !handledDepartmentIds.contains(deptId)) {
                Departamento dept = departmentMap.get(deptId);
                Tareas deptOnlyTask = assignmentMapper.toEntity(incident, dept, null, assignedBy);
                tareasRepository.save(deptOnlyTask);
                currentActiveTasks.add(deptOnlyTask);
            }
        }

        // Si la incidencia estaba en VALIDATED o REOPENED, transicionar a ASSIGNED
        if (currentStatus == EstadoIncidencia.VALIDATED || currentStatus == EstadoIncidencia.REOPENED) {
            incident.setStatus(EstadoIncidencia.ASSIGNED);
            incidenciaRepository.save(incident);
        }

        return getActiveAssignments(incidentId);
    }

    /**
     * Desactiva lógicamente asignaciones específicas (departamentos o técnicos) de una incidencia.
     * <p>
     * Marca {@code is_active = false} en los registros correspondientes (RF-11). Si tras la desactivación
     * ya no resta ningún departamento activo vinculado a la incidencia, revierte automáticamente
     * el estado de la incidencia a {@link EstadoIncidencia#VALIDATED} (RF-12).
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con los identificadores de departamentos y/o técnicos a desactivar.
     * @param userEmail  Dirección de correo electrónico del usuario que solicita la desactivación.
     * @return Lista actualizada de asignaciones activas resultantes tras la operación.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalArgumentException  si el usuario no existe o no se suministran criterios de desactivación.
     */
    @Transactional
    public List<AssignmentResponse> deactivateAssignments(Long incidentId, DeactivateAssignmentRequest request, String userEmail) {
        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado: " + userEmail));

        if (request == null
                || ((request.getDepartmentIds() == null || request.getDepartmentIds().isEmpty())
                && (request.getTechnicianIds() == null || request.getTechnicianIds().isEmpty()))) {
            throw new IllegalArgumentException("Debe especificar al menos un departamento o técnico para desactivar");
        }

        List<Tareas> activeTasks = tareasRepository.findByIncidentIdAndIsActiveTrue(incidentId);
        List<Tareas> toDeactivate = new ArrayList<>();

        for (Tareas task : activeTasks) {
            boolean matchesDepartment = request.getDepartmentIds() != null
                    && request.getDepartmentIds().contains(task.getDepartment().getId());

            boolean matchesTechnician = request.getTechnicianIds() != null
                    && task.getTechnician() != null
                    && request.getTechnicianIds().contains(task.getTechnician().getId());

            if (matchesDepartment || matchesTechnician) {
                task.setIsActive(false);
                toDeactivate.add(task);
            }
        }

        if (!toDeactivate.isEmpty()) {
            tareasRepository.saveAll(toDeactivate);
            tareasRepository.flush();
        }

        List<Tareas> remainingActiveTasks = tareasRepository.findByIncidentIdAndIsActiveTrue(incidentId);

        // Si ya no quedan asignaciones activas, revertir automáticamente a VALIDATED
        if (remainingActiveTasks.isEmpty() && incident.getStatus() == EstadoIncidencia.ASSIGNED) {
            incident.setStatus(EstadoIncidencia.VALIDATED);
            incidenciaRepository.save(incident);
        }

        return assignmentMapper.toDtoList(remainingActiveTasks);
    }

    /**
     * Desactiva de forma masiva la totalidad de asignaciones activas asociadas a una incidencia.
     * <p>
     * Utilizado internamente durante los flujos de rechazo definitivo (RF-5) o cierre de expediente (RF-17).
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @throws IncidentNotFoundException si la incidencia no existe.
     */
    @Transactional
    public void deactivateAllActiveAssignments(Long incidentId) {
        if (!incidenciaRepository.existsById(incidentId)) {
            throw new IncidentNotFoundException(incidentId);
        }

        List<Tareas> activeTasks = tareasRepository.findByIncidentIdAndIsActiveTrue(incidentId);
        if (!activeTasks.isEmpty()) {
            for (Tareas task : activeTasks) {
                task.setIsActive(false);
            }
            tareasRepository.saveAll(activeTasks);
            tareasRepository.flush();
        }
    }

    /**
     * Recupera la colección de asignaciones activas actualmente asociadas a una incidencia.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista de {@link AssignmentResponse} que representan las asignaciones con {@code is_active = true}.
     * @throws IncidentNotFoundException si la incidencia no existe.
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getActiveAssignments(Long incidentId) {
        if (!incidenciaRepository.existsById(incidentId)) {
            throw new IncidentNotFoundException(incidentId);
        }

        List<Tareas> activeTasks = tareasRepository.findByIncidentIdAndIsActiveTrue(incidentId);
        return assignmentMapper.toDtoList(activeTasks);
    }
}
