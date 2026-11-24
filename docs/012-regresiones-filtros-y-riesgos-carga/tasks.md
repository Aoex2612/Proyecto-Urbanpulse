# 012 — Corrección de Regresiones de Filtros y Riesgos de Carga — Tareas

_Checklist accionable derivada del `plan.md`. Tareas principales y subtareas pequeñas y concretas; marca `[x]` al completarlas._

## [x] Tarea 1: Blindaje de scripts k6 ante fallos de autenticación — Sin dependencias — RF: RNF-FIABILIDAD-K6
- [x] Incorporar la importación y llamada a `fail(...)` de k6 en `k6/experimento-3-emergencia.js` dentro de `setup()`.
- [x] Aplicar la misma validación estricta con `fail(...)` en `k6/baseline.js` y `k6/experimento-2-saturacion.js`.
- [x] Validar sintácticamente los scripts de k6.

## [x] Tarea 2: Consistencia documental del Experimento 3 — Depende de Tarea 1 — RF: DOC-K6-METRICAS
- [x] Actualizar `docs/rendimiento/experimento-3-emergencia.md` aclarando que las 1.022 peticiones totales corresponden a 1 petición de login en `setup` + 1.021 checks/reqs bajo carga (446 lecturas de mapa + 575 reportes de emergencia).
- [x] Alinear las tablas y conclusiones para mantener consistencia estricta en todo el documento.

## [ ] Tarea 3: Corrección de resultados obsoletos y estado de carga en Frontend — Sin dependencias — RF: RF-FILTROS-UX
- [ ] En `frontend/src/pages/MapPage.jsx`, gestionar el estado de carga y cancelar o invalidar peticiones de búsqueda obsoletas para que no se muestren datos del filtro previo durante el debounce.
- [ ] En `frontend/src/pages/OperatorPanelPage.jsx`, sincronizar el estado visual de carga y descarte de respuestas desfasadas.
- [ ] Actualizar o ampliar las pruebas frontend (`MapPage.test.jsx`, `OperatorPanelPage.test.jsx`) para validar el comportamiento con debounce y estados pendientes.

## [ ] Tarea 4: Verificación global de calidad — Depende de Tareas 1, 2 y 3 — RF: QA-REGRESIONES
- [ ] Ejecutar la suite de tests de frontend (`npm test` en `frontend/`).
- [ ] Ejecutar el linter de frontend (`npm run lint` en `frontend/`).
- [ ] Validar que la compilación y tests de backend siguen intactos (`mvn test-compile` o `mvn test`).

---
## Mantenimiento (checklist recurrente)

- [ ] Verificar que cualquier nuevo script k6 aborte con `fail()` si no supera la fase de `setup()`.
- [ ] Al incorporar la paginación en el Experimento 4, reutilizar los componentes de filtro frontend asegurando la compatibilidad de payloads.
