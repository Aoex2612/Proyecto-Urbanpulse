package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) que consolida los indicadores clave de rendimiento (KPIs)
 * operacionales y de cumplimiento de acuerdos de nivel de servicio (SLA) para un periodo determinado.
 * <p>
 * Refleja métricas agregadas globales como el Tiempo Medio de Triaje (MTTT), Tiempo Medio de Resolución (MTTR),
 * y la tasa porcentual de cumplimiento de SLA conforme a los requisitos RF-1 a RF-5.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationalMetricsResponse {

    /**
     * Volumen total de incidencias registradas en el periodo analizado.
     */
    private Long totalIncidents;

    /**
     * Total de incidencias que alcanzaron el estado de resolución (RESOLVED o CLOSED).
     */
    private Long resolvedIncidents;

    /**
     * Total de incidencias actualmente pendientes de resolución.
     */
    private Long pendingIncidents;

    /**
     * Tiempo Medio de Triaje (MTTT) en horas (diferencia entre reporte inicial y primera validación/rechazo).
     */
    private Double mtttHours;

    /**
     * Tiempo Medio de Resolución (MTTR) en horas (diferencia entre validación/asignación y resolución).
     */
    private Double mttrHours;

    /**
     * Porcentaje global de cumplimiento de acuerdos de nivel de servicio SLA (0.0 a 100.0).
     */
    private Double slaComplianceRate;

    /**
     * Total de incidencias que han incumplido el plazo máximo de SLA estipulado según su prioridad.
     */
    private Long totalSlaBreached;

    /**
     * Total de incidencias que han cumplido satisfactoriamente con su compromiso de SLA.
     */
    private Long totalSlaCompliant;
}
