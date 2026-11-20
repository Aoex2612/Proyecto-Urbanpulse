package org.bulbaseros.repository;

import org.bulbaseros.entity.ComentariosIncidencias;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración y persistencia para el repositorio {@link ComentariosIncidenciasRepository}.
 * <p>
 * Verifica mediante {@link DataJpaTest} la ordenación cronológica de comentarios,
 * la segregación estricta de notas internas vs comentarios públicos y la persistencia de {@code updatedAt}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class ComentariosIncidenciasRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ComentariosIncidenciasRepository comentariosRepository;

    private Incidencia incident1;
    private Incidencia incident2;
    private Usuario citizen;
    private Usuario operator;

    @BeforeEach
    void setUp() {
        citizen = new Usuario("vecino.comentarios@urbanpulse.es", "$2a$10$xyzHashedPass", "Vecino Comentarios", RolUsuario.CITIZEN);
        citizen = entityManager.persistAndFlush(citizen);

        operator = new Usuario("operador.comentarios@urbanpulse.es", "$2a$10$xyzHashedPass", "Operador Comentarios", RolUsuario.OPERATOR);
        operator = entityManager.persistAndFlush(operator);

        incident1 = new Incidencia("Fuga de agua", "Tubería rota en vía pública", "Agua", 36.71, -4.42, citizen);
        incident1 = entityManager.persistAndFlush(incident1);

        incident2 = new Incidencia("Semáforo apagado", "Cruce peligroso sin semáforo", "Tráfico", 36.72, -4.43, citizen);
        incident2 = entityManager.persistAndFlush(incident2);
    }

    @Test
    @DisplayName("Debería retornar todos los comentarios (internos y públicos) en orden cronológico ascendente")
    void shouldReturnAllCommentsOrderedByCreatedAtAsc() {
        OffsetDateTime now = OffsetDateTime.now();

        ComentariosIncidencias firstPublic = new ComentariosIncidencias(incident1, citizen, "Primer aviso vecinal", false);
        firstPublic.setCreatedAt(now.minusHours(3));
        entityManager.persistAndFlush(firstPublic);

        ComentariosIncidencias secondInternal = new ComentariosIncidencias(incident1, operator, "Nota técnica interna: enviando cuadrilla", true);
        secondInternal.setCreatedAt(now.minusHours(2));
        entityManager.persistAndFlush(secondInternal);

        ComentariosIncidencias thirdPublic = new ComentariosIncidencias(incident1, operator, "Aviso público: corte de carril previsto", false);
        thirdPublic.setCreatedAt(now.minusHours(1));
        entityManager.persistAndFlush(thirdPublic);

        // Comentario de otra incidencia
        ComentariosIncidencias otherIncidentComment = new ComentariosIncidencias(incident2, citizen, "Comentario de otra incidencia", false);
        otherIncidentComment.setCreatedAt(now);
        entityManager.persistAndFlush(otherIncidentComment);

        List<ComentariosIncidencias> results = comentariosRepository.findByIncidentIdOrderByCreatedAtAsc(incident1.getId());

        assertEquals(3, results.size(), "Debe recuperar exactamente los 3 comentarios asociados a incident1");
        assertEquals("Primer aviso vecinal", results.get(0).getComment());
        assertEquals("Nota técnica interna: enviando cuadrilla", results.get(1).getComment());
        assertEquals("Aviso público: corte de carril previsto", results.get(2).getComment());
        assertTrue(results.get(0).getCreatedAt().isBefore(results.get(1).getCreatedAt()));
        assertTrue(results.get(1).getCreatedAt().isBefore(results.get(2).getCreatedAt()));
    }

    @Test
    @DisplayName("Debería retornar únicamente comentarios públicos (isInternal = false) segregando notas internas")
    void shouldReturnOnlyPublicCommentsFilteringInternalNotes() {
        OffsetDateTime now = OffsetDateTime.now();

        ComentariosIncidencias publicComment1 = new ComentariosIncidencias(incident1, citizen, "Aviso público 1", false);
        publicComment1.setCreatedAt(now.minusHours(3));
        entityManager.persistAndFlush(publicComment1);

        ComentariosIncidencias internalNote = new ComentariosIncidencias(incident1, operator, "Nota interna confidencial de coordinación", true);
        internalNote.setCreatedAt(now.minusHours(2));
        entityManager.persistAndFlush(internalNote);

        ComentariosIncidencias publicComment2 = new ComentariosIncidencias(incident1, operator, "Aviso público 2: intervención iniciada", false);
        publicComment2.setCreatedAt(now.minusHours(1));
        entityManager.persistAndFlush(publicComment2);

        List<ComentariosIncidencias> publicResults = comentariosRepository.findByIncidentIdAndIsInternalFalseOrderByCreatedAtAsc(incident1.getId());

        assertEquals(2, publicResults.size(), "Solo deben devolverse los 2 comentarios públicos");
        assertFalse(publicResults.get(0).getIsInternal(), "El primer comentario debe ser público");
        assertEquals("Aviso público 1", publicResults.get(0).getComment());
        assertFalse(publicResults.get(1).getIsInternal(), "El segundo comentario debe ser público");
        assertEquals("Aviso público 2: intervención iniciada", publicResults.get(1).getComment());

        // Asegurar que ninguna nota interna está presente en los resultados
        boolean containsInternal = publicResults.stream().anyMatch(ComentariosIncidencias::getIsInternal);
        assertFalse(containsInternal, "Ningún resultado debe tener isInternal = true");
    }

    @Test
    @DisplayName("Debería persistir y actualizar el campo updatedAt al editar un comentario")
    void shouldPersistAndTrackUpdatedAtOnCommentEdit() {
        ComentariosIncidencias comment = new ComentariosIncidencias(incident1, operator, "Comentario original", false);
        comment = entityManager.persistAndFlush(comment);

        assertNull(comment.getUpdatedAt(), "Inicialmente updatedAt debe ser nulo");

        OffsetDateTime editTimestamp = OffsetDateTime.now().plusMinutes(15).truncatedTo(ChronoUnit.SECONDS);
        comment.setComment("Comentario con corrección de texto");
        comment.setUpdatedAt(editTimestamp);
        entityManager.persistAndFlush(comment);
        entityManager.clear();

        ComentariosIncidencias reloaded = comentariosRepository.findById(comment.getId()).orElseThrow();
        assertEquals("Comentario con corrección de texto", reloaded.getComment());
        assertNotNull(reloaded.getUpdatedAt(), "updatedAt no debe ser nulo tras la edición");
        assertEquals(editTimestamp, reloaded.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS));
    }
}
