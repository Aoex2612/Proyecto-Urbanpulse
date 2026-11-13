# Pruebas de Carga y Rendimiento con Grafana k6

Este directorio contiene los scripts y utilidades para la caracterización y pruebas de rendimiento del backend de **UrbanPulse**, alineadas con la práctica de observabilidad y atributos de calidad.

---

## 1. Instalación de k6

### En Linux (CachyOS / Arch Linux):
```bash
sudo pacman -S k6
```

### En Ubuntu / Debian:
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

### Mediante Docker (sin instalar en host):
```bash
docker run --rm -i --net="host" -v $(pwd):/work -w /work grafana/k6 run k6/baseline.js
```

---

## 2. Experimento 1: Medición de la Baseline (`baseline.js`)

El script [k6/baseline.js](file:///home/alejandro/Documentos/ETSII/4%20curso/Desarrollo%20Software%20en%20Plataformas%20en%20la%20Nube/Proyecto-Urbanpulse/k6/baseline.js) permite medir el rendimiento en condiciones estables (por defecto **10 usuarios virtuales durante 2 minutos**).

### Características del Script:
* **Autenticación automática (`setup`):** Inicia sesión con `admin@urbanpulse.es` / `Admin1234` contra `/api/v1/auth/login` y obtiene el JWT Bearer para las peticiones que requieren RBAC.
* **Selección de Endpoint:** Permite medir endpoints aislados a través de la variable de entorno `ENDPOINT`.
* **Umbrales (*thresholds*):** Exige una tasa de fallos inferior al 1% (`http_req_failed < 0.01`).

### Opciones de Endpoint soportadas:
1. `list_incidents`: `GET /api/v1/incidents` (listado activo para mapa).
2. `get_incident`: `GET /api/v1/incidents/1` (detalle puntual por ID).
3. `search_incidents`: `GET /api/v1/incidents/search?category=ALUMBRADO` (filtrado y búsqueda).
4. `analytics_summary`: `GET /api/v1/analytics/summary` (cálculos agregados).
5. `create_incident`: `POST /api/v1/incidents` (escritura con coordenadas y títulos dinámicos con `__VU` e `__ITER`).

---

## 3. Formas de Ejecución

### Opción A: Ejecutar un endpoint específico con dashboard web en tiempo real
```bash
K6_WEB_DASHBOARD=true \
K6_WEB_DASHBOARD_EXPORT=k6/reports/baseline_list.html \
ENDPOINT=list_incidents \
k6 run k6/baseline.js
```
* Accede al dashboard interactivo en vivo en: `http://localhost:5665`

### Opción B: Ejecutar la batería completa automáticamente
```bash
./k6/run-baseline.sh
```
O para un solo endpoint:
```bash
./k6/run-baseline.sh analytics_summary
```

### Parámetros configurables mediante variables de entorno:
* `BASE_URL`: Dirección base de la API (por defecto `http://localhost:8080`).
* `VUS`: Número de usuarios concurrentes (por defecto `10`).
* `DURATION`: Tiempo de prueba (por defecto `2m`).
* `USER_EMAIL` / `USER_PASSWORD`: Credenciales para obtener el JWT token.
