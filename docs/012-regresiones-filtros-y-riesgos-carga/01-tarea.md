# Tarea 1: Blindaje de scripts k6 ante fallos de autenticación

## Descripción
Se ha asegurado la fiabilidad de la ejecución de pruebas de carga en k6 abortando la ejecución de manera temprana y estricta en la fase `setup()` si no se obtiene una autenticación válida.

Anteriormente, si la llamada al endpoint de login (`/api/v1/auth/login` o `/api/auth/login`) fallaba o devolvía un código distinto de 200, los scripts emitían un mensaje pasivo con `console.warn` y retornaban `{ token: null }`. Esto permitía que los Virtual Users (VUs) continuaran ejecutándose bajo carga realizando peticiones sin cabecera `Authorization` o con cabecera inválida, disparando masivamente respuestas HTTP 401/403 no deseadas que distorsionaban las métricas de latencia, percentiles y tasas de fallo reales del sistema.

## Archivos modificados
- `k6/baseline.js`: Importa `fail` de `'k6'`. En `setup()`, si `res.status !== 200` o si `token` es nulo/vacío, ejecuta `fail(...)` provocando la interrupción inmediata del test.
- `k6/experimento-2-saturacion.js`: Importa `fail` de `'k6'` y aplica la misma validación estricta en `setup()`.
- `k6/experimento-3-emergencia.js`: Importa `fail` de `'k6'` y aplica la misma validación estricta en `setup()`.

## Verificación y Pruebas
1. **Validación estática de sintaxis JavaScript:**
   Se comprobó que los tres scripts tienen sintaxis válida ejecutando `node --check`:
   ```bash
   node --check k6/baseline.js
   node --check k6/experimento-2-saturacion.js
   node --check k6/experimento-3-emergencia.js
   ```
   Todos los ficheros superaron la verificación sin errores sintácticos ni de parseo de módulos.
