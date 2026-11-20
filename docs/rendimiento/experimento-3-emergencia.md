# Experimento 3: Simulación de un Pico de Incidencias Urbanas (Emergencia Urbana / Spike Test)

Este documento recoge la metodología, hipótesis, resultados y análisis arquitectónico correspondientes al **Experimento 3** de la práctica de observabilidad y atributos de calidad de *Desarrollo Software en Plataformas en la Nube*.

---

## 1. Contexto y Objetivos

* **Escenario de negocio:** Simulación de un episodio de lluvias torrenciales e inundaciones en Málaga, provocando un aumento repentino de avisos ciudadanos (alcantarillas desbordadas, balsas de agua, fallos de semáforos), mientras múltiples usuarios y operadores consultan activamente el mapa de incidencias.
* **Pregunta arquitectónica clave:**
  > *¿Qué ocurre con la creación de incidencias si al mismo tiempo hay muchos usuarios consultando el mapa de incidencias?*
* **Atributos de calidad evaluados:**
  * **Resiliencia:** Capacidad del backend de absorber un pico repentino de tráfico de escritura sin bloquearse ni colapsar.
  * **Concurrencia Mixta (Lectura vs. Escritura):** Competencia por conexiones de base de datos (HikariCP pool), bloqueos a nivel de fila/tabla en PostgreSQL e hilos de Tomcat.
  * **Tiempo de Recuperación:** Velocidad con la que el sistema vuelve a las latencias de línea base (baseline) una vez superado el pico de peticiones.

---

## 2. Metodología de Prueba

El script implementado ([k6/experimento-3-emergencia.js](file:///home/alejandro/Documentos/ETSII/4%20curso/Desarrollo%20Software%20en%20Plataformas%20en%20la%20Nube/Proyecto-Urbanpulse/k6/experimento-3-emergencia.js)) ejecuta dos escenarios concurrentes e independientes:

1. **`map_readers` (Carga de lectura continua):**
   * Petición: `GET /api/v1/incidents`
   * Carga: 20 VUs base con incremento hasta 40 VUs en el momento crítico.
   * Representa a ciudadanos y técnicos supervisando el estado de la ciudad en tiempo real.
2. **`emergency_writers` (Spike de escritura):**
   * Petición: `POST /api/v1/incidents`
   * Comportamiento: Carga inicial baja (1 VU), subida repentina a **50 VUs simultáneos** en 15 segundos sosteniendo el pico durante 1 minuto, seguido de una caída abrupta a la normalidad.

### Parámetros de Ejecución Recomendados
```bash
K6_WEB_DASHBOARD=true \
K6_WEB_DASHBOARD_EXPORT=k6/reports/exp3_emergencia_spike.html \
k6 run k6/experimento-3-emergencia.js
```

---

## 3. Métricas a Correlacionar en Grafana y Prometheus

Durante la ejecución del test, se debe observar el comportamiento coordinado de la aplicación en los dashboards de Grafana:

1. **HikariCP (Pool de Conexiones a PostgreSQL):**
   * `hikaricp_connections_active`: Comprobar si se satura el pool (por defecto 10 conexiones).
   * `hikaricp_connections_pending`: Detectar si las transacciones de escritura retienen conexiones esperando confirmación a disco mientras las lecturas esperan turno.
   * `hikaricp_connections_timeout_total`: Indicaría denegación de servicio interna si supera 0.
2. **Tomcat (Hilos HTTP):**
   * `tomcat_threads_busy_threads`: Medir el consumo del pool de hilos de atención HTTP.
3. **Métricas k6:**
   * Comparativa de latencia `read_map_duration` vs `write_incident_duration` (p50, p95, p99).
   * Tasa de fallos `read_map_failures` y `write_incident_failures`.
   * Comprobar si la latencia del mapa se degrada por el aumento de tamaño de la tabla o por la contención de transacciones concurrentes.

---

## 4. Registro de Resultados y Análisis

*(Rellenar con los resultados de la ejecución real)*

| Métrica | Fase Base (Lecturas) | Durante el Pico (Spike Mixto) | Fase Recuperación |
| :--- | :---: | :---: | :---: |
| **Throughput Total (req/s)** | | | |
| **Latencia Mapa p50 / p95** | | | |
| **Latencia Reporte p50 / p95** | | | |
| **Conexiones HikariCP Activas** | | | |
| **Tasa de Errores (%)** | | | |

### Conclusiones Arquitectónicas
*(Espacio para documentar el impacto en PostgreSQL, HikariCP y la recuperación del servicio)*
