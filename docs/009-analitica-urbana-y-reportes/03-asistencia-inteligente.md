# Tarea 3: Servicio de Asistencia Inteligente y Recomendación con Grado de Confianza

**Feature:** `009-analitica-urbana-y-reportes`  
**Estado:** Completada ✅  
**Requisitos funcionales relacionados:** RF-14, RF-15, RF-16, RF-17, RF-18  

---

## 1. Resumen de la Tarea

En esta tarea se ha diseñado e implementado el servicio central de recomendación asistida y analítica de desempeño del asistente (`IntelligentRecommendationService`), proporcionando un motor heurístico autónomo local para la sugerencia óptima del departamento municipal en el triaje de incidencias urbanas y la auditoría de su grado de adopción por parte de los operadores.

### 1.1 Componente de Negocio Implementado (`org.bulbaseros.service.IntelligentRecommendationService`)
- Componente Spring `@Service` con inyección explícita mediante `@Autowired` de `DepartamentoRepository`, `TareasRepository` e `IncidenciaRepository`.
- Métodos públicos desarrollados:
  - `recommendDepartment(String title, String description, String category)`: Analiza el contenido léxico de la incidencia contra el catálogo municipal en BD y computa la sugerencia departamental, estimación de certeza porcentual (0.0% a 100.0%) y justificación sintética explicativa.
  - `recommendDepartment(Incidencia incident)`: Sobrecarga null-safe que extrae los metadatos léxicos de la entidad de incidencia y delega en el motor heurístico.
  - `recommendDepartment(Long incidentId)`: Consulta la incidencia por identificador mediante el repositorio inyectado y obtiene la sugerencia, lanzando `IncidentNotFoundException` si no existe o `IllegalArgumentException` si el ID es nulo.
  - `recommendDepartment(Long incidentId, IncidenciaRepository customRepo)`: Sobrecarga flexible para permitir la inyección o uso de repositorios contextuales.
  - `evaluatePerformance(List<Incidencia> incidents)`: Evalúa el desempeño global del recomendador comparando las sugerencias calculadas contra las asignaciones históricas reales de `TareasRepository` (`findByIncidentIdIn`), calculando la tasa de adopción porcentual (`adoptionRate`) y la certeza media (`averageConfidence`).

---

## 2. Motor Heurístico Léxico Ponderado

El motor opera localmente sin dependencias externas mediante un analizador léxico ponderado con normalización probabilística de cadenas de texto:

### 2.1 Normalización Canónica de Cadenas
1. **Descomposición Unicode NFD:** Eliminación determinista de tildes y caracteres diacríticos (`Normalizer.normalize(..., Form.NFD).replaceAll("\\p{M}+", "")`), permitiendo que términos como *"árbol"*, *"vías"* o *"eléctrico"* coincidan con sus variantes sin acento.
2. **Transformación a Minúsculas e Higienización:** Conversión a minúsculas (`Locale.ROOT`) y sustitución de caracteres no alfanuméricos por espacios simples (`[^a-z0-9\\s]`).
3. **Mapeo Delimitado por Límites de Palabra (`\b`):** Compilación de expresiones regulares con límites de palabra para evitar falsos positivos por subcadenas espurias.

### 2.2 Diccionario Léxico Municipal y Correspondencia Dinámica
Se estructura un catálogo léxico canónico agrupado por áreas de especialidad técnica:
- **`VIAS_PUBLICAS`**: *bache, baches, asfalto, calzada, calzadas, acera, aceras, socavón, socavones, pavimento, adoquín, badén, badenes, baldosa, grieta, bordillo, vía, peatonal, hundimiento, zanja, alcantarilla...*
- **`ALUMBRADO`**: *farola, farolas, luz, luces, iluminación, bombilla, foco, apagada, alumbrado, poste, cable, electricidad, eléctrico, eléctrica, luminaria, fundida, no enciende, cortocircuito...*
- **`LIMPIEZA`**: *basura, basuras, contenedor, contenedores, desbordado, suciedad, papelera, residuos, limpieza, vertido, escombros, mancha, recogida, cristales, excrementos, malos olores, pintadas...*
- **`PARQUES_JARDINES`**: *árbol, árboles, rama, ramas, parque, jardín, jardines, césped, arbusto, plantas, vegetación, poda, hojas, zona infantil, columpio, columpios, banco, riego, caído, flores...*
- **`MOVILIDAD`**: *semáforo, semáforos, tráfico, señal, señales, paso de cebra, disco, carril bici, aparcamiento, estacionamiento, vehículo, coche, radar, parada, autobús, marquesina, circulación...*
- **`MEDIO_AMBIENTE`**: *ruido, contaminación, acústica, plaga, plagas, ratas, cucarachas, insectos, palomas, emisiones, humo, ambiental...*
- **`SANEAMIENTO`**: *tubería, fuga, fuga de agua, inundación, charco, atasco, sumidero, desagüe, fosas...*

Los departamentos registrados en BD (`departamentoRepository.findAllByOrderByNameAsc()`) se enlazan dinámicamente con estas familias léxicas analizando sus denominaciones oficiales, e incorporando de forma autónoma los términos específicos de cada departamento (longitud $\ge 3$) como palabras clave adicionales.

### 2.3 Matriz de Ponderación
Para cada término coincidente detectado en el reporte:
- **Coincidencia en Título:** Factor $\times 3.0$
- **Coincidencia en Categoría:** Factor $\times 3.0$
- **Coincidencia en Descripción:** Factor $\times 1.5$
- **Afinidad directa Categoría-Departamento:** Bonificación $+3.0$

---

## 3. Estimación del Grado de Confianza y Reglas de Advertencia (< 50%)

El cálculo de `confidencePercentage` (0.0% a 100.0%) y la activación del distintivo `isLowConfidenceWarning` (RF-17) siguen una jerarquía determinista de evaluación:

1. **Catálogo Vacío o Resiliencia de BD:**
   - Si no existen departamentos registrados en BD, retorna `confidence = 0.0%`, `isLowConfidenceWarning = true`, `suggestedDepartmentId = null` y aviso explicativo.
2. **Sin Coincidencias Léxicas:**
   - Si la puntuación máxima es $0.0$, retorna `confidence = 0.0%`, `isLowConfidenceWarning = true` y justificación indicando la ausencia de coincidencias léxicas con los departamentos municipales.
3. **Texto Extremadamente Escueto ($< 15$ caracteres):**
   - Si la suma de longitud de título y descripción es inferior a 15 caracteres (por ejemplo: *"Roto"* o *"Fallo"*), la confianza se restringe a un rango bajo ($15.0\% \le \text{confidence} \le 30.0\% < 50.0\%$) y se activa obligatoriamente `isLowConfidenceWarning = true` con una advertencia explícita en `reasoning`.
4. **Ambigüedad por Empate o Separación Estrecha entre Departamentos:**
   - Si el departamento líder ($S_1$) y el segundo ($S_2$) empatan o la diferencia porcentual $(S_1 - S_2) / S_1 \le 0.25$, se diagnostica ambigüedad léxica. La confianza se fija estrictamente por debajo del umbral ($25.0\% \le \text{confidence} \le 45.0\% < 50.0\%$), activando `isLowConfidenceWarning = true` y detallando en `reasoning` los dos departamentos en conflicto y los términos causantes.
5. **Coincidencia Clara y Dominante ($\ge 50\%$ hasta $98\%$):**
   - En casos de separación unívoca, se aplica la fórmula de consolidación:
     $$\text{confidence} = \min(98.0, \max(50.0, 55.0 + \min(30.0, S_1 \times 3.5) + \min(10.0, (S_1 - S_2) \times 2.5)))$$
   - Arroja valores típicamente superiores al $80\%-95\%$ y desactiva la advertencia (`isLowConfidenceWarning = false`).

---

## 4. Auditoría de Rendimiento y Tasa de Adopción (RF-18)

El método `evaluatePerformance(List<Incidencia> incidents)` suministra las métricas agregadas del motor de recomendación:
- **`totalRecommendations`**: Total de incidencias evaluadas en el lote.
- **`acceptedRecommendations`**: Número de incidencias donde el departamento sugerido por el asistente coincidió con alguno de los departamentos asignados históricamente en `assignments` (`tareasRepository.findByIncidentIdIn`).
- **`adoptionRate`**: Porcentaje de adopción calculado como $(\text{accepted} / \text{total}) \times 100.0$, redondeado a un decimal.
- **`averageConfidence`**: Grado de certeza medio de todas las recomendaciones emitidas en el conjunto evaluado.
- **Protección ante denominadores nulos:** Ante listas vacías o nulas, retorna contadores y porcentajes a `0.0` sin divisiones por cero ni `NaN`/`Infinity`.

---

## 5. Decisiones Técnicas y Arquitectónicas

1. **Independencia Heurística Autónoma:** El motor opera 100% en backend sin dependencias externas de APIs de terceros o modelos en la nube, garantizando cero latencia adicional en el triaje y cumplimiento íntegro del principio de autosuficiencia y soberanía de datos del proyecto.
2. **Inyección con `@Autowired` explícito:** Se garantiza compatibilidad total tanto con las pruebas unitarias basadas en Mockito (`@InjectMocks`) como con la inicialización de contexto completo de Spring Boot (`@SpringBootTest`).
3. **Resiliencia Matemática Absoluta:** Todas las métricas de porcentaje y promedios están blindadas ante colecciones vacías o textos nulos.

---

## 6. Cobertura y Verificación de Pruebas Unitarias

La suite `IntelligentRecommendationServiceTest` incluye 19 pruebas unitarias exhaustivas con JUnit 5 y Mockito:
- **`HighConfidenceRecommendationTests` (4 tests):**
  - Sugerencia de *"Vías Públicas"* para baches/asfalto/calzada con confianza $\ge 80\%$ y `isLowConfidenceWarning = false`.
  - Sugerencia de *"Alumbrado"* para farolas fundidas y luminarias.
  - Sugerencia de *"Limpieza"* para contenedores de basura desbordados y suciedad.
  - Sugerencia de *"Parques y Jardines"* para árboles caídos y ramas rotas en columpios.
- **`LowConfidenceAndEdgeCasesTests` (5 tests):**
  - Activación de advertencia y confianza $< 50\%$ por ambigüedad léxica entre departamentos.
  - Activación de advertencia y confianza $< 50\%$ ante textos escuetos ($< 15$ caracteres, ej. *"Roto"*).
  - Confianza $0.0\%$ y advertencia ante descripciones sin coincidencias léxicas.
  - Manejo seguro de campos y cadenas nulas.
  - Resiliencia ante catálogo de departamentos vacío en base de datos.
- **`OverloadedMethodsTests` (6 tests):**
  - Extracción y delegación correcta desde la entidad `Incidencia`.
  - Manejo seguro de entidad nula.
  - Consulta por identificador mediante `incidenciaRepository`.
  - Lanzamiento de `IncidentNotFoundException` ante IDs inexistentes.
  - Lanzamiento de `IllegalArgumentException` ante IDs nulos.
  - Delegación en repositorio personalizado (`customRepo`).
- **`AssistantPerformanceTests` (4 tests):**
  - Cálculo de tasa de adopción parcial (2 de 3 aceptadas $\rightarrow 66.7\%$).
  - Prevención de división por cero ante colección vacía.
  - Retorno seguro de métricas en cero ante parámetro nulo.
  - Cero recomendaciones aceptadas cuando no existen asignaciones en BD.

### Resultados de Ejecución
- `mvn test -Dtest=IntelligentRecommendationServiceTest`: **19 de 19 tests en verde (100%)**.
- `mvn test` (Suite completa de Backend): **529 tests ejecutados, 0 fallos, 0 errores (100% verde)**.
