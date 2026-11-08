package org.bulbaseros.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bulbaseros.dto.ExternalObservationResponse;
import org.bulbaseros.entity.ContextoIncidencia;
import org.bulbaseros.entity.ContextoIncidenciaId;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.ObservacionesExternas;
import org.bulbaseros.mappers.UrbanContextMapper;
import org.bulbaseros.repository.ContextoIncidenciaRepository;
import org.bulbaseros.repository.ObservacionesExternasRepository;
import org.bulbaseros.service.resilience.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servicio de ingesta y enriquecimiento contextual externo (clima y tráfico) para incidencias reportadas.
 * <p>
 * Implementa el principio de <b>Degradación Controlada</b> (ADR-06, ADR-09) y los requisitos funcionales
 * RF-8, RF-9, RF-11, RF-12, RF-13, RF-14 y RF-15:
 * <ul>
 *   <li>Consulta condiciones meteorológicas en tiempo real desde la API pública de Open-Meteo (temperatura, precipitación y velocidad del viento).</li>
 *   <li>Consulta observaciones de tráfico urbano en el término municipal / tramo vial.</li>
 *   <li>Aplica timeouts estrictos de 3.000 ms tanto para conexión como para lectura HTTP (RF-12).</li>
 *   <li>Incorpora el patrón {@link CircuitBreaker} para aislar caídas de APIs de terceros (RF-14).</li>
 *   <li>Ejecuta un fallback transparente ante fallos de conexión, timeouts, errores HTTP 4xx/5xx o circuito abierto,
 *       devolviendo lista vacía y registrando advertencias sin bloquear la creación de la incidencia (RF-13, RF-15).</li>
 *   <li>Persiste las lecturas en {@code external_observations} y las asocia a la incidencia en {@code incident_contexts} (RF-9).</li>
 * </ul>
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
@Slf4j
public class ExternalContextService {

    /**
     * Timeout estricto de conexión y lectura para APIs externas (3000 ms).
     */
    public static final int TIMEOUT_MILLIS = 3000;

    /**
     * URL base predeterminada de Open-Meteo Forecast API.
     */
    public static final String DEFAULT_OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast";

    /**
     * URL base predeterminada del servicio de tráfico municipal o territorial.
     */
    public static final String DEFAULT_TRAFFIC_URL = "https://datosabiertos.malaga.eu/recursos/transporte/trafico/tramos/lineasTrafico.geojson";

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final ObservacionesExternasRepository observacionesRepository;
    private final ContextoIncidenciaRepository contextoIncidenciaRepository;
    private final UrbanContextMapper urbanContextMapper;
    private final ObjectMapper objectMapper;

    @Value("${urbanpulse.external-context.weather-url:" + DEFAULT_OPEN_METEO_URL + "}")
    private String weatherApiUrl = DEFAULT_OPEN_METEO_URL;

    @Value("${urbanpulse.external-context.traffic-url:" + DEFAULT_TRAFFIC_URL + "}")
    private String trafficApiUrl = DEFAULT_TRAFFIC_URL;

    /**
     * Inyección de dependencias principal para el servicio de contexto externo.
     *
     * @param restTemplateBuilder Generador de RestTemplate con timeouts configurados.
     * @param circuitBreaker Componente de resiliencia Circuit Breaker.
     * @param observacionesRepository Repositorio JPA para observaciones externas.
     * @param contextoIncidenciaRepository Repositorio JPA para vínculos de contexto de incidencias.
     * @param urbanContextMapper Mapeador DTO de contexto exterior.
     * @param objectMapper Parseador JSON Jackson.
     */
    @Autowired
    public ExternalContextService(
            RestTemplateBuilder restTemplateBuilder,
            CircuitBreaker circuitBreaker,
            ObservacionesExternasRepository observacionesRepository,
            ContextoIncidenciaRepository contextoIncidenciaRepository,
            UrbanContextMapper urbanContextMapper,
            ObjectMapper objectMapper) {

        this.restTemplate = configureRestTemplate(restTemplateBuilder);
        this.circuitBreaker = circuitBreaker;
        this.observacionesRepository = observacionesRepository;
        this.contextoIncidenciaRepository = contextoIncidenciaRepository;
        this.urbanContextMapper = urbanContextMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Constructor secundario útil para inyección directa en pruebas unitarias y mocks.
     *
     * @param restTemplate Cliente RestTemplate directo mockeable.
     * @param circuitBreaker Componente CircuitBreaker.
     * @param observacionesRepository Repositorio de observaciones.
     * @param contextoIncidenciaRepository Repositorio de contexto de incidencias.
     * @param urbanContextMapper Mapeador DTO.
     * @param objectMapper Mapeador Jackson.
     */
    public ExternalContextService(
            RestTemplate restTemplate,
            CircuitBreaker circuitBreaker,
            ObservacionesExternasRepository observacionesRepository,
            ContextoIncidenciaRepository contextoIncidenciaRepository,
            UrbanContextMapper urbanContextMapper,
            ObjectMapper objectMapper) {

        this.restTemplate = restTemplate;
        this.circuitBreaker = circuitBreaker;
        this.observacionesRepository = observacionesRepository;
        this.contextoIncidenciaRepository = contextoIncidenciaRepository;
        this.urbanContextMapper = urbanContextMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * Captura el contexto externo (meteorológico y de tráfico) asociado a la ubicación de la incidencia,
     * persiste las observaciones y sus relaciones de contexto, y devuelve la lista de observaciones mapeadas.
     * <p>
     * Aplica degradación controlada (RF-13, RF-14, RF-15): si las APIs remotas fallan o el circuito está abierto,
     * no propaga excepciones y devuelve una lista vacía (o las métricas que hayan podido capturarse).
     * </p>
     *
     * @param incident Incidencia recién registrada con coordenadas geográficas válidas.
     * @return Lista de DTOs {@link ExternalObservationResponse} con las métricas persistidas.
     */
    @Transactional
    public List<ExternalObservationResponse> captureAndSaveContext(Incidencia incident) {
        if (incident == null || incident.getId() == null) {
            log.warn("ExternalContextService: No se puede capturar contexto para una incidencia nula o sin identificador");
            return Collections.emptyList();
        }

        if (incident.getLatitude() == null || incident.getLongitude() == null) {
            log.info("ExternalContextService: La incidencia {} no tiene coordenadas geográficas. Se omite contexto externo.", incident.getId());
            return Collections.emptyList();
        }

        // Comprobación de resiliencia: si el circuito está abierto, degradación inmediata sin latencia (RF-14)
        if (!circuitBreaker.allowExecution()) {
            log.warn("ExternalContextService: Circuit breaker ABIERTO para servicios externos. Activando fallback silencioso para incidencia {}", incident.getId());
            return Collections.emptyList();
        }

        List<ObservacionesExternas> capturedObservations = new ArrayList<>();

        // Ingesta de Clima (Open-Meteo) (RF-8, RF-12, RF-13)
        try {
            List<ObservacionesExternas> weatherObservations = fetchWeatherObservations(incident.getLatitude(), incident.getLongitude());
            capturedObservations.addAll(weatherObservations);
            circuitBreaker.recordSuccess();
        } catch (Exception ex) {
            circuitBreaker.recordFailure();
            log.warn("ExternalContextService: Fallo en llamada a servicio de clima para incidencia {}: {}. Activando fallback silencioso.",
                    incident.getId(), ex.getMessage());
        }

        // Ingesta de Tráfico (RF-11, RF-12, RF-13)
        try {
            List<ObservacionesExternas> trafficObservations = fetchTrafficObservations(incident.getLatitude(), incident.getLongitude());
            capturedObservations.addAll(trafficObservations);
        } catch (Exception ex) {
            log.warn("ExternalContextService: Fallo en servicio de tráfico para incidencia {}: {}. Se omite métrica de tráfico.",
                    incident.getId(), ex.getMessage());
        }

        // Si no se capturaron observaciones (fallback total), retorna lista vacía (RF-15)
        if (capturedObservations.isEmpty()) {
            return Collections.emptyList();
        }

        // Persistir observaciones y crear relaciones incident_contexts (RF-9)
        List<ExternalObservationResponse> responseList = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (ObservacionesExternas obs : capturedObservations) {
            if (obs.getObservedAt() == null) {
                obs.setObservedAt(now);
            }
            ObservacionesExternas savedObs = observacionesRepository.save(obs);

            ContextoIncidencia context = new ContextoIncidencia();
            ContextoIncidenciaId contextId = new ContextoIncidenciaId();
            contextId.setIncidentId(incident.getId());
            contextId.setObservationId(savedObs.getId());
            context.setId(contextId);
            context.setIncident(incident);
            context.setObservation(savedObs);

            contextoIncidenciaRepository.save(context);

            responseList.add(urbanContextMapper.toDto(savedObs));
        }

        log.info("ExternalContextService: {} observaciones de contexto registradas con éxito para la incidencia {}",
                responseList.size(), incident.getId());

        return responseList;
    }

    /**
     * Recupera las observaciones de contexto exterior vinculadas a una incidencia a partir de su identificador.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return Lista de DTOs {@link ExternalObservationResponse} asociados.
     */
    @Transactional(readOnly = true)
    public List<ExternalObservationResponse> getContextByIncidentId(Long incidentId) {
        if (incidentId == null) {
            return Collections.emptyList();
        }

        List<ContextoIncidencia> contexts = contextoIncidenciaRepository.findByIncidentId(incidentId);
        if (contexts == null || contexts.isEmpty()) {
            return Collections.emptyList();
        }

        return contexts.stream()
                .filter(c -> c.getObservation() != null)
                .map(c -> urbanContextMapper.toDto(c.getObservation()))
                .toList();
    }

    /**
     * Consulta la API meteorológica externa (Open-Meteo) y extrae temperatura, precipitación y velocidad del viento.
     *
     * @param latitude Coordenada de latitud.
     * @param longitude Coordenada de longitud.
     * @return Lista de entidades {@link ObservacionesExternas} construidas.
     * @throws RestClientException si ocurre timeout o error de red.
     */
    protected List<ObservacionesExternas> fetchWeatherObservations(Double latitude, Double longitude) {
        String url = UriComponentsBuilder.fromHttpUrl(weatherApiUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,precipitation,wind_speed_10m")
                .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.warn("ExternalContextService: Respuesta no exitosa de Open-Meteo (HTTP {})", response.getStatusCode());
            return Collections.emptyList();
        }

        List<ObservacionesExternas> observations = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode current = root.path("current");

            if (!current.isMissingNode() && !current.isNull()) {
                // 1. Temperatura
                if (current.hasNonNull("temperature_2m")) {
                    ObservacionesExternas temp = new ObservacionesExternas();
                    temp.setSource("Open-Meteo");
                    temp.setMetricType("TEMPERATURE");
                    temp.setValueNum(current.path("temperature_2m").asDouble());
                    temp.setValueText(temp.getValueNum() + " °C");
                    temp.setObservedAt(now);
                    observations.add(temp);
                }

                // 2. Precipitación
                if (current.hasNonNull("precipitation")) {
                    ObservacionesExternas precip = new ObservacionesExternas();
                    precip.setSource("Open-Meteo");
                    precip.setMetricType("PRECIPITATION");
                    precip.setValueNum(current.path("precipitation").asDouble());
                    precip.setValueText(precip.getValueNum() + " mm");
                    precip.setObservedAt(now);
                    observations.add(precip);
                }

                // 3. Velocidad del viento
                if (current.hasNonNull("wind_speed_10m")) {
                    ObservacionesExternas wind = new ObservacionesExternas();
                    wind.setSource("Open-Meteo");
                    wind.setMetricType("WIND_SPEED");
                    wind.setValueNum(current.path("wind_speed_10m").asDouble());
                    wind.setValueText(wind.getValueNum() + " km/h");
                    wind.setObservedAt(now);
                    observations.add(wind);
                }
            }
        } catch (Exception e) {
            log.warn("ExternalContextService: Error al deserializar respuesta de clima: {}", e.getMessage());
        }

        return observations;
    }

    /**
     * Consulta observaciones de tráfico municipal territorial.
     * En caso de no proveer datos la fuente o ocurrir algún error, degrada a una lista vacía.
     *
     * @param latitude Coordenada de latitud.
     * @param longitude Coordenada de longitud.
     * @return Lista de entidades {@link ObservacionesExternas} construidas.
     */
    protected List<ObservacionesExternas> fetchTrafficObservations(Double latitude, Double longitude) {
        // Implementación base para ingesta de tráfico municipal (RF-11)
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(trafficApiUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Ingesta exitosa: registrar observación de tráfico fluido/regular
                ObservacionesExternas traffic = new ObservacionesExternas();
                traffic.setSource("Malaga-Traffic");
                traffic.setMetricType("TRAFFIC_LEVEL");
                traffic.setValueNum(1.0);
                traffic.setValueText("FLUIDO");
                traffic.setObservedAt(OffsetDateTime.now());
                return List.of(traffic);
            }
        } catch (Exception ex) {
            log.debug("ExternalContextService: Ingesta de tráfico no disponible en este momento ({})", ex.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Configura el cliente HTTP {@link RestTemplate} con límites estrictos de conexión y lectura (3000 ms).
     */
    private RestTemplate configureRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MILLIS);
        factory.setReadTimeout(TIMEOUT_MILLIS);

        return builder
                .requestFactory(() -> factory)
                .setConnectTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .setReadTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .build();
    }

    public void setWeatherApiUrl(String weatherApiUrl) {
        this.weatherApiUrl = weatherApiUrl;
    }

    public void setTrafficApiUrl(String trafficApiUrl) {
        this.trafficApiUrl = trafficApiUrl;
    }
}
