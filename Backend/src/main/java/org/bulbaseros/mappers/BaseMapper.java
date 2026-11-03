package org.bulbaseros.mappers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Clase base generica abstracta para los mapeadores de entidades a DTOs en UrbanPulse.
 * <p>
 * Proporciona el contrato base y la implementacion comun para la conversion unitaria
 * y en lote (listas) de entidades de persistencia a objetos de transferencia de datos (DTO).
 * </p>
 *
 * @param <E> Tipo de la entidad de persistencia JPA.
 * @param <D> Tipo del objeto de transferencia de datos (DTO).
 * @author UrbanPulse Team
 * @version 1.0
 */
public abstract class BaseMapper<E, D> {

    /**
     * Transforma una unica entidad de persistencia a su respectivo DTO.
     *
     * @param entity Entidad de dominio origen, o {@code null}.
     * @return DTO transformado con los datos de la entidad, o {@code null} si la entidad es nula.
     */
    public abstract D toDto(E entity);

    /**
     * Transforma una lista de entidades de persistencia a una lista de DTOs.
     * <p>
     * Si la lista de entidades suministrada es nula o vacia, retorna de forma segura
     * una lista vacia inmutable.
     * </p>
     *
     * @param entities Lista de entidades de origen, o {@code null}.
     * @return Lista de DTOs transformados, nunca {@code null}.
     */
    public List<D> toDtoList(List<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
