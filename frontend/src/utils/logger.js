/**
 * @fileoverview Logger centralizado del frontend con niveles y contexto estructurado.
 */

/**
 * Convierte un error en un objeto serializable sin perder su información principal.
 *
 * @param {unknown} error - Valor capturado durante la operación.
 * @returns {unknown} Error normalizado o valor original.
 */
const normalizeError = (error) => {
  if (!(error instanceof Error)) {
    return error;
  }

  return {
    name: error.name,
    message: error.message,
    ...(error.stack ? { stack: error.stack } : {}),
  };
};

/**
 * Normaliza el contexto para que los errores sean legibles y estructurados.
 *
 * @param {Object} context - Datos adicionales de la operación.
 * @returns {Object} Contexto serializable.
 */
const normalizeContext = (context = {}) =>
  Object.fromEntries(
    Object.entries(context).map(([key, value]) => [key, normalizeError(value)])
  );

/**
 * Formatea una entrada de log manteniendo un prefijo legible y el contexto JSON.
 *
 * @param {string} level - Nivel de la entrada.
 * @param {string} message - Mensaje descriptivo.
 * @param {Object} context - Contexto estructurado.
 * @returns {string} Entrada formateada.
 */
const formatEntry = (level, message, context) => {
  const contextText = Object.keys(context).length
    ? ` ${JSON.stringify(context)}`
    : '';
  return `[${level.toUpperCase()}] ${message}${contextText}`;
};

/**
 * Registra un mensaje en el nivel indicado.
 *
 * @param {'error'|'warn'|'info'} level - Nivel de severidad.
 * @param {string} message - Mensaje descriptivo de la operación.
 * @param {Object} [context={}] - Contexto estructurado de la operación.
 * @returns {void}
 */
const log = (level, message, context = {}) => {
  const normalizedContext = normalizeContext(context);
  const output = console[level];

  if (typeof output === 'function') {
    output(formatEntry(level, message, normalizedContext), normalizedContext);
  }
};

/**
 * Logger centralizado para operaciones del frontend.
 *
 * @type {{error: Function, warn: Function, info: Function}}
 */
const logger = {
  /**
   * Registra un error de operación.
   *
   * @param {string} message - Mensaje descriptivo.
   * @param {Object} [context={}] - Contexto estructurado.
   * @returns {void}
   */
  error: (message, context = {}) => log('error', message, context),

  /**
   * Registra una advertencia de operación.
   *
   * @param {string} message - Mensaje descriptivo.
   * @param {Object} [context={}] - Contexto estructurado.
   * @returns {void}
   */
  warn: (message, context = {}) => log('warn', message, context),

  /**
   * Registra información de operación.
   *
   * @param {string} message - Mensaje descriptivo.
   * @param {Object} [context={}] - Contexto estructurado.
   * @returns {void}
   */
  info: (message, context = {}) => log('info', message, context),
};

export default logger;
