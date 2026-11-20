package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) que representa la distribución y tasa de resolución
 * de incidencias agrupadas por categoría temática (ej. Alumbrado, Vías Públicas, Limpieza).
 * <p>
 * Facilita el diagnóstico de las áreas temáticas con mayor recurrencia o dificultades operativas
 * en cumplimiento del requisito RF-6.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMetricResponse {

    /**
     * Denominación de la categoría temática de la incidencia.
     */
    private String category;

    /**
     * Número total de incidencias registradas dentro de esta categoría.
     */
    private Long totalIncidents;

    /**
     * Número de incidencias resueltas dentro de esta categoría.
     */
    private Long resolvedCount;

    /**
     * Porcentaje de cumplimiento de acuerdos de SLA para las incidencias de esta categoría (0.0 a 100.0).
     */
    private Double slaComplianceRate;
}
