package org.bulbaseros.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el DTO {@link CreateIncidentRequest}.
 * <p>
 * Valida el cumplimiento de las restricciones Jakarta Bean Validation impuestas
 * sobre el alta de incidencias (@NotBlank en titulo, descripcion y categoria; @NotNull y rangos en coordenadas).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class CreateIncidentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Deberia ser valido cuando todos los campos requeridos estan presentes y dentro de rangos")
    void shouldBeValidWhenAllRequiredFieldsArePresent() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Farola fundida",
                "Calle Marmoles 5 sin iluminacion",
                "Alumbrado",
                36.7213,
                -4.4214
        );

        Set<ConstraintViolation<CreateIncidentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "No deberia existir ninguna violacion de validacion");
    }

    @Test
    @DisplayName("Deberia fallar la validacion si el titulo es nulo o esta en blanco")
    void shouldFailWhenTitleIsBlank() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "   ",
                "Descripcion valida",
                "Alumbrado",
                36.7213,
                -4.4214
        );

        Set<ConstraintViolation<CreateIncidentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("El titulo de la incidencia es obligatorio")));
    }

    @Test
    @DisplayName("Deberia fallar la validacion si la descripcion es nula o esta en blanco")
    void shouldFailWhenDescriptionIsBlank() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Titulo valido",
                "",
                "Alumbrado",
                36.7213,
                -4.4214
        );

        Set<ConstraintViolation<CreateIncidentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("La descripcion de la incidencia es obligatoria")));
    }

    @Test
    @DisplayName("Deberia fallar la validacion si la categoria es nula o esta en blanco")
    void shouldFailWhenCategoryIsBlank() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Titulo valido",
                "Descripcion valida",
                null,
                36.7213,
                -4.4214
        );

        Set<ConstraintViolation<CreateIncidentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("La categoria de la incidencia es obligatoria")));
    }

    @Test
    @DisplayName("Deberia fallar la validacion si las coordenadas son nulas")
    void shouldFailWhenCoordinatesAreNull() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Titulo valido",
                "Descripcion valida",
                "Limpieza",
                null,
                null
        );

        Set<ConstraintViolation<CreateIncidentRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("La latitud de la ubicacion es obligatoria")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("La longitud de la ubicacion es obligatoria")));
    }

    @Test
    @DisplayName("Deberia fallar la validacion si las coordenadas exceden los limites geograficos")
    void shouldFailWhenCoordinatesAreOutOfRange() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Titulo valido",
                "Descripcion valida",
                "Limpieza",
                95.0,
                -190.0
        );

        Set<ConstraintViolation<CreateIncidentRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("latitud")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("longitud")));
    }

    @Test
    @DisplayName("Deberia verificar constructor completo y getters/setters")
    void shouldVerifyFullConstructorAndGettersSetters() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Contenedor quemado");
        request.setDescription("Fuego extinguido pero restos de basura");
        request.setCategory("Limpieza");
        request.setLatitude(36.7150);
        request.setLongitude(-4.4250);
        request.setAccuracyMeters(5.0);
        request.setAddress("Calle Victoria 25");
        request.setDistrict("Centro");
        request.setNeighborhood("La Merced");

        assertEquals("Contenedor quemado", request.getTitle());
        assertEquals("Fuego extinguido pero restos de basura", request.getDescription());
        assertEquals("Limpieza", request.getCategory());
        assertEquals(36.7150, request.getLatitude());
        assertEquals(-4.4250, request.getLongitude());
        assertEquals(5.0, request.getAccuracyMeters());
        assertEquals("Calle Victoria 25", request.getAddress());
        assertEquals("Centro", request.getDistrict());
        assertEquals("La Merced", request.getNeighborhood());
    }
}
