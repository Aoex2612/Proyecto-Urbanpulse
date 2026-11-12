package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para la representacion de una notificacion en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseV2 {

    private String id;
    private String userId;
    private String incidentId;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private OffsetDateTime createdAt;
}
