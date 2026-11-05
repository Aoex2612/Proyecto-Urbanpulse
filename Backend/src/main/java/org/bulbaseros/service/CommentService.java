package org.bulbaseros.service;

import org.bulbaseros.dto.CommentResponse;
import org.bulbaseros.dto.CreateCommentRequest;
import org.bulbaseros.dto.UpdateCommentRequest;
import org.bulbaseros.entity.ComentariosIncidencias;
import org.bulbaseros.entity.EstadoIncidencia;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.mappers.CommentMapper;
import org.bulbaseros.repository.ComentariosIncidenciasRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Servicio de negocio para la gestión del muro de comentarios y notas técnicas de incidencias.
 * <p>
 * Implementa la autorización granular por roles para la segregación de notas internas frente a comentarios públicos (RF-20, RF-21),
 * el bloqueo estricto de interacciones sobre expedientes cerrados o rechazados (RF-24) y el control de autoría o privilegios
 * de administración en la edición de comentarios preexistentes (RF-22, RF-23).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class CommentService {

    private final ComentariosIncidenciasRepository comentariosRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    /**
     * Construye una nueva instancia de {@link CommentService} inyectando las dependencias necesarias.
     *
     * @param comentariosRepository Repositorio de persistencia de comentarios.
     * @param incidenciaRepository   Repositorio de consulta y actualización de incidencias.
     * @param userRepository         Repositorio de usuarios del sistema.
     * @param commentMapper          Mapeador entre entidades {@link ComentariosIncidencias} y DTOs {@link CommentResponse}.
     */
    public CommentService(ComentariosIncidenciasRepository comentariosRepository,
                          IncidenciaRepository incidenciaRepository,
                          UserRepository userRepository,
                          CommentMapper commentMapper) {
        this.comentariosRepository = comentariosRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.userRepository = userRepository;
        this.commentMapper = commentMapper;
    }

    /**
     * Da de alta una nueva nota técnica interna o aviso público asociado a una incidencia activa.
     * <p>
     * Valida que la incidencia no se encuentre en un estado terminal ({@code CLOSED} o {@code REJECTED})
     * y que el usuario autenticado posea privilegios operativos ({@code OPERATOR}, {@code TECHNICIAN} o {@code ADMIN}).
     * Los usuarios con rol {@code CITIZEN} tienen denegada la publicación de comentarios en el triaje.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param request    DTO con el contenido del comentario y el indicador de visibilidad interna.
     * @param userEmail  Dirección de correo electrónico del usuario autor autenticado.
     * @return DTO {@link CommentResponse} representativo del comentario recién persistido.
     * @throws IncidentNotFoundException si la incidencia especificada no existe.
     * @throws IllegalArgumentException  si la incidencia está cerrada o rechazada, o los datos de solicitud son inválidos.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario es un ciudadano o carece de permisos de publicación.
     */
    @Transactional
    public CommentResponse addComment(Long incidentId, CreateCommentRequest request, String userEmail) {
        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        if (incident.getStatus() == EstadoIncidencia.CLOSED || incident.getStatus() == EstadoIncidencia.REJECTED) {
            throw new IllegalArgumentException("No se pueden añadir comentarios a una incidencia en estado " + incident.getStatus());
        }

        Usuario user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado: " + userEmail));

        if (user.getRole() == RolUsuario.CITIZEN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los ciudadanos no tienen permisos para publicar comentarios en el triaje");
        }

        if (user.getRole() != RolUsuario.OPERATOR
                && user.getRole() != RolUsuario.TECHNICIAN
                && user.getRole() != RolUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario no dispone de permisos para publicar comentarios");
        }

        if (request == null || request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido del comentario es obligatorio");
        }

        if (request.getIsInternal() == null) {
            throw new IllegalArgumentException("El indicador de visibilidad interna es obligatorio");
        }

        ComentariosIncidencias comment = commentMapper.toEntity(
                incident,
                user,
                request.getComment().trim(),
                request.getIsInternal()
        );
        comment.setCreatedAt(OffsetDateTime.now());

        ComentariosIncidencias savedComment = comentariosRepository.save(comment);
        return commentMapper.toDto(savedComment);
    }

    /**
     * Recupera el listado cronológico de comentarios asociados a una incidencia según los permisos del usuario.
     * <p>
     * Si el usuario autenticado posee rol {@link RolUsuario#CITIZEN}, se verifica que sea el autor original del reporte;
     * de ser así, se devuelven exclusivamente los comentarios públicos ({@code is_internal = false}). Si no es el autor,
     * se deniega el acceso con 403 Forbidden. Para personal municipal ({@code OPERATOR}, {@code TECHNICIAN}, {@code ADMIN}),
     * se recupera la totalidad de notas internas y comentarios públicos ordenados cronológicamente.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param userEmail  Dirección de correo electrónico del usuario autenticado solicitante.
     * @return Lista de comentarios transformados a {@link CommentResponse}.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalArgumentException  si el usuario autenticado no existe en la base de datos.
     * @throws ResponseStatusException   con código 403 Forbidden si un ciudadano intenta consultar una incidencia ajena
     *                                   o el rol carece de autorización.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long incidentId, String userEmail) {
        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        Usuario user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado: " + userEmail));

        if (user.getRole() == RolUsuario.CITIZEN) {
            if (incident.getCitizen() == null || !incident.getCitizen().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un ciudadano solo puede consultar comentarios de sus propias incidencias");
            }
            List<ComentariosIncidencias> publicComments =
                    comentariosRepository.findByIncidentIdAndIsInternalFalseOrderByCreatedAtAsc(incidentId);
            return commentMapper.toDtoList(publicComments);
        }

        if (user.getRole() == RolUsuario.OPERATOR
                || user.getRole() == RolUsuario.TECHNICIAN
                || user.getRole() == RolUsuario.ADMIN) {
            List<ComentariosIncidencias> allComments =
                    comentariosRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
            return commentMapper.toDtoList(allComments);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rol no autorizado para visualizar comentarios");
    }

    /**
     * Actualiza el contenido textual de un comentario existente.
     * <p>
     * Valida que la incidencia asociada no se encuentre en estado {@code CLOSED} ni {@code REJECTED} (RF-24),
     * que el comentario exista y pertenezca a la incidencia, y que el usuario autenticado sea el autor original
     * del comentario o un usuario con rol {@link RolUsuario#ADMIN} (RF-22, RF-23). Registra la marca temporal
     * {@code updatedAt} al momento de persistir la modificación.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param commentId  Identificador único del comentario a editar.
     * @param request    DTO con el nuevo texto del comentario.
     * @param userEmail  Dirección de correo electrónico del usuario que solicita la edición.
     * @return DTO {@link CommentResponse} con los datos actualizados del comentario.
     * @throws IncidentNotFoundException si la incidencia no existe.
     * @throws IllegalArgumentException  si el expediente está finalizado, el comentario no existe o no pertenece a la incidencia.
     * @throws ResponseStatusException   con código 403 Forbidden si el usuario no es el autor original ni administrador.
     */
    @Transactional
    public CommentResponse updateComment(Long incidentId, Long commentId, UpdateCommentRequest request, String userEmail) {
        Incidencia incident = incidenciaRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        if (incident.getStatus() == EstadoIncidencia.CLOSED || incident.getStatus() == EstadoIncidencia.REJECTED) {
            throw new IllegalArgumentException("No se pueden editar comentarios en una incidencia en estado " + incident.getStatus());
        }

        ComentariosIncidencias comment = comentariosRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado con id: " + commentId));

        if (comment.getIncident() == null || !comment.getIncident().getId().equals(incidentId)) {
            throw new IllegalArgumentException("El comentario indicado no pertenece a la incidencia");
        }

        Usuario user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado: " + userEmail));

        boolean isAuthor = comment.getAuthor() != null && comment.getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == RolUsuario.ADMIN;

        if (!isAuthor && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el autor original o un administrador pueden editar este comentario");
        }

        if (request == null || request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido del comentario es obligatorio");
        }

        comment.setComment(request.getComment().trim());
        comment.setUpdatedAt(OffsetDateTime.now());

        ComentariosIncidencias updatedComment = comentariosRepository.save(comment);
        return commentMapper.toDto(updatedComment);
    }
}
