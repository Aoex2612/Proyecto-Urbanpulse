package org.bulbaseros.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bulbaseros.entity.ArchivoAdjunto;

import java.time.OffsetDateTime;

/**
 * Objeto de transferencia de datos (DTO) que representa la información de un archivo multimedia adjunto a una incidencia.
 * <p>
 * Contiene los metadatos del archivo persistido en el servidor: identificador, identificador de la incidencia asociada,
 * nombre original del archivo, URL estática relativa de acceso, tipo MIME, tamaño en bytes y marca temporal de subida.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentResponse {

    /**
     * Identificador único del archivo adjunto en la base de datos.
     */
    private Long id;

    /**
     * Identificador único de la incidencia a la cual pertenece este archivo adjunto.
     */
    private Long incidentId;

    /**
     * Nombre original del archivo subido por el usuario.
     */
    private String fileName;

    /**
     * Ruta URL relativa o pública para acceder al recurso multimedia estático.
     */
    private String fileUrl;

    /**
     * Tipo MIME del archivo multimedia (ej. image/jpeg, image/png).
     */
    private String mimeType;

    /**
     * Tamaño físico del archivo expresado en bytes.
     */
    private Long fileSizeBytes;

    /**
     * Marca temporal en que fue subido y almacenado el archivo.
     */
    private OffsetDateTime uploadedAt;
}
