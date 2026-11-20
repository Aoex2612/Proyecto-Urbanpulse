package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) que contiene las métricas agregadas asociadas a una
 * demarcación territorial específica (Distrito municipal o Barrio).
 * <p>
 * Facilita el análisis espacial y la visualización de la concentración geográfica de incidencias
 * en cumplimiento de los requisitos RF-7 y RF-10.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerritorialMetricResponse {

    /**
     * Nombre de la demarcación territorial (ej. nombre del distrito, barrio, o "Sin distrito especificado").
     */
    private String territoryName;

    /**
     * Número total de incidencias registradas en esta demarcación territorial.
     */
    private Long totalIncidents;

    /**
     * Número de incidencias resueltas en esta demarcación.
     */
    private Long resolvedCount;

    /**
     * Número de incidencias que permanecen pendientes en esta demarcación.
     */
    private Long pendingCount;

    /**
     * Porcentaje de cumplimiento de compromisos SLA en esta demarcación (0.0 a 100.0).
     */
    private Double slaComplianceRate;
}
