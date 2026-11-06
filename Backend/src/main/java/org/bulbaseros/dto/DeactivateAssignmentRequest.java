package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) para la solicitud de desactivación lógica de asignaciones
 * activas (departamentos o técnicos específicos) de una incidencia.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeactivateAssignmentRequest {

    /**
     * Lista opcional de identificadores de departamentos cuyas asignaciones activas deben desactivarse.
     */
    private List<Long> departmentIds;

    /**
     * Lista opcional de identificadores de técnicos cuyas asignaciones activas deben desactivarse.
     */
    private List<Long> technicianIds;
}
