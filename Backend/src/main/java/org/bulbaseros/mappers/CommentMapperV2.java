package org.bulbaseros.mappers;

import org.bulbaseros.dto.v2.CommentResponseV2;
import org.bulbaseros.entity.ComentariosIncidencias;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapeador de componentes Spring para transformar {@link ComentariosIncidencias} a {@link CommentResponseV2} en API v2.
 *
 * @author UrbanPulse Team
 * @version 2.0
 */
@Component
public class CommentMapperV2 {

    public CommentResponseV2 toDto(ComentariosIncidencias entity) {
        if (entity == null) {
            return null;
        }

        String id = entity.getPublicId() != null ? entity.getPublicId().toString() : null;
        String incidentId = (entity.getIncident() != null && entity.getIncident().getPublicId() != null)
                ? entity.getIncident().getPublicId().toString() : null;
        String authorId = (entity.getAuthor() != null && entity.getAuthor().getPublicId() != null)
                ? entity.getAuthor().getPublicId().toString() : null;
        String authorName = (entity.getAuthor() != null) ? entity.getAuthor().getFullName() : null;

        return CommentResponseV2.builder()
                .id(id)
                .incidentId(incidentId)
                .authorId(authorId)
                .authorName(authorName)
                .comment(entity.getComment())
                .isInternal(entity.getIsInternal())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<CommentResponseV2> toDtoList(List<ComentariosIncidencias> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
