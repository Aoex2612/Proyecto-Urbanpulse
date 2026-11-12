package org.bulbaseros.repository;

import org.bulbaseros.entity.ActivoUrbano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la gestión de persistencia y consultas de la entidad {@link ActivoUrbano}.
 * <p>
 * Proporciona métodos para la localización espacial de activos dentro de una caja delimitadora (bounding box)
 * y para la búsqueda unívoca por código de activo.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface ActivoUrbanoRepository extends JpaRepository<ActivoUrbano, Long> {

    /**
     * Recupera todos los activos urbanos cuyas coordenadas geográficas se encuentren dentro del rango delimitado
     * por las latitudes y longitudes especificadas (bounding box).
     *
     * @param minLat Latitud mínima del área de búsqueda.
     * @param maxLat Latitud máxima del área de búsqueda.
     * @param minLon Longitud mínima del área de búsqueda.
     * @param maxLon Longitud máxima del área de búsqueda.
     * @return Lista de activos urbanos situados dentro del bounding box.
     */
    List<ActivoUrbano> findByLatitudeBetweenAndLongitudeBetween(Double minLat, Double maxLat, Double minLon, Double maxLon);

    /**
     * Busca un activo urbano mediante su código identificador único de inventario.
     *
     * @param assetCode Código único del activo urbano.
     * @return {@link Optional} que contiene el activo urbano si existe, o vacío en caso contrario.
     */
    Optional<ActivoUrbano> findByAssetCode(String assetCode);

    /**
     * Busca un activo urbano a partir de su identificador publico universal (UUID v4).
     *
     * @param publicId Identificador publico del activo.
     * @return {@link Optional} con el activo urbano o vacio.
     */
    Optional<ActivoUrbano> findByPublicId(UUID publicId);
}
