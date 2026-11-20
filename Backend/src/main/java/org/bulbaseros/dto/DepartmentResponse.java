package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) de respuesta que representa la información de un departamento municipal.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    /**
     * Identificador único del departamento municipal.
     */
    private Long id;

    /**
     * Denominación o nombre oficial del departamento.
     */
    private String name;

    /**
     * Descripción de competencias o alcance funcional del departamento.
     */
    private String description;
}
