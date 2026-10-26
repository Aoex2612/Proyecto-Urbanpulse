package org.bulbaseros.service;

import org.bulbaseros.dto.CreateIncidentRequest;
import org.bulbaseros.dto.IncidentResponse;
import org.bulbaseros.dto.IncidentStatusResponse;
import org.bulbaseros.dto.StatusChangeResponse;
import org.bulbaseros.dto.UpdateIncidentStatusRequest;
import org.bulbaseros.entity.CambiosEstado;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.mappers.IncidentMapper;
import org.bulbaseros.mappers.StatusChangeMapper;
import org.bulbaseros.repository.CambiosEstadoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de logica de negocio transaccional para la gestion de incidencias y auditoria transversal en UrbanPulse.
 * <p>
 * Proporciona metodos para la consulta georreferenciada de avisos activos en la cartografia,
 * el alta de incidencias vinculadas al ciudadano emisor, la modificacion de estados de triaje
 * con registro inmutable de auditoria en una unica transaccion atómica, y la recuperacion
 * del historial cronologico de transiciones para inspeccion administrativa.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class IncidentService {

    /**
     * Repositorio de datos JPA para la gestion de persistencia y consultas de {@link Incidencia}.
     */
    private final IncidenciaRepository incidenciaRepository;

    /**
     * Repositorio de datos JPA para el registro inmutable de auditoria de transiciones {@link CambiosEstado}.
     */
    private final CambiosEstadoRepository cambiosEstadoRepository;

    /**
     * Repositorio de datos JPA para la busqueda y verificacion de usuarios {@link Usuario}.
     */
    private final UserRepository userRepository;

    /**
     * Mapeador para transformar entidades {@link Incidencia} en DTOs {@link IncidentResponse}.
     */
    private final IncidentMapper incidentMapper;

    /**
     * Mapeador para transformar entidades {@link CambiosEstado} en DTOs {@link StatusChangeResponse}.
     */
    private final StatusChangeMapper statusChangeMapper;

    /**
     * Mapa inmutable que define las transiciones permitidas según el ciclo de vida oficial
     * de incidencias en UrbanPulse:
     * <ul>
     *   <li>REPORTED -> VALIDATED, REJECTED</li>
     *   <li>VALIDATED -> ASSIGNED</li>
     *   <li>ASSIGNED -> IN_PROGRESS</li>
     *   <li>IN_PROGRESS -> RESOLVED</li>
     *   <li>RESOLVED -> CLOSED, REOPENED</li>
     *   <li>REOPENED -> ASSIGNED</li>
     *   <li>REJECTED, CLOSED -> terminales (sin transiciones posteriores)</li>
     * </ul>
     */
    public static final Map<EstadoIncidencia, Set<EstadoIncidencia>> ALLOWED_TRANSITIONS = Map.of(
            EstadoIncidencia.REPORTED, Set.of(EstadoIncidencia.VALIDATED, EstadoIncidencia.REJECTED),
            EstadoIncidencia.VALIDATED, Set.of(EstadoIncidencia.ASSIGNED),
            EstadoIncidencia.ASSIGNED, Set.of(EstadoIncidencia.IN_PROGRESS),
            EstadoIncidencia.IN_PROGRESS, Set.of(EstadoIncidencia.RESOLVED),
            EstadoIncidencia.RESOLVED, Set.of(EstadoIncidencia.CLOSED, EstadoIncidencia.REOPENED),
            EstadoIncidencia.REOPENED, Set.of(EstadoIncidencia.ASSIGNED),
            EstadoIncidencia.REJECTED, Set.of(),
            EstadoIncidencia.CLOSED, Set.of()
    );

    /**
     * Constructor con inyeccion de dependencias para el servicio de incidencias.
     *
     * @param incidenciaRepository    Repositorio JPA de incidencias urbanas.
     * @param cambiosEstadoRepository Repositorio JPA de transiciones de estado de auditoria.
     * @param userRepository          Repositorio JPA de usuarios del sistema.
     * @param incidentMapper          Mapeador de incidencias a DTOs.
     * @param statusChangeMapper      Mapeador de cambios de estado a DTOs.
     */
    public IncidentService(IncidenciaRepository incidenciaRepository,
                           CambiosEstadoRepository cambiosEstadoRepository,
                           UserRepository userRepository,
                           IncidentMapper incidentMapper,
                           StatusChangeMapper statusChangeMapper) {
        this.incidenciaRepository = incidenciaRepository;
        this.cambiosEstadoRepository = cambiosEstadoRepository;
        this.userRepository = userRepository;
        this.incidentMapper = incidentMapper;
        this.statusChangeMapper = statusChangeMapper;
    }

    /**
     * Recupera todas las incidencias urbanas activas en la plataforma para su representacion en el mapa cartografico.
     * <p>
     * Excluye deliberadamente los estados finales de ciclo de vida ({@link EstadoIncidencia#CLOSED}
     * y {@link EstadoIncidencia#REJECTED}) con el proposito de no saturar la vista interactiva.
     * </p>
     *
     * @return Lista de DTOs {@link IncidentResponse} representando las incidencias activas.
     */
    @Transactional(readOnly = true)
    public List<IncidentResponse> getActiveIncidents() {
        List<Incidencia> activeIncidents = incidenciaRepository.findByStatusNotIn(
                List.of(EstadoIncidencia.CLOSED, EstadoIncidencia.REJECTED)
        );
        return incidentMapper.toDtoList(activeIncidents);
    }

    /**
     * Obtiene los datos detallados de una incidencia urbana a partir de su identificador unico.
     *
     * @param id Identificador unico de la incidencia a recuperar.
     * @return DTO {@link IncidentResponse} con la informacion completa de la incidencia.
     * @throws IllegalArgumentException  si el identificador suministrado es nulo.
     * @throws IncidentNotFoundException si no existe una incidencia registrada con el identificador provisto.
     */
    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador de la incidencia no puede ser nulo");
        }
        return incidenciaRepository.findById(id)
                .map(incidentMapper::toDto)
                .orElseThrow(() -> new IncidentNotFoundException("Incidencia no encontrada con id: " + id));
    }

    /**
     * Registra una nueva incidencia urbana en el sistema asociandola al ciudadano emisor autenticado.
     * <p>
     * Busca al usuario por su correo electronico normalizado, inicializa los valores por defecto del ciclo
     * de vida (estado {@link EstadoIncidencia#REPORTED}, prioridad {@link PrioridadIncidencia#MEDIUM} y marcas
     * temporales actuales) y persiste la entidad en la base de datos relacional.
     * </p>
     *
     * @param request   DTO con los datos validados del reporte (titulo, descripcion, categoria y coordenadas).
     * @param userEmail Correo electronico del usuario autenticado extraido del token JWT.
     * @return DTO {@link IncidentResponse} con los datos persistidos de la incidencia creada.
     * @throws IllegalArgumentException      si la peticion recibida o el correo electronico son nulos o vacios.
     * @throws ResponseStatusException       con estado 404 si el usuario autenticado no existe en la base de datos.
     */
    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request, String userEmail) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de creacion de incidencia no puede ser nula");
        }
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo electronico del usuario no puede estar vacio");
        }

        String normalizedEmail = userEmail.trim().toLowerCase();
        Usuario citizen = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario emisor no encontrado con email: " + userEmail));

        Incidencia incidencia = new Incidencia();
        incidencia.setTitle(request.getTitle());
        incidencia.setDescription(request.getDescription());
        incidencia.setCategory(request.getCategory());
        incidencia.setLatitude(request.getLatitude());
        incidencia.setLongitude(request.getLongitude());
        incidencia.setAccuracyMeters(request.getAccuracyMeters());
        incidencia.setAddress(request.getAddress());
        incidencia.setDistrict(request.getDistrict());
        incidencia.setNeighborhood(request.getNeighborhood());
        incidencia.setCitizen(citizen);
        incidencia.setStatus(EstadoIncidencia.REPORTED);
        incidencia.setPriority(PrioridadIncidencia.MEDIUM);
        incidencia.setCreatedAt(OffsetDateTime.now());
        incidencia.setUpdatedAt(OffsetDateTime.now());

        Incidencia savedIncident = incidenciaRepository.save(incidencia);
        return incidentMapper.toDto(savedIncident);
    }

    /**
     * Actualiza el estado de una incidencia urbana en el flujo de triaje operativo y registra
     * simultaneamente una entrada inmutable de auditoria en la tabla de transiciones de estado.
     * <p>
     * Ambas operaciones (actualizacion de {@link Incidencia} y creacion de {@link CambiosEstado})
     * se ejecutan de manera indivisible bajo una unica transaccion gestionada por {@link Transactional}.
     * </p>
     *
     * @param id        Identificador numerico de la incidencia que experimenta el cambio de estado.
     * @param request   DTO con el nuevo estado a asignar y el motivo o comentario de justificacion.
     * @param userEmail Correo electronico del operador o administrador que autoriza la transicion.
     * @return DTO {@link IncidentStatusResponse} confirmando el cambio aplicado y la marca temporal de actualizacion.
     * @throws IllegalArgumentException  si el identificador, la solicitud, el estado o el correo son nulos o invalidos.
     * @throws IncidentNotFoundException si no existe ninguna incidencia registrada con el identificador provisto.
     * @throws ResponseStatusException   con estado 404 si el usuario actor no existe en la base de datos.
     */
    @Transactional
    public IncidentStatusResponse updateIncidentStatus(Long id, UpdateIncidentStatusRequest request, String userEmail) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador de la incidencia no puede ser nulo");
        }
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("La solicitud de actualizacion y el estado destino no pueden ser nulos");
        }
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo electronico del usuario operador no puede estar vacio");
        }

        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("Incidencia no encontrada con id: " + id));

        String normalizedEmail = userEmail.trim().toLowerCase();
        Usuario actor = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario actor no encontrado con email: " + userEmail));

        EstadoIncidencia currentStatus = (incidencia.getStatus() != null)
                ? incidencia.getStatus()
                : EstadoIncidencia.REPORTED;
        EstadoIncidencia toStatus = request.getStatus();

        Set<EstadoIncidencia> allowedNextStates = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedNextStates.contains(toStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Transición de estado no permitida: no se puede pasar de %s a %s en el ciclo de vida",
                            currentStatus, toStatus)
            );
        }

        String fromStatus = currentStatus.name();

        incidencia.setStatus(toStatus);
        incidencia.setUpdatedAt(OffsetDateTime.now());
        Incidencia updatedIncident = incidenciaRepository.save(incidencia);

        CambiosEstado cambio = new CambiosEstado(
                updatedIncident,
                fromStatus,
                toStatus.name(),
                actor,
                request.getComment()
        );
        cambio.setCreatedAt(OffsetDateTime.now());
        cambiosEstadoRepository.save(cambio);

        return new IncidentStatusResponse(
                id,
                toStatus,
                "Estado de la incidencia actualizado exitosamente",
                updatedIncident.getUpdatedAt()
        );
    }

    /**
     * Recupera la secuencia cronologica completa de transiciones de estado de una incidencia para fines de auditoria.
     * <p>
     * Comprueba en primera instancia la existencia de la incidencia en el sistema (arrojando un error 404 si no existe)
     * y posteriormente consulta el historial ordenado ascendentemente por marca temporal.
     * </p>
     *
     * @param id Identificador unico de la incidencia cuyo historial se desea auditar.
     * @return Lista cronologica de DTOs {@link StatusChangeResponse} con los hitos de cambio de estado.
     * @throws IllegalArgumentException  si el identificador suministrado es nulo.
     * @throws IncidentNotFoundException si la incidencia solicitada no existe en la base de datos.
     */
    @Transactional(readOnly = true)
    public List<StatusChangeResponse> getIncidentHistory(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador de la incidencia no puede ser nulo");
        }

        if (!incidenciaRepository.existsById(id)) {
            throw new IncidentNotFoundException("Incidencia no encontrada con id: " + id);
        }

        List<CambiosEstado> statusChanges = cambiosEstadoRepository.findByIncidentIdOrderByCreatedAtAsc(id);
        return statusChangeMapper.toDtoList(statusChanges);
    }
}
