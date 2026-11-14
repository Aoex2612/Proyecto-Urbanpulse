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

| Métrica Global | Resultado (Prueba Completa 100 VUs) | Comparación con Baseline (10 VUs) |
| :--- | :---: | :---: |
| **Peticiones Totales** | **16.512 reqs** | 1.191 reqs (×13,8) |
| **Throughput Medio** | **42,19 req/s** | 9,83 req/s (×4,3) |
| **p50 (Mediana)** | **7,81 ms** | 8,49 ms (Excelente estabilidad) |
| **p90** | **8,33 ms** | 17,42 ms |
| **p95** | **8,66 ms** | 23,74 ms |
| **Max** | **526,92 ms** | 626,49 ms |
| **Tasa de Errores** | **0,00 %** (0 de 16.512) | 0,00 % |

#### Conclusiones de la Rampa de Concurrencia:
1. **Escalabilidad horizontal en lecturas puntuales:** Con hasta 100 usuarios virtuales simultáneos, el endpoint `get_incident` **no muestra ninguna señal de fatiga ni contención**. La p95 se mantiene en apenas **8,66 ms** (incluso mejor que en la baseline por optimizaciones en caliente de la JVM / JIT y cachés de buffer de PostgreSQL).
2. **Capacidad del pool de conexiones:** El pool por defecto de HikariCP (10 conexiones) y los hilos de Tomcat absorbieron perfectamente los 100 VUs concurrentes con `sleep(1)` gracias a que cada transacción de lectura toma menos de 8 ms, dejando la conexión libre de inmediato para el siguiente usuario virtual.
3. **El límite no está en 100 VUs para lecturas simples:** Para encontrar el verdadero límite de saturación y forzar la aparición de `dropped_iterations`, debemos someter al sistema a una tasa de llegada fija agresiva (**Parte B**) o aplicar la rampa sobre un endpoint más costoso como `search_incidents` o `create_incident`.

---

### Parte B: Tasa de Llegada Fija (`constant-arrival-rate`)

*Endpoint evaluado:* `GET /api/v1/incidents/1` (`get_incident`)

| Tasa Solicitada (rate) | VUs Máximos | Throughput Real | p50 | p95 | Dropped Iterations | % Errores | Estado del SLA / Diagnóstico |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **150 req/s** | 50 (usados: 20) | **149,58 req/s** | **5,58 ms** | **6,39 ms** | **0** | **0,00 %** | Óptimo. Solo requirió 1 VU activo concurrente debido a latencia ultra-baja (<6 ms). |
| **400 req/s** | 50 (usados: 20) | **398,82 req/s** | **5,58 ms** | **6,32 ms** | **0** | **0,00 %** | Rendimiento excepcional: 24.000 peticiones en 1 min con p95 = 6,32 ms y 0 fallos. |
| **search (20 req/s)** | 10 (tope estricto) | **8,69 req/s** | **1.115 ms** | **1.143 ms** | **672** (56% perdidas) | **0,00 %** | **Saturación total demostrada:** De 1.200 iteraciones programadas, se perdieron 672 por VUs ocupados. |

---

## 4. Análisis y Respuestas a las Preguntas de la Práctica

### 1. ¿En qué punto comienza a degradarse la API y cuándo incumple sus requisitos?
* **En lecturas simples indexadas (`get_incident`):** La API no se degrada ni siquiera a **400 req/s** ni con **100 VUs**. La latencia p95 se mantiene plana e inferior a 7 ms. Esto demuestra que cuando una consulta está indexada y la entidad cabe en buffer, la API escala prácticamente sin contención.
* **En consultas de búsqueda sin índices (`search_incidents`):** El límite de saturación se alcanza a partir de **~8,7 peticiones por segundo** con 10 VUs. Al pedirle una tasa de llegada de 20 req/s, la latencia se dispara de 780 ms (en reposo) a más de **1.140 ms**, y el sistema no es capaz de procesar más de 8,7 req/s, colapsando la capacidad de absorción.

### 2. ¿Qué indica exactamente la métrica `dropped_iterations`?
La métrica `dropped_iterations` (en este caso **672 iteraciones caídas**, un **56% del tráfico programado**) indica que k6 intentó iniciar 20 nuevas peticiones por segundo según el contrato de llegada, pero **los 10 VUs asignados estaban todos ocupados esperando a que PostgreSQL terminara de responder peticiones previas**.

#### Relación con la Ley de Little:
$$\text{VUs necesarios} = \text{Tasa de llegada (Arrival Rate)} \times \text{Tiempo de respuesta medio}$$
$$\text{VUs necesarios} = 20 \text{ req/s} \times 1{,}11 \text{ s} = \mathbf{22{,}2 \text{ VUs}}$$

Como impusimos un límite de `maxVUs: 10`, el sistema solo pudo absorber:
$$\text{Throughput máximo alcanzable} = \frac{10 \text{ VUs}}{1{,}11 \text{ s}} \approx \mathbf{9{,}0 \text{ req/s}}$$

El valor real medido fue de **8,69 req/s** y las restantes 11 peticiones por segundo no pudieron ni siquiera lanzarse, contabilizándose como `dropped_iterations`. Esto ilustra con rigor científico la diferencia entre:
* **Tasa de llegada demandada:** 20 req/s.
* **Capacidad real del sistema:** ~8,7 req/s.
* **Sobrecarga descartada:** 672 iteraciones (11,04 iteraciones perdidas por segundo).
