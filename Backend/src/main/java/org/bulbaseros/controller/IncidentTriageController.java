package org.bulbaseros.controller;

import jakarta.validation.Valid;
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
import org.bulbaseros.dto.UpdateCommentRequest;
import org.bulbaseros.dto.UpdatePriorityRequest;
import org.bulbaseros.dto.ValidateIncidentRequest;
import org.bulbaseros.service.AssignmentService;
import org.bulbaseros.service.CommentService;
import org.bulbaseros.service.IncidentTriageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST que gestiona las operaciones de triaje operativo, ciclo de vida, asignaciones
 * multidepartamentales y notas colaborativas en el sistema UrbanPulse.
 * <p>
 * Centraliza los endpoints protegidos con seguridad basada en roles (RBAC) para el cálculo y fijación
 * de prioridad asistida, transiciones de estado formal (validación, rechazo, reevaluación, inicio de intervención,
 * resolución técnica, cierre administrativo y reapertura), asignación y desasignación lógica de departamentos
 * y técnicos, y el muro de comentarios internos y públicos.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentTriageController {

    private final IncidentTriageService incidentTriageService;
    private final AssignmentService assignmentService;
    private final CommentService commentService;

    /**
     * Construye una nueva instancia de {@link IncidentTriageController} inyectando los servicios de negocio requeridos.
     *
     * @param incidentTriageService Servicio de orquestación del ciclo de vida y triaje operativo.
     * @param assignmentService     Servicio de gestión de asignaciones multidepartamentales y técnicas.
     * @param commentService        Servicio de gestión de notas internas y comentarios colaborativos.
     */
    public IncidentTriageController(IncidentTriageService incidentTriageService,
                                    AssignmentService assignmentService,
                                    CommentService commentService) {
        this.incidentTriageService = incidentTriageService;
        this.assignmentService = assignmentService;
        this.commentService = commentService;
    }

    /**
     * Calcula la prioridad sugerida asistida para una incidencia según los factores de vía y riesgo de seguridad.
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO con los factores de corte de calle y nivel de riesgo evaluados.
     * @param authentication Contexto de autenticación del usuario solicitante.
     * @return {@link ResponseEntity} con código HTTP 200 OK y la respuesta de prioridad asistida {@link PriorityCalculationResponse}.
     */
    @PostMapping("/{id}/priority/calculate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<PriorityCalculationResponse> calculateAssistedPriority(
            @PathVariable("id") Long id,
            @Valid @RequestBody PriorityCalculationRequest request,
            Authentication authentication) {
        PriorityCalculationResponse response = incidentTriageService.calculateAssistedPriority(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza y fija la prioridad operativa definitiva y su justificación textual para una incidencia.
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO con la prioridad seleccionada y la justificación explicativa.
     * @param authentication Contexto de autenticación del usuario operador o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentResponse} actualizado.
     */
    @PutMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponse> updatePriority(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePriorityRequest request,
            Authentication authentication) {
        IncidentResponse response = incidentTriageService.updatePriority(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Valida formalmente una incidencia en estado de reporte inicial ({@code REPORTED} &rarr; {@code VALIDATED}).
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO opcional con observaciones o motivo de la validación.
     * @param authentication Contexto de autenticación del operador o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentResponse} actualizado.
     */
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponse> validateIncident(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid ValidateIncidentRequest request,
            Authentication authentication) {
        IncidentResponse response = incidentTriageService.validateIncident(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Rechaza una incidencia descartándola del circuito operativo con motivo justificado (mínimo 5 caracteres).
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO con el motivo obligatorio de rechazo.
     * @param authentication Contexto de autenticación del operador o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentResponse} en estado {@code REJECTED}.
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponse> rejectIncident(
            @PathVariable("id") Long id,
            @Valid @RequestBody RejectIncidentRequest request,
            Authentication authentication) {
        IncidentResponse response = incidentTriageService.rejectIncident(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Reevalúa una incidencia rechazada devolviéndola al estado {@code REPORTED} o {@code VALIDATED}.
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO con el estado destino y el motivo explicativo (mínimo 5 caracteres).
     * @param authentication Contexto de autenticación del operador o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentResponse} reactivado.
     */
    @PostMapping("/{id}/reevaluate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponse> reevaluateIncident(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReevaluateIncidentRequest request,
            Authentication authentication) {
        IncidentResponse response = incidentTriageService.reevaluateIncident(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Inicia los trabajos e intervenciones de campo sobre una incidencia asignada o reabierta.
     *
     * @param id             Identificador único de la incidencia.
     * @param authentication Contexto de autenticación del técnico asignado.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentResponse} en estado {@code IN_PROGRESS}.
     */
    @PostMapping("/{id}/start-work")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<IncidentResponse> startWork(
            @PathVariable("id") Long id,
            Authentication authentication) {
        IncidentResponse response = incidentTriageService.startIntervention(id, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Declara una incidencia como resuelta tras finalizar los trabajos técnicos de campo.
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO opcional con la memoria descriptiva de actuaciones realizadas.
     * @param authentication Contexto de autenticación del técnico asignado.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentResponse} en estado {@code RESOLVED}.
     */
    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<IncidentResponse> resolveIncident(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid ResolveIncidentRequest request,
            Authentication authentication) {
        IncidentResponse response = incidentTriageService.resolveIncident(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Cierra formal y administrativamente el expediente de una incidencia resuelta.
     *
     * @param id             Identificador único de la incidencia.
     * @param authentication Contexto de autenticación del operador o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentResponse} en estado {@code CLOSED}.
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponse> closeIncident(
            @PathVariable("id") Long id,
            Authentication authentication) {
        IncidentResponse response = incidentTriageService.closeIncident(id, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Reabre una incidencia previamente resuelta con motivo justificado (mínimo 5 caracteres).
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO con el motivo de reapertura.
     * @param authentication Contexto de autenticación del operador o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentResponse} en estado {@code REOPENED}.
     */
    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponse> reopenIncident(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReopenIncidentRequest request,
            Authentication authentication) {
        IncidentResponse response = incidentTriageService.reopenIncident(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Recupera el listado de asignaciones activas de una incidencia urbana.
     *
     * @param id Identificador único de la incidencia.
     * @return {@link ResponseEntity} con código HTTP 200 OK y la lista de asignaciones activas {@link AssignmentResponse}.
     */
    @GetMapping("/{id}/assignments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AssignmentResponse>> getIncidentAssignments(@PathVariable("id") Long id) {
        List<AssignmentResponse> response = assignmentService.getActiveAssignments(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Asigna uno o más departamentos y técnicos específicos a una incidencia urbana.
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO con las listas de departamentos y técnicos solicitados.
     * @param authentication Contexto de autenticación del operador o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y la lista de asignaciones activas {@link AssignmentResponse}.
     */
    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> assignDepartmentsAndTechnicians(
            @PathVariable("id") Long id,
            @Valid @RequestBody AssignIncidentRequest request,
            Authentication authentication) {
        List<AssignmentResponse> response = assignmentService.assignDepartmentsAndTechnicians(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Desactiva lógicamente asignaciones departamentales o técnicas específicas de una incidencia.
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO con los identificadores de departamentos o técnicos a desactivar.
     * @param authentication Contexto de autenticación del operador o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y la lista de asignaciones activas resultantes.
     */
    @PostMapping("/{id}/assignments/deactivate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> deactivateAssignments(
            @PathVariable("id") Long id,
            @Valid @RequestBody DeactivateAssignmentRequest request,
            Authentication authentication) {
        List<AssignmentResponse> response = assignmentService.deactivateAssignments(id, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Recupera el listado cronológico de notas y comentarios vinculados a una incidencia según los permisos del usuario.
     *
     * @param id             Identificador único de la incidencia.
     * @param authentication Contexto de autenticación del usuario solicitante.
     * @return {@link ResponseEntity} con código HTTP 200 OK y la lista de comentarios {@link CommentResponse}.
     */
    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable("id") Long id,
            Authentication authentication) {
        List<CommentResponse> response = commentService.getComments(id, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Da de alta un nuevo comentario o nota interna en el triaje de la incidencia.
     *
     * @param id             Identificador único de la incidencia.
     * @param request        DTO con el contenido del comentario y la indicación de si es interno.
     * @param authentication Contexto de autenticación del usuario emisor (operador, técnico o administrador).
     * @return {@link ResponseEntity} con código HTTP 201 CREATED y el comentario recién creado {@link CommentResponse}.
     */
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('OPERATOR', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        CommentResponse response = commentService.addComment(id, request, extractUserEmail(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza el contenido de un comentario existente verificando la autoría o privilegios administrativos.
     *
     * @param id             Identificador único de la incidencia.
     * @param commentId      Identificador único del comentario a editar.
     * @param request        DTO con el nuevo texto del comentario.
     * @param authentication Contexto de autenticación del autor o administrador.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el comentario actualizado {@link CommentResponse}.
     */
    @PutMapping("/{id}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable("id") Long id,
            @PathVariable("commentId") Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication) {
        CommentResponse response = commentService.updateComment(id, commentId, request, extractUserEmail(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Extrae de forma segura el correo electrónico del emisor a partir del contexto de autenticación.
     *
     * @param authentication Contexto de seguridad de Spring Security.
     * @return Correo electrónico del usuario autenticado, o {@code null} si no está autenticado.
     */
    private String extractUserEmail(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
