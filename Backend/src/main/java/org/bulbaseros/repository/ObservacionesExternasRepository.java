package org.bulbaseros.repository;

import org.bulbaseros.entity.ObservacionesExternas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
