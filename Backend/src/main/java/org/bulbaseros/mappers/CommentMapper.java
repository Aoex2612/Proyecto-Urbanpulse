package org.bulbaseros.mappers;

import org.bulbaseros.dto.CommentResponse;
import org.bulbaseros.entity.ComentariosIncidencias;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Usuario;
import org.springframework.stereotype.Component;

/**
 * Mapeador desacoplado de componentes Spring para la conversión entre la entidad de persistencia
 * {@link ComentariosIncidencias} y el objeto de transferencia de datos {@link CommentResponse}.
 * <p>
 * Facilita la serialización controlada de notas y comentarios de seguimiento de incidencias,
 * abstrayendo las relaciones JPA y proyectando los datos del autor de forma plana y segura.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Component
public class CommentMapper extends BaseMapper<ComentariosIncidencias, CommentResponse> {

    /**
     * Transforma una entidad de persistencia {@link ComentariosIncidencias} a su respectivo DTO {@link CommentResponse}.
     *
     * @param entity Entidad de comentario de origen, o {@code null}.
     * @return DTO {@link CommentResponse} mapeado con los datos del comentario y autor, o {@code null} si la entidad es nula.
     */
    @Override
    public CommentResponse toDto(ComentariosIncidencias entity) {
        if (entity == null) {
            return null;
        }

        Long incidentId = (entity.getIncident() != null) ? entity.getIncident().getId() : null;
        Long authorId = (entity.getAuthor() != null) ? entity.getAuthor().getId() : null;
        String authorName = (entity.getAuthor() != null) ? entity.getAuthor().getFullName() : null;
        String authorRole = (entity.getAuthor() != null && entity.getAuthor().getRole() != null)
                ? entity.getAuthor().getRole().name()
                : null;

        return CommentResponse.builder()
                .id(entity.getId())
                .incidentId(incidentId)
                .authorId(authorId)
                .authorName(authorName)
                .authorRole(authorRole)
                .comment(entity.getComment())
                .isInternal(entity.getIsInternal())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Construye una nueva entidad {@link ComentariosIncidencias} a partir de las entidades de dominio y datos requeridos.
     *
     * @param incident   Incidencia urbana asociada al comentario.
     * @param author     Usuario autor que redacta el comentario.
     * @param comment    Texto descriptivo u observación.
     * @param isInternal Indicador de visibilidad interna para personal municipal.
     * @return Nueva entidad {@link ComentariosIncidencias} inicializada.
     */
    public ComentariosIncidencias toEntity(Incidencia incident, Usuario author, String comment, Boolean isInternal) {
        return new ComentariosIncidencias(incident, author, comment, isInternal);
    }
}
