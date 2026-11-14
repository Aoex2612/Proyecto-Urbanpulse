package org.bulbaseros.specification;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.bulbaseros.dto.IncidentFilterCriteria;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.Tareas;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Clase de especificaciones JPA Criteria API para la construcción dinámica de predicados
 * de búsqueda y filtrado multicriterio de incidencias urbanas.
 * <p>
 * Proporciona métodos estáticos que generan predicados combinables mediante operador {@code and}
 * para los criterios de texto libre, categoría, rango de fechas, estado del ciclo de vida,
 * distrito, prioridad y departamento asignado.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class IncidentSpecifications {

    /**
     * Constructor privado para evitar instanciación de clase utilitaria.
     */
    private IncidentSpecifications() {
        // Clase utilitaria, no instanciable
    }

    /**
     * Genera una especificación que filtra incidencias cuyo título o descripción contienen
     * el texto especificado (búsqueda insensible a mayúsculas/minúsculas).
     *
     * @param text Cadena de texto libre a buscar en título y descripción.
     * @return Especificación de tipo {@link Specification} para el filtro de texto, o {@code null} si el texto es vacío.
     */
    public static Specification<Incidencia> hasTextSearch(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String pattern = "%" + text.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    /**
     * Genera una especificación que filtra incidencias por categoría exacta (insensible a mayúsculas).
     *
     * @param category Categoría de la incidencia a filtrar.
     * @return Especificación de tipo {@link Specification} para el filtro de categoría, o {@code null} si es vacía.
     */
    public static Specification<Incidencia> hasCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase());
    }

    /**
     * Genera una especificación que filtra incidencias creadas a partir de la fecha indicada (inclusive).
     *
     * @param start Fecha y hora de inicio del rango de creación.
     * @return Especificación de tipo {@link Specification} para el filtro de fecha inicial, o {@code null} si es nula.
     */
    public static Specification<Incidencia> hasCreatedAfter(OffsetDateTime start) {
        if (start == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start);
    }

    /**
     * Genera una especificación que filtra incidencias creadas hasta la fecha indicada (inclusive).
     *
     * @param end Fecha y hora de fin del rango de creación.
     * @return Especificación de tipo {@link Specification} para el filtro de fecha final, o {@code null} si es nula.
     */
    public static Specification<Incidencia> hasCreatedBefore(OffsetDateTime end) {
        if (end == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), end);
    }

    /**
     * Genera una especificación que filtra incidencias por estado exacto del ciclo de vida.
     *
     * @param status Estado de la incidencia a filtrar.
     * @return Especificación de tipo {@link Specification} para el filtro de estado, o {@code null} si es nulo.
     */
    public static Specification<Incidencia> hasStatus(EstadoIncidencia status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Genera una especificación que filtra incidencias por distrito municipal exacto,
     * ignorando mayúsculas/minúsculas y espacios exteriores.
     *
     * @param district Nombre del distrito a filtrar.
     * @return Especificación de tipo {@link Specification} para el filtro de distrito, o {@code null} si es vacío.
     */
    public static Specification<Incidencia> hasDistrict(String district) {
        if (district == null || district.trim().isEmpty()) {
            return null;
        }
        String normalized = district.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(cb.trim(root.get("district"))), normalized);
    }

    /**
     * Genera una especificación que filtra incidencias por prioridad técnica exacta.
     *
     * @param priority Prioridad de la incidencia a filtrar.
     * @return Especificación de tipo {@link Specification} para el filtro de prioridad, o {@code null} si es nula.
     */
    public static Specification<Incidencia> hasPriority(PrioridadIncidencia priority) {
        if (priority == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    /**
     * Genera una especificación que filtra incidencias con al menos una tarea activa
     * asignada al departamento indicado, mediante subconsulta correlacionada {@code EXISTS}.
     *
     * @param departmentId Identificador numérico del departamento municipal.
     * @return Especificación de tipo {@link Specification} para el filtro de departamento, o {@code null} si es nulo.
     */
    public static Specification<Incidencia> hasDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Tareas> tareas = subquery.from(Tareas.class);
            subquery.select(tareas.get("id")).where(
                    cb.equal(tareas.get("incident"), root),
                    cb.equal(tareas.get("department").get("id"), departmentId),
                    cb.isTrue(tareas.get("isActive"))
            );
            return cb.exists(subquery);
        };
    }

    /**
     * Genera una especificación que filtra incidencias sin ninguna tarea activa asignada
     * a un departamento, mediante subconsulta correlacionada {@code NOT EXISTS}.
     *
     * @return Especificación de tipo {@link Specification} para incidencias sin departamento.
     */
    public static Specification<Incidencia> hasNoDepartment() {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Tareas> tareas = subquery.from(Tareas.class);
            subquery.select(tareas.get("id")).where(
                    cb.equal(tareas.get("incident"), root),
                    cb.isTrue(tareas.get("isActive"))
            );
            return cb.not(cb.exists(subquery));
        };
    }

    /**
     * Construye una especificación combinada a partir del DTO de criterios de búsqueda,
     * aplicando todos los predicados activos mediante {@code AND}.
     * <p>
     * Los filtros de estado, prioridad y departamento solo se aplican si
     * {@code criteria.includeInternalFilters()} es {@code true}; en caso contrario se omiten
     * silenciosamente (control de acceso RBAC).
     * </p>
     *
     * @param criteria Criterios de búsqueda multicriterio (puede ser {@code null}).
     * @return Especificación combinada con todos los predicados activos; {@code Specification.where(null)} si ninguno aplica.
     */
    public static Specification<Incidencia> build(IncidentFilterCriteria criteria) {
        if (criteria == null) {
            return Specification.where(null);
        }
        Specification<Incidencia> spec = Specification.where(null);
        spec = andIfNotNull(spec, hasTextSearch(criteria.text()));
        spec = andIfNotNull(spec, hasCategory(criteria.category()));
        spec = andIfNotNull(spec, hasCreatedAfter(criteria.start()));
        spec = andIfNotNull(spec, hasCreatedBefore(criteria.end()));
        spec = andIfNotNull(spec, hasDistrict(criteria.district()));
        if (criteria.includeInternalFilters()) {
            spec = andIfNotNull(spec, hasStatus(criteria.status()));
            spec = andIfNotNull(spec, hasPriority(criteria.priority()));
            spec = andIfNotNull(spec, hasDepartment(criteria.departmentId()));
            if (criteria.unassignedDepartment()) {
                spec = andIfNotNull(spec, hasNoDepartment());
            }
        }
        return spec;
    }

    /**
     * Construye una especificación combinada con los criterios de la firma previa.
     * <p>
     * Sobrecarga retrocompatible que delega en {@link #build(IncidentFilterCriteria)} sin
     * distrito, prioridad ni departamento.
     * </p>
     *
     * @param text          Texto libre para buscar en título y descripción (opcional).
     * @param category      Categoría exacta de la incidencia (opcional).
     * @param start         Fecha de inicio del rango de creación (opcional).
     * @param end           Fecha de fin del rango de creación (opcional).
     * @param status        Estado del ciclo de vida de la incidencia (opcional).
     * @param includeStatus Si {@code true}, incluye el filtro de estado; si {@code false}, lo ignora.
     * @return Especificación combinada con todos los predicados activos; {@code Specification.where(null)} si ninguno aplica.
     */
    public static Specification<Incidencia> build(String text, String category,
                                                   OffsetDateTime start, OffsetDateTime end,
                                                   EstadoIncidencia status, boolean includeStatus) {
        return build(new IncidentFilterCriteria(text, category, start, end, status,
                null, null, null, false, includeStatus));
    }

    /**
     * Combina dos especificaciones con {@code AND} si la segunda no es nula.
     *
     * @param base      Especificación base.
     * @param addition  Especificación a añadir con {@code AND} si no es nula.
     * @return Especificación combinada.
     */
    private static Specification<Incidencia> andIfNotNull(Specification<Incidencia> base,
                                                            Specification<Incidencia> addition) {
        return addition != null ? base.and(addition) : base;
    }
}
