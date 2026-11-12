package org.bulbaseros.controller.v2;

import jakarta.validation.Valid;
import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.PageResponse;
import org.bulbaseros.dto.ReverseGeocodeResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.bulbaseros.dto.v2.AttachmentResponseV2;
import org.bulbaseros.dto.v2.IncidentAssetResponseV2;
import org.bulbaseros.dto.v2.IncidentResponseV2;
import org.bulbaseros.dto.v2.IncidentStatusResponseV2;
import org.bulbaseros.dto.v2.StatusChangeResponseV2;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.repository.UserRepository;
import org.bulbaseros.service.AttachmentService;
import org.bulbaseros.service.IncidentService;
import org.bulbaseros.service.MalagaOpenDataService;
import org.bulbaseros.service.UrbanAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestion de incidencias en la API v2 de UrbanPulse.
 * Opera exclusivamente con identificadores publicos universales (UUID v4),
 * protegiendo la identidad y metadatos internos de persistencia.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@RestController
@RequestMapping("/api/v2/incidents")
public class IncidentControllerV2 {

    private final IncidentService incidentService;
    private final AttachmentService attachmentService;
    private final UrbanAssetService urbanAssetService;
    private final MalagaOpenDataService malagaOpenDataService;
    private final UserRepository userRepository;

    @Autowired
    public IncidentControllerV2(IncidentService incidentService,
                                @Autowired(required = false) AttachmentService attachmentService,
                                @Autowired(required = false) UrbanAssetService urbanAssetService,
                                @Autowired(required = false) MalagaOpenDataService malagaOpenDataService,
                                @Autowired(required = false) UserRepository userRepository) {
        this.incidentService = incidentService;
        this.attachmentService = attachmentService;
        this.urbanAssetService = urbanAssetService;
        this.malagaOpenDataService = malagaOpenDataService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponseV2>> getActiveIncidents() {
        return ResponseEntity.ok(incidentService.getActiveIncidentsV2());
    }

    @GetMapping("/my-incidents")
    public ResponseEntity<PageResponse<IncidentResponseV2>> getMyIncidents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return ResponseEntity.ok(incidentService.getMyIncidentsV2(page, size, status, authentication.getName()));
    }

    @GetMapping("/geocode/reverse")
    public ResponseEntity<ReverseGeocodeResponse> reverseGeocode(
            @RequestParam(name = "lat") Double latitude,
            @RequestParam(name = "lon") Double longitude
    ) {
        if (malagaOpenDataService == null) {
            return ResponseEntity.ok(new ReverseGeocodeResponse(null, null, null));
        }
        return ResponseEntity.ok(malagaOpenDataService.resolveLocation(latitude, longitude));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponseV2> getIncidentById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentByPublicId(id));
    }

    @PostMapping
    public ResponseEntity<IncidentResponseV2> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        IncidentResponseV2 created = incidentService.createIncidentV2(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentStatusResponseV2> updateIncidentStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return ResponseEntity.ok(incidentService.updateIncidentStatusV2(id, request, authentication.getName()));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StatusChangeResponseV2>> getIncidentStatusHistory(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(incidentService.getIncidentStatusHistoryV2(id));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponseV2> uploadAttachment(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        if (attachmentService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de archivos no disponible");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.saveAttachmentV2(id, file));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponseV2>> getAttachments(@PathVariable("id") UUID id) {
        if (attachmentService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(attachmentService.getAttachmentsByIncidentPublicId(id));
    }

    @PostMapping("/{incidentId}/assets/{assetId}/confirm")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentAssetResponseV2> confirmAsset(
            @PathVariable("incidentId") UUID incidentId,
            @PathVariable("assetId") UUID assetId
    ) {
        if (urbanAssetService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de activos no disponible");
        }
        return ResponseEntity.ok(urbanAssetService.confirmAssetAssociationV2(incidentId, assetId));
    }

    @DeleteMapping("/{incidentId}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> removeAsset(
            @PathVariable("incidentId") UUID incidentId,
            @PathVariable("assetId") UUID assetId
    ) {
        if (urbanAssetService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de activos no disponible");
        }
        urbanAssetService.removeAssetAssociationV2(incidentId, assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<IncidentResponseV2>> search(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(value = "status", required = false) String statusStr,
            Authentication authentication
    ) {
        EstadoIncidencia status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = EstadoIncidencia.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        boolean includeStatus = true;
        if (authentication != null && userRepository != null) {
            Usuario user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null && user.getRole() == RolUsuario.CITIZEN) {
                includeStatus = false;
            }
        }

        return ResponseEntity.ok(incidentService.searchIncidentsV2(text, category, start, end, status, includeStatus));
    }
}
