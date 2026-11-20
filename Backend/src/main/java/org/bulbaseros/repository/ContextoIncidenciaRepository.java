package org.bulbaseros.repository;

import org.bulbaseros.entity.ContextoIncidencia;
import org.bulbaseros.entity.ContextoIncidenciaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la gestión de relaciones entre incidencias y observaciones externas.
 * <p>
 * Gestiona la entidad {@link ContextoIncidencia} identificada por la clave compuesta {@link ContextoIncidenciaId}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface ContextoIncidenciaRepository extends JpaRepository<ContextoIncidencia, ContextoIncidenciaId> {

    /**
     * Recupera todas las relaciones de contexto asociadas a una incidencia dada.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista de relaciones de contexto para la incidencia indicada.
     */
    List<ContextoIncidencia> findByIncidentId(Long incidentId);

    /**
     * Recupera todas las relaciones de contexto asociadas a una incidencia mediante su UUID publico.
     *
     * @param incidentPublicId Identificador publico de la incidencia.
     * @return Lista de relaciones de contexto.
     */
    List<ContextoIncidencia> findByIncidentPublicId(UUID incidentPublicId);
}
