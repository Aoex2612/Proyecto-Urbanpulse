package org.bulbaseros.repository;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion y persistencia para el repositorio {@link IncidenciaRepository}.
 * <p>
 * Verifica operaciones sobre la base de datos en memoria H2, con especial foco en la consulta
 * {@code findByStatusNotIn} para recuperar incidencias activas en el mapa.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class IncidenciaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    private Usuario citizen;

    @BeforeEach
    void setUp() {
        citizen = new Usuario("ciudadano.mapa@urbanpulse.es", "$2a$10$hashedPass", "Ciudadano Mapa");
        citizen = entityManager.persistAndFlush(citizen);
    }

    @Test
    @DisplayName("Deberia persistir y recuperar una incidencia por su ID")
    void shouldPersistAndFindById() {
        Incidencia inc = new Incidencia(
                "Semaforo averiado",
                "Semaforo intermitente en cruce peligroso",
                "Vias Publicas",
                36.7213,
                -4.4214,
                citizen
        );
        inc = incidenciaRepository.save(inc);

        Optional<Incidencia> found = incidenciaRepository.findById(inc.getId());

        assertTrue(found.isPresent());
        assertEquals("Semaforo averiado", found.get().getTitle());
        assertEquals(EstadoIncidencia.REPORTED, found.get().getStatus());
        assertEquals(PrioridadIncidencia.MEDIUM, found.get().getPriority());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    @DisplayName("Deberia filtrar y excluir incidencias terminadas mediante findByStatusNotIn con enums")
    void shouldExcludeFinishedIncidentsUsingEnumCollection() {
        Incidencia inc1 = new Incidencia("Incidencia 1", "Desc 1", "Alumbrado", 36.72, -4.42, citizen);
        inc1.setStatus(EstadoIncidencia.REPORTED);
        entityManager.persist(inc1);

        Incidencia inc2 = new Incidencia("Incidencia 2", "Desc 2", "Limpieza", 36.73, -4.43, citizen);
        inc2.setStatus(EstadoIncidencia.IN_PROGRESS);
        entityManager.persist(inc2);

        Incidencia inc3 = new Incidencia("Incidencia 3", "Desc 3", "Parques", 36.74, -4.44, citizen);
        inc3.setStatus(EstadoIncidencia.CLOSED);
        entityManager.persist(inc3);

        Incidencia inc4 = new Incidencia("Incidencia 4", "Desc 4", "Mobiliario", 36.75, -4.45, citizen);
        inc4.setStatus(EstadoIncidencia.REJECTED);
        entityManager.persist(inc4);

        entityManager.flush();

        List<Incidencia> activeIncidents = incidenciaRepository.findByStatusNotIn(
                List.of(EstadoIncidencia.CLOSED, EstadoIncidencia.REJECTED)
        );

        assertEquals(2, activeIncidents.size(), "Solo deben recuperarse incidencias no terminadas");
        assertTrue(activeIncidents.stream().anyMatch(i -> i.getStatus() == EstadoIncidencia.REPORTED));
        assertTrue(activeIncidents.stream().anyMatch(i -> i.getStatus() == EstadoIncidencia.IN_PROGRESS));
        assertFalse(activeIncidents.stream().anyMatch(i -> i.getStatus() == EstadoIncidencia.CLOSED));
        assertFalse(activeIncidents.stream().anyMatch(i -> i.getStatus() == EstadoIncidencia.REJECTED));
    }

    @Test
    @DisplayName("Deberia filtrar y excluir incidencias terminadas mediante findByStatusNotInStrings con cadenas")
    void shouldExcludeFinishedIncidentsUsingStringCollection() {
        Incidencia inc1 = new Incidencia("Aviso activo", "Desc", "Alumbrado", 36.72, -4.42, citizen);
        inc1.setStatus(EstadoIncidencia.VALIDATED);
        entityManager.persist(inc1);

        Incidencia inc2 = new Incidencia("Aviso cerrado", "Desc", "Alumbrado", 36.73, -4.43, citizen);
        inc2.setStatus(EstadoIncidencia.CLOSED);
        entityManager.persist(inc2);

        entityManager.flush();

        List<Incidencia> result = incidenciaRepository.findByStatusNotInStrings(List.of("CLOSED", "REJECTED"));

        assertEquals(1, result.size());
        assertEquals(EstadoIncidencia.VALIDATED, result.get(0).getStatus());
    }
}
