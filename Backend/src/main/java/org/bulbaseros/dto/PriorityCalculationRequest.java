package org.bulbaseros.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) para solicitar el cálculo asistido de prioridad
 * determinista de una incidencia urbana.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityCalculationRequest {

    /**
     * Indicador booleano que determina si la incidencia provoca o requiere el corte de una vía pública.
     */
    @NotNull(message = "El indicador de corte de vía es obligatorio")
    private Boolean streetCut;

    /**
     * Nivel de riesgo para la seguridad ciudadana asociado a la incidencia.
     */
    @NotNull(message = "El nivel de riesgo de seguridad es obligatorio")
    private SafetyRiskLevel safetyRisk;
}
