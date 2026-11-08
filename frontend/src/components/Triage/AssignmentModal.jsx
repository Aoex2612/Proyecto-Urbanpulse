/**
 * @fileoverview Modal de Asignación Multidepartamental y Técnica para UrbanPulse.
 * Permite a operadores y administradores asignar uno o más departamentos municipales
 * a una incidencia y vincular técnicos especialistas pertenecientes a dichas áreas.
 * Soporta también la visualización y desactivación lógica de asignaciones activas existentes.
 */

import { useState, useEffect, useRef, useCallback } from 'react';
import triageService from '../../services/triageService';
import analyticsService from '../../services/analyticsService';
import './AssignmentModal.css';

/**
 * Componente modal accesible para la gestión de asignaciones de departamentos y técnicos.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {boolean} [props.isOpen=true] - Controla la visibilidad del modal.
 * @param {Function} props.onClose - Callback ejecutado al solicitar el cierre del modal.
 * @param {Object|null} props.incident - Datos de la incidencia a asignar.
 * @param {Function} [props.onAssignmentsUpdated] - Callback emitido con las asignaciones resultantes.
 * @returns {import('react').JSX.Element|null} Elemento JSX del modal o null si está cerrado.
 */
export default function AssignmentModal({
  isOpen = true,
  onClose,
  incident,
  onAssignmentsUpdated,
}) {
  const [departments, setDepartments] = useState([]);
  const [selectedDeptIds, setSelectedDeptIds] = useState([]);
  const [techniciansByDept, setTechniciansByDept] = useState({});
  const [loadingTechsByDept, setLoadingTechsByDept] = useState({});
  const [selectedTechIds, setSelectedTechIds] = useState([]);

  const [activeAssignments, setActiveAssignments] = useState([]);
  const [loadingDepts, setLoadingDepts] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deactivatingId, setDeactivatingId] = useState(null);
  const [error, setError] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [recommendation, setRecommendation] = useState(null);

  const closeBtnRef = useRef(null);

  // Sincronizar asignaciones activas desde la incidencia recibida y consultar API
  useEffect(() => {
    if (!isOpen || !incident) {
      return;
    }

    let isMounted = true;

    Promise.resolve().then(() => {
      if (isMounted) {
        const initialActive = (incident.assignments || []).filter((a) => a.isActive !== false);
        setActiveAssignments(initialActive);
        setSelectedDeptIds([]);
        setSelectedTechIds([]);
        setError(null);
        setFeedback(null);
        setRecommendation(null);
      }
    });

    if (incident.id && typeof triageService.getIncidentAssignments === 'function') {
      triageService
        .getIncidentAssignments(incident.id)
        .then((data) => {
          if (isMounted && Array.isArray(data)) {
            setActiveAssignments(data.filter((a) => a.isActive !== false));
          }
        })
        .catch(() => {
          // Ignorar si falla, conservando el estado inicial
        });
    }

    return () => {
      isMounted = false;
    };
  }, [isOpen, incident]);

  // Consultar recomendación de departamento asistida por analítica (RF-15, RF-16, RF-17)
  useEffect(() => {
    if (!isOpen || !incident) {
      return;
    }

    let isMounted = true;

    analyticsService
      .getDepartmentRecommendation({
        incidentId: incident.id,
        title: incident.title,
        description: incident.description,
        category: incident.category,
      })
      .then((data) => {
        if (!isMounted || !data) return;
        setRecommendation(data);
        if (data.suggestedDepartmentId != null) {
          const suggestedId = Number(data.suggestedDepartmentId);
          const alreadyAssigned = (incident.assignments || []).some(
            (a) => a.isActive !== false && Number(a.departmentId) === suggestedId
          );
          if (!alreadyAssigned) {
            setSelectedDeptIds((prev) =>
              prev.includes(suggestedId) ? prev : [...prev, suggestedId]
            );
            // Pre-cargar técnicos para el departamento sugerido
            triageService
              .getTechniciansByDepartment(suggestedId)
              .then((techs) => {
                if (isMounted) {
                  setTechniciansByDept((prev) => ({
                    ...prev,
                    [suggestedId]: techs || [],
                  }));
                }
              })
              .catch(() => {});
          }
        }
      })
      .catch(() => {
        // En caso de error en el motor de analítica, degradación controlada sin bloquear el modal
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen, incident]);

  // Cargar catálogo de departamentos al abrir
  useEffect(() => {
    if (!isOpen || !incident) {
      return;
    }

    let isMounted = true;

    Promise.resolve().then(() => {
      if (!isMounted) return;
      setLoadingDepts(true);
      setError(null);

      triageService
        .getDepartments()
        .then((data) => {
          if (isMounted) {
            setDepartments(Array.isArray(data) ? data : []);
          }
        })
        .catch((err) => {
          if (isMounted) {
            setError(
              err?.response?.data?.message ||
                'Error al cargar el catálogo de departamentos municipales.'
            );
          }
        })
        .finally(() => {
          if (isMounted) {
            setLoadingDepts(false);
          }
        });
    });

    return () => {
      isMounted = false;
    };
  }, [isOpen, incident]);

  // Manejo de cierre memorizado
  const handleDismiss = useCallback(() => {
    if (!saving && !deactivatingId && onClose) {
      onClose();
    }
  }, [saving, deactivatingId, onClose]);

  // Cierre mediante tecla Escape y autofoco
  useEffect(() => {
    if (isOpen && incident) {
      const timer = setTimeout(() => {
        closeBtnRef.current?.focus();
      }, 50);

      /**
       * Listener de teclado para capturar Escape.
       *
       * @param {KeyboardEvent} e - Evento nativo.
       */
      const handleKeyDown = (e) => {
        if (e.key === 'Escape') {
          handleDismiss();
        }
      };

      window.addEventListener('keydown', handleKeyDown);
      return () => {
        clearTimeout(timer);
        window.removeEventListener('keydown', handleKeyDown);
      };
    }
  }, [isOpen, incident, handleDismiss]);

  if (!isOpen || !incident) {
    return null;
  }

  /**
   * Clic en el fondo semitransparente para descartar el modal.
   *
   * @param {import('react').MouseEvent} e - Evento de clic.
   */
  const handleBackdropClick = (e) => {
    if (e.target === e.currentTarget) {
      handleDismiss();
    }
  };

  /**
   * Conmuta la selección de un departamento y carga sus técnicos bajo demanda.
   *
   * @param {number} deptId - Identificador único del departamento.
   */
  const toggleDepartment = async (deptId) => {
    setError(null);
    if (selectedDeptIds.includes(deptId)) {
      // Deseleccionar departamento
      setSelectedDeptIds((prev) => prev.filter((id) => id !== deptId));
      // Deseleccionar técnicos de este departamento
      const deptTechIds = (techniciansByDept[deptId] || []).map((t) => t.id);
      setSelectedTechIds((prev) => prev.filter((id) => !deptTechIds.includes(id)));
    } else {
      // Seleccionar departamento
      setSelectedDeptIds((prev) => [...prev, deptId]);

      // Si no tenemos los técnicos en caché, cargarlos
      if (!techniciansByDept[deptId]) {
        setLoadingTechsByDept((prev) => ({ ...prev, [deptId]: true }));
        try {
          const techs = await triageService.getTechniciansByDepartment(deptId);
          setTechniciansByDept((prev) => ({ ...prev, [deptId]: techs || [] }));
        } catch (err) {
          setError(
            err?.response?.data?.message ||
              `Error al cargar los técnicos del departamento #${deptId}.`
          );
        } finally {
          setLoadingTechsByDept((prev) => ({ ...prev, [deptId]: false }));
        }
      }
    }
  };

  /**
   * Conmuta la selección de un técnico para asignación.
   *
   * @param {number} techId - Identificador del técnico.
   */
  const toggleTechnician = (techId) => {
    setSelectedTechIds((prev) =>
      prev.includes(techId) ? prev.filter((id) => id !== techId) : [...prev, techId]
    );
  };

  /**
   * Desactiva lógicamente una asignación activa existente de la incidencia.
   *
   * @param {Object} assignment - Objeto de asignación a desactivar.
   */
  const handleDeactivateAssignment = async (assignment) => {
    setDeactivatingId(assignment.id);
    setError(null);
    try {
      const payload = {
        departmentIds: [assignment.departmentId],
        technicianIds: assignment.technicianId ? [assignment.technicianId] : [],
      };
      const updated = await triageService.deactivateAssignments(incident.id, payload);
      const active = (updated || []).filter((a) => a.isActive !== false);
      setActiveAssignments(active);
      setFeedback('Asignación desactivada exitosamente.');
      if (onAssignmentsUpdated) {
        onAssignmentsUpdated(updated);
      }
    } catch (err) {
      setError(
        err?.response?.data?.message || 'Error al desactivar la asignación de la incidencia.'
      );
    } finally {
      setDeactivatingId(null);
    }
  };

  /**
   * Valida y guarda las nuevas asignaciones multidepartamentales.
   *
   * @param {import('react').FormEvent} e - Evento de envío.
   */
  const handleSaveAssignments = async (e) => {
    e.preventDefault();
    if (selectedDeptIds.length === 0) {
      setError('Debe seleccionar al menos un departamento municipal para asignar.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await triageService.assignDepartmentsAndTechnicians(incident.id, {
        departmentIds: selectedDeptIds,
        technicianIds: selectedTechIds,
      });
      if (onAssignmentsUpdated) {
        onAssignmentsUpdated(updated);
      }
      if (onClose) {
        onClose();
      }
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          'Error al guardar las asignaciones. Asegúrese de que la incidencia permite asignación.'
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div
      className="modal-backdrop"
      onClick={handleBackdropClick}
      role="presentation"
    >
      <div
        className="assignment-modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="assignment-modal-title"
      >
        {/* Cabecera */}
        <header className="assignment-modal-header">
          <div>
            <span className="assignment-modal-eyebrow">
              Triaje Operativo &bull; Incidencia #{incident.id}
            </span>
            <h2 id="assignment-modal-title" className="assignment-modal-title">
              Asignación de Departamentos y Técnicos
            </h2>
          </div>
          <button
            ref={closeBtnRef}
            type="button"
            className="modal-close-btn"
            onClick={handleDismiss}
            aria-label="Cerrar modal de asignación"
          >
            &times;
          </button>
        </header>

        {/* Formulario */}
        <form onSubmit={handleSaveAssignments}>
          <div className="assignment-modal-body">
            {error && <div className="priority-error-banner" role="alert">{error}</div>}
            {feedback && <div className="assignment-feedback-banner" role="status">{feedback}</div>}

            {/* Asignaciones activas actuales si existen */}
            {activeAssignments.length > 0 && (
              <div className="assignment-section">
                <div className="assignment-section-header">
                  <h3 className="assignment-section-title">Asignaciones Activas Actuales</h3>
                  <span className="assignment-section-hint">
                    {activeAssignments.length} asignación(es) vigente(s)
                  </span>
                </div>
                <div className="active-assignments-list">
                  {activeAssignments.map((assign) => (
                    <div key={assign.id} className="active-assignment-item">
                      <div className="active-assignment-info">
                        <span className="active-assignment-dept">
                          {assign.departmentName || `Departamento #${assign.departmentId}`}
                        </span>
                        <span className="active-assignment-tech">
                          {assign.technicianName
                            ? `Técnico asignado: ${assign.technicianName}`
                            : 'Sin técnico individual asignado'}
                        </span>
                      </div>
                      <button
                        type="button"
                        className="active-assignment-deactivate-btn"
                        onClick={() => handleDeactivateAssignment(assign)}
                        disabled={deactivatingId === assign.id || saving}
                      >
                        {deactivatingId === assign.id ? 'Desactivando...' : 'Desactivar'}
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Asistencia Inteligente: Recomendación de Departamento (RF-15, RF-16, RF-17) */}
            {recommendation && recommendation.suggestedDepartmentName && (
              <div
                className="recommendation-badge-container"
                role="region"
                aria-label="Recomendación inteligente de departamento"
              >
                <div className="recommendation-badge-main">
                  <span className="recommendation-badge-icon" aria-hidden="true">
                    💡
                  </span>
                  <div className="recommendation-badge-content">
                    <span className="recommendation-badge-title">
                      Sugerencia Inteligente: <strong>{recommendation.suggestedDepartmentName}</strong> ({recommendation.confidencePercentage}%)
                    </span>
                    {recommendation.reasoning && (
                      <p className="recommendation-badge-reasoning">{recommendation.reasoning}</p>
                    )}
                  </div>
                </div>
                {(recommendation.isLowConfidenceWarning || recommendation.confidencePercentage < 50) && (
                  <div className="recommendation-low-confidence-warning" role="alert">
                    <span className="warning-icon" aria-hidden="true">
                      ⚠️
                    </span>
                    <span>
                      Atención: Nivel de confianza bajo (&lt; 50%). Requiere confirmación rigurosa del operador.
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* Catálogo de departamentos */}
            <div className="assignment-section">
              <div className="assignment-section-header">
                <h3 className="assignment-section-title">Departamentos Municipales</h3>
                <span className="assignment-section-hint">
                  Seleccione uno o más departamentos
                </span>
              </div>

              {loadingDepts ? (
                <div className="technicians-loading">Cargando catálogo de departamentos...</div>
              ) : departments.length === 0 ? (
                <div className="technicians-empty">No hay departamentos disponibles.</div>
              ) : (
                <div className="departments-grid">
                  {departments.map((dept) => {
                    const isSelected = selectedDeptIds.includes(dept.id);
                    const isRecommended =
                      recommendation?.suggestedDepartmentId != null &&
                      Number(recommendation.suggestedDepartmentId) === dept.id;
                    return (
                      <label
                        key={dept.id}
                        className={`department-card-label ${isSelected ? 'selected' : ''}`}
                      >
                        <input
                          type="checkbox"
                          className="department-checkbox"
                          checked={isSelected}
                          onChange={() => toggleDepartment(dept.id)}
                          aria-label={`Seleccionar departamento ${dept.name}`}
                        />
                        <div className="department-card-content">
                          <div className="department-card-title-row">
                            <span className="department-card-name">{dept.name}</span>
                            {isRecommended && (
                              <span className="department-suggested-pill">
                                Recomendado ({recommendation.confidencePercentage}%)
                              </span>
                            )}
                          </div>
                          {dept.description && (
                            <span className="department-card-desc">{dept.description}</span>
                          )}
                        </div>
                      </label>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Selección dependiente de técnicos */}
            {selectedDeptIds.length > 0 && (
              <div className="assignment-section">
                <div className="assignment-section-header">
                  <h3 className="assignment-section-title">
                    Técnicos de Campo por Departamento (Opcional)
                  </h3>
                  <span className="assignment-section-hint">
                    Asigne técnicos específicos a la intervención
                  </span>
                </div>

                <div className="technicians-container">
                  {selectedDeptIds.map((deptId) => {
                    const dept = departments.find((d) => d.id === deptId);
                    const deptName = dept ? dept.name : `Departamento #${deptId}`;
                    const techs = techniciansByDept[deptId] || [];
                    const isLoadingTechs = loadingTechsByDept[deptId];

                    return (
                      <div key={deptId} className="department-tech-group">
                        <h4 className="department-tech-group-title">{deptName}</h4>
                        {isLoadingTechs ? (
                          <div className="technicians-loading">Cargando técnicos...</div>
                        ) : techs.length === 0 ? (
                          <div className="technicians-empty">
                            No hay técnicos disponibles registrados en este departamento.
                          </div>
                        ) : (
                          <div className="technicians-list">
                            {techs.map((tech) => {
                              const isTechSelected = selectedTechIds.includes(tech.id);
                              return (
                                <label
                                  key={tech.id}
                                  className={`technician-chip-label ${
                                    isTechSelected ? 'selected' : ''
                                  }`}
                                >
                                  <input
                                    type="checkbox"
                                    className="technician-checkbox"
                                    checked={isTechSelected}
                                    onChange={() => toggleTechnician(tech.id)}
                                    aria-label={`Asignar a ${tech.fullName || tech.email}`}
                                  />
                                  <span>{tech.fullName || tech.email}</span>
                                </label>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* Pie */}
          <footer className="assignment-modal-footer">
            <button
              type="button"
              className="assignment-btn-secondary"
              onClick={handleDismiss}
              disabled={saving || !!deactivatingId}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="assignment-btn-primary"
              disabled={saving || !!deactivatingId || loadingDepts}
            >
              {saving ? 'Guardando Asignación...' : 'Guardar Asignación'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}
