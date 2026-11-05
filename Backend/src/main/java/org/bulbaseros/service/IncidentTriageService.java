package org.bulbaseros.service;

import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.PriorityCalculationRequest;
import org.bulbaseros.dto.PriorityCalculationResponse;
import org.bulbaseros.dto.ReevaluateIncidentRequest;
import org.bulbaseros.dto.RejectIncidentRequest;
import org.bulbaseros.dto.ReopenIncidentRequest;
import org.bulbaseros.dto.ResolveIncidentRequest;
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
import org.bulbaseros.repository.TareasRepository;
import org.bulbaseros.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

/**
 * Servicio de orquestación del ciclo de vida, triaje operativo y auditoría de incidencias urbanas.
 * <p>
 * Centraliza las transiciones de estado legales ({@code REPORTED} &rarr; {@code VALIDATED} / {@code REJECTED},
 * {@code REJECTED} &rarr; {@code REPORTED} / {@code VALIDATED}, {@code ASSIGNED} / {@code REOPENED} &rarr; {@code IN_PROGRESS},
 * {@code IN_PROGRESS} &rarr; {@code RESOLVED}, y {@code RESOLVED} &rarr; {@code CLOSED} / {@code REOPENED}),
 * la priorización asistida y manual, y el registro inmutable de trazabilidad en {@link CambiosEstado}
 * garantizando control de acceso basado en roles (RBAC).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class IncidentTriageService {

    private final IncidenciaRepository incidenciaRepository;
    private final UserRepository userRepository;
    private final CambiosEstadoRepository cambiosEstadoRepository;
    private final TareasRepository tareasRepository;
    private final AssignmentService assignmentService;
    private final PriorityService priorityService;
    private final IncidentMapper incidentMapper;

    /**
     * Construye una nueva instancia de {@link IncidentTriageService} inyectando las dependencias requeridas.
     *
     * @param incidenciaRepository    Repositorio de persistencia y consulta de incidencias.
     * @param userRepository          Repositorio de usuarios del sistema.
     * @param cambiosEstadoRepository Repositorio de auditoría de transiciones de estado.
     * @param tareasRepository        Repositorio de asignaciones departamentales y técnicas.
     * @param assignmentService       Servicio de negocio para la gestión de asignaciones.
     * @param priorityService         Servicio del motor determinista de cálculo de prioridades asistidas.
     * @param incidentMapper          Mapeador de conversión entre {@link Incidencia} e {@link IncidentResponse}.
     */
    public IncidentTriageService(IncidenciaRepository incidenciaRepository,
                                 UserRepository userRepository,
                                 CambiosEstadoRepository cambiosEstadoRepository,
                                 TareasRepository tareasRepository,
                                 AssignmentService assignmentService,
                                 PriorityService priorityService,
                                 IncidentMapper incidentMapper) {
        this.incidenciaRepository = incidenciaRepository;
        this.userRepository = userRepository;
        this.cambiosEstadoRepository = cambiosEstadoRepository;
        this.tareasRepository = tareasRepository;
        this.assignmentService = assignmentService;
        this.priorityService = priorityService;
        this.incidentMapper = incidentMapper;
    }

    /**
     * Calcula la prioridad sugerida asistida para una incidencia a partir de los factores de corte de vía y riesgo de seguridad.
     * <p>
     * Exige que el usuario disponga de rol {@link RolUsuario#OPERATOR} o {@link RolUsuario#ADMIN} (RF-3).
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con el indicador de corte de calle y el nivel de seguridad.
     * @param userEmail  Dirección de correo electrónico del usuario autenticado solicitante.
     * @return DTO {@link PriorityCalculationResponse} con la prioridad recomendada y la justificación explicativa.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario no tiene rol {@code OPERATOR} ni {@code ADMIN}.
     * @throws IllegalArgumentException  si los parámetros son inválidos.
     */
    @Transactional(readOnly = true)
    public PriorityCalculationResponse calculateAssistedPriority(Long incidentId, PriorityCalculationRequest request, String userEmail) {
        validateOperatorOrAdmin(userEmail);

        if (!incidenciaRepository.existsById(incidentId)) {
            throw new IncidentNotFoundException(incidentId);
        }

        return priorityService.calculateAssistedPriority(request);
    }

    /**
     * Actualiza y fija la prioridad operativa definitiva y su justificación descriptiva en una incidencia urbana.
     * <p>
     * Valida que el usuario tenga rol {@link RolUsuario#OPERATOR} o {@link RolUsuario#ADMIN} (RF-2, RF-3).
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con la prioridad asignada y la justificación textual.
     * @param userEmail  Dirección de correo electrónico del usuario autenticado que autoriza la asignación.
     * @return DTO {@link IncidentResponse} con los datos actualizados de la incidencia.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario carece de permisos de operador o administrador.
     * @throws IllegalArgumentException  si la prioridad especificada es nula.
     */
    @Transactional
    public IncidentResponse updatePriority(Long incidentId, UpdatePriorityRequest request, String userEmail) {
        validateOperatorOrAdmin(userEmail);

        if (request == null || request.getPriority() == null) {
            throw new IllegalArgumentException("La prioridad de la incidencia es obligatoria");
        }

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        incident.setPriority(request.getPriority());
        incident.setPriorityReason(request.getPriorityReason());
        incident.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incident);
        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Valida formalmente una incidencia en estado de reporte inicial ({@code REPORTED} &rarr; {@code VALIDATED}).
     * <p>
     * Registra el hito en {@link CambiosEstado} para auditoría operativa (RF-4).
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO opcional con observaciones o motivo de la validación.
     * @param userEmail  Dirección de correo electrónico del operador o administrador autenticado.
     * @return DTO {@link IncidentResponse} reflejando el nuevo estado {@code VALIDATED}.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalStateException     si la incidencia no se encuentra en estado {@code REPORTED}.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario no tiene permisos de operador o administrador.
     */
    @Transactional
    public IncidentResponse validateIncident(Long incidentId, ValidateIncidentRequest request, String userEmail) {
        Usuario user = validateOperatorOrAdmin(userEmail);

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        if (incident.getStatus() != EstadoIncidencia.REPORTED) {
            throw new IllegalStateException("Solo se pueden validar incidencias en estado REPORTED. Estado actual: " + incident.getStatus());
        }

        String fromStatus = incident.getStatus().name();
        incident.setStatus(EstadoIncidencia.VALIDATED);
        incident.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incident);

        String reason = (request != null && request.getReason() != null && !request.getReason().trim().isEmpty())
                ? request.getReason().trim()
                : null;

        recordStatusChange(savedIncident, fromStatus, EstadoIncidencia.VALIDATED.name(), user, reason);

        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Rechaza una incidencia descartándola del circuito operativo ({@code REPORTED} u otro estado no terminal &rarr; {@code REJECTED}).
     * <p>
     * Exige un motivo obligatorio de al menos 5 caracteres (RF-6), desactiva todas las asignaciones activas
     * preexistentes (RF-5) y registra el evento en auditoría.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con el motivo obligatorio de rechazo.
     * @param userEmail  Dirección de correo electrónico del operador o administrador autenticado.
     * @return DTO {@link IncidentResponse} reflejando el nuevo estado {@code REJECTED}.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalStateException     si la incidencia ya se encuentra en un estado terminal ({@code REJECTED} o {@code CLOSED}).
     * @throws IllegalArgumentException  si el motivo de rechazo es nulo o tiene menos de 5 caracteres no vacíos.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario no tiene permisos de operador o administrador.
     */
    @Transactional
    public IncidentResponse rejectIncident(Long incidentId, RejectIncidentRequest request, String userEmail) {
        Usuario user = validateOperatorOrAdmin(userEmail);

        if (request == null || request.getReason() == null || request.getReason().trim().length() < 5) {
            throw new IllegalArgumentException("El motivo de rechazo es obligatorio y debe tener al menos 5 caracteres");
        }

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        if (incident.getStatus() == EstadoIncidencia.REJECTED || incident.getStatus() == EstadoIncidencia.CLOSED) {
            throw new IllegalStateException("No se puede rechazar una incidencia que ya se encuentra en estado " + incident.getStatus());
        }

        assignmentService.deactivateAllActiveAssignments(incidentId);

        String fromStatus = incident.getStatus().name();
        incident.setStatus(EstadoIncidencia.REJECTED);
        incident.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incident);

        recordStatusChange(savedIncident, fromStatus, EstadoIncidencia.REJECTED.name(), user, request.getReason().trim());

        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Reevalúa una incidencia previamente rechazada devolviéndola al flujo operativo (RF-7).
     * <p>
     * Restringe estrictamente el estado de destino a {@link EstadoIncidencia#REPORTED} o {@link EstadoIncidencia#VALIDATED},
     * requiriendo justificación de al menos 5 caracteres y registro en auditoría.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con el estado destino y el motivo explicativo.
     * @param userEmail  Dirección de correo electrónico del operador o administrador autenticado.
     * @return DTO {@link IncidentResponse} con la incidencia reactivada en el estado solicitado.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalStateException     si la incidencia no se encuentra actualmente en estado {@code REJECTED}.
     * @throws IllegalArgumentException  si el estado destino no es {@code REPORTED} o {@code VALIDATED}, o el motivo tiene menos de 5 caracteres.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario no tiene permisos de operador o administrador.
     */
    @Transactional
    public IncidentResponse reevaluateIncident(Long incidentId, ReevaluateIncidentRequest request, String userEmail) {
        Usuario user = validateOperatorOrAdmin(userEmail);

        if (request == null || request.getTargetStatus() == null || !request.isValidTargetStatus()) {
            throw new IllegalArgumentException("El estado destino en la reevaluación debe ser estrictamente REPORTED o VALIDATED");
        }

        if (request.getReason() == null || request.getReason().trim().length() < 5) {
            throw new IllegalArgumentException("El motivo de reevaluación es obligatorio y debe tener al menos 5 caracteres");
        }

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        if (incident.getStatus() != EstadoIncidencia.REJECTED) {
            throw new IllegalStateException("Solo se pueden reevaluar incidencias en estado REJECTED. Estado actual: " + incident.getStatus());
        }

        String fromStatus = incident.getStatus().name();
        incident.setStatus(request.getTargetStatus());
        incident.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incident);

        recordStatusChange(savedIncident, fromStatus, request.getTargetStatus().name(), user, request.getReason().trim());

        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Inicia los trabajos e intervenciones de campo sobre una incidencia asignada o reabierta (RF-13).
     * <p>
     * Valida que el usuario sea un técnico asignado activamente a la incidencia (RF-14) o un administrador,
     * transicionando el estado a {@code IN_PROGRESS} y registrando la auditoría correspondiente.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param userEmail  Dirección de correo electrónico del técnico o administrador autenticado.
     * @return DTO {@link IncidentResponse} reflejando el nuevo estado {@code IN_PROGRESS}.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalStateException     si la incidencia no se encuentra en estado {@code ASSIGNED} ni {@code REOPENED}.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario no es un técnico asignado activo ni administrador.
     */
    @Transactional
    public IncidentResponse startIntervention(Long incidentId, String userEmail) {
        Usuario user = validateTechnicianOrAdmin(userEmail);

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        validateTechnicianAssignment(incidentId, user);

        if (incident.getStatus() != EstadoIncidencia.ASSIGNED && incident.getStatus() != EstadoIncidencia.REOPENED) {
            throw new IllegalStateException("Solo se puede iniciar intervención en incidencias en estado ASSIGNED o REOPENED. Estado actual: " + incident.getStatus());
        }

        String fromStatus = incident.getStatus().name();
        incident.setStatus(EstadoIncidencia.IN_PROGRESS);
        incident.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incident);

        recordStatusChange(savedIncident, fromStatus, EstadoIncidencia.IN_PROGRESS.name(), user, "Inicio de intervención operativa técnica");

        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Declara una incidencia como técnicamente resuelta tras concluir los trabajos de campo (RF-15).
     * <p>
     * Valida que el usuario sea el técnico asignado activo o un administrador (RF-14), que el estado actual sea {@code IN_PROGRESS},
     * y exige una descripción detallada de actuaciones (&ge; 10 caracteres) para prioridades no menores ({@code MEDIUM}, {@code HIGH}, {@code CRITICAL}) (RF-16).
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con la descripción de actuaciones realizadas.
     * @param userEmail  Dirección de correo electrónico del técnico o administrador autenticado.
     * @return DTO {@link IncidentResponse} reflejando el nuevo estado {@code RESOLVED}.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalStateException     si la incidencia no se encuentra en estado {@code IN_PROGRESS}.
     * @throws IllegalArgumentException  si la descripción de resolución es obligatoria y no alcanza los 10 caracteres no vacíos.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario no es un técnico asignado activo ni administrador.
     */
    @Transactional
    public IncidentResponse resolveIncident(Long incidentId, ResolveIncidentRequest request, String userEmail) {
        Usuario user = validateTechnicianOrAdmin(userEmail);

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        validateTechnicianAssignment(incidentId, user);

        if (incident.getStatus() != EstadoIncidencia.IN_PROGRESS) {
            throw new IllegalStateException("Solo se pueden resolver incidencias en estado IN_PROGRESS. Estado actual: " + incident.getStatus());
        }

        String notes = (request != null && request.getResolutionNotes() != null)
                ? request.getResolutionNotes().trim()
                : "";

        if (incident.getPriority() != PrioridadIncidencia.LOW && notes.length() < 10) {
            throw new IllegalArgumentException("La descripción de resolución es obligatoria (mínimo 10 caracteres) para incidencias de prioridad " + incident.getPriority());
        }

        String fromStatus = incident.getStatus().name();
        incident.setStatus(EstadoIncidencia.RESOLVED);
        incident.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incident);

        String reason = notes.isEmpty() ? null : notes;
        recordStatusChange(savedIncident, fromStatus, EstadoIncidencia.RESOLVED.name(), user, reason);

        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Cierra definitivamente el expediente administrativo de una incidencia resuelta (RF-17).
     * <p>
     * Exige rol {@link RolUsuario#OPERATOR} o {@link RolUsuario#ADMIN} (RF-19), desactiva todas las asignaciones
     * activas de forma automática y registra el cierre en auditoría.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param userEmail  Dirección de correo electrónico del operador o administrador autenticado.
     * @return DTO {@link IncidentResponse} reflejando el estado definitivo {@code CLOSED}.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalStateException     si la incidencia no se encuentra en estado {@code RESOLVED}.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario carece de permisos de operador o administrador.
     */
    @Transactional
    public IncidentResponse closeIncident(Long incidentId, String userEmail) {
        Usuario user = validateOperatorOrAdmin(userEmail);

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        if (incident.getStatus() != EstadoIncidencia.RESOLVED) {
            throw new IllegalStateException("Solo se pueden cerrar incidencias en estado RESOLVED. Estado actual: " + incident.getStatus());
        }

        assignmentService.deactivateAllActiveAssignments(incidentId);

        String fromStatus = incident.getStatus().name();
        incident.setStatus(EstadoIncidencia.CLOSED);
        incident.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incident);

        recordStatusChange(savedIncident, fromStatus, EstadoIncidencia.CLOSED.name(), user, "Cierre definitivo del expediente tras verificar resolución");

        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Reabre una incidencia resuelta debido a actuaciones insatisfactorias o persistencia del problema (RF-18).
     * <p>
     * Exige rol {@link RolUsuario#OPERATOR} o {@link RolUsuario#ADMIN} (RF-19) y un motivo de reapertura
     * justificado de al menos 5 caracteres no vacíos.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con el motivo obligatorio de reapertura.
     * @param userEmail  Dirección de correo electrónico del operador o administrador autenticado.
     * @return DTO {@link IncidentResponse} reflejando el nuevo estado operativo {@code REOPENED}.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalStateException     si la incidencia no se encuentra en estado {@code RESOLVED}.
     * @throws IllegalArgumentException  si el motivo de reapertura es nulo o tiene menos de 5 caracteres no vacíos.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario carece de permisos de operador o administrador.
     */
    @Transactional
    public IncidentResponse reopenIncident(Long incidentId, ReopenIncidentRequest request, String userEmail) {
        Usuario user = validateOperatorOrAdmin(userEmail);

        if (request == null || request.getReason() == null || request.getReason().trim().length() < 5) {
            throw new IllegalArgumentException("El motivo de reapertura es obligatorio y debe tener al menos 5 caracteres");
        }

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        if (incident.getStatus() != EstadoIncidencia.RESOLVED) {
            throw new IllegalStateException("Solo se pueden reabrir incidencias en estado RESOLVED. Estado actual: " + incident.getStatus());
        }

        String fromStatus = incident.getStatus().name();
        incident.setStatus(EstadoIncidencia.REOPENED);
        incident.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incident);

        recordStatusChange(savedIncident, fromStatus, EstadoIncidencia.REOPENED.name(), user, request.getReason().trim());

        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Registra un cambio de estado en la auditoría inmutable {@link CambiosEstado}.
     *
     * @param incident   Incidencia asociada a la transición.
     * @param fromStatus Estado inicial.
     * @param toStatus   Estado final.
     * @param user       Usuario responsable del cambio.
     * @param reason     Motivo o justificativo del cambio.
     */
    private void recordStatusChange(Incidencia incident, String fromStatus, String toStatus, Usuario user, String reason) {
        CambiosEstado cambio = new CambiosEstado(incident, fromStatus, toStatus, user, reason);
        cambio.setCreatedAt(OffsetDateTime.now());
        cambiosEstadoRepository.save(cambio);
    }

    /**
     * Valida que el usuario exista y posea rol de operador o administrador.
     *
     * @param userEmail Correo del usuario autenticado.
     * @return Entidad {@link Usuario} autenticada.
     * @throws ResponseStatusException con código 403 si no existe o no tiene los roles permitidos.
     */
    private Usuario validateOperatorOrAdmin(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El correo electrónico del usuario es obligatorio");
        }
        Usuario user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario autenticado no encontrado: " + userEmail));

        if (user.getRole() != RolUsuario.OPERATOR && user.getRole() != RolUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: se requiere rol OPERATOR o ADMIN");
        }
        return user;
    }

    /**
     * Valida que el usuario exista y posea rol de técnico o administrador.
     *
     * @param userEmail Correo del usuario autenticado.
     * @return Entidad {@link Usuario} autenticada.
     * @throws ResponseStatusException con código 403 si no existe o no tiene los roles permitidos.
     */
    private Usuario validateTechnicianOrAdmin(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El correo electrónico del usuario es obligatorio");
        }
        Usuario user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario autenticado no encontrado: " + userEmail));

        if (user.getRole() != RolUsuario.TECHNICIAN && user.getRole() != RolUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: se requiere rol TECHNICIAN o ADMIN");
        }
        return user;
    }

    /**
     * Verifica que si el usuario es técnico, se encuentre activamente asignado a la incidencia.
     *
     * @param incidentId Identificador único de la incidencia.
     * @param user       Usuario técnico o administrador.
     * @throws ResponseStatusException con código 403 si el técnico no está asignado activamente a la incidencia.
     */
    private void validateTechnicianAssignment(Long incidentId, Usuario user) {
        if (user.getRole() == RolUsuario.TECHNICIAN) {
            boolean isAssigned = tareasRepository.existsByIncidentIdAndTechnicianIdAndIsActiveTrue(incidentId, user.getId());
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No es un técnico asignado a esta incidencia");
            }
        }
    }
}
