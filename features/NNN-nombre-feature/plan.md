# NNN — <Nombre de la feature> — Plan

_Cómo se implementa lo descrito en `spec.md`. Debe respetar la `constitution/`._

## Enfoque

<Estrategia general en pocas frases: qué aproximación se toma y por qué encaja con el stack y los principios del proyecto.>

## Implementación

_Pasos técnicos concretos, en orden. Indica los archivos/módulos que se tocan._

1. <Paso — archivo/módulo afectado.>
2. <Paso — archivo/módulo afectado.>
3. <Paso — archivo/módulo afectado.>

## Decisiones

_Elecciones de diseño relevantes y su justificación. Alternativas descartadas y por qué._

- **<Decisión>** — <por qué; qué se descartó>.
- **<Decisión>** — <por qué; qué se descartó>.

## Riesgos

_Qué puede salir mal o requerir cuidado, y cómo se mitiga._

- **<Riesgo>** — <mitigación>.

## Estrategia de tests
_Estrategia que se seguiran para los test_
- <Unitarios de core con fechas inyectadas (rachas: vacía, viva por ayer, rota, de 1 día, larga).>
- <storage con tmp_path de pytest (inexistente, válido, corrupto).>
- <Smoke test de CLI invocando el parser.>