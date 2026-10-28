/**
 * @fileoverview Vista de seguimiento "Mis Incidencias" (/my-incidents) para ciudadanos en UrbanPulse.
 * Permite al ciudadano autenticado consultar de forma cronológica, paginada y filtrable por estado
 * sus incidencias reportadas en el término municipal de Málaga, visualizar fechas clave (creación
 * y última actualización), dirección postal, distrito municipal y abrir el modal de detalle completo.
 */

import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import incidentService from '../services/incidentService';
import { IncidentDetailModal } from '../components/Map';
import {
  getStatusLabel,
  INCIDENT_STATUSES,
} from '../components/Map/mapConstants';
import { formatDate } from '../utils/dateUtils';
import './MyIncidentsPage.css';

/**
 * Cantidad fija de incidencias por página conforme a los requisitos de diseño.
 * @type {number}
 */
const PAGE_SIZE = 10;

/**
 * Componente principal de la página "Mis Incidencias".
 *
 * @returns {import('react').JSX.Element} Vista accesible de seguimiento y auditoría ciudadana.
 */
export default function MyIncidentsPage() {
  const navigate = useNavigate();
  const hasOperatorAccess = authService.hasAnyRole(['OPERATOR', 'ADMIN']);

  // Estado de datos paginados
  const [incidents, setIncidents] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Estado de filtrado por estado (predeterminado "TODOS")
  const [statusFilter, setStatusFilter] = useState('TODOS');

  // Estados de control de carga y errores
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Estado de inspección detallada de una incidencia seleccionada
  const [selectedIncident, setSelectedIncident] = useState(null);

  /**
   * Carga desde el backend las incidencias registradas por el usuario actual.
   *
   * @param {number} targetPage - Índice de página a consultar.
   * @param {string} targetStatus - Filtro de estado seleccionado.
   * @returns {Promise<void>}
   */
  const fetchMyIncidents = useCallback(async (targetPage, targetStatus) => {
    setLoading(true);
    setError(null);

    try {
      const response = await incidentService.getMyIncidents({
        page: targetPage,
        size: PAGE_SIZE,
        status: targetStatus,
      });

      const content = Array.isArray(response?.content) ? response.content : [];
      setIncidents(content);
      setTotalPages(typeof response?.totalPages === 'number' ? response.totalPages : 1);
      setTotalElements(typeof response?.totalElements === 'number' ? response.totalElements : content.length);
    } catch (err) {
      const errorMsg =
        err?.response?.data?.message ||
        'No se pudieron cargar tus incidencias. Por favor, inténtalo de nuevo.';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  }, []);

  // Consulta al montar el componente y al cambiar de página o filtro
  useEffect(() => {
    let isMounted = true;

    Promise.resolve().then(() => {
      if (isMounted) {
        fetchMyIncidents(page, statusFilter);
      }
    });

    return () => {
      isMounted = false;
    };
  }, [fetchMyIncidents, page, statusFilter]);

  /**
   * Manejador del cambio de filtro por estado.
   * Actualiza el filtro seleccionado y resetea la página a 0.
   *
   * @param {import('react').ChangeEvent<HTMLSelectElement>} event - Evento de cambio del selector.
   * @returns {void}
   */
  const handleStatusChange = (event) => {
    const newStatus = event.target.value;
    setStatusFilter(newStatus);
    setPage(0);
  };

  /**
   * Manejador de navegación a la página anterior.
   *
   * @returns {void}
   */
  const handlePreviousPage = () => {
    setPage((prevPage) => Math.max(0, prevPage - 1));
  };

  /**
   * Manejador de navegación a la página siguiente.
   *
   * @returns {void}
   */
  const handleNextPage = () => {
    setPage((prevPage) => prevPage + 1);
  };

  /**
   * Cierra la sesión activa del usuario y redirige al login.
   *
   * @returns {void}
   */
  const handleLogout = () => {
    authService.logout();
    navigate('/login', { replace: true });
  };

  /**
   * Abre el modal de detalle para la incidencia especificada.
   *
   * @param {Object} incident - Incidencia a inspeccionar.
   * @returns {void}
   */
  const handleOpenDetail = (incident) => {
    setSelectedIncident(incident);
  };

  /**
   * Cierra el modal de detalle.
   *
   * @returns {void}
   */
  const handleCloseDetail = () => {
    setSelectedIncident(null);
  };

  const isPreviousDisabled = page === 0 || loading;
  const isNextDisabled =
    page >= (totalPages > 0 ? totalPages - 1 : 0) ||
    incidents.length === 0 ||
    loading;

  const isFiltered = statusFilter !== 'TODOS';
  const showEmptyState = !loading && !error && incidents.length === 0;

  return (
    <div className="my-incidents-container">
      {/* Cabecera de Navegación Principal */}
      <header className="my-incidents-header">
        <div className="my-incidents-brand">
          <h1 className="brand-title">UrbanPulse</h1>
          <span className="brand-badge">Mis Incidencias</span>
        </div>
        <nav className="my-incidents-nav-actions" aria-label="Navegación principal">
          <Link
            to="/"
            className="my-incidents-nav-link"
            title="Volver al Portal Ciudadano"
          >
            Portal Ciudadano
          </Link>
          <Link
            to="/map"
            className="my-incidents-map-nav-link"
            title="Ir al mapa cartográfico interactivo"
          >
            Ver Mapa de Incidencias
          </Link>
          {hasOperatorAccess && (
            <Link
              to="/operator-panel"
              className="operator-nav-link"
              title="Ir al panel de triaje técnico"
            >
              Panel de Operador
            </Link>
          )}
          <button
            type="button"
            onClick={handleLogout}
            className="logout-button"
            aria-label="Cerrar sesión"
          >
            Cerrar sesión
          </button>
        </nav>
      </header>

      {/* Contenido Principal */}
      <main className="my-incidents-main">
        {/* Sección de Encabezado y Filtros */}
        <section className="my-incidents-toolbar-card">
          <div className="my-incidents-title-group">
            <h2>Seguimiento de Mis Incidencias</h2>
            <p className="my-incidents-subtitle">
              Consulta el estado de tramitación, resolución territorial y actuaciones técnicas
              asociadas a tus avisos reportados en la ciudad de Málaga.
            </p>
          </div>

          <div className="my-incidents-controls">
            <div className="filter-group">
              <label htmlFor="status-filter-select" className="filter-label">
                Filtrar por estado:
              </label>
              <select
                id="status-filter-select"
                className="status-filter-select"
                value={statusFilter}
                onChange={handleStatusChange}
                aria-label="Selector de filtro por estado"
              >
                <option value="TODOS">TODOS (Todas las incidencias)</option>
                {INCIDENT_STATUSES.map((statusKey) => (
                  <option key={statusKey} value={statusKey}>
                    {getStatusLabel(statusKey)}
                  </option>
                ))}
              </select>
            </div>

            <button
              type="button"
              className="btn-refresh"
              onClick={() => fetchMyIncidents(page, statusFilter)}
              disabled={loading}
              title="Actualizar listado"
              aria-label="Actualizar incidencias"
            >
              🔄 Actualizar
            </button>
          </div>
        </section>

        {/* Estado de Carga Accesible */}
        {loading && (
          <div
            className="my-incidents-loading"
            role="status"
            aria-live="polite"
          >
            <span className="loading-spinner" aria-hidden="true" />
            <span>Cargando tus incidencias...</span>
          </div>
        )}

        {/* Mensaje de Error con Opción de Reintentar */}
        {error && !loading && (
          <div className="my-incidents-error" role="alert" aria-live="assertive">
            <div className="error-message-content">
              <span className="error-icon" aria-hidden="true">⚠️</span>
              <p className="error-text">{error}</p>
            </div>
            <button
              type="button"
              className="btn-retry"
              onClick={() => fetchMyIncidents(page, statusFilter)}
            >
              Reintentar
            </button>
          </div>
        )}

        {/* Vista de Estado Vacío */}
        {showEmptyState && (
          <section className="my-incidents-empty-card" aria-label="Sin incidencias">
            <span className="empty-state-icon" aria-hidden="true">
              {isFiltered ? '🔍' : '📋'}
            </span>
            <h3 className="empty-state-title">
              {isFiltered
                ? 'No se encontraron incidencias con el estado seleccionado'
                : 'No tienes incidencias registradas todavía'}
            </h3>
            <p className="empty-state-description">
              {isFiltered
                ? 'Prueba a seleccionar otro estado o muestra todas las incidencias para revisar tu historial completo.'
                : 'Contribuye activamente al mantenimiento de la ciudad reportando avisos sobre el mapa cartográfico interactivo.'}
            </p>
            <div className="empty-state-actions">
              {isFiltered ? (
                <button
                  type="button"
                  className="btn-clear-filter"
                  onClick={() => {
                    setStatusFilter('TODOS');
                    setPage(0);
                  }}
                >
                  Mostrar todas las incidencias
                </button>
              ) : (
                <Link to="/map" className="btn-create-action">
                  Reportar incidencia en el mapa
                </Link>
              )}
            </div>
          </section>
        )}

        {/* Listado de Incidencias en Tabla Responsive */}
        {!loading && !error && incidents.length > 0 && (
          <section className="my-incidents-list-card">
            <div className="table-responsive">
              <table
                className="my-incidents-table"
                aria-label="Tabla de incidencias del ciudadano"
              >
                <thead>
                  <tr>
                    <th scope="col">Título y Categoría</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Dirección</th>
                    <th scope="col">Distrito</th>
                    <th scope="col">Fecha de Reporte</th>
                    <th scope="col">Última Actualización</th>
                    <th scope="col" className="text-center">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {incidents.map((incident) => {
                    const statusClass = incident.status
                      ? `status-${incident.status.toLowerCase()}`
                      : 'status-reported';
                    const addressText = incident.address?.trim() || 'Sin asignar';
                    const districtText = incident.district?.trim() || 'Sin asignar';

                    return (
                      <tr key={incident.id} className="incident-row">
                        <td className="incident-title-cell">
                          <strong className="incident-title">{incident.title}</strong>
                          {incident.category && (
                            <span className="incident-category-chip">
                              {incident.category}
                            </span>
                          )}
                        </td>
                        <td>
                          <span className={`status-badge ${statusClass}`}>
                            {getStatusLabel(incident.status)}
                          </span>
                        </td>
                        <td className="incident-location-cell">
                          <span
                            className={
                              addressText === 'Sin asignar'
                                ? 'location-unassigned'
                                : 'location-assigned'
                            }
                          >
                            {addressText}
                          </span>
                        </td>
                        <td className="incident-district-cell">
                          <span
                            className={
                              districtText === 'Sin asignar'
                                ? 'location-unassigned'
                                : 'location-assigned'
                            }
                          >
                            {districtText}
                          </span>
                        </td>
                        <td className="incident-date-cell">
                          <time dateTime={incident.createdAt}>
                            {formatDate(incident.createdAt)}
                          </time>
                        </td>
                        <td className="incident-date-cell">
                          <time dateTime={incident.updatedAt || incident.createdAt}>
                            {formatDate(incident.updatedAt || incident.createdAt)}
                          </time>
                        </td>
                        <td className="incident-actions-cell text-center">
                          <button
                            type="button"
                            className="btn-view-detail"
                            onClick={() => handleOpenDetail(incident)}
                            aria-label={`Ver detalle de la incidencia ${incident.title}`}
                          >
                            Ver detalle
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Controles de Paginación Accesibles */}
            <nav
              className="my-incidents-pagination"
              aria-label="Paginación de incidencias"
            >
              <button
                type="button"
                className="pagination-btn"
                onClick={handlePreviousPage}
                disabled={isPreviousDisabled}
                aria-label="Página anterior"
              >
                &larr; Página anterior
              </button>

              <span className="pagination-indicator" aria-live="polite">
                {`Página ${totalPages === 0 || totalElements === 0 ? 1 : page + 1} de ${
                  Math.max(totalPages, 1)
                }`}
              </span>

              <button
                type="button"
                className="pagination-btn"
                onClick={handleNextPage}
                disabled={isNextDisabled}
                aria-label="Página siguiente"
              >
                Página siguiente &rarr;
              </button>
            </nav>
          </section>
        )}
      </main>

      {/* Modal de Detalle Exhaustivo de Incidencia */}
      {selectedIncident && (
        <IncidentDetailModal
          incident={selectedIncident}
          isOpen={Boolean(selectedIncident)}
          onClose={handleCloseDetail}
        />
      )}
    </div>
  );
}
