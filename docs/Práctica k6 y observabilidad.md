# Práctica k6 y observabilidad

> Conversión de PDF a Markdown. El texto y la disposición se han extraído; el contenido gráfico puede no estar incluido.

```text
Práctica sobre atributos de calidad y
           observabilidad


Núm. Experimento
   1 Caracterización del rendimiento de la API
         § ¿Qué operaciones son más lentas?
         § Medimos la latencia y el throughput de diferentes endpoints con una carga
             pequeña y estable.
     Objetivo: establecer una línea base
     Condiciones propuestas: por ejemplo, 10 usuarios virtuales, dos minutos de
     duración por operación, base de datos precargada y misma configuración de
     infraestructura.
   2 Análisis de escalabilidad y saturación
         § Incrementar la carga progresivamente hasta detectar cuándo el sistema deja
             de cumplir sus objetivos de rendimiento.
         § Utilidad: comprender la diferencia entre concurrencia, capacidad y
             escalabilidad.
     Objetivos:
         - identificar límites de capacidad
         - estudiar la degradación progresiva del servicio al aumentar la carga.
   3 Simulación de un pico de incidencias urbanas
         § Simular un evento extraordinario en Málaga que provoque un aumento
             brusco de nuevas incidencias.
     Objetivo: evaluar tolerancia a picos de tráfico
   4 Comparación de operaciones de lectura y escritura
         § Determinar cómo afectan las operaciones concurrentes de creación,
             modificación y consulta al comportamiento de PostgreSQL.
     Objetivo: identificar operaciones costosas




                        Universidad de Málaga. Eduardo Guzmán de los Riscos
           Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 2

```text
Experimento 1. ¿Qué operaciones son
más lentas?

            Operación/endpoint            p50    p95     p99      Peticiones/s      Errores




 §   Preguntas que deberéis responder:

     1.   ¿Qué endpoint presenta mayor latencia?
     2.   ¿La operación más lenta es también la que presenta menor throughput?
     3.   ¿Hay diferencias importantes entre p50 y p99?
     4.   ¿Puede identificarse qué operaciones merecen una optimización prioritaria?


Experimento 2. ¿Cuál es la capacidad del
sistema?
 §   Podéis utilizar inicialmente 10, 25, 50 y 100 usuarios virtuales, ajustando esos valores
     al hardware disponible.
 §   A partir de los resultados reales, debéis determinar el punto donde la API comienza a
     degradarse y cuándo incumple sus requisitos.
 §   Para estimar de forma más controlada la capacidad en peticiones por segundo, añadid
     un segundo experimento con el ejecutor constant-arrival-rate, que mantiene
     una tasa de inicio de iteraciones independiente del tiempo de respuesta, siempre que
     haya usuarios virtuales suficientes.
 §   Explorad también el ejecutor ramping-arrival-rate.




                          Universidad de Málaga. Eduardo Guzmán de los Riscos
             Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 3

```text
Experimento 3. ¿Qué ocurre durante una
emergencia urbana?
 §   Escenario de ejemplo: durante un episodio de lluvias intensas en Málaga, aumenta
     repentinamente el número de ciudadanos que notifican alcantarillas obstruidas,
     inundaciones y problemas de tráfico.
 §   Podéis estudiar si durante el pico aparecen errores, cuánto aumenta la latencia y si el
     servicio recupera su comportamiento normal al desaparecer la sobrecarga.
 §   Pregunta arquitectónica:

       ¿Qué ocurre con la creación de incidencias si al mismo tiempo hay muchos
                     usuarios consultando el mapa de incidencias?

        -   Esto permite estudiar la competencia por conexiones, CPU y acceso a
            PostgreSQL.


Experimento 4. ¿Cómo influye el volumen
de datos?
 §   Debéis repetirían las mismas consultas con diferentes tamaños de base de datos. Por
     ejemplo, con 1.000, 10.000, 100.000 y 1.000.000 de incidencias.
        - Los tamaños son orientativos y deben adaptarse a los recursos disponibles.
 §   Deberéis estudiar cómo evoluciona el rendimiento de consultas filtradas, paginación,
     ordenación por fecha y estadísticas agregadas
 §   Sugerencias de mejora: añadir un índice sobre los campos habitualmente filtrados y
     repetir el experimento

         ¿Mejorar el esquema de PostgreSQL permite aumentar la capacidad sin
                  modificar el número de instancias de Spring Boot?

 §   Este experimento conecta directamente la calidad arquitectónica con el diseño y
     almacenamiento de datos.




                         Universidad de Málaga. Eduardo Guzmán de los Riscos
            Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 4

```text
¿Cómo realizar las pruebas?
 1. Ejecutad las pruebas sobre datos representativos y con el entorno previamente
    estabilizado.
 2. Repetid cada configuración al menos tres veces.
 3. Representad la variabilidad, no solamente un resultado aislado.
 4. Analizad la métrica dropped_iterations en los experimentos y explicad qué indica.




                       Universidad de Málaga. Eduardo Guzmán de los Riscos
          Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```
