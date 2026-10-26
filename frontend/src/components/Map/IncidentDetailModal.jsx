/**
 * @fileoverview Modal de detalle exhaustivo de incidencia urbana para UrbanPulse.
 * Despliega la información completa de la incidencia seleccionada (título, categoría,
 * descripción, estado, prioridad, coordenadas y fecha de reporte) y, si el usuario en sesión
 * dispone de rol administrativo (ADMIN), consulta e integra de forma condicional la línea
 * temporal de auditoría (StatusTimeline) con las transiciones históricas de estado.
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import authService from '../../services/authService';
import incidentService from '../../services/incidentService';
import {
  getStatusLabel,
  getPriorityLabel,
  canViewAuditHistory,
} from './mapConstants';
import { formatDateTime } from '../../utils/dateUtils';
import StatusTimeline from '../Timeline/StatusTimeline';
import './Map.css';

/**
 * Componente modal accesible para la visualización detallada de incidencias y su trazabilidad.
 *
 * @param {Object} props - Propiedades del componente modal.
 * @param {Object|null} props.incident - Objeto con los datos detallados de la incidencia.
 * @param {boolean} [props.isOpen=true] - Bandera para controlar la apertura o cierre del modal.
 * @param {Function} [props.onClose] - Callback ejecutado al solicitar el cierre del modal.
 * @param {Object|null} [props.currentUser=null] - Usuario autenticado opcional (por defecto consulta authService).
 * @returns {import('react').JSX.Element|null} Elemento JSX del modal de detalle o null si está cerrado.
 */
export default function IncidentDetailModal({
  incident,
  isOpen = true,
  onClose,
  currentUser = null,
}) {
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState(null);

  const closeButtonRef = useRef(null);

  // Obtener el usuario activo en sesión (prop o fallback de servicio de autenticación)
  const effectiveUser =
    currentUser ||
    (typeof authService.getCurrentUser === 'function'
      ? authService.getCurrentUser()
      : authService.getUser());
  const isAuditAllowed = canViewAuditHistory(effectiveUser?.role);

  // Cierre memorizado para su invocación en eventos de teclado y clics
  const handleDismiss = useCallback(() => {
    if (onClose) {
      onClose();
    }
  }, [onClose]);

  // Manejo de pulsación en el fondo oscuro exterior para descartar el modal
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      handleDismiss();
    }
  };

  // Cierre mediante tecla Escape y autofoco accesible en botón de cierre
  useEffect(() => {
    if (isOpen && incident) {
      const timer = setTimeout(() => {
        closeButtonRef.current?.focus();
      }, 50);

      /**
       * Listener de teclado para capturar la tecla Escape.
       *
       * @param {KeyboardEvent} e - Evento nativo de teclado.
       * @returns {void}
       */
      const handleKeyDown = (e) => {
        if (e.key === 'Escape') {
          handleDismiss();
        }
      };

      window.addEventListener('keydown', handleKeyDown);

      return () => {
        clearTimeout(timer);
        window.removeEventListener('keydown', handleKeyDown);
      };
    }
  }, [isOpen, incident, handleDismiss]);

  // Consulta asíncrona del historial de auditoría restringida a usuarios con rol autorizado
  useEffect(() => {
    if (!isOpen || !incident?.id || !isAuditAllowed) {
      return;
    }

    let isMounted = true;

    Promise.resolve().then(() => {
      if (!isMounted) return;
      setLoadingHistory(true);
      setHistoryError(null);

      incidentService
        .getIncidentHistory(incident.id)
        .then((data) => {
          if (isMounted) {
            setHistory(Array.isArray(data) ? data : []);
            setLoadingHistory(false);
          }
        })
        .catch((err) => {
          if (isMounted) {
            const errorMsg =
              err?.response?.data?.message ||
              'No se pudo recuperar el registro cronológico de auditoría.';
            setHistoryError(errorMsg);
            setLoadingHistory(false);
          }
        });
    });

    return () => {
      isMounted = false;
    };
  }, [isOpen, incident?.id, isAuditAllowed]);

  // Si el modal está cerrado o no hay datos de incidencia, no renderizar nada
  if (!isOpen || !incident) {
    return null;
  }

  const latNumber = Number(incident.latitude);
  const lngNumber = Number(incident.longitude);
  const formattedLatitude = !Number.isNaN(latNumber)
    ? latNumber.toFixed(6)
    : 'No disponible';
  const formattedLongitude = !Number.isNaN(lngNumber)
    ? lngNumber.toFixed(6)
    : 'No disponible';

  const statusClass = incident.status
    ? `status-${incident.status.toLowerCase()}`
    : 'status-reported';

  const priorityClass = incident.priority
    ? `priority-${incident.priority.toLowerCase()}`
    : 'priority-medium';

  const reporterName =
    incident.citizenName || incident.citizenEmail || 'Ciudadano de UrbanPulse';

  return (
    <div
      className="modal-backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        className="modal-card incident-detail-modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="incident-detail-modal-title"
      >
        {/* Cabecera del Modal */}
        <header className="modal-header">
          <div className="modal-header-text">
            <span className="modal-eyebrow">
              Incidencia #{incident.id ?? '---'}
            </span>
            <h2 id="incident-detail-modal-title" className="modal-title">
              Detalle de Incidencia
            </h2>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            className="modal-close-btn"
            onClick={handleDismiss}
            aria-label="Cerrar modal de detalle"
          >
            &times;
          </button>
        </header>

        {/* Cuerpo del Modal con Información y Auditoría */}
        <div className="modal-body incident-detail-body">
          {/* Título y Badges Principales */}
          <section className="incident-detail-header-card">
            <h3 className="incident-detail-title">{incident.title}</h3>

            <div className="detail-badges-row">
              <span className={`status-badge ${statusClass}`}>
                {getStatusLabel(incident.status)}
              </span>

              <span className={`priority-badge ${priorityClass}`}>
                Prioridad: {getPriorityLabel(incident.priority)}
              </span>

              {incident.category && (
                <span className="category-badge">{incident.category}</span>
              )}
            </div>
          </section>

          {/* Rejilla de Metadatos de la Incidencia */}
          <section
            className="detail-meta-grid"
            aria-label="Metadatos de la incidencia"
          >
            <div className="detail-meta-item">
              <span className="detail-meta-label">Fecha de Reporte</span>
              <span className="detail-meta-value">
                {formatDateTime(incident.createdAt)}
              </span>
            </div>

            <div className="detail-meta-item">
              <span className="detail-meta-label">Reportado por</span>
              <span className="detail-meta-value">{reporterName}</span>
            </div>

            <div className="detail-meta-item detail-meta-span-2">
              <span className="detail-meta-label">Coordenadas Geográficas</span>
              <span className="detail-meta-value coordinates-font">
                Lat: {formattedLatitude} | Long: {formattedLongitude}
              </span>
            </div>
          </section>

          {/* Descripción Completa */}
          <section className="incident-detail-section">
            <h4 className="detail-section-heading">Descripción</h4>
            <p className="detail-description-text">
              {incident.description || 'Sin descripción detallada proporcionada.'}
            </p>
          </section>

          {/* Sección de Auditoría Transversal (Visualización Condicional por Rol) */}
          {isAuditAllowed && (
            <section
              className="incident-detail-audit-section"
              aria-labelledby="audit-section-heading"
            >
              <div className="audit-section-header">
                <h4 id="audit-section-heading" className="detail-section-heading">
                  <span className="audit-header-icon" aria-hidden="true">
                    📋
                  </span>
                  Historial de Auditoría
                </h4>
                <span className="audit-role-tag">Vista Administrador</span>
              </div>

              <StatusTimeline
                history={history}
                loading={loadingHistory}
                error={historyError}
              />
            </section>
          )}
        </div>

        {/* Pie del Modal */}
        <footer className="modal-footer">
          <button
            type="button"
            className="btn-secondary"
            onClick={handleDismiss}
          >
            Cerrar
          </button>
        </footer>
      </div>
    </div>
  );
}
