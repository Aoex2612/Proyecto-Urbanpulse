package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) para registrar la resolución técnica de una incidencia.
 * <p>
 * Empleado por operarios técnicos o administradores para reportar el término de las labores de campo (RF-15).
 * Para incidencias con prioridad media, alta o crítica, el servicio exigirá notas descriptivas de al menos 10 caracteres.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolveIncidentRequest {

    /**
     * Notas explicativas o descripción de las actuaciones operativas realizadas para subsanar la incidencia.
     */
    private String resolutionNotes;
}
