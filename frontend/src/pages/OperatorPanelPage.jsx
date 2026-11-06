/**
 * @fileoverview Vista del Panel de Operador y Gestión de Triaje de Incidencias en UrbanPulse.
 * Proporciona a los perfiles con rol `OPERATOR` y `ADMIN` una interfaz integral de supervisión,
 * priorización asistida, asignación multidepartamental, resolución técnica, reapertura,
 * rechazo y colaboración mediante notas internas y avisos públicos conectados en tiempo real a PostgreSQL.
 */

import { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import authService from '../services/authService';
import incidentService from '../services/incidentService';
import triageService from '../services/triageService';
import { formatDateTime } from '../utils/dateUtils';
import { getStatusLabel, getPriorityLabel } from '../components/Map/mapConstants';
import {
  PriorityAssistantModal,
  AssignmentModal,
  ResolveIncidentModal,
  ReasonModal,
} from '../components/Triage';
import IncidentCommentsSection from '../components/Comments/IncidentCommentsSection';
import './OperatorPanelPage.css';

/**
 * Componente principal del Panel de Operador / Centro de Triaje y Operaciones Urbanas.
 *
 * @returns {import('react').JSX.Element} Vista del panel de operador protegida por RBAC conectada a PostgreSQL.
 */
export default function OperatorPanelPage() {
  const navigate = useNavigate();
  const user = authService.getUser();
  const isTechnician = user?.role === 'TECHNICIAN';
  const isAdmin = user?.role === 'ADMIN';
  const canOperateTriage = isAdmin || user?.role === 'OPERATOR';
  const canExecuteFieldWork = isAdmin || isTechnician;
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [feedbackMessage, setFeedbackMessage] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);

  // Estados para control de modales de triaje
  const [priorityModalIncident, setPriorityModalIncident] = useState(null);
  const [assignmentModalIncident, setAssignmentModalIncident] = useState(null);
  const [resolveModalIncident, setResolveModalIncident] = useState(null);
  const [reasonModalState, setReasonModalState] = useState(null); // { isOpen, incident, actionType }
  const [commentsModalIncident, setCommentsModalIncident] = useState(null);

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
   * Valida directamente una incidencia en estado de reporte inicial (REPORTED -> VALIDATED).
   *
   * @param {number|string} id - Identificador de la incidencia.
   * @returns {Promise<void>}
   */
  const handleValidate = async (id) => {
    setUpdatingId(id);
    try {
      const updated = await triageService.validateIncident(id);
      setIncidents((prev) =>
        prev.map((inc) => (inc.id === id ? { ...inc, ...updated, status: 'VALIDATED' } : inc))
      );
      setFeedbackMessage(`Incidencia #${id} validada formalmente en el circuito de operaciones.`);
    } catch (err) {
      setFeedbackMessage(`Error al validar la incidencia #${id}: ${err?.response?.data?.message || 'Error de servidor'}`);
    } finally {
      setUpdatingId(null);
    }
  };

  /**
   * Inicia los trabajos sobre el terreno (ASSIGNED / REOPENED -> IN_PROGRESS).
   *
   * @param {number|string} id - Identificador de la incidencia.
   * @returns {Promise<void>}
   */
  const handleStartWork = async (id) => {
    setUpdatingId(id);
    try {
      const updated = await triageService.startWork(id);
      setIncidents((prev) =>
        prev.map((inc) => (inc.id === id ? { ...inc, ...updated, status: 'IN_PROGRESS' } : inc))
      );
      setFeedbackMessage(`Trabajos de campo iniciados sobre la incidencia #${id} (estado EN PROGRESO).`);
    } catch (err) {
      setFeedbackMessage(`Error al iniciar trabajos en la incidencia #${id}: ${err?.response?.data?.message || 'Error de servidor'}`);
    } finally {
      setUpdatingId(null);
    }
  };

  /**
   * Cierra de forma administrativa una incidencia resuelta (RESOLVED -> CLOSED).
   *
   * @param {number|string} id - Identificador de la incidencia.
   * @returns {Promise<void>}
   */
  const handleClose = async (id) => {
    setUpdatingId(id);
    try {
      const updated = await triageService.closeIncident(id);
      setIncidents((prev) =>
        prev.map((inc) =>
          inc.id === id
            ? {
                ...inc,
                ...updated,
                status: 'CLOSED',
                assignments: (inc.assignments || []).map((a) => ({ ...a, isActive: false })),
              }
            : inc
        )
      );
      setFeedbackMessage(`Incidencia #${id} cerrada formal y administrativamente.`);
    } catch (err) {
      setFeedbackMessage(`Error al cerrar la incidencia #${id}: ${err?.response?.data?.message || 'Error de servidor'}`);
    } finally {
      setUpdatingId(null);
    }
  };

  /**
   * Abre el modal de motivo para rechazar, reabrir o reevaluar una incidencia.
   *
   * @param {Object} incident - Datos de la incidencia seleccionada.
   * @param {'REJECT'|'REOPEN'|'REEVALUATE'} actionType - Acción que requiere motivo.
   */
  const openReasonModal = (incident, actionType) => {
    setReasonModalState({
      isOpen: true,
      incident,
      actionType,
    });
  };

  /**
   * Procesa la confirmación con motivo del diálogo ReasonModal.
   *
   * @param {Object} data - Datos devueltos por el modal.
   * @param {string} data.reason - Motivo obligatorio aportado por el operador.
   * @param {'REPORTED'|'VALIDATED'} [data.targetStatus] - Estado de destino en reevaluaciones.
   * @returns {Promise<void>}
   */
  const handleReasonSubmit = async ({ reason, targetStatus }) => {
    if (!reasonModalState?.incident) return;
    const { incident, actionType } = reasonModalState;

    if (actionType === 'REJECT') {
      const updated = await triageService.rejectIncident(incident.id, { reason });
      setIncidents((prev) =>
        prev.map((item) =>
          item.id === incident.id
            ? {
                ...item,
                ...updated,
                status: 'REJECTED',
                assignments: (item.assignments || []).map((a) => ({ ...a, isActive: false })),
              }
            : item
        )
      );
      setFeedbackMessage(`Incidencia #${incident.id} rechazada y asignaciones inactivadas.`);
    } else if (actionType === 'REOPEN') {
      const updated = await triageService.reopenIncident(incident.id, { reason });
      setIncidents((prev) =>
        prev.map((item) => (item.id === incident.id ? { ...item, ...updated, status: 'REOPENED' } : item))
      );
      setFeedbackMessage(`Incidencia #${incident.id} reabierta para nuevas actuaciones.`);
    } else if (actionType === 'REEVALUATE') {
      const updated = await triageService.reevaluateIncident(incident.id, { targetStatus, reason });
      setIncidents((prev) =>
        prev.map((item) => (item.id === incident.id ? { ...item, ...updated, status: targetStatus } : item))
      );
      setFeedbackMessage(`Incidencia #${incident.id} reevaluada y reactivada en estado '${getStatusLabel(targetStatus)}'.`);
    }
  };

  /**
   * Actualiza la incidencia tras guardar una nueva prioridad en PriorityAssistantModal.
   *
   * @param {Object} updatedIncident - Objeto de la incidencia con prioridad actualizada.
   */
  const handlePriorityUpdated = (updatedIncident) => {
    setIncidents((prev) =>
      prev.map((inc) => (inc.id === updatedIncident.id ? { ...inc, ...updatedIncident } : inc))
    );
    setFeedbackMessage(
      `Prioridad de la incidencia #${updatedIncident.id} actualizada exitosamente a '${getPriorityLabel(
        updatedIncident.priority
      )}'.`
    );
    setPriorityModalIncident(null);
  };

  /**
   * Actualiza el listado reactivo de asignaciones tras interactuar con AssignmentModal.
   *
   * @param {Array<Object>} updatedAssignments - Lista de asignaciones activas resultantes.
   */
  const handleAssignmentsUpdated = (updatedAssignments) => {
    if (!assignmentModalIncident) return;
    const incidentId = assignmentModalIncident.id;
    const activeCount = (updatedAssignments || []).filter((a) => a.isActive !== false).length;

    setIncidents((prev) =>
      prev.map((inc) => {
        if (inc.id !== incidentId) return inc;
        let nextStatus = inc.status;
        if (inc.status === 'VALIDATED' && activeCount > 0) {
          nextStatus = 'ASSIGNED';
        } else if (inc.status === 'ASSIGNED' && activeCount === 0) {
          nextStatus = 'VALIDATED';
        }
        return {
          ...inc,
          assignments: updatedAssignments,
          status: nextStatus,
        };
      })
    );
    setFeedbackMessage(`Asignaciones de la incidencia #${incidentId} actualizadas exitosamente.`);
    setAssignmentModalIncident(null);
  };

  /**
   * Actualiza el estado reactivo tras resolver una incidencia con memoria técnica.
   *
   * @param {Object} updatedIncident - Incidencia en estado RESOLVED.
   */
  const handleIncidentResolved = (updatedIncident) => {
    setIncidents((prev) =>
      prev.map((inc) => (inc.id === updatedIncident.id ? { ...inc, ...updatedIncident, status: 'RESOLVED' } : inc))
    );
    setFeedbackMessage(`Incidencia #${updatedIncident.id} resuelta técnicamente con memoria de actuaciones registrada.`);
    setResolveModalIncident(null);
  };

  /**
   * Obtiene la lista de asignaciones activas para una incidencia.
   *
   * @param {Object} incident - Objeto de la incidencia.
   * @returns {Array<Object>} Lista de asignaciones activas.
   */
  const getActiveAssignments = (incident) => {
    if (!Array.isArray(incident.assignments)) return [];
    return incident.assignments.filter((a) => a.isActive !== false);
  };

  return (
    <div className="operator-container">
      <header className="operator-header">
        <div className="operator-brand">
          <h1 className="operator-brand-title">UrbanPulse</h1>
          <span className="operator-badge">{isTechnician ? 'Técnico de Campo' : 'Panel de Operador'}</span>
        </div>
        <div className="operator-header-actions">
          <Link to="/map" className="operator-map-nav-link" title="Explorar incidencias en el mapa">
            Ver Mapa de Incidencias
          </Link>
          <Link to="/my-incidents" className="operator-my-incidents-nav-link" title="Ver mis incidencias">
            Mis Incidencias
          </Link>
          <Link to="/" className="back-to-home-link">
            &larr; Portal Ciudadano
          </Link>
          <button type="button" onClick={handleLogout} className="logout-button">
            Cerrar sesión
          </button>
        </div>
      </header>

      <main className="operator-main">
        <section className="operator-overview-card">
          <div className="overview-header">
            <div className="overview-title-group">
              <h2>{isTechnician ? 'Mis Asignaciones Técnicas' : 'Centro de Triaje y Operaciones Urbanas'}</h2>
              <p className="overview-subtitle">
                {isTechnician
                  ? 'Incidencias asignadas para intervención y resolución técnica en campo.'
                  : 'Supervisión de actuaciones técnicas, validación y priorización de reportes municipales conectados en tiempo real a PostgreSQL.'}
              </p>
            </div>
            <div className="user-badge-info">
              <span className="user-badge-name">{user?.fullName || (isTechnician ? 'Técnico Municipal' : 'Operador Municipal')}</span>
              <span className="user-badge-role">{user?.role || (isTechnician ? 'TECHNICIAN' : 'OPERATOR')}</span>
            </div>
          </div>

          {isTechnician ? (
            <div className="stats-grid">
              <div className="stat-card">
                <span className="stat-label">Pendientes de Inicio</span>
                <span className="stat-number">
                  {incidents.filter((i) => ['ASSIGNED', 'REOPENED'].includes(i.status)).length}
                </span>
                <span className="stat-help">Asignadas para campo</span>
              </div>
              <div className="stat-card">
                <span className="stat-label">En Intervención</span>
                <span className="stat-number">
                  {incidents.filter((i) => i.status === 'IN_PROGRESS').length}
                </span>
                <span className="stat-help">Trabajos en curso</span>
              </div>
              <div className="stat-card">
                <span className="stat-label">Resueltas</span>
                <span className="stat-number">
                  {incidents.filter((i) => ['RESOLVED', 'CLOSED'].includes(i.status)).length}
                </span>
                <span className="stat-help">Completadas en campo</span>
              </div>
              <div className="stat-card">
                <span className="stat-label">Total Asignadas</span>
                <span className="stat-number">{incidents.length}</span>
                <span className="stat-help">Incidencias en tu cola</span>
              </div>
            </div>
          ) : (
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
                  {
                    incidents.filter((i) => ['VALIDATED', 'ASSIGNED', 'IN_PROGRESS', 'REOPENED'].includes(i.status))
                      .length
                  }
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
          )}
        </section>

        <section className="triage-section">
          <div className="section-header">
            <h3>{isTechnician ? 'Mis Incidencias Asignadas' : 'Cola de Incidencias en Base de Datos'}</h3>
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
                &times;
              </button>
            </div>
          )}

          {loading ? (
            <div style={{ padding: '32px', textAlign: 'center', color: '#6b7280' }}>
              <p>Cargando incidencias desde PostgreSQL...</p>
            </div>
          ) : error ? (
            <div
              style={{
                padding: '24px',
                backgroundColor: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: '8px',
                color: '#b91c1c',
              }}
            >
              <p>
                <strong>Error:</strong> {error}
              </p>
              <button
                type="button"
                onClick={fetchIncidents}
                style={{ marginTop: '8px', padding: '6px 12px', cursor: 'pointer' }}
              >
                Reintentar
              </button>
            </div>
          ) : incidents.length === 0 ? (
            <div style={{ padding: '32px', textAlign: 'center', color: '#6b7280' }}>
              <p>{isTechnician ? 'No tienes incidencias asignadas en este momento.' : 'No hay incidencias registradas en la base de datos en este momento.'}</p>
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
                    <th>Departamentos</th>
                    <th>Fecha</th>
                    <th>Estado de Triaje</th>
                    <th>Acciones Operativas</th>
                  </tr>
                </thead>
                <tbody>
                  {incidents.map((incident) => {
                    const activeAssignments = getActiveAssignments(incident);
                    const isProcessing = updatingId === incident.id;

                    return (
                      <tr key={incident.id} className={`incident-row incident-status-${incident.status?.toLowerCase()}`}>
                        <td className="incident-id">#{incident.id}</td>
                        <td className="incident-title-cell">
                          <div className="incident-title">{incident.title}</div>
                          <p className="incident-desc">{incident.description}</p>
                        </td>
                        <td>{incident.category}</td>
                        <td>
                          <div className="priority-cell-wrapper">
                            <span className={`badge-priority ${(incident.priority || 'medium').toLowerCase()}`}>
                              {getPriorityLabel(incident.priority)}
                            </span>
                            {canOperateTriage && (
                              <button
                                type="button"
                                className="btn-priority-assistant"
                                onClick={() => setPriorityModalIncident(incident)}
                                title="Calcular o modificar prioridad asistida"
                                aria-label={`Prioridad asistida para incidencia #${incident.id}`}
                              >
                                ⚡ Asistente
                              </button>
                            )}
                          </div>
                        </td>
                        <td>
                          {activeAssignments.length > 0 ? (
                            <div className="dept-badges-container">
                              {activeAssignments.map((a, idx) => (
                                <span
                                  key={a.id || idx}
                                  className="badge-dept"
                                  title={a.technicianName ? `Técnico: ${a.technicianName}` : 'Departamento activo'}
                                >
                                  {a.departmentName || `Dpto. #${a.departmentId}`}
                                  {a.technicianName && <span className="badge-dept-tech"> · {a.technicianName}</span>}
                                </span>
                              ))}
                            </div>
                          ) : (
                            <span className="badge-unassigned">Sin asignar</span>
                          )}
                        </td>
                        <td>{formatDateTime(incident.createdAt)}</td>
                        <td>
                          <span className={`badge-status badge-status-${incident.status?.toLowerCase()}`}>
                            {getStatusLabel(incident.status)}
                          </span>
                        </td>
                        <td className="triage-actions-cell">
                          <div className="action-buttons-group">
                            {/* Botones contextuales según estado */}
                            {incident.status === 'REPORTED' && canOperateTriage && (
                              <>
                                <button
                                  type="button"
                                  className="btn-action-triage btn-triage-validate"
                                  onClick={() => handleValidate(incident.id)}
                                  disabled={isProcessing}
                                  aria-label={`Validar incidencia #${incident.id}`}
                                >
                                  Validar
                                </button>
                                <button
                                  type="button"
                                  className="btn-action-triage btn-triage-reject"
                                  onClick={() => openReasonModal(incident, 'REJECT')}
                                  disabled={isProcessing}
                                  aria-label={`Rechazar incidencia #${incident.id}`}
                                >
                                  Rechazar
                                </button>
                              </>
                            )}

                            {incident.status === 'VALIDATED' && canOperateTriage && (
                              <>
                                <button
                                  type="button"
                                  className="btn-action-triage btn-triage-assign"
                                  onClick={() => setAssignmentModalIncident(incident)}
                                  disabled={isProcessing}
                                  aria-label={`Asignar departamentos a #${incident.id}`}
                                >
                                  Asignar Departamentos/Técnicos
                                </button>
                                <button
                                  type="button"
                                  className="btn-action-triage btn-triage-reevaluate"
                                  onClick={() => openReasonModal(incident, 'REEVALUATE')}
                                  disabled={isProcessing}
                                  aria-label={`Reevaluar incidencia #${incident.id}`}
                                >
                                  Reevaluar
                                </button>
                                <button
                                  type="button"
                                  className="btn-action-triage btn-triage-reject"
                                  onClick={() => openReasonModal(incident, 'REJECT')}
                                  disabled={isProcessing}
                                  aria-label={`Rechazar incidencia #${incident.id}`}
                                >
                                  Rechazar
                                </button>
                              </>
                            )}

                            {(incident.status === 'ASSIGNED' || incident.status === 'REOPENED') && (
                              <>
                                {canExecuteFieldWork && (
                                  <button
                                    type="button"
                                    className="btn-action-triage btn-triage-start"
                                    onClick={() => handleStartWork(incident.id)}
                                    disabled={isProcessing}
                                    aria-label={`Iniciar trabajo en incidencia #${incident.id}`}
                                  >
                                    Iniciar Trabajo
                                  </button>
                                )}
                                {canOperateTriage && (
                                  <>
                                    <button
                                      type="button"
                                      className="btn-action-triage btn-triage-assign"
                                      onClick={() => setAssignmentModalIncident(incident)}
                                      disabled={isProcessing}
                                      aria-label={`Asignar o reasignar departamentos a #${incident.id}`}
                                    >
                                      Asignar / Reasignar
                                    </button>
                                    <button
                                      type="button"
                                      className="btn-action-triage btn-triage-reject"
                                      onClick={() => openReasonModal(incident, 'REJECT')}
                                      disabled={isProcessing}
                                      aria-label={`Rechazar incidencia #${incident.id}`}
                                    >
                                      Rechazar
                                    </button>
                                  </>
                                )}
                              </>
                            )}

                            {incident.status === 'IN_PROGRESS' && (
                              <>
                                {canExecuteFieldWork && (
                                  <button
                                    type="button"
                                    className="btn-action-triage btn-triage-resolve"
                                    onClick={() => setResolveModalIncident(incident)}
                                    disabled={isProcessing}
                                    aria-label={`Resolver incidencia #${incident.id}`}
                                  >
                                    Resolver
                                  </button>
                                )}
                                {canOperateTriage && (
                                  <button
                                    type="button"
                                    className="btn-action-triage btn-triage-assign"
                                    onClick={() => setAssignmentModalIncident(incident)}
                                    disabled={isProcessing}
                                    aria-label={`Asignar o reasignar departamentos a #${incident.id}`}
                                  >
                                    Asignar / Reasignar
                                  </button>
                                )}
                              </>
                            )}

                            {incident.status === 'RESOLVED' && canOperateTriage && (
                              <>
                                <button
                                  type="button"
                                  className="btn-action-triage btn-triage-close"
                                  onClick={() => handleClose(incident.id)}
                                  disabled={isProcessing}
                                  aria-label={`Cerrar incidencia #${incident.id}`}
                                >
                                  Cerrar
                                </button>
                                <button
                                  type="button"
                                  className="btn-action-triage btn-triage-reopen"
                                  onClick={() => openReasonModal(incident, 'REOPEN')}
                                  disabled={isProcessing}
                                  aria-label={`Reabrir incidencia #${incident.id}`}
                                >
                                  Reabrir
                                </button>
                              </>
                            )}

                            {incident.status === 'REJECTED' && canOperateTriage && (
                              <button
                                type="button"
                                className="btn-action-triage btn-triage-reevaluate"
                                onClick={() => openReasonModal(incident, 'REEVALUATE')}
                                disabled={isProcessing}
                                aria-label={`Reevaluar incidencia rechazada #${incident.id}`}
                              >
                                Reevaluar
                              </button>
                            )}

                            {/* Botón universal para consultar y gestionar comentarios y notas */}
                            <button
                              type="button"
                              className="btn-action-triage btn-triage-comments"
                              onClick={() => setCommentsModalIncident(incident)}
                              aria-label={`Ver y redactar comentarios de la incidencia #${incident.id}`}
                            >
                              💬 Comentarios
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>

      {/* Modal 1: Asistente de Prioridad Asistida */}
      {priorityModalIncident && (
        <PriorityAssistantModal
          isOpen={Boolean(priorityModalIncident)}
          incident={priorityModalIncident}
          onClose={() => setPriorityModalIncident(null)}
          onPriorityUpdated={handlePriorityUpdated}
          onConfirm={handlePriorityUpdated}
        />
      )}

      {/* Modal 2: Asignación Multidepartamental y de Técnicos */}
      {assignmentModalIncident && (
        <AssignmentModal
          isOpen={Boolean(assignmentModalIncident)}
          incident={assignmentModalIncident}
          onClose={() => setAssignmentModalIncident(null)}
          onAssignmentsUpdated={handleAssignmentsUpdated}
        />
      )}

      {/* Modal 3: Reporte de Resolución Técnica */}
      {resolveModalIncident && (
        <ResolveIncidentModal
          isOpen={Boolean(resolveModalIncident)}
          incident={resolveModalIncident}
          onClose={() => setResolveModalIncident(null)}
          onIncidentResolved={handleIncidentResolved}
        />
      )}

      {/* Modal 4: Captura de Motivo Obligatorio (Rechazar, Reabrir, Reevaluar) */}
      {reasonModalState?.isOpen && (
        <ReasonModal
          isOpen={Boolean(reasonModalState?.isOpen)}
          incident={reasonModalState?.incident}
          actionType={reasonModalState?.actionType}
          onClose={() => setReasonModalState(null)}
          onSubmit={handleReasonSubmit}
        />
      )}

      {/* Modal 5: Muro de Comentarios y Actuaciones */}
      {commentsModalIncident && (
        <div
          className="modal-backdrop comments-modal-backdrop"
          role="presentation"
          onClick={(e) => {
            if (e.target === e.currentTarget) {
              setCommentsModalIncident(null);
            }
          }}
        >
          <div
            className="modal-card comments-modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="comments-modal-title"
          >
            <header className="modal-header comments-modal-header">
              <div>
                <h3 id="comments-modal-title" className="modal-title">
                  Muro de Comentarios — Incidencia #{commentsModalIncident.id}
                </h3>
                <p className="modal-subtitle">
                  {commentsModalIncident.title} &bull; Estado: {getStatusLabel(commentsModalIncident.status)}
                </p>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setCommentsModalIncident(null)}
                aria-label="Cerrar muro de comentarios"
              >
                &times;
              </button>
            </header>
            <div className="comments-modal-body">
              <IncidentCommentsSection
                incidentId={commentsModalIncident.id}
                incidentStatus={commentsModalIncident.status}
                isClosedOrRejected={['CLOSED', 'REJECTED'].includes(commentsModalIncident.status)}
                currentUser={user}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
