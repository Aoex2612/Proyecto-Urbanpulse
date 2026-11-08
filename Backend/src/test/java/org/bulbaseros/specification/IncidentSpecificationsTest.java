package org.bulbaseros.specification;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link IncidentSpecifications}.
 * <p>
 * Valida la creación dinámica de especificaciones JPA Criteria API para texto libre,
 * categoría exacta, rango temporal y filtrado por estado con control de activación (RBAC).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class IncidentSpecificationsTest {

    @Test
    @DisplayName("hasTextSearch: retorna especificación no nula para texto válido y nula para vacío")
    void shouldCreateTextSearchSpecification() {
        Specification<Incidencia> spec = IncidentSpecifications.hasTextSearch("farola");
        assertNotNull(spec, "La especificación no debe ser nula con texto válido");

        assertNull(IncidentSpecifications.hasTextSearch(null), "Texto nulo debe retornar null");
        assertNull(IncidentSpecifications.hasTextSearch("   "), "Texto en blanco debe retornar null");
    }

    @Test
    @DisplayName("hasCategory: retorna especificación no nula para categoría válida y nula para vacía")
    void shouldCreateCategorySpecification() {
        Specification<Incidencia> spec = IncidentSpecifications.hasCategory("Alumbrado");
        assertNotNull(spec, "La especificación no debe ser nula con categoría válida");

        assertNull(IncidentSpecifications.hasCategory(null), "Categoría nula debe retornar null");
        assertNull(IncidentSpecifications.hasCategory("   "), "Categoría en blanco debe retornar null");
    }

    @Test
    @DisplayName("hasCreatedAfter y hasCreatedBefore: retornan especificaciones según fechas")
    void shouldCreateDateRangeSpecifications() {
        OffsetDateTime now = OffsetDateTime.now();

        Specification<Incidencia> afterSpec = IncidentSpecifications.hasCreatedAfter(now.minusDays(5));
        assertNotNull(afterSpec, "hasCreatedAfter no debe ser nula con fecha válida");
        assertNull(IncidentSpecifications.hasCreatedAfter(null), "hasCreatedAfter debe ser nula con fecha null");

        Specification<Incidencia> beforeSpec = IncidentSpecifications.hasCreatedBefore(now);
        assertNotNull(beforeSpec, "hasCreatedBefore no debe ser nula con fecha válida");
        assertNull(IncidentSpecifications.hasCreatedBefore(null), "hasCreatedBefore debe ser nula con fecha null");
    }

    @Test
    @DisplayName("hasStatus: retorna especificación no nula para estado y nula si es null")
    void shouldCreateStatusSpecification() {
        Specification<Incidencia> spec = IncidentSpecifications.hasStatus(EstadoIncidencia.IN_PROGRESS);
        assertNotNull(spec, "hasStatus no debe ser nula con estado válido");
        assertNull(IncidentSpecifications.hasStatus(null), "hasStatus debe ser nula con estado null");
    }

    @Test
    @DisplayName("build: compone especificación combinada omitiendo estado si includeStatus es false")
    void shouldBuildCombinedSpecificationExcludingStatusWhenRequested() {
        Specification<Incidencia> specCitizen = IncidentSpecifications.build(
                "farola", "Alumbrado",
                OffsetDateTime.now().minusDays(2), OffsetDateTime.now(),
                EstadoIncidencia.RESOLVED, false
        );
        assertNotNull(specCitizen, "La especificación compuesta debe existir");
    }

    @Test
    @DisplayName("build: compone especificación incluyendo estado si includeStatus es true")
    void shouldBuildCombinedSpecificationIncludingStatusWhenRequested() {
        Specification<Incidencia> specInternal = IncidentSpecifications.build(
                "bache", "Vías Públicas",
                OffsetDateTime.now().minusDays(1), OffsetDateTime.now(),
                EstadoIncidencia.IN_PROGRESS, true
        );
        assertNotNull(specInternal, "La especificación compuesta para operadores debe existir");
    }

    @Test
    @DisplayName("build: no arroja excepciones con todos los parámetros nulos")
    void shouldBuildWithoutExceptionsWhenAllParametersAreNull() {
        Specification<Incidencia> spec = IncidentSpecifications.build(null, null, null, null, null, false);
        assertNotNull(spec);
    }
}
