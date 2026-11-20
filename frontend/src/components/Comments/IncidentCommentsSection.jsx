/**
 * @fileoverview Sección de comentarios, notas internas y avisos públicos de una incidencia para UrbanPulse.
 * Permite a la ciudadanía y al personal municipal visualizar el histórico de intervenciones y notas,
 * distinguiendo avisos públicos de notas técnicas internas, indicando ediciones con sellado temporal
 * y facilitando la publicación y edición con control de autorización granular.
 */

import { useState, useEffect, useCallback } from 'react';
import authService from '../../services/authService';
import triageService from '../../services/triageService';
import { formatDate } from '../../utils/dateUtils';
import './IncidentCommentsSection.css';

/**
 * Roles autorizados para redactar notas y comentarios en el triaje de incidencias.
 * @type {string[]}
 */
const MUNICIPAL_ROLES = ['OPERATOR', 'TECHNICIAN', 'ADMIN'];

/**
 * Componente que renderiza el muro cronológico de comentarios y el formulario de publicación.
 *
 * @param {Object} props - Propiedades del componente.
 * @param {number|string} props.incidentId - Identificador único de la incidencia.
 * @param {string} [props.incidentStatus] - Estado de ciclo de vida actual de la incidencia.
 * @param {boolean} [props.isClosedOrRejected] - Bandera explícita que marca si la incidencia está en estado terminal.
 * @param {Object|null} [props.currentUser=null] - Usuario autenticado (opcional, consulta authService por defecto).
 * @param {Function} [props.onCommentAdded] - Callback ejecutado tras publicar satisfactoriamente un comentario.
 * @returns {import('react').JSX.Element} Sección interactiva de comentarios.
 */
export default function IncidentCommentsSection({
  incidentId,
  incidentStatus = '',
  isClosedOrRejected = false,
  currentUser = null,
  onCommentAdded,
}) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetchError, setFetchError] = useState(null);

  // Estados del formulario de nuevo comentario
  const [newCommentText, setNewCommentText] = useState('');
  const [isInternal, setIsInternal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  // Estados de edición en línea
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingText, setEditingText] = useState('');
  const [updating, setUpdating] = useState(false);
  const [updateError, setUpdateError] = useState(null);

  // Obtener usuario activo efectivo
  const effectiveUser =
    currentUser ||
    (typeof authService.getCurrentUser === 'function'
      ? authService.getCurrentUser()
      : authService.getUser());

  const userRole = effectiveUser?.role || 'CITIZEN';
  const isMunicipalStaff = MUNICIPAL_ROLES.includes(userRole);

  // Determinar si la incidencia se encuentra en estado terminal bloqueado
  const isTerminal =
    Boolean(isClosedOrRejected) ||
    incidentStatus === 'CLOSED' ||
    incidentStatus === 'REJECTED';

  /**
   * Carga asíncrona de comentarios asociados a la incidencia.
   */
  const loadComments = useCallback(async () => {
    if (!incidentId) return;

    setLoading(true);
    setFetchError(null);

    try {
      const data = await triageService.getIncidentComments(incidentId);
      setComments(Array.isArray(data) ? data : []);
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        'No se pudieron cargar los comentarios de la incidencia.';
      setFetchError(msg);
      setComments([]);
    } finally {
      setLoading(false);
    }
  }, [incidentId]);

  useEffect(() => {
    let isMounted = true;

    Promise.resolve().then(() => {
      if (isMounted) {
        loadComments();
      }
    });

    return () => {
      isMounted = false;
    };
  }, [loadComments]);

  /**
   * Comprueba si el usuario autenticado tiene permisos para editar un comentario específico.
   *
   * @param {Object} comment - Objeto de comentario a comprobar.
   * @returns {boolean} True si es el autor o un administrador y la incidencia no está cerrada.
   */
  const canEditComment = (comment) => {
    if (isTerminal) return false;
    if (userRole === 'ADMIN') return true;

    const isAuthor = Boolean(
      (effectiveUser?.id != null && comment.authorId === effectiveUser.id) ||
      (effectiveUser?.email && comment.authorEmail === effectiveUser.email)
    );

    return isAuthor;
  };

  /**
   * Inicia el modo de edición en línea para un comentario.
   *
   * @param {Object} comment - Comentario a editar.
   */
  const handleStartEdit = (comment) => {
    setEditingCommentId(comment.id);
    setEditingText(comment.comment || '');
    setUpdateError(null);
  };

  /**
   * Cancela la edición en línea y descarta los cambios temporales.
   */
  const handleCancelEdit = () => {
    setEditingCommentId(null);
    setEditingText('');
    setUpdateError(null);
  };

  /**
   * Guarda las modificaciones de un comentario invocando el servicio de triaje.
   *
   * @param {number|string} commentId - Identificador del comentario.
   */
  const handleSaveEdit = async (commentId) => {
    const trimmed = editingText.trim();
    if (!trimmed) {
      setUpdateError('El comentario no puede estar vacío.');
      return;
    }

    setUpdating(true);
    setUpdateError(null);

    try {
      const updated = await triageService.updateIncidentComment(incidentId, commentId, {
        comment: trimmed,
      });

      setComments((prev) =>
        prev.map((c) => (c.id === commentId ? { ...c, ...updated } : c))
      );
      setEditingCommentId(null);
      setEditingText('');
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        'Error al guardar la edición del comentario.';
      setUpdateError(msg);
    } finally {
      setUpdating(false);
    }
  };

  /**
   * Envía un nuevo comentario o nota interna al servidor.
   *
   * @param {import('react').FormEvent} e - Evento de envío del formulario.
   */
  const handleSubmitNewComment = async (e) => {
    e.preventDefault();

    const trimmed = newCommentText.trim();
    if (!trimmed) {
      setSubmitError('El comentario no puede estar vacío.');
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    try {
      const created = await triageService.addIncidentComment(incidentId, {
        comment: trimmed,
        isInternal: isMunicipalStaff ? isInternal : false,
      });

      setComments((prev) => [...prev, created]);
      setNewCommentText('');
      setIsInternal(false);

      if (onCommentAdded) {
        onCommentAdded(created);
      }
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        'No se pudo publicar el comentario. Por favor, inténtalo de nuevo.';
      setSubmitError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section
      className="incident-comments-section"
      aria-labelledby="comments-section-title"
    >
      <div className="comments-section-header">
        <h4 id="comments-section-title" className="comments-section-title">
          <span className="comments-header-icon" aria-hidden="true">💬</span>
          Comentarios y Actuaciones
          <span className="comments-count-badge" aria-label={`${comments.length} comentarios`}>
            {comments.length}
          </span>
        </h4>
      </div>

      {/* Estado de carga */}
      {loading && (
        <div className="comments-loading" role="status" aria-live="polite">
          <span className="loading-spinner" aria-hidden="true" />
          <span>Cargando comentarios y avisos...</span>
        </div>
      )}

      {/* Error al recuperar comentarios */}
      {fetchError && !loading && (
        <div className="comments-error-banner" role="alert">
          <span>⚠️ {fetchError}</span>
          <button
            type="button"
            className="btn-retry-comments"
            onClick={loadComments}
          >
            Reintentar
          </button>
        </div>
      )}

      {/* Lista cronológica de comentarios */}
      {!loading && !fetchError && comments.length === 0 && (
        <div className="comments-empty-message">
          <p>No hay comentarios ni avisos registrados para esta incidencia.</p>
        </div>
      )}

      {!loading && comments.length > 0 && (
        <ol className="comments-list" aria-label="Historial cronológico de comentarios">
          {comments.map((comment) => {
            const isEditingThis = editingCommentId === comment.id;
            const canEdit = canEditComment(comment);
            const isEdited =
              Boolean(comment.updatedAt) &&
              comment.updatedAt !== comment.createdAt;

            return (
              <li
                key={comment.id}
                className={`comment-item ${
                  comment.isInternal ? 'comment-item-internal' : 'comment-item-public'
                }`}
              >
                <div className="comment-item-header">
                  <div className="comment-author-info">
                    <span className="comment-author-name">
                      {comment.authorName || comment.authorEmail || 'Personal municipal'}
                    </span>
                    {comment.authorRole && (
                      <span className="comment-author-role-badge">
                        {comment.authorRole}
                      </span>
                    )}
                    <span
                      className={`comment-badge ${
                        comment.isInternal
                          ? 'comment-badge-internal'
                          : 'comment-badge-public'
                      }`}
                      aria-label={
                        comment.isInternal
                          ? 'Nota interna solo visible para personal municipal'
                          : 'Aviso público visible para la ciudadanía'
                      }
                    >
                      {comment.isInternal ? 'Nota Interna' : 'Aviso Público'}
                    </span>
                  </div>

                  <div className="comment-meta-dates">
                    <time
                      dateTime={comment.createdAt}
                      className="comment-created-time"
                    >
                      {formatDate(comment.createdAt)}
                    </time>
                    {isEdited && (
                      <span
                        className="comment-edited-indicator"
                        title={`Modificado el ${formatDate(comment.updatedAt)}`}
                      >
                        (Editado el {formatDate(comment.updatedAt)})
                      </span>
                    )}
                  </div>
                </div>

                {/* Cuerpo del comentario o formulario de edición */}
                <div className="comment-item-body">
                  {isEditingThis ? (
                    <div className="comment-inline-edit-form">
                      <label
                        htmlFor={`edit-comment-${comment.id}`}
                        className="sr-only"
                      >
                        Editar comentario
                      </label>
                      <textarea
                        id={`edit-comment-${comment.id}`}
                        className="comment-edit-textarea"
                        value={editingText}
                        onChange={(e) => setEditingText(e.target.value)}
                        disabled={updating}
                        rows={3}
                        autoFocus
                      />

                      {updateError && (
                        <p className="comment-action-error" role="alert">
                          {updateError}
                        </p>
                      )}

                      <div className="comment-edit-actions">
                        <button
                          type="button"
                          className="btn-save-comment"
                          onClick={() => handleSaveEdit(comment.id)}
                          disabled={updating || !editingText.trim()}
                        >
                          {updating ? 'Guardando...' : 'Guardar'}
                        </button>
                        <button
                          type="button"
                          className="btn-cancel-edit"
                          onClick={handleCancelEdit}
                          disabled={updating}
                        >
                          Cancelar
                        </button>
                      </div>
                    </div>
                  ) : (
                    <p className="comment-text-content">{comment.comment}</p>
                  )}
                </div>

                {/* Acciones del comentario (botón editar) */}
                {canEdit && !isEditingThis && (
                  <div className="comment-item-actions">
                    <button
                      type="button"
                      className="btn-edit-comment"
                      onClick={() => handleStartEdit(comment)}
                      aria-label={`Editar comentario de ${comment.authorName || 'autor'}`}
                    >
                      ✏️ Editar
                    </button>
                  </div>
                )}
              </li>
            );
          })}
        </ol>
      )}

      {/* Formulario de nuevo comentario o aviso de estado terminal */}
      <div className="comments-form-container">
        {isTerminal ? (
          <div
            className="comments-closed-banner"
            role="status"
            aria-live="polite"
          >
            <span className="closed-banner-icon" aria-hidden="true">🔒</span>
            <p className="closed-banner-text">
              La incidencia está cerrada o rechazada. No se admiten nuevos comentarios.
            </p>
          </div>
        ) : isMunicipalStaff ? (
          <form
            onSubmit={handleSubmitNewComment}
            className="new-comment-form"
            aria-label="Formulario para redactar un nuevo comentario o nota interna"
          >
            <div className="form-group">
              <label htmlFor="new-comment-input" className="form-label">
                Añadir Comentario o Nota Operativa
              </label>
              <textarea
                id="new-comment-input"
                className="comment-input-textarea"
                placeholder="Escribe una actualización técnica, aviso para la ciudadanía o nota operativa..."
                value={newCommentText}
                onChange={(e) => setNewCommentText(e.target.value)}
                rows={3}
                disabled={submitting}
                required
              />
            </div>

            {/* Selector de visibilidad interna (exclusivo para personal municipal) */}
            <div className="comment-visibility-row">
              <label className="visibility-switch-label">
                <input
                  type="checkbox"
                  id="internal-comment-checkbox"
                  className="visibility-checkbox"
                  checked={isInternal}
                  onChange={(e) => setIsInternal(e.target.checked)}
                  disabled={submitting}
                />
                <span className="visibility-switch-text">
                  🔒 Nota interna (solo visible para personal municipal)
                </span>
              </label>
            </div>

            {submitError && (
              <p className="comment-submit-error" role="alert">
                ⚠️ {submitError}
              </p>
            )}

            <div className="form-actions-row">
              <button
                type="submit"
                className="btn-submit-comment"
                disabled={submitting || !newCommentText.trim()}
              >
                {submitting ? 'Publicando...' : 'Publicar comentario'}
              </button>
            </div>
          </form>
        ) : (
          <div className="comments-citizen-notice">
            <p className="citizen-notice-text">
              ℹ️ Los avisos oficiales e informes de actuación publicados por el personal municipal se reflejan en este muro cronológico.
            </p>
          </div>
        )}
      </div>
    </section>
  );
}
