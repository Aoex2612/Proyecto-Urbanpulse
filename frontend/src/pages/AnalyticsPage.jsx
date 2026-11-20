/**
 * @fileoverview Página principal del Panel de Analítica Urbana y Reportes Ejecutivos (RF-1 al RF-18).
 * Integra indicadores ejecutivos (KPIs), controles de filtrado temporal y territorial, visualización gráfica
 * en SVG nativo (distribución territorial y cumplimiento de SLA por departamento), tabla paginada de registros
 * y exportación oficial en PDF bajo directrices del Ayuntamiento de Málaga.
 */

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import analyticsService from '../services/analyticsService';
import authService from '../services/authService';
import {
  AnalyticsKpiCards,
  AnalyticsDateFilter,
  TerritorialChart,
  SlaComplianceChart,
  EmptyAnalyticsState,
  ExportPdfButton,
} from '../components/analytics';
import { formatDate } from '../utils/dateUtils';
import { formatFilterDescription } from '../utils/reportUtils';
import './AnalyticsPage.css';

/**
 * Filtros predeterminados al ingresar al panel analítico.
 */
const DEFAULT_FILTERS = Object.freeze({
  preset: 'LAST_30_DAYS',
  year: new Date().getFullYear(),
  district: '',
  neighborhood: '',
});

/**
 * Cantidad de incidencias mostradas por página en la tabla de detalle.
 */
const ITEMS_PER_PAGE = 10;

/**
 * Devuelve la clase CSS de prioridad para la insignia en la tabla.
 *
 * @param {string} priority - Nivel de prioridad de la incidencia.
 * @returns {string} Clase modificadora.
 */
const getPriorityBadgeClass = (priority) => {
  switch (String(priority).toUpperCase()) {
    case 'CRITICAL':
      return 'priority-badge-critical';
    case 'HIGH':
      return 'priority-badge-high';
    case 'MEDIUM':
      return 'priority-badge-medium';
    case 'LOW':
      return 'priority-badge-low';
    default:
      return 'priority-badge-default';
  }
};

/**
 * Devuelve la clase CSS de estado de triaje para la insignia en la tabla.
 *
 * @param {string} status - Estado de la incidencia.
 * @returns {string} Clase modificadora.
 */
const getStatusBadgeClass = (status) => {
  switch (String(status).toUpperCase()) {
    case 'RESOLVED':
    case 'CLOSED':
      return 'status-badge-resolved';
    case 'IN_PROGRESS':
    case 'ASSIGNED':
      return 'status-badge-progress';
    case 'VALIDATED':
      return 'status-badge-validated';
    case 'REJECTED':
      return 'status-badge-rejected';
    case 'REPORTED':
    default:
      return 'status-badge-reported';
  }
};

/**
 * Componente de página principal para el Panel de Analítica Urbana.
 *
 * @returns {import('react').JSX.Element} Vista del panel analítico completo.
 */
export default function AnalyticsPage() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [reloadTrigger, setReloadTrigger] = useState(0);
  const [summaryData, setSummaryData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [currentUser] = useState(() => authService.getUser());

  // Carga reactiva de datos analíticos cuando cambian los filtros o se solicita reintento
  useEffect(() => {
    let ignore = false;

    // Preparar parámetros limpios
    const cleanParams = {};
    if (filters.preset) {
      cleanParams.preset = filters.preset;
      if (filters.preset === 'SPECIFIC_YEAR' && filters.year) {
        cleanParams.year = filters.year;
      }
    }
    if (filters.district) {
      cleanParams.district = filters.district;
    }
    if (filters.neighborhood) {
      cleanParams.neighborhood = filters.neighborhood;
    }

    analyticsService
      .getAnalyticsSummary(cleanParams)
      .then((data) => {
        if (!ignore) {
          setSummaryData(data);
          setCurrentPage(1);
          setLoading(false);
          setError(null);
        }
      })
      .catch((err) => {
        if (!ignore) {
          const msg =
            err?.response?.data?.message ||
            'Error al conectar con el servicio de analítica urbana. Comprueba tu conexión o permisos de acceso.';
          setError(msg);
          setLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [filters, reloadTrigger]);

  const handleFilterChange = (newFilters) => {
    setLoading(true);
    setError(null);
    setFilters(newFilters);
  };

  const handleResetFilters = () => {
    setLoading(true);
    setError(null);
    setFilters(DEFAULT_FILTERS);
  };

  const handleRetry = () => {
    setLoading(true);
    setError(null);
    setReloadTrigger((prev) => prev + 1);
  };

  const filterSummary = formatFilterDescription(filters);
  const incidents = Array.isArray(summaryData?.filteredIncidents)
    ? summaryData.filteredIncidents
    : [];

  const totalIncidents = summaryData?.operationalMetrics?.totalIncidents ?? incidents.length;
  const isEmpty = !loading && !error && totalIncidents === 0 && incidents.length === 0;

  // Paginación de la tabla de incidencias filtradas
  const totalPages = Math.max(1, Math.ceil(incidents.length / ITEMS_PER_PAGE));
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const paginatedIncidents = incidents.slice(startIndex, startIndex + ITEMS_PER_PAGE);

  return (
    <main className="analytics-page-container" aria-label="Panel de Analítica Urbana y Reportes">
      {/* 1. Cabecera institucional */}
      <header className="analytics-header">
        <div className="analytics-brand-block">
          <div className="analytics-brand-row">
            <span className="analytics-badge-institutional">Panel Ejecutivo</span>
            <span className="analytics-period-badge" aria-label="Periodo analizado">
              {filterSummary.periodText}
            </span>
          </div>
          <h1 className="analytics-title">Analítica Urbana y Rendimiento Operativo</h1>
          <p className="analytics-subtitle">
            Ayuntamiento de Málaga · Plataforma Central de Gestión e Inteligencia Urbana
          </p>
        </div>

        <div className="analytics-header-actions">
          <Link to="/" className="analytics-nav-link" title="Volver a la vista principal">
            <svg
              viewBox="0 0 24 24"
              className="analytics-nav-icon"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            Inicio
          </Link>

          <Link
            to="/operator-panel"
            className="analytics-nav-link"
            title="Ir al Centro de Triaje y Operaciones"
          >
            Centro de Triaje
          </Link>

          <ExportPdfButton
            summaryData={summaryData}
            currentUser={currentUser}
            disabled={loading || Boolean(error) || isEmpty}
          />
        </div>
      </header>

      {/* 2. Barra de filtros temporal y territorial */}
      <AnalyticsDateFilter
        filters={filters}
        onFilterChange={handleFilterChange}
        onReset={handleResetFilters}
        disabled={loading}
      />

      {/* 3. Estado de Carga (Spinner / Skeleton accesible) */}
      {loading && (
        <div
          className="analytics-loading-state"
          role="status"
          aria-live="polite"
          data-testid="analytics-loading"
        >
          <div className="analytics-spinner" aria-hidden="true" />
          <p className="analytics-loading-text">
            Compilando indicadores analíticos y métricas operacionales...
          </p>
        </div>
      )}

      {/* 4. Estado de Error con botón de reintento */}
      {!loading && error && (
        <section
          className="analytics-error-state"
          role="alert"
          aria-live="assertive"
          data-testid="analytics-error"
        >
          <div className="analytics-error-icon" aria-hidden="true">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
          </div>
          <div className="analytics-error-details">
            <h3 className="analytics-error-title">No se pudieron cargar los datos analíticos</h3>
            <p className="analytics-error-message">{error}</p>
          </div>
          <button
            type="button"
            className="analytics-retry-button"
            onClick={handleRetry}
          >
            Reintentar consulta
          </button>
        </section>
      )}

      {/* 5. Estado Vacío (RF-11) */}
      {isEmpty && (
        <EmptyAnalyticsState onResetFilters={handleResetFilters} />
      )}

      {/* 6. Vista principal de datos (KPIs, Gráficos y Tabla de incidencias) */}
      {!loading && !error && !isEmpty && summaryData && (
        <div className="analytics-content-layout" data-testid="analytics-content">
          {/* Bloque 1: Tarjetas ejecutivas de KPIs */}
          <AnalyticsKpiCards summaryData={summaryData} />

          {/* Bloque 2: Gráficos visuales en SVG nativo */}
          <section className="analytics-charts-grid" aria-label="Visualizaciones gráficas">
            <TerritorialChart
              data={summaryData.territorialMetrics}
              title="Distribución Territorial de Incidencias"
            />
            <SlaComplianceChart
              data={summaryData.departmentMetrics}
              title="Cumplimiento de SLA por Departamento"
            />
          </section>

          {/* Bloque 3: Tabla detallada de incidencias filtradas */}
          <section
            className="analytics-table-section"
            aria-label="Listado detallado de incidencias filtradas"
          >
            <header className="analytics-table-header">
              <div className="table-header-info">
                <h2 className="table-title">Incidencias Filtradas</h2>
                <span className="table-count-badge">
                  {incidents.length} {incidents.length === 1 ? 'registro' : 'registros'}
                </span>
              </div>
              <span className="table-filter-note">
                {filterSummary.districtText} · {filterSummary.neighborhoodText}
              </span>
            </header>

            <div className="analytics-table-container">
              <table className="analytics-table" data-testid="analytics-incidents-table">
                <thead>
                  <tr>
                    <th scope="col" className="col-id">Ref.</th>
                    <th scope="col" className="col-title">Título de Incidencia</th>
                    <th scope="col" className="col-territory">Distrito / Barrio</th>
                    <th scope="col" className="col-category">Categoría</th>
                    <th scope="col" className="col-priority">Prioridad</th>
                    <th scope="col" className="col-status">Estado</th>
                    <th scope="col" className="col-date">Fecha de Registro</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedIncidents.map((incident) => (
                    <tr key={incident.id} className="analytics-table-row">
                      <td className="col-id font-mono">#{incident.id}</td>
                      <td className="col-title">
                        <span className="incident-title-text" title={incident.title}>
                          {incident.title}
                        </span>
                      </td>
                      <td className="col-territory">
                        <span className="territory-district">
                          {incident.district || 'Sin distrito'}
                        </span>
                        {incident.neighborhood && (
                          <span className="territory-neighborhood"> · {incident.neighborhood}</span>
                        )}
                      </td>
                      <td className="col-category">
                        <span className="category-tag">{incident.category || 'General'}</span>
                      </td>
                      <td className="col-priority">
                        <span
                          className={`priority-badge ${getPriorityBadgeClass(incident.priority)}`}
                        >
                          {incident.priority || 'MEDIUM'}
                        </span>
                      </td>
                      <td className="col-status">
                        <span className={`status-badge ${getStatusBadgeClass(incident.status)}`}>
                          {incident.status || 'REPORTED'}
                        </span>
                      </td>
                      <td className="col-date">
                        {incident.createdAt ? formatDate(incident.createdAt, true) : 'N/A'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Paginación */}
            {totalPages > 1 && (
              <footer className="analytics-pagination" aria-label="Navegación de páginas">
                <button
                  type="button"
                  className="pagination-btn"
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  disabled={currentPage === 1}
                  aria-label="Página anterior"
                >
                  &larr; Anterior
                </button>

                <span className="pagination-info" aria-current="page">
                  Página <strong>{currentPage}</strong> de <strong>{totalPages}</strong>
                </span>

                <button
                  type="button"
                  className="pagination-btn"
                  onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                  disabled={currentPage === totalPages}
                  aria-label="Página siguiente"
                >
                  Siguiente &rarr;
                </button>
              </footer>
            )}
          </section>
        </div>
      )}
    </main>
  );
}
