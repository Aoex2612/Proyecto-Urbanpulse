package org.bulbaseros.repository;

import org.bulbaseros.entity.CambiosEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la gestion de persistencia y auditoria de la entidad {@link CambiosEstado}.
 * <p>
 * Proporciona metodos de acceso a datos para registrar transiciones de estado de forma inmutable
 * y consultar el historial cronologico de auditoria para cada incidencia urbana.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface CambiosEstadoRepository extends JpaRepository<CambiosEstado, Long> {

    /**
     * Recupera la secuencia cronologica completa de cambios de estado correspondientes a una incidencia,
     * ordenados ascendentemente por la fecha y hora de creacion del registro.
     *
     * @param incidentId Identificador unico de la incidencia a auditar.
     * @return Lista cronologica de transiciones {@link CambiosEstado} registradas para la incidencia.
     */
    @Query("SELECT c FROM CambiosEstado c WHERE c.incident.id = :incidentId ORDER BY c.createdAt ASC")
    List<CambiosEstado> findByIncidentIdOrderByCreatedAtAsc(@Param("incidentId") Long incidentId);
}
