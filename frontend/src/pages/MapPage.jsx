/**
 * @fileoverview Vista principal del mapa cartográfico interactivo (/map) en UrbanPulse.
 * Orquesta la visualización de incidencias geolocalizadas sobre Leaflet con OpenStreetMap,
 * la barra superior de mandos con resumen de incidencias no terminadas, el botón toggle de
 * captura geográfica asistida, el modal de alta de incidencia y el modal de detalle con auditoría.
 */

import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import authService from '../services/authService';
import incidentService from '../services/incidentService';
import {
  IncidentsMap,
  CreateIncidentModal,
  IncidentDetailModal,
} from '../components/Map';
import './MapPage.css';

/**
 * Componente principal de la página del Mapa de Incidencias Urbanas.
 *
 * @returns {import('react').JSX.Element} Vista del mapa cartográfico interactivo.
 */
export default function MapPage() {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Estados de flujo de creación asistida
  const [isSelectingLocation, setIsSelectingLocation] = useState(false);
  const [selectedLocation, setSelectedLocation] = useState(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  // Estados de flujo de inspección de detalle
  const [selectedIncident, setSelectedIncident] = useState(null);

  // Mensaje temporal de éxito / feedback tras alta
  const [toastMessage, setToastMessage] = useState(null);

  // Obtener usuario autenticado en sesión
  const currentUser = useMemo(() => {
    return typeof authService.getCurrentUser === 'function'
      ? authService.getCurrentUser()
      : authService.getUser();
  }, []);

  const hasOperatorAccess = authService.hasAnyRole(['OPERATOR', 'ADMIN']);

  /**
   * Carga desde el backend la lista de incidencias urbanas activas.
   *
   * @param {boolean} [isManualRefresh=false] - Indica si la petición proviene de un refresco manual.
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
        'No se pudieron cargar las incidencias en el mapa. Por favor, inténtalo de nuevo.';
      setError(errorMsg);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Carga inicial asíncrona al montar la página
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
   * Conmuta el modo de selección geográfica en el mapa (RF-6 y RF-7).
   * Al cancelarse, descarta cualquier coordenada provisional previa.
   *
   * @returns {void}
   */
  const handleToggleSelectionMode = () => {
    if (isSelectingLocation) {
      // Cancelar selección (RF-7)
      setIsSelectingLocation(false);
      setSelectedLocation(null);
    } else {
      // Activar modo selección (RF-6)
      setIsSelectingLocation(true);
      setSelectedLocation(null);
    }
  };

  /**
   * Captura las coordenadas seleccionadas mediante un clic en el mapa de Leaflet (RF-8).
   * Desactiva el modo de selección y despliega el modal de alta con las coordenadas prefijadas.
   *
   * @param {{latitude: number, longitude: number}} location - Coordenadas geográficas seleccionadas.
   * @returns {void}
   */
  const handleLocationSelected = (location) => {
    setSelectedLocation(location);
    setIsSelectingLocation(false);
    setIsCreateModalOpen(true);
  };

  /**
   * Descarta la selección de coordenadas y cierra el modal de alta sin persistir datos (RF-9).
   *
   * @returns {void}
   */
  const handleCloseCreateModal = () => {
    setIsCreateModalOpen(false);
    setSelectedLocation(null);
  };

  /**
   * Callback invocado tras la creación exitosa de una nueva incidencia (RF-11).
   * Incorpora inmediatamente la nueva incidencia al estado para reflejar su marcador
   * en el mapa y despliega una notificación de confirmación.
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
    setToastMessage('¡Incidencia registrada con éxito y posicionada en el mapa!');
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
   * Cierra el modal de inspección detallada de incidencia.
   *
   * @returns {void}
   */
  const handleCloseDetailModal = () => {
    setSelectedIncident(null);
  };

  // Cálculo de incidencias activas no finalizadas
  const activeCount = useMemo(() => {
    return incidents.filter(
      (inc) => inc.status !== 'CLOSED' && inc.status !== 'REJECTED'
    ).length;
  }, [incidents]);

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
            <span className="map-badge-city">Málaga</span>
          </div>
        </div>

        <div className="map-header-center">
          <div
            className="map-counter-chip"
            aria-label={`Total de incidencias activas: ${activeCount}`}
          >
            <span className="counter-icon" aria-hidden="true">📍</span>
            <span className="counter-number">{activeCount}</span>
            <span className="counter-text">
              {activeCount === 1 ? 'incidencia activa' : 'incidencias activas'}
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

          {/* Botón de refresco manual */}
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
            <span className="refresh-icon" aria-hidden="true">🔄</span>
            <span className="refresh-label">{refreshing ? 'Cargando...' : 'Actualizar'}</span>
          </button>

          {/* Botón toggle de creación / cancelación de selección (RF-6 y RF-7) */}
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
                <span className="toggle-btn-icon" aria-hidden="true">✖</span>
                <span>Cancelar selección</span>
              </>
            ) : (
              <>
                <span className="toggle-btn-icon" aria-hidden="true">➕</span>
                <span>Crear incidencia</span>
              </>
            )}
          </button>
        </div>
      </header>

      {/* Banner / Toast de confirmación tras crear una incidencia */}
      {toastMessage && (
        <aside className="map-toast-banner" role="status" aria-live="polite">
          <div className="toast-content">
            <span className="toast-icon" aria-hidden="true">✅</span>
            <span className="toast-text">{toastMessage}</span>
          </div>
          <button
            type="button"
            className="toast-close-btn"
            onClick={() => setToastMessage(null)}
            aria-label="Cerrar notificación"
          >
            ×
          </button>
        </aside>
      )}

      {/* Alerta de error en la consulta de incidencias con opción de reintento */}
      {error && (
        <div className="map-error-alert" role="alert" aria-live="assertive">
          <div className="error-alert-content">
            <span className="error-alert-icon" aria-hidden="true">⚠️</span>
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

      {/* Contenedor principal del mapa cartográfico */}
      <main className="map-page-main">
        {loading ? (
          <div className="map-loading-overlay" role="status" aria-live="polite">
            <div className="map-spinner" aria-hidden="true" />
            <p className="map-loading-text">Cargando mapa cartográfico de Málaga...</p>
          </div>
        ) : (
          <IncidentsMap
            incidents={incidents}
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

      {/* Modal de detalle completo y auditoría transversal (RF-2, RF-3, RF-18, RF-19) */}
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
