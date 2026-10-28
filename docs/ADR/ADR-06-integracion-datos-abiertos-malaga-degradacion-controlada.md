# ADR-06: Integración con Datos Abiertos de Málaga y Degradación Controlada

* Estado: propuesto
* Fecha: 2026-10-28

## Contexto
UrbanPulse requiere obtener la dirección postal oficial (HU04) y detectar automáticamente el barrio y distrito municipal (HU15) a partir de las coordenadas geográficas capturadas en el mapa, utilizando como fuente oficial la plataforma de Datos Abiertos del Ayuntamiento de Málaga (`datosabiertos.malaga.eu`). 
Asimismo, la constitución y los requisitos de calidad del sistema imponen el principio de *Degradación Controlada*: la indisponibilidad, latencia o fallo en los servicios o datasets externos nunca debe bloquear el registro de una incidencia ni degradar críticamente la experiencia de la ciudadanía.

## Opciones consideradas
* **Opción 1: Consultas directas desde el navegador (Frontend) a la API de datos abiertos.**
  - Las peticiones se originan desde la aplicación cliente en React directamente hacia los endpoints externos.
* **Opción 2: Infraestructura espacial compleja con PostGIS y capas vectoriales persistidas en base de datos.**
  - Incorporar extensiones geoespaciales avanzadas y almacenar internamente la totalidad del callejero y delimitaciones territoriales.
* **Opción 3: Servicio backend de integración y resolución territorial (`MalagaOpenDataService`) con proxy REST y degradación controlada.**
  - El backend centraliza la comunicación con los servicios y datasets de datos abiertos de Málaga, gestiona timeouts estrictos (3 segundos), aplica degradación asignando `null` ante fallos, y provee un endpoint ligero para la previsualización de dirección en el formulario frontend.

## Decisión tomada
Optamos por la **Opción 3: Servicio backend de integración y resolución territorial (`MalagaOpenDataService`) con proxy REST y degradación controlada**, porque desacopla al frontend de la infraestructura externa, evita problemas de CORS y latencia descontrolada en el cliente, y asegura que la lógica de normalización de distrito y barrio se aplique de forma consistente y atómica en el ciclo de vida del alta de incidencias.

### Razón / Por qué se tomó la decisión
* **Garantía de degradación controlada:** El backend aísla las llamadas HTTP a la API externa mediante un cliente con timeout configurado a 3000 ms. Si la fuente externa no responde o falla, el servicio captura la excepción limpiamente y prosigue el alta persistiendo valores nulos (`null`), visualizados como `"Sin asignar"`, cumpliendo estrictamente con la constitución del proyecto.
* **Seguridad, encapsulamiento y prevención de CORS:** Centralizar las consultas en el servidor Spring Boot elimina restricciones de origen cruzado en los navegadores y evita exponer URLs o configuraciones de proveedores externos en el cliente web.
* **Arquitectura evolutiva y simplicidad:** Permite mantener PostgreSQL relacional estándar sin introducir sobrecarga de extensiones GIS en el Sprint 1, manteniendo la promesa de arquitectura simple y medible.

## Consecuencias
* **Positivo / Alivio:** 
  - Trazabilidad y robustez unificada en el servidor ante caídas de la red externa.
  - El frontend interactúa únicamente con el contrato REST canónico de UrbanPulse (`/api/geo/reverse` e `/api/incidents`).
  - La normalización de barrio y distrito es idéntica independientemente del canal de entrada.
* **Negativo / Riesgo:** 
  - Introduce una dependencia de red externa durante el proceso de guardado o previsualización, requiriendo un manejo riguroso de timeouts para no penalizar el tiempo de respuesta.
* **Neutral / Compromiso:** 
  - Se debe implementar un mock en pruebas unitarias y de integración para simular respuestas exitosas, fallos de red y timeouts del portal de datos abiertos sin depender de la conectividad en entornos de integración continua (CI).
