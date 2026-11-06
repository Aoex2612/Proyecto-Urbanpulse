package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) que encapsula el rendimiento resolutivo y volumen
 * de trabajo de un departamento municipal.
 * <p>
 * Proporciona visibilidad sobre la carga operativa, tiempos medios de atención y porcentaje de
 * cumplimiento de SLA por área técnica responsable, conforme a los requisitos RF-6 y RF-10.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentMetricResponse {

    /**
     * Identificador único del departamento municipal.
     */
    private Long departmentId;

    /**
     * Denominación o nombre oficial del departamento municipal.
     */
    private String departmentName;

    /**
     * Número total de incidencias asignadas al departamento en el periodo consultado.
     */
    private Long totalAssigned;

    /**
     * Número total de incidencias resueltas satisfactoriamente por el departamento.
     */
    private Long resolvedCount;

    /**
     * Tiempo medio de resolución en horas logrado por el departamento.
     */
    private Double avgResolutionHours;

    /**
     * Porcentaje de cumplimiento de SLA alcanzado por el departamento (0.0 a 100.0).
     */
    private Double slaComplianceRate;
}
