package org.bulbaseros.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) para la asignación operativa de una incidencia a uno o varios
 * departamentos municipales y técnicos correspondientes.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignIncidentRequest {

    /**
     * Lista de identificadores de los departamentos municipales asignados.
     * Es obligatoria y no puede estar vacía.
     */
    @NotEmpty(message = "Debe indicarse al menos un departamento para la asignación")
    private List<Long> departmentIds;

    /**
     * Lista opcional de identificadores de los técnicos operativos asignados.
     */
    private List<Long> technicianIds;
}
