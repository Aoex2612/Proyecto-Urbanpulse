package org.bulbaseros.service;

import org.bulbaseros.dto.AttachmentResponse;
import org.bulbaseros.entity.ArchivoAdjunto;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.mappers.AttachmentMapper;
import org.bulbaseros.repository.ArchivoAdjuntoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de lógica de negocio para la gestión, almacenamiento y validación de archivos multimedia adjuntos a incidencias.
 * <p>
 * Valida formatos permitidos (JPEG y PNG), restricciones de tamaño máximo (5MB),
 * almacena físicamente los ficheros en el sistema de archivos del servidor y registra
 * los metadatos correspondientes en la base de datos de manera transaccional.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class AttachmentService {

    /**
     * Conjunto de tipos MIME permitidos para la carga de adjuntos multimedia.
     */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/jpg");

    /**
     * Tamaño máximo permitido por archivo adjunto (5 Megabytes = 5.242.880 bytes).
     */
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final IncidenciaRepository incidenciaRepository;
    private final ArchivoAdjuntoRepository archivoAdjuntoRepository;
    private final AttachmentMapper attachmentMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Constructor con inyección de dependencias para el servicio de archivos adjuntos.
     *
     * @param incidenciaRepository    Repositorio para consultar y verificar incidencias.
     * @param archivoAdjuntoRepository Repositorio para persistir metadatos de archivos adjuntos.
     * @param attachmentMapper        Mapeador para convertir entidades {@link ArchivoAdjunto} a DTOs.
     */
    public AttachmentService(
            IncidenciaRepository incidenciaRepository,
            ArchivoAdjuntoRepository archivoAdjuntoRepository,
            AttachmentMapper attachmentMapper
    ) {
        this.incidenciaRepository = incidenciaRepository;
        this.archivoAdjuntoRepository = archivoAdjuntoRepository;
        this.attachmentMapper = attachmentMapper;
    }

    /**
     * Almacena un archivo multimedia en disco y registra sus metadatos vinculados a una incidencia.
     *
     * @param incidentId Identificador único de la incidencia receptora del adjunto.
     * @param file       Archivo multipart recibido en la petición HTTP.
     * @return DTO {@link AttachmentResponse} con los datos del archivo almacenado.
     * @throws IncidentNotFoundException Si la incidencia indicada no existe.
     * @throws ResponseStatusException  Si el archivo está vacío, excede el tamaño máximo o posee un formato no permitido.
     */
    @Transactional
    public AttachmentResponse uploadAttachment(Long incidentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo adjunto no puede estar vacío.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Formato de archivo no permitido. Solo se aceptan imágenes JPEG y PNG."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El archivo supera el tamaño máximo permitido de 5MB."
            );
        }

        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        String originalFilename = file.getOriginalFilename() != null ? StringUtils.cleanPath(file.getOriginalFilename()) : "attachment.jpg";
        if (originalFilename.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del archivo contiene caracteres inválidos.");
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        } else {
            extension = contentType.equalsIgnoreCase("image/png") ? ".png" : ".jpg";
        }

        String storedFileName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetLocation = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al guardar el archivo multimedia en el almacenamiento del servidor.",
                    e
            );
        }

        String fileUrl = "/uploads/" + storedFileName;

        ArchivoAdjunto attachment = ArchivoAdjunto.builder()
                .incident(incident)
                .fileName(originalFilename)
                .fileUrl(fileUrl)
                .mimeType(contentType)
                .fileSizeBytes(file.getSize())
                .uploadedAt(OffsetDateTime.now())
                .build();

        ArchivoAdjunto saved = archivoAdjuntoRepository.save(attachment);
        return attachmentMapper.toDto(saved);
    }

    /**
     * Recupera todos los archivos adjuntos asociados a una incidencia urbana.
     *
     * @param incidentId Identificador único de la incidencia a consultar.
     * @return Lista de {@link AttachmentResponse} asociados a la incidencia en orden cronológico.
     * @throws IncidentNotFoundException Si la incidencia indicada no existe.
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByIncidentId(Long incidentId) {
        if (!incidenciaRepository.existsById(incidentId)) {
            throw new IncidentNotFoundException(incidentId);
        }

        List<ArchivoAdjunto> attachments = archivoAdjuntoRepository.findByIncidentIdOrderByUploadedAtAsc(incidentId);
        return attachmentMapper.toDtoList(attachments);
    }
}
