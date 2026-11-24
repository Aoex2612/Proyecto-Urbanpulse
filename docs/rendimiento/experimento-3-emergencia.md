# Experimento 3: Simulación de un Pico de Incidencias Urbanas (Emergencia Urbana / Spike Test)

Este documento recoge la metodología, resultados obtenidos y análisis arquitectónico correspondientes al **Experimento 3** de la práctica de observabilidad y atributos de calidad de *Desarrollo Software en Plataformas en la Nube*.

---

## 1. Contexto y Objetivos

* **Escenario de negocio:** Simulación de un episodio extraordinario de lluvias torrenciales e inundaciones en Málaga, provocando un aluvión repentino de reportes ciudadanos de emergencias (alcantarillas desbordadas, bolsas de agua, problemas de tráfico), mientras los ciudadanos y operadores consultan intensamente el mapa de incidencias.
* **Pregunta arquitectónica clave formulada en la práctica:**
  > *¿Qué ocurre con la creación de incidencias si al mismo tiempo hay muchos usuarios consultando el mapa de incidencias?*
* **Atributos de calidad evaluados:**
  * **Fiabilidad:** Capacidad de responder sin errores HTTP 5xx bajo choque de concurrencia.
  * **Resiliencia y Concurrencia Mixta (Lectura vs. Escritura):** Cómo compiten las operaciones de lectura masiva y escritura por conexiones de base de datos (HikariCP), bloqueos transaccionales en PostgreSQL y ancho de banda de red/memoria.
  * **Degradación de Latencia:** Impacto en el tiempo de respuesta percibido por usuarios que leen y los que reportan.

---

## 2. Metodología de la Prueba

El script implementado ([k6/experimento-3-emergencia.js](file:///home/alejandro/Documentos/ETSII/4%20curso/Desarrollo%20Software%20en%20Plataformas%20en%20la%20Nube/Proyecto-Urbanpulse/k6/experimento-3-emergencia.js)) ejecutó dos escenarios simultáneos durante ~4 minutos con un máximo de **90 usuarios virtuales concurrentes**:

1. **`map_readers` (Carga continua de lectura en mapa):**
   * Endpoint: `GET /api/v1/incidents`
   * Concurrencia: Escala de 5 a 20 VUs base y subida a 40 VUs en el momento crítico.
   * Total de peticiones ejecutadas: **446 lecturas**.
2. **`emergency_writers` (Pico repentino o Spike de escritura):**
   * Endpoint: `POST /api/v1/incidents` (con geolocalizaciones dinámicas y descripciones de emergencia pluvial).
   * Concurrencia: Pico violento de 1 a **50 VUs simultáneos** en 15 segundos, sostenido durante 1 minuto.
   * Total de peticiones ejecutadas: **575 escrituras**.

Reporte interactivo generado: [k6/reports/exp3_emergencia_spike.html](file:///home/alejandro/Documentos/ETSII/4%20curso/Desarrollo%20Software%20en%20Plataformas%20en%20la%20Nube/Proyecto-Urbanpulse/k6/reports/exp3_emergencia_spike.html).

---

## 3. Resultados Obtenidos

### Resumen Global de Ejecución

| Métrica | Valor Obtenido | Umbral (*Threshold*) Definido | Estado |
| :--- | :---: | :---: | :---: |
| **Peticiones Totales (HTTP)** | **1.022 peticiones** (1 login en `setup()` + 1.021 bajo carga) | - | Completado |
| **Checks de Validación bajo Carga** | **1.021 checks** (446 lecturas + 575 escrituras) | - | Completado (100% éxito) |
| **Throughput Global** | **4,24 req/s** | - | Estable bajo saturación |
| **Tasa Global de Errores** | **0,00 %** (0 de 1.022) | `< 5 %` | **SUPERADO (Excelente Fiabilidad)** |
| **Tasa Fallo Lecturas Mapa** | **0,00 %** (0 de 446) | `< 5 %` | **SUPERADO** |
| **Tasa Fallo Escritura Emergencias** | **0,00 %** (0 de 575) | `< 5 %` | **SUPERADO** |
| **Latencia p95 Mapa (`read_map_duration`)** | **20.273 ms (~20,3 s)** | `< 1.500 ms` | **INCUMPLIDO (Cuello de botella)** |
| **Latencia p95 Creación (`write_incident_duration`)** | **15.210 ms (~15,2 s)** | `< 2.500 ms` | **INCUMPLIDO (Contención severa)** |
| **Volumen de Datos Transferidos** | **2,0 GB recibidos** | - | **8,3 MB/s** |

> **Nota sobre el desglose de peticiones y checks:** En la ejecución global de k6 se contabilizan un total de **1.022 peticiones HTTP**: 1 petición técnica inicial de login ejecutada durante la fase `setup()` para obtener el token JWT de operador, y **1.021 peticiones evaluadas con checks bajo carga** correspondientes a los dos escenarios concurrentes: 446 lecturas de mapa (`GET /incidents`) y 575 reportes de emergencia (`POST /incidents`) (446 + 575 = 1.021).

---

### Desglose Detallado por Operación

| Métrica de Latencia | Lectura del Mapa (`GET /incidents`) | Creación de Incidencia (`POST /incidents`) | Global HTTP |
| :--- | :---: | :---: | :---: |
| **Mínimo (Min)** | 2.528,30 ms (2,53 s) | 135,23 ms (0,14 s) | 135,23 ms |
| **Mediana (p50)** | **10.538,03 ms (10,54 s)** | **5.550,68 ms (5,55 s)** | **6.820,00 ms (6,82 s)** |
| **Media (Avg)** | 12.107,10 ms (12,11 s) | 6.839,50 ms (6,84 s) | 9.130,00 ms |
| **Percentil 90 (p90)**| 19.922,15 ms (19,92 s) | 12.982,26 ms (12,98 s) | 19.640,00 ms |
| **Percentil 95 (p95)**| **20.273,28 ms (20,27 s)** | **15.210,33 ms (15,21 s)** | **19.860,00 ms (19,86 s)** |
| **Máximo (Max)** | 20.794,44 ms (20,79 s) | 15.895,66 ms (15,90 s) | 20.790,00 ms |

---

## 4. Diagnóstico y Análisis Arquitectónico

### Hallazgo 1: Fiabilidad impecable (0 % errores) pero degradación severa de latencia
A pesar de la brusquedad del pico (subida instantánea a 50 VUs creando incidencias y 40 VUs leyendo), **el servidor no arrojó un solo error HTTP 500, 503 o 504**. Todos los 1.021 checks bajo carga pasaron con éxito (status 200 para las 446 lecturas y 201 para las 575 creaciones), sumando junto al login inicial 1.022 peticiones HTTP exitosas. El sistema es resiliente a caídas, pero sufre de una **altísima contención en cola**.

### Hallazgo 2: ¿Por qué la lectura del mapa fue el doble de lenta que la escritura?
* La creación (`POST`) tuvo una mediana de **5,5 s**, mientras que la lectura del mapa (`GET`) se disparó a **10,5 s** (y más de **20 s** en p95).
* **El factor del Payload (2.0 GB de transferencia de red):** En apenas 4 minutos y con 446 lecturas, se transfirieron **2 Gigabytes de datos** (una media de ~4,5 MB por cada petición de mapa, a un caudal constante de **8,3 MB/s**). 
* El endpoint `GET /api/v1/incidents` devuelve la colección completa sin paginación estricta ni proyección reducida de campos para mapa (GeoJSON ligero). Conforme los 50 VUs escribían nuevas incidencias en la base de datos, cada sucesiva lectura del mapa devolvía un JSON cada vez más voluminoso.
* La serialización Jackson en Spring Boot y la transferencia por socket local saturaron el ciclo de CPU y ancho de banda, penalizando exponencialmente a las lecturas.

### Hallazgo 3: Competencia en el Pool de Conexiones (HikariCP)
* Con 90 VUs activos de forma concurrente y el pool estándar de HikariCP limitado por defecto a **10 conexiones**, las peticiones se apilaron en la cola de espera de conexiones (`hikaricp.connections.pending`).
* Mientras los `POST` requieren una transacción corta de escritura e inserción, los `GET` de toda la tabla retienen la conexión JDBC durante todo el tiempo que PostgreSQL tarda en escanear la tabla, transferir las filas a memoria y mapearlas a entidades JPA.

---

## 5. Respuestas a las Preguntas Guía del Experimento 3

1. **¿Qué ocurre con la creación de incidencias si al mismo tiempo hay muchos usuarios consultando el mapa?**
   * Se produce un fenómeno de **contención bidireccional**. Las inserciones compiten con los escaneos de tabla de las lecturas. Si bien las escrituras tardan menos que las lecturas (mediana 5,5 s frente a 10,5 s), ambas sufren colas de espera en el pool de HikariCP y en los hilos de Tomcat.
2. **¿Aparecen errores durante el pico?**
   * No. La tasa de fallo fue del **0,00 %**. Las peticiones no sufrieron *timeout* de conexión ni desbordamiento con rechazo, lo que demuestra buena tolerancia bajo carga extrema sin caída de procesos.
3. **¿Cuánto aumenta la latencia frente a la línea base?**
   * En la baseline ([baseline.js](file:///home/alejandro/Documentos/ETSII/4%20curso/Desarrollo%20Software%20en%20Plataformas%20en%20la%20Nube/Proyecto-Urbanpulse/k6/baseline.js)), `list_incidents` rondaba los ~15-25 ms y `create_incident` los ~30-40 ms. Durante el pico de emergencia urbana, las latencias se multiplicaron por un factor de **x400 a x800** (alcanzando 15-20 segundos).
4. **¿Qué medidas arquitectónicas mitigarían este cuello de botella?**
   * **Endpoint optimizado para mapa (Bounding Box / Clústeres / DTO ligero):** En lugar de devolver entidades completas con descripciones e historiales, el mapa debe consumir un endpoint paginado o filtrado por coordenadas geográficas (`/api/v1/incidents/map?bbox=...`) con campos mínimos (`id`, `lat`, `lng`, `category`, `status`). Esto reduciría el consumo de red de 2.0 GB a unos pocos megabytes.
   * **Caché de segundo nivel o Redis para lecturas del mapa:** Evitar que cada consulta toque directamente PostgreSQL cuando hay cientos de lecturas concurrentes.
   * **Ajuste del Pool de Conexiones HikariCP:** Incrementar el `maximum-pool-size` (por ejemplo, a 20-30 conexiones si la CPU y PostgreSQL lo admiten) para reducir los tiempos en cola de conexión (`connection-timeout`).
