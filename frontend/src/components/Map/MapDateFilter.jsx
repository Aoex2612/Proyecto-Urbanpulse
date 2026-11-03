/**
 * @fileoverview Barra de filtrado temporal para el mapa cartografico interactivo de UrbanPulse.
 * Permite acotar las incidencias visibles mediante presets rapidos ('week', 'today', 'month', 'all')
 * o mediante un rango de fechas personalizado ('custom' con Desde / Hasta).
 */

import { DATE_FILTER_PRESETS } from '../../utils/dateUtils';
import './MapDateFilter.css';

/**
 * Componente de filtrado por rango de fechas para el mapa de incidencias.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.filter - Estado actual del filtro.
 * @param {string} props.filter.preset - Clave del preset activo ('week', 'today', 'month', 'all', 'custom').
 * @param {string} props.filter.startDate - Fecha inicial en formato 'YYYY-MM-DD'.
 * @param {string} props.filter.endDate - Fecha final en formato 'YYYY-MM-DD'.
 * @param {Function} props.onFilterChange - Callback notificado al modificarse el preset o los limites de fecha.
 * @param {number} [props.totalCount=0] - Numero total de incidencias activas en el sistema.
 * @param {number} [props.filteredCount=0] - Numero de incidencias que cumplen el filtro y se muestran en el mapa.
 * @returns {import('react').JSX.Element} Barra de herramientas de filtrado temporal.
 */
export default function MapDateFilter({
  filter,
  onFilterChange,
  totalCount = 0,
  filteredCount = 0,
}) {
  const currentPreset = filter?.preset || DATE_FILTER_PRESETS.WEEK;
  const startDate = filter?.startDate || '';
  const endDate = filter?.endDate || '';

  const isFiltered = currentPreset !== DATE_FILTER_PRESETS.ALL;
  const isCustom = currentPreset === DATE_FILTER_PRESETS.CUSTOM;

  /**
   * Conmuta el preset seleccionado.
   *
   * @param {string} presetKey - Clave del preset elegido.
   */
  const handlePresetSelect = (presetKey) => {
    onFilterChange?.({
      preset: presetKey,
      startDate: presetKey === DATE_FILTER_PRESETS.CUSTOM ? startDate : '',
      endDate: presetKey === DATE_FILTER_PRESETS.CUSTOM ? endDate : '',
    });
  };

  /**
   * Actualiza la fecha inicial en modo personalizado.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} e - Evento de cambio del input date.
   */
  const handleStartDateChange = (e) => {
    onFilterChange?.({
      preset: DATE_FILTER_PRESETS.CUSTOM,
      startDate: e.target.value,
      endDate,
    });
  };

  /**
   * Actualiza la fecha final en modo personalizado.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement>} e - Evento de cambio del input date.
   */
  const handleEndDateChange = (e) => {
    onFilterChange?.({
      preset: DATE_FILTER_PRESETS.CUSTOM,
      startDate,
      endDate: e.target.value,
    });
  };

  /**
   * Restablece el filtro al preset predeterminado de la semana.
   */
  const handleReset = () => {
    onFilterChange?.({
      preset: DATE_FILTER_PRESETS.WEEK,
      startDate: '',
      endDate: '',
    });
  };

  return (
    <section
      className="map-date-filter-bar"
      role="region"
      aria-label="Filtro temporal de incidencias en el mapa"
    >
      <div className="filter-presets-group" role="group" aria-label="Presets temporales">
        <span className="filter-group-label" aria-hidden="true">
          📅 Periodo:
        </span>

        <button
          type="button"
          className={`btn-filter-preset ${
            currentPreset === DATE_FILTER_PRESETS.WEEK ? 'is-active' : ''
          }`}
          onClick={() => handlePresetSelect(DATE_FILTER_PRESETS.WEEK)}
          aria-pressed={currentPreset === DATE_FILTER_PRESETS.WEEK}
          title="Ver incidencias registradas en los ultimos 7 dias"
        >
          Ultimos 7 dias
        </button>

        <button
          type="button"
          className={`btn-filter-preset ${
            currentPreset === DATE_FILTER_PRESETS.TODAY ? 'is-active' : ''
          }`}
          onClick={() => handlePresetSelect(DATE_FILTER_PRESETS.TODAY)}
          aria-pressed={currentPreset === DATE_FILTER_PRESETS.TODAY}
          title="Ver incidencias reportadas en el dia de hoy"
        >
          Hoy
        </button>

        <button
          type="button"
          className={`btn-filter-preset ${
            currentPreset === DATE_FILTER_PRESETS.MONTH ? 'is-active' : ''
          }`}
          onClick={() => handlePresetSelect(DATE_FILTER_PRESETS.MONTH)}
          aria-pressed={currentPreset === DATE_FILTER_PRESETS.MONTH}
          title="Ver incidencias de los ultimos 30 dias"
        >
          Ultimos 30 dias
        </button>

        <button
          type="button"
          className={`btn-filter-preset ${
            currentPreset === DATE_FILTER_PRESETS.ALL ? 'is-active' : ''
          }`}
          onClick={() => handlePresetSelect(DATE_FILTER_PRESETS.ALL)}
          aria-pressed={currentPreset === DATE_FILTER_PRESETS.ALL}
          title="Ver todo el historico de incidencias sin limite de fecha"
        >
          Todas
        </button>

        <button
          type="button"
          className={`btn-filter-preset ${isCustom ? 'is-active' : ''}`}
          onClick={() => handlePresetSelect(DATE_FILTER_PRESETS.CUSTOM)}
          aria-pressed={isCustom}
          title="Fijar un intervalo de fechas personalizado"
        >
          Personalizado
        </button>
      </div>

      {/* Selectores de fecha para intervalo personalizado */}
      {isCustom && (
        <div className="filter-custom-dates" role="group" aria-label="Rango de fechas especifico">
          <label className="filter-date-field">
            <span className="field-label">Desde:</span>
            <input
              type="date"
              className="filter-date-input"
              value={startDate}
              onChange={handleStartDateChange}
              aria-label="Fecha inicial de busqueda"
            />
          </label>

          <label className="filter-date-field">
            <span className="field-label">Hasta:</span>
            <input
              type="date"
              className="filter-date-input"
              value={endDate}
              onChange={handleEndDateChange}
              aria-label="Fecha final de busqueda"
            />
          </label>
        </div>
      )}

      {/* Indicador de incidencias filtradas y boton de reseteo */}
      <div className="filter-status-info">
        <span className="filter-count-badge" aria-live="polite">
          {filteredCount === totalCount ? (
            `${filteredCount} incidencias`
          ) : (
            <>
              <strong>{filteredCount}</strong> de {totalCount} incidencias
            </>
          )}
        </span>

        {isFiltered && currentPreset !== DATE_FILTER_PRESETS.WEEK && (
          <button
            type="button"
            className="btn-filter-reset"
            onClick={handleReset}
            title="Volver a los ultimos 7 dias"
            aria-label="Restablecer filtro a los ultimos 7 dias"
          >
            Restablecer
          </button>
        )}
      </div>
    </section>
  );
}
