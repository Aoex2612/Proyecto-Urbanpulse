package org.bulbaseros.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.PrioridadIncidencia;
import org.bulbaseros.entity.RolUsuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de Bean Validation y estructura de datos para los DTOs de triaje y operativa.
 * <p>
 * Verifica que las restricciones de validación (@NotNull, @NotBlank, @Size, @NotEmpty)
 * se apliquen rigurosamente según lo especificado en la feature 005.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // --- PriorityCalculationRequest ---

    @Test
    @DisplayName("PriorityCalculationRequest: Debe validar correctamente con campos obligatorios")
    void shouldValidatePriorityCalculationRequestSuccessfully() {
        PriorityCalculationRequest request = PriorityCalculationRequest.builder()
                .streetCut(true)
                .safetyRisk(SafetyRiskLevel.HIGH)
                .build();

        Set<ConstraintViolation<PriorityCalculationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("PriorityCalculationRequest: Debe fallar si streetCut o safetyRisk son nulos")
    void shouldFailWhenPriorityCalculationRequestHasNullFields() {
        PriorityCalculationRequest request = new PriorityCalculationRequest();

        Set<ConstraintViolation<PriorityCalculationRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
    }

    // --- PriorityCalculationResponse ---

    @Test
    @DisplayName("PriorityCalculationResponse: Debe contener y recuperar prioridad sugerida y justificación")
    void shouldHoldDataInPriorityCalculationResponse() {
        PriorityCalculationResponse response = PriorityCalculationResponse.builder()
                .suggestedPriority(PrioridadIncidencia.HIGH)
                .priorityReason("Riesgo para la seguridad vial")
                .build();

        assertEquals(PrioridadIncidencia.HIGH, response.getSuggestedPriority());
        assertEquals("Riesgo para la seguridad vial", response.getPriorityReason());
    }

    // --- UpdatePriorityRequest ---

    @Test
    @DisplayName("UpdatePriorityRequest: Debe validar correctamente con prioridad informada")
    void shouldValidateUpdatePriorityRequestSuccessfully() {
        UpdatePriorityRequest request = UpdatePriorityRequest.builder()
                .priority(PrioridadIncidencia.CRITICAL)
                .priorityReason("Incendio activo")
                .build();

        Set<ConstraintViolation<UpdatePriorityRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("UpdatePriorityRequest: Debe fallar si priority es nula")
    void shouldFailWhenUpdatePriorityRequestHasNullPriority() {
        UpdatePriorityRequest request = UpdatePriorityRequest.builder()
                .priorityReason("Sin prioridad")
                .build();

        Set<ConstraintViolation<UpdatePriorityRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // --- ValidateIncidentRequest ---

    @Test
    @DisplayName("ValidateIncidentRequest: Debe permitir motivo opcional o nulo")
    void shouldValidateValidateIncidentRequestWithOptionalReason() {
        ValidateIncidentRequest req1 = new ValidateIncidentRequest("Comprobación visual confirmada");
        ValidateIncidentRequest req2 = new ValidateIncidentRequest();

        assertTrue(validator.validate(req1).isEmpty());
        assertTrue(validator.validate(req2).isEmpty());
    }

    // --- RejectIncidentRequest ---

    @Test
    @DisplayName("RejectIncidentRequest: Debe validar con motivo de longitud suficiente (>= 5)")
    void shouldValidateRejectIncidentRequestSuccessfully() {
        RejectIncidentRequest request = new RejectIncidentRequest("Reporte duplicado con la incidencia #123");

        Set<ConstraintViolation<RejectIncidentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("RejectIncidentRequest: Debe fallar si el motivo es nulo, en blanco o menor a 5 caracteres")
    void shouldFailWhenRejectIncidentRequestHasInvalidReason() {
        RejectIncidentRequest reqNull = new RejectIncidentRequest(null);
        assertFalse(validator.validate(reqNull).isEmpty());

        RejectIncidentRequest reqBlank = new RejectIncidentRequest("   ");
        assertFalse(validator.validate(reqBlank).isEmpty());

        RejectIncidentRequest reqShort = new RejectIncidentRequest("No");
        Set<ConstraintViolation<RejectIncidentRequest>> violations = validator.validate(reqShort);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("5 caracteres")));
    }

    // --- ReevaluateIncidentRequest ---

    @Test
    @DisplayName("ReevaluateIncidentRequest: Debe validar con estado destino y motivo válidos")
    void shouldValidateReevaluateIncidentRequestSuccessfully() {
        ReevaluateIncidentRequest request = ReevaluateIncidentRequest.builder()
                .targetStatus(EstadoIncidencia.REPORTED)
                .reason("El ciudadano adjunta fotografías aclaratorias")
                .build();

        Set<ConstraintViolation<ReevaluateIncidentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
        assertTrue(request.isValidTargetStatus());
    }

    @Test
    @DisplayName("ReevaluateIncidentRequest: isValidTargetStatus debe ser true solo para REPORTED y VALIDATED")
    void shouldCheckTargetStatusValidity() {
        ReevaluateIncidentRequest reqReported = ReevaluateIncidentRequest.builder().targetStatus(EstadoIncidencia.REPORTED).build();
        ReevaluateIncidentRequest reqValidated = ReevaluateIncidentRequest.builder().targetStatus(EstadoIncidencia.VALIDATED).build();
        ReevaluateIncidentRequest reqClosed = ReevaluateIncidentRequest.builder().targetStatus(EstadoIncidencia.CLOSED).build();
        ReevaluateIncidentRequest reqResolved = ReevaluateIncidentRequest.builder().targetStatus(EstadoIncidencia.RESOLVED).build();

        assertTrue(reqReported.isValidTargetStatus());
        assertTrue(reqValidated.isValidTargetStatus());
        assertFalse(reqClosed.isValidTargetStatus());
        assertFalse(reqResolved.isValidTargetStatus());
    }

    @Test
    @DisplayName("ReevaluateIncidentRequest: Debe fallar si targetStatus es nulo o motivo es inválido")
    void shouldFailWhenReevaluateIncidentRequestHasInvalidFields() {
        ReevaluateIncidentRequest reqNoStatus = ReevaluateIncidentRequest.builder()
                .reason("Motivo suficientemente largo")
                .build();
        assertFalse(validator.validate(reqNoStatus).isEmpty());

        ReevaluateIncidentRequest reqShortReason = ReevaluateIncidentRequest.builder()
                .targetStatus(EstadoIncidencia.REPORTED)
                .reason("Hola")
                .build();
        assertFalse(validator.validate(reqShortReason).isEmpty());
    }

    // --- ResolveIncidentRequest ---

    @Test
    @DisplayName("ResolveIncidentRequest: Permite notas de resolución")
    void shouldHoldDataInResolveIncidentRequest() {
        ResolveIncidentRequest request = ResolveIncidentRequest.builder()
                .resolutionNotes("Se procedió a sustituir la luminaria defectuosa.")
                .build();

        assertEquals("Se procedió a sustituir la luminaria defectuosa.", request.getResolutionNotes());
        assertTrue(validator.validate(request).isEmpty());
    }

    // --- ReopenIncidentRequest ---

    @Test
    @DisplayName("ReopenIncidentRequest: Debe validar con motivo de al menos 5 caracteres")
    void shouldValidateReopenIncidentRequestSuccessfully() {
        ReopenIncidentRequest request = new ReopenIncidentRequest("La baldosa sigue levantada tras las obras");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("ReopenIncidentRequest: Debe fallar con motivo nulo, en blanco o menor a 5 caracteres")
    void shouldFailWhenReopenIncidentRequestHasInvalidReason() {
        ReopenIncidentRequest reqNull = new ReopenIncidentRequest(null);
        assertFalse(validator.validate(reqNull).isEmpty());

        ReopenIncidentRequest reqBlank = new ReopenIncidentRequest("    ");
        assertFalse(validator.validate(reqBlank).isEmpty());

        ReopenIncidentRequest reqShort = new ReopenIncidentRequest("Mal");
        assertFalse(validator.validate(reqShort).isEmpty());
    }

    // --- AssignIncidentRequest ---

    @Test
    @DisplayName("AssignIncidentRequest: Debe validar correctamente con lista no vacía de departamentos")
    void shouldValidateAssignIncidentRequestSuccessfully() {
        AssignIncidentRequest request = AssignIncidentRequest.builder()
                .departmentIds(List.of(1L, 2L))
                .technicianIds(List.of(10L))
                .build();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("AssignIncidentRequest: Debe fallar si departmentIds es nulo o vacío")
    void shouldFailWhenAssignIncidentRequestHasEmptyDepartmentIds() {
        AssignIncidentRequest reqNull = AssignIncidentRequest.builder()
                .technicianIds(List.of(10L))
                .build();
        assertFalse(validator.validate(reqNull).isEmpty());

        AssignIncidentRequest reqEmpty = AssignIncidentRequest.builder()
                .departmentIds(Collections.emptyList())
                .build();
        assertFalse(validator.validate(reqEmpty).isEmpty());
    }

    // --- DeactivateAssignmentRequest ---

    @Test
    @DisplayName("DeactivateAssignmentRequest: Permite instanciar con listas de departamentos y técnicos")
    void shouldHoldDataInDeactivateAssignmentRequest() {
        DeactivateAssignmentRequest request = DeactivateAssignmentRequest.builder()
                .departmentIds(List.of(1L))
                .technicianIds(List.of(5L))
                .build();

        assertEquals(List.of(1L), request.getDepartmentIds());
        assertEquals(List.of(5L), request.getTechnicianIds());
        assertTrue(validator.validate(request).isEmpty());
    }

    // --- CreateCommentRequest ---

    @Test
    @DisplayName("CreateCommentRequest: Debe validar con comentario y visibilidad interna especificados")
    void shouldValidateCreateCommentRequestSuccessfully() {
        CreateCommentRequest request = CreateCommentRequest.builder()
                .comment("Se avisa al equipo de turno")
                .isInternal(true)
                .build();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("CreateCommentRequest: Debe fallar si comment está en blanco o isInternal es nulo")
    void shouldFailWhenCreateCommentRequestHasInvalidFields() {
        CreateCommentRequest reqBlankComment = CreateCommentRequest.builder()
                .comment("   ")
                .isInternal(false)
                .build();
        assertFalse(validator.validate(reqBlankComment).isEmpty());

        CreateCommentRequest reqNullInternal = CreateCommentRequest.builder()
                .comment("Comentario válido")
                .isInternal(null)
                .build();
        assertFalse(validator.validate(reqNullInternal).isEmpty());
    }

    // --- UpdateCommentRequest ---

    @Test
    @DisplayName("UpdateCommentRequest: Debe validar con texto no en blanco")
    void shouldValidateUpdateCommentRequestSuccessfully() {
        UpdateCommentRequest request = new UpdateCommentRequest("Comentario corregido y ampliado");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    @DisplayName("UpdateCommentRequest: Debe fallar si comment es nulo o en blanco")
    void shouldFailWhenUpdateCommentRequestIsBlank() {
        UpdateCommentRequest reqNull = new UpdateCommentRequest(null);
        assertFalse(validator.validate(reqNull).isEmpty());

        UpdateCommentRequest reqBlank = new UpdateCommentRequest("");
        assertFalse(validator.validate(reqBlank).isEmpty());
    }

    // --- DTO Structure & SafetyRiskLevel Enum ---

    @Test
    @DisplayName("SafetyRiskLevel: Debe contener los tres niveles requeridos")
    void shouldVerifySafetyRiskLevelEnumValues() {
        SafetyRiskLevel[] values = SafetyRiskLevel.values();
        assertEquals(3, values.length);
        assertNotNull(SafetyRiskLevel.valueOf("NONE"));
        assertNotNull(SafetyRiskLevel.valueOf("LOW"));
        assertNotNull(SafetyRiskLevel.valueOf("HIGH"));
    }

    @Test
    @DisplayName("AssignmentResponse: Integridad de getters y setters con Lombok")
    void shouldVerifyAssignmentResponseGettersSetters() {
        OffsetDateTime now = OffsetDateTime.now();
        AssignmentResponse response = AssignmentResponse.builder()
                .id(1L)
                .departmentId(2L)
                .departmentName("Vías")
                .technicianId(3L)
                .technicianName("Pepe")
                .assignedById(4L)
                .assignedByName("Ana")
                .isActive(true)
                .assignedAt(now)
                .build();

        assertEquals(1L, response.getId());
        assertEquals(2L, response.getDepartmentId());
        assertEquals("Vías", response.getDepartmentName());
        assertEquals(3L, response.getTechnicianId());
        assertEquals("Pepe", response.getTechnicianName());
        assertEquals(4L, response.getAssignedById());
        assertEquals("Ana", response.getAssignedByName());
        assertTrue(response.getIsActive());
        assertEquals(now, response.getAssignedAt());
    }

    @Test
    @DisplayName("DepartmentResponse: Integridad de getters y setters con Lombok")
    void shouldVerifyDepartmentResponseGettersSetters() {
        DepartmentResponse response = DepartmentResponse.builder()
                .id(10L)
                .name("Alumbrado")
                .description("Red eléctrica urbana")
                .build();

        assertEquals(10L, response.getId());
        assertEquals("Alumbrado", response.getName());
        assertEquals("Red eléctrica urbana", response.getDescription());
    }

    @Test
    @DisplayName("UserSummaryResponse: Integridad de getters y setters con Lombok")
    void shouldVerifyUserSummaryResponseGettersSetters() {
        UserSummaryResponse response = UserSummaryResponse.builder()
                .id(100L)
                .fullName("Laura Sánchez")
                .email("laura@urbanpulse.es")
                .role(RolUsuario.TECHNICIAN)
                .departmentId(10L)
                .build();

        assertEquals(100L, response.getId());
        assertEquals("Laura Sánchez", response.getFullName());
        assertEquals("laura@urbanpulse.es", response.getEmail());
        assertEquals(RolUsuario.TECHNICIAN, response.getRole());
        assertEquals(10L, response.getDepartmentId());
    }

    @Test
    @DisplayName("CommentResponse: Integridad de getters y setters con Lombok")
    void shouldVerifyCommentResponseGettersSetters() {
        OffsetDateTime now = OffsetDateTime.now();
        CommentResponse response = CommentResponse.builder()
                .id(50L)
                .incidentId(12L)
                .authorId(7L)
                .authorName("Carlos")
                .authorRole("OPERATOR")
                .comment("Nota informativa")
                .isInternal(false)
                .createdAt(now)
                .updatedAt(now.plusHours(1))
                .build();

        assertEquals(50L, response.getId());
        assertEquals(12L, response.getIncidentId());
        assertEquals(7L, response.getAuthorId());
        assertEquals("Carlos", response.getAuthorName());
        assertEquals("OPERATOR", response.getAuthorRole());
        assertEquals("Nota informativa", response.getComment());
        assertFalse(response.getIsInternal());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now.plusHours(1), response.getUpdatedAt());
    }
}
