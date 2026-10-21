/**
 * @fileoverview Vista del Panel de Operador y Gestión de Triaje de Incidencias en UrbanPulse.
 * Proporciona a los perfiles con rol `OPERATOR` y `ADMIN` una interfaz de supervisión,
 * priorización y actualización del estado de incidencias reportadas en la vía pública.
 */

import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from '../services/authService';
import './OperatorPanelPage.css';

/**
 * Listado inicial simulado de incidencias pendientes de triaje operativo.
 *
 * @type {Array<{id: string, title: string, description: string, category: string, priority: 'ALTA'|'MEDIA'|'BAJA', status: string, reportedAt: string}>}
 */
const INITIAL_INCIDENTS = [
  {
    id: 'INC-2026-001',
    title: 'Avería en farolas de alumbrado público',
    description: 'Tramo peatonal a oscuras entre Plaza Mayor y C/ Granada.',
    category: 'Alumbrado',
    priority: 'ALTA',
    status: 'PENDIENTE',
    reportedAt: '21/10/2026 14:30',
  },
  {
    id: 'INC-2026-002',
    title: 'Bache de gran profundidad en calzada',
    description: 'Desperfecto en asfalto con riesgo para motoristas y ciclistas.',
    category: 'Vía Pública',
    priority: 'ALTA',
    status: 'EN_PROCESO',
    reportedAt: '21/10/2026 15:10',
  },
  {
    id: 'INC-2026-003',
    title: 'Contenedor de reciclaje desbordado',
    description: 'Residuos acumulados en la acera impidiendo paso de personas.',
    category: 'Limpieza',
    priority: 'MEDIA',
    status: 'PENDIENTE',
    reportedAt: '21/10/2026 16:05',
  },
  {
    id: 'INC-2026-004',
    title: 'Semáforo intermitente fuera de sincronía',
    description: 'Cruce semafórico en ámbar intermitente causando retenciones.',
    category: 'Movilidad',
    priority: 'ALTA',
    status: 'PENDIENTE',
    reportedAt: '21/10/2026 16:45',
  },
];

/**
 * Componente principal del Panel de Operador / Centro de Triaje.
 *
 * @returns {import('react').JSX.Element} Vista del panel de operador protegida por RBAC.
 */
export default function OperatorPanelPage() {
  const navigate = useNavigate();
  const user = authService.getUser();
  const [incidents, setIncidents] = useState(INITIAL_INCIDENTS);
  const [feedbackMessage, setFeedbackMessage] = useState(null);

  /**
   * Manejador para el cierre de sesión seguro del operador.
   * Elimina el token y datos de usuario en localStorage y redirige al login.
   *
   * @returns {void}
   */
  const handleLogout = () => {
    authService.logout();
    navigate('/login', { replace: true });
  };

  /**
   * Actualiza el estado de una incidencia en la interfaz informativa de triaje.
   *
   * @param {string} id - Identificador de la incidencia.
   * @param {string} newStatus - Nuevo estado asignado (PENDIENTE, EN_PROCESO, RESUELTA, DESCARTADA).
   * @returns {void}
   */
  const handleStatusChange = (id, newStatus) => {
    setIncidents((prev) =>
      prev.map((inc) => (inc.id === id ? { ...inc, status: newStatus } : inc))
    );
    setFeedbackMessage(`Incidencia ${id} actualizada con éxito al estado '${newStatus}'.`);
  };

  return (
    <div className="operator-container">
      <header className="operator-header">
        <div className="operator-brand">
          <h1 className="operator-brand-title">UrbanPulse</h1>
          <span className="operator-badge">Panel de Operador</span>
        </div>
        <div className="operator-header-actions">
          <Link to="/" className="back-to-home-link">
            &larr; Portal Ciudadano
          </Link>
          <button
            type="button"
            onClick={handleLogout}
            className="logout-button"
          >
            Cerrar sesión
          </button>
        </div>
      </header>

      <main className="operator-main">
        <section className="operator-overview-card">
          <div className="overview-header">
            <div className="overview-title-group">
              <h2>Centro de Triaje y Operaciones Urbanas</h2>
              <p className="overview-subtitle">
                Supervisión de actuaciones técnicas, validación y priorización de reportes municipales.
              </p>
            </div>
            <div className="user-badge-info">
              <span className="user-badge-name">{user?.fullName || 'Operador Municipal'}</span>
              <span className="user-badge-role">{user?.role || 'OPERATOR'}</span>
            </div>
          </div>

          <div className="stats-grid">
            <div className="stat-card">
              <span className="stat-label">Pendientes de Triaje</span>
              <span className="stat-number">
                {incidents.filter((i) => i.status === 'PENDIENTE').length}
              </span>
              <span className="stat-help">Requieren revisión técnica</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">En Tramitación</span>
              <span className="stat-number">
                {incidents.filter((i) => i.status === 'EN_PROCESO').length}
              </span>
              <span className="stat-help">Equipos de campo asignados</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Resueltas Hoy</span>
              <span className="stat-number">
                {incidents.filter((i) => i.status === 'RESUELTA').length}
              </span>
              <span className="stat-help">Validaciones completadas</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Control RBAC</span>
              <span className="stat-number">Activo</span>
              <span className="stat-help">OPERATOR / ADMIN</span>
            </div>
          </div>
        </section>

        <section className="triage-section">
          <div className="section-header">
            <h3>Cola de Incidencias para Triaje</h3>
            <span className="triage-tag-info">
              Ruta protegida por RBAC: Operadores y Administradores
            </span>
          </div>

          {feedbackMessage && (
            <div className="feedback-banner" role="status">
              <span>{feedbackMessage}</span>
              <button
                type="button"
                className="feedback-dismiss-btn"
                onClick={() => setFeedbackMessage(null)}
                aria-label="Cerrar notificación"
              >
                ×
              </button>
            </div>
          )}

          <div className="incidents-table-wrapper">
            <table className="incidents-table">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Incidencia</th>
                  <th>Categoría</th>
                  <th>Prioridad</th>
                  <th>Fecha</th>
                  <th>Estado de Triaje</th>
                </tr>
              </thead>
              <tbody>
                {incidents.map((incident) => (
                  <tr key={incident.id}>
                    <td className="incident-id">{incident.id}</td>
                    <td className="incident-title-cell">
                      <div className="incident-title">{incident.title}</div>
                      <p className="incident-desc">{incident.description}</p>
                    </td>
                    <td>{incident.category}</td>
                    <td>
                      <span
                        className={`badge-priority ${incident.priority.toLowerCase()}`}
                      >
                        {incident.priority}
                      </span>
                    </td>
                    <td>{incident.reportedAt}</td>
                    <td>
                      <select
                        aria-label={`Cambiar estado de ${incident.id}`}
                        className="status-select"
                        value={incident.status}
                        onChange={(e) =>
                          handleStatusChange(incident.id, e.target.value)
                        }
                      >
                        <option value="PENDIENTE">PENDIENTE</option>
                        <option value="EN_PROCESO">EN PROCESO</option>
                        <option value="RESUELTA">RESUELTA</option>
                        <option value="DESCARTADA">DESCARTADA</option>
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        <aside className="security-notice-card">
          <h4>Control de Seguridad Perimetral y RBAC</h4>
          <p>
            Esta vista y sus operaciones asociadas están restringidas exclusivamente a usuarios
            con roles <strong>OPERATOR</strong> o <strong>ADMIN</strong>. Las peticiones a
            la API backend (<code>PATCH /api/incidents/:id/status</code>) son validadas
            criptográficamente mediante JWT y denegadas con HTTP 403 Forbidden para cualquier otro rol.
          </p>
        </aside>
      </main>
    </div>
  );
}
