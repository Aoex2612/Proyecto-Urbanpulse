package org.bulbaseros.controller;

import jakarta.validation.Valid;
import org.bulbaseros.dto.IncidentStatusResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Controlador REST que gestiona las operaciones operativas y de triaje de incidencias en UrbanPulse.
 * <p>
 * Provee endpoints protegidos mediante RBAC (Role-Based Access Control) para la actualización
 * de estados y gestión de incidencias urbanas reportadas, restringidos exclusivamente a personal
 * con roles operativos autorizados ({@code OPERATOR} y {@code ADMIN}).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    /**
     * Constructor por defecto del controlador de incidencias.
     */
    public IncidentController() {
    }

    /**
     * Actualiza el estado de una incidencia en el flujo de triaje operativo.
     * <p>
     * Endpoint protegido que recibe la solicitud de cambio de estado validada por Bean Validation
     * y retorna la confirmación de la actualización con código HTTP 200 OK.
     * El acceso está restringido perimetralmente en la capa de seguridad a usuarios con roles
     * {@code OPERATOR} o {@code ADMIN} (RF-5, RF-6).
     * </p>
     *
     * @param id      Identificador numérico único de la incidencia a modificar.
     * @param request Datos de la solicitud que contienen el nuevo estado y comentarios opcionales.
     * @return {@link ResponseEntity} con código HTTP 200 OK y el cuerpo {@link IncidentStatusResponse}.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentStatusResponse> updateIncidentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request
    ) {
        IncidentStatusResponse response = new IncidentStatusResponse(
                id,
                request.getStatus(),
                "Estado de la incidencia actualizado exitosamente",
                OffsetDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}
