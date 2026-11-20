package org.bulbaseros.dto;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.PrioridadIncidencia;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) inmutable que encapsula los criterios de búsqueda
 * multicriterio de incidencias urbanas.
 * <p>
 * Agrupa los filtros públicos (texto, categoría, rango de fechas y distrito) junto con los
 * filtros internos sujetos a control de acceso RBAC (estado, prioridad y departamento),
 * evitando firmas posicionales frágiles en la capa de especificaciones JPA.
 * </p>
 *
 * @param text                    Texto libre a buscar en título y descripción (opcional).
 * @param category                Categoría exacta de la incidencia (opcional).
 * @param start                   Fecha y hora inicial del rango de creación (opcional).
 * @param end                     Fecha y hora final del rango de creación (opcional).
 * @param status                  Estado del ciclo de vida de la incidencia (opcional, filtro interno).
 * @param district                Distrito municipal de la incidencia (opcional, filtro público).
 * @param priority                Prioridad técnica de la incidencia (opcional, filtro interno).
 * @param departmentId            Identificador numérico del departamento asignado (opcional, filtro interno).
 * @param unassignedDepartment    {@code true} para filtrar incidencias sin departamento activo (filtro interno).
 * @param includeInternalFilters  {@code true} si el usuario puede aplicar filtros internos (RBAC).
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public record IncidentFilterCriteria(
        String text,
        String category,
        OffsetDateTime start,
        OffsetDateTime end,
        EstadoIncidencia status,
        String district,
        PrioridadIncidencia priority,
        Long departmentId,
        boolean unassignedDepartment,
        boolean includeInternalFilters
) {
}
