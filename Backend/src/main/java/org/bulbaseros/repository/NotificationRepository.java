package org.bulbaseros.repository;

import org.bulbaseros.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para la persistencia, consulta y gestión de notificaciones ({@link Notification}).
 * <p>
 * Proporciona métodos derivados de consulta optimizados mediante índices para la recuperación
 * cronológica descendente de notificaciones por usuario, el conteo rápido de avisos pendientes de lectura
 * y la búsqueda segura validando la titularidad del usuario destinatario.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Recupera todas las notificaciones destinadas a un usuario específico, ordenadas de forma cronológica descendente.
     * <p>
     * Se apoya en el índice {@code idx_notifications_user_id} para garantizar una recuperación eficiente en la bandeja de entrada.
     * </p>
     *
     * @param userId Identificador único del usuario destinatario.
     * @return Lista cronológica descendente de notificaciones del usuario.
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Contabiliza el número total de notificaciones no leídas ({@code isRead = false}) asociadas a un usuario.
     * <p>
     * Utilizado para alimentar el badge numérico reactivo del componente de campana en la barra de navegación.
     * Se apoya en la combinación de índices sobre {@code user_id} e {@code is_read}.
     * </p>
     *
     * @param userId Identificador único del usuario destinatario.
     * @return Conteo de notificaciones pendientes de lectura.
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Busca una notificación concreta por su identificador único asegurando que pertenezca al usuario especificado.
     * <p>
     * Previene ataques de referencia directa insegura a objetos (IDOR) al validar simultáneamente el id y el propietario.
     * </p>
     *
     * @param id     Identificador único de la notificación.
     * @param userId Identificador único del usuario destinatario.
     * @return {@link Optional} conteniendo la notificación si existe y pertenece al usuario, o vacío en caso contrario.
     */
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    /**
     * Busca una notificacion a partir de su identificador publico universal (UUID v4).
     *
     * @param publicId Identificador publico de la notificacion.
     * @return {@link Optional} con la notificacion o vacio.
     */
    Optional<Notification> findByPublicId(UUID publicId);

    /**
     * Busca una notificacion por su identificador publico asegurando que pertenezca al usuario especificado.
     *
     * @param publicId Identificador publico de la notificacion.
     * @param userId   Identificador interno del usuario.
     * @return {@link Optional} con la notificacion si coincide o vacio.
     */
    Optional<Notification> findByPublicIdAndUserId(UUID publicId, Long userId);
}
