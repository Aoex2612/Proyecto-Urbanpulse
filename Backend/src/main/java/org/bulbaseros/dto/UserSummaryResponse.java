package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bulbaseros.entity.RolUsuario;

/**
 * Objeto de transferencia de datos (DTO) de respuesta con el resumen público/operativo de un usuario
 * (habitualmente empleado para listados de técnicos o asignatarios).
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {

    /**
     * Identificador único del usuario.
     */
    private Long id;

    /**
     * Nombre completo y apellidos del usuario.
     */
    private String fullName;

    /**
     * Dirección de correo electrónico del usuario.
     */
    private String email;

    /**
     * Rol funcional asignado dentro de la plataforma.
     */
    private RolUsuario role;

    /**
     * Identificador del departamento municipal al que pertenece el usuario (opcional).
     */
    private Long departmentId;
}
