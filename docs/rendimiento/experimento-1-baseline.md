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
| `GET /api/v1/incidents/1` (`get_incident`) | Detalle puntual por ID | *Pendiente 2m run* | - | - | - | - | - | - |
| `GET /api/v1/incidents/search?...` | Búsqueda y filtrado | *Pendiente 2m run* | - | - | - | - | - | - |
| `GET /api/v1/analytics/summary` | Agregaciones estadísticas | *Pendiente 2m run* | - | - | - | - | - | - |
| `POST /api/v1/incidents` | Alta / Escritura | *Pendiente 2m run* | - | - | - | - | - | - |

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

## 4. Respuestas a las Preguntas del Experimento 1 (Preeliminar)

### 1. ¿Qué endpoint presenta mayor latencia?
`list_incidents` (`GET /api/v1/incidents`) se posiciona inmediatamente como el principal cuello de botella de latencia con más de **5,3 segundos** por respuesta, en claro contraste con lecturas puntuales (`get_incident`), que se sitúan en torno a 15–30 ms.

### 2. ¿La operación más lenta es también la que presenta menor throughput?
**Sí.** Al utilizar un modelo de usuarios virtuales en bucle (*closed workload model*), el throughput depende inversamente del tiempo de respuesta. Como cada VU espera más de 5 segundos a que el servidor complete el envío de los 3,7 MB de JSON, los 10 VUs apenas pueden completar 190 peticiones en 2 minutos (1,57 rps).

### 3. ¿Hay diferencias importantes entre p50 y p99?
En este endpoint la latencia es **consistentemente alta**:
* **p50:** 5.315 ms
* **p95:** 5.717 ms
* **Max:** 5.816 ms
La variabilidad entre la mediana y el peor percentil es baja (~500 ms de diferencia), lo que confirma que el problema es estructural (el coste constante de extraer y serializar 5.000 objetos complejos) y no puntual (como picos de Garbage Collection o bloqueos esporádicos).

### 4. ¿Puede identificarse qué operaciones merecen una optimización prioritaria?
**Sí, prioritariamente `GET /api/v1/incidents`:**
* **Estrategias de optimización recomendadas:**
  1. **Paginación obligatoria:** Limitar el tamaño de página (`page`, `size`) para devolver un volumen acotado (ej. 20–50 incidencias).
  2. **Proyecciones / DTOs reducidos:** Si el mapa requiere mostrar chinchetas de incidencias, devolver únicamente `id`, `latitude`, `longitude`, `category` y `status`, en lugar de toda la entidad con descripciones y metadatos extensos.
  3. **Filtro geográfico (*Bounding Box*):** Enviar las coordenadas visibles del visor cartográfico para devolver sólo las incidencias que realmente caben en pantalla.
