/**
 * @fileoverview Componente campana de notificaciones in-app para UrbanPulse.
 * Muestra el contador de avisos pendientes de lectura en un badge reactivo,
 * abre un menú desplegable con la lista cronológica de notificaciones y
 * permite marcar cada una como leída individualmente y navegar a la ficha.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import notificationService from '../../services/notificationService';
import './NotificationBell.css';

/**
 * Componente interactivo de campana de notificaciones con badge, dropdown y marcado individual.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Function} [props.onIncidentClick] - Callback disparado al hacer clic en una notificación con el id de la incidencia.
 * @returns {import('react').JSX.Element} Elemento JSX de la campana de notificaciones.
 */
export default function NotificationBell({ onIncidentClick }) {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef(null);
  const POLL_INTERVAL_MS = 60000;

  /**
   * Carga la lista cronológica de notificaciones del usuario autenticado.
   */
  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const data = await notificationService.getNotifications();
      setNotifications(Array.isArray(data) ? data : []);
    } catch {
      // Errores de red silenciados controladamente
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Actualiza el contador numérico de notificaciones sin leer.
   */
  const fetchUnreadCount = useCallback(async () => {
    try {
      const count = await notificationService.getUnreadCount();
      setUnreadCount(typeof count === 'number' ? count : 0);
    } catch {
      // Silenciar error en sondeo
    }
  }, []);

  // Polling periódico para actualizar el badge de avisos
  useEffect(() => {
    let isMounted = true;
    Promise.resolve().then(() => {
      if (isMounted) {
        fetchUnreadCount();
      }
    });

    const interval = setInterval(() => {
      if (isMounted) {
        fetchUnreadCount();
      }
    }, POLL_INTERVAL_MS);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [fetchUnreadCount]);

  // Recupera las notificaciones completas cuando se despliega el menú
  useEffect(() => {
    let isMounted = true;
    if (isOpen) {
      Promise.resolve().then(() => {
        if (isMounted) {
          fetchNotifications();
        }
      });
    }
    return () => {
      isMounted = false;
    };
  }, [isOpen, fetchNotifications]);

  // Cierre automático al pulsar fuera del menú desplegable
  useEffect(() => {
    /**
     * Cierra el desplegable si el clic ocurrió fuera de la referencia.
     * @param {MouseEvent} event Evento de clic en documento.
     */
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  /**
   * Marca una notificación individual como leída en backend y actualiza el estado local.
   *
   * @param {number} notificationId Identificador de la notificación.
   */
  const handleMarkAsRead = async (notificationId) => {
    try {
      await notificationService.markAsRead(notificationId);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notificationId ? { ...n, isRead: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // Control de fallo silencioso
    }
  };

  /**
   * Gestiona el clic sobre el título de una notificación para abrir la ficha correspondiente.
   *
   * @param {Object} notification Objeto con los datos del aviso.
   */
  const handleNotificationClick = (notification) => {
    if (onIncidentClick && notification.incidentId) {
      onIncidentClick(notification.incidentId);
      setIsOpen(false);
    }
  };

  /**
   * Formatea la fecha ISO a un formato legible en español.
   *
   * @param {string} isoString Fecha en formato ISO 8601.
   * @returns {string} Fecha formateada.
   */
  const formatDate = (isoString) => {
    if (!isoString) return '';
    return new Date(isoString).toLocaleString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="notification-bell-wrapper" ref={dropdownRef}>
      <button
        type="button"
        className="notification-bell-btn"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-label={`Notificaciones${unreadCount > 0 ? `, ${unreadCount} sin leer` : ''}`}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
      >
        <span className="notification-bell-icon" aria-hidden="true">🔔</span>
        {unreadCount > 0 && (
          <span className="notification-badge" aria-live="polite">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="notification-dropdown" role="listbox" aria-label="Notificaciones">
          <div className="notification-dropdown-header">
            <h3 className="notification-dropdown-title">Notificaciones</h3>
          </div>

          <div className="notification-list">
            {loading && (
              <p className="notification-loading">Cargando avisos...</p>
            )}
            {!loading && notifications.length === 0 && (
              <p className="notification-empty">No hay notificaciones pendientes.</p>
            )}
            {!loading && notifications.map((notification) => (
              <div
                key={notification.id}
                className={`notification-item${notification.isRead ? '' : ' notification-item--unread'}`}
                role="option"
                aria-selected={!notification.isRead}
              >
                <div className="notification-item-header">
                  {!notification.isRead && (
                    <span className="notification-unread-dot" aria-hidden="true" />
                  )}
                  <button
                    type="button"
                    className="notification-item-title"
                    onClick={() => handleNotificationClick(notification)}
                  >
                    {notification.title}
                  </button>
                </div>
                <p className="notification-item-message">{notification.message}</p>
                <div className="notification-item-footer">
                  <span className="notification-item-date">{formatDate(notification.createdAt)}</span>
                  {!notification.isRead && (
                    <button
                      type="button"
                      className="notification-mark-read-btn"
                      onClick={() => handleMarkAsRead(notification.id)}
                    >
                      Marcar como leída
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
