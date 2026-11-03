package org.bulbaseros.repository;

import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion y persistencia para el repositorio {@link CambiosEstadoRepository}.
 * <p>
 * Verifica la recuperacion cronologica ascendente de la traza de auditoria de estados para una incidencia dada.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class CambiosEstadoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CambiosEstadoRepository cambiosEstadoRepository;

    private Usuario adminUser;
    private Incidencia incident1;
    private Incidencia incident2;

    @BeforeEach
    void setUp() {
        Usuario citizen = new Usuario("vecino@urbanpulse.es", "$2a$10$hashedPass", "Vecino Prueba");
        citizen = entityManager.persistAndFlush(citizen);

        adminUser = new Usuario("admin.auditoria@urbanpulse.es", "$2a$10$hashedPass", "Admin Auditor");
        adminUser = entityManager.persistAndFlush(adminUser);

        incident1 = new Incidencia("Bache profundo", "Bache en calzada", "Vias Publicas", 36.72, -4.42, citizen);
        incident1 = entityManager.persistAndFlush(incident1);

        incident2 = new Incidencia("Papelera rota", "Papelera arrancada", "Limpieza", 36.73, -4.43, citizen);
        incident2 = entityManager.persistAndFlush(incident2);
    }

    @Test
    @DisplayName("Deberia retornar el historial ordenado ascendentemente por createdAt y filtrado por incidencia")
    void shouldReturnOrderedHistoryFilteredByIncident() {
        OffsetDateTime t1 = OffsetDateTime.now().minusHours(3);
        OffsetDateTime t2 = OffsetDateTime.now().minusHours(2);
        OffsetDateTime t3 = OffsetDateTime.now().minusHours(1);

        CambiosEstado c1 = new CambiosEstado(incident1, "REPORTED", "VALIDATED", adminUser, "Revision inicial");
        c1.setCreatedAt(t1);
        entityManager.persist(c1);

        CambiosEstado c2 = new CambiosEstado(incident1, "VALIDATED", "IN_PROGRESS", adminUser, "Asignado a cuadrilla");
        c2.setCreatedAt(t2);
        entityManager.persist(c2);

        CambiosEstado c3 = new CambiosEstado(incident1, "IN_PROGRESS", "RESOLVED", adminUser, "Asfalto reparado");
        c3.setCreatedAt(t3);
        entityManager.persist(c3);

        CambiosEstado cOther = new CambiosEstado(incident2, "REPORTED", "REJECTED", adminUser, "No procede");
        cOther.setCreatedAt(OffsetDateTime.now());
        entityManager.persist(cOther);

        entityManager.flush();

        List<CambiosEstado> history = cambiosEstadoRepository.findByIncidentIdOrderByCreatedAtAsc(incident1.getId());

        assertEquals(3, history.size(), "Solo deben recuperarse los 3 hitos de la incidencia 1");
        assertEquals("REPORTED", history.get(0).getFromStatus());
        assertEquals("VALIDATED", history.get(0).getToStatus());
        assertEquals("VALIDATED", history.get(1).getFromStatus());
        assertEquals("IN_PROGRESS", history.get(1).getToStatus());
        assertEquals("IN_PROGRESS", history.get(2).getFromStatus());
        assertEquals("RESOLVED", history.get(2).getToStatus());
        assertTrue(history.get(0).getCreatedAt().isBefore(history.get(1).getCreatedAt()) || history.get(0).getCreatedAt().isEqual(history.get(1).getCreatedAt()));
        assertTrue(history.get(1).getCreatedAt().isBefore(history.get(2).getCreatedAt()) || history.get(1).getCreatedAt().isEqual(history.get(2).getCreatedAt()));
    }

    @Test
    @DisplayName("Deberia retornar lista vacia si la incidencia no tiene cambios de estado registrados")
    void shouldReturnEmptyListWhenNoStatusChangesExist() {
        List<CambiosEstado> history = cambiosEstadoRepository.findByIncidentIdOrderByCreatedAtAsc(incident1.getId());

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }
}
