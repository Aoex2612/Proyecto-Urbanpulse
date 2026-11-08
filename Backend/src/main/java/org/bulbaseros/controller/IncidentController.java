package org.bulbaseros.controller;

import jakarta.validation.Valid;
import org.bulbaseros.dto.AttachmentResponse;
import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.IncidentAssetResponse;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.IncidentStatusResponse;
import org.bulbaseros.dto.PageResponse;
import org.bulbaseros.dto.ReverseGeocodeResponse;
import org.bulbaseros.dto.StatusChangeResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.repository.UserRepository;
import org.bulbaseros.service.AttachmentService;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.OffsetDateTime;
import org.bulbaseros.service.IncidentService;
import org.bulbaseros.service.UrbanAssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Controlador REST que gestiona las operaciones del ciclo de vida, auditoria, adjuntos multimedia
 * y gestión de activos urbanos asociados a incidencias en UrbanPulse.
 * <p>
 * Expone endpoints protegidos mediante RBAC (Role-Based Access Control) para la consulta cartografica
 * de incidencias activas, obtencion del detalle individual, alta de reportes ciudadanos, triaje operativo
 * de estados con auditoria inmutable, inspeccion cronologica de transiciones para administradores,
 * subida/consulta de archivos multimedia adjuntos (imágenes JPEG y PNG) y confirmación/desvinculación
 * de elementos de equipamiento urbano (semáforos, fuentes, contenedores).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.3
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    /**
     * Servicio de logica de negocio transaccional para la gestion de incidencias y auditoria.
     */
    private final IncidentService incidentService;

    /**
     * Servicio de logica de negocio para la gestion y almacenamiento de archivos adjuntos.
     */
    private final AttachmentService attachmentService;
    private final UserRepository userRepository;

    /**
     * Servicio de logica de negocio espacial y gestion de activos urbanos.
     */
    private final UrbanAssetService urbanAssetService;

    /**
     * Constructor para compatibilidad con pruebas unitarias que no requieran servicio de adjuntos ni de activos.
     *
     * @param incidentService Servicio de logica de negocio transaccional de incidencias.
     */
    public IncidentController(IncidentService incidentService) {
        this(incidentService, null, null, null);
    }

    /**
     * Constructor con inyección de dependencias para el controlador de incidencias y servicio de adjuntos.
     *
     * @param incidentService   Servicio de logica de negocio transaccional de incidencias.
     * @param attachmentService Servicio de almacenamiento y gestion de archivos adjuntos.
     */
    public IncidentController(IncidentService incidentService, AttachmentService attachmentService) {
        this(incidentService, attachmentService, null, null);
    }

    /**
     * Constructor con inyección de dependencias para pruebas unitarias con activos urbanos.
     *
     * @param incidentService   Servicio de logica de negocio transaccional de incidencias.
     * @param attachmentService Servicio de almacenamiento y gestion de archivos adjuntos.
     * @param urbanAssetService Servicio de gestion espacial y vinculación de activos urbanos.
     */
    public IncidentController(IncidentService incidentService, AttachmentService attachmentService, UrbanAssetService urbanAssetService) {
        this(incidentService, attachmentService, urbanAssetService, null);
    }

    /**
     * Constructor principal con inyeccion de dependencias para el controlador de incidencias,
     * almacenamiento de adjuntos, gestión espacial de activos urbanos y repositorio de usuarios.
     *
     * @param incidentService   Servicio de logica de negocio transaccional de incidencias.
     * @param attachmentService Servicio de almacenamiento y gestion de archivos adjuntos.
     * @param urbanAssetService Servicio de gestion espacial y vinculación de activos urbanos.
     * @param userRepository    Repositorio de usuarios para control de roles en búsqueda.
     */
    @Autowired
    public IncidentController(
            IncidentService incidentService,
            @Autowired(required = false) AttachmentService attachmentService,
            @Autowired(required = false) UrbanAssetService urbanAssetService,
            @Autowired(required = false) UserRepository userRepository
    ) {
        this.incidentService = incidentService;
        this.attachmentService = attachmentService;
        this.urbanAssetService = urbanAssetService;
        this.userRepository = userRepository;
    }

    /**
     * Recupera todas las incidencias urbanas activas en la plataforma para su representacion en el mapa interactivo.
     * <p>
     * Excluye del resultado los estados finales de ciclo de vida (CLOSED y REJECTED).
     * Aplica segregación de incidencias según el perfil autenticado: si es un técnico en campo (TECHNICIAN),
     * devuelve exclusivamente aquellas donde posea una asignación técnica activa (RF-25).
     * Para operadores, administradores o ciudadanos, devuelve la totalidad de las incidencias activas.
     * </p>
     *
     * @param authentication Contexto de autenticación de Spring Security para determinar la identidad y rol del usuario.
     * @return {@link ResponseEntity} con codigo HTTP 200 OK y la lista de {@link IncidentResponse} activas.
     */
    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getActiveIncidents(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        List<IncidentResponse> incidents = incidentService.getActiveIncidents(userEmail);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Recupera el listado paginado de incidencias urbanas reportadas por el ciudadano autenticado.
     * <p>
     * Permite filtrar opcionalmente por el estado del ciclo de vida ({@link EstadoIncidencia}).
     * Si el parámetro {@code status} se omite, está vacío o equivale a "TODOS" (ignorando mayúsculas/minúsculas),
     * no se aplica filtro de estado y se recuperan todos los avisos del emisor.
     * Extrae el correo electrónico del emisor a partir del token JWT contenido en {@link Authentication}.
     * Aplica ordenación cronológica descendente por fecha de creación por defecto.
     * </p>
     *
     * @param status         Filtro opcional por estado del ciclo de vida (o "TODOS" para consultar todos los avisos).
     * @param page           Número de página basado en índice cero (por defecto 0, mínimo 0).
     * @param size           Tamaño de página o cantidad de registros por página (por defecto 10, mínimo 1, máximo 50).
     * @param authentication Contexto de autenticación de Spring Security para extraer la identidad del emisor.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el cuerpo {@link PageResponse} estructurado.
     */
    @GetMapping("/my-incidents")
    public ResponseEntity<PageResponse<IncidentResponse>> getMyIncidents(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication authentication
    ) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El parámetro 'page' no puede ser negativo");
        }
        if (size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El parámetro 'size' debe estar comprendido entre 1 y 50");
        }

        EstadoIncidencia estadoFilter = null;
        if (status != null && !status.trim().isEmpty() && !"TODOS".equalsIgnoreCase(status.trim())) {
            try {
                estadoFilter = EstadoIncidencia.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de incidencia no válido: " + status);
            }
        }

        String userEmail = authentication != null ? authentication.getName() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<IncidentResponse> pageResponse = incidentService.getMyIncidents(userEmail, estadoFilter, pageable);
        return ResponseEntity.ok(pageResponse);
    }

    /**
     * Resuelve territorialmente la dirección postal, distrito y barrio asociados a un par de coordenadas geográficas.
     * <p>
     * Endpoint protegido que requiere autenticación previa. Valida que las coordenadas pertenezcan al
     * término municipal de Málaga (latitud [36.5000, 36.9000], longitud [-4.7000, -4.2000]).
     * Si las coordenadas son nulas o exceden dichos límites, responde con HTTP 400 Bad Request.
     * En caso de indisponibilidad o degradación controlada de los datos abiertos, retorna campos nulos.
     * </p>
     *
     * @param latitude  Coordenada de latitud en grados decimales (WGS84).
     * @param longitude Coordenada de longitud en grados decimales (WGS84).
     * @return {@link ResponseEntity} con código HTTP 200 OK y el cuerpo {@link ReverseGeocodeResponse}.
     */
    @GetMapping("/geocode/reverse")
    public ResponseEntity<ReverseGeocodeResponse> reverseGeocode(
            @RequestParam(name = "latitude", required = false) Double latitude,
            @RequestParam(name = "longitude", required = false) Double longitude
    ) {
        ReverseGeocodeResponse response = incidentService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene la informacion detallada de una incidencia especifica a partir de su identificador unico.
     * <p>
     * Requiere que el usuario este debidamente autenticado en la plataforma.
     * Retorna HTTP 404 Not Found si la incidencia no existe en la base de datos.
     * </p>
     *
     * @param id Identificador numerico unico de la incidencia a recuperar.
     * @return {@link ResponseEntity} con codigo HTTP 200 OK y el cuerpo {@link IncidentResponse}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable Long id) {
        IncidentResponse incident = incidentService.getIncidentById(id);
        return ResponseEntity.ok(incident);
    }

    /**
     * Registra una nueva incidencia urbana en el sistema asociandola al ciudadano autenticado.
     * <p>
     * Extrae el correo electronico del emisor desde el token JWT en {@link Authentication},
     * inicializa los valores por defecto del ciclo de vida y persiste la entidad en base de datos.
     * </p>
     *
     * @param request        Datos de la solicitud validados con Bean Validation (titulo, descripcion, categoria y coordenadas).
     * @param authentication Objeto de autenticacion de Spring Security para extraer la identidad del emisor.
     * @return {@link ResponseEntity} con codigo HTTP 201 Created y el cuerpo {@link IncidentResponse} creado.
     */
    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication != null ? authentication.getName() : null;
        IncidentResponse response = incidentService.createIncident(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza el estado de una incidencia en el flujo de triaje operativo y registra la transicion de auditoria.
     * <p>
     * Endpoint protegido cuyo acceso perimetral esta restringido exclusivamente a usuarios con rol
     * {@code OPERATOR} o {@code ADMIN}. Ambas operaciones se ejecutan de manera transaccional y atomica.
     * </p>
     *
     * @param id             Identificador numerico unico de la incidencia a modificar.
     * @param request        Datos validados de la solicitud con el nuevo estado y comentarios opcionales.
     * @param authentication Objeto de autenticacion de Spring Security para identificar al operador o administrador actor.
     * @return {@link ResponseEntity} con codigo HTTP 200 OK y el cuerpo {@link IncidentStatusResponse}.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentStatusResponse> updateIncidentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            Authentication authentication
    ) {
        String actorEmail = authentication != null ? authentication.getName() : null;
        IncidentStatusResponse response = incidentService.updateIncidentStatus(id, request, actorEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Recupera la secuencia cronologica completa de transiciones de estado de una incidencia para fines de auditoria.
     * <p>
     * Endpoint restringido exclusivamente a usuarios con rol {@code ADMIN}.
     * Retorna HTTP 404 Not Found si el identificador de la incidencia no existe en el sistema.
     * </p>
     *
     * @param id Identificador numerico unico de la incidencia cuyo historial se desea consultar.
     * @return {@link ResponseEntity} con codigo HTTP 200 OK y la lista cronologica de {@link StatusChangeResponse}.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<StatusChangeResponse>> getIncidentHistory(@PathVariable Long id) {
        List<StatusChangeResponse> history = incidentService.getIncidentHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Sube y asocia un archivo multimedia (imagen JPEG o PNG) a una incidencia urbana existente.
     * <p>
     * Endpoint protegido que requiere autenticación previa. Valida el tipo MIME permitido,
     * el límite de tamaño máximo de 5MB y almacena físicamente el fichero en el servidor.
     * </p>
     *
     * @param id             Identificador numérico único de la incidencia.
     * @param file           Fichero multimedia recibido en formato multipart (parámetro "file").
     * @param authentication Objeto de autenticación de Spring Security del usuario emisor.
     * @return {@link ResponseEntity} con código HTTP 201 Created y el cuerpo {@link AttachmentResponse}.
     */
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        AttachmentResponse response = attachmentService.uploadAttachment(id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Recupera todos los archivos multimedia adjuntos vinculados a una incidencia.
     *
     * @param id Identificador numérico único de la incidencia.
     * @return {@link ResponseEntity} con código HTTP 200 OK y la lista de {@link AttachmentResponse}.
     */
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachments(@PathVariable Long id) {
        List<AttachmentResponse> attachments = attachmentService.getAttachmentsByIncidentId(id);
        return ResponseEntity.ok(attachments);
    }

    /**
     * Confirma formalmente la vinculación de un activo urbano a una incidencia.
     * <p>
     * Endpoint protegido con control de acceso perimetral restringido a usuarios con rol {@code OPERATOR} o {@code ADMIN}.
     * Actualiza el indicador {@code isConfirmed} a {@code true} para el activo especificado.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param assetId    Identificador único del activo urbano asociado.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el DTO {@link IncidentAssetResponse} actualizado.
     */
    @PostMapping("/{incidentId}/assets/{assetId}/confirm")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<IncidentAssetResponse> confirmAssetAssociation(
            @PathVariable("incidentId") Long incidentId,
            @PathVariable("assetId") Long assetId
    ) {
        if (urbanAssetService == null) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Servicio de activos urbanos no disponible");
        }
        IncidentAssetResponse response = urbanAssetService.confirmAssetAssociation(incidentId, assetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina o desvincula la asociación entre un activo urbano y una incidencia de servicio.
     * <p>
     * Endpoint protegido con control de acceso perimetral restringido a usuarios con rol {@code OPERATOR} o {@code ADMIN}.
     * Retorna HTTP 200 OK (o 204 No Content) tras completar la desvinculación.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param assetId    Identificador único del activo urbano a desvincular.
     * @return {@link ResponseEntity} con código HTTP 200 OK y confirmación de la supresión.
     */
    @DeleteMapping("/{incidentId}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> removeAssetAssociation(
            @PathVariable("incidentId") Long incidentId,
            @PathVariable("assetId") Long assetId
    ) {
        if (urbanAssetService == null) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Servicio de activos urbanos no disponible");
        }
        urbanAssetService.removeAssetAssociation(incidentId, assetId);
        return ResponseEntity.ok().build();
    }

    /**
     * Busca incidencias aplicando filtros multicriterio: texto libre, categoría, rango de fechas
     * y estado del ciclo de vida (HU-19).
     * <p>
     * El filtro por estado ({@code status}) se encuentra restringido a perfiles internos; si la petición
     * es realizada por un ciudadano ({@link RolUsuario#CITIZEN}), el parámetro es ignorado automáticamente.
     * </p>
     *
     * @param text      Texto libre en título o descripción (opcional).
     * @param category  Categoría temática de la incidencia (opcional).
     * @param startDate Fecha inicial del rango de creación (opcional).
     * @param endDate   Fecha final del rango de creación (opcional).
     * @param status    Estado de triaje de la incidencia (opcional, ignorado para CITIZEN).
     * @param auth      Contexto de autenticación del usuario.
     * @return Lista de incidencias que satisfacen los criterios combinados.
     */
    @GetMapping("/search")
    public ResponseEntity<List<IncidentResponse>> searchIncidents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(required = false) EstadoIncidencia status,
            Authentication auth) {

        boolean includeStatus = false;
        if (auth != null && auth.isAuthenticated()) {
            boolean hasCitizenRole = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CITIZEN") || a.getAuthority().equals("CITIZEN"));
            if (!hasCitizenRole && userRepository != null) {
                java.util.Optional<Usuario> userOpt = userRepository.findByEmail(auth.getName());
                if (userOpt.isPresent()) {
                    includeStatus = userOpt.get().getRole() != RolUsuario.CITIZEN;
                } else {
                    includeStatus = true;
                }
            } else {
                includeStatus = !hasCitizenRole;
            }
        }

        List<IncidentResponse> results = incidentService.searchIncidents(text, category, startDate, endDate, status, includeStatus);
        return ResponseEntity.ok(results);
    }
    }
}
