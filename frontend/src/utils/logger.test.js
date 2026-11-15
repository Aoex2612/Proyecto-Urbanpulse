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
});
