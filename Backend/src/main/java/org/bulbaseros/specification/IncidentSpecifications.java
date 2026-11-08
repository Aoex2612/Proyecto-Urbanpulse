package org.bulbaseros.specification;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Clase de especificaciones JPA Criteria API para la construcción dinámica de predicados
 * de búsqueda y filtrado multicriterio de incidencias urbanas.
 * <p>
 * Proporciona métodos estáticos que generan predicados combinables mediante operador {@code and}
 * para los criterios de texto libre, categoría, rango de fechas y estado del ciclo de vida.
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
     * Construye una especificación combinada con todos los criterios de búsqueda aplicados mediante {@code AND}.
     * <p>
     * El filtro de estado ({@code status}) solo se aplica si {@code includeStatus} es {@code true}.
     * El filtro de estado está deshabilitado para usuarios con rol {@code CITIZEN}.
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
        Specification<Incidencia> spec = Specification.where(null);
        spec = andIfNotNull(spec, hasTextSearch(text));
        spec = andIfNotNull(spec, hasCategory(category));
        spec = andIfNotNull(spec, hasCreatedAfter(start));
        spec = andIfNotNull(spec, hasCreatedBefore(end));
        if (includeStatus) {
            spec = andIfNotNull(spec, hasStatus(status));
        }
        return spec;
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
