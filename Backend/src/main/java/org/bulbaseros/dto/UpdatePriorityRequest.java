package org.bulbaseros.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bulbaseros.entity.PrioridadIncidencia;

/**
 * Objeto de transferencia de datos (DTO) para la asignación o actualización manual
 * de la prioridad operativa de una incidencia y su justificación.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePriorityRequest {

    /**
     * Nivel de prioridad definitivo que se asignará a la incidencia.
     */
    @NotNull(message = "La prioridad de la incidencia es obligatoria")
    private PrioridadIncidencia priority;

    /**
     * Motivo, justificación o explicación técnica de la prioridad fijada.
     */
    private String priorityReason;
}
