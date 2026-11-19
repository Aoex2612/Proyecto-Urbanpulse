import { beforeEach, describe, expect, it, vi } from 'vitest';
import logger from './logger';

describe('logger', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it.each([
    ['error', 'error'],
    ['warn', 'warn'],
    ['info', 'info'],
  ])('registra mensajes en el nivel %s con contexto estructurado', (level, consoleMethod) => {
    const consoleSpy = vi.spyOn(console, consoleMethod).mockImplementation(() => {});
    const context = { operation: 'testOperation', incidentId: 42 };

    logger[level]('Operación ejecutada', context);

    expect(consoleSpy).toHaveBeenCalledWith(
      '[%s] Operación ejecutada {"operation":"testOperation","incidentId":42}'.replace(
        '%s',
        level.toUpperCase()
      ),
      context
    );
  });

  it('normaliza los errores para conservar mensaje y stacktrace', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const error = new Error('Fallo de red');

    logger.error('Operación fallida', { operation: 'request', error });

    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining('"message":"Fallo de red"'),
      expect.objectContaining({
        operation: 'request',
        error: expect.objectContaining({
          name: 'Error',
          message: 'Fallo de red',
          stack: error.stack,
        }),
      })
    );
  });

  it('serializa contextos con referencias circulares sin lanzar excepciones', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const context = { operation: 'circularContext' };
    context.self = context;

    expect(() => logger.error('Operación con contexto circular', context)).not.toThrow();

    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining('[Circular]'),
      expect.objectContaining({ operation: 'circularContext' })
    );
  });

  it('serializa BigInt y errores anidados sin lanzar excepciones', () => {
    const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const error = new Error('Fallo anidado');

    expect(() =>
      logger.warn('Operación con valores especiales', {
        operation: 'specialValues',
        total: 10n,
        meta: { error },
      })
    ).not.toThrow();

    const [entry] = consoleSpy.mock.calls[0];
    expect(entry).toContain('"total":"10"');
    expect(entry).toContain('"message":"Fallo anidado"');
  });

  it('usa un fallback legible cuando el contexto no es serializable', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const context = {
      operation: 'brokenContext',
      payload: {
        toJSON() {
          throw new Error('No serializable');
        },
      },
    };

    expect(() =>
      logger.error('Operación con contexto no serializable', context)
    ).not.toThrow();

    expect(consoleSpy).toHaveBeenCalledWith(
      expect.stringContaining('[Unserializable context]'),
      expect.objectContaining({ operation: 'brokenContext' })
    );
  });

  it('no propaga excepciones si el método de consola falla', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {
      throw new Error('Consola no disponible');
    });

    expect(() =>
      logger.error('Operación con consola rota', { operation: 'consoleFailure' })
    ).not.toThrow();
  });
});
