# Tarea 7: Verificación Integral de Criterios de Aceptación, Cobertura y Guía de QA

**Feature:** `004-reporte-y-seguimiento-de-incidencias`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** Todos (RF-1 a RF-18)  
**Requisitos no funcionales:** Integridad de pruebas, calidad estática de código, documentación de QA y resiliencia  

---

## 1. Resumen de la Tarea

En esta tarea se ha llevado a cabo la verificación cruzada de calidad, la comprobación de la totalidad de pruebas automatizadas y la revisión integral de la guía de pruebas manuales:

1. **Ejecución y Verificación de la Suite de Backend:**
   - Se ejecutó `mvn test` en el módulo `Backend`.
   - **Resultado:** 276 tests ejecutados, 0 fallos, 0 errores, 0 skipped en entorno local (1 skipped en CI para PostgreSQL real).
   - Cobertura global de JaCoCo superior al 95% de instrucciones y más del 75% de ramas lógicas, superando con holgura los umbrales mínimos del proyecto (70% y 60%).

2. **Ejecución y Verificación de la Suite de Frontend:**
   - Se ejecutó `npm test` en el módulo `frontend`.
   - **Resultado:** 6 suites de prueba, 43 tests ejecutados, 0 fallos.
   - Verificación estática con `npm run lint` arrojando 0 errores y 0 advertencias de ESLint.

3. **Corrección y Alineamiento de la Guía de Pruebas Manuales (`qa-note.md`):**
   - Se subsanó el error de URL en el **Escenario C**, reemplazando la referencia incorrecta al puerto del frontend por el endpoint REST canónico `http://localhost:8080/api/incidents/{id}` (con cabecera `Authorization: Bearer <token_jwt>`).
   - Se clarificó la validación del código HTTP 404 estructurado en `http://localhost:8080/api/incidents/99999`.
   - Se documentó la verificación interactiva en el cliente web mediante modales accesibles en `/my-incidents` y `/map`.

4. **Trazabilidad y Criterios de Aceptación:**
   - Todos los criterios de aceptación funcionales y no funcionales de `spec.md` han sido contrastados frente a la implementación y tests, validando la resiliencia TLS/SSL y el mecanismo de fallback cartográfico.
