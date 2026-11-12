package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para la representacion del historial de auditoria en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeResponseV2 {

    private String id;
    private String incidentId;
    private String fromStatus;
    private String toStatus;
    private String changedByUserId;
    private String changedByUserName;
    private String reason;
    private OffsetDateTime createdAt;
}
