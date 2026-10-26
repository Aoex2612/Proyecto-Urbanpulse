package org.bulbaseros.controller;

import jakarta.validation.Valid;
import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.IncidentStatusResponse;
import org.bulbaseros.dto.StatusChangeResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.bulbaseros.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST que gestiona las operaciones del ciclo de vida y auditoria de incidencias en UrbanPulse.
 * <p>
 * Expone endpoints protegidos mediante RBAC (Role-Based Access Control) para la consulta cartografica
 * de incidencias activas, obtencion del detalle individual, alta de reportes ciudadanos, triaje operativo
 * de estados con auditoria inmutable e inspeccion cronologica de transiciones para administradores.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    /**
     * Servicio de logica de negocio transaccional para la gestion de incidencias y auditoria.
     */
    private final IncidentService incidentService;

    /**
     * Constructor con inyeccion de dependencias para el controlador de incidencias.
     *
     * @param incidentService Servicio de logica de negocio transaccional de incidencias.
     */
    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    /**
     * Recupera todas las incidencias urbanas activas en la plataforma para su representacion en el mapa interactivo.
     * <p>
     * Excluye del resultado los estados finales de ciclo de vida (CLOSED y REJECTED).
     * Requiere que la peticion provenga de un usuario autenticado con cualquier rol valido.
     * </p>
     *
     * @return {@link ResponseEntity} con codigo HTTP 200 OK y la lista de {@link IncidentResponse} activas.
     */
    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getActiveIncidents() {
        List<IncidentResponse> incidents = incidentService.getActiveIncidents();
        return ResponseEntity.ok(incidents);
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
}
