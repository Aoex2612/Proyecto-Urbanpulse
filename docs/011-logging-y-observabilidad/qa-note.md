# QA Note — Feature 011: Logging y Observabilidad

## Veredicto

Validación completada con éxito. Se corrigió el aislamiento de `IncidenciaRepositoryTest` usando timestamps deterministas escritos después de persistir las entidades. La suite completa de backend y las pruebas de frontend pasan sin fallos.

## Trazabilidad de requisitos

| Requisito | Evidencia | Resultado |
|---|---|---|
| RF-1 | `GlobalExceptionHandlerTest` verifica nivel ERROR y stacktrace. | Cumplido |
| RF-2 | `JwtAuthenticationFilterTest`, `JwtAuthenticationEntryPointTest` y `CustomAccessDeniedHandlerTest` verifican WARN, URI, motivo y redacción. | Cumplido |
| RF-3 | `IncidentServiceTest` verifica WARN antes del fallback. | Cumplido |
| RF-4 | `IncidentServiceTest` verifica ID en creación y cambio de estado. | Cumplido |
| RF-5 | `logger.test.js`, `incidentService.test.js`, `CreateIncidentModal.test.jsx` e `IncidentDetailModal.test.jsx`. | Cumplido |
| RF-6 | Configuración verificada en `application.properties` y `application-dev.properties`. | Cumplido |
| NFR-1 | Tests de redacción y revisión de mensajes sin credenciales, contraseñas ni tokens completos. | Cumplido |
| NFR-2 | Test de excepción no controlada con nivel ERROR. | Cumplido |

## Cobertura técnica

### Backend — JaCoCo

| Métrica | Cobertura |
|---|---:|
| Instrucciones | 80% |
| Líneas | 82% |
| Ramas | 63% |
| Clases | 92% |

### Frontend — Vitest

| Métrica | Cobertura |
|---|---:|
| Líneas | 82.53% |
| Ramas | 71.79% |
| Funciones | 75.79% |
| Sentencias | 81.35% |

## Validación ejecutada

- `cd Backend && mvn test -Dtest=IncidenciaRepositoryTest` — 5 tests correctos.
- `cd Backend && mvn clean test` — 732 tests correctos, 1 omitido por disponibilidad de PostgreSQL.
- `cd frontend && npm test -- --run` — 294 tests correctos.
- `cd frontend && npm run test:coverage` — correcto.
- `cd frontend && npm run lint` — correcto.

## Prueba manual

1. Arrancar el backend con `cd Backend && mvn spring-boot:run`.
2. Ejecutar una petición con JWT inválido y revisar un `WARN` con URI y motivo sin token completo.
3. Crear o cambiar el estado de una incidencia y revisar el `INFO` con su identificador.
4. Ejecutar una operación frontend que falle y comprobar que el mensaje pasa por `logger.error`.
