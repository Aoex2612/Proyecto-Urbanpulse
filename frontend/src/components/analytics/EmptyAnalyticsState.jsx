/**
 * @fileoverview Componente visual para estados vacíos de analítica urbana (RF-11).
 * Ofrece un mensaje claro, amigable e informativo cuando no se encuentran incidencias
 * registradas en el período temporal o demarcación territorial consultada, orientando al usuario
 * a ampliar el rango o restablecer los filtros aplicados.
 */

import './EmptyAnalyticsState.css';

/**
 * Componente de estado vacío cuando no existen incidencias en los filtros activos.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {string} [props.title='No se encontraron incidencias en el período seleccionado'] - Encabezado explicativo.
 * @param {string} [props.message] - Mensaje descriptivo con recomendaciones de acción.
 * @param {Function} [props.onResetFilters] - Callback opcional para restablecer los filtros de búsqueda.
 * @returns {import('react').JSX.Element} Vista amigable de estado vacío con acción orientadora.
 */
export default function EmptyAnalyticsState({
  title = 'No se encontraron incidencias en el período seleccionado',
  message = 'No constan incidencias registradas que coincidan con los criterios temporales o territoriales vigentes. Puedes ampliar el rango temporal o seleccionar otra demarcación territorial para visualizar las métricas.',
  onResetFilters = null,
}) {
  return (
    <section
      className="empty-analytics-container"
      data-testid="empty-analytics-state"
      role="region"
      aria-label="Sin resultados de analítica urbana"
    >
      <div className="empty-analytics-card">
        <div className="empty-analytics-icon-wrapper" aria-hidden="true">
          <svg
            viewBox="0 0 64 64"
            className="empty-analytics-illustration"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            {/* Hoja / Informe base */}
            <path d="M16 8h24l12 12v36H16a4 4 0 0 1-4-4V12a4 4 0 0 1 4-4z" />
            <polyline points="40 8 40 20 52 20" />
            {/* Líneas sutiles de informe */}
            <line x1="22" y1="28" x2="34" y2="28" />
            <line x1="22" y1="36" x2="42" y2="36" />
            {/* Lupa con destello suave */}
            <circle cx="38" cy="42" r="10" />
            <line x1="45" y1="49" x2="54" y2="58" />
          </svg>
        </div>

        <h3 className="empty-analytics-title">{title}</h3>
        <p className="empty-analytics-message">{message}</p>

        {typeof onResetFilters === 'function' && (
          <div className="empty-analytics-actions">
            <button
              type="button"
              className="empty-analytics-btn-reset"
              onClick={onResetFilters}
              aria-label="Restablecer filtros de búsqueda"
            >
              <svg
                viewBox="0 0 24 24"
                className="btn-icon"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <polyline points="1 4 1 10 7 10" />
                <polyline points="23 20 23 14 17 14" />
                <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15" />
              </svg>
              Restablecer filtros
            </button>
          </div>
        )}
      </div>
    </section>
  );
}

