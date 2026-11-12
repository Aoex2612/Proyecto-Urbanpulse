package org.bulbaseros.dto.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bulbaseros.entity.RolUsuario;

/**
 * DTO para el resumen de informacion de usuario en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponseV2 {

    private String id;
    private String fullName;
    private String email;
    private RolUsuario role;
    private String departmentId;
}
