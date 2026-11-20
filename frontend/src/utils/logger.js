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
 * Texto de respaldo cuando el contexto no puede serializarse.
 *
 * @type {string}
 */
const UNSERIALIZABLE_CONTEXT = '[Unserializable context]';

/**
 * Marcador para referencias circulares detectadas durante la serialización.
 *
 * @type {string}
 */
const CIRCULAR_REFERENCE = '[Circular]';

/**
 * Normaliza valores especiales para que puedan serializarse con `JSON.stringify`.
 *
 * @param {unknown} value - Valor a normalizar.
 * @returns {unknown} Valor serializable.
 */
const normalizeValue = (value) => {
  if (value instanceof Error) {
    return normalizeError(value);
  }

  if (typeof value === 'bigint') {
    return value.toString();
  }

  return value;
};

/**
 * Crea un `replacer` seguro que evita referencias circulares y normaliza
 * valores no serializables de forma controlada.
 *
 * @returns {(key: string, value: unknown) => unknown} Replacer para `JSON.stringify`.
 */
const createSafeReplacer = () => {
  const seen = new WeakSet();

  return (key, value) => {
    const normalized = normalizeValue(value);

    if (normalized && typeof normalized === 'object') {
      if (seen.has(normalized)) {
        return CIRCULAR_REFERENCE;
      }
      seen.add(normalized);
    }

    return normalized;
  };
};

/**
 * Serializa un contexto sin lanzar excepciones ante valores no serializables.
 *
 * @param {unknown} value - Contexto a serializar.
 * @returns {string} JSON del contexto o texto de respaldo legible.
 */
const safeStringify = (value) => {
  try {
    return JSON.stringify(value, createSafeReplacer());
  } catch {
    return UNSERIALIZABLE_CONTEXT;
  }
};

/**
 * Normaliza el contexto para que los errores sean legibles y estructurados.
 *
 * @param {Object} context - Datos adicionales de la operación.
 * @returns {Object} Contexto serializable.
 */
const normalizeContext = (context = {}) => {
  if (context === null || context === undefined) {
    return {};
  }

  if (typeof context !== 'object') {
    return { value: context };
  }

  try {
    return Object.fromEntries(
      Object.entries(context).map(([key, value]) => [key, normalizeError(value)])
    );
  } catch {
    return { context: UNSERIALIZABLE_CONTEXT };
  }
};

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
    ? ` ${safeStringify(context)}`
    : '';
  return `[${level.toUpperCase()}] ${message}${contextText}`;
};

/**
 * Registra un mensaje en el nivel indicado sin interrumpir el flujo de la aplicación.
 *
 * @param {'error'|'warn'|'info'} level - Nivel de severidad.
 * @param {string} message - Mensaje descriptivo de la operación.
 * @param {Object} [context={}] - Contexto estructurado de la operación.
 * @returns {void}
 */
const log = (level, message, context = {}) => {
  try {
    const normalizedContext = normalizeContext(context);
    const output = console[level];

    if (typeof output === 'function') {
      output(formatEntry(level, message, normalizedContext), normalizedContext);
    }
  } catch {
    // El logger nunca debe interrumpir el manejo del error que está registrando.
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
