package org.bulbaseros.service;

import org.bulbaseros.dto.AttachmentResponse;
import org.bulbaseros.entity.ArchivoAdjunto;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.mappers.AttachmentMapper;
import org.bulbaseros.repository.ArchivoAdjuntoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link AttachmentService}.
 * <p>
 * Verifica la validación de archivos multimedia (MIME y tamaño), almacenamiento físico,
 * gestión de persistencia de metadatos y tratamiento de errores.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private ArchivoAdjuntoRepository archivoAdjuntoRepository;

    @Spy
    private AttachmentMapper attachmentMapper = new AttachmentMapper();

    @InjectMocks
    private AttachmentService attachmentService;

    @TempDir
    Path tempUploadDir;

    private Incidencia testIncident;

    @BeforeEach
    void setUp() {
        testIncident = new Incidencia();
        testIncident.setId(100L);
        testIncident.setTitle("Farola rota");

        ReflectionTestUtils.setField(attachmentService, "uploadDir", tempUploadDir.toString());
    }

    @Test
    @DisplayName("Debería subir y almacenar exitosamente una imagen JPEG válida")
    void shouldUploadJpegSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "foto_farola.jpg",
                "image/jpeg",
                "contenido-de-prueba-imagen".getBytes()
        );

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(testIncident));
        when(archivoAdjuntoRepository.save(any(ArchivoAdjunto.class))).thenAnswer(invocation -> {
            ArchivoAdjunto a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

        AttachmentResponse response = attachmentService.uploadAttachment(100L, file);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getIncidentId());
        assertEquals("foto_farola.jpg", response.getFileName());
        assertEquals("image/jpeg", response.getMimeType());
        assertTrue(response.getFileUrl().startsWith("/uploads/"));

        ArgumentCaptor<ArchivoAdjunto> captor = ArgumentCaptor.forClass(ArchivoAdjunto.class);
        verify(archivoAdjuntoRepository).save(captor.capture());
        ArchivoAdjunto savedEntity = captor.getValue();
        assertEquals(testIncident, savedEntity.getIncident());
        assertEquals("foto_farola.jpg", savedEntity.getFileName());

        // Comprobar que el archivo se guardó físicamente en tempUploadDir
        File[] storedFiles = tempUploadDir.toFile().listFiles();
        assertNotNull(storedFiles);
        assertEquals(1, storedFiles.length);
    }

    @Test
    @DisplayName("Debería subir y almacenar exitosamente una imagen PNG válida")
    void shouldUploadPngSuccessfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "captura.png",
                "image/png",
                "bytes-png".getBytes()
        );

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(testIncident));
        when(archivoAdjuntoRepository.save(any(ArchivoAdjunto.class))).thenAnswer(invocation -> {
            ArchivoAdjunto a = invocation.getArgument(0);
            a.setId(2L);
            return a;
        });

        AttachmentResponse response = attachmentService.uploadAttachment(100L, file);

        assertNotNull(response);
        assertEquals("captura.png", response.getFileName());
        assertEquals("image/png", response.getMimeType());
        assertTrue(response.getFileUrl().endsWith(".png"));
    }

    @Test
    @DisplayName("Debería lanzar error 400 Bad Request si el archivo está vacío o es nulo")
    void shouldRejectEmptyOrNullFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vacio.jpg",
                "image/jpeg",
                new byte[0]
        );

        ResponseStatusException ex1 = assertThrows(
                ResponseStatusException.class,
                () -> attachmentService.uploadAttachment(100L, emptyFile)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(
                ResponseStatusException.class,
                () -> attachmentService.uploadAttachment(100L, null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
    }

    @Test
    @DisplayName("Debería lanzar error 400 Bad Request si el tipo MIME no es JPEG ni PNG")
    void shouldRejectUnsupportedMimeType() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "fake-pdf".getBytes()
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> attachmentService.uploadAttachment(100L, pdfFile)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Solo se aceptan imágenes JPEG y PNG"));
    }

    @Test
    @DisplayName("Debería lanzar error 400 Bad Request si el archivo supera el límite de 5MB")
    void shouldRejectOversizedFile() {
        byte[] oversizedBytes = new byte[(int) AttachmentService.MAX_FILE_SIZE_BYTES + 10];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "pesada.jpg",
                "image/jpeg",
                oversizedBytes
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> attachmentService.uploadAttachment(100L, largeFile)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("5MB"));
    }

    @Test
    @DisplayName("Debería lanzar IncidentNotFoundException si la incidencia no existe")
    void shouldThrowWhenIncidentNotFound() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "foto.jpg",
                "image/jpeg",
                "bytes".getBytes()
        );

        when(incidenciaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IncidentNotFoundException.class, () -> attachmentService.uploadAttachment(999L, file));
    }

    @Test
    @DisplayName("Debería rechazar nombres de archivo con path traversal (..)")
    void shouldRejectPathTraversal() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../etc/passwd",
                "image/jpeg",
                "bytes".getBytes()
        );

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(testIncident));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> attachmentService.uploadAttachment(100L, file)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debería recuperar los archivos adjuntos de una incidencia existente")
    void shouldGetAttachmentsByIncidentId() {
        when(incidenciaRepository.existsById(100L)).thenReturn(true);

        ArchivoAdjunto a1 = ArchivoAdjunto.builder()
                .id(1L)
                .incident(testIncident)
                .fileName("foto1.jpg")
                .fileUrl("/uploads/foto1.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(5000L)
                .build();

        when(archivoAdjuntoRepository.findByIncidentIdOrderByUploadedAtAsc(100L))
                .thenReturn(List.of(a1));

        List<AttachmentResponse> result = attachmentService.getAttachmentsByIncidentId(100L);

        assertEquals(1, result.size());
        assertEquals("foto1.jpg", result.get(0).getFileName());
    }

    @Test
    @DisplayName("Debería lanzar IncidentNotFoundException al consultar adjuntos de una incidencia inexistente")
    void shouldThrowWhenGettingAttachmentsOfNonExistentIncident() {
        when(incidenciaRepository.existsById(888L)).thenReturn(false);

        assertThrows(IncidentNotFoundException.class, () -> attachmentService.getAttachmentsByIncidentId(888L));
    }
}
