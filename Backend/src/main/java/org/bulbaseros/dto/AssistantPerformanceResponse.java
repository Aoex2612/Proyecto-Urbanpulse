package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) que consolida el desempeño histórico y las métricas
 * de adopción operativa del motor de asistencia inteligente de recomendaciones.
 * <p>
 * Facilita a analistas y administradores el seguimiento de la fiabilidad y tasa de aceptación
 * de las sugerencias de asignación departamental emitidas en la plataforma (RF-18).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantPerformanceResponse {

    /**
     * Número total de recomendaciones emitidas por el asistente a los operadores.
     */
    private Long totalRecommendations;

    /**
     * Número de recomendaciones que fueron confirmadas o aceptadas por los operadores en el triaje.
     */
    private Long acceptedRecommendations;

    /**
     * Tasa porcentual de adopción de las recomendaciones asistidas (0.0 a 100.0).
     */
    private Double adoptionRate;

    /**
     * Nivel de confianza promedio estimado a lo largo de las recomendaciones evaluadas (0.0 a 100.0).
     */
    private Double averageConfidence;
}
