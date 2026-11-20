package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bulbaseros.dto.ExternalObservationResponse;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.PrioridadIncidencia;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO para la representacion publica de una incidencia en la API v2.
 * Expone unicamente identificadores publicos en formato UUID como String,
 * ocultando completamente el identificador numerico interno de base de datos.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponseV2 {

    private String id;
    private String title;
    private String description;
    private String category;
    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    private String address;
    private String district;
    private String neighborhood;
    private EstadoIncidencia status;
    private PrioridadIncidencia priority;
    private String priorityReason;
    private String citizenId;
    private String citizenName;
    private String citizenEmail;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<IncidentAssetResponseV2> urbanAssets;
    private List<ExternalObservationResponse> urbanContext;
    private List<AssignmentResponseV2> activeAssignments;
}
