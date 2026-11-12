package org.bulbaseros.repository;

import org.bulbaseros.entity.AsociacionIncidenciaActivo;
import org.bulbaseros.entity.AsociacionIncidenciaActivoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la gestión de persistencia de las asociaciones entre incidencias y activos urbanos.
 * <p>
 * Gestiona la entidad {@link AsociacionIncidenciaActivo} identificada por la clave compuesta {@link AsociacionIncidenciaActivoId}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface AsociacionIncidenciaActivoRepository extends JpaRepository<AsociacionIncidenciaActivo, AsociacionIncidenciaActivoId> {

    /**
     * Recupera todas las asociaciones de activos vinculadas a una incidencia específica.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista de asociaciones de activos para la incidencia indicada.
     */
    List<AsociacionIncidenciaActivo> findByIncidentId(Long incidentId);

    /**
     * Busca una asociación concreta entre una incidencia y un activo urbano utilizando sus identificadores.
     *
     * @param incidentId Identificador único de la incidencia.
     * @param assetId Identificador único del activo urbano.
     * @return {@link Optional} que contiene la asociación si existe, o vacío en caso contrario.
     */
    Optional<AsociacionIncidenciaActivo> findByIncidentIdAndAssetId(Long incidentId, Long assetId);

    /**
     * Elimina la asociación entre una incidencia y un activo urbano específico.
     *
     * @param incidentId Identificador único de la incidencia.
     * @param assetId Identificador único del activo urbano.
     */
    void deleteByIncidentIdAndAssetId(Long incidentId, Long assetId);

    /**
     * Recupera todas las asociaciones de activos vinculadas a una incidencia mediante su UUID publico.
     *
     * @param incidentPublicId Identificador publico de la incidencia.
     * @return Lista de asociaciones de activos.
     */
    List<AsociacionIncidenciaActivo> findByIncidentPublicId(UUID incidentPublicId);

    /**
     * Busca una asociacion concreta utilizando los identificadores publicos (UUID v4) de incidencia y activo.
     *
     * @param incidentPublicId Identificador publico de la incidencia.
     * @param assetPublicId Identificador publico del activo.
     * @return {@link Optional} con la asociacion o vacio.
     */
    Optional<AsociacionIncidenciaActivo> findByIncidentPublicIdAndAssetPublicId(UUID incidentPublicId, UUID assetPublicId);

    /**
     * Elimina la asociacion entre una incidencia y un activo urbano mediante sus identificadores publicos.
     *
     * @param incidentPublicId Identificador publico de la incidencia.
     * @param assetPublicId Identificador publico del activo.
     */
    void deleteByIncidentPublicIdAndAssetPublicId(UUID incidentPublicId, UUID assetPublicId);
}
