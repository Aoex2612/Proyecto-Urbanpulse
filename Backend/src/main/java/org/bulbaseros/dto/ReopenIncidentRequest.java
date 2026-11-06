package org.bulbaseros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) para la solicitud de reapertura de una incidencia resuelta o cerrada.
 * <p>
 * Exige a operadores o administradores un motivo justificado de al menos 5 caracteres (RF-18).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReopenIncidentRequest {

    /**
     * Motivo o justificación de la reapertura de la incidencia.
     * Es obligatorio y debe contener al menos 5 caracteres.
     */
    @NotBlank(message = "El motivo de reapertura es obligatorio")
    @Size(min = 5, message = "El motivo debe tener al menos 5 caracteres")
    private String reason;
}
