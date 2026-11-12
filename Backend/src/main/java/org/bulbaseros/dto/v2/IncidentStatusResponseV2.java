package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bulbaseros.entity.EstadoIncidencia;

import java.time.OffsetDateTime;

/**
 * DTO para la respuesta de actualizacion de estado en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentStatusResponseV2 {

    private String id;
    private EstadoIncidencia status;
    private OffsetDateTime updatedAt;
    private String reason;
    private String updatedBy;
}
