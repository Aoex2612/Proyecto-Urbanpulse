package org.bulbaseros.mappers;

import org.bulbaseros.dto.v2.StatusChangeResponseV2;
import org.bulbaseros.entity.CambiosEstado;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador de componentes Spring para transformar {@link CambiosEstado} a {@link StatusChangeResponseV2} en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Component
public class StatusChangeMapperV2 {

    public StatusChangeResponseV2 toDto(CambiosEstado entity) {
        if (entity == null) {
            return null;
        }

        String id = entity.getPublicId() != null ? entity.getPublicId().toString() : null;
        String incidentId = (entity.getIncident() != null && entity.getIncident().getPublicId() != null)
                ? entity.getIncident().getPublicId().toString() : null;
        String changedByUserId = (entity.getChangedByUser() != null && entity.getChangedByUser().getPublicId() != null)
                ? entity.getChangedByUser().getPublicId().toString() : null;
        String changedByUserName = (entity.getChangedByUser() != null) ? entity.getChangedByUser().getFullName() : null;

        return StatusChangeResponseV2.builder()
                .id(id)
                .incidentId(incidentId)
                .fromStatus(entity.getFromStatus())
                .toStatus(entity.getToStatus())
                .changedByUserId(changedByUserId)
                .changedByUserName(changedByUserName)
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<StatusChangeResponseV2> toDtoList(List<CambiosEstado> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
