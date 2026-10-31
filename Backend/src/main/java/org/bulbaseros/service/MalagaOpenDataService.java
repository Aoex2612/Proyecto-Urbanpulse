package org.bulbaseros.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bulbaseros.dto.ReverseGeocodeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * Servicio de integracion con el portal de Datos Abiertos del Ayuntamiento de Malaga
 * ({@code datosabiertos.malaga.eu}) y geocodificacion inversa asistida (Nominatim / OpenStreetMap)
 * para la resolucion territorial asistida de incidencias.
 * <p>
 * Implementa el principio arquitectonico de <b>Degradacion Controlada</b> definido en el ADR-06:
 * ante latencias excesivas (timeout estricto de 3000 ms), indisponibilidad del servicio externo,
 * errores de certificacion SSL, errores HTTP o coordenadas sin correspondencia territorial, el servicio
 * gestiona la conexion de forma limpia y transparente, consulta fuentes alternativas de fallback o devuelve un
 * {@link ReverseGeocodeResponse} con campos nulos para no bloquear el flujo de creacion o consulta.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.1
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
     * URL base predeterminada para el servicio de geocodificacion inversa de respaldo (Nominatim OpenStreetMap).
     */
    public static final String DEFAULT_NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";

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
     * URL del servicio de fallback Nominatim OpenStreetMap (opcional).
     */
    private final String nominatimUrl;

    /**
     * Deserializador JSON para procesar respuestas complejas o estructuradas de CKAN / GeoJSON / Nominatim.
     */
    private final ObjectMapper objectMapper;

    /**
     * Crea una factoria de clientes HTTP con timeouts de 3000 ms y configuracion SSL permisiva
     * para evitar excepciones {@code SunCertPathBuilderException} producidas por certificados
     * de administraciones publicas con cadenas intermedias no presentes en el truststore JVM.
     *
     * @return {@link SimpleClientHttpRequestFactory} con soporte TLS permisivo y timeouts estrictos.
     */
    public static SimpleClientHttpRequestFactory createSecureClientHttpRequestFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            sslContext.init(null, trustAllCerts, new SecureRandom());

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection httpsConnection) {
                        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };
            factory.setConnectTimeout(TIMEOUT_MILLIS);
            factory.setReadTimeout(TIMEOUT_MILLIS);
            return factory;
        } catch (Exception e) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(TIMEOUT_MILLIS);
            factory.setReadTimeout(TIMEOUT_MILLIS);
            return factory;
        }
    }

    /**
     * Construye un cliente {@link RestTemplate} con factoria segura y timeouts estrictos.
     *
     * @param builder Generador de clientes RestTemplate inyectado por Spring Boot.
     * @return Instancia configurada de RestTemplate.
     */
    private static RestTemplate createCustomRestTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(MalagaOpenDataService::createSecureClientHttpRequestFactory)
                .connectTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .readTimeout(Duration.ofMillis(TIMEOUT_MILLIS))
                .defaultHeader(HttpHeaders.USER_AGENT, "UrbanPulse-App/1.0")
                .build();
    }

    /**
     * Constructor principal para la inyeccion de dependencias en Spring Boot.
     *
     * @param restTemplateBuilder Creador de clientes RestTemplate proporcionado por Spring Boot.
     * @param baseUrl             URL base inyectada desde propiedades de configuracion.
     * @param endpointPath        Ruta del endpoint territorial inyectada desde configuracion.
     * @param nominatimUrl        URL base del servicio de respaldo Nominatim.
     */
    @Autowired
    public MalagaOpenDataService(RestTemplateBuilder restTemplateBuilder,
                                 @Value("${malaga.opendata.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
                                 @Value("${malaga.opendata.endpoint-path:" + DEFAULT_ENDPOINT_PATH + "}") String endpointPath,
                                 @Value("${nominatim.base-url:" + DEFAULT_NOMINATIM_URL + "}") String nominatimUrl) {
        this(createCustomRestTemplate(restTemplateBuilder), baseUrl, endpointPath, nominatimUrl);
    }

    /**
     * Constructor sobrecargado con {@link RestTemplateBuilder} para compatibilidad con firmas previas.
     *
     * @param restTemplateBuilder Creador de clientes RestTemplate.
     * @param baseUrl             URL base inyectada.
     * @param endpointPath        Ruta del endpoint.
     */
    public MalagaOpenDataService(RestTemplateBuilder restTemplateBuilder, String baseUrl, String endpointPath) {
        this(restTemplateBuilder, baseUrl, endpointPath, DEFAULT_NOMINATIM_URL);
    }

    /**
     * Constructor parametrizado para pruebas unitarias con {@link RestTemplate} e integracion de respaldo.
     *
     * @param restTemplate Instancia de cliente HTTP a utilizar.
     * @param baseUrl      URL base del servicio remoto.
     * @param endpointPath Ruta del endpoint de consulta territorial.
     * @param nominatimUrl URL base del servicio de fallback Nominatim.
     */
    public MalagaOpenDataService(RestTemplate restTemplate, String baseUrl, String endpointPath, String nominatimUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : DEFAULT_BASE_URL;
        this.endpointPath = (endpointPath != null && !endpointPath.isBlank()) ? endpointPath : DEFAULT_ENDPOINT_PATH;
        this.nominatimUrl = (nominatimUrl != null && !nominatimUrl.isBlank()) ? nominatimUrl : null;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor sobrecargado de conveniencia para pruebas con un {@link RestTemplate} mockeado
     * sin servicio de respaldo de red activo por defecto.
     *
     * @param restTemplate Instancia de cliente HTTP configurada.
     * @param baseUrl      URL base del servicio remoto.
     * @param endpointPath Ruta del endpoint de consulta territorial.
     */
    public MalagaOpenDataService(RestTemplate restTemplate, String baseUrl, String endpointPath) {
        this(restTemplate, baseUrl, endpointPath, null);
    }

    /**
     * Constructor sobrecargado de conveniencia para pruebas con un {@link RestTemplate} mockeado
     * empleando las URLs predeterminadas del servicio sin fallback activo.
     *
     * @param restTemplate Instancia de cliente HTTP configurada.
     */
    public MalagaOpenDataService(RestTemplate restTemplate) {
        this(restTemplate, DEFAULT_BASE_URL, DEFAULT_ENDPOINT_PATH, null);
    }

    /**
     * Constructor sin argumentos que crea un cliente {@link RestTemplate} autonomo con timeouts y SSL permisivo.
     */
    public MalagaOpenDataService() {
        this.restTemplate = new RestTemplate(createSecureClientHttpRequestFactory());
        this.baseUrl = DEFAULT_BASE_URL;
        this.endpointPath = DEFAULT_ENDPOINT_PATH;
        this.nominatimUrl = DEFAULT_NOMINATIM_URL;
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
     * Obtiene la URL configurada para el servicio de respaldo Nominatim.
     *
     * @return La URL de Nominatim o {@code null} si no esta configurada.
     */
    public String getNominatimUrl() {
        return this.nominatimUrl;
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
     * los datos abiertos oficiales del Ayuntamiento de Malaga o su fallback de respaldo (Nominatim OpenStreetMap).
     * <p>
     * Aplica la politica de degradacion controlada: si las coordenadas estan fuera de los limites de Malaga,
     * si se supera el timeout de 3 segundos, si el portal remoto responde con un error HTTP o de certificado
     * o si no se halla ninguna coincidencia, se captura la condicion limpiamente, se consulta el fallback y,
     * en caso de agotarse las vias, se devuelve una instancia de {@link ReverseGeocodeResponse} con atributos en {@code null}.
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

        ReverseGeocodeResponse response = null;

        try {
            URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .path(endpointPath)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .build()
                    .toUri();

            log.debug("Consultando servicio de Datos Abiertos de Malaga en URI: {}", uri);
            String responseBody = restTemplate.getForObject(uri, String.class);

            if (responseBody != null && !responseBody.isBlank()) {
                response = parseTerritorialResponse(responseBody);
            } else {
                log.warn("Respuesta vacia al consultar datos abiertos de Malaga para ({}, {})", latitude, longitude);
            }

        } catch (ResourceAccessException e) {
            log.warn("Timeout o fallo de conectividad (limite 3000 ms) al consultar datos abiertos de Malaga para ({}, {}): {}",
                    latitude, longitude, e.getMessage());
        } catch (HttpStatusCodeException e) {
            log.warn("Error HTTP {} recibido desde datos abiertos de Malaga para ({}, {}): {}",
                    e.getStatusCode(), latitude, longitude, e.getMessage());
        } catch (RestClientException e) {
            log.warn("Error de cliente REST al consultar datos abiertos de Malaga para ({}, {}): {}",
                    latitude, longitude, e.getMessage());
        } catch (Exception e) {
            log.warn("Error inesperado al resolver ubicacion con datos abiertos de Malaga para ({}, {}): {}",
                    latitude, longitude, e.getMessage());
        }

        // Si la resolucion primaria obtuvo algun dato util, se devuelve directamente
        if (response != null && (response.getAddress() != null || response.getDistrict() != null || response.getNeighborhood() != null)) {
            return response;
        }

        // Fallback a Nominatim OpenStreetMap si esta configurado
        if (nominatimUrl != null && !nominatimUrl.isBlank()) {
            log.info("Activando fallback de geocodificacion inversa con Nominatim para ({}, {})", latitude, longitude);
            ReverseGeocodeResponse fallbackResponse = resolveWithNominatim(latitude, longitude);
            if (fallbackResponse != null && (fallbackResponse.getAddress() != null || fallbackResponse.getDistrict() != null || fallbackResponse.getNeighborhood() != null)) {
                return fallbackResponse;
            }
        }

        return new ReverseGeocodeResponse(null, null, null);
    }

    /**
     * Consulta el servicio de geocodificacion inversa de respaldo (Nominatim OpenStreetMap)
     * ante indisponibilidad, error de certificado o respuesta vacia del portal primario.
     *
     * @param latitude  Latitud del punto.
     * @param longitude Longitud del punto.
     * @return DTO {@link ReverseGeocodeResponse} normalizado o {@code null} si la consulta falla.
     */
    private ReverseGeocodeResponse resolveWithNominatim(Double latitude, Double longitude) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(nominatimUrl)
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("format", "json")
                    .build()
                    .toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "UrbanPulse-App/1.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isBlank()) {
                return parseTerritorialResponse(response.getBody());
            }
        } catch (Exception e) {
            log.warn("Fallo en fallback de Nominatim para ({}, {}): {}", latitude, longitude, e.getMessage());
        }
        return null;
    }

    /**
     * Parsea el cuerpo de la respuesta JSON proveniente de Datos Abiertos de Malaga o Nominatim,
     * admitiendo esquemas CKAN Action API, geometrias GeoJSON, arrays directos, objetos planos y Nominatim.
     *
     * @param json Cuerpo de la respuesta HTTP en formato texto JSON.
     * @return DTO {@link ReverseGeocodeResponse} normalizado con los campos extraidos.
     */
    public ReverseGeocodeResponse parseTerritorialResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode targetNode = null;

            // 1. Caso Nominatim OpenStreetMap (objeto con bloque 'address' anidado)
            if (root.has("address") && root.get("address").isObject()) {
                JsonNode addressNode = root.get("address");
                String road = extractField(addressNode, "road", "pedestrian", "footway", "street", "cycleway", "path", "square");
                String houseNumber = extractField(addressNode, "house_number");
                String address = road;
                if (road != null && houseNumber != null) {
                    address = road + ", " + houseNumber;
                } else if (address == null) {
                    address = extractField(root, "display_name", "name");
                }
                String district = extractField(addressNode, "city_district", "district", "borough", "suburb");
                String neighborhood = extractField(addressNode, "neighbourhood", "suburb", "quarter", "residential");

                return new ReverseGeocodeResponse(address, district, neighborhood);
            }

            // 2. Caso respuesta CKAN Action API (e.g., result.records[])
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
            // 3. Caso GeoJSON (e.g., features[].properties)
            else if (root.has("features") && root.get("features").isArray()) {
                JsonNode features = root.get("features");
                if (!features.isEmpty()) {
                    JsonNode firstFeature = features.get(0);
                    targetNode = firstFeature.has("properties") ? firstFeature.get("properties") : firstFeature;
                }
            }
            // 4. Caso array directo de registros JSON
            else if (root.isArray()) {
                if (!root.isEmpty()) {
                    targetNode = root.get(0);
                }
            }
            // 5. Caso objeto JSON plano directo
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
