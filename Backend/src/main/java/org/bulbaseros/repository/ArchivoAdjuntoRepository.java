package org.bulbaseros.repository;

import org.bulbaseros.entity.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la gestión de persistencia de la entidad {@link ArchivoAdjunto}.
 * <p>
 * Proporciona métodos de acceso a datos para almacenar los metadatos de archivos multimedia adjuntos
 * y consultar los recursos asociados a cada incidencia urbana.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Long> {

    /**
     * Recupera todos los archivos adjuntos correspondientes a una incidencia específica,
     * ordenados cronológicamente por la fecha de carga.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista de archivos multimedia {@link ArchivoAdjunto} asociados a la incidencia.
     */
    @Query("SELECT a FROM ArchivoAdjunto a WHERE a.incident.id = :incidentId ORDER BY a.uploadedAt ASC")
    List<ArchivoAdjunto> findByIncidentIdOrderByUploadedAtAsc(@Param("incidentId") Long incidentId);

    /**
     * Recupera todos los archivos adjuntos asociados a una incidencia.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista de {@link ArchivoAdjunto} asociados.
     */
    List<ArchivoAdjunto> findByIncidentId(Long incidentId);
}
