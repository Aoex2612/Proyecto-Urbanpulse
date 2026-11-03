/**
 * @fileoverview Componente modular y desacoplado para la visualización del historial de cambios de estado (Timeline de Auditoría).
 * Presenta de forma secuencial y cronológica cada transición sufrida por una incidencia, reflejando
 * el estado de origen, el estado resultante, el usuario responsable de la acción, la marca temporal
 * y el motivo justificado registrado para dicha transición.
 */

import { getStatusLabel } from '../Map/mapConstants';
import { formatDateTime } from '../../utils/dateUtils';
import './StatusTimeline.css';

/**
 * Componente modular de línea temporal de auditoría de transiciones de estado.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Array<Object>} [props.history=[]] - Lista cronológica de transiciones de estado registradas.
 * @param {boolean} [props.loading=false] - Indica si la consulta asíncrona del historial se encuentra en progreso.
 * @param {string|null} [props.error=null] - Mensaje de error retornado si la consulta del historial falla.
 * @param {string} [props.className=''] - Clases CSS adicionales para el contenedor principal.
 * @returns {import('react').JSX.Element} Vista del timeline de auditoría de estados.
 */
export default function StatusTimeline({
  history = [],
  loading = false,
  error = null,
  className = '',
}) {
  // Estado 1: Consulta de auditoría en curso
  if (loading) {
    return (
      <div
        className={`status-timeline-container ${className}`.trim()}
        role="status"
        aria-live="polite"
      >
        <div className="status-timeline-loading">
          <div className="timeline-spinner" aria-hidden="true" />
          <span>Cargando historial de cambios de estado...</span>
        </div>
      </div>
    );
  }

  // Estado 2: Error durante la recuperación del historial
  if (error) {
    return (
      <div
        className={`status-timeline-container ${className}`.trim()}
        role="alert"
      >
        <div className="status-timeline-error">
          <span className="timeline-error-icon" aria-hidden="true">
            ⚠️
          </span>
          <div className="timeline-error-content">
            <h5 className="timeline-error-title">Error al consultar auditoría</h5>
            <p className="timeline-error-message">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  // Estado 3: Historial vacío (sin transiciones registradas tras el reporte inicial)
  if (!history || history.length === 0) {
    return (
      <div
        className={`status-timeline-container ${className}`.trim()}
        role="status"
      >
        <div className="status-timeline-empty">
          <div className="timeline-empty-icon" aria-hidden="true">
            <svg
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="16" x2="12" y2="12" />
              <line x1="12" y1="8" x2="12.01" y2="8" />
            </svg>
          </div>
          <div className="timeline-empty-content">
            <h5 className="timeline-empty-title">Sin cambios de estado posteriores</h5>
            <p className="timeline-empty-description">
              Esta incidencia permanece en su estado inicial de reporte (
              <strong>Reportada</strong>) sin transiciones operativas registradas.
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Estado 4: Renderizado de la lista cronológica de transiciones
  return (
    <div className={`status-timeline-container ${className}`.trim()}>
      <ol
        className="status-timeline-list"
        aria-label="Línea temporal de cambios de estado"
      >
        {history.map((item, index) => {
          const fromClass = item.fromStatus
            ? `status-${item.fromStatus.toLowerCase()}`
            : 'status-reported';
          const toClass = item.toStatus
            ? `status-${item.toStatus.toLowerCase()}`
            : 'status-reported';

          const actorDisplayName =
            item.changedByUserName ||
            item.changedByUserEmail ||
            'Usuario del sistema';

          return (
            <li
              key={item.id ?? `transition-${index}`}
              className="status-timeline-item"
            >
              <div className="timeline-marker" aria-hidden="true" />
              <article className="timeline-card">
                <header className="timeline-card-header">
                  <div className="timeline-transition">
                    <span className={`status-badge ${fromClass}`}>
                      {getStatusLabel(item.fromStatus)}
                    </span>
                    <span className="timeline-arrow" aria-hidden="true">
                      &rarr;
                    </span>
                    <span className={`status-badge ${toClass}`}>
                      {getStatusLabel(item.toStatus)}
                    </span>
                  </div>
                  <time
                    className="timeline-time"
                    dateTime={item.createdAt || undefined}
                  >
                    {formatDateTime(item.createdAt)}
                  </time>
                </header>

                <div className="timeline-card-body">
                  <div className="timeline-actor">
                    <span className="timeline-actor-icon" aria-hidden="true">
                      👤
                    </span>
                    <span>
                      Modificado por:{' '}
                      <strong className="timeline-actor-name">
                        {actorDisplayName}
                      </strong>
                    </span>
                  </div>

                  {item.reason ? (
                    <p className="timeline-reason">
                      <span className="timeline-reason-label">Motivo:</span>
                      {item.reason}
                    </p>
                  ) : (
                    <p className="timeline-reason empty">
                      Sin motivo justificado especificado.
                    </p>
                  )}
                </div>
              </article>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
