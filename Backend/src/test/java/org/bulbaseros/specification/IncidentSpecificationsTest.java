package org.bulbaseros.specification;

import org.bulbaseros.dto.IncidentFilterCriteria;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.repository.IncidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de {@link IncidentSpecifications} contra la base de datos en memoria H2.
 * <p>
 * Verifica la no-regresión de los filtros previos (texto, categoría, fechas y estado) y el
 * comportamiento real de los nuevos predicados de distrito, prioridad y departamento, así como
 * el control de activación de filtros internos (RBAC) y la combinación lógica {@code AND}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@DataJpaTest
class IncidentSpecificationsTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    private Departamento deptAlumbrado;
    private Departamento deptVias;
    private Incidencia incCentro1;
    private Incidencia incCentro2;
    private Incidencia incSinDistrito;
    private Incidencia incDistritoVacio;
    private Incidencia incNorte;

    @BeforeEach
    void setUp() {
        Usuario citizen = entityManager.persistAndFlush(
                new Usuario("vecino.filtros@urbanpulse.es", "$2a$10$xyzHashedPass", "Vecino Filtros", RolUsuario.CITIZEN));
        Usuario operator = entityManager.persistAndFlush(
                new Usuario("operador.filtros@urbanpulse.es", "$2a$10$xyzHashedPass", "Operador Filtros", RolUsuario.OPERATOR));

        deptAlumbrado = entityManager.persistAndFlush(new Departamento("Alumbrado Publico", "Mantenimiento de luminarias"));
        deptVias = entityManager.persistAndFlush(new Departamento("Vias Publicas", "Reparacion de calzadas"));

        OffsetDateTime old = OffsetDateTime.now().minusDays(10);

        incCentro1 = persistIncident("Farola rota en el centro", "Alumbrado", "Centro",
                PrioridadIncidencia.HIGH, EstadoIncidencia.IN_PROGRESS, citizen);
        incCentro2 = persistIncident("Bache en calle del centro", "Vias Publicas", "  CENTRO  ",
                PrioridadIncidencia.LOW, EstadoIncidencia.REPORTED, citizen);
        incSinDistrito = persistIncident("Farola apagada sin distrito", "Alumbrado", null,
                PrioridadIncidencia.MEDIUM, EstadoIncidencia.REPORTED, citizen);
        incDistritoVacio = persistIncident("Contenedor desbordado", "Limpieza", "",
                PrioridadIncidencia.CRITICAL, EstadoIncidencia.REPORTED, citizen);
        incNorte = persistIncident("Bache en distrito norte", "Vias Publicas", "Norte",
                PrioridadIncidencia.CRITICAL, EstadoIncidencia.REPORTED, citizen);

        updateCreatedAt(incCentro1, old);

        persistTask(incCentro1, deptAlumbrado, operator, true);
        persistTask(incCentro2, deptVias, operator, true);
        persistTask(incCentro2, deptAlumbrado, operator, true);
        persistTask(incNorte, deptVias, operator, false);
    }

    @Test
    @DisplayName("hasTextSearch: filtra por título sin alterar el comportamiento previo")
    void shouldFilterByText() {
        assertEquals(Set.of(incCentro1.getId(), incSinDistrito.getId()),
                ids(findAll(IncidentSpecifications.hasTextSearch("farola"))));
    }

    @Test
    @DisplayName("hasCategory: filtra por categoría insensible a mayúsculas")
    void shouldFilterByCategoryIgnoringCase() {
        assertEquals(Set.of(incCentro1.getId(), incSinDistrito.getId()),
                ids(findAll(IncidentSpecifications.hasCategory("alumbrado"))));
    }

    @Test
    @DisplayName("hasCreatedAfter y hasCreatedBefore: mantienen el rango temporal previo")
    void shouldFilterByDateRange() {
        OffsetDateTime limit = OffsetDateTime.now().minusDays(5);

        assertEquals(4, findAll(IncidentSpecifications.hasCreatedAfter(limit)).size());
        assertEquals(Set.of(incCentro1.getId()), ids(findAll(IncidentSpecifications.hasCreatedBefore(limit))));
    }

    @Test
    @DisplayName("hasStatus: mantiene el filtro por estado del ciclo de vida")
    void shouldFilterByStatus() {
        assertEquals(Set.of(incCentro1.getId()),
                ids(findAll(IncidentSpecifications.hasStatus(EstadoIncidencia.IN_PROGRESS))));
    }

    @Test
    @DisplayName("hasDistrict: compara sin distinguir mayúsculas ni espacios exteriores")
    void shouldFilterByDistrictIgnoringCaseAndOuterSpaces() {
        Set<Long> expected = Set.of(incCentro1.getId(), incCentro2.getId());

        assertEquals(expected, ids(findAll(IncidentSpecifications.hasDistrict("centro"))));
        assertEquals(expected, ids(findAll(IncidentSpecifications.hasDistrict("  CENTRO  "))));
    }

    @Test
    @DisplayName("C-1: distrito nulo o vacío se excluye al filtrar y aparece sin filtro de distrito")
    void shouldExcludeNullOrEmptyDistrictWhenFiltering() {
        List<Incidencia> filtered = findAll(IncidentSpecifications.hasDistrict("Centro"));
        assertFalse(ids(filtered).contains(incSinDistrito.getId()), "Distrito nulo no debe coincidir");
        assertFalse(ids(filtered).contains(incDistritoVacio.getId()), "Distrito vacío no debe coincidir");

        assertEquals(5, findAll(IncidentSpecifications.build(
                new IncidentFilterCriteria(null, null, null, null, null, null, null, null, false, false))).size(),
                "Sin filtro de distrito deben aparecer todas las incidencias");
    }

    @Test
    @DisplayName("hasPriority: filtra por cada nivel LOW, MEDIUM, HIGH y CRITICAL")
    void shouldFilterByEachPriority() {
        assertEquals(Set.of(incCentro2.getId()), ids(findAll(IncidentSpecifications.hasPriority(PrioridadIncidencia.LOW))));
        assertEquals(Set.of(incSinDistrito.getId()), ids(findAll(IncidentSpecifications.hasPriority(PrioridadIncidencia.MEDIUM))));
        assertEquals(Set.of(incCentro1.getId()), ids(findAll(IncidentSpecifications.hasPriority(PrioridadIncidencia.HIGH))));
        assertEquals(Set.of(incDistritoVacio.getId(), incNorte.getId()),
                ids(findAll(IncidentSpecifications.hasPriority(PrioridadIncidencia.CRITICAL))));
    }

    @Test
    @DisplayName("hasDepartment: solo cuentan las tareas activas y admite varios departamentos por incidencia (C-2)")
    void shouldFilterByActiveDepartment() {
        assertEquals(Set.of(incCentro1.getId(), incCentro2.getId()),
                ids(findAll(IncidentSpecifications.hasDepartment(deptAlumbrado.getId()))));
        assertEquals(Set.of(incCentro2.getId()),
                ids(findAll(IncidentSpecifications.hasDepartment(deptVias.getId()))),
                "La tarea inactiva de Vías en la incidencia Norte no debe contar");
    }

    @Test
    @DisplayName("hasNoDepartment: devuelve las incidencias sin tareas activas (incluida la de tarea inactiva)")
    void shouldFilterByIncidentsWithoutActiveDepartment() {
        assertEquals(Set.of(incSinDistrito.getId(), incDistritoVacio.getId(), incNorte.getId()),
                ids(findAll(IncidentSpecifications.hasNoDepartment())));
    }

    @Test
    @DisplayName("build: combina distrito, prioridad y departamento con AND")
    void shouldCombineCriteriaWithAnd() {
        IncidentFilterCriteria criteria = new IncidentFilterCriteria(
                null, null, null, null, null, "Centro",
                PrioridadIncidencia.HIGH, deptAlumbrado.getId(), false, true);

        assertEquals(Set.of(incCentro1.getId()), ids(findAll(IncidentSpecifications.build(criteria))));
    }

    @Test
    @DisplayName("build: ignora prioridad, departamento y sin departamento si includeInternalFilters es false")
    void shouldIgnoreInternalFiltersWhenNotAllowed() {
        IncidentFilterCriteria criteria = new IncidentFilterCriteria(
                null, null, null, null, EstadoIncidencia.IN_PROGRESS, null,
                PrioridadIncidencia.HIGH, deptVias.getId(), true, false);

        assertEquals(5, findAll(IncidentSpecifications.build(criteria)).size(),
                "Para un ciudadano deben aplicarse solo los filtros públicos");
    }

    @Test
    @DisplayName("build: aplica prioridad, departamento y sin departamento si includeInternalFilters es true")
    void shouldApplyInternalFiltersWhenAllowed() {
        IncidentFilterCriteria byPriorityAndDepartment = new IncidentFilterCriteria(
                null, null, null, null, null, null,
                PrioridadIncidencia.HIGH, deptAlumbrado.getId(), false, true);
        assertEquals(Set.of(incCentro1.getId()),
                ids(findAll(IncidentSpecifications.build(byPriorityAndDepartment))));

        IncidentFilterCriteria unassigned = new IncidentFilterCriteria(
                null, null, null, null, null, null, null, null, true, true);
        assertEquals(Set.of(incSinDistrito.getId(), incDistritoVacio.getId(), incNorte.getId()),
                ids(findAll(IncidentSpecifications.build(unassigned))));
    }

    @Test
    @DisplayName("hasDistrict, hasPriority y hasDepartment: retornan null con valores vacíos")
    void shouldReturnNullSpecificationsForEmptyValues() {
        assertNull(IncidentSpecifications.hasDistrict(null));
        assertNull(IncidentSpecifications.hasDistrict("   "));
        assertNull(IncidentSpecifications.hasPriority(null));
        assertNull(IncidentSpecifications.hasDepartment(null));
        assertNotNull(IncidentSpecifications.hasNoDepartment());
    }

    @Test
    @DisplayName("build: sobrecarga previa mantiene el comportamiento sin excepciones")
    void shouldBuildLegacyOverload() {
        assertNotNull(IncidentSpecifications.build(
                "farola", "Alumbrado", OffsetDateTime.now().minusDays(15), OffsetDateTime.now(),
                EstadoIncidencia.RESOLVED, false));
        assertEquals(5, findAll(IncidentSpecifications.build(null, null, null, null, null, false)).size());
    }

    private Incidencia persistIncident(String title, String category, String district,
                                       PrioridadIncidencia priority, EstadoIncidencia status,
                                       Usuario citizen) {
        Incidencia incidencia = new Incidencia(title, "Descripcion de " + title, category,
                36.72, -4.42, citizen);
        incidencia.setDistrict(district);
        incidencia.setPriority(priority);
        incidencia.setStatus(status);
        return entityManager.persistAndFlush(incidencia);
    }

    /**
     * {@code @CreationTimestamp} sobrescribe el valor en el insert; se ajusta después por SQL nativo.
     */
    private void updateCreatedAt(Incidencia incidencia, OffsetDateTime createdAt) {
        entityManager.getEntityManager()
                .createNativeQuery("UPDATE incidents SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", incidencia.getId())
                .executeUpdate();
    }

    private void persistTask(Incidencia incident, Departamento department, Usuario assignedBy, boolean active) {
        Tareas tarea = new Tareas(incident, department, null, assignedBy);
        tarea.setIsActive(active);
        entityManager.persistAndFlush(tarea);
    }

    private List<Incidencia> findAll(Specification<Incidencia> spec) {
        return incidenciaRepository.findAll(spec);
    }

    private Set<Long> ids(List<Incidencia> incidents) {
        return incidents.stream().map(Incidencia::getId).collect(Collectors.toSet());
    }
}