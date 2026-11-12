package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la representacion de un departamento municipal en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponseV2 {

    private String id;
    private String name;
    private String description;
}
