package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para la representacion de una asignacion operativa en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponseV2 {

    private String id;
    private String incidentId;
    private String departmentId;
    private String departmentName;
    private String technicianId;
    private String technicianName;
    private String assignedById;
    private String assignedByName;
    private Boolean isActive;
    private OffsetDateTime assignedAt;
}
