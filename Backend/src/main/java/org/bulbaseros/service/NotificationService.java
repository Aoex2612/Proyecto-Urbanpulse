package org.bulbaseros.service;

import org.bulbaseros.dto.NotificationResponse;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Notification;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.mappers.NotificationMapper;
import org.bulbaseros.repository.NotificationRepository;
import org.bulbaseros.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Servicio de lógica de negocio transaccional para la gestión integral de notificaciones in-app.
 * <p>
 * Proporciona operaciones para persistir nuevas notificaciones del ciclo de vida de incidencias,
 * recuperar el historial cronológico descendente de avisos de un usuario autenticado, contabilizar
 * los avisos pendientes de lectura y marcar notificaciones individuales como leídas asegurando
 * aislamiento estricto de propiedad para mitigar vulnerabilidades de acceso cruzado (IDOR).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    /**
     * Construye una nueva instancia de {@link NotificationService} con sus dependencias inyectadas por constructor.
     *
     * @param notificationRepository Repositorio Spring Data JPA para la persistencia de notificaciones.
     * @param userRepository         Repositorio Spring Data JPA para la consulta de usuarios.
     * @param notificationMapper     Mapeador de conversión entre entidades {@link Notification} y DTOs {@link NotificationResponse}.
     */
    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationMapper = notificationMapper;
    }

    /**
     * Persiste una nueva entidad de notificación asociada a un usuario e incidencia urbana determinada.
     * <p>
     * Se ejecuta dentro de una transacción de escritura para asegurar persistencia atómica en base de datos.
     * </p>
     *
     * @param user     Usuario destinatario del aviso informativo.
     * @param incident Incidencia urbana asociada al evento o aviso.
     * @param type     Tipo funcional o categoría del aviso (ej. STATUS_CHANGE, DUPLICATE_SUGGESTION).
     * @param title    Título conciso de la notificación.
     * @param message  Cuerpo textual descriptivo del aviso.
     * @return Entidad {@link Notification} persistida con su identificador generado.
     */
    @Transactional
    public Notification createNotification(Usuario user, Incidencia incident, String type, String title, String message) {
        Notification notification = notificationMapper.toEntity(user, incident, type, title, message);
        return notificationRepository.save(notification);
    }

    /**
     * Recupera la lista cronológica descendente de notificaciones asociadas al usuario especificado por su email.
     *
     * @param userEmail Dirección de correo electrónico del usuario autenticado.
     * @return Lista de DTOs {@link NotificationResponse} con los avisos del usuario.
     * @throws ResponseStatusException con código HTTP 404 Not Found si el usuario no existe en el sistema.
     */
    public List<NotificationResponse> getUserNotifications(String userEmail) {
        Usuario user = findUserByEmailOrThrow(userEmail);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return notificationMapper.toDtoList(notifications);
    }

    /**
     * Contabiliza el número total de notificaciones no leídas correspondientes al usuario especificado por su email.
     * <p>
     * Utilizado para alimentar en tiempo real el badge numérico reactivo del componente de campana.
     * </p>
     *
     * @param userEmail Dirección de correo electrónico del usuario autenticado.
     * @return Cantidad de notificaciones pendientes de lectura ({@code isRead = false}).
     * @throws ResponseStatusException con código HTTP 404 Not Found si el usuario no existe en el sistema.
     */
    public long getUnreadCount(String userEmail) {
        Usuario user = findUserByEmailOrThrow(userEmail);
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    /**
     * Marca una notificación individual como leída asegurando que pertenezca al usuario especificado.
     * <p>
     * Valida de manera estricta la titularidad de la notificación mediante {@code findByIdAndUserId}
     * para impedir la manipulación indebida de avisos de otros usuarios.
     * </p>
     *
     * @param notificationId Identificador único de la notificación a marcar como leída.
     * @param userEmail      Dirección de correo electrónico del usuario autenticado solicitante.
     * @return DTO {@link NotificationResponse} actualizado con el estado de lectura en {@code true}.
     * @throws ResponseStatusException con código HTTP 404 Not Found si el usuario no existe, o si la
     *                                 notificación no existe o no pertenece al usuario autenticado.
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String userEmail) {
        Usuario user = findUserByEmailOrThrow(userEmail);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada o no pertenece al usuario"));

        notification.setIsRead(true);
        Notification savedNotification = notificationRepository.save(notification);
        return notificationMapper.toDto(savedNotification);
    }

    /**
     * Método auxiliar privado para buscar un usuario por su email o lanzar una excepción 404 Not Found.
     *
     * @param userEmail Dirección de correo electrónico del usuario a buscar.
     * @return Entidad {@link Usuario} correspondiente al correo proporcionado.
     * @throws ResponseStatusException con código HTTP 404 Not Found si no existe ningún usuario con dicho correo.
     */
    private Usuario findUserByEmailOrThrow(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
