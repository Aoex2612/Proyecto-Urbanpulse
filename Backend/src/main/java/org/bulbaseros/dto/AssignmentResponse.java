package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) de respuesta que representa los detalles de una asignación operativa.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {

    /**
     * Identificador único de la asignación (clave primaria de la tarea).
     */
    private Long id;

    /**
     * Identificador único del departamento municipal asignado.
     */
    private Long departmentId;

    /**
     * Nombre descriptivo del departamento municipal asignado.
     */
    private String departmentName;

    /**
     * Identificador único del técnico municipal asignado (opcional).
     */
    private Long technicianId;

    /**
     * Nombre completo del técnico municipal asignado (opcional).
     */
    private String technicianName;

    /**
     * Identificador único del usuario que autorizó o realizó la asignación.
     */
    private Long assignedById;

    /**
     * Nombre completo del usuario que autorizó o realizó la asignación.
     */
    private String assignedByName;

    /**
     * Indicador de vigencia operativa (activo/inactivo de forma lógica).
     */
    private Boolean isActive;

    /**
     * Marca temporal en que se registró la asignación.
     */
    private OffsetDateTime assignedAt;
}
