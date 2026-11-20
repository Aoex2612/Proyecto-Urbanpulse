package org.bulbaseros.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos (DTO) para la edición o modificación del contenido textual
 * de un comentario preexistente.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCommentRequest {

    /**
     * Nuevo contenido textual actualizado del comentario.
     */
    @NotBlank(message = "El contenido del comentario es obligatorio")
    private String comment;
}
