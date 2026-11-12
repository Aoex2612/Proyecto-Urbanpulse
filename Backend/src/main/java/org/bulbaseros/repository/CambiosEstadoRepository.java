package org.bulbaseros.repository;

import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.EstadoIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    /**
     * Recupera todas las transiciones de estado asociadas a una coleccion de identificadores de incidencias,
     * ordenadas cronologicamente por su fecha de creacion ascendente.
     *
     * @param incidentIds Coleccion de identificadores unicos de incidencias.
     * @return Lista cronologica de cambios de estado para las incidencias indicadas.
     */
    @Query("SELECT c FROM CambiosEstado c WHERE c.incident.id IN :incidentIds ORDER BY c.createdAt ASC")
    List<CambiosEstado> findByIncidentIdInOrderByCreatedAtAsc(@Param("incidentIds") Collection<Long> incidentIds);

    /**
     * Recupera los cambios de estado de una incidencia especifica cuyo nuevo estado coincida con el estado indicado.
     *
     * @param incidentId Identificador unico de la incidencia.
     * @param toStatus   Nombre del estado destino tras la transicion.
     * @return Lista de transiciones hacia el estado especificado para la incidencia.
     */
    @Query("SELECT c FROM CambiosEstado c WHERE c.incident.id = :incidentId AND c.toStatus = :toStatus ORDER BY c.createdAt ASC")
    List<CambiosEstado> findByIncidentIdAndToStatus(@Param("incidentId") Long incidentId, @Param("toStatus") String toStatus);

    /**
     * Recupera los cambios de estado de una incidencia hacia un {@link EstadoIncidencia} especifico del ciclo de vida.
     *
     * @param incidentId Identificador unico de la incidencia.
     * @param toStatus   Enumerado del estado destino.
     * @return Lista de transiciones hacia el estado destino para la incidencia.
     */
    default List<CambiosEstado> findByIncidentIdAndToStatus(Long incidentId, EstadoIncidencia toStatus) {
        return findByIncidentIdAndToStatus(incidentId, toStatus != null ? toStatus.name() : null);
    }

    /**
     * Recupera un cambio de estado a partir de su identificador publico universal (UUID v4).
     *
     * @param publicId Identificador publico del registro de auditoria.
     * @return {@link Optional} con el cambio de estado o vacio.
     */
    Optional<CambiosEstado> findByPublicId(UUID publicId);

    /**
     * Recupera la secuencia cronologica completa de cambios de estado para una incidencia identificada por su UUID publico.
     *
     * @param incidentPublicId Identificador publico de la incidencia.
     * @return Lista cronologica ascendente de transiciones auditadas.
     */
    @Query("SELECT c FROM CambiosEstado c WHERE c.incident.publicId = :incidentPublicId ORDER BY c.createdAt ASC")
    List<CambiosEstado> findByIncidentPublicIdOrderByCreatedAtAsc(@Param("incidentPublicId") UUID incidentPublicId);
}
