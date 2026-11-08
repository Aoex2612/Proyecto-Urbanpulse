package org.bulbaseros.repository;

import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias y de integración sobre la capa de persistencia para las consultas
 * analíticas introducidas en {@link IncidenciaRepository}, {@link CambiosEstadoRepository}
 * y {@link TareasRepository}.
 * <p>
 * Verifica el correcto funcionamiento de los filtros temporales, territoriales por distrito y barrio,
 * ordenamiento cronológico, recuperación de asignaciones departamentales activas e históricas,
 * y la robustez ante colecciones vacías o parámetros nulos.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class AnalyticsRepositoriesTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private CambiosEstadoRepository cambiosEstadoRepository;

    @Autowired
    private TareasRepository tareasRepository;

    private Usuario citizen;
    private Usuario operator;
    private Usuario technician;
    private Departamento deptVias;
    private Departamento deptAlumbrado;

    @BeforeEach
    void setUp() {
        deptVias = new Departamento("Vías Públicas", "Reparación de calzadas y aceras");
        deptVias = entityManager.persistAndFlush(deptVias);

        deptAlumbrado = new Departamento("Alumbrado", "Mantenimiento de luminarias públicas");
        deptAlumbrado = entityManager.persistAndFlush(deptAlumbrado);

        citizen = new Usuario("ciudadano.analitica@urbanpulse.es", "$2a$10$hashedPass", "Vecino Analítica", RolUsuario.CITIZEN);
        citizen = entityManager.persistAndFlush(citizen);

        operator = new Usuario("operador.analitica@urbanpulse.es", "$2a$10$hashedPass", "Operador Triaje", RolUsuario.OPERATOR);
        operator = entityManager.persistAndFlush(operator);

        technician = new Usuario("tecnico.analitica@urbanpulse.es", "$2a$10$hashedPass", "Técnico Especialista", RolUsuario.TECHNICIAN, deptVias);
        technician = entityManager.persistAndFlush(technician);
    }

    private Incidencia createIncident(String title, String category, String district, String neighborhood,
                                     EstadoIncidencia status, OffsetDateTime createdAt) {
        Incidencia inc = new Incidencia(title, "Descripción detallada de " + title, category, 36.72, -4.42, citizen);
        inc.setDistrict(district);
        inc.setNeighborhood(neighborhood);
        inc.setStatus(status);
        inc.setPriority(PrioridadIncidencia.HIGH);
        inc = entityManager.persistAndFlush(inc);
        if (createdAt != null) {
            entityManager.getEntityManager()
                    .createNativeQuery("UPDATE incidents SET created_at = :createdAt WHERE id = :id")
                    .setParameter("createdAt", createdAt)
                    .setParameter("id", inc.getId())
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
        }
        return inc;
    }

    private CambiosEstado createStatusChange(Incidencia incident, String fromStatus, String toStatus, OffsetDateTime createdAt) {
        CambiosEstado c = new CambiosEstado(incident, fromStatus, toStatus, operator, "Motivo " + toStatus);
        c = entityManager.persistAndFlush(c);
        if (createdAt != null) {
            entityManager.getEntityManager()
                    .createNativeQuery("UPDATE status_changes SET created_at = :createdAt WHERE id = :id")
                    .setParameter("createdAt", createdAt)
                    .setParameter("id", c.getId())
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();
        }
        return c;
    }

    @Test
    @DisplayName("Debería filtrar incidencias por rango temporal y territorio (distrito y barrio)")
    void shouldFilterIncidentsByDateRangeAndTerritory() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime fiveDaysAgo = now.minusDays(5);
        OffsetDateTime tenDaysAgo = now.minusDays(10);
        OffsetDateTime twentyDaysAgo = now.minusDays(20);

        // En rango (hace 5 días), Distrito Centro, Barrio Soho
        createIncident("Bache en Alameda", "Vías Públicas", "Centro", "Soho", EstadoIncidencia.VALIDATED, fiveDaysAgo);
        // En rango (hace 10 días), Distrito Centro, Barrio La Merced
        createIncident("Farola rota en Plaza", "Alumbrado", "Centro", "La Merced", EstadoIncidencia.REPORTED, tenDaysAgo);
        // En rango (hace 8 días), Distrito Este, Barrio El Palo
        createIncident("Contenedor quemado", "Limpieza", "Este", "El Palo", EstadoIncidencia.IN_PROGRESS, now.minusDays(8));
        // Fuera de rango (hace 20 días), Distrito Centro, Barrio Soho
        createIncident("Antigua papelera", "Limpieza", "Centro", "Soho", EstadoIncidencia.CLOSED, twentyDaysAgo);

        OffsetDateTime rangeStart = now.minusDays(15);
        OffsetDateTime rangeEnd = now;

        // 1. Filtrar por rango y distrito "Centro" (case-insensitive) sin especificar barrio
        List<Incidencia> centroIncidents = incidenciaRepository.findByDateRangeAndTerritory(rangeStart, rangeEnd, "centro", null);
        assertEquals(2, centroIncidents.size(), "Debe recuperar las 2 incidencias en Centro dentro de los últimos 15 días");

        // 2. Filtrar por rango, distrito "Centro" y barrio "Soho"
        List<Incidencia> sohoIncidents = incidenciaRepository.findByDateRangeAndTerritory(rangeStart, rangeEnd, "Centro", "Soho");
        assertEquals(1, sohoIncidents.size());
        assertEquals("Bache en Alameda", sohoIncidents.get(0).getTitle());

        // 3. Filtrar sin distrito ni barrio (todos en rango)
        List<Incidencia> allInRange = incidenciaRepository.findByDateRangeAndTerritory(rangeStart, rangeEnd, null, "");
        assertEquals(3, allInRange.size(), "Debe retornar todas las incidencias dentro del rango temporal");
    }

    @Test
    @DisplayName("Debería retornar lista vacía cuando no existan incidencias en el rango temporal")
    void shouldReturnEmptyListWhenNoIncidentsMatchDateRange() {
        OffsetDateTime now = OffsetDateTime.now();
        createIncident("Incidencia antigua", "Vías Públicas", "Centro", "Soho", EstadoIncidencia.CLOSED, now.minusDays(60));

        OffsetDateTime start = now.minusDays(7);
        OffsetDateTime end = now;

        List<Incidencia> results = incidenciaRepository.findByDateRangeAndTerritory(start, end, null, null);
        assertNotNull(results);
        assertTrue(results.isEmpty(), "La lista debe estar vacía para un rango temporal sin incidencias");
    }

    @Test
    @DisplayName("Debería ordenar incidencias descendentemente por createdAt con findByCreatedAtBetweenOrderByCreatedAtDesc")
    void shouldOrderIncidentsByCreatedAtDesc() {
        OffsetDateTime now = OffsetDateTime.now();
        Incidencia inc1 = createIncident("Incidencia día 3", "Vías Públicas", "Centro", "Soho", EstadoIncidencia.REPORTED, now.minusDays(3));
        Incidencia inc2 = createIncident("Incidencia día 1", "Vías Públicas", "Centro", "Soho", EstadoIncidencia.REPORTED, now.minusDays(1));
        Incidencia inc3 = createIncident("Incidencia día 2", "Vías Públicas", "Centro", "Soho", EstadoIncidencia.REPORTED, now.minusDays(2));

        List<Incidencia> results = incidenciaRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(now.minusDays(5), now);
        assertEquals(3, results.size());
        assertEquals(inc2.getId(), results.get(0).getId(), "La más reciente (día 1) debe ser la primera");
        assertEquals(inc3.getId(), results.get(1).getId(), "La intermedia (día 2) debe ser la segunda");
        assertEquals(inc1.getId(), results.get(2).getId(), "La más antigua (día 3) debe ser la última");
    }

    @Test
    @DisplayName("Debería recuperar transiciones de estado por lote de IDs ordenadas ascendentemente")
    void shouldFindStatusChangesByIncidentIdInOrderedByCreatedAtAsc() {
        OffsetDateTime now = OffsetDateTime.now();
        Incidencia inc1 = createIncident("Incidencia 1", "Vías Públicas", "Centro", "Soho", EstadoIncidencia.IN_PROGRESS, now.minusDays(5));
        Incidencia inc2 = createIncident("Incidencia 2", "Alumbrado", "Este", "El Palo", EstadoIncidencia.RESOLVED, now.minusDays(4));

        createStatusChange(inc1, "REPORTED", "VALIDATED", now.minusDays(5).plusHours(2));
        createStatusChange(inc1, "VALIDATED", "IN_PROGRESS", now.minusDays(5).plusHours(6));
        createStatusChange(inc2, "REPORTED", "VALIDATED", now.minusDays(4).plusHours(1));
        createStatusChange(inc2, "VALIDATED", "RESOLVED", now.minusDays(4).plusHours(10));

        List<CambiosEstado> transitions = cambiosEstadoRepository.findByIncidentIdInOrderByCreatedAtAsc(List.of(inc1.getId(), inc2.getId()));
        assertEquals(4, transitions.size(), "Debe recuperar todas las transiciones de las incidencias solicitadas");
        assertTrue(transitions.get(0).getCreatedAt().isBefore(transitions.get(1).getCreatedAt())
                || transitions.get(0).getCreatedAt().isEqual(transitions.get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("Debería recuperar transiciones hacia un estado específico con findByIncidentIdAndToStatus")
    void shouldFindStatusChangesByIncidentIdAndToStatus() {
        OffsetDateTime now = OffsetDateTime.now();
        Incidencia inc = createIncident("Incidencia Muestra", "Vías Públicas", "Centro", "Soho", EstadoIncidencia.RESOLVED, now.minusDays(2));

        createStatusChange(inc, "REPORTED", "VALIDATED", now.minusDays(2).plusHours(1));
        createStatusChange(inc, "VALIDATED", "RESOLVED", now.minusDays(1));

        // Búsqueda por String
        List<CambiosEstado> validatedChanges = cambiosEstadoRepository.findByIncidentIdAndToStatus(inc.getId(), "VALIDATED");
        assertEquals(1, validatedChanges.size());
        assertEquals("VALIDATED", validatedChanges.get(0).getToStatus());

        // Búsqueda por Enum sobrecargado
        List<CambiosEstado> resolvedChanges = cambiosEstadoRepository.findByIncidentIdAndToStatus(inc.getId(), EstadoIncidencia.RESOLVED);
        assertEquals(1, resolvedChanges.size());
        assertEquals("RESOLVED", resolvedChanges.get(0).getToStatus());
    }

    @Test
    @DisplayName("Debería recuperar asignaciones de tareas por lista de incidencias e inactividad lógica")
    void shouldFindTasksByIncidentIdInAndActiveStatus() {
        Incidencia inc1 = createIncident("Incidencia Tarea 1", "Vías Públicas", "Centro", "Soho", EstadoIncidencia.IN_PROGRESS, OffsetDateTime.now());
        Incidencia inc2 = createIncident("Incidencia Tarea 2", "Alumbrado", "Este", "El Palo", EstadoIncidencia.VALIDATED, OffsetDateTime.now());

        // Tarea 1: activa en Vías
        Tareas t1 = new Tareas(inc1, deptVias, technician, operator);
        t1.setIsActive(true);
        entityManager.persist(t1);

        // Tarea 2: inactiva en Vías (reasignada)
        Tareas t2 = new Tareas(inc1, deptAlumbrado, null, operator);
        t2.setIsActive(false);
        entityManager.persist(t2);

        // Tarea 3: activa en Alumbrado para inc2
        Tareas t3 = new Tareas(inc2, deptAlumbrado, null, operator);
        t3.setIsActive(true);
        entityManager.persist(t3);

        entityManager.flush();

        // 1. Todas las tareas de ambas incidencias
        List<Tareas> allTasks = tareasRepository.findByIncidentIdIn(List.of(inc1.getId(), inc2.getId()));
        assertEquals(3, allTasks.size());

        // 2. Solo las tareas activas de ambas incidencias
        List<Tareas> activeTasks = tareasRepository.findByIncidentIdInAndIsActiveTrue(List.of(inc1.getId(), inc2.getId()));
        assertEquals(2, activeTasks.size());
        assertTrue(activeTasks.stream().allMatch(Tareas::getIsActive));

        // 3. Tareas por departamento
        List<Tareas> viasTasks = tareasRepository.findByDepartmentId(deptVias.getId());
        assertEquals(1, viasTasks.size());

        List<Tareas> alumbradoActiveTasks = tareasRepository.findByDepartmentIdAndIsActiveTrue(deptAlumbrado.getId());
        assertEquals(1, alumbradoActiveTasks.size());
        assertEquals(inc2.getId(), alumbradoActiveTasks.get(0).getIncident().getId());
    }
}
