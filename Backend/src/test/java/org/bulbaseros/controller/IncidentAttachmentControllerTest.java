package org.bulbaseros.controller;

import org.bulbaseros.dto.AttachmentResponse;
import org.bulbaseros.service.AttachmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de endpoints REST y seguridad para la gestión de adjuntos multimedia en {@link IncidentController}.
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentAttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttachmentService attachmentService;

    @Test
    @DisplayName("Petición no autenticada para subir adjunto debe retornar 401 Unauthorized")
    void shouldRejectUnauthenticatedUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "imagen-prueba".getBytes()
        );

        mockMvc.perform(multipart("/api/incidents/1/attachments").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = {"CITIZEN"})
    @DisplayName("Petición autenticada con imagen JPEG válida debe retornar 201 Created y metadatos del adjunto")
    void shouldUploadValidAttachmentSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bache.jpg",
                "image/jpeg",
                "contenido-imagen".getBytes()
        );

        AttachmentResponse expectedResponse = AttachmentResponse.builder()
                .id(10L)
                .incidentId(1L)
                .fileName("bache.jpg")
                .fileUrl("/uploads/uuid-bache.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(16L)
                .uploadedAt(OffsetDateTime.now())
                .build();

        when(attachmentService.uploadAttachment(eq(1L), any())).thenReturn(expectedResponse);

        mockMvc.perform(multipart("/api/incidents/1/attachments").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.incidentId").value(1))
                .andExpect(jsonPath("$.fileName").value("bache.jpg"))
                .andExpect(jsonPath("$.fileUrl").value("/uploads/uuid-bache.jpg"))
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = {"CITIZEN"})
    @DisplayName("Petición con formato inválido debe retornar 400 Bad Request")
    void shouldReturnBadRequestWhenFormatIsInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "fake-pdf".getBytes()
        );

        when(attachmentService.uploadAttachment(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no permitido"));

        mockMvc.perform(multipart("/api/incidents/1/attachments").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "citizen@urbanpulse.es", roles = {"CITIZEN"})
    @DisplayName("Petición con archivo de tamaño excesivo debe retornar 400 Bad Request")
    void shouldReturnBadRequestWhenFileIsOversized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "muy_pesada.jpg",
                "image/jpeg",
                new byte[100]
        );

        when(attachmentService.uploadAttachment(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo supera el tamaño máximo permitido de 5MB."));

        mockMvc.perform(multipart("/api/incidents/1/attachments").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "operator@urbanpulse.es", roles = {"OPERATOR"})
    @DisplayName("Consulta de adjuntos de incidencia debe retornar 200 OK con la lista de adjuntos")
    void shouldGetAttachmentsSuccessfully() throws Exception {
        AttachmentResponse att = AttachmentResponse.builder()
                .id(1L)
                .incidentId(1L)
                .fileName("evidencia.png")
                .fileUrl("/uploads/uuid-evidencia.png")
                .mimeType("image/png")
                .fileSizeBytes(2048L)
                .uploadedAt(OffsetDateTime.now())
                .build();

        when(attachmentService.getAttachmentsByIncidentId(1L)).thenReturn(List.of(att));

        mockMvc.perform(get("/api/incidents/1/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fileName").value("evidencia.png"));
    }

    @Test
    @DisplayName("La ruta de recursos estáticos /uploads/** debe ser pública y no requerir autenticación")
    void shouldAllowPublicAccessToUploads() throws Exception {
        mockMvc.perform(get("/uploads/test.jpg"))
                .andExpect(status().is(not(401)))
                .andExpect(status().is(not(403)));
    }
}
