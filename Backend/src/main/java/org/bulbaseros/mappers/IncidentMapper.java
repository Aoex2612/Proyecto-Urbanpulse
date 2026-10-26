package org.bulbaseros.mappers;

import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.entity.Incidencia;
import org.springframework.stereotype.Component;

/**
 * Mapeador de componentes Spring para la conversion entre entidades {@link Incidencia}
 * y DTOs {@link IncidentResponse}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class IncidentMapper extends BaseMapper<Incidencia, IncidentResponse> {

    /**
     * Convierte una entidad {@link Incidencia} en su correspondiente DTO {@link IncidentResponse}.
     *
     * @param entity Entidad de incidencia origen, o {@code null}.
     * @return DTO {@link IncidentResponse} mapeado, o {@code null} si la entidad es nula.
     */
    @Override
    public IncidentResponse toDto(Incidencia entity) {
        if (entity == null) {
            return null;
        }

        Long citizenId = (entity.getCitizen() != null) ? entity.getCitizen().getId() : null;
        String citizenName = (entity.getCitizen() != null) ? entity.getCitizen().getFullName() : null;

        return new IncidentResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getStatus(),
                entity.getPriority(),
                citizenId,
                citizenName,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
