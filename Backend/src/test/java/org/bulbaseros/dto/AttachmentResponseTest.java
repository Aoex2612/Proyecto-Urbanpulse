package org.bulbaseros.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el DTO {@link AttachmentResponse}.
 * <p>
 * Verifica la correcta instanciación mediante constructores, builder y métodos de acceso getter/setter.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class AttachmentResponseTest {

    @Test
    @DisplayName("Debería inicializar DTO correctamente mediante constructor completo y getters")
    void shouldInitializeWithFullConstructorAndGetters() {
        OffsetDateTime now = OffsetDateTime.now();
        AttachmentResponse response = new AttachmentResponse(
                1L,
                10L,
                "bache.jpg",
                "/uploads/uuid_bache.jpg",
                "image/jpeg",
                102400L,
                now
        );

        assertEquals(1L, response.getId());
        assertEquals(10L, response.getIncidentId());
        assertEquals("bache.jpg", response.getFileName());
        assertEquals("/uploads/uuid_bache.jpg", response.getFileUrl());
        assertEquals("image/jpeg", response.getMimeType());
        assertEquals(102400L, response.getFileSizeBytes());
        assertEquals(now, response.getUploadedAt());
    }

    @Test
    @DisplayName("Debería permitir construir instancias mediante patrón Builder y setters")
    void shouldBuildAndMutateWithSetters() {
        OffsetDateTime now = OffsetDateTime.now();
        AttachmentResponse response = AttachmentResponse.builder()
                .id(2L)
                .incidentId(20L)
                .fileName("foto.png")
                .fileUrl("/uploads/foto.png")
                .mimeType("image/png")
                .fileSizeBytes(204800L)
                .uploadedAt(now)
                .build();

        assertEquals(2L, response.getId());
        assertEquals(20L, response.getIncidentId());
        assertEquals("foto.png", response.getFileName());

        response.setFileName("foto_actualizada.png");
        assertEquals("foto_actualizada.png", response.getFileName());
    }

    @Test
    @DisplayName("Debería soportar constructor vacío e igualdad de Lombok")
    void shouldSupportNoArgsConstructorAndEquals() {
        AttachmentResponse response1 = new AttachmentResponse();
        response1.setId(5L);

        AttachmentResponse response2 = new AttachmentResponse();
        response2.setId(5L);

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertTrue(response1.toString().contains("id=5"));
    }
}
