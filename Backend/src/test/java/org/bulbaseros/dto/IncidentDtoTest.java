package org.bulbaseros.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.bulbaseros.entity.EstadoIncidencia;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para los DTOs de triaje de incidencias:
 * {@link UpdateIncidentStatusRequest} e {@link IncidentStatusResponse}.
 * <p>
 * Verifica restricciones Bean Validation, constructores sobrecargados,
 * métodos accesores (getters y setters) y marcas de tiempo por defecto.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class IncidentDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    /**
     * Valida que una solicitud con estado válido no contenga violaciones de validación.
     */
    @Test
    @DisplayName("UpdateIncidentStatusRequest válido supera las restricciones Bean Validation")
    void shouldValidateValidUpdateIncidentStatusRequest() {
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest(EstadoIncidencia.IN_PROGRESS, "Comentario de triaje");
        Set<ConstraintViolation<UpdateIncidentStatusRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "No deben existir violaciones de validación");
        assertEquals(EstadoIncidencia.IN_PROGRESS, request.getStatus());
        assertEquals("Comentario de triaje", request.getComment());
    }

    /**
     * Valida que una solicitud con estado nulo falle la validación Bean Validation.
     */
    @Test
    @DisplayName("UpdateIncidentStatusRequest con estado nulo falla la validación @NotNull")
    void shouldFailValidationWhenStatusIsNull() {
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        Set<ConstraintViolation<UpdateIncidentStatusRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Debe existir al menos una violación de validación");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("El estado de la incidencia es obligatorio")));
    }

    /**
     * Valida los constructores y setters de UpdateIncidentStatusRequest.
     */
    @Test
    @DisplayName("UpdateIncidentStatusRequest soporta constructores y setters adecuadamente")
    void shouldSupportConstructorsAndSettersForRequest() {
        UpdateIncidentStatusRequest req1 = new UpdateIncidentStatusRequest(EstadoIncidencia.VALIDATED);
        assertEquals(EstadoIncidencia.VALIDATED, req1.getStatus());
        assertNull(req1.getComment());

        UpdateIncidentStatusRequest req2 = new UpdateIncidentStatusRequest("ASSIGNED");
        assertEquals(EstadoIncidencia.ASSIGNED, req2.getStatus());

        UpdateIncidentStatusRequest reqNull = new UpdateIncidentStatusRequest((String) null);
        assertNull(reqNull.getStatus());

        UpdateIncidentStatusRequest reqEmpty = new UpdateIncidentStatusRequest("   ");
        assertNull(reqEmpty.getStatus());

        req1.setStatus(EstadoIncidencia.RESOLVED);
        req1.setComment("Resuelto satisfactoriamente");
        assertEquals(EstadoIncidencia.RESOLVED, req1.getStatus());
        assertEquals("Resuelto satisfactoriamente", req1.getComment());
    }

    /**
     * Valida los constructores, getters y setters de IncidentStatusResponse.
     */
    @Test
    @DisplayName("IncidentStatusResponse soporta constructores, getters y setters correctamente")
    void shouldSupportConstructorsAndGettersForResponse() {
        OffsetDateTime now = OffsetDateTime.now();
        IncidentStatusResponse response = new IncidentStatusResponse(10L, EstadoIncidencia.CLOSED, "Incidencia cerrada", now);

        assertEquals(10L, response.getId());
        assertEquals(EstadoIncidencia.CLOSED, response.getStatus());
        assertEquals("Incidencia cerrada", response.getMessage());
        assertEquals(now, response.getUpdatedAt());

        IncidentStatusResponse responseDefault = new IncidentStatusResponse();
        assertNotNull(responseDefault.getUpdatedAt());

        IncidentStatusResponse responseThreeArgs = new IncidentStatusResponse(20L, EstadoIncidencia.REJECTED, "Incidencia rechazada");
        assertEquals(20L, responseThreeArgs.getId());
        assertEquals(EstadoIncidencia.REJECTED, responseThreeArgs.getStatus());
        assertEquals("Incidencia rechazada", responseThreeArgs.getMessage());
        assertNotNull(responseThreeArgs.getUpdatedAt());

        responseDefault.setId(30L);
        responseDefault.setStatus(EstadoIncidencia.REOPENED);
        responseDefault.setMessage("Reabierta para comprobación");
        OffsetDateTime customTime = now.plusDays(1);
        responseDefault.setUpdatedAt(customTime);

        assertEquals(30L, responseDefault.getId());
        assertEquals(EstadoIncidencia.REOPENED, responseDefault.getStatus());
        assertEquals("Reabierta para comprobación", responseDefault.getMessage());
        assertEquals(customTime, responseDefault.getUpdatedAt());
    }
}
