package org.bulbaseros.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bulbaseros.dto.ReverseGeocodeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * Servicio de integracion con el portal de Datos Abiertos del Ayuntamiento de Malaga
 * ({@code datosabiertos.malaga.eu}) para la resolucion territorial asistida de incidencias.
 * <p>
 * Implementa el principio arquitectonico de <b>Degradacion Controlada</b> definido en el ADR-06:
 * ante latencias excesivas (timeout estricto de 3000 ms), indisponibilidad del servicio externo,
 * errores HTTP o coordenadas sin correspondencia territorial, el servicio captura la excepcion
 * de forma limpia y transparente, emite un log informativo/advertencia y devuelve un
 * {@link ReverseGeocodeResponse} con campos nulos para no bloquear el flujo de creacion o consulta.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
@Slf4j
public class MalagaOpenDataService {

    /**
     * Limite inferior de latitud para el termino municipal de Malaga.
     */
    public static final double MIN_LATITUDE = 36.5000;

    /**
     * Limite superior de latitud para el termino municipal de Malaga.
     */
    public static final double MAX_LATITUDE = 36.9000;

    /**
     * Limite inferior de longitud para el termino municipal de Malaga.
     */
    public static final double MIN_LONGITUDE = -4.7000;

    /**
     * Limite superior de longitud para el termino municipal de Malaga.
     */
    public static final double MAX_LONGITUDE = -4.2000;

    /**
     * Tiempo maximo de espera en milisegundos para conexion y lectura HTTP (3000 ms / 3 segundos).
     */
    public static final int TIMEOUT_MILLIS = 3000;

    /**
     * URL base predeterminada de la plataforma de Datos Abiertos del Ayuntamiento de Malaga.
     */
    public static final String DEFAULT_BASE_URL = "https://datosabiertos.malaga.eu";

    /**
     * Ruta por defecto del recurso de geocodificacion inversa / busqueda territorial en CKAN.
     */
    public static final String DEFAULT_ENDPOINT_PATH = "/api/action/datastore_search";

    /**
     * Cliente HTTP configurado para la comunicacion con la API externa.
     */
    private final RestTemplate restTemplate;

    /**
     * URL base del portal de datos abiertos.
     */
    private final String baseUrl;

    /**
     * Ruta del endpoint para la resolucion geografica.
     */
    private final String endpointPath;

    /**
     * Deserializador JSON para procesar respuestas complejas o estructuradas de CKAN / GeoJSON.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructor principal para la inyeccion de dependencias en Spring Boot.
     * <p>
     * Aplica la configuracion de timeout estricto de 3 segundos (3000 ms) tanto para la conexion
     * como para la lectura de la respuesta.
     * </p>
     *
     * @param restTemplateBuilder Creador de clientes RestTemplate proporcionado por Spring Boot.
     * @param baseUrl             URL base inyectada desde propiedades de configuracion.
     * @param endpointPath        Ruta del endpoint territorial inyectada desde configuracion.
     */
    @Autowired
    public MalagaOpenDataService(RestTemplateBuilder restTemplateBuilder,
                                 @Value("${malaga.opendata.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
                                 @Value("${malaga.opendata.endpoint-path:" + DEFAULT_ENDPOINT_PATH + "}") String endpointPath) {
        this(restTemplateBuilder
                .connectTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .readTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .build(),
             baseUrl,
             endpointPath);
    }

    /**
     * Constructor sobrecargado para pruebas unitarias con {@link RestTemplate} parametrizado.
     *
     * @param restTemplate Instancia de cliente HTTP a utilizar.
     * @param baseUrl      URL base del servicio remoto.
     * @param endpointPath Ruta del endpoint de consulta territorial.
     */
    public MalagaOpenDataService(RestTemplate restTemplate, String baseUrl, String endpointPath) {
        this.restTemplate = restTemplate;
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : DEFAULT_BASE_URL;
        this.endpointPath = (endpointPath != null && !endpointPath.isBlank()) ? endpointPath : DEFAULT_ENDPOINT_PATH;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor sobrecargado de conveniencia para pruebas con un {@link RestTemplate} mockeado
     * empleando las URLs predeterminadas del servicio.
     *
     * @param restTemplate Instancia de cliente HTTP configurada.
     */
    public MalagaOpenDataService(RestTemplate restTemplate) {
        this(restTemplate, DEFAULT_BASE_URL, DEFAULT_ENDPOINT_PATH);
    }

    /**
     * Constructor sin argumentos que crea un cliente {@link RestTemplate} autonomo con timeouts de 3000 ms.
     */
    public MalagaOpenDataService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MILLIS);
        factory.setReadTimeout(TIMEOUT_MILLIS);
        this.restTemplate = new RestTemplate(factory);
        this.baseUrl = DEFAULT_BASE_URL;
        this.endpointPath = DEFAULT_ENDPOINT_PATH;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Obtiene el cliente {@link RestTemplate} subyacente.
     *
     * @return El cliente RestTemplate configurado.
     */
    public RestTemplate getRestTemplate() {
        return this.restTemplate;
    }

    /**
     * Comprueba si unas coordenadas geograficas se encuentran dentro del termino municipal de Malaga.
     *
     * @param latitude  Latitud geografica en grados decimales.
     * @param longitude Longitud geografica en grados decimales.
     * @return {@code true} si las coordenadas no son nulas y se hallan dentro del rectangulo delimitador
     *         oficial de Malaga (latitud [36.5000, 36.9000], longitud [-4.7000, -4.2000]); {@code false} en otro caso.
     */
    public boolean isWithinMalagaBounds(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE
                && longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
    }

    /**
     * Valida de forma estricta que las coordenadas no sean nulas y pertenezcan a los limites
     * del municipio de Malaga, lanzando {@link IllegalArgumentException} si se vulnera la condicion.
     *
     * @param latitude  Latitud a validar.
     * @param longitude Longitud a validar.
     * @throws IllegalArgumentException si las coordenadas son nulas o estan fuera del termino municipal.
     */
    public void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Las coordenadas geograficas no pueden ser nulas");
        }
        if (!isWithinMalagaBounds(latitude, longitude)) {
            throw new IllegalArgumentException(String.format(
                    "Las coordenadas (%.4f, %.4f) exceden el termino municipal de Malaga [lat: %.4f - %.4f, lon: %.4f - %.4f]",
                    latitude, longitude, MIN_LATITUDE, MAX_LATITUDE, MIN_LONGITUDE, MAX_LONGITUDE));
        }
    }

    /**
     * Resuelve territorialmente una ubicacion a partir de sus coordenadas geograficas consultando
     * los datos abiertos oficiales del Ayuntamiento de Malaga.
     * <p>
     * Aplica la politica de degradacion controlada: si las coordenadas estan fuera de los limites de Malaga,
     * si se supera el timeout de 3 segundos, si el portal remoto responde con un error HTTP (ej. 500)
     * o si no se halla ninguna coincidencia, se captura la condicion limpiamente, se registra una advertencia
     * y se devuelve una instancia de {@link ReverseGeocodeResponse} con todos sus atributos en {@code null}.
     * </p>
     *
     * @param latitude  Latitud del punto en grados decimales.
     * @param longitude Longitud del punto en grados decimales.
     * @return DTO {@link ReverseGeocodeResponse} con la direccion, distrito y barrio obtenidos,
     *         o con campos nulos en caso de degradacion controlada o falta de coincidencia.
     */
    public ReverseGeocodeResponse resolveLocation(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            log.warn("Coordenadas geograficas nulas recibidas para geocodificacion inversa. Aplicando degradacion controlada.");
            return new ReverseGeocodeResponse(null, null, null);
        }

        if (!isWithinMalagaBounds(latitude, longitude)) {
            log.warn("Coordenadas fuera del termino municipal de Malaga (lat: {}, lon: {}). Aplicando degradacion controlada.",
                    latitude, longitude);
            return new ReverseGeocodeResponse(null, null, null);
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path(endpointPath)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .build()
                    .toUri();

            log.debug("Consultando servicio de Datos Abiertos de Malaga en URI: {}", uri);
            String responseBody = restTemplate.getForObject(uri, String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Respuesta vacia al consultar datos abiertos de Malaga para ({}, {})", latitude, longitude);
                return new ReverseGeocodeResponse(null, null, null);
            }

            return parseTerritorialResponse(responseBody);

        } catch (ResourceAccessException e) {
            log.warn("Timeout o fallo de conectividad (limite 3000 ms) al consultar datos abiertos de Malaga para ({}, {}): {}",
                    latitude, longitude, e.getMessage());
            return new ReverseGeocodeResponse(null, null, null);
        } catch (HttpStatusCodeException e) {
            log.warn("Error HTTP {} recibido desde datos abiertos de Malaga para ({}, {}): {}",
                    e.getStatusCode(), latitude, longitude, e.getMessage());
            return new ReverseGeocodeResponse(null, null, null);
        } catch (RestClientException e) {
            log.warn("Error de cliente REST al consultar datos abiertos de Malaga para ({}, {}): {}",
                    latitude, longitude, e.getMessage());
            return new ReverseGeocodeResponse(null, null, null);
        } catch (Exception e) {
            log.warn("Error inesperado al resolver ubicacion con datos abiertos de Malaga para ({}, {}): {}",
                    latitude, longitude, e.getMessage());
            return new ReverseGeocodeResponse(null, null, null);
        }
    }

    /**
     * Parsea el cuerpo de la respuesta JSON proveniente de Datos Abiertos de Malaga,
     * admitiendo esquemas directos, respuestas CKAN Action API o geometrias GeoJSON.
     *
     * @param json Cuerpo de la respuesta HTTP en formato texto JSON.
     * @return DTO {@link ReverseGeocodeResponse} normalizado con los campos extraidos.
     */
    private ReverseGeocodeResponse parseTerritorialResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode targetNode = null;

            // 1. Caso respuesta CKAN Action API (e.g., result.records[])
            if (root.has("result")) {
                JsonNode resultNode = root.get("result");
                if (resultNode.has("records") && resultNode.get("records").isArray()) {
                    JsonNode records = resultNode.get("records");
                    if (!records.isEmpty()) {
                        targetNode = records.get(0);
                    }
                } else if (resultNode.isObject()) {
                    targetNode = resultNode;
                }
            }
            // 2. Caso GeoJSON (e.g., features[].properties)
            else if (root.has("features") && root.get("features").isArray()) {
                JsonNode features = root.get("features");
                if (!features.isEmpty()) {
                    JsonNode firstFeature = features.get(0);
                    targetNode = firstFeature.has("properties") ? firstFeature.get("properties") : firstFeature;
                }
            }
            // 3. Caso array directo de registros JSON
            else if (root.isArray()) {
                if (!root.isEmpty()) {
                    targetNode = root.get(0);
                }
            }
            // 4. Caso objeto JSON plano directo
            else if (root.isObject()) {
                targetNode = root;
            }

            if (targetNode == null) {
                log.info("No se hallaron registros coincidentes en los datos abiertos de Malaga.");
                return new ReverseGeocodeResponse(null, null, null);
            }

            String address = extractField(targetNode, "address", "direccion", "calle", "nombre_via", "formatted_address", "display_name", "name");
            String district = extractField(targetNode, "district", "distrito", "nombre_distrito", "nom_distrito", "distrito_nombre");
            String neighborhood = extractField(targetNode, "neighborhood", "barrio", "nombre_barrio", "nom_barrio", "barrio_nombre");

            return new ReverseGeocodeResponse(address, district, neighborhood);

        } catch (JsonProcessingException e) {
            log.warn("Fallo de deserializacion al parsear respuesta JSON de datos abiertos: {}", e.getMessage());
            return new ReverseGeocodeResponse(null, null, null);
        }
    }

    /**
     * Extrae el primer valor no nulo ni vacio para un conjunto de claves posibles en un nodo JSON.
     *
     * @param node       Nodo JSON del cual extraer el campo.
     * @param fieldNames Claves alternativas a inspeccionar.
     * @return El valor de texto limpio, o {@code null} si ninguna clave coincide o tiene valor significativo.
     */
    private String extractField(JsonNode node, String... fieldNames) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode val = node.get(fieldName);
            if (val != null && !val.isNull()) {
                String text = val.asText();
                if (text != null && !text.trim().isEmpty() && !text.equalsIgnoreCase("null")) {
                    return text.trim();
                }
            }
        }
        return null;
    }
}
