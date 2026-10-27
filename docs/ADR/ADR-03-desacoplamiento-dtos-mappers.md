# ADR-03: Desacoplamiento de capa de datos y API mediante DTOs y Mappers dedicados

* Estado: aceptado
* Fecha: 2026-10-25

## Contexto
En arquitecturas basadas en Spring Boot y Spring Data JPA, es una tentación habitual exponer las entidades del modelo de dominio relacional (`Usuario`, `Incidencia`, `Department`, etc.) directamente en los controladores REST y devolverlas al frontend.
Sin embargo, esto provocaba:
1. Acoplamiento directo del contrato público de la API REST a las estructuras internas de la base de datos.
2. Fugas de seguridad potenciales (por ejemplo, serialización inadvertida de atributos confidenciales como `passwordHash` o identificadores internos).
3. Problemas de rendimiento y serialización como `LazyInitializationException` o bucles infinitos por relaciones bidireccionales en Hibernate/Jackson.
4. Complejidad en la validación de peticiones de entrada (las restricciones de validación de formulario mezcladas con las restricciones de persistencia).

## Opciones consideradas
* **Opción 1: Exposición directa de entidades JPA con anotaciones de serialización (`@JsonIgnore`, `@JsonView`):** Utilizar las propias entidades decoradas para ocultar campos no deseados.
* **Opción 2: Librería de mapeo reflexivo/automático (ModelMapper o MapStruct):** Generación automática de código o mapeo en tiempo de ejecución por reflexión.
* **Opción 3: DTOs dedicados con capa de Mappers desacoplados (`BaseMapper` e implementaciones explícitas en `mappers/`):** Definir DTOs específicos para cada caso de uso y mappers que transforman manualmente de forma predecible y fuertemente tipada en la capa de servicios (`service`).

## Decisión tomada
Optamos por la **Opción 3: DTOs dedicados y capa de Mappers desacoplados**, implementados bajo la interfaz `BaseMapper<E, D>` en el paquete `org.bulbaseros.mappers`, gestionados como componentes de Spring e invocados exclusivamente por la capa de servicio (`service`).

### Razón / Por qué se tomó la decisión
* **Encapsulación estricta:** Los controladores REST solo interactúan con DTOs de petición y respuesta, garantizando que ninguna entidad JPA se exponga al cliente.
* **Trazabilidad y ausencia de 'magia':** A diferencia de las librerías basadas en reflexión (que introducen costes de rendimiento y comportamientos inesperados) o procesadores de anotaciones complejos, los mappers explícitos son fáciles de depurar, tipar y testear de forma unitaria.
* **Mantenibilidad y reducción de boilerplate:** El uso complementario de Lombok en las clases DTO (`@Data`, `@Builder`, `@AllArgsConstructor`) mantiene las clases concisas sin sacrificar la claridad del mapeo.
* **Flexibilidad de evolución:** Permite versionar o cambiar el modelo de datos relacional sin romper los contratos públicos JSON que consumen el frontend o clientes externos.

## Consecuencias
* **Positivo:** Eliminación de errores de serialización JSON y `LazyInitializationException`, protección robusta contra exposición de credenciales y datos sensibles, y desacoplamiento limpio entre capas.
* **Negativo:** Requiere escribir y mantener clases DTO adicionales y sus respectivos mappers.
* **Neutral:** El equipo debe seguir la convención arquitectónica de inyectar y utilizar los mappers en `service`, manteniendo los controladores limpios de lógica de transformación.
