package org.bulbaseros.mappers;

import org.bulbaseros.dto.v2.NotificationResponseV2;
import org.bulbaseros.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador de componentes Spring para transformar {@link Notification} a {@link NotificationResponseV2} en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Component
public class NotificationMapperV2 {

    public NotificationResponseV2 toDto(Notification entity) {
        if (entity == null) {
            return null;
        }

        String id = entity.getPublicId() != null ? entity.getPublicId().toString() : null;
        String userId = (entity.getUser() != null && entity.getUser().getPublicId() != null)
                ? entity.getUser().getPublicId().toString() : null;
        String incidentId = (entity.getIncident() != null && entity.getIncident().getPublicId() != null)
                ? entity.getIncident().getPublicId().toString() : null;

        return NotificationResponseV2.builder()
                .id(id)
                .userId(userId)
                .incidentId(incidentId)
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<NotificationResponseV2> toDtoList(List<Notification> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
