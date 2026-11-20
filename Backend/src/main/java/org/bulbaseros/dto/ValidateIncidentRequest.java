package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) para la operación de validación de una incidencia.
 * <p>
 * Permite a operadores o administradores validar formalmente un reporte ciudadano,
 * aportando opcionalmente un motivo o comentario descriptivo.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateIncidentRequest {

    /**
     * Motivo, justificación u observaciones de la validación operativa (opcional).
     */
    private String reason;
}
