package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) de respuesta que contiene la sugerencia asistida
 * de departamento municipal para el triaje y asignación de una incidencia.
 * <p>
 * Calculada por el motor heurístico autónomo local en función de correlaciones léxicas
 * (título, descripción, categoría, territorio) y frecuencias históricas, suministrando
 * una estimación porcentual de certeza (0% a 100%) y una advertencia explícita en caso de baja
 * confianza (inferior al 50%) conforme a los requisitos RF-15, RF-16 y RF-17.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRecommendationResponse {

    /**
     * Identificador del departamento municipal sugerido (o {@code null} si no se identificó correspondencia).
     */
    private Long suggestedDepartmentId;

    /**
     * Denominación oficial del departamento municipal sugerido.
     */
    private String suggestedDepartmentName;

    /**
     * Grado de certeza o confianza probabilística estimada (0.0 a 100.0).
     */
    private Double confidencePercentage;

    /**
     * Indicador booleano que señala si la confianza es baja (< 50%), advirtiendo al operador
     * de que se requiere validación manual exhaustiva.
     */
    private Boolean isLowConfidenceWarning;

    /**
     * Justificación o razonamiento sintético que explica las coincidencias léxicas o reglas que motivaron la sugerencia.
     */
    private String reasoning;
}
