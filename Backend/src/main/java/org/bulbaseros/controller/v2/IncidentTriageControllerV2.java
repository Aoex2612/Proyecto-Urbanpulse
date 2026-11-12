package org.bulbaseros.controller.v2;

import jakarta.validation.Valid;
import org.bulbaseros.dto.*;
import org.bulbaseros.dto.v2.AssignIncidentRequestV2;
import org.bulbaseros.dto.v2.AssignmentResponseV2;
import org.bulbaseros.dto.v2.CommentResponseV2;
import org.bulbaseros.dto.v2.DeactivateAssignmentRequestV2;
import org.bulbaseros.dto.v2.IncidentResponseV2;
import org.bulbaseros.service.AssignmentService;
import org.bulbaseros.service.CommentService;
import org.bulbaseros.service.IncidentTriageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el flujo de triaje operativo de incidencias en API v2.
 * Gestiona transiciones de ciclo de vida, asignaciones y comentarios utilizando UUIDs publicos.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@RestController
@RequestMapping("/api/v2/incidents")
public class IncidentTriageControllerV2 {

    private final IncidentTriageService incidentTriageService;
    private final AssignmentService assignmentService;
    private final CommentService commentService;

    @Autowired
    public IncidentTriageControllerV2(IncidentTriageService incidentTriageService,
                                      @Autowired(required = false) AssignmentService assignmentService,
                                      @Autowired(required = false) CommentService commentService) {
        this.incidentTriageService = incidentTriageService;
        this.assignmentService = assignmentService;
        this.commentService = commentService;
    }

    @PostMapping("/{id}/priority/calculate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<PriorityCalculationResponse> calculatePriority(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PriorityCalculationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.calculatePriorityV2(id, request, authentication.getName()));
    }

    @PutMapping("/{id}/priority")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> updatePriority(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdatePriorityRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.updatePriorityV2(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> validateIncident(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) ValidateIncidentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.validateIncidentV2(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> rejectIncident(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RejectIncidentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.rejectIncidentV2(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/reevaluate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> reevaluateIncident(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReevaluateIncidentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.reevaluateIncidentV2(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/start-work")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> startWork(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.startWorkV2(id, authentication.getName()));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> resolveIncident(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ResolveIncidentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.resolveIncidentV2(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> closeIncident(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.closeIncidentV2(id, authentication.getName()));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> reopenIncident(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReopenIncidentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.reopenIncidentV2(id, request, authentication.getName()));
    }

    @GetMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN', 'TECHNICIAN')")
    public ResponseEntity<List<AssignmentResponseV2>> getAssignments(@PathVariable("id") UUID id) {
        if (assignmentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de asignaciones no disponible");
        }
        return ResponseEntity.ok(assignmentService.getActiveAssignmentsV2(id));
    }

    @PostMapping("/{id}/assignments")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<AssignmentResponseV2>> assignIncident(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AssignIncidentRequestV2 request,
            Authentication authentication
    ) {
        if (assignmentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de asignaciones no disponible");
        }
        return ResponseEntity.ok(assignmentService.assignDepartmentsAndTechniciansV2(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/assignments/deactivate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<List<AssignmentResponseV2>> deactivateAssignments(
            @PathVariable("id") UUID id,
            @Valid @RequestBody DeactivateAssignmentRequestV2 request,
            Authentication authentication
    ) {
        if (assignmentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de asignaciones no disponible");
        }
        return ResponseEntity.ok(assignmentService.deactivateAssignmentsV2(id, request, authentication.getName()));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentResponseV2>> getComments(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        if (commentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de comentarios no disponible");
        }
        return ResponseEntity.ok(commentService.getCommentsByIncidentPublicId(id, authentication));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN', 'TECHNICIAN')")
    public ResponseEntity<CommentResponseV2> addComment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        if (commentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de comentarios no disponible");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addCommentV2(id, request, authentication.getName()));
    }

    @PutMapping("/{id}/comments/{commentId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN', 'TECHNICIAN')")
    public ResponseEntity<CommentResponseV2> updateComment(
            @PathVariable("id") UUID id,
            @PathVariable("commentId") UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication
    ) {
        if (commentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de comentarios no disponible");
        }
        return ResponseEntity.ok(commentService.updateCommentV2(id, commentId, request, authentication.getName()));
    }

    @PostMapping("/{id}/mark-duplicate")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentResponseV2> markAsDuplicate(
            @PathVariable("id") UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(incidentTriageService.markIncidentAsDuplicateV2(id, authentication.getName()));
    }
}
