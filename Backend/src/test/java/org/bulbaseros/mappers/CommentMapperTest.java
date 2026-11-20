package org.bulbaseros.mappers;

import org.bulbaseros.dto.CommentResponse;
import org.bulbaseros.entity.ComentariosIncidencias;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link CommentMapper}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class CommentMapperTest {

    private final CommentMapper mapper = new CommentMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad ComentariosIncidencias es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("Debe mapear correctamente todos los campos de ComentariosIncidencias a CommentResponse")
    void shouldMapEntityToDtoCorrectly() {
        Incidencia incident = new Incidencia();
        incident.setId(100L);

        Usuario author = new Usuario("autor@urbanpulse.es", "hash", "María Redactora");
        author.setId(50L);
        author.setRole(RolUsuario.OPERATOR);

        OffsetDateTime createdAt = OffsetDateTime.now().minusHours(2);
        OffsetDateTime updatedAt = OffsetDateTime.now();

        ComentariosIncidencias comment = new ComentariosIncidencias();
        comment.setId(10L);
        comment.setIncident(incident);
        comment.setAuthor(author);
        comment.setComment("Se ha contactado con el equipo de señalización");
        comment.setIsInternal(true);
        comment.setCreatedAt(createdAt);
        comment.setUpdatedAt(updatedAt);

        CommentResponse dto = mapper.toDto(comment);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals(100L, dto.getIncidentId());
        assertEquals(50L, dto.getAuthorId());
        assertEquals("María Redactora", dto.getAuthorName());
        assertEquals("OPERATOR", dto.getAuthorRole());
        assertEquals("Se ha contactado con el equipo de señalización", dto.getComment());
        assertTrue(dto.getIsInternal());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    @DisplayName("Debe mapear correctamente ComentariosIncidencias con relaciones nulas")
    void shouldMapEntityWithNullRelationsCorrectly() {
        ComentariosIncidencias comment = new ComentariosIncidencias();
        comment.setId(11L);
        comment.setComment("Nota simple");
        comment.setIsInternal(false);

        CommentResponse dto = mapper.toDto(comment);

        assertNotNull(dto);
        assertEquals(11L, dto.getId());
        assertNull(dto.getIncidentId());
        assertNull(dto.getAuthorId());
        assertNull(dto.getAuthorName());
        assertNull(dto.getAuthorRole());
        assertEquals("Nota simple", dto.getComment());
        assertFalse(dto.getIsInternal());
        assertNull(dto.getCreatedAt());
        assertNull(dto.getUpdatedAt());
    }

    @Test
    @DisplayName("Debe transformar lista de comentarios a lista de CommentResponse")
    void shouldMapEntityListToDtoList() {
        ComentariosIncidencias c1 = new ComentariosIncidencias();
        c1.setId(1L);
        ComentariosIncidencias c2 = new ComentariosIncidencias();
        c2.setId(2L);

        List<CommentResponse> dtos = mapper.toDtoList(List.of(c1, c2));

        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals(2L, dtos.get(1).getId());
    }

    @Test
    @DisplayName("Debe retornar lista vacía si la lista de entidades es nula o vacía")
    void shouldReturnEmptyListWhenInputIsNullOrEmpty() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("Debe construir una entidad ComentariosIncidencias mediante toEntity")
    void shouldConstructEntityViaToEntity() {
        Incidencia incident = new Incidencia();
        incident.setId(100L);
        Usuario author = new Usuario("user@test.es", "hash", "User");
        String text = "Comentario de prueba";

        ComentariosIncidencias comment = mapper.toEntity(incident, author, text, false);

        assertNotNull(comment);
        assertEquals(incident, comment.getIncident());
        assertEquals(author, comment.getAuthor());
        assertEquals(text, comment.getComment());
        assertFalse(comment.getIsInternal());
        assertNotNull(comment.getCreatedAt());
    }
}
