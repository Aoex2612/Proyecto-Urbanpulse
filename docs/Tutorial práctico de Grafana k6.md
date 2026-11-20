# Tutorial práctico de Grafana k6

> Conversión de PDF a Markdown. El texto y la disposición se han extraído; el contenido gráfico puede no estar incluido.

```text
        Tutorial práctico de Grafana k6

El problema que queremos resolver ....................................................................................... 2
¿Qué es k6 y cómo funciona? ................................................................................................ 3
Instalación y primera prueba ................................................................................................. 3
Cómo interpretamos las métricas .......................................................................................... 4
Cómo simular concurrencia y diferentes cargas ..................................................................... 5
Un ejemplo con un POST ..................................................................................................... 6
Cómo utilizar datos diferentes ............................................................................................... 7
Cómo establecer requisitos de calidad con thresholds ........................................................... 8
Medir una tasa ﬁja de operaciones por segundo .................................................................... 9
Ejemplos con k6 en función del tipo de prueba ................................................................... 11
Cómo visualizar los resultados con Grafana k6 ................................................................... 12
Grafana con Prometheus ..................................................................................................... 13
Micrometer .......................................................................................................................... 13
¿Qué información adicional aporta Micrometer? ................................................................. 14
Ejemplo de diagnóstico entre k6 y Micrometer ................................................................... 14
Cómo incorporar Micrometer a Spring Boot ....................................................................... 15
Referencias........................................................................................................................... 17




                             Universidad de Málaga. Eduardo Guzmán de los Riscos
                Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 2

```text
El problema que queremos resolver
  §      En una API REST, con herramientas como Postman se puede verificar que petición
         como la siguiente funcionan:

                             GET http://localhost:8080/api/incidents


            - La respuesta llega en 80 ms. Parece un buen resultado.
  §      Hay preguntas que Postman, ejecutando una única petición, no puede responder
         adecuadamente:
            - ¿Qué sucede cuando acceden 50 usuarios simultáneamente?
            - Y, cuándo acceden 500 …
                  o ¿Aumenta progresivamente el tiempo de respuesta?
                  o ¿Cuándo comienzan a producirse errores?
                  o ¿Qué ocurre si la base de datos empieza a saturarse?


  §      Tipos de pruebas:

                                                                          Duración            Atributo      de
Tipo      Objetivo                              Carga aplicada
                                                                          habitual            calidad
          Comprobar que la aplicación
Smoke                                           Muy baja                  Segundos            Fiabilidad
          responde correctamente
          Evaluar el comportamiento bajo la
Load                                            Normal o prevista         Minutos             Rendimiento
          carga esperada
                                                Creciente, por encima                         Rendimiento,
Stress    Encontrar los límites del sistema                               Minutos
                                                de lo esperado                                fiabilidad
          Evaluar la respuesta ante aumentos
Spike                                           Picos repentinos          Minutos             Resiliencia
          bruscos


  §      ¿Cuál utilizar en cada momento del desarrollo?

            -   Smoke: después de un despliegue o antes de una batería de pruebas.
            -   Load: para verificar que el sistema cumple los requisitos no funcionales bajo las
                condiciones esperadas.
            -   Stress: antes de producción o al evaluar cambios arquitectónicos importantes,
                para conocer los límites y el comportamiento ante sobrecarga.
            -   Spike: cuando esperamos ráfagas repentinas de tráfico o necesitamos evaluar la
                resiliencia frente a ellas.




                             Universidad de Málaga. Eduardo Guzmán de los Riscos
                Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 3

```text
         -   Soak: que consiste en mantener una carga durante un periodo prolongado para
             detectar problemas de memoria, agotamiento progresivo de conexiones o
             degradaciones que no aparecen en pruebas cortas.


¿Qué es k6 y cómo funciona?
  §   k6 permite repetir peticiones y recorridos de usuario bajo distintos patrones de carga
      para observar el comportamiento de la aplicación.
  §   https://grafana.com/docs/k6/latest/
  §   Los usuarios virtuales ejecutan scripts que generan peticiones contra una aplicación
      desplegada.
  §   Cada usuario virtual (Virtual User o VU) ejecuta las operaciones definidas en un script
      JavaScript.
         - no es necesariamente un usuario humano conectado mediante navegador;
         - es una unidad de ejecución de k6 que simula el comportamiento programado;
         - puede ejecutar varias peticiones en cada iteración.

         Concepto                    Significado
         Usuario virtual (VU)        ejecutor de un recorrido
         Iteración                   ejecución completa de la función principal del script




Instalación y primera prueba
  §   Cómo instalar k6: https://grafana.com/docs/k6/latest/set-up/install-k6/
  §   Primer ejemplo de script: 01-primer-test.js


               import http from 'k6/http';

               export default function () {
                 http.get('https://test.k6.io/');
               }




import http                          permite realizar peticiones HTTP
export default function()            define las operaciones que ejecutará cada usuario virtual




                          Universidad de Málaga. Eduardo Guzmán de los Riscos
             Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 4

```text
 §   Cómo ejecutar la prueba: k6 run 01-primer-test.js
       - Se ejecutará una prueba con un usuario virtual y una iteración, si no se especifica
         otra configuración.
       - http://test.k6.io es una web de demostración proporcionada por Grafana.



Cómo interpretamos las métricas
 §   Métricas principales de k6:

         Métrica                      Significado
         http_req_duration            Duración HTTP, incluyendo envío, espera y recepción
         http_req_failed              Tasa de peticiones consideradas fallidas
         http_reqs                    Número y tasa de peticiones HTTP
         iteration_duration           Duración de cada iteración completa
         iterations                   Número de iteraciones
         vus                          Usuarios virtuales activos
         vus_max                      Máximo de VUs inicializados
         checks                       Proporción de comprobaciones superadas


 §   Otro ejemplo de script:


             import http from 'k6/http';
             import { sleep, check } from 'k6';

             export const options = {
               vus: 5,
               duration: '30s',
             };

             export default function () {
               const res = http.get('https://test.k6.io/');

                 check(res, {
                   'La respuesta es 200': (r) => r.status === 200,
                 });

                 sleep(1);
             }




         vus: 5                   establece cinco usuarios virtuales
         duration: '30s'          mantiene el experimento durante 30 segundos
         check()                  define una comprobación del resultado
         sleep(1)                 introduce una pausa de un segundo por iteración,
                                  simulando un pequeño tiempo de espera


                        Universidad de Málaga. Eduardo Guzmán de los Riscos
           Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 5

```text
Cómo simular concurrencia y diferentes
cargas
 §   Ejemplo de script de pruebas de carga:



                   import http from 'k6/http';
                   import { sleep } from 'k6';

                   const BASE_URL =
                     __ENV.BASE_URL || 'http://localhost:8080';

                   export const options = {
                     vus: 20,
                     duration: '1m',
                   };

                   export default function () {
                     http.get(`${BASE_URL}/api/incidents`);

                       sleep(1);
                   }



       -   Tenemos 20 usuarios virtuales durante 1 min, realizando repetidamente la misma
           petición.
       -   Sugerencia: ¿Qué ocurre si aumentamos la concurrencia?

                        Experimento VUs Duración
                        A             1 1 minuto
                        B            10 1 minuto
                        C            20 1 minuto
                        D            50 1 minuto

       1. Para cada experimento registra p95, throughput y errores.
       2. Compara los resultados: cómo evoluciona esa degradación y si sigue cumpliendo
          los requisitos.




                        Universidad de Málaga. Eduardo Guzmán de los Riscos
           Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 6

```text
 §   Configurar cargas crecientes con ramping-vus:
     - En lugar de lanzar cuatro experimentos separados, podemos incrementar
       progresivamente la carga dentro de una misma ejecución:


                     export const options = {
                       scenarios: {
                         usuarios_crecientes: {
                           executor: 'ramping-vus',
                           startVUs: 0,
                           stages: [
                             { duration: '30s', target: 10 },
                             { duration: '1m', target: 10 },
                             { duration: '30s', target: 30 },
                             { duration: '1m', target: 30 },
                             { duration: '30s', target: 50 },
                             { duration: '1m', target: 50 },
                             { duration: '30s', target: 0 },
                           ],
                         },
                       },
                     };



     -   El bloque sustituye los options del script anterior.
     -   { duration: '30s', target: 10 }: Significa que k6 debe aumentar o
         disminuir progresivamente el número de VUs hasta llegar a 10 VUs en 30 segundos.
     -   Esto permite observar la aplicación durante periodos de carga estable y durante los
         cambios de intensidad.


Un ejemplo con un POST
 §   Ejemplo:


                import http from 'k6/http';
                import { check } from 'k6';

                const BASE_URL =
                  __ENV.BASE_URL || 'http://localhost:8080';

                export const options = {
                  vus: 1,
                  iterations: 1,
                };

                export default function () {
                  const payload = JSON.stringify({
                    title: 'Farola averiada',
                    description: 'No funciona la iluminación',
                    category: 'LIGHTING',
                  });



                        Universidad de Málaga. Eduardo Guzmán de los Riscos
           Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 7

```text
                     const params = {
                       headers: {
                         'Content-Type': 'application/json',
                       },
                     };

                     const res = http.post(
                       `${BASE_URL}/api/incidents`,
                       payload,
                       params
                     );

                     check(res, {
                       'Incidencia creada': (r) => r.status === 201,
                     });
                 }



        -   La API acepta esos campos y responde con 201 Created.


Cómo utilizar datos diferentes
 §   Si cien usuarios virtuales intentan crear exactamente la misma incidencia, es posible que
     la aplicación rechace algunas peticiones por reglas de negocio.
 §   Podemos introducir datos distintos:

                 const payload = JSON.stringify({
                   title: `Incidencia VU-${__VU}-${__ITER}`,
                   description: 'Incidencia de prueba',
                   category: 'LIGHTING',
                 });


        §   __VU identifica el usuario virtual y
        §   __ITER la iteración de ese VU.




                         Universidad de Málaga. Eduardo Guzmán de los Riscos
            Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 8

```text
Cómo establecer requisitos de calidad con
thresholds
 §   ¿Cómo decidimos si el rendimiento es aceptable? à definiendo criterios de aceptación
     antes de ejecutar las pruebas.
 §   Consideremos el siguiente requisito funcional:

       La consulta de incidencias debe responder en menos de 500 ms
       para el 95 % de las peticiones, con una tasa de errores inferior al
       1 %, bajo una carga de 50 usuarios virtuales.

        -   Este requisito contiene cuatro elementos:

                    Elemento              Definición
                    Operación             Consultar incidencias
                    Condición             50 usuarios virtuales
                    Métrica de tiempo     Latencia p95
                    Criterios             p95 < 500 ms y errores < 1 %


 §   Los thresholds permiten transformar los criterios de calidad en condiciones de éxito o
     fracaso de la prueba.
 §   Ejemplo de script con criterios de aceptación:


                   import http from 'k6/http';
                   import { check, sleep } from 'k6';

                   const BASE_URL =
                     __ENV.BASE_URL || 'http://localhost:8080';

                   export const options = {
                     vus: 50,
                     duration: '2m',

                     thresholds: {
                       http_req_duration: ['p(95)<500'],
                       http_req_failed: ['rate<0.01'],
                       checks: ['rate>0.99'],
                     },
                   };

                   export default function () {
                     const res = http.get(
                       `${BASE_URL}/api/incidents`
                     );

                      check(res, {
                        'HTTP 200': (r) => r.status === 200,


                         Universidad de Málaga. Eduardo Guzmán de los Riscos
            Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 9

```text
                       });

                       sleep(1);
                   }



        -  Se definen tres criterios:
           • http_req_duration: controla la latencia.
           • http_req_failed: controla los fallos HTTP.
           • checks: controla las comprobaciones que hemos definido explícitamente.
 §   Un threshold incumplido provoca que k6 finalice con un código de salida de error.


Medir una tasa fija de operaciones por
segundo
 §   Existe una diferencia importante entre generar carga con una cantidad fija de usuarios
     y generar carga con una tasa fija de operaciones.
 §   Si queremos responder a la pregunta: ¿cómo podemos comprobar si una API es capaz de
     procesar 100 consultas por segundo?
 §   Ejemplo: k6 intentará iniciar 100 iteraciones por segundo durante dos minutos


                import http from 'k6/http';
                import { check } from 'k6';

                const BASE_URL =
                  __ENV.BASE_URL || 'http://localhost:8080';

                export const options = {
                  scenarios: {
                    consultas: {
                      executor: 'constant-arrival-rate',
                      rate: 100,
                      timeUnit: '1s',
                      duration: '2m',
                      preAllocatedVUs: 30,
                      maxVUs: 100,
                    },
                  },
                  thresholds: {
                    http_req_duration: ['p(95)<500'],
                    http_req_failed: ['rate<0.01'],
                    dropped_iterations: ['count==0'],
                  },
                };

                export default function () {
                  const res = http.get(
                    `${BASE_URL}/api/incidents`
                  );


                         Universidad de Málaga. Eduardo Guzmán de los Riscos
            Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 10

```text
                    check(res, {
                      'Consulta correcta': (r) => r.status === 200,
                    });
                }




Parámetro                                      Explicación
scenarios                                      Deﬁne los distintos escenarios de carga que ejecutará k6.
                                               Nombre del escenario. Puedes deﬁnir varios escenarios con
consultas
                                               nombres diferentes.
                                               Intenta iniciar iteraciones a una tasa constante,
executor: 'constant-arrival-rate'
                                               independientemente de cuánto tarden en completarse.
                                               Número de iteraciones que k6 intentará iniciar por cada
rate: 100
                                               unidad de tiempo.
                                               Unidad de tiempo para rate. En este caso, 100 iteraciones
timeUnit: '1s'
                                               por segundo.
duration: '2m'                                 Duración del escenario: 2 minutos.
                                               Número de usuarios virtuales que k6 prepara inicialmente
preAllocatedVUs: 30
                                               para ejecutar iteraciones.
                                               Número máximo de VUs que k6 puede utilizar si los 30
maxVUs: 100
                                               iniciales no son suﬁcientes.


    -   ¿Qué hace exactamente esta configuración? Durante 2 minutos, k6 intentará iniciar
        100 iteraciones por segundo, es decir, unas 12.000 iteraciones en total.
        o Comienza con 30 VUs disponibles.
        o Si las iteraciones tardan demasiado y los 30 VUs están ocupados, puede incorporar
            más VUs hasta llegar a 100.
        o Si alcanza los 100 VUs y siguen sin ser suﬁcientes, algunas iteraciones no podrán
            iniciarse y se contabilizarán como dropped_iterations.
    -   Importante:
        o 100 iteraciones por segundo no signiﬁcan necesariamente 100 peticiones HTTP
            por segundo. Una iteración puede contener varias peticiones.
        o maxVUs establece un límite, pero no garantiza que k6 consiga mantener la tasa
            solicitada.
    -   preAllocatedVUs y maxVUs determinan cuántos usuarios virtuales puede utilizar
        k6 para generar el ritmo solicitado.
    -   dropped_iterations: Iteraciones que se podrían ejecutar pero que no lo hacen por
        falta de VUs
    -   https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/arrival-rate-vu-
        allocation/




                         Universidad de Málaga. Eduardo Guzmán de los Riscos
            Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 11

```text
Ejemplos con k6 en función del tipo de
prueba
 §   Lo que cambia es el bloque options:
 §   Smoke testing: ¿Funciona el sistema?
        - Ejecutamos una carga mínima para comprobar que el endpoint responde y que
           podemos recoger resultados.

            export const options = {
              vus: 1,
              duration: '10s'
            };


       - Es habitual ejecutarlo antes de pruebas más exigentes.
 §   Load testing: ¿Soporta la carga prevista?
       - Simulamos una carga normal de 50 usuarios virtuales.

            export const options = {
              vus: 50,
              duration: '3m',
              thresholds: {
                http_req_duration: ['p(95)<500'],
                http_req_failed: ['rate<0.01']
              }
            };

        - Es una de las pruebas más importantes para verificar requisitos de rendimiento.
 §   Stress testing: ¿Dónde está el límite?
        - Incrementamos progresivamente la carga hasta superar el funcionamiento
           esperado.
            export const options = {
              stages: [
                { duration: '1m', target: 50 },
                { duration: '1m', target: 100 },
                { duration: '1m', target: 200 },
                { duration: '1m', target: 300 },
                { duration: '1m', target: 0 }
              ]
            };

        -   Buscamos el punto en el que la latencia aumenta excesivamente, aparecen errores
            o el throughput deja de crecer.




                         Universidad de Málaga. Eduardo Guzmán de los Riscos
            Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 12

```text
 §   Spike testing: ¿Cómo responde a un pico inesperado?
        - Aumentamos la carga de manera brusca y después la reducimos.
            export const options = {
              stages: [
                { duration: '30s', target: 10 },
                { duration: '5s', target: 200 },
                { duration: '30s', target: 200 },
                { duration: '5s', target: 10 },
                { duration: '30s', target: 10 }
              ]
            };

        -   Queremos comprobar si la aplicación soporta el pico y si se recupera cuando
            desaparece.


Cómo visualizar                                   los          resultados                     con
Grafana k6
 §   k6 también proporciona un dashboard web integrado.
 §   Activar el dashboard de k6:
        - Ejecutamos:

            K6_WEB_DASHBOARD=true \
            k6 run 04-thresholds.js

        - En el navegador escribimos: http://localhost:5665
        - Muestra una interfaz para observar el comportamiento de la prueba en tiempo
          real.
 §   Generar un informe HTML:
       - Podemos guardar un informe al terminar la prueba:

                 K6_WEB_DASHBOARD=true \
                 K6_WEB_DASHBOARD_EXPORT=report.html \
                 k6 run 04-thresholds.js


        -   El fichero report.html contiene los resultados de la ejecución y puede abrirse
            con un navegador.




                         Universidad de Málaga. Eduardo Guzmán de los Riscos
            Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 13

```text
Grafana con Prometheus
 §   Nos permite incorporar una infraestructura de observabilidad.
 §   La idea es recoger dos tipos de información:

               Desde k6:

                   •   Peticiones por segundo.
                   •   Latencia.
                   •   Errores.
                   •   Usuarios virtuales.

               Desde la aplicación y la infraestructura:

                   •   CPU y memoria.
                   •   Hilos activos.
                   •   Conexiones de base de datos.
                   •   Métricas JVM.
                   •   Consultas y operaciones del backend.


 §   En Spring Boot, se puede instrumentar la aplicación mediante Actuator y
     Micrometer, exponer las métricas a Prometheus y visualizarlas en Grafana.
 §   Para almacenar las métricas de k6 en Prometheus, se puede utilizar la salida compatible
     con Prometheus Remote Write; requiere configurar correctamente el receptor
     correspondiente.
         - Más info.: https://grafana.com/docs/k6/latest/results-output/real-
            time/prometheus-remote-write/




Micrometer
 §   https://micrometer.io/
 §   Es una biblioteca de instrumentación y observabilidad para aplicaciones Java,
     especialmente utilizada con Spring Boot,
 §   Permite recopilar métricas sobre el funcionamiento interno de una aplicación.
 §   Relación con Grafana k6: son herramientas complementarias que permiten descubrir
     problemas de rendimiento:
        - k6 mide cómo responde una aplicación ante la carga, desde la perspectiva del
            cliente.
        - Micrometer mide qué está ocurriendo dentro de la aplicación mientras procesa
            esa carga.


                        Universidad de Málaga. Eduardo Guzmán de los Riscos
           Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 14

```text
¿Qué información                                        adicional                       aporta
Micrometer?
 §   Proporciona varios tipos de instrumentos de medición.
 §   Los tres fundamentales son:
        - Counter: cuenta cuántas veces ocurre algo (incidencias creadas, errores de
            negocio, etc.).
        - Gauge: mide un valor que cambia con el tiempo (conexiones activas, tareas
            pendientes).
        - Timer: mide la duración y frecuencia de operaciones (consultas, validaciones,
            procesamiento).
 §   Ejemplo: en una aplicación Spring Boot puede recopilar automáticamente métricas de la
     JVM, CPU, memoria y otros componentes
 §   Puede utilizarse para métricas de distribución y observabilidad mediante Micrometer
     Observation y Micrometer Tracing.
 §   Alternativas en otros lenguajes de programación:

           Node.js/Express          prom-client, OpenTelemetry JS
           Python / FastAPI         prometheus-client, OpenTelemetry Python
           C# / ASP.NET Core        System.Diagnostics.Metrics




Ejemplo de                         diagnóstico                        entre                  k6   y
Micrometer
 §   Supongamos que ejecutamos una prueba de estrés sobre UrbanPulse y obtenemos los
     siguientes resultados:

                                   Métrica                         Inicio     Bajo carga
                              latencia p95                          120 ms       1.200 ms
              K6
                              peticiones fallidas                       0%            4%
                              uso de CPU                              25 %           42 %
              Micrometer      conexiones PostgreSQL activas            3/10         10/10
                              conexiones pendientes                       0            18




                        Universidad de Málaga. Eduardo Guzmán de los Riscos
           Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 15

```text
 §   ¿Qué podemos deducir?
        - k6 indica que la aplicación se ha ralentizado y está fallando.
        - Micrometer revela que la CPU no está especialmente saturada, pero el pool de
           conexiones está completamente ocupado y hay solicitudes esperando.
        - Hipótesis: el cuello de botella podría encontrarse en el acceso a PostgreSQL, por
           ejemplo, por consultas lentas o conexiones retenidas demasiado tiempo.


Cómo incorporar Micrometer a Spring
Boot
 §   Paso 1. Añadir dependencias en pom.xml:

            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-actuator</artifactId>
            </dependency>

            <dependency>
                <groupId>io.micrometer</groupId>
                <artifactId>micrometer-registry-prometheus</artifactId>
            </dependency>


 §   Paso 2. Configurar la exposición de métrica en application.properties:

     management.endpoints.web.exposure.include=health,info,metrics,prometheus
     management.endpoint.health.show-details=never


 §   Paso 3. Consultar las métricas con Prometheus: iniciamos Spring Boot y
     accedemos a:

     http://localhost:8080/actuator/prometheus


        -   Encontraremos series temporales como:

                 jvm_memory_used_bytes
                 process_cpu_usage
                 hikaricp_connections_active
                 hikaricp_connections_pending


        -   Las métricas disponibles dependen de los componentes y de la configuración de
            la aplicación.




                         Universidad de Málaga. Eduardo Guzmán de los Riscos
            Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 16

```text
§   Paso 4. Crear métricas propias
      - También podemos medir operaciones específicas de la aplicación.
      - Ejemplo: contar las incidencias creadas

           @Service
           public class IncidentService {

                private final Counter incidentsCreated;

                public IncidentService(MeterRegistry registry) {
                    this.incidentsCreated = Counter.builder("incidents.created")
                        .description("Number of incidents created")
                        .register(registry);
                }

                public void createIncident() {
                    // Validar y guardar la incidencia
                    // ...

                      // Incrementar solo después de crearla correctamente
                      incidentsCreated.increment();
                }
           }


       -   Micrometer no se limita a medir recursos de infraestructura: también puede
           recoger métricas vinculadas al dominio de negocio.




                        Universidad de Málaga. Eduardo Guzmán de los Riscos
           Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```

## Página 17

```text
Referencias
 §   Get started with k6 - tutorial inicial:
     https://grafana.com/docs/k6/latest/examples/get-started-with-k6/.
        - Scenarios: configuración de diferentes patrones de carga.
            https://grafana.com/docs/k6/latest/using-k6/scenarios/
        - Thresholds: validación de requisitos de calidad.
            https://grafana.com/docs/k6/latest/using-k6/thresholds/
 §   Web dashboard - visualización e informes:
     https://grafana.com/docs/k6/latest/results-output/web-dashboard/
 §   Micrometer: https://docs.micrometer.io/micrometer/reference/concepts.html
 §   Quick guide to Micrometer: https://www.baeldung.com/micrometer
 §   Spring Boot - métricas con Actuator: https://docs.spring.io/spring-
     boot/reference/actuator/metrics.html
 §   Prometheus - introducción: https://prometheus.io/docs/introduction/overview/
 §   Grafana - documentación: https://grafana.com/docs/grafana/latest/
 §   OpenTelemetry – documentación: https://opentelemetry.io/docs/




                        Universidad de Málaga. Eduardo Guzmán de los Riscos
           Desarrollo Software en Plataformas en la Nube. Grado en Ingeniería del Software
```
