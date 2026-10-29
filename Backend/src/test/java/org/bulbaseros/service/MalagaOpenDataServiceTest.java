package org.bulbaseros.service;

import org.bulbaseros.dto.ReverseGeocodeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pruebas unitarias e integradas para {@link MalagaOpenDataService}.
 * <p>
 * Verifica la comunicacion con el portal de datos abiertos de Malaga, la extraccion
 * de datos territoriales (direccion, distrito, barrio), la validacion de limites geograficos
 * y la aplicacion estricta de degradacion controlada ante timeouts, errores HTTP y fallos de red.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class MalagaOpenDataServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private MalagaOpenDataService service;

    private static final String BASE_URL = "https://datosabiertos.malaga.eu";
    private static final String ENDPOINT_PATH = "/api/action/datastore_search";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        service = new MalagaOpenDataService(restTemplate, BASE_URL, ENDPOINT_PATH);
    }

    @Nested
    @DisplayName("Pruebas de resolucion exitosa con diferentes esquemas de respuesta")
    class SuccessfulResolutionTests {

        @Test
        @DisplayName("Debe resolver direccion, distrito y barrio con respuesta estandar CKAN Action API")
        void resolveLocation_whenSuccessfulCkanResponse_returnsPopulatedResponse() {
            Double lat = 36.7213;
            Double lon = -4.4214;
            String ckanJson = """
                {
                    "help": "https://datosabiertos.malaga.eu/api/3/action/help_show?name=datastore_search",
                    "success": true,
                    "result": {
                        "records": [
                            {
                                "direccion": "Calle Marqués de Larios, 4",
                                "distrito": "Centro",
                                "barrio": "Centro Histórico"
                            }
                        ]
                    }
                }
                """;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(requestTo(containsString("latitude=36.7213")))
                    .andExpect(requestTo(containsString("longitude=-4.4214")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(ckanJson, MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isEqualTo("Calle Marqués de Larios, 4");
            assertThat(response.getDistrict()).isEqualTo("Centro");
            assertThat(response.getNeighborhood()).isEqualTo("Centro Histórico");
        }

        @Test
        @DisplayName("Debe resolver campos territoriales con esquema GeoJSON FeatureCollection")
        void resolveLocation_whenGeoJsonResponse_returnsExtractedFields() {
            Double lat = 36.7180;
            Double lon = -4.4230;
            String geoJson = """
                {
                    "type": "FeatureCollection",
                    "features": [
                        {
                            "type": "Feature",
                            "properties": {
                                "calle": "Alameda Principal, 12",
                                "nombre_distrito": "Distrito 1 - Centro",
                                "nombre_barrio": "Ensanche Centro"
                            }
                        }
                    ]
                }
                """;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(geoJson, MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isEqualTo("Alameda Principal, 12");
            assertThat(response.getDistrict()).isEqualTo("Distrito 1 - Centro");
            assertThat(response.getNeighborhood()).isEqualTo("Ensanche Centro");
        }

        @Test
        @DisplayName("Debe resolver campos territoriales con objeto JSON plano directo")
        void resolveLocation_whenDirectJsonObject_returnsExtractedFields() {
            Double lat = 36.7200;
            Double lon = -4.4200;
            String flatJson = """
                {
                    "address": "Plaza de la Marina, 2",
                    "district": "Centro",
                    "neighborhood": "Centro"
                }
                """;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(flatJson, MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isEqualTo("Plaza de la Marina, 2");
            assertThat(response.getDistrict()).isEqualTo("Centro");
            assertThat(response.getNeighborhood()).isEqualTo("Centro");
        }

        @Test
        @DisplayName("Debe resolver campos territoriales con un array JSON de registros directos")
        void resolveLocation_whenJsonArray_returnsExtractedFields() {
            Double lat = 36.7220;
            Double lon = -4.4250;
            String arrayJson = """
                [
                    {
                        "nombre_via": "Calle Granada, 10",
                        "distrito": "Centro",
                        "barrio": "Centro Histórico"
                    }
                ]
                """;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(arrayJson, MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isEqualTo("Calle Granada, 10");
            assertThat(response.getDistrict()).isEqualTo("Centro");
            assertThat(response.getNeighborhood()).isEqualTo("Centro Histórico");
        }

        @Test
        @DisplayName("Debe resolver campos territoriales cuando result de CKAN es un objeto directo")
        void resolveLocation_whenCkanResultIsDirectObject_returnsExtractedFields() {
            Double lat = 36.7210;
            Double lon = -4.4210;
            String json = """
                {
                    "success": true,
                    "result": {
                        "direccion": "Calle Larios, 2",
                        "distrito": "Centro",
                        "barrio": "Centro Histórico"
                    }
                }
                """;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isEqualTo("Calle Larios, 2");
            assertThat(response.getDistrict()).isEqualTo("Centro");
            assertThat(response.getNeighborhood()).isEqualTo("Centro Histórico");
        }

        @Test
        @DisplayName("Debe resolver campos cuando un feature de GeoJSON no incluye bloque properties anidado")
        void resolveLocation_whenGeoJsonFeatureWithoutProperties_returnsExtractedFields() {
            Double lat = 36.7215;
            Double lon = -4.4215;
            String json = """
                {
                    "type": "FeatureCollection",
                    "features": [
                        {
                            "calle": "Calle Alcazabilla",
                            "distrito": "Centro",
                            "barrio": "Centro"
                        }
                    ]
                }
                """;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isEqualTo("Calle Alcazabilla");
            assertThat(response.getDistrict()).isEqualTo("Centro");
            assertThat(response.getNeighborhood()).isEqualTo("Centro");
        }
    }

    @Nested
    @DisplayName("Pruebas de respuesta sin coincidencias")
    class NoMatchTests {

        @Test
        @DisplayName("Debe retornar campos null cuando CKAN devuelve lista records vacia")
        void resolveLocation_whenCkanRecordsEmpty_returnsNullFields() {
            Double lat = 36.7200;
            Double lon = -4.4200;
            String emptyCkan = """
                {
                    "success": true,
                    "result": {
                        "records": []
                    }
                }
                """;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(emptyCkan, MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe retornar campos null cuando GeoJSON devuelve lista features vacia")
        void resolveLocation_whenGeoJsonFeaturesEmpty_returnsNullFields() {
            Double lat = 36.7200;
            Double lon = -4.4200;
            String emptyGeoJson = """
                {
                    "type": "FeatureCollection",
                    "features": []
                }
                """;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(emptyGeoJson, MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe retornar campos null cuando el cuerpo de la respuesta es un JSON vacio")
        void resolveLocation_whenEmptyJsonObject_returnsNullFields() {
            Double lat = 36.7200;
            Double lon = -4.4200;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe retornar campos null cuando el cuerpo de respuesta esta completamente en blanco")
        void resolveLocation_whenBlankResponseBody_returnsNullFields() {
            Double lat = 36.7200;
            Double lon = -4.4200;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("   ", MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe retornar campos null cuando el JSON no es valido sintacticamente")
        void resolveLocation_whenMalformedJson_returnsNullFields() {
            Double lat = 36.7200;
            Double lon = -4.4200;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("{no-es-json-valido", MediaType.APPLICATION_JSON));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }
    }

    @Nested
    @DisplayName("Pruebas de degradacion controlada ante timeout y fallos de red")
    class ControlledDegradationTimeoutAndNetworkTests {

        @Test
        @DisplayName("Debe aplicar degradacion controlada devolviendo campos null ante timeout de 3 segundos")
        void resolveLocation_whenTimeoutOccurs_returnsNullFieldsWithoutThrowing() {
            Double lat = 36.7213;
            Double lon = -4.4214;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withException(new SocketTimeoutException("Read timed out")));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe aplicar degradacion controlada devolviendo campos null ante excepcion de conectividad general")
        void resolveLocation_whenConnectionFails_returnsNullFieldsWithoutThrowing() {
            Double lat = 36.7213;
            Double lon = -4.4214;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withException(new java.io.IOException("Connection refused")));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe aplicar degradacion controlada ante RestClientException no relacionada con HTTP/I/O")
        void resolveLocation_whenRestClientException_returnsNullFields() {
            RestTemplate mockedRestTemplate = org.mockito.Mockito.mock(RestTemplate.class);
            org.mockito.Mockito.when(mockedRestTemplate.getForObject(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(String.class)))
                    .thenThrow(new org.springframework.web.client.RestClientException("Error generico en cliente REST"));

            MalagaOpenDataService mockService = new MalagaOpenDataService(mockedRestTemplate, BASE_URL, ENDPOINT_PATH);
            ReverseGeocodeResponse response = mockService.resolveLocation(36.7213, -4.4214);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe aplicar degradacion controlada ante excepcion no controlada del sistema")
        void resolveLocation_whenUnexpectedException_returnsNullFields() {
            RestTemplate mockedRestTemplate = org.mockito.Mockito.mock(RestTemplate.class);
            org.mockito.Mockito.when(mockedRestTemplate.getForObject(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(String.class)))
                    .thenThrow(new RuntimeException("Fallo catastrofico inesperado"));

            MalagaOpenDataService mockService = new MalagaOpenDataService(mockedRestTemplate, BASE_URL, ENDPOINT_PATH);
            ReverseGeocodeResponse response = mockService.resolveLocation(36.7213, -4.4214);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }
    }

    @Nested
    @DisplayName("Pruebas de degradacion controlada ante errores HTTP del servidor remoto")
    class ControlledDegradationHttpErrorTests {

        @Test
        @DisplayName("Debe aplicar degradacion controlada devolviendo campos null ante HTTP 500 Internal Server Error")
        void resolveLocation_whenHttp500ServerError_returnsNullFieldsWithoutThrowing() {
            Double lat = 36.7213;
            Double lon = -4.4214;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withServerError());

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe aplicar degradacion controlada devolviendo campos null ante HTTP 503 Service Unavailable")
        void resolveLocation_whenHttp503ServiceUnavailable_returnsNullFieldsWithoutThrowing() {
            Double lat = 36.7213;
            Double lon = -4.4214;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }

        @Test
        @DisplayName("Debe aplicar degradacion controlada devolviendo campos null ante HTTP 404 Not Found")
        void resolveLocation_whenHttp404NotFound_returnsNullFieldsWithoutThrowing() {
            Double lat = 36.7213;
            Double lon = -4.4214;

            mockServer.expect(requestTo(containsString(ENDPOINT_PATH)))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            ReverseGeocodeResponse response = service.resolveLocation(lat, lon);

            mockServer.verify();
            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
        }
    }

    @Nested
    @DisplayName("Pruebas de validacion de coordenadas nulas o fuera de limites de Malaga")
    class CoordinatesValidationTests {

        @Test
        @DisplayName("Debe retornar campos null de inmediato sin invocar HTTP cuando latitud es nula")
        void resolveLocation_whenLatitudeIsNull_returnsNullFieldsWithoutHttpRequest() {
            ReverseGeocodeResponse response = service.resolveLocation(null, -4.4214);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("Debe retornar campos null de inmediato sin invocar HTTP cuando longitud es nula")
        void resolveLocation_whenLongitudeIsNull_returnsNullFieldsWithoutHttpRequest() {
            ReverseGeocodeResponse response = service.resolveLocation(36.7213, null);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("Debe retornar campos null de inmediato sin invocar HTTP cuando ambas coordenadas son nulas")
        void resolveLocation_whenBothCoordinatesNull_returnsNullFieldsWithoutHttpRequest() {
            ReverseGeocodeResponse response = service.resolveLocation(null, null);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            assertThat(response.getDistrict()).isNull();
            assertThat(response.getNeighborhood()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("Debe retornar campos null cuando latitud excede el limite superior de Malaga (lat > 36.9000)")
        void resolveLocation_whenLatitudeExceedsMax_returnsNullFields() {
            ReverseGeocodeResponse response = service.resolveLocation(37.0000, -4.4214);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("Debe retornar campos null cuando latitud es inferior al limite inferior de Malaga (lat < 36.5000)")
        void resolveLocation_whenLatitudeBelowMin_returnsNullFields() {
            ReverseGeocodeResponse response = service.resolveLocation(36.4000, -4.4214);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("Debe retornar campos null cuando longitud es inferior al limite oeste de Malaga (lon < -4.7000)")
        void resolveLocation_whenLongitudeBelowMin_returnsNullFields() {
            ReverseGeocodeResponse response = service.resolveLocation(36.7213, -4.8000);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("Debe retornar campos null cuando longitud excede el limite este de Malaga (lon > -4.2000)")
        void resolveLocation_whenLongitudeExceedsMax_returnsNullFields() {
            ReverseGeocodeResponse response = service.resolveLocation(36.7213, -4.1000);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("Debe retornar campos null para coordenadas lejanas como Madrid (40.4168, -3.7038)")
        void resolveLocation_whenCoordinatesInMadrid_returnsNullFields() {
            ReverseGeocodeResponse response = service.resolveLocation(40.4168, -3.7038);

            assertThat(response).isNotNull();
            assertThat(response.getAddress()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("isWithinMalagaBounds debe validar coordenadas dentro y fuera de limites con precision")
        void isWithinMalagaBounds_verifiesCorrectly() {
            // Centro de Malaga
            assertThat(service.isWithinMalagaBounds(36.7213, -4.4214)).isTrue();

            // Puntos frontera exactos
            assertThat(service.isWithinMalagaBounds(36.5000, -4.7000)).isTrue();
            assertThat(service.isWithinMalagaBounds(36.9000, -4.2000)).isTrue();
            assertThat(service.isWithinMalagaBounds(36.5000, -4.2000)).isTrue();
            assertThat(service.isWithinMalagaBounds(36.9000, -4.7000)).isTrue();

            // Puntos justo fuera de los limites
            assertThat(service.isWithinMalagaBounds(36.4999, -4.4214)).isFalse();
            assertThat(service.isWithinMalagaBounds(36.9001, -4.4214)).isFalse();
            assertThat(service.isWithinMalagaBounds(36.7213, -4.7001)).isFalse();
            assertThat(service.isWithinMalagaBounds(36.7213, -4.1999)).isFalse();

            // Nulos
            assertThat(service.isWithinMalagaBounds(null, -4.4214)).isFalse();
            assertThat(service.isWithinMalagaBounds(36.7213, null)).isFalse();
            assertThat(service.isWithinMalagaBounds(null, null)).isFalse();
        }

        @Test
        @DisplayName("validateCoordinates no debe lanzar excepcion para coordenadas validas")
        void validateCoordinates_withValidCoordinates_doesNotThrow() {
            assertThatCode(() -> service.validateCoordinates(36.7213, -4.4214))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateCoordinates debe lanzar IllegalArgumentException para coordenadas nulas")
        void validateCoordinates_withNullCoordinates_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> service.validateCoordinates(null, -4.4214))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no pueden ser nulas");

            assertThatThrownBy(() -> service.validateCoordinates(36.7213, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no pueden ser nulas");
        }

        @Test
        @DisplayName("validateCoordinates debe lanzar IllegalArgumentException para coordenadas fuera de limites")
        void validateCoordinates_withOutOfLimitsCoordinates_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> service.validateCoordinates(40.4168, -3.7038))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceden el termino municipal de Malaga");
        }
    }

    @Nested
    @DisplayName("Pruebas de constructores e inicializacion")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor con RestTemplateBuilder debe instanciar el servicio con configuracion de timeouts")
        void constructor_withRestTemplateBuilder_initializesProperly() {
            RestTemplateBuilder builder = new RestTemplateBuilder();
            MalagaOpenDataService customService = new MalagaOpenDataService(builder, BASE_URL, ENDPOINT_PATH);

            assertThat(customService).isNotNull();
            assertThat(customService.getRestTemplate()).isNotNull();
        }

        @Test
        @DisplayName("Constructor con solo RestTemplate debe usar valores por defecto")
        void constructor_withRestTemplateOnly_initializesWithDefaults() {
            MalagaOpenDataService customService = new MalagaOpenDataService(restTemplate);

            assertThat(customService).isNotNull();
            assertThat(customService.getRestTemplate()).isSameAs(restTemplate);
        }

        @Test
        @DisplayName("Constructor con valores nulos o en blanco de URL debe usar valores por defecto")
        void constructor_withNullOrBlankUrls_usesDefaults() {
            MalagaOpenDataService customService = new MalagaOpenDataService(restTemplate, null, "  ");

            assertThat(customService).isNotNull();
            assertThat(customService.getRestTemplate()).isSameAs(restTemplate);
        }

        @Test
        @DisplayName("Constructor sin argumentos debe inicializar RestTemplate autonomo")
        void defaultConstructor_initializesProperly() {
            MalagaOpenDataService defaultService = new MalagaOpenDataService();

            assertThat(defaultService).isNotNull();
            assertThat(defaultService.getRestTemplate()).isNotNull();
        }
    }
}
