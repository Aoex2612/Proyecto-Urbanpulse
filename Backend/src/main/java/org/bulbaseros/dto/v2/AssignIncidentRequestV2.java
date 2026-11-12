package org.bulbaseros.dto.v2;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la asignacion de incidencias con identificadores publicos en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignIncidentRequestV2 {

    @NotEmpty(message = "Debe indicarse al menos un departamento para la asignación")
    private List<String> departmentIds;

    private List<String> technicianIds;
}
