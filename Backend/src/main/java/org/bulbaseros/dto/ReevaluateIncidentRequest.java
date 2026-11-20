package org.bulbaseros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bulbaseros.entity.EstadoIncidencia;

/**
 * Objeto de transferencia de datos (DTO) para la operación de reevaluación de una incidencia rechazada.
 * <p>
 * Permite a operadores o administradores devolver una incidencia a los estados operativos
 * {@link EstadoIncidencia#REPORTED} o {@link EstadoIncidencia#VALIDATED}, exigiendo un motivo justificado (RF-7).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReevaluateIncidentRequest {

    /**
     * Estado operativo de destino tras la reevaluación.
     * Solo son válidos los estados REPORTED o VALIDATED.
     */
    @NotNull(message = "El estado destino es obligatorio")
    private EstadoIncidencia targetStatus;

    /**
     * Justificación técnica u operativa del motivo de la reevaluación.
     * Es obligatoria y debe contener al menos 5 caracteres.
     */
    @NotBlank(message = "El motivo de reevaluación es obligatorio")
    @Size(min = 5, message = "El motivo debe tener al menos 5 caracteres")
    private String reason;

    /**
     * Comprueba si el estado destino indicado es uno de los permitidos para la reevaluación.
     *
     * @return {@code true} si el estado es {@link EstadoIncidencia#REPORTED} o {@link EstadoIncidencia#VALIDATED}; {@code false} en caso contrario.
     */
    public boolean isValidTargetStatus() {
        return targetStatus == EstadoIncidencia.REPORTED || targetStatus == EstadoIncidencia.VALIDATED;
    }
}
