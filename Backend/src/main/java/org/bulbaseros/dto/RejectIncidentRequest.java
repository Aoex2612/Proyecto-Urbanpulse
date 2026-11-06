package org.bulbaseros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) para la operación de rechazo de una incidencia.
 * <p>
 * Requiere un motivo explicativo con una longitud mínima de 5 caracteres para garantizar
 * la trazabilidad y debida justificación ante la ciudadanía (RF-5).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectIncidentRequest {

    /**
     * Motivo o justificación técnica/operativa del rechazo de la incidencia.
     * Es obligatorio y debe contener al menos 5 caracteres.
     */
    @NotBlank(message = "El motivo de rechazo es obligatorio")
    @Size(min = 5, message = "El motivo debe tener al menos 5 caracteres")
    private String reason;
}
