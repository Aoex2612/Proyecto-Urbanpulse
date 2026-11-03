package org.bulbaseros.mappers;

import org.bulbaseros.dto.StatusChangeResponse;
import org.bulbaseros.entity.CambiosEstado;
import org.springframework.stereotype.Component;

/**
 * Mapeador de componentes Spring para la conversion entre entidades {@link CambiosEstado}
 * y DTOs {@link StatusChangeResponse}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class StatusChangeMapper extends BaseMapper<CambiosEstado, StatusChangeResponse> {

    /**
     * Convierte una entidad {@link CambiosEstado} en su correspondiente DTO {@link StatusChangeResponse}.
     *
     * @param entity Entidad de transicion de estado origen, o {@code null}.
     * @return DTO {@link StatusChangeResponse} mapeado, o {@code null} si la entidad es nula.
     */
    @Override
    public StatusChangeResponse toDto(CambiosEstado entity) {
        if (entity == null) {
            return null;
        }

        Long incidentId = (entity.getIncident() != null) ? entity.getIncident().getId() : null;
        Long changedByUserId = (entity.getChangedByUser() != null) ? entity.getChangedByUser().getId() : null;
        String changedByUserName = (entity.getChangedByUser() != null) ? entity.getChangedByUser().getFullName() : null;
        String changedByUserEmail = (entity.getChangedByUser() != null) ? entity.getChangedByUser().getEmail() : null;

        return new StatusChangeResponse(
                entity.getId(),
                incidentId,
                entity.getFromStatus(),
                entity.getToStatus(),
                changedByUserId,
                changedByUserName,
                changedByUserEmail,
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
