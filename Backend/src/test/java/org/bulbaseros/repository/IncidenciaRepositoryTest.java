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

    @Test
    @DisplayName("Deberia recuperar incidencias paginadas de un ciudadano ordenadas descendentemente por fecha")
    void shouldFindPagedIncidentsByCitizenIdOrderedByCreatedAtDesc() {
        Usuario otherCitizen = new Usuario("otro.ciudadano@urbanpulse.es", "$2a$10$hashedPass", "Otro Ciudadano");
        entityManager.persist(otherCitizen);

        Incidencia inc1 = new Incidencia("Aviso Antiguo", "Desc 1", "Alumbrado", 36.72, -4.42, citizen);
        inc1.setCreatedAt(OffsetDateTime.now().minusDays(3));
        entityManager.persist(inc1);

        Incidencia inc2 = new Incidencia("Aviso Medio", "Desc 2", "Limpieza", 36.73, -4.43, citizen);
        inc2.setCreatedAt(OffsetDateTime.now().minusDays(1));
        entityManager.persist(inc2);

        Incidencia inc3 = new Incidencia("Aviso Reciente", "Desc 3", "Parques", 36.74, -4.44, citizen);
        inc3.setCreatedAt(OffsetDateTime.now());
        entityManager.persist(inc3);

        Incidencia incOther = new Incidencia("Aviso Ajeno", "Desc otro", "Vias", 36.75, -4.45, otherCitizen);
        entityManager.persist(incOther);

        entityManager.flush();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                0, 2, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        org.springframework.data.domain.Page<Incidencia> pageResult = incidenciaRepository.findByCitizenId(citizen.getId(), pageable);

        assertNotNull(pageResult);
        assertEquals(3, pageResult.getTotalElements(), "El ciudadano debe tener un total de 3 incidencias");
        assertEquals(2, pageResult.getTotalPages(), "Con tamano 2 y 3 incidencias debe haber 2 paginas");
        assertEquals(2, pageResult.getContent().size(), "La primera pagina debe contener 2 elementos");
        assertEquals("Aviso Reciente", pageResult.getContent().get(0).getTitle(), "El primer elemento debe ser el mas reciente");
        assertEquals("Aviso Medio", pageResult.getContent().get(1).getTitle());
    }

    @Test
    @DisplayName("Deberia recuperar incidencias paginadas de un ciudadano filtradas por estado")
    void shouldFindPagedIncidentsByCitizenIdAndStatus() {
        Incidencia inc1 = new Incidencia("Aviso Reportado 1", "Desc", "Alumbrado", 36.72, -4.42, citizen);
        inc1.setStatus(EstadoIncidencia.REPORTED);
        entityManager.persist(inc1);

        Incidencia inc2 = new Incidencia("Aviso Reportado 2", "Desc", "Limpieza", 36.73, -4.43, citizen);
        inc2.setStatus(EstadoIncidencia.REPORTED);
        entityManager.persist(inc2);

        Incidencia inc3 = new Incidencia("Aviso Resuelto", "Desc", "Parques", 36.74, -4.44, citizen);
        inc3.setStatus(EstadoIncidencia.RESOLVED);
        entityManager.persist(inc3);

        entityManager.flush();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        org.springframework.data.domain.Page<Incidencia> reportedPage = incidenciaRepository.findByCitizenIdAndStatus(
                citizen.getId(), EstadoIncidencia.REPORTED, pageable
        );

        assertEquals(2, reportedPage.getTotalElements());
        assertEquals(2, reportedPage.getContent().size());
        assertTrue(reportedPage.getContent().stream().allMatch(i -> i.getStatus() == EstadoIncidencia.REPORTED));

        org.springframework.data.domain.Page<Incidencia> inProgressPage = incidenciaRepository.findByCitizenIdAndStatus(
                citizen.getId(), EstadoIncidencia.IN_PROGRESS, pageable
        );
        assertEquals(0, reportedPage.getTotalElements() > 0 ? inProgressPage.getTotalElements() : 0);
        assertTrue(inProgressPage.getContent().isEmpty());
    }
}
