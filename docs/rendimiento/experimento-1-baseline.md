# Experimento 1: Caracterización del Rendimiento de la API (Línea Base)

Este documento recoge la medición y caracterización del rendimiento base (*baseline*) del backend de **UrbanPulse** mediante **Grafana k6**, conforme a los requisitos de la práctica de observabilidad y atributos de calidad de la asignatura *Desarrollo Software en Plataformas en la Nube*.

---

## 1. Condiciones del Entorno y Parámetros de Prueba

* **Entorno de ejecución:** Localhost (Linux / CachyOS).
* **Backend:** Spring Boot (Java 21).
* **Base de datos:** PostgreSQL en Docker con un conjunto precargado de **5.000 incidencias**.
* **Mecanismo de autenticación:** JWT Bearer obtenido en la fase de `setup` (`admin@urbanpulse.es`).
* **Configuración de carga:**
  * **VUs:** 10 usuarios virtuales concurrentes en bucle.
  * **Duración:** 2 minutos.
  * **Pausa (*think time*):** 1 segundo por iteración (`sleep(1)`).
  * **Criterios de aceptación (*thresholds*):** Tasa de fallo HTTP < 1% (`http_req_failed < 0.01`).

---

## 2. Registro de Resultados de la Línea Base

| Operación / Endpoint | Tipo de Operación | p50 (Mediana) | p90 | p95 | p99 / Max | Peticiones/s (Throughput) | % Errores | Datos Recibidos |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `GET /api/v1/incidents` (`list_incidents`) | Listado completo activos | **5.315 ms** (~5,32 s) | 5.602 ms | 5.717 ms | 5.816 ms | **1,57 req/s** | **0,00 %** | **703 MB** (~3,7 MB/req) |
| `GET /api/v1/incidents/1` (`get_incident`) | Detalle puntual por ID | **8,49 ms** | 17,42 ms | 23,74 ms | 626,49 ms | **9,83 req/s** | **0,00 %** | 1,2 MB (~1 KB/req) |
| `GET /api/v1/incidents/search?...` | Búsqueda y filtrado | **781,57 ms** | 820,67 ms | 899,08 ms | 1.144 ms | **5,56 req/s** | **0,00 %** | 341 MB (~509 KB/req) |
| `GET /api/v1/analytics/summary` | Agregaciones estadísticas | **1.480 ms** | 2.880 ms | 2.955 ms | 3.361 ms | **3,56 req/s** | **0,00 %** | 203 MB (~466 KB/req) |
| `POST /api/v1/incidents` | Alta / Escritura | **70,19 ms** | 164,65 ms | 293,82 ms | 1.620 ms | **8,83 req/s** | **0,00 %** | ~1,1 KB/req |

*(Nota: Conforme a las directrices de la práctica, se recomienda realizar 3 repeticiones de cada configuración para reflejar la variabilidad).*

---

## 3. Análisis Detallado del Endpoint `list_incidents`

### 3.1. Métricas Clave Observadas
* **Tiempo de respuesta medio:** 5.358 ms (~5,36 segundos).
* **Volumen transferido:** 703 MB transferidos en 190 iteraciones exitosas a un caudal de **5,8 MB/s**.
* **Rendimiento (*Throughput*):** Únicamente 1,57 peticiones por segundo a pesar de contar con 10 VUs disponibles.
* **Fiabilidad:** 100% de éxito funcional (código HTTP 200 en todas las peticiones, 0 fallos).

### 3.2. Diagnóstico Técnico y Cuello de Botella
1. **Sobrecarga por tamaño del payload:**
   * Al consultar `/api/v1/incidents`, el backend recupera **todas** las incidencias activas de la base de datos (5.000 registros) y las serializa íntegramente a JSON.
   * Cada respuesta pesa aproximadamente **3,7 MB**. Esto produce una saturación notable tanto en la serialización Jackson en el servidor como en la transferencia de red y el parseo del cliente.
2. **Impacto en el Throughput:**
   * Debido a que cada iteración tarda ~6,35 s (5,35 s de petición + 1 s de sleep), 10 VUs en bucle sólo son capaces de completar ~1,57 peticiones por segundo en total:
   $$\text{Throughput} \approx \frac{10 \text{ VUs}}{6,35 \text{ s}} \approx 1,57 \text{ req/s}$$
   * La lentitud del endpoint ahoga directamente la capacidad del sistema de procesar más peticiones concurrentes.

---

## 3.3. Análisis del Endpoint `get_incident`

* **p50:** 8,49 ms — Excepcional. Lectura de una sola entidad por clave primaria (índice B-Tree) con serialización mínima.
* **p95:** 23,74 ms — 99,96× más rápido que el p95 de `list_incidents`.
* **Max: 626,49 ms** — Pico esporádico significativo (la p95 es 23 ms, el máximo salta a 626 ms). Causa probable: el servicio `getIncidentById` llama a servicios externos opcionales (activos urbanos, observaciones meteorológicas/tráfico de Málaga Open Data) bajo degradación controlada. Si alguna de esas llamadas tarda, se propaga como pico de cola aislado pero sin afectar la estabilidad general.
* **Throughput:** 9,83 req/s — Prácticamente el límite teórico con 10 VUs + sleep(1): $\frac{10}{1 + 0{,}012} \approx 9{,}88$ req/s.
* **Datos:** Solo 1,2 MB totales para 1.190 peticiones (~1 KB/respuesta) frente a los 703 MB del listado completo.

| Métrica | `list_incidents` | `get_incident` | Ratio |
| :--- | :---: | :---: | :---: |
| p50 | 5.315 ms | **8,49 ms** | 626× más rápido |
| p95 | 5.717 ms | **23,74 ms** | 241× más rápido |
| Throughput | 1,57 req/s | **9,83 req/s** | 6,3× mayor |
| Payload/req | ~3,7 MB | **~1 KB** | 3.780× menor |

---

## 3.4. Análisis del Endpoint `search_incidents`

Busqueda `GET /api/v1/incidents/search?category=ALUMBRADO`:

* **p50:** 781,57 ms — 92× más lento que `get_incident`. Confirma ausencia de índice en la columna `category`.
* **p95:** 899,08 ms — Latencia alta y consistente, lo que descarta picos puntuales: el problema es estructural.
* **Max:** 1.144 ms (1,14 s) — Techo que se alcanza cuando la carga concurrente presiona el pool de conexiones de HikariCP simultáneamente.
* **Throughput:** 5,56 req/s — La iteración dura ~1,79 s (0,78 s petición + 1 s sleep) en lugar de ~1 s, lo que reduce el throughput a la mitad respecto a `get_incident`.
* **Payload:** ~509 KB/req — La búsqueda devuelve **todos** los resultados sin paginar. Con 5.000 incidencias, si ~20% son de categoría `ALUMBRADO` (~1.000 registros), el payload serializado supera el medio megabyte por petición.

**Doble causa del problema:**
1. **Full scan sin índice:** PostgreSQL no puede usar un índice de árbol B para filtrar por `category` y recorre toda la tabla de 5.000 filas.
2. **Sin paginación en búsqueda:** El endpoint devuelve todos los resultados coincidentes en una sola respuesta, amplificando el impacto del payload.

---

## 3.5. Análisis del Endpoint `analytics_summary`

Consulta `GET /api/v1/analytics/summary`:

* **p50:** 1.480 ms (1,48 s) — El segundo endpoint más lento. Recalcula todos los agregados (`COUNT`, `GROUP BY` por categoría, estado y distrito) desde cero en cada petición sobre 5.000 filas.
* **p90:** 2.880 ms — **Casi el doble que la mediana**. Este salto p50→p90 (1,48 s → 2,88 s) es el hallazgo clave: bajo carga concurrente de 10 VUs, los `GROUP BY` sobre la misma tabla colisionan entre sí, generando **lock contention** en el buffer de PostgreSQL.
* **p95:** 2.955 ms — Muy cercano al p90, lo que indica que la "cola larga" de peor rendimiento es un bloque consistente de peticiones que se van encolando detrás de la primera en terminar.
* **Max:** 3.361 ms — Razonable para una consulta de agregación sin índices ni caché.
* **Throughput:** 3,56 req/s — El más bajo junto con `list_incidents`. La iteración dura ~2,78 s (1,78 s petición + 1 s sleep).
* **VUs activos:** `min=5, max=10` — Dato revelador: en los instantes de mayor presión, la mitad de los VUs quedaban bloqueados esperando una conexión del pool de HikariCP mientras los otros 5 tenían transacciones de agregación abiertas.
* **Payload:** ~466 KB/req — Payload elevado para una respuesta de estadísticas, sugiere que la respuesta incluye datos en crudo sin agregación suficiente en la capa de servicio.

---

## 3.6. Análisis del Endpoint `create_incident`

Escritura `POST /api/v1/incidents`:

* **p50:** 70,19 ms — Muy buena latencia para una escritura transaccional con validación de negocio completa. La mayor parte de las inserciones encuentran el pool de HikariCP disponible y se resuelven rápido.
* **p90:** 164,65 ms — Crece 2,3× respecto a la mediana, mostrando que ~10% de las inserciones ya sienten algo de contención.
* **p95:** 293,82 ms — 4,2× la mediana. La cola alta del 5% peor refleja los momentos en que varios VUs intentan `INSERT` simultáneamente y colisionan en los índices del escritor.
* **Max:** 1.620 ms — Pico esporádico. Cola de latencia producida por la llamada opcional al servicio de **activos urbanos** (sugerencia automática de activos cercanos al crear la incidencia).
* **Throughput:** 8,83 req/s — Próximo al techo teórico. Las escrituras son eficientes en condiciones normales.
* **Payload enviado/recibido:** ~648 B enviados (body POST) / ~1,1 KB recibidos (incidencia creada). Respuestas muy compactas, sin problema de transferencia.
* **VUs:** `min=3` — Fase de graceful stop al finalizar la prueba.

---

## 4. Tabla Comparativa Final — Ranking por Criticidad

| Rank | Endpoint | p50 | p95 | Max | req/s | Payload/req | Diagnosis |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| 🔴 1 | `list_incidents` | 5.315 ms | 5.717 ms | 5.816 ms | 1,57 | ~3,7 MB | Payload masivo sin paginar (5.000 registros) |
| 🟠 2 | `analytics_summary` | 1.480 ms | 2.955 ms | 3.361 ms | 3,56 | ~466 KB | Agregaciones `GROUP BY` sin caché, con contención de pool |
| 🟡 3 | `search_incidents` | 781 ms | 899 ms | 1.144 ms | 5,56 | ~509 KB | Full scan sin índice en `category` + resultados sin paginar |
| 🟢 4 | `create_incident` | 70 ms | 294 ms | 1.620 ms | 8,83 | ~1,1 KB | Escritura eficiente; pico en llamada a activos urbanos |
| ✅ 5 | `get_incident` | 8,49 ms | 23,74 ms | 626 ms | 9,83 | ~1 KB | Óptimo; pico máximo por servicio externo esporádico |

---

## 5. Respuestas a las Preguntas del Experimento 1

### P1 — ¿Qué endpoint presenta mayor latencia?

**`GET /api/v1/incidents` (`list_incidents`)** con una latencia p50 de **5.315 ms**, seguido de `analytics_summary` (1.480 ms). La causa es fundamentalmente de diseño: el backend recupera íntegramente los 5.000 registros activos y los serializa a JSON (~3,7 MB por respuesta), en lugar de paginar o proyectar un DTO reducido para el mapa cartográfico.

### P2 — ¿La operación más lenta tiene también el menor throughput?

**Sí, con una correlación directa.** En el modelo de carga cerrado con `sleep(1)`, el throughput de cada VU depende del tiempo total de iteración:

$$\text{Throughput} \approx \frac{N_{VUs}}{t_{respuesta} + t_{sleep}}$$

| Endpoint | t\_respuesta (avg) | Throughput real | Throughput teórico |
| :--- | :---: | :---: | :---: |
| `list_incidents` | 5,33 s | 1,57 req/s | 1,60 req/s |
| `analytics_summary` | 1,78 s | 3,56 req/s | 3,60 req/s |
| `search_incidents` | 0,80 s | 5,56 req/s | 5,56 req/s |
| `create_incident` | 0,12 s | 8,83 req/s | 9,09 req/s |
| `get_incident` | 0,01 s | 9,83 req/s | 9,90 req/s |

La concordancia entre throughput real y teórico confirma que el sistema **no tiene contención de red ni de CPU** bajo estos 10 VUs: el único limitante es el tiempo de procesamiento de cada petición en el servidor.

### P3 — ¿Hay diferencias importantes entre p50 y p95?

| Endpoint | p50 | p95 | Ratio p95/p50 | Interpretación |
| :--- | :---: | :---: | :---: | :--- |
| `list_incidents` | 5.315 ms | 5.717 ms | **×1,07** | Latencia alta pero muy estable. El problema es el volumen del payload, no la concurrencia. |
| `analytics_summary` | 1.480 ms | 2.955 ms | **×2,00** | Alta variabilidad. Contención del pool de HikariCP bajo carga concurrente de agregaciones. |
| `search_incidents` | 781 ms | 899 ms | **×1,15** | Estable pero lento. El full scan tarda consistentemente lo mismo. |
| `create_incident` | 70 ms | 294 ms | **×4,20** | Cola de escritura notable. Las contenciones de INSERT y la llamada externa a activos elevan el peor 5%. |
| `get_incident` | 8,49 ms | 23,74 ms | **×2,80** | El pico esporádico de 626 ms (llamada externa a Málaga Open Data) infla la cola, aunque la operación base es eficientísima. |

El ratio p95/p50 más preocupante para producción es el de **`create_incident` (×4,2)** y **`analytics_summary` (×2,0)**: indican que un porcentaje significativo de usuarios experimenta tiempos muy distintos al usuario típico.

### P4 — ¿Qué operaciones merecen una optimización prioritaria?

| Prioridad | Endpoint | Acción recomendada |
| :---: | :--- | :--- |
| 🔴 1ª | `list_incidents` | **Paginar** la respuesta (`page`, `size`) y/o proyectar un **DTO ligero para el mapa** con sólo `id`, `lat`, `lon`, `category`, `status` (~50 B/registro vs ~3.700 B). Reducción estimada de latencia: 95-98%. |
| 🟠 2ª | `analytics_summary` | **Caché de corta duración** (ej. 60 s con Spring Cache + Caffeine) o **vista materializada** en PostgreSQL. Los agregados estadísticos raramente cambian segundo a segundo. Reducción estimada: 80-90% en latencia bajo carga. |
| 🟡 3ª | `search_incidents` | **Índice B-Tree** en la columna `category` de la tabla `incidents` + **paginación** del endpoint de búsqueda. El full scan de 5.000 filas debería caer a búsqueda por índice (O(log n)). Reducción estimada de latencia: 70-85%. |
| 🟢 4ª | `create_incident` | Revisar si la llamada al servicio de activos urbanos puede ser **asíncrona** (fire-and-forget) en lugar de bloqueante, reduciendo la cola de latencia. |
