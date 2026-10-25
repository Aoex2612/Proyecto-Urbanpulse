/**
 * @fileoverview Vista del Panel de Operador y Gestión de Triaje de Incidencias en UrbanPulse.
 * Proporciona a los perfiles con rol `OPERATOR` y `ADMIN` una interfaz de supervisión,
 * priorización y actualización en tiempo real del estado de incidencias reportadas en la base de datos,
 * así como acceso directo a la vista cartográfica (/map).
 */

import { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from '../services/authService';
import incidentService from '../services/incidentService';
import { formatDateTime } from '../utils/dateUtils';
import { getStatusLabel, getPriorityLabel, getNextAllowedStatuses } from '../components/Map/mapConstants';
import './OperatorPanelPage.css';



/**
 * Componente principal del Panel de Operador / Centro de Triaje.
 *
 * @returns {import('react').JSX.Element} Vista del panel de operador protegida por RBAC conectada a PostgreSQL.
 */
export default function OperatorPanelPage() {
  const navigate = useNavigate();
  const user = authService.getUser();
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [feedbackMessage, setFeedbackMessage] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);

  /**
   * Carga el listado de incidencias activas desde la API backend.
   *
   * @returns {Promise<void>}
   */
  const fetchIncidents = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await incidentService.getAllIncidents();
      setIncidents(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.response?.data?.message || 'Error al conectar con la base de datos de incidencias.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let isMounted = true;

    Promise.resolve().then(() => {
      if (isMounted) {
        fetchIncidents();
      }
    });

    return () => {
      isMounted = false;
    };
  }, [fetchIncidents]);

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
   * Actualiza el estado de una incidencia en el backend con registro de auditoría.
   *
   * @param {number|string} id - Identificador numérico de la incidencia.
   * @param {string} newStatus - Nuevo estado asignado.
   * @returns {Promise<void>}
   */
  const handleStatusChange = async (id, newStatus) => {
    setUpdatingId(id);
    try {
      await incidentService.updateIncidentStatus(id, {
        status: newStatus,
        comment: `Transición a ${newStatus} realizada desde el Centro de Triaje por ${user?.fullName || user?.email}`,
      });
      setIncidents((prev) =>
        prev.map((inc) => (inc.id === id ? { ...inc, status: newStatus } : inc))
      );
      setFeedbackMessage(`Incidencia #${id} actualizada exitosamente al estado '${getStatusLabel(newStatus)}'. Transición registrada en auditoría.`);
    } catch (err) {
      setFeedbackMessage(`Error al actualizar la incidencia #${id}: ${err?.response?.data?.message || 'Operación denegada o error de red.'}`);
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="operator-container">
      <header className="operator-header">
        <div className="operator-brand">
          <h1 className="operator-brand-title">UrbanPulse</h1>
          <span className="operator-badge">Panel de Operador</span>
        </div>
        <div className="operator-header-actions">
          <Link to="/map" className="operator-map-nav-link" title="Explorar incidencias en el mapa">
            Ver Mapa de Incidencias
          </Link>
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
                Supervisión de actuaciones técnicas, validación y priorización de reportes municipales conectados en tiempo real a PostgreSQL.
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
                {incidents.filter((i) => i.status === 'REPORTED').length}
              </span>
              <span className="stat-help">Reportadas sin validar</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">En Tramitación</span>
              <span className="stat-number">
                {incidents.filter((i) => ['VALIDATED', 'ASSIGNED', 'IN_PROGRESS'].includes(i.status)).length}
              </span>
              <span className="stat-help">Equipos de campo asignados</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Resueltas / Cerradas</span>
              <span className="stat-number">
                {incidents.filter((i) => ['RESOLVED', 'CLOSED'].includes(i.status)).length}
              </span>
              <span className="stat-help">Validaciones completadas</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Total en BD</span>
              <span className="stat-number">{incidents.length}</span>
              <span className="stat-help">Incidencias registradas</span>
            </div>
          </div>
        </section>

        <section className="triage-section">
          <div className="section-header">
            <h3>Cola de Incidencias en Base de Datos</h3>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <button
                type="button"
                onClick={fetchIncidents}
                className="back-to-home-link"
                style={{ cursor: 'pointer', padding: '4px 10px', fontSize: '13px' }}
                title="Refrescar listado desde PostgreSQL"
              >
                🔄 Refrescar
              </button>
              <span className="triage-tag-info">
                Ruta protegida por RBAC: Operadores y Administradores
              </span>
            </div>
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

          {loading ? (
            <div style={{ padding: '32px', textAlign: 'center', color: '#6b7280' }}>
              <p>Cargando incidencias desde PostgreSQL...</p>
            </div>
          ) : error ? (
            <div style={{ padding: '24px', backgroundColor: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#b91c1c' }}>
              <p><strong>Error:</strong> {error}</p>
              <button type="button" onClick={fetchIncidents} style={{ marginTop: '8px', padding: '6px 12px', cursor: 'pointer' }}>
                Reintentar
              </button>
            </div>
          ) : incidents.length === 0 ? (
            <div style={{ padding: '32px', textAlign: 'center', color: '#6b7280' }}>
              <p>No hay incidencias registradas en la base de datos en este momento.</p>
              <Link to="/map" style={{ color: '#0284c7', textDecoration: 'underline' }}>
                Ir al mapa para registrar una nueva incidencia
              </Link>
            </div>
          ) : (
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
                      <td className="incident-id">#{incident.id}</td>
                      <td className="incident-title-cell">
                        <div className="incident-title">{incident.title}</div>
                        <p className="incident-desc">{incident.description}</p>
                      </td>
                      <td>{incident.category}</td>
                      <td>
                        <span
                          className={`badge-priority ${(incident.priority || 'medium').toLowerCase()}`}
                        >
                          {getPriorityLabel(incident.priority)}
                        </span>
                      </td>
                      <td>{formatDateTime(incident.createdAt)}</td>
                      <td>
                        {(() => {
                          const allowedNext = getNextAllowedStatuses(incident.status);
                          if (allowedNext.length === 0) {
                            return (
                              <span
                                style={{
                                  display: 'inline-block',
                                  padding: '4px 8px',
                                  borderRadius: '4px',
                                  fontSize: '12px',
                                  fontWeight: '600',
                                  backgroundColor: incident.status === 'REJECTED' ? '#fee2e2' : '#f3f4f6',
                                  color: incident.status === 'REJECTED' ? '#dc2626' : '#4b5563',
                                }}
                              >
                                {getStatusLabel(incident.status)} (Final)
                              </span>
                            );
                          }
                          return (
                            <select
                              aria-label={`Cambiar estado de #${incident.id}`}
                              className="status-select"
                              value=""
                              disabled={updatingId === incident.id}
                              onChange={(e) => {
                                if (e.target.value) {
                                  handleStatusChange(incident.id, e.target.value);
                                }
                              }}
                            >
                              <option value="" disabled>
                                Actual: {getStatusLabel(incident.status)}
                              </option>
                              {allowedNext.map((nextStatus) => (
                                <option key={nextStatus} value={nextStatus}>
                                  Avanzar a: {getStatusLabel(nextStatus)}
                                </option>
                              ))}
                            </select>
                          );
                        })()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
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
