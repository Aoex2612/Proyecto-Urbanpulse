package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) de respuesta que expone un comentario o nota técnica asociada a una incidencia.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    /**
     * Identificador único del comentario.
     */
    private Long id;

    /**
     * Identificador de la incidencia asociada.
     */
    private Long incidentId;

    /**
     * Identificador del usuario autor del comentario.
     */
    private Long authorId;

    /**
     * Nombre completo del autor del comentario.
     */
    private String authorName;

    /**
     * Rol funcional del autor del comentario en el momento de la consulta.
     */
    private String authorRole;

    /**
     * Contenido textual del comentario.
     */
    private String comment;

    /**
     * Indicador de nota interna (confidencial para personal) o aviso público.
     */
    private Boolean isInternal;

    /**
     * Fecha y hora de creación original del comentario.
     */
    private OffsetDateTime createdAt;

    /**
     * Fecha y hora de la última edición efectuada sobre el comentario (nula si nunca ha sido editado).
     */
    private OffsetDateTime updatedAt;
}
