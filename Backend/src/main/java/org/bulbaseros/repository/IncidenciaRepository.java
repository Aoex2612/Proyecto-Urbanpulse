package org.bulbaseros.repository;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la gestion de persistencia y consultas de la entidad {@link Incidencia}.
 * <p>
 * Proporciona operaciones CRUD estandar heredadas de {@link JpaRepository}, consultas especializadas
 * para la recuperacion de incidencias activas en el mapa mediante la exclusion de estados terminados,
 * y metodos paginados para el seguimiento de reportes propios del ciudadano con o sin filtrado por estado.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long>, JpaSpecificationExecutor<Incidencia> {

    /**
     * Recupera todas las incidencias cuyo estado no se encuentre dentro de la coleccion de estados especificada.
     * <p>
     * Utilizado para obtener incidencias activas en el mapa cartografico (por ejemplo, excluyendo
     * los estados {@link EstadoIncidencia#CLOSED} y {@link EstadoIncidencia#REJECTED}).
     * </p>
     *
     * @param statuses Coleccion de estados {@link EstadoIncidencia} que deben ser excluidos del resultado.
     * @return Lista de entidades {@link Incidencia} que no poseen ninguno de los estados excluidos.
     */
    List<Incidencia> findByStatusNotIn(Collection<EstadoIncidencia> statuses);

    /**
     * Recupera todas las incidencias cuyo estado expresado como cadena de texto no se encuentre
     * en la lista de estados proporcionada.
     * <p>
     * Consulta JPQL alternativa que permite evaluar estados como literales {@link String}.
     * </p>
     *
     * @param statuses Coleccion de nombres textuales de estados a excluir.
     * @return Lista de entidades {@link Incidencia} activas.
     */
    @Query("SELECT i FROM Incidencia i WHERE CAST(i.status AS string) NOT IN :statuses")
    List<Incidencia> findByStatusNotInStrings(@Param("statuses") Collection<String> statuses);

    /**
     * Recupera de forma paginada todas las incidencias registradas por un ciudadano especifico.
     *
     * @param citizenId Identificador numerico del ciudadano emisor.
     * @param pageable  Objeto {@link Pageable} con informacion de paginacion y ordenacion.
     * @return Pagina de entidades {@link Incidencia} asociadas al ciudadano.
     */
    Page<Incidencia> findByCitizenId(Long citizenId, Pageable pageable);

    /**
     * Recupera de forma paginada las incidencias de un ciudadano filtradas por un estado especifico del ciclo de vida.
     *
     * @param citizenId Identificador numerico del ciudadano emisor.
     * @param status    Estado de la incidencia {@link EstadoIncidencia} por el que filtrar.
     * @param pageable  Objeto {@link Pageable} con informacion de paginacion y ordenacion.
     * @return Pagina de entidades {@link Incidencia} asociadas al ciudadano y estado indicados.
     */
    Page<Incidencia> findByCitizenIdAndStatus(Long citizenId, EstadoIncidencia status, Pageable pageable);

    /**
     * Recupera todas las incidencias dentro de un rango temporal con filtrado opcional por distrito y barrio.
     * <p>
     * Si los parametros {@code district} o {@code neighborhood} son nulos o cadenas vacias, la condicion respectiva
     * es ignorada, recuperando todas las incidencias creadas en el intervalo temporal indicado ordenadas
     * de forma cronologica descendente.
     * </p>
     *
     * @param startDate    Fecha y hora de inicio del intervalo temporal (inclusive).
     * @param endDate      Fecha y hora de finalizacion del intervalo temporal (inclusive).
     * @param district     Nombre del distrito municipal por el que filtrar, o {@code null} para ignorar.
     * @param neighborhood Nombre del barrio por el que filtrar, o {@code null} para ignorar.
     * @return Lista de incidencias que satisfacen los filtros temporales y territoriales.
     */
    @Query("SELECT i FROM Incidencia i WHERE i.createdAt >= :startDate AND i.createdAt <= :endDate " +
           "AND (:district IS NULL OR :district = '' OR LOWER(i.district) = LOWER(:district)) " +
           "AND (:neighborhood IS NULL OR :neighborhood = '' OR LOWER(i.neighborhood) = LOWER(:neighborhood)) " +
           "ORDER BY i.createdAt DESC")
    List<Incidencia> findByDateRangeAndTerritory(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("district") String district,
            @Param("neighborhood") String neighborhood);

    /**
     * Recupera todas las incidencias registradas en un intervalo temporal ordenadas descendentemente por fecha.
     *
     * @param startDate Fecha inicial del rango.
     * @param endDate   Fecha final del rango.
     * @return Lista de incidencias creadas entre ambas fechas.
     */
    List<Incidencia> findByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Recupera una incidencia urbana a partir de su identificador publico universal (UUID v4).
     *
     * @param publicId Identificador publico de la incidencia.
     * @return Contenedor {@link Optional} con la incidencia hallada o vacio si no existe.
     */
    Optional<Incidencia> findByPublicId(UUID publicId);

    /**
     * Comprueba la existencia de una incidencia urbana a partir de su identificador publico universal (UUID v4).
     *
     * @param publicId Identificador publico de la incidencia.
     * @return {@code true} si existe una incidencia con dicho UUID publico, {@code false} en caso contrario.
     */
    boolean existsByPublicId(UUID publicId);
}
