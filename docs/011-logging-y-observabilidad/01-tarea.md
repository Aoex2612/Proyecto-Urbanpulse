# 011 — Logging y Observabilidad — Tarea 1: Registro de errores no controlados en backend (RF: RF-1, RNF: NFR-2)

## Resumen global

Se completó la Tarea 1 de la feature 011: se añadió el logger con `@Slf4j` al `GlobalExceptionHandler` y se registró en el manejador `handleGenericException` (`@ExceptionHandler(Exception.class)`) cualquier excepción no controlada capturada, invocando `log.error(...)` con el stacktrace de la excepción.

El contrato de respuesta HTTP no se alteró: el cuerpo del `ErrorResponse` mantiene el mensaje genérico ("Ha ocurrido un error interno en el servidor") y el estado 500, por lo que no se expone información sensible al cliente.

Ficheros modificados:

- `Backend/src/main/java/org/bulbaseros/exception/GlobalExceptionHandler.java`
- `Backend/src/test/java/org/bulbaseros/exception/GlobalExceptionHandlerTest.java`

## Subtareas

### 1. Añadir logger con `@Slf4j` al `GlobalExceptionHandler`

- Se añadió la anotación Lombok `@Slf4j` sobre la clase, siguiendo la convención ya usada en componentes como `CircuitBreaker`, `IncidentService` o los controllers/analytics.
- Lombok genera el campo final `log` de tipo `org.slf4j.Logger` para el logger de la clase (`GlobalExceptionHandler`).

### 2. Registrar errores no controlados con `log.error` e incluir stacktrace

- En `handleGenericException` se invoca `log.error("Error no controlado en el procesamiento de la petición", ex)`.
- Al pasar la excepción como último argumento al logger, Logback incluye el stacktrace en la salida del error.
- El mensaje es descriptivo y no concatena el mensaje de la excepción, evitando filtrar datos sensibles (NFR-1).

## Decisiones de diseño

- Se usó el formato `log.error(msg, ex)` (mensaje + throwable) en lugar de placeholders con argumentos, para garantizar de forma explícita la inclusión del stacktrace.
- El mensaje del log es descriptivo y fijo ("Error no controlado en el procesamiento de la petición"), sin interpolación del mensaje de la excepción, en línea con la política de no redacción de datos sensibles (NFR-1).

## Problemas y solución

- El `ListAppender` de la versión de logback del classpath (1.5.16) expone la colección de eventos mediante el campo público `list` y no mediante `getEvents()`; además, la interfaz `IThrowableProxy` no expone el método `getThrowable()`, por lo que en el test se accede al throwable del proxy concreto.
- Se ajustó el test a esta API y se validó que el error se registra con nivel ERROR y con stacktrace.

## Comandos de test ejecutados

```bash
cd Backend && mvn -q -Dtest=GlobalExceptionHandlerTest test
# Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

## Requisitos cubiertos

RF-1 y NFR-2 en la capa de excepciones: el error no controlado se registra con nivel ERROR, mensaje descriptivo e stacktrace; la NFR-2 exige que los errores no controlados se registren obligatoriamente con nivel ERROR.

La validación formal contra los criterios de aceptación de `spec.md` (caso RF-1: "La RF-1 quedará verificada con un test unitario/integración que confirme que el error no controlado se registra con nivel ERROR y stacktrace") se enmarca en la fase de validación posterior.
