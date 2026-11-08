/**
 * @fileoverview Componente de filtrado temporal y territorial para el Panel de Analítica Urbana.
 * Proporciona botones de preset temporal rápido (Hoy, 7d, 30d, 3m, 6m, Año, Año específico),
 * selector de año para análisis histórico, y selectores desplegables para distritos oficiales de Málaga
 * y sus respectivos barrios (RF-7, RF-8, RF-9, RF-10).
 */

import { useId } from 'react';
import {
  MALAGA_DISTRICTS,
  MALAGA_NEIGHBORHOODS_BY_DISTRICT,
  PRESET_OPTIONS,
} from '../../utils/analyticsConstants';
import './AnalyticsDateFilter.css';

/**
 * Componente interactivo para configurar filtros temporales y territoriales en la analítica urbana.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Object} props.filters - Estado actual de los filtros aplicados.
 * @param {string} [props.filters.preset='LAST_30_DAYS'] - Preset temporal activo.
 * @param {number} [props.filters.year] - Año seleccionado si preset es SPECIFIC_YEAR.
 * @param {string} [props.filters.district=''] - Distrito seleccionado o vacío para todos.
 * @param {string} [props.filters.neighborhood=''] - Barrio seleccionado o vacío para todos.
 * @param {Function} props.onFilterChange - Callback ejecutado de forma reactiva al modificar cualquier filtro.
 * @param {Function} [props.onReset] - Callback para restablecer los filtros a los valores predeterminados.
 * @param {boolean} [props.disabled=false] - Inhabilitación temporal de controles durante carga.
 * @returns {import('react').JSX.Element} Panel de controles de filtrado analítico.
 */
export default function AnalyticsDateFilter({
  filters = {
    preset: 'LAST_30_DAYS',
    year: new Date().getFullYear(),
    district: '',
    neighborhood: '',
  },
  onFilterChange,
  onReset = null,
  disabled = false,
}) {
  const currentYear = new Date().getFullYear();
  const yearOptions = [currentYear, currentYear - 1, currentYear - 2, currentYear - 3, currentYear - 4];

  const districtSelectId = useId();
  const neighborhoodSelectId = useId();
  const yearSelectId = useId();

  const activePreset = filters.preset || 'LAST_30_DAYS';
  const activeDistrict = filters.district || '';
  const activeNeighborhood = filters.neighborhood || '';
  const activeYear = filters.year || currentYear;

  const availableNeighborhoods = activeDistrict
    ? MALAGA_NEIGHBORHOODS_BY_DISTRICT[activeDistrict] || []
    : [];

  const handlePresetSelect = (presetId) => {
    if (disabled || presetId === activePreset) return;
    const nextFilters = {
      ...filters,
      preset: presetId,
    };
    if (presetId === 'SPECIFIC_YEAR' && !nextFilters.year) {
      nextFilters.year = currentYear;
    }
    onFilterChange(nextFilters);
  };

  const handleYearChange = (e) => {
    const selectedYear = Number(e.target.value);
    onFilterChange({
      ...filters,
      preset: 'SPECIFIC_YEAR',
      year: selectedYear,
    });
  };

  const handleDistrictChange = (e) => {
    const newDistrict = e.target.value;
    // Si cambia de distrito y el barrio actual no pertenece a él, se resetea el barrio
    let newNeighborhood = activeNeighborhood;
    if (newDistrict && activeNeighborhood) {
      const allowed = MALAGA_NEIGHBORHOODS_BY_DISTRICT[newDistrict] || [];
      if (!allowed.includes(activeNeighborhood)) {
        newNeighborhood = '';
      }
    } else if (!newDistrict) {
      newNeighborhood = '';
    }

    onFilterChange({
      ...filters,
      district: newDistrict,
      neighborhood: newNeighborhood,
    });
  };

  const handleNeighborhoodChange = (e) => {
    onFilterChange({
      ...filters,
      neighborhood: e.target.value,
    });
  };

  const handleResetClick = () => {
    const defaultFilters = {
      preset: 'LAST_30_DAYS',
      year: currentYear,
      district: '',
      neighborhood: '',
    };
    if (typeof onReset === 'function') {
      onReset();
    } else {
      onFilterChange(defaultFilters);
    }
  };

  const hasActiveCustomFilters =
    activePreset !== 'LAST_30_DAYS' || Boolean(activeDistrict) || Boolean(activeNeighborhood);

  return (
    <section className="analytics-filter-panel" aria-label="Filtros de analítica urbana">
      {/* 1. Presets temporales */}
      <div className="analytics-filter-row">
        <div className="analytics-filter-group" role="group" aria-label="Rango temporal">
          <span className="analytics-filter-group-label" id="time-preset-label">
            Periodo:
          </span>
          <div className="analytics-preset-chips" aria-labelledby="time-preset-label">
            {PRESET_OPTIONS.map((opt) => {
              const isSelected = activePreset === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  className={`analytics-preset-chip ${isSelected ? 'is-active' : ''}`}
                  onClick={() => handlePresetSelect(opt.id)}
                  disabled={disabled}
                  aria-pressed={isSelected}
                  data-testid={`preset-btn-${opt.id}`}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>
        </div>

        {/* Selector de año específico (visible si preset === SPECIFIC_YEAR) */}
        {activePreset === 'SPECIFIC_YEAR' && (
          <div className="analytics-filter-control year-control">
            <label htmlFor={yearSelectId} className="analytics-filter-label">
              Año:
            </label>
            <select
              id={yearSelectId}
              className="analytics-select"
              value={activeYear}
              onChange={handleYearChange}
              disabled={disabled}
              aria-label="Seleccionar año específico"
            >
              {yearOptions.map((yr) => (
                <option key={yr} value={yr}>
                  {yr}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* 2. Filtros territoriales (Distrito y Barrio) y Acción de restablecimiento */}
      <div className="analytics-filter-row territorial-row">
        <div className="analytics-filter-control">
          <label htmlFor={districtSelectId} className="analytics-filter-label">
            Distrito:
          </label>
          <select
            id={districtSelectId}
            className="analytics-select"
            value={activeDistrict}
            onChange={handleDistrictChange}
            disabled={disabled}
            aria-label="Filtrar por distrito municipal"
          >
            <option value="">Todos los distritos</option>
            {MALAGA_DISTRICTS.map((d) => (
              <option key={d} value={d}>
                {d}
              </option>
            ))}
          </select>
        </div>

        <div className="analytics-filter-control">
          <label htmlFor={neighborhoodSelectId} className="analytics-filter-label">
            Barrio:
          </label>
          <select
            id={neighborhoodSelectId}
            className="analytics-select"
            value={activeNeighborhood}
            onChange={handleNeighborhoodChange}
            disabled={disabled || !activeDistrict}
            aria-label="Filtrar por barrio"
          >
            <option value="">
              {!activeDistrict ? 'Selecciona un distrito primero' : 'Todos los barrios'}
            </option>
            {availableNeighborhoods.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
        </div>

        {hasActiveCustomFilters && (
          <button
            type="button"
            className="analytics-reset-btn"
            onClick={handleResetClick}
            disabled={disabled}
            aria-label="Restablecer filtros a valores predeterminados"
          >
            <svg
              viewBox="0 0 24 24"
              className="analytics-reset-icon"
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
        )}
      </div>
    </section>
  );
}

