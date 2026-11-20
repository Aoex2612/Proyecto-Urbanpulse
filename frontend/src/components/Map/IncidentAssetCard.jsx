/**
 * @fileoverview Componente de tarjeta para visualizar y gestionar activos urbanos
 * vinculados o sugeridos a una incidencia en UrbanPulse (HU16).
 * Muestra el nombre, código de activo, tipo, distancia en metros y porcentaje de confianza.
 * Proporciona controles reactivos de confirmación y desvinculación restringidos a
 * usuarios con perfil operativo (OPERATOR o ADMIN).
 */

import { useState } from 'react';
import incidentService from '../../services/incidentService';
import './IncidentAssetCard.css';

/**
 * @typedef {Object} UrbanAsset
 * @property {number} assetId - Identificador numérico del activo urbano.
 * @property {string} assetCode - Código de inventario municipal.
 * @property {string} [type] - Tipo de activo (SEMAFORO, FUENTE, CONTENEDOR, etc.).
 * @property {string} [name] - Denominación o nombre descriptivo.
 * @property {number} [latitude] - Latitud WGS84 del activo.
 * @property {number} [longitude] - Longitud WGS84 del activo.
 * @property {number} [distanceMeters] - Distancia calculada en metros desde la incidencia.
 * @property {number} [confidenceScore] - Puntuación de confianza normalizada (0.0 a 1.0).
 * @property {boolean} [isConfirmed] - Indicador de confirmación manual.
 */

/**
 * Propiedades del componente IncidentAssetCard.
 *
 * @param {Object} props
 * @param {number|string} props.incidentId - Identificador único de la incidencia.
 * @param {UrbanAsset[]} [props.assets=[]] - Lista de activos asociados o sugeridos.
 * @param {boolean} [props.canManage=false] - Bandera que habilita los controles de confirmación y borrado (OPERATOR/ADMIN).
 * @param {Function} [props.onAssetUpdated] - Callback invocado tras confirmar o eliminar una asociación.
 * @returns {import('react').JSX.Element}
 */
export default function IncidentAssetCard({
  incidentId,
  assets = [],
  canManage = false,
  onAssetUpdated,
}) {
  const [processingId, setProcessingId] = useState(null);
  const [feedback, setFeedback] = useState(null); // { type: 'success' | 'error', message: string }

  /**
   * Confirma la vinculación de un activo urbano.
   *
   * @param {number} assetId
   */
  const handleConfirm = async (assetId) => {
    try {
      setProcessingId(assetId);
      setFeedback(null);
      await incidentService.confirmIncidentAsset(incidentId, assetId);
      setFeedback({
        type: 'success',
        message: 'Activo urbano confirmado correctamente.',
      });
      if (onAssetUpdated) {
        onAssetUpdated();
      }
    } catch (err) {
      const errorMsg =
        err?.response?.data?.message ||
        'Error al confirmar la asociación del activo.';
      setFeedback({ type: 'error', message: errorMsg });
    } finally {
      setProcessingId(null);
    }
  };

  /**
   * Desvincula o elimina la asociación de un activo urbano.
   *
   * @param {number} assetId
   */
  const handleRemove = async (assetId) => {
    try {
      setProcessingId(assetId);
      setFeedback(null);
      await incidentService.removeIncidentAsset(incidentId, assetId);
      setFeedback({
        type: 'success',
        message: 'Asociación de activo eliminada correctamente.',
      });
      if (onAssetUpdated) {
        onAssetUpdated();
      }
    } catch (err) {
      const errorMsg =
        err?.response?.data?.message ||
        'Error al desvincular el activo urbano.';
      setFeedback({ type: 'error', message: errorMsg });
    } finally {
      setProcessingId(null);
    }
  };

  if (!assets || assets.length === 0) {
    return (
      <section className="incident-asset-card" aria-labelledby="assets-card-title">
        <div className="incident-asset-header">
          <h4 id="assets-card-title" className="detail-section-heading">
            <span className="asset-header-icon" aria-hidden="true">🏛️</span>
            Activos Urbanos Cercanos
          </h4>
        </div>
        <p className="incident-asset-empty">
          No se han detectado elementos de mobiliario ni infraestructura urbana catalogada a menos de 50 metros del aviso.
        </p>
      </section>
    );
  }

  return (
    <section className="incident-asset-card" aria-labelledby="assets-card-title">
      <div className="incident-asset-header">
        <h4 id="assets-card-title" className="detail-section-heading">
          <span className="asset-header-icon" aria-hidden="true">🏛️</span>
          Activos Urbanos Cercanos ({assets.length})
        </h4>
        {canManage && (
          <span className="asset-operator-badge">Gestión de Operador</span>
        )}
      </div>

      {feedback && (
        <div
          role="status"
          className={`incident-asset-feedback feedback-${feedback.type}`}
        >
          {feedback.message}
        </div>
      )}

      <ul className="incident-asset-list" aria-label="Lista de activos asociados">
        {assets.map((asset) => {
          const confidencePercent =
            asset.confidenceScore != null
              ? Math.round(asset.confidenceScore * 100)
              : null;
          const distanceFormatted =
            asset.distanceMeters != null
              ? `${Number(asset.distanceMeters).toFixed(1)} m`
              : 'Distancia desconocida';

          const isProcessing = processingId === asset.assetId;

          return (
            <li
              key={asset.assetId}
              className={`incident-asset-item ${asset.isConfirmed ? 'is-confirmed' : ''}`}
            >
              <div className="asset-main-info">
                <div className="asset-title-row">
                  <strong className="asset-name">{asset.name || 'Activo Urbano'}</strong>
                  {asset.assetCode && (
                    <span className="asset-code-badge">{asset.assetCode}</span>
                  )}
                  {asset.isConfirmed ? (
                    <span className="asset-confirmed-tag" title="Confirmado por operador">
                      ✓ Confirmado
                    </span>
                  ) : (
                    <span className="asset-candidate-tag" title="Sugerencia automática por proximidad">
                      Sugerido
                    </span>
                  )}
                </div>

                <div className="asset-meta-row">
                  {asset.type && (
                    <span className="asset-type-badge">
                      Tipo: {asset.type}
                    </span>
                  )}
                  <span className="asset-distance-badge" title="Distancia calculada respecto al aviso">
                    📍 {distanceFormatted}
                  </span>
                  {confidencePercent != null && (
                    <span
                      className="asset-confidence-badge"
                      title="Nivel de certidumbre probabilística basado en proximidad"
                    >
                      Confianza: {confidencePercent}%
                    </span>
                  )}
                </div>
              </div>

              {canManage && (
                <div className="asset-actions-row">
                  {!asset.isConfirmed && (
                    <button
                      type="button"
                      className="btn-asset-confirm"
                      onClick={() => handleConfirm(asset.assetId)}
                      disabled={isProcessing}
                      aria-label={`Confirmar activo ${asset.name || asset.assetCode}`}
                    >
                      {isProcessing ? 'Procesando...' : 'Confirmar'}
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn-asset-remove"
                    onClick={() => handleRemove(asset.assetId)}
                    disabled={isProcessing}
                    aria-label={`Desvincular activo ${asset.name || asset.assetCode}`}
                  >
                    {isProcessing ? '...' : 'Desvincular'}
                  </button>
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
