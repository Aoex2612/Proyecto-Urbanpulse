# ADR-08: Motor de Analítica Agregada, Evaluación de SLAs y Asistencia Inteligente Desacoplada

* Estado: aceptado
* Fecha: 2026-11-06

## Contexto
La Feature 009 (*Analítica Urbana y Reportes*, HU22–HU25) exige proveer capacidades de análisis cuantitativo y temporal en tiempo real sobre el histórico de incidencias de UrbanPulse:
1. Cálculo de métricas operativas (Tiempo Medio de Triaje - MTTT y Tiempo Medio de Resolución - MTTR).
2. Evaluación rigurosa de cumplimiento de SLAs por niveles de prioridad (`CRITICAL`: 3 días, `HIGH`: 7 días, `MEDIUM`: 15 días, `LOW`: 30 días).
3. Agregación territorial dinámica por distrito y barrio con filtros temporales históricos.
4. Generación de informes ejecutivos descargables en formato PDF.
5. Recomendación asistida del departamento responsable con estimación de grado de certeza probabilística en el triaje.

Se requería definir el diseño arquitectónico de estas cuatro piezas garantizando el desacoplamiento estricto, la autonomía operativa (cero dependencias de servicios externos de pago que puedan degradar la disponibilidad) y un tiempo de respuesta inferior a 500 ms sobre más de 5.000 incidencias.

## Opciones consideradas

### 1. Motor de Agregación y Métricas de SLAs
* **Opción 1A: Almacén analítico dedicado / Data Warehouse separado (ej. ClickHouse o Elasticsearch):** Desplegar una base de datos columnar independiente con réplica continua mediante eventos (CDC).
* **Opción 1B: Servicio de Analítica sobre PostgreSQL con Consultas Agregadas Indexadas y Cálculos Temporales en Capa de Negocio:** Aprovechar la base de datos relacional existente (PostgreSQL 16) con índices B-Tree sobre marcas temporales y claves foráneas, delegando las operaciones de agregación y el cómputo de deltas de tiempo en `AnalyticsService` apoyado en `CambiosEstadoRepository` e `IncidenciaRepository`.

### 2. Generación del Informe en PDF
* **Opción 2A: Servicio de renderizado headless en servidor (ej. Puppeteer / Chromium):** Ejecutar una instancia de navegador sin cabeza para maquetar el HTML como PDF.
* **Opción 2B: Generación estructurada cliente-servidor:** La API backend provee un DTO consolidado con la totalidad de métricas ejecutivas e incidencias filtradas (`AnalyticsSummaryResponse`), y el cliente web (o librería ligera de backend) genera de forma determinista y predecible el archivo PDF estructurado con portada ejecutiva, tablas formateadas y estilos oficiales de UrbanPulse.

### 3. Motor de Recomendación Inteligente con Grado de Confianza
* **Opción 3A: Integración con LLM externo vía API de pago (ej. OpenAI / Anthropic):** Invocar modelos de lenguaje remotos para clasificar el departamento.
* **Opción 3B: Motor Heurístico-Bayesiano Autónomo en Capa de Servicio:** Componente local (`IntelligentRecommendationService`) que analiza frecuencias léxicas ponderadas del reporte (título, descripción, categoría y demarcación territorial) contrastadas con los patrones históricos de asignación departamental, generando una puntuación probabilística normalizada (0% a 100%) y activando una advertencia si la confianza es menor al 50%.

## Decisión tomada
Se adoptan las **Opciones 1B, 2B y 3B**:
1. **Métricas y SLAs en Capa de Negocio (1B):** `AnalyticsService` efectúa consultas filtradas y agregadas sobre PostgreSQL, computando con precisión en Java 21 los tiempos entre estados (`status_changes`) y la verificación de SLA, evitando la complejidad y el coste de un segundo motor de base de datos.
2. **Generación de Informes Estructurada (2B):** Se provee el dataset analítico completo a través de la API y se compila el informe PDF con maquetación ejecutiva, sin dependencias de navegadores externos.
3. **Motor Heurístico Autónomo (3B):** `IntelligentRecommendationService` opera 100% en memoria en el servidor, garantizando tiempo de respuesta $< 50$ ms, cero coste por petición, total privacidad de los datos ciudadanos y funcionamiento garantizado sin conexión a internet.

### Razón / Por qué se tomó la decisión
* **Respeto a los principios fundacionales de la Constitución:** *"Los datos externos enriquecen; nunca bloquean"* y *"Se adopta la solución más sencilla que satisfaga los atributos de calidad medidos"*. Evitar APIs externas de pago y bases de datos analíticas adicionales reduce drásticamente los puntos de fallo y los costes de infraestructura.
* **Rendimiento predecible:** El motor heurístico y las consultas agregadas indexadas garantizan respuestas holgadamente por debajo del umbral de 500 ms exigido en la especificación.
* **Trazabilidad y explicabilidad:** El porcentaje de confianza y el departamento sugerido se basan en términos observables y reglas reproducibles, facilitando la auditoría operativa para los analistas y administradores.

## Consecuencias
* **Positivo:** Cero costes recurrentes de APIs de IA de terceros; arquitectura resiliente y autónoma.
* **Positivo:** Alta cohesión y desacoplamiento mediante DTOs inmutables en `org.bulbaseros.dto` y mapeadores en `org.bulbaseros.mappers`.
* **Positivo:** Facilidad para crear pruebas unitarias exhaustivas con datos sintéticos y deterministas sin necesidad de mocks de red externa.
* **Negativo:** El motor heurístico de recomendación depende de un diccionario léxico ponderado y patrones históricos, por lo que requerirá actualizar sus términos si se incorporan nuevos departamentos o categorías muy dispares en el futuro.
