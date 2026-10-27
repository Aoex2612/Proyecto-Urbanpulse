/**
 * @fileoverview Vista principal del mapa cartografico interactivo (/map) en UrbanPulse.
 * Orquesta la visualizacion de incidencias geolocalizadas sobre Leaflet con OpenStreetMap,
 * la barra superior de mandos con resumen de incidencias no terminadas, la barra de filtrado
 * temporal con preset semanal por defecto, el boton toggle de captura geografica asistida,
 * el modal de alta de incidencia y el modal de detalle con auditoria.
 */

import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import authService from '../services/authService';
import incidentService from '../services/incidentService';
import {
  IncidentsMap,
  CreateIncidentModal,
  IncidentDetailModal,
  MapDateFilter,
} from '../components/Map';
import { filterIncidentsByDate, DATE_FILTER_PRESETS } from '../utils/dateUtils';
import './MapPage.css';

/**
 * Componente principal de la pagina del Mapa de Incidencias Urbanas.
 *
 * @returns {import('react').JSX.Element} Vista del mapa cartografico interactivo.
 */
export default function MapPage() {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Estado del filtro temporal (preset predeterminado: 'week' - ultimos 7 dias)
  const [dateFilter, setDateFilter] = useState({
    preset: DATE_FILTER_PRESETS.WEEK,
    startDate: '',
    endDate: '',
  });

  // Estados de flujo de creacion asistida
  const [isSelectingLocation, setIsSelectingLocation] = useState(false);
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  // Estados de flujo de inspeccion de detalle
  const [selectedIncident, setSelectedIncident] = useState(null);

  // Mensaje temporal de exito / feedback tras alta
  const [toastMessage, setToastMessage] = useState(null);

  // Obtener usuario autenticado en sesion
  const currentUser = useMemo(() => {
    return typeof authService.getCurrentUser === 'function'
      ? authService.getCurrentUser()
      : authService.getUser();
  }, []);

  const hasOperatorAccess = authService.hasAnyRole(['OPERATOR', 'ADMIN']);

  /**
   * Carga desde el backend la lista de incidencias urbanas activas.
   *
   * @param {boolean} [isManualRefresh=false] - Indica si la peticion proviene de un refresco manual.
   * @returns {Promise<void>}
   */
  const loadIncidents = useCallback(async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setRefreshing(true);
    }
    setError(null);

    try {
      const data = await incidentService.getAllIncidents();
      setIncidents(Array.isArray(data) ? data : []);
    } catch (err) {
      const errorMsg =
        err?.response?.data?.message ||
        'No se pudieron cargar las incidencias en el mapa. Por favor, intentalo de nuevo.';
      setError(errorMsg);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Carga inicial asincrona al montar la pagina
  useEffect(() => {
    let isMounted = true;

    Promise.resolve().then(() => {
      if (isMounted) {
        loadIncidents(false);
      }
    });

    return () => {
      isMounted = false;
    };
  }, [loadIncidents]);

  /**
   * Conmuta el modo de seleccion geografica en el mapa (RF-6 y RF-7).
   * Al cancelarse, descarta cualquier coordenada provisional previa.
   *
   * @returns {void}
   */
  const handleToggleSelectionMode = () => {
    if (isSelectingLocation) {
      // Cancelar seleccion (RF-7)
      setIsSelectingLocation(false);
      setSelectedLocation(null);
    } else {
      // Activar modo seleccion (RF-6)
      setIsSelectingLocation(true);
      setSelectedLocation(null);
    }
  };

  /**
   * Captura las coordenadas seleccionadas mediante un clic en el mapa de Leaflet (RF-8).
   * Desactiva el modo de seleccion y despliega el modal de alta con las coordenadas prefijadas.
   *
   * @param {{latitude: number, longitude: number}} location - Coordenadas geograficas seleccionadas.
   * @returns {void}
   */
  const handleLocationSelected = (location) => {
    setSelectedLocation(location);
    setIsSelectingLocation(false);
    setIsCreateModalOpen(true);
  };

  /**
   * Descarta la seleccion de coordenadas y cierra el modal de alta sin persistir datos (RF-9).
   *
   * @returns {void}
   */
  const handleCloseCreateModal = () => {
    setIsCreateModalOpen(false);
    setSelectedLocation(null);
  };

  /**
   * Callback invocado tras la creacion exitosa de una nueva incidencia (RF-11).
   * Incorpora inmediatamente la nueva incidencia al estado para reflejar su marcador
   * en el mapa y despliega una notificacion de confirmacion.
   *
   * @param {Object} newIncident - Incidencia devuelta por la API con HTTP 201.
   * @returns {void}
   */
  const handleIncidentCreated = (newIncident) => {
    if (newIncident) {
      setIncidents((prev) => [
        newIncident,
        ...prev.filter((item) => item.id !== newIncident.id),
      ]);
    }
    setIsCreateModalOpen(false);
    setSelectedLocation(null);
    setToastMessage('¡Incidencia registrada con exito y posicionada en el mapa!');
  };

  /**
   * Abre el modal de detalle para la incidencia seleccionada desde el popup de Leaflet (RF-3, RF-18).
   *
   * @param {Object} incident - Objeto con los datos de la incidencia a examinar.
   * @returns {void}
   */
  const handleSelectIncident = (incident) => {
    setSelectedIncident(incident);
  };

  /**
   * Cierra el modal de inspeccion detallada de incidencia.
   *
   * @returns {void}
   */
  const handleCloseDetailModal = () => {
    setSelectedIncident(null);
  };

  // Filtrado temporal memoizado de incidencias segun el preset o rango configurado
  const filteredIncidents = useMemo(() => {
    return filterIncidentsByDate(incidents, dateFilter);
  }, [incidents, dateFilter]);

  // Recuento de incidencias activas globales en la plataforma
  const totalActiveCount = useMemo(() => {
    return incidents.filter(
      (inc) => inc.status !== 'CLOSED' && inc.status !== 'REJECTED'
    ).length;
  }, [incidents]);

  // Recuento de incidencias activas que cumplen el filtro temporal actual
  const filteredActiveCount = useMemo(() => {
    return filteredIncidents.filter(
      (inc) => inc.status !== 'CLOSED' && inc.status !== 'REJECTED'
    ).length;
  }, [filteredIncidents]);

  return (
    <div className="map-page-container">
      {/* Cabecera superior del mapa */}
      <header className="map-page-header">
        <div className="map-header-left">
          <Link to="/" className="map-nav-back" title="Volver al Portal Ciudadano">
            &larr; Portal Ciudadano
          </Link>
          <div className="map-title-group">
            <h1 className="map-main-title">Mapa de Incidencias Urbanas</h1>
            <span className="map-badge-city">Malaga</span>
          </div>
        </div>

        <div className="map-header-center">
          <div
            className="map-counter-chip"
            aria-label={`Incidencias activas visibles: ${filteredActiveCount} de ${totalActiveCount}`}
          >
            <span className="counter-icon" aria-hidden="true">
              📍
            </span>
            <span className="counter-number">{filteredActiveCount}</span>
            <span className="counter-text">
              {dateFilter.preset === DATE_FILTER_PRESETS.ALL
                ? totalActiveCount === 1
                  ? 'incidencia activa'
                  : 'incidencias activas'
                : `de ${totalActiveCount} activas`}
            </span>
          </div>
        </div>

        <div className="map-header-actions">
          {hasOperatorAccess && (
            <Link
              to="/operator-panel"
              className="map-header-operator-link"
              title="Ir al Centro de Triaje"
            >
              Panel de Operador
            </Link>
          )}

          {/* Boton de refresco manual */}
          <button
            type="button"
            className={`map-btn-refresh ${refreshing ? 'is-spinning' : ''}`}
            onClick={() => {
              setLoading(true);
              loadIncidents(true);
            }}
            disabled={loading || refreshing}
            title="Actualizar listado de incidencias"
            aria-label="Actualizar incidencias"
          >
            <span className="refresh-icon" aria-hidden="true">
              🔄
            </span>
            <span className="refresh-label">{refreshing ? 'Cargando...' : 'Actualizar'}</span>
          </button>

          {/* Boton toggle de creacion / cancelacion de seleccion (RF-6 y RF-7) */}
          <button
            type="button"
            className={`map-btn-toggle-create ${
              isSelectingLocation ? 'mode-active' : 'mode-inactive'
            }`}
            onClick={handleToggleSelectionMode}
            aria-pressed={isSelectingLocation}
          >
            {isSelectingLocation ? (
              <>
                <span className="toggle-btn-icon" aria-hidden="true">
                  ✖
                </span>
                <span>Cancelar seleccion</span>
              </>
            ) : (
              <>
                <span className="toggle-btn-icon" aria-hidden="true">
                  ➕
                </span>
                <span>Crear incidencia</span>
              </>
            )}
          </button>
        </div>
      </header>

      {/* Barra interactiva de filtrado temporal de incidencias */}
      {!loading && !error && (
        <MapDateFilter
          filter={dateFilter}
          onFilterChange={setDateFilter}
          totalCount={totalActiveCount}
          filteredCount={filteredActiveCount}
        />
      )}

      {/* Banner / Toast de confirmacion tras crear una incidencia */}
      {toastMessage && (
        <aside className="map-toast-banner" role="status" aria-live="polite">
          <div className="toast-content">
            <span className="toast-icon" aria-hidden="true">
              ✅
            </span>
            <span className="toast-text">{toastMessage}</span>
          </div>
          <button
            type="button"
            className="toast-close-btn"
            onClick={() => setToastMessage(null)}
            aria-label="Cerrar notificacion"
          >
            ×
          </button>
        </aside>
      )}

      {/* Alerta de error en la consulta de incidencias con opcion de reintento */}
      {error && (
        <div className="map-error-alert" role="alert" aria-live="assertive">
          <div className="error-alert-content">
            <span className="error-alert-icon" aria-hidden="true">
              ⚠️
            </span>
            <span className="error-alert-text">{error}</span>
          </div>
          <button
            type="button"
            className="error-retry-btn"
            onClick={() => {
              setLoading(true);
              loadIncidents(false);
            }}
          >
            Reintentar
          </button>
        </div>
      )}

      {/* Contenedor principal del mapa cartografico */}
      <main className="map-page-main">
        {loading ? (
          <div className="map-loading-overlay" role="status" aria-live="polite">
            <div className="map-spinner" aria-hidden="true" />
            <p className="map-loading-text">Cargando mapa cartografico de Malaga...</p>
          </div>
        ) : (
          <IncidentsMap
            incidents={filteredIncidents}
            isSelectingLocation={isSelectingLocation}
            onLocationSelected={handleLocationSelected}
            onSelectIncident={handleSelectIncident}
            selectedLocation={selectedLocation}
            className="map-view-fullscreen"
          />
        )}
      </main>

      {/* Modal de alta asistida de incidencia (RF-8, RF-9, RF-10, RF-11, RF-12) */}
      {isCreateModalOpen && (
        <CreateIncidentModal
          isOpen={isCreateModalOpen}
          selectedLocation={selectedLocation}
          onClose={handleCloseCreateModal}
          onCancel={handleCloseCreateModal}
          onCreated={handleIncidentCreated}
        />
      )}

      {/* Modal de detalle completo y auditoria transversal (RF-2, RF-3, RF-18, RF-19) */}
      {selectedIncident && (
        <IncidentDetailModal
          isOpen={Boolean(selectedIncident)}
          incident={selectedIncident}
          onClose={handleCloseDetailModal}
          currentUser={currentUser}
        />
      )}
    </div>
  );
}
