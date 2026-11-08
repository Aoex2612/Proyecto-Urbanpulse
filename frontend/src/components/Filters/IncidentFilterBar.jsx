/**
 * @fileoverview Barra de búsqueda y filtrado multicriterio de incidencias con control RBAC.
 * Permite filtrar por texto libre, categoría temática y rango de fechas a todos los roles,
 * restringiendo el filtrado por estado de triaje exclusivamente a roles internos (no CITIZEN) (HU19).
 */

import { useState } from 'react';
import { INCIDENT_CATEGORIES, INCIDENT_STATUSES, getStatusLabel } from '../Map/mapConstants';
import './IncidentFilterBar.css';

/**
 * Componente de barra de filtros multicriterio con control de acceso basado en roles.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {Function} props.onFilterChange - Callback invocado al modificar cualquier filtro con el objeto de criterios.
 * @param {string} [props.userRole] - Rol funcional del usuario autenticado (ej. CITIZEN, OPERATOR, ADMIN).
 * @param {boolean} [props.hasResults=true] - Indicador booleano para mostrar aviso de búsqueda sin resultados.
 * @returns {import('react').JSX.Element} Elemento JSX de la barra de filtros.
 */
export default function IncidentFilterBar({ onFilterChange, userRole, hasResults = true }) {
  const [filters, setFilters] = useState({
    text: '',
    category: '',
    startDate: '',
    endDate: '',
    status: '',
  });

  const canFilterByStatus = userRole !== 'CITIZEN';

  /**
   * Maneja el cambio de valores en los campos de filtrado y notifica al componente padre.
   *
   * @param {string} field Nombre del filtro modificado.
   * @param {string} value Nuevo valor asignado al filtro.
   */
  const handleChange = (field, value) => {
    const updated = { ...filters, [field]: value };
    setFilters(updated);
    if (onFilterChange) {
      onFilterChange(updated);
    }
  };

  /**
   * Restablece todos los campos a su estado inicial en blanco.
   */
  const handleClear = () => {
    const reset = {
      text: '',
      category: '',
      startDate: '',
      endDate: '',
      status: '',
    };
    setFilters(reset);
    if (onFilterChange) {
      onFilterChange(reset);
    }
  };

  return (
    <div className="incident-filter-bar" aria-label="Filtros de incidencias">
      <div className="filter-group text-filter">
        <label htmlFor="filter-text" className="filter-label">Buscar:</label>
        <input
          id="filter-text"
          type="text"
          className="filter-input"
          placeholder="Título o descripción..."
          value={filters.text}
          onChange={(e) => handleChange('text', e.target.value)}
        />
      </div>

      <div className="filter-group">
        <label htmlFor="filter-category" className="filter-label">Categoría:</label>
        <select
          id="filter-category"
          className="filter-select"
          value={filters.category}
          onChange={(e) => handleChange('category', e.target.value)}
        >
          <option value="">Todas las categorías</option>
          {INCIDENT_CATEGORIES.map((cat) => (
            <option key={cat} value={cat}>
              {cat}
            </option>
          ))}
        </select>
      </div>

      <div className="filter-group">
        <label htmlFor="filter-start-date" className="filter-label">Desde:</label>
        <input
          id="filter-start-date"
          type="date"
          className="filter-input-date"
          value={filters.startDate}
          onChange={(e) => handleChange('startDate', e.target.value)}
        />
      </div>

      <div className="filter-group">
        <label htmlFor="filter-end-date" className="filter-label">Hasta:</label>
        <input
          id="filter-end-date"
          type="date"
          className="filter-input-date"
          value={filters.endDate}
          onChange={(e) => handleChange('endDate', e.target.value)}
        />
      </div>

      {canFilterByStatus && (
        <div className="filter-group">
          <label htmlFor="filter-status" className="filter-label">Estado:</label>
          <select
            id="filter-status"
            className="filter-select"
            value={filters.status}
            onChange={(e) => handleChange('status', e.target.value)}
          >
            <option value="">Todos los estados</option>
            {INCIDENT_STATUSES.map((st) => (
              <option key={st} value={st}>
                {getStatusLabel(st)}
              </option>
            ))}
          </select>
        </div>
      )}

      <button
        type="button"
        className="filter-clear-btn"
        onClick={handleClear}
      >
        Limpiar filtros
      </button>

      {!hasResults && (
        <span className="filter-no-results" role="status">
          No se encontraron incidencias con los filtros aplicados.
        </span>
      )}
    </div>
  );
}
