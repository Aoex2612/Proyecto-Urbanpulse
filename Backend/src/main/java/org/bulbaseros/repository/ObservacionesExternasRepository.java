package org.bulbaseros.repository;

import org.bulbaseros.entity.ObservacionesExternas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la gestión de persistencia de observaciones contextuales externas.
 * <p>
 * Gestiona la entidad {@link ObservacionesExternas} correspondiente a métricas meteorológicas o de tráfico.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface ObservacionesExternasRepository extends JpaRepository<ObservacionesExternas, Long> {

    /**
     * Recupera una observacion externa por su identificador publico universal (UUID v4).
     *
     * @param publicId Identificador publico de la observacion.
     * @return {@link Optional} con la observacion o vacio.
     */
    Optional<ObservacionesExternas> findByPublicId(UUID publicId);
}
