# Spec 011 — Logging y Observabilidad

**Estado:** propuesta

## Contexto y objetivo
UrbanPulse registra logs de forma parcial y dispersa: solo algunos servicios (analytics, incidentes, contexto externo, circuit breaker) y el filtro JWT cuentan con logger, el manejador global de excepciones no registra los errores capturados, los controllers y services restantes no disponen de logger, y el frontend no centraliza la captura de errores (uso directo de `console`). La cobertura actual es insuficiente para la depuración en desarrollo y para la trazabilidad operativa en incidentes. Esta especificación define los requisitos para completar la cobertura de logs en backend y frontend, sin alterar la lógica de negocio existente.

## Usuarios / actores
- **Desarrollador del equipo:** utiliza los logs para reproducir y diagnosticar incidencias durante el ciclo de desarrollo.
- **Operador municipal / técnico de soporte:** utiliza los logs para reconstruir el historial operativo de una incidencia.
- **Analista / administrador:** consulta los logs cuando se requiera evidencia operativa.

## Historias de usuario
- **H1:** Como desarrollador, quiero que los errores no controlados queden registrados con nivel, mensaje y stacktrace, para reproducirlos con rapidez.
- **H2:** Como operador, quiero que los eventos del ciclo de vida de una incidencia se registren con identificador de incidencia, para reconstruir su historial.
- **H3:** Como equipo de desarrollo, quiero contar con un mecanismo centralizado de registro de errores en el frontend, para sustituir el uso disperso de `console`.

## Requisitos funcionales (criterios de aceptación en EARS)

### Completar la cobertura de logs en el backend (H1, H2)

- RF-1: CUANDO el backend capture una excepción no controlada durante el procesamiento de una petición, ENTONCES el sistema registrará el error con nivel ERROR, mensaje descriptivo y stacktrace en el logger correspondiente.
- RF-2: CUANDO el backend valide un token JWT o reciba un acceso denegado, ENTONCES el sistema registrará el evento de seguridad con la URI de la petición y el motivo del fallo.
- RF-3: CUANDO un servicio dependencia sufra un fallo recuperable que active un fallback, ENTONCES el sistema registrará el fallo con nivel WARN antes de activar el fallback.
- RF-4: CUANDO se registre una incidencia o se produzca un cambio de estado, ENTONCES el sistema incluirá el identificador de la incidencia como contexto en el evento de log.

### Registrar errores en el frontend (H3)

- RF-5: CUANDO el frontend capture un error en una operación asíncrona, ENTONCES el sistema registrará el error con nivel ERROR y contexto de la operación.

### Configurar niveles de log por perfil de ejecución (H1)

- RF-6: EL SISTEMA dispondrá de niveles de log configurados por perfil de ejecución para ERROR, WARN, INFO y DEBUG según corresponda.

## Requisitos no funcionales
- NFR-1: Los mensajes de log no deberán contener credenciales, contraseñas ni tokens completos.
- NFR-2: Los errores no controlados deberán registrarse obligatoriamente con nivel ERROR.

## Casos límite
- SI el contexto externo no está disponible para una incidencia, ENTONCES EL SISTEMA registrará un WARN y omitirá el contexto sin fallar la operación.
- SI el circuit breaker se encuentra en estado OPEN, ENTONCES EL SISTEMA registrará el estado y no realizará llamadas externas.

## Fuera de alcance
- Rediseño o refactor de los logs que ya existen; esta especificación solo contempla completar los puntos que carecen de logger.
- Métricas, tracing distribuido, sistemas de alerta, agregación centralizada (ELK) y logging estructurado en JSON.
- Cambios en la lógica de negocio o en los contratos de la API REST.

## Criterios de aceptación

_Cada criterio debe cumplirse con prueba o demostración para dar por terminada la funcionalidad._

- [ ] La RF-1 quedará verificada con un test unitario/integración que confirme que el error no controlado se registra con nivel ERROR y stacktrace.
- [ ] La RF-2 quedará verificada con test que confirme el registro del evento de validación JWT o acceso denegado.
- [ ] La RF-3 quedará verificada con test que confirme el registro WARN previo al fallback en fallos recuperables.
- [ ] La RF-4 quedará verificada con test que confirme el contexto del identificador de incidencia en los eventos de creación/cambio de estado.
- [ ] La RF-5 quedará verificada con test del mecanismo centralizado de registro en el frontend.
- [ ] La RF-6 quedará verificada con la configuración de niveles por perfil de ejecución.
- [ ] La NFR-1 quedará verificada en el contenido de los mensajes de log generados.

## Dudas abiertas
- **Resuelta (Opción A):** Se determinó que los eventos de autenticación/autorización se registran con nivel `WARN` (token inválido/expirado y acceso denegado), y que la política de redacción excluye contraseñas y tokens completos de los logs.
