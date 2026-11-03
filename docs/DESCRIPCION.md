Universidad de Málaga. Eduardo Guzmán de los Riscos
Todos los derechos reservados. Uso exclusivamente docente para alumnado matriculado.
UrbanPulse: Plataforma cloud para
la gestión inteligente de incidencias
urbanas
Presentación
UrbanPulse se plantea como una plataforma cloud para la gestión inteligente de incidencias urbanas que
integra la información reportada por la ciudadanía con datos contextuales de la ciudad para mejorar su
clasificación, priorización, seguimiento, análisis y resolución.
El documento define el caso de estudio principal que evolucionará a lo largo de todas las sesiones de
laboratorio. La incidencia será el centro del modelo del dominio y, adicionalmente, se plantea el uso de datos
externos de la ciudad de Málaga que no formarán un subsistema independiente, sino que enriquecerán la
incidencia, la vincularán con información urbana, aportarán el contexto operativo, etc.
Partiremos de un monolito sencillo con tecnologías como Spring Boot y PostgreSQL y solo se incorporarán
nuevos componentes cuando se introduzca un problema observable, una hipótesis comprobable y una decisión
arquitectónica que pueda documentarse y evaluarse.
1. Visión y propósito
Problema:
Las incidencias urbanas suelen llegar mediante canales heterogéneos, con descripciones incompletas y poca
información sobre su contexto. Un aviso, como un semáforo averiado o un árbol caído en la vía urbana, es más
útil cuando el sistema conoce el activo afectado, el distrito, la existencia de avisos similares, u otros factores
como el estado del tráfico, las condiciones meteorológicas o eventos próximos.
La idea es que UrbanPulse transforme un reporte aislado en una unidad de trabajo enriquecida con datos
contextuales, trazable y susceptible de análisis. La plataforma apoya al ciudadano, al operador municipal y al
responsable de planificación sin sustituir la decisión humana en actuaciones sensibles.
Universidad de Málaga. Eduardo Guzmán de los Riscos
Todos los derechos reservados. Uso exclusivamente docente para alumnado matriculado.
Visión del producto:
Para la ciudadanía, UrbanPulse permitirá comunicar una incidencia, aportar localización y evidencias y
seguir su evolución. Para el personal municipal, ofrecerá validación, asignación, priorización y contexto.
Para responsables y analistas, aportará indicadores, modelos predictivos y consultas basadas en datos y en
documentos.
Pautas de diseño:
§ La incidencia representará la entidad central del dominio y conservará un ciclo de vida auditable.
§ Los datos externos enriquecen; nunca bloquean la creación del reporte si una fuente no está disponible.
§ Se adopta la solución más sencilla que satisfaga los atributos de calidad medidos.
§ Cada cambio relevante actualiza el modelo C4, un ADR y las pruebas asociadas.
2. Alcance y actores
Actores principales:
Actor Responsabilidades Necesidades
Ciudadano Registra incidencias, aporta ubicación y evidencias,
consulta el estado.
Interfaz sencilla, confirmación, privacidad y
seguimiento.
Operador municipal Valida, rechaza, clasifica, prioriza y asigna. Contexto fiable, historial, búsqueda y trazabilidad.
Técnico Acepta trabajos, actualiza el progreso y documenta la
resolución. Cola de trabajo y seguimiento del progreso.
Administrador Gestiona usuarios, roles, categorías, departamentos y
fuentes.
Configuración segura, auditoría y control de
acceso.
Analista Explora indicadores, patrones espaciales y resultados de
modelos.
Datos consistentes, exportación, métricas y
explicabilidad.
Sistema externo Proporciona datos urbanos, meteorológicos, territoriales o
documentales.
Casos de uso principales:
§ Registrar una incidencia con texto, categoría opcional, coordenadas y fotografías o vídeos.
§ Detectar avisos potencialmente duplicados sin impedir que el ciudadano aporte información.
§ Determinar dirección, barrio y distrito a partir de la posición.
§ Asociar la incidencia al activo urbano más probable.
§ Obtener una visión de contexto urbano cercano al instante del reporte.
Universidad de Málaga. Eduardo Guzmán de los Riscos
Todos los derechos reservados. Uso exclusivamente docente para alumnado matriculado.
§ Validar o rechazar, calcular prioridad asistida y asignar departamento.
§ Actualizar el estado, añadir evidencias, notificar y mantener el historial completo.
§ Consultar mapa, listados paginados, filtros y estadísticas territoriales.
§ Entrenar y evaluar modelos de clasificación, priorización y detección de hotspots.
§ Consultar procedimientos con RAG y generar recomendaciones con citas y evidencia recuperada.
3. Requisitos funcionales
ID Requisito Definición
RF01 Identidad y acceso El sistema permitirá registro o acceso de usuarios y autenticación de personal autorizado.
RF02 Roles y permisos El sistema aplicará permisos diferenciados para ciudadanía, operadores, técnicos, administración y
analistas.
RF03 Registrar incidencia El ciudadano podrá registrar una incidencia con título, descripción, posición y categoría opcional.
RF04 Localización La incidencia conservará coordenadas, dirección normalizada y precisión de la localización.
RF05 Evidencias Se podrán adjuntar fotografías u otros ficheros con validación de tipo y tamaño.
RF06 Consulta y seguimiento El usuario podrá consultar detalle, estado, fechas e historial visible de sus incidencias.
RF07 Búsqueda El personal podrá buscar y filtrar por estado, categoría, fecha, prioridad, distrito y departamento.
RF08 Mapa El sistema mostrará incidencias y capas urbanas relevantes en un mapa.
RF09 Validación Un operador podrá validar, solicitar información o rechazar un reporte dejando motivo.
RF10 Prioridad El sistema permitirá establecer prioridad manual o asistida y conservar su justificación.
RF11 Asignación Una incidencia validada podrá asignarse a un departamento, equipo o técnico.
RF12 Ciclo de vida Los cambios de estado respetarán transiciones permitidas y quedarán auditados.
RF13 Colaboración Operadores y técnicos podrán añadir comentarios, evidencias y datos de resolución.
RF14 Notificaciones El sistema notificará cambios relevantes mediante canales configurables.
RF15 Estadísticas Se calcularán indicadores por categoría, territorio, estado y tiempo de resolución.
RF16 Duplicados El sistema podrá sugerir incidencias similares por proximidad, tiempo y semántica.
RF17 Análisis zonal Se podrán consultar incidencias y contexto agregado para una zona y periodo.
RF18 Auditoría Se registrarán acciones administrativas, decisiones asistidas y cambios relevantes.
Universidad de Málaga. Eduardo Guzmán de los Riscos
Todos los derechos reservados. Uso exclusivamente docente para alumnado matriculado.
ID Requisito Definición
RF19 Informes El sistema generará informes operativos y territoriales exportables.
RF20 Asistencia inteligente La plataforma podrá clasificar, priorizar, resumir y recomendar con indicación de confianza.
RF21 Barrio y distrito El sistema identificará barrio y distrito a partir de la localización.
RF22 Activo urbano El sistema podrá asociar una incidencia a un activo urbano próximo.
RF23 Contexto externo El sistema podrá obtener información contextual de fuentes externas.
RF24 Vista contextual El operador podrá visualizar el contexto urbano asociado.
RF25 Correlación histórica El sistema podrá correlacionar incidencias con variables urbanas históricas.
RF26 Degradación controlada La falta de disponibilidad de una fuente externa no impedirá crear la incidencia.
4. Modelo de dominio
Entidades principales:
Concepto Responsabilidad
Incident Reporte central con descripción, categoría, localización, prioridad, estado y marcas temporales.
User Ciudadano o profesional que crea, consulta o modifica según sus permisos.
UrbanAsset Elemento físico identificable como semáforo, contenedor, fuente, parada, aparcamiento o zona verde.
UrbanContext Información contextual de clima, tráfico, movilidad, eventos, ambiente, demografía y entorno
espacial.
Attachment Fotografía o documento almacenado fuera de la base relacional y referenciado por metadatos.
Assignment Relación temporal entre incidencia, departamento, equipo, técnico, etc.
StatusChange Transición auditable con actor, instante, motivo y datos asociados.
ExternalObservation Dato normalizado con fuente, instante de observación, instante de ingesta, unidad y calidad.
Notification Comunicación derivada de un evento y su estado de entrega.
KnowledgeDocument Procedimiento o normativa versionada que puede indexarse para RAG.
Universidad de Málaga. Eduardo Guzmán de los Riscos
Todos los derechos reservados. Uso exclusivamente docente para alumnado matriculado.
UrbanAsset
Conecta una incidencia con el elemento físico afectado. La asociación puede ser explícita, inferida por
proximidad o confirmada por un operador. Debe conservar id, tipo, fuente, geometría, metadatos, distancia a
la incidencia y nivel de confianza.
Ejemplo. Un aviso sobre un panel roto situado a siete metros de una parada EMT puede asociarse a
BUS_STOP 3948. Si existen varios candidatos próximos, el sistema presenta opciones en lugar de asumir una
correspondencia definitiva.
UrbanContext
Información contextual reproducible conocida para una incidencia o zona concreta. Cada observación deberá
conservar su procedencia, fecha de observación, cuándo fue registrada, cuándo caducó, estado, etc.
Ciclo de vida de la incidencia:
Estado Significado
REPORTED Incidencia recibida y pendiente de revisión.
VALIDATED Contenido suficiente y competencia municipal
confirmada.
REJECTED Fuera de alcance, inválida o duplicada con motivo
registrado.
ASSIGNED Asignada a departamento, equipo o técnico.
IN PROGRESS Existe una actuación en curso.
RESOLVED Actuación completada y documentada.
REOPENED La resolución no fue efectiva o aparece nueva
evidencia.
CLOSED Cierre definitivo después de verificación o plazo
establecido.
