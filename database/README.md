# Inicialización de Base de Datos — UrbanPulse

Este directorio contiene la configuración y los scripts necesarios para desplegar y poblar la base de datos PostgreSQL del proyecto **UrbanPulse** mediante contenedores con Docker Compose.

---

## Requisitos Previos

* [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución.
* Docker Compose v2+ (`docker compose version`).

---

## Parámetros de Conexión

| Parámetro | Valor local |
| :--- | :--- |
| **Motor** | PostgreSQL 16 (Alpine) |
| **Host** | `localhost` |
| **Puerto** | `5432` |
| **Base de Datos** | `urban_pulse_db` |
| **Usuario** | `admin` |
| **Contraseña** | `database` |
| **URL JDBC** | `jdbc:postgresql://localhost:5432/urban_pulse_db` |

---

## Estructura del Directorio

```text
Proyecto-Urbanpulse/
├── database/
│   ├── init/
│   │   ├── 01-schema.sql       # Creación de tablas, restricciones e índices
│   │   └── 02-seed-data.sql    # Datos iniciales de prueba (opcional)
│   └── README.md
└── docker-compose.yml          # Orquestación del servicio PostgreSQL