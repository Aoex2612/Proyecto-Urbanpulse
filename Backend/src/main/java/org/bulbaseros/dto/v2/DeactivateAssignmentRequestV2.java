package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la desactivacion de asignaciones con identificadores publicos en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivateAssignmentRequestV2 {

    private List<String> departmentIds;
    private List<String> technicianIds;
}
