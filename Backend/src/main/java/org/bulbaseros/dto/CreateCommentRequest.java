package org.bulbaseros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) para la publicación de un nuevo comentario o nota de seguimiento
 * asociada a una incidencia urbana.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCommentRequest {

    /**
     * Contenido textual del comentario o nota.
     */
    @NotBlank(message = "El contenido del comentario es obligatorio")
    private String comment;

    /**
     * Indicador de visibilidad interna.
     * Si es {@code true}, el comentario solo es visible para personal municipal (operadores, técnicos y administradores).
     * Si es {@code false}, es público y visible para el ciudadano creador de la incidencia.
     */
    @NotNull(message = "El indicador de visibilidad interna es obligatorio")
    private Boolean isInternal;
}
