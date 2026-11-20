package org.bulbaseros.repository;

import org.bulbaseros.entity.ComentariosIncidencias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la persistencia y consulta de comentarios y notas de incidencias ({@link ComentariosIncidencias}).
 * <p>
 * Ofrece consultas derivadas para recuperar el muro cronológico de comentarios, soportando
 * la segregación de seguridad entre comentarios públicos y notas internas de gestión operativa.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface ComentariosIncidenciasRepository extends JpaRepository<ComentariosIncidencias, Long> {

    /**
     * Recupera la totalidad de comentarios y notas técnicas asociadas a una incidencia, ordenados cronológicamente por su fecha de publicación.
     * <p>
     * Este método está destinado al uso por parte del personal municipal (operadores, técnicos y administradores) que goza de visibilidad global.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista cronológica ascendente de comentarios asociados a la incidencia.
     */
    List<ComentariosIncidencias> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);

    /**
     * Recupera exclusivamente los comentarios públicos (con {@code is_internal = false}) de una incidencia, ordenados cronológicamente por fecha de publicación.
     * <p>
     * Este método está destinado al seguimiento ciudadano, impidiendo la divulgación de notas técnicas o comunicaciones internas restringidas.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista cronológica ascendente de comentarios públicos de la incidencia.
     */
    List<ComentariosIncidencias> findByIncidentIdAndIsInternalFalseOrderByCreatedAtAsc(Long incidentId);

    /**
     * Recupera un comentario a partir de su identificador publico universal (UUID v4).
     *
     * @param publicId Identificador publico del comentario.
     * @return {@link Optional} con el comentario o vacio.
     */
    Optional<ComentariosIncidencias> findByPublicId(UUID publicId);

    /**
     * Recupera la totalidad de comentarios asociados a una incidencia identificada por su UUID publico.
     *
     * @param incidentPublicId Identificador publico de la incidencia.
     * @return Lista cronologica ascendente de comentarios.
     */
    List<ComentariosIncidencias> findByIncidentPublicIdOrderByCreatedAtAsc(UUID incidentPublicId);

    /**
     * Recupera exclusivamente los comentarios publicos de una incidencia identificada por su UUID publico.
     *
     * @param incidentPublicId Identificador publico de la incidencia.
     * @return Lista cronologica ascendente de comentarios publicos.
     */
    List<ComentariosIncidencias> findByIncidentPublicIdAndIsInternalFalseOrderByCreatedAtAsc(UUID incidentPublicId);
}
