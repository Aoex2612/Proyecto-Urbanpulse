# Experimento 2: Análisis de Escalabilidad y Saturación

Este documento recoge la metodología, resultados y análisis correspondientes al **Experimento 2** de la práctica de observabilidad y atributos de calidad de *Desarrollo Software en Plataformas en la Nube*.

---

## 1. Objetivos del Experimento

1. **Identificar los límites de capacidad:** Determinar hasta cuánta carga concurrente o tasa de peticiones puede sostener el backend de UrbanPulse antes de degradar su tiempo de respuesta o generar errores.
2. **Diferenciar conceptos arquitectónicos clave:**
   * **Concurrencia:** Número de usuarios virtuales que interactúan simultáneamente con el sistema.
   * **Capacidad (Throughput):** Volumen máximo de peticiones por segundo procesadas con éxito dentro de los límites de SLA.
   * **Escalabilidad:** Capacidad del sistema de mantener o aumentar el rendimiento proporcionalmente a la adición de recursos o carga.
3. **Analizar la métrica `dropped_iterations`:** Comprender y documentar qué ocurre cuando la tasa de llegada supera la capacidad de los VUs asignados.

---

## 2. Metodología de Prueba

### Parte A: Análisis con Carga Concurrente Creciente (`ramping-vus`)
Se incrementa la carga en etapas consecutivas:
* **10 VUs** (Línea base) $\rightarrow$ **25 VUs** $\rightarrow$ **50 VUs** $\rightarrow$ **100 VUs**.
* Se registra la evolución de `p50`, `p90`, `p95`, `p99`, `Throughput (req/s)` y `Tasa de errores (%)`.

### Parte B: Análisis de Tasa Fija (`constant-arrival-rate`)
Se somete a la API a tasas constantes de peticiones por segundo independientes del tiempo de respuesta del servidor (ej. 50, 100, 200 req/s) para detectar el punto de rotura donde aparecen `dropped_iterations`.

---

## 3. Registro de Resultados

### Parte A: Ramping VUs (10 $\rightarrow$ 25 $\rightarrow$ 50 $\rightarrow$ 100 VUs)

*Endpoint evaluado:* `GET /api/v1/incidents/1` (`get_incident`)

| Nivel de Concurrencia | p50 (Mediana) | p90 | p95 | p99 / Max | Peticiones/s | % Errores | Observaciones / Cuello de botella |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **10 VUs** (Baseline) | 8,49 ms | 17,42 ms | 23,74 ms | 626 ms | 9,83 req/s | 0,00 % | Comportamiento nominal sin contención |
| **25 VUs** | *Pendiente* | - | - | - | - | - | - |
| **50 VUs** | *Pendiente* | - | - | - | - | - | - |
| **100 VUs** | *Pendiente* | - | - | - | - | - | - |

---

### Parte B: Tasa de Llegada Fija (`constant-arrival-rate`)

| Tasa Solicitada (rate) | VUs Máximos | Throughput Real | p95 | Dropped Iterations | Estado del SLA |
| :---: | :---: | :---: | :---: | :---: | :--- |
| **50 req/s** | 100 | *Pendiente* | - | - | - |
| **100 req/s** | 100 | *Pendiente* | - | - | - |
| **200 req/s** | 100 | *Pendiente* | - | - | - |

---

## 4. Análisis y Respuestas a las Preguntas de la Práctica

### 1. ¿En qué punto comienza a degradarse la API y cuándo incumple sus requisitos?
*(Se completará con los resultados empíricos)*

### 2. ¿Qué indica exactamente la métrica `dropped_iterations`?
`dropped_iterations` mide el número de iteraciones programadas por el generador de carga que **no pudieron iniciarse** porque todos los `maxVUs` asignados estaban ocupados esperando la respuesta de peticiones anteriores. Es el síntoma inequívoco de que la tasa de llegada supera la capacidad de absorción del sistema: el sistema entra en colapso por saturación.
