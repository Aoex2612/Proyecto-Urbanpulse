package org.bulbaseros.dto;

import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.PrioridadIncidencia;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) que representa la informacion publica y detallada de una incidencia.
 * <p>
 * Empleado para la representacion de marcadores interactivos en el mapa cartografico y en el modal
 * de detalle, exponiendo datos de localizacion geografica, estado de triaje, prioridad y emisor ciudadano.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class IncidentResponse {

    /**
     * Identificador unico de la incidencia.
     */
    private Long id;

    /**
     * Titulo descriptivo de la incidencia.
     */
    private String title;

    /**
     * Descripcion detallada del problema o suceso reportado.
     */
    private String description;

    /**
     * Categoria tematica de clasificacion del incidente.
     */
    private String category;

    /**
     * Coordenada geografica de latitud para el posicionado en el mapa.
     */
    private Double latitude;

    /**
     * Coordenada geografica de longitud para el posicionado en el mapa.
     */
    private Double longitude;

    /**
     * Estado actual del incidente dentro del ciclo de vida y flujo de triaje.
     */
    private EstadoIncidencia status;

    /**
     * Nivel de prioridad asignado al incidente.
     */
    private PrioridadIncidencia priority;

    /**
     * Identificador unico del ciudadano emisor del reporte.
     */
    private Long citizenId;

    /**
     * Nombre completo del ciudadano emisor del reporte.
     */
    private String citizenName;

    /**
     * Marca temporal en que fue registrada la incidencia.
     */
    private OffsetDateTime createdAt;

    /**
     * Marca temporal en que se actualizo por ultima vez la incidencia.
     */
    private OffsetDateTime updatedAt;

    /**
     * Precision estimada de la captura geografica en metros (opcional).
     */
    private Double accuracyMeters;

    /**
     * Direccion postal normalizada del aviso.
     */
    private String address;

    /**
     * Distrito municipal administrativo en el que se ubica la incidencia.
     */
    private String district;

    /**
     * Barrio o vecindario en el que se ubica la incidencia.
     */
    private String neighborhood;

    /**
     * Constructor por defecto requerido para deserializacion JSON.
     */
    public IncidentResponse() {
    }

    /**
     * Constructor completo con todos los atributos de representacion de la incidencia.
     *
     * @param id          Identificador unico de la incidencia.
     * @param title       Titulo sintetico de la incidencia.
     * @param description Descripcion completa del suceso.
     * @param category    Categoria tematica de clasificacion.
     * @param latitude    Latitud geografica.
     * @param longitude   Longitud geografica.
     * @param status      Estado de ciclo de vida.
     * @param priority    Prioridad asignada.
     * @param citizenId   Identificador del ciudadano creador.
     * @param citizenName Nombre del ciudadano creador.
     * @param createdAt   Marca temporal de creacion.
     * @param updatedAt   Marca temporal de ultima actualizacion.
     */
    public IncidentResponse(Long id, String title, String description, String category, Double latitude, Double longitude,
                            EstadoIncidencia status, PrioridadIncidencia priority, Long citizenId, String citizenName,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, title, description, category, latitude, longitude, status, priority, citizenId, citizenName,
                createdAt, updatedAt, null, null, null, null);
    }

    /**
     * Constructor completo con todos los atributos de representacion de la incidencia incluyendo datos territoriales.
     *
     * @param id             Identificador unico de la incidencia.
     * @param title          Titulo sintetico de la incidencia.
     * @param description    Descripcion completa del suceso.
     * @param category       Categoria tematica de clasificacion.
     * @param latitude       Latitud geografica.
     * @param longitude      Longitud geografica.
     * @param status         Estado de ciclo de vida.
     * @param priority       Prioridad asignada.
     * @param citizenId      Identificador del ciudadano creador.
     * @param citizenName    Nombre del ciudadano creador.
     * @param createdAt      Marca temporal de creacion.
     * @param updatedAt      Marca temporal de ultima actualizacion.
     * @param accuracyMeters Precision estimada de captura en metros.
     * @param address        Direccion postal normalizada.
     * @param district       Distrito municipal.
     * @param neighborhood   Barrio o vecindario.
     */
    public IncidentResponse(Long id, String title, String description, String category, Double latitude, Double longitude,
                            EstadoIncidencia status, PrioridadIncidencia priority, Long citizenId, String citizenName,
                            OffsetDateTime createdAt, OffsetDateTime updatedAt, Double accuracyMeters, String address,
                            String district, String neighborhood) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.priority = priority;
        this.citizenId = citizenId;
        this.citizenName = citizenName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.accuracyMeters = accuracyMeters;
        this.address = address;
        this.district = district;
        this.neighborhood = neighborhood;
    }

    /**
     * Metodo de factoria estatica para transformar una entidad de dominio {@link Incidencia} en un DTO {@link IncidentResponse}.
     *
     * @param incidencia Entidad de dominio origen, o {@code null}.
     * @return DTO instanciado y poblado, o {@code null} si la entidad recibida es nula.
     */
    public static IncidentResponse fromEntity(Incidencia incidencia) {
        if (incidencia == null) {
            return null;
        }
        Long citizenId = (incidencia.getCitizen() != null) ? incidencia.getCitizen().getId() : null;
        String citizenName = (incidencia.getCitizen() != null) ? incidencia.getCitizen().getFullName() : null;

        return new IncidentResponse(
                incidencia.getId(),
                incidencia.getTitle(),
                incidencia.getDescription(),
                incidencia.getCategory(),
                incidencia.getLatitude(),
                incidencia.getLongitude(),
                incidencia.getStatus(),
                incidencia.getPriority(),
                citizenId,
                citizenName,
                incidencia.getCreatedAt(),
                incidencia.getUpdatedAt(),
                incidencia.getAccuracyMeters(),
                incidencia.getAddress(),
                incidencia.getDistrict(),
                incidencia.getNeighborhood()
        );
    }

    /**
     * Obtiene el identificador unico de la incidencia.
     *
     * @return Identificador como {@link Long}.
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador unico de la incidencia.
     *
     * @param id Identificador como {@link Long}.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el titulo descriptivo de la incidencia.
     *
     * @return Titulo como {@link String}.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Establece el titulo descriptivo de la incidencia.
     *
     * @param title Titulo como {@link String}.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Obtiene la descripcion detallada de la incidencia.
     *
     * @return Descripcion como {@link String}.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Establece la descripcion detallada de la incidencia.
     *
     * @param description Descripcion como {@link String}.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Obtiene la categoria de clasificacion de la incidencia.
     *
     * @return Categoria como {@link String}.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Establece la categoria de clasificacion de la incidencia.
     *
     * @param category Categoria como {@link String}.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Obtiene la latitud geografica.
     *
     * @return Latitud como {@link Double}.
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Establece la latitud geografica.
     *
     * @param latitude Latitud como {@link Double}.
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * Obtiene la longitud geografica.
     *
     * @return Longitud como {@link Double}.
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Establece la longitud geografica.
     *
     * @param longitude Longitud como {@link Double}.
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * Obtiene el estado actual de la incidencia.
     *
     * @return Estado como {@link EstadoIncidencia}.
     */
    public EstadoIncidencia getStatus() {
        return status;
    }

    /**
     * Establece el estado actual de la incidencia.
     *
     * @param status Estado como {@link EstadoIncidencia}.
     */
    public void setStatus(EstadoIncidencia status) {
        this.status = status;
    }

    /**
     * Obtiene el nivel de prioridad asignado a la incidencia.
     *
     * @return Prioridad como {@link PrioridadIncidencia}.
     */
    public PrioridadIncidencia getPriority() {
        return priority;
    }

    /**
     * Establece el nivel de prioridad asignado a la incidencia.
     *
     * @param priority Prioridad como {@link PrioridadIncidencia}.
     */
    public void setPriority(PrioridadIncidencia priority) {
        this.priority = priority;
    }

    /**
     * Obtiene el identificador del ciudadano emisor del reporte.
     *
     * @return Identificador del ciudadano como {@link Long}.
     */
    public Long getCitizenId() {
        return citizenId;
    }

    /**
     * Establece el identificador del ciudadano emisor del reporte.
     *
     * @param citizenId Identificador del ciudadano como {@link Long}.
     */
    public void setCitizenId(Long citizenId) {
        this.citizenId = citizenId;
    }

    /**
     * Obtiene el nombre completo del ciudadano emisor del reporte.
     *
     * @return Nombre del ciudadano como {@link String}.
     */
    public String getCitizenName() {
        return citizenName;
    }

    /**
     * Establece el nombre completo del ciudadano emisor del reporte.
     *
     * @param citizenName Nombre del ciudadano como {@link String}.
     */
    public void setCitizenName(String citizenName) {
        this.citizenName = citizenName;
    }

    /**
     * Obtiene la fecha y hora de creacion de la incidencia.
     *
     * @return Marca temporal como {@link OffsetDateTime}.
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la fecha y hora de creacion de la incidencia.
     *
     * @param createdAt Marca temporal como {@link OffsetDateTime}.
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtiene la fecha y hora de la ultima actualizacion de la incidencia.
     *
     * @return Marca temporal como {@link OffsetDateTime}.
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Establece la fecha y hora de la ultima actualizacion de la incidencia.
     *
     * @param updatedAt Marca temporal como {@link OffsetDateTime}.
     */
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Obtiene la precision estimada en metros de la captura.
     *
     * @return Precision como {@link Double}.
     */
    public Double getAccuracyMeters() {
        return accuracyMeters;
    }

    /**
     * Establece la precision estimada en metros de la captura.
     *
     * @param accuracyMeters Precision como {@link Double}.
     */
    public void setAccuracyMeters(Double accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    /**
     * Obtiene la direccion postal normalizada.
     *
     * @return Direccion postal como {@link String}.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Establece la direccion postal normalizada.
     *
     * @param address Direccion postal como {@link String}.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Obtiene el distrito municipal de la incidencia.
     *
     * @return Distrito municipal como {@link String}.
     */
    public String getDistrict() {
        return district;
    }

    /**
     * Establece el distrito municipal de la incidencia.
     *
     * @param district Distrito municipal como {@link String}.
     */
    public void setDistrict(String district) {
        this.district = district;
    }

    /**
     * Obtiene el barrio o vecindario de la incidencia.
     *
     * @return Barrio como {@link String}.
     */
    public String getNeighborhood() {
        return neighborhood;
    }

    /**
     * Establece el barrio o vecindario de la incidencia.
     *
     * @param neighborhood Barrio como {@link String}.
     */
    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }
}
