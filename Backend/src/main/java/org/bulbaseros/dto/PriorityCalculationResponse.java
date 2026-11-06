package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bulbaseros.entity.PrioridadIncidencia;

/**
 * Objeto de transferencia de datos (DTO) que contiene el resultado sugerido del cálculo
 * asistido de priorización determinista y su respectiva justificación explicativa.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityCalculationResponse {

    /**
     * Prioridad calculada recomendada por el motor de reglas deterministas.
     */
    private PrioridadIncidencia suggestedPriority;

    /**
     * Justificación descriptiva o explicación textual de la prioridad sugerida.
     */
    private String priorityReason;
}
