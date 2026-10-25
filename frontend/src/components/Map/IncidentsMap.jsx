/**
 * @fileoverview Componente cartográfico interactivo de incidencias basado en React Leaflet.
 * Renderiza el mapa urbano centrado por defecto en Málaga, posiciona los marcadores de las
 * incidencias activas con popups nativos detallados y gestiona la captura asistida de
 * coordenadas geográficas mediante eventos de clic sobre el lienzo cartográfico.
 */

import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from 'react-leaflet';
import { createColoredMarkerIcon } from '../../utils/leafletIcons';
import {
  DEFAULT_MAP_CENTER,
  DEFAULT_MAP_ZOOM,
  getStatusLabel,
  getIncidentMarkerIcon,
} from './mapConstants';
import './Map.css';

/**
 * Subcomponente interno para capturar eventos de interacción y pulsación sobre el lienzo de Leaflet.
 * Se encuentra activo exclusivamente cuando el modo de selección geográfica (`isSelectingLocation`)
 * está habilitado.
 *
 * @param {Object} props - Propiedades del manejador de eventos cartográficos.
 * @param {boolean} props.isSelectingLocation - Bandera que activa la escucha de selección de ubicación.
 * @param {Function} [props.onLocationSelected] - Función callback invocada con las coordenadas seleccionadas `{ latitude, longitude }`.
 * @returns {null} No renderiza contenido en el DOM.
 */
function MapEventsHandler({ isSelectingLocation, onLocationSelected }) {
  useMapEvents({
    click(e) {
      if (isSelectingLocation && onLocationSelected) {
        onLocationSelected({
          latitude: e.latlng.lat,
          longitude: e.latlng.lng,
        });
      }
    },
  });

  return null;
}

/**
 * Componente principal de visualización cartográfica de incidencias con Leaflet.
 *
 * @param {Object} props - Propiedades del componente cartográfico.
 * @param {Array<Object>} [props.incidents=[]] - Lista de incidencias activas a geoposicionar en el mapa.
 * @param {Function} [props.onSelectIncident] - Callback ejecutado al pulsar "Ver detalle completo" en un marcador.
 * @param {boolean} [props.isSelectingLocation=false] - Indica si el mapa está en modo de captura de coordenadas.
 * @param {Function} [props.onLocationSelected] - Callback notificado al hacer clic sobre el mapa en modo selección.
 * @param {Object} [props.selectedLocation=null] - Ubicación geográfica temporal seleccionada `{ latitude, longitude }`.
 * @param {[number, number]} [props.center=DEFAULT_MAP_CENTER] - Coordenadas iniciales del centro cartográfico.
 * @param {number} [props.zoom=DEFAULT_MAP_ZOOM] - Nivel inicial de zoom.
 * @param {string} [props.className=''] - Clases CSS adicionales para el contenedor.
 * @returns {import('react').JSX.Element} Vista del mapa interactivo con OpenStreetMap.
 */
export default function IncidentsMap({
  incidents = [],
  onSelectIncident,
  isSelectingLocation = false,
  onLocationSelected,
  selectedLocation = null,
  center = DEFAULT_MAP_CENTER,
  zoom = DEFAULT_MAP_ZOOM,
  className = '',
}) {
  // Filtrar únicamente aquellas incidencias que presenten coordenadas geográficas numéricas válidas
  const validIncidents = incidents.filter(
    (incident) =>
      incident &&
      incident.latitude != null &&
      incident.longitude != null &&
      !Number.isNaN(Number(incident.latitude)) &&
      !Number.isNaN(Number(incident.longitude))
  );

  return (
    <div
      className={`incidents-map-wrapper ${
        isSelectingLocation ? 'map-selection-active' : ''
      } ${className}`.trim()}
    >
      {isSelectingLocation && (
        <div className="map-selection-banner" role="status" aria-live="polite">
          <span className="map-selection-banner-icon" aria-hidden="true">
            🎯
          </span>
          <span>Modo de selección activo: Haz clic en el mapa para ubicar la incidencia</span>
        </div>
      )}

      <MapContainer
        center={center}
        zoom={zoom}
        scrollWheelZoom={true}
        className="incidents-map-container"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {/* Capturador de eventos de clic en el lienzo de Leaflet */}
        <MapEventsHandler
          isSelectingLocation={isSelectingLocation}
          onLocationSelected={onLocationSelected}
        />

        {/* Marcadores de incidencias urbanas registradas */}
        {validIncidents.map((incident) => {
          const lat = Number(incident.latitude);
          const lng = Number(incident.longitude);
          const markerIcon = getIncidentMarkerIcon(incident.status);
          const statusClass = incident.status
            ? `status-${incident.status.toLowerCase()}`
            : 'status-reported';

          return (
            <Marker
              key={incident.id}
              position={[lat, lng]}
              icon={markerIcon}
            >
              <Popup className="incident-popup">
                <div className="incident-popup-card">
                  <div className="incident-popup-header">
                    <span className={`status-badge ${statusClass}`}>
                      {getStatusLabel(incident.status)}
                    </span>
                    {incident.category && (
                      <span className="category-badge">{incident.category}</span>
                    )}
                  </div>

                  <h3 className="incident-popup-title">{incident.title}</h3>

                  {incident.description && (
                    <p className="incident-popup-description">
                      {incident.description}
                    </p>
                  )}

                  <div className="incident-popup-actions">
                    <button
                      type="button"
                      className="btn-popup-detail"
                      onClick={() => onSelectIncident?.(incident)}
                    >
                      Ver detalle completo
                    </button>
                  </div>
                </div>
              </Popup>
            </Marker>
          );
        })}

        {/* Marcador temporal indicativo de la coordenada seleccionada */}
        {selectedLocation &&
          selectedLocation.latitude != null &&
          selectedLocation.longitude != null &&
          !Number.isNaN(Number(selectedLocation.latitude)) &&
          !Number.isNaN(Number(selectedLocation.longitude)) && (
            <Marker
              position={[
                Number(selectedLocation.latitude),
                Number(selectedLocation.longitude),
              ]}
              icon={createColoredMarkerIcon('#aa3bff')}
            >
              <Popup className="incident-popup">
                <div className="incident-popup-card">
                  <div className="incident-popup-header">
                    <span className="status-badge status-draft">
                      Ubicación Seleccionada
                    </span>
                  </div>
                  <p className="incident-popup-description">
                    Coordenadas preparadas para el formulario de alta.
                  </p>
                </div>
              </Popup>
            </Marker>
          )}
      </MapContainer>
    </div>
  );
}
