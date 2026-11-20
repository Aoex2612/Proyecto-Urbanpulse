# Nota QA — Feature 010: Filtros avanzados de incidencias con RBAC

## Veredicto

**Validación superada.** La implementación cumple los criterios funcionales y no funcionales de la especificación. Las pruebas de backend y frontend finalizaron sin fallos; el único test omitido corresponde a una integración condicionada a la disponibilidad de PostgreSQL.

## Trazabilidad de requisitos funcionales

| Requisito | Cobertura verificada |
|---|---|
| RF-1, RF-2 | `IncidentSpecificationsTest`, `IncidentFilterBar.test.jsx`, `MapPage.test.jsx` |
| RF-3, RF-4, RF-5, RF-6 | `IncidentFilterBar.test.jsx`, `IncidentServiceTest` |
| RF-7, RF-8, RF-9, RF-10, RF-11 | `IncidentFilterBar.test.jsx`, `IncidentSpecificationsTest`, `IncidentServiceTest`, `IncidentControllerTest`, `IncidentControllerV2Test` |
| RF-12 | `IncidentSpecificationsTest`, `IncidentServiceTest`, `MapPage.test.jsx`, `OperatorPanelPage.test.jsx` |
| RF-13 | `IncidentFilterBar.test.jsx` |
| RF-14 | `IncidentFilterBar.test.jsx`, `MapPage.test.jsx`, `OperatorPanelPage.test.jsx` |
| RF-15 | `MapPage.test.jsx`, `OperatorPanelPage.test.jsx` |

**Cobertura funcional:** 15/15 RF con pruebas dedicadas o trazabilidad directa — **100%**.

## Cobertura técnica

### Backend — JaCoCo

| Métrica | Resultado |
|---|---:|
| Líneas | 82,20% |
| Instrucciones | 80,45% |
| Ramas | 63,79% |
| Métodos | 77,79% |
| Clases analizadas | 92 |

### Frontend — Vitest/V8

| Métrica | Resultado |
|---|---:|
| Líneas | 82,27% |
| Sentencias | 81,10% |
| Ramas | 71,71% |
| Funciones | 75,61% |

## Ejecución realizada

- Backend: `cd Backend && ~/.m2/wrapper/dists/apache-maven-3.8.4-bin/52ccbt68d252mdldqsfsn03jlf/apache-maven-3.8.4/bin/mvn clean test -o`
  - 724 tests, 0 fallos, 0 errores, 1 omitido por PostgreSQL no disponible.
- Frontend: `cd frontend && npm run test:coverage`
  - 288 tests, 0 fallos.
- Frontend: `cd frontend && npm run lint`
  - Sin errores.

## Prueba manual mínima

1. Iniciar PostgreSQL y la aplicación backend; iniciar el frontend.
2. Entrar como ciudadano: comprobar que se muestran texto, categoría, fechas y distrito, pero no prioridad ni departamento.
3. Seleccionar un distrito y combinarlo con texto o categoría; comprobar que el resultado aplica todos los filtros.
4. Entrar como operador: comprobar los selectores de prioridad y departamento, incluido «Sin departamento asignado».
5. Aplicar una combinación sin resultados y comprobar el aviso informativo.
6. Abrir y cerrar un modal de detalle; comprobar que los filtros permanecen seleccionados.
7. Pulsar «Limpiar filtros» y comprobar que todos los controles vuelven a su valor inicial.
