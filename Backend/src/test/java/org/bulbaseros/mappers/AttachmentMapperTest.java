package org.bulbaseros.mappers;

import org.bulbaseros.dto.AttachmentResponse;
import org.bulbaseros.entity.ArchivoAdjunto;
import org.bulbaseros.entity.Incidencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link AttachmentMapper}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
class AttachmentMapperTest {

    private final AttachmentMapper mapper = new AttachmentMapper();

    @Test
    @DisplayName("Debe retornar null cuando la entidad ArchivoAdjunto es nula")
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    @DisplayName("Debe mapear correctamente todos los campos de ArchivoAdjunto a AttachmentResponse")
    void shouldMapEntityToDtoCorrectly() {
        Incidencia incident = new Incidencia();
        incident.setId(42L);

        OffsetDateTime now = OffsetDateTime.now();
        ArchivoAdjunto entity = ArchivoAdjunto.builder()
                .id(1L)
                .incident(incident)
                .fileName("evidencia.jpg")
                .fileUrl("/uploads/uuid-evidencia.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(150000L)
                .uploadedAt(now)
                .build();

        AttachmentResponse dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(42L, dto.getIncidentId());
        assertEquals("evidencia.jpg", dto.getFileName());
        assertEquals("/uploads/uuid-evidencia.jpg", dto.getFileUrl());
        assertEquals("image/jpeg", dto.getMimeType());
        assertEquals(150000L, dto.getFileSizeBytes());
        assertEquals(now, dto.getUploadedAt());
    }

    @Test
    @DisplayName("Debe asignar incidentId nulo si la entidad no tiene incidencia asociada")
    void shouldHandleNullIncidentInEntity() {
        ArchivoAdjunto entity = ArchivoAdjunto.builder()
                .id(2L)
                .incident(null)
                .fileName("foto.png")
                .fileUrl("/uploads/foto.png")
                .mimeType("image/png")
                .fileSizeBytes(2000L)
                .uploadedAt(OffsetDateTime.now())
                .build();

        AttachmentResponse dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(2L, dto.getId());
        assertNull(dto.getIncidentId());
    }

    @Test
    @DisplayName("Debe mapear una lista de entidades a lista de DTOs correctamente")
    void shouldMapEntityListToDtoListCorrectly() {
        Incidencia incident = new Incidencia();
        incident.setId(10L);

        ArchivoAdjunto entity1 = ArchivoAdjunto.builder()
                .id(1L)
                .incident(incident)
                .fileName("foto1.jpg")
                .fileUrl("/uploads/foto1.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(100L)
                .uploadedAt(OffsetDateTime.now())
                .build();

        ArchivoAdjunto entity2 = ArchivoAdjunto.builder()
                .id(2L)
                .incident(incident)
                .fileName("foto2.png")
                .fileUrl("/uploads/foto2.png")
                .mimeType("image/png")
                .fileSizeBytes(200L)
                .uploadedAt(OffsetDateTime.now())
                .build();

        List<AttachmentResponse> dtoList = mapper.toDtoList(List.of(entity1, entity2));

        assertEquals(2, dtoList.size());
        assertEquals("foto1.jpg", dtoList.get(0).getFileName());
        assertEquals("foto2.png", dtoList.get(1).getFileName());
    }

    @Test
    @DisplayName("Debe retornar lista vacía cuando la lista de entidades es nula o vacía")
    void shouldReturnEmptyListWhenInputListIsNullOrEmpty() {
        assertTrue(mapper.toDtoList(null).isEmpty());
        assertTrue(mapper.toDtoList(Collections.emptyList()).isEmpty());
    }
}
