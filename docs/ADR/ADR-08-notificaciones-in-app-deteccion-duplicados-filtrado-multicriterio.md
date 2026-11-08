# ADR-08: Notificaciones Persistentes In-App, Detección Espaciotemporal de Duplicados y Filtrado Dinámico RBAC

* Estado: aceptado
* Fecha: 2026-11-08

## Contexto
La Feature 008 (HU19–HU21) introduce tres capacidades críticas transversales en UrbanPulse:
1. **HU19:** Búsqueda y filtrado multicriterio dinámico (texto libre, categoría, rango de fechas y estado de triaje restringido por RBAC).
2. **HU20:** Detección y sugerencia automática de incidencias duplicadas evaluando proximidad geográfica ($\le 60\text{ m}$), ventana temporal ($\le 48\text{ h}$) y afinidad temática/semántica, con alertas dirigidas a operadores y técnicos.
3. **HU21:** Notificaciones persistentes de cambios de estado dirigidas al ciudadano autor y al personal asignado, gestionadas mediante una bandeja in-app con contador de no leídas y marcado individual.

Se requería definir la arquitectura técnica para la persistencia de notificaciones, la evaluación de duplicidad geográfica y el filtrado dinámico sin introducir dependencias externas que complejicen el despliegue monolítico modular de la plataforma.

## Opciones consideradas
* **Opción 1: Infraestructura distribuida con brokers de mensajería externa y motor de búsqueda (RabbitMQ/Kafka + Elasticsearch):** Delegar las notificaciones en un broker AMQP/Kafka y las búsquedas/duplicados en un cluster Elasticsearch.
* **Opción 2: Gestión efímera en memoria en frontend sin persistencia:** Filtrar los arrays de incidencias exclusivamente en el cliente web y mostrar notificaciones como simples toasts volátiles que desaparecen al recargar la página.
* **Opción 3: Persistencia Relacional en PostgreSQL, Spring Data JPA Specifications y Servicio de Dominio para Duplicados:**
  - Modelar la entidad `Notification` en PostgreSQL vinculada a `User` e `Incidencia` con estado de lectura (`is_read`) y tipo de evento.
  - Generar notificaciones de forma síncrona/transaccional ante cambios de estado válidos en la capa de servicios.
  - Implementar la búsqueda multicriterio con `JpaSpecificationExecutor` combinando predicados dinámicos con Bean Validation y control perimetral RBAC.
  - Calcular la proximidad geográfica mediante la fórmula de Haversine en el servicio de detección de duplicados y aplicar similitud léxica normalizada (tokenización sin tildes ni mayúsculas).

## Decisión tomada
Optamos por la **Opción 3: Persistencia Relacional en PostgreSQL, JPA Specifications y Servicio de Dominio Especializado**.

### Razón / Por qué se tomó la decisión
* **Simplicidad operativa y alineación con la arquitectura:** Cumple el principio de la `constitution/` de mantener una arquitectura desacoplada y contenida en Spring Boot + PostgreSQL, sin añadir servicios pesados externos como Kafka o Elasticsearch innecesarios para la escala actual.
* **Integridad transaccional y persistencia garantizada:** Cada cambio de estado genera la notificación en la misma transacción o inmediatamente después, asegurando que el ciudadano y los técnicos nunca pierdan el historial de eventos tras recargar la interfaz.
* **Seguridad y aislamiento RBAC:** El filtrado con JPA Specifications permite excluir o incluir el predicado de estado según los claims del token JWT del usuario de forma transparente.
* **Eficiencia y precisión:** La fórmula de Haversine ofrece precisión métrica para el umbral de 60 metros sobre coordenadas WGS84, y la coincidencia léxica cubre los casos de uso sin requerir dependencias complejas de procesamiento de lenguaje natural.

## Consecuencias
* **Positivo:** Trazabilidad completa de las notificaciones, persistencia histórica de alertas y soporte multicriterio eficiente en backend y frontend.
* **Positivo:** Cero sobrecarga de infraestructura; el despliegue con Docker Compose sigue dependiendo exclusivamente de PostgreSQL.
* **Positivo:** Pruebas unitarias e integración sencillas con Mockito, `@DataJpaTest` y MockMvc.
* **Negativo:** El cliente web consulta periódicamente o al interactuar el contador de notificaciones no leídas mediante endpoints REST en lugar de una conexión WebSocket persistente.
