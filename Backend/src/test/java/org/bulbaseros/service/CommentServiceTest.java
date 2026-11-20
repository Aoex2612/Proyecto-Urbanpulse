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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link CommentService} con Mockito.
 * <p>
 * Verifica la segregación de notas internas frente a comentarios públicos (RF-20, RF-21),
 * el control de acceso estricto según rol, el bloqueo en expedientes cerrados o rechazados (RF-24)
 * y la edición de comentarios con sellado de fecha por autor o administrador (RF-22, RF-23).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private ComentariosIncidenciasRepository comentariosRepository;

    @Mock
    private IncidenciaRepository incidenciaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    private Incidencia incident;
    private Usuario citizenCreator;
    private Usuario otherCitizen;
    private Usuario operator;
    private Usuario technician;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        citizenCreator = new Usuario("autor.ciudadano@urbanpulse.es", "hash", "Carlos Creador", RolUsuario.CITIZEN);
        citizenCreator.setId(1L);

        otherCitizen = new Usuario("otro.ciudadano@urbanpulse.es", "hash", "Marta Vecina", RolUsuario.CITIZEN);
        otherCitizen.setId(2L);

        operator = new Usuario("operador@urbanpulse.es", "hash", "Elena Operadora", RolUsuario.OPERATOR);
        operator.setId(10L);

        technician = new Usuario("tecnico@urbanpulse.es", "hash", "Mario Técnico", RolUsuario.TECHNICIAN);
        technician.setId(20L);

        admin = new Usuario("admin@urbanpulse.es", "hash", "Laura Administradora", RolUsuario.ADMIN);
        admin.setId(30L);

        incident = new Incidencia("Semáforo averiado", "Luces apagadas", "Tráfico", 36.72, -4.42, citizenCreator);
        incident.setId(100L);
        incident.setStatus(EstadoIncidencia.ASSIGNED);
    }

    @Test
    @DisplayName("addComment debe registrar nota técnica interna con rol OPERATOR exitosamente")
    void shouldAddInternalNoteSuccessfullyByOperator() {
        CreateCommentRequest request = CreateCommentRequest.builder()
                .comment("Requiere reemplazo de fusible central")
                .isInternal(true)
                .build();

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        ComentariosIncidencias commentEntity = new ComentariosIncidencias(incident, operator, request.getComment(), true);
        commentEntity.setId(50L);
        when(commentMapper.toEntity(incident, operator, request.getComment(), true)).thenReturn(commentEntity);
        when(comentariosRepository.save(commentEntity)).thenReturn(commentEntity);

        CommentResponse responseDto = CommentResponse.builder()
                .id(50L)
                .comment(request.getComment())
                .isInternal(true)
                .authorId(10L)
                .authorName("Elena Operadora")
                .build();
        when(commentMapper.toDto(commentEntity)).thenReturn(responseDto);

        CommentResponse result = commentService.addComment(100L, request, "operador@urbanpulse.es");

        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertTrue(result.getIsInternal());
        verify(comentariosRepository).save(commentEntity);
    }

    @Test
    @DisplayName("addComment debe registrar aviso público con rol TECHNICIAN exitosamente")
    void shouldAddPublicCommentSuccessfullyByTechnician() {
        CreateCommentRequest request = CreateCommentRequest.builder()
                .comment("Se ha señalizado la zona y se procederá a la reparación")
                .isInternal(false)
                .build();

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));

        ComentariosIncidencias commentEntity = new ComentariosIncidencias(incident, technician, request.getComment(), false);
        when(commentMapper.toEntity(incident, technician, request.getComment(), false)).thenReturn(commentEntity);
        when(comentariosRepository.save(commentEntity)).thenReturn(commentEntity);

        CommentResponse responseDto = CommentResponse.builder()
                .id(51L)
                .comment(request.getComment())
                .isInternal(false)
                .build();
        when(commentMapper.toDto(commentEntity)).thenReturn(responseDto);

        CommentResponse result = commentService.addComment(100L, request, "tecnico@urbanpulse.es");

        assertNotNull(result);
        assertFalse(result.getIsInternal());
        verify(comentariosRepository).save(commentEntity);
    }

    @Test
    @DisplayName("addComment debe lanzar IllegalArgumentException si la incidencia está en estado CLOSED (RF-24)")
    void shouldThrowExceptionWhenAddingCommentToClosedIncident() {
        incident.setStatus(EstadoIncidencia.CLOSED);
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        CreateCommentRequest request = CreateCommentRequest.builder()
                .comment("Comentario tardío")
                .isInternal(false)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commentService.addComment(100L, request, "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("No se pueden añadir comentarios"));
    }

    @Test
    @DisplayName("addComment debe lanzar IllegalArgumentException si la incidencia está en estado REJECTED (RF-24)")
    void shouldThrowExceptionWhenAddingCommentToRejectedIncident() {
        incident.setStatus(EstadoIncidencia.REJECTED);
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        CreateCommentRequest request = CreateCommentRequest.builder()
                .comment("Comentario en rechazada")
                .isInternal(true)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commentService.addComment(100L, request, "operador@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("No se pueden añadir comentarios"));
    }

    @Test
    @DisplayName("addComment debe lanzar 403 Forbidden si un usuario con rol CITIZEN intenta publicar (RF-20)")
    void shouldThrowForbiddenWhenCitizenAttemptsToComment() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("autor.ciudadano@urbanpulse.es")).thenReturn(Optional.of(citizenCreator));

        CreateCommentRequest request = CreateCommentRequest.builder()
                .comment("Intento ciudadano de comentar")
                .isInternal(false)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> commentService.addComment(100L, request, "autor.ciudadano@urbanpulse.es"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("no tienen permisos"));
    }

    @Test
    @DisplayName("getComments debe devolver únicamente comentarios públicos al ciudadano creador (RF-21)")
    void shouldReturnOnlyPublicCommentsToCitizenCreator() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("autor.ciudadano@urbanpulse.es")).thenReturn(Optional.of(citizenCreator));

        ComentariosIncidencias publicComment = new ComentariosIncidencias(incident, operator, "Aviso público al vecino", false);
        when(comentariosRepository.findByIncidentIdAndIsInternalFalseOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(publicComment));

        CommentResponse dto = CommentResponse.builder().id(1L).comment("Aviso público al vecino").isInternal(false).build();
        when(commentMapper.toDtoList(List.of(publicComment))).thenReturn(List.of(dto));

        List<CommentResponse> comments = commentService.getComments(100L, "autor.ciudadano@urbanpulse.es");

        assertNotNull(comments);
        assertEquals(1, comments.size());
        assertFalse(comments.get(0).getIsInternal());
        verify(comentariosRepository, never()).findByIncidentIdOrderByCreatedAtAsc(any());
        verify(comentariosRepository).findByIncidentIdAndIsInternalFalseOrderByCreatedAtAsc(100L);
    }

    @Test
    @DisplayName("getComments debe lanzar 403 Forbidden si un ciudadano intenta ver comentarios de una incidencia que no es suya")
    void shouldThrowForbiddenWhenCitizenRequestsAnotherUsersIncidentComments() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("otro.ciudadano@urbanpulse.es")).thenReturn(Optional.of(otherCitizen));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> commentService.getComments(100L, "otro.ciudadano@urbanpulse.es"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("solo puede consultar comentarios de sus propias incidencias"));
    }

    @Test
    @DisplayName("getComments debe devolver la totalidad de comentarios (internos y públicos) al rol OPERATOR")
    void shouldReturnAllCommentsToOperator() {
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator));

        ComentariosIncidencias publicComment = new ComentariosIncidencias(incident, operator, "Aviso público", false);
        ComentariosIncidencias internalComment = new ComentariosIncidencias(incident, technician, "Nota interna", true);

        when(comentariosRepository.findByIncidentIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(publicComment, internalComment));

        CommentResponse dto1 = CommentResponse.builder().id(1L).isInternal(false).build();
        CommentResponse dto2 = CommentResponse.builder().id(2L).isInternal(true).build();
        when(commentMapper.toDtoList(List.of(publicComment, internalComment))).thenReturn(List.of(dto1, dto2));

        List<CommentResponse> comments = commentService.getComments(100L, "operador@urbanpulse.es");

        assertNotNull(comments);
        assertEquals(2, comments.size());
        verify(comentariosRepository).findByIncidentIdOrderByCreatedAtAsc(100L);
    }

    @Test
    @DisplayName("updateComment debe permitir al autor original modificar el texto y registrar updatedAt (RF-22)")
    void shouldUpdateCommentSuccessfullyByOriginalAuthor() {
        ComentariosIncidencias existingComment = new ComentariosIncidencias(incident, technician, "Texto preliminar", true);
        existingComment.setId(42L);

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(comentariosRepository.findById(42L)).thenReturn(Optional.of(existingComment));
        when(userRepository.findByEmail("tecnico@urbanpulse.es")).thenReturn(Optional.of(technician));
        when(comentariosRepository.save(existingComment)).thenReturn(existingComment);

        CommentResponse dto = CommentResponse.builder()
                .id(42L)
                .comment("Texto corregido y ampliado")
                .updatedAt(OffsetDateTime.now())
                .build();
        when(commentMapper.toDto(existingComment)).thenReturn(dto);

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .comment("Texto corregido y ampliado")
                .build();

        CommentResponse updated = commentService.updateComment(100L, 42L, request, "tecnico@urbanpulse.es");

        assertNotNull(updated);
        assertEquals("Texto corregido y ampliado", existingComment.getComment());
        assertNotNull(existingComment.getUpdatedAt());
        verify(comentariosRepository).save(existingComment);
    }

    @Test
    @DisplayName("updateComment debe permitir a un ADMIN editar el comentario de otro autor (RF-22)")
    void shouldAllowAdminToUpdateCommentByAnotherAuthor() {
        ComentariosIncidencias existingComment = new ComentariosIncidencias(incident, technician, "Texto original", true);
        existingComment.setId(42L);

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(comentariosRepository.findById(42L)).thenReturn(Optional.of(existingComment));
        when(userRepository.findByEmail("admin@urbanpulse.es")).thenReturn(Optional.of(admin));
        when(comentariosRepository.save(existingComment)).thenReturn(existingComment);

        CommentResponse dto = CommentResponse.builder()
                .id(42L)
                .comment("Texto rectificado por administración")
                .build();
        when(commentMapper.toDto(existingComment)).thenReturn(dto);

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .comment("Texto rectificado por administración")
                .build();

        CommentResponse updated = commentService.updateComment(100L, 42L, request, "admin@urbanpulse.es");

        assertNotNull(updated);
        assertEquals("Texto rectificado por administración", existingComment.getComment());
        verify(comentariosRepository).save(existingComment);
    }

    @Test
    @DisplayName("updateComment debe lanzar 403 Forbidden si un tercero no admin intenta editar el comentario (RF-23)")
    void shouldThrowForbiddenWhenUnauthorizedUserTriesToEditComment() {
        ComentariosIncidencias existingComment = new ComentariosIncidencias(incident, technician, "Nota de técnico", true);
        existingComment.setId(42L);

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(comentariosRepository.findById(42L)).thenReturn(Optional.of(existingComment));
        when(userRepository.findByEmail("operador@urbanpulse.es")).thenReturn(Optional.of(operator)); // Operador != autor ni admin

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .comment("Intento no autorizado")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> commentService.updateComment(100L, 42L, request, "operador@urbanpulse.es"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Solo el autor original o un administrador"));
    }

    @Test
    @DisplayName("updateComment debe lanzar IllegalArgumentException si la incidencia está en estado CLOSED (RF-24)")
    void shouldThrowExceptionWhenEditingCommentOnClosedIncident() {
        incident.setStatus(EstadoIncidencia.CLOSED);
        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .comment("Intento en cerrada")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commentService.updateComment(100L, 42L, request, "admin@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("No se pueden editar comentarios"));
    }

    @Test
    @DisplayName("updateComment debe lanzar IllegalArgumentException si el comentario no pertenece a la incidencia")
    void shouldThrowExceptionWhenCommentBelongsToDifferentIncident() {
        Incidencia anotherIncident = new Incidencia("Otra", "Otro", "Cat", 36.0, -4.0, citizenCreator);
        anotherIncident.setId(999L);

        ComentariosIncidencias existingComment = new ComentariosIncidencias(anotherIncident, technician, "Nota ajena", true);
        existingComment.setId(42L);

        when(incidenciaRepository.findById(100L)).thenReturn(Optional.of(incident));
        when(comentariosRepository.findById(42L)).thenReturn(Optional.of(existingComment));

        UpdateCommentRequest request = UpdateCommentRequest.builder()
                .comment("Texto de prueba")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commentService.updateComment(100L, 42L, request, "tecnico@urbanpulse.es"));
        assertTrue(ex.getMessage().contains("no pertenece a la incidencia"));
    }
}
