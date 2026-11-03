package org.bulbaseros.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Objeto de transferencia de datos (DTO) para la solicitud de creacion de una nueva incidencia urbana.
 * <p>
 * Contiene los datos requeridos aportados por el ciudadano desde la interfaz cartografica,
 * incluyendo titulo, descripcion, categoria tematica normalizada y coordenadas geograficas de captura.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class CreateIncidentRequest {

    /**
     * Titulo sintetico y descriptivo de la incidencia reportada.
     */
    @NotBlank(message = "El titulo de la incidencia es obligatorio")
    @Size(max = 200, message = "El titulo no puede exceder los 200 caracteres")
    private String title;

    /**
     * Descripcion detallada del problema o suceso observado.
     */
    @NotBlank(message = "La descripcion de la incidencia es obligatoria")
    private String description;

    /**
     * Categoria tematica de clasificacion del incidente (ej. Alumbrado, Vias Publicas, Limpieza, etc.).
     */
    @NotBlank(message = "La categoria de la incidencia es obligatoria")
    @Size(max = 50, message = "La categoria no puede exceder los 50 caracteres")
    private String category;

    /**
     * Coordenada de latitud geografica capturada en el mapa.
     */
    @NotNull(message = "La latitud de la ubicacion es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90.0")
    @DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90.0")
    private Double latitude;

    /**
     * Coordenada de longitud geografica capturada en el mapa.
     */
    @NotNull(message = "La longitud de la ubicacion es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud debe ser mayor o igual a -180.0")
    @DecimalMax(value = "180.0", message = "La longitud debe ser menor o igual a 180.0")
    private Double longitude;

    /**
     * Precision estimada del punto de captura en metros (opcional).
     */
    private Double accuracyMeters;

    /**
     * Direccion postal aproximada calculada o indicada (opcional).
     */
    @Size(max = 255, message = "La direccion no puede exceder los 255 caracteres")
    private String address;

    /**
     * Distrito municipal correspondiente al punto del reporte (opcional).
     */
    @Size(max = 100, message = "El distrito no puede exceder los 100 caracteres")
    private String district;

    /**
     * Barrio o vecindario donde se ubica el problema (opcional).
     */
    @Size(max = 100, message = "El barrio no puede exceder los 100 caracteres")
    private String neighborhood;

    /**
     * Constructor por defecto requerido para deserializacion JSON.
     */
    public CreateIncidentRequest() {
    }

    /**
     * Constructor con los campos obligatorios para el registro de una incidencia.
     *
     * @param title       Titulo descriptivo de la incidencia.
     * @param description Descripcion del suceso reportado.
     * @param category    Categoria tematica normalizada.
     * @param latitude    Coordenada de latitud.
     * @param longitude   Coordenada de longitud.
     */
    public CreateIncidentRequest(String title, String description, String category, Double latitude, Double longitude) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Constructor completo con todos los atributos opcionales de localizacion.
     *
     * @param title          Titulo descriptivo de la incidencia.
     * @param description    Descripcion del suceso reportado.
     * @param category       Categoria tematica normalizada.
     * @param latitude       Coordenada de latitud.
     * @param longitude      Coordenada de longitud.
     * @param accuracyMeters Precision estimada en metros.
     * @param address        Direccion postal aproximada.
     * @param district       Distrito municipal.
     * @param neighborhood   Barrio o vecindario.
     */
    public CreateIncidentRequest(String title, String description, String category, Double latitude, Double longitude,
                                 Double accuracyMeters, String address, String district, String neighborhood) {
        this(title, description, category, latitude, longitude);
        this.accuracyMeters = accuracyMeters;
        this.address = address;
        this.district = district;
        this.neighborhood = neighborhood;
    }

    /**
     * Obtiene el titulo de la incidencia.
     *
     * @return Titulo descriptivo como {@link String}.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Establece el titulo de la incidencia.
     *
     * @param title Titulo descriptivo como {@link String}.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Obtiene la descripcion del suceso reportado.
     *
     * @return Descripcion como {@link String}.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Establece la descripcion del suceso reportado.
     *
     * @param description Descripcion como {@link String}.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Obtiene la categoria de la incidencia.
     *
     * @return Categoria como {@link String}.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Establece la categoria de la incidencia.
     *
     * @param category Categoria como {@link String}.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Obtiene la latitud de la ubicacion geográfica.
     *
     * @return Latitud como {@link Double}.
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Establece la latitud de la ubicacion geográfica.
     *
     * @param latitude Latitud como {@link Double}.
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * Obtiene la longitud de la ubicacion geográfica.
     *
     * @return Longitud como {@link Double}.
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Establece la longitud de la ubicacion geográfica.
     *
     * @param longitude Longitud como {@link Double}.
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * Obtiene la precision en metros de la captura geografica.
     *
     * @return Precision como {@link Double}, o {@code null} si no fue registrada.
     */
    public Double getAccuracyMeters() {
        return accuracyMeters;
    }

    /**
     * Establece la precision en metros de la captura geografica.
     *
     * @param accuracyMeters Precision como {@link Double}.
     */
    public void setAccuracyMeters(Double accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    /**
     * Obtiene la direccion postal aproximada.
     *
     * @return Direccion postal como {@link String}.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Establece la direccion postal aproximada.
     *
     * @param address Direccion postal como {@link String}.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Obtiene el distrito municipal de la incidencia.
     *
     * @return Distrito como {@link String}.
     */
    public String getDistrict() {
        return district;
    }

    /**
     * Establece el distrito municipal de la incidencia.
     *
     * @param district Distrito como {@link String}.
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
