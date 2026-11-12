package org.bulbaseros.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bulbaseros.dto.AssistantPerformanceResponse;
import org.bulbaseros.dto.DepartmentRecommendationResponse;
import org.bulbaseros.dto.v2.DepartmentRecommendationResponseV2;
import org.bulbaseros.entity.Departamento;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.Tareas;
import org.bulbaseros.exception.IncidentNotFoundException;
import org.bulbaseros.repository.DepartamentoRepository;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.TareasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio de negocio responsable del motor heurístico de asistencia inteligente y sugerencia
 * del departamento municipal idóneo para el triaje y asignación de incidencias urbanas.
 * <p>
 * Implementa un analizador léxico ponderado con normalización probabilística de texto (remoción de tildes,
 * puntuación y transformación a minúsculas) para computar el grado de certeza o confianza porcentual
 * (0.0% a 100.0%) de cada recomendación (RF-14, RF-15, RF-16).
 * </p>
 * <p>
 * Asimismo, activa una advertencia explícita ({@code isLowConfidenceWarning = true}) ante textos escuetos,
 * ambiguos o con confianza inferior al umbral del 50% (RF-17), y suministra métricas agregadas de desempeño
 * y tasa histórica de adopción de las recomendaciones por los operadores (RF-18).
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
@Slf4j
public class IntelligentRecommendationService {

    private final DepartamentoRepository departamentoRepository;
    private final TareasRepository tareasRepository;
    private final IncidenciaRepository incidenciaRepository;

    /**
     * Patrón de expresiones regulares para la detección y remoción de diacríticos y tildes Unicode.
     */
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");

    /**
     * Patrón de expresiones regulares para sustituir caracteres no alfanuméricos por espacios.
     */
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9\\s]");

    /**
     * Palabras vacías o stopwords comunes que deben omitirse al extraer términos canónicos de nombres de departamento.
     */
    private static final Set<String> STOPWORDS = Set.of(
            "de", "del", "la", "las", "el", "los", "en", "y", "e", "o", "u", "para", "por", "con", "sin", "un", "una"
    );

    /**
     * Diccionario léxico municipal estructurado por familias temáticas canónicas.
     */
    private static final Map<String, List<String>> CANONICAL_KEYWORDS = Map.of(
            "VIAS_PUBLICAS", List.of(
                    "bache", "baches", "asfalto", "calzada", "calzadas", "acera", "aceras",
                    "socavon", "socavones", "pavimento", "adoquin", "adoquines", "baden",
                    "badenes", "baldosa", "baldosas", "grieta", "grietas", "obra", "obras",
                    "bordillo", "bordillos", "via", "vias", "peatonal", "hundimiento",
                    "alquitran", "zanja", "zanjas", "desnivel", "alcantarilla", "tapa de alcantarilla"
            ),
            "ALUMBRADO", List.of(
                    "farola", "farolas", "luz", "luces", "iluminacion", "bombilla", "bombillas",
                    "foco", "focos", "apagada", "apagadas", "alumbrado", "poste", "postes",
                    "cable", "cables", "cableado", "electricidad", "electrico", "electrica",
                    "luminaria", "luminarias", "intermitente", "fundida", "fundidas",
                    "no enciende", "enciende", "cortocircuito", "farola apagada"
            ),
            "LIMPIEZA", List.of(
                    "basura", "basuras", "contenedor", "contenedores", "desbordado", "desbordados",
                    "suciedad", "papelera", "papeleras", "residuos", "limpieza", "vertido",
                    "vertidos", "escombros", "mancha", "manchas", "recogida", "cristales",
                    "orina", "orines", "caca", "excrementos", "desperdicios", "reciclaje",
                    "maleza", "olores", "mal olor", "graffiti", "pintada", "pintadas"
            ),
            "PARQUES_JARDINES", List.of(
                    "arbol", "arboles", "rama", "ramas", "parque", "parques", "jardin", "jardines",
                    "cesped", "arbusto", "arbustos", "planta", "plantas", "vegetacion", "poda",
                    "podas", "hojas", "zona infantil", "columpio", "columpios", "banco", "bancos",
                    "riego", "jardinera", "jardineras", "caido", "caidos", "caida", "caidas",
                    "flores", "palmera", "palmeras"
            ),
            "MOVILIDAD", List.of(
                    "semaforo", "semaforos", "trafico", "senal", "senales", "paso de cebra",
                    "disco", "discos", "carril", "carril bici", "aparcamiento", "aparcamientos",
                    "estacionamiento", "vehiculo", "vehiculos", "coche", "coches", "radares",
                    "parada", "autobus", "autobuses", "marquesina", "marquesinas", "circulacion",
                    "vial", "viales", "valla", "vallas", "velocidad"
            ),
            "MEDIO_AMBIENTE", List.of(
                    "ruido", "ruidos", "contaminacion", "acustica", "plaga", "plagas", "ratas",
                    "cucarachas", "insectos", "palomas", "aire", "emisiones", "humo",
                    "medio ambiente", "ambiental"
            ),
            "SANEAMIENTO", List.of(
                    "tuberia", "tuberias", "fuga", "fugas", "fuga de agua", "inundacion",
                    "charco", "atasco", "sumidero", "sumideros", "desague", "fosas"
            )
    );

    /**
     * Constructor principal de inyección de dependencias para Spring.
     *
     * @param departamentoRepository Repositorio de consulta de departamentos municipales.
     * @param tareasRepository       Repositorio de asignaciones operativas históricas y vigentes.
     * @param incidenciaRepository   Repositorio de acceso a incidencias urbanas.
     */
    @Autowired
    public IntelligentRecommendationService(DepartamentoRepository departamentoRepository,
                                           TareasRepository tareasRepository,
                                           IncidenciaRepository incidenciaRepository) {
        this.departamentoRepository = departamentoRepository;
        this.tareasRepository = tareasRepository;
        this.incidenciaRepository = incidenciaRepository;
    }

    /**
     * Constructor de conveniencia para instanciar el servicio con los repositorios esenciales.
     *
     * @param departamentoRepository Repositorio de departamentos municipales.
     * @param tareasRepository       Repositorio de asignaciones operativas.
     */
    public IntelligentRecommendationService(DepartamentoRepository departamentoRepository,
                                           TareasRepository tareasRepository) {
        this(departamentoRepository, tareasRepository, null);
    }

    /**
     * Genera la sugerencia asistida de departamento municipal a partir del título, descripción y categoría
     * reportados para una incidencia urbana.
     * <p>
     * Aplica la ponderación léxica de coincidencias:
     * <ul>
     *   <li>Términos coincidentes en Título: factor &times;3.0</li>
     *   <li>Términos coincidentes en Categoría: factor &times;3.0</li>
     *   <li>Términos coincidentes en Descripción: factor &times;1.5</li>
     * </ul>
     * Si la longitud del texto es inferior a 15 caracteres, existe ambigüedad cercana entre departamentos o
     * no hay coincidencias léxicas concluyentes, el grado de confianza se fija en un valor estrictamente inferior
     * al 50.0% con la advertencia {@code isLowConfidenceWarning = true}.
     * </p>
     *
     * @param title       Título descriptivo de la incidencia.
     * @param description Detalle explicativo de la incidencia.
     * @param category    Categoría temática o clasificación inicial.
     * @return {@link DepartmentRecommendationResponse} con el departamento sugerido, porcentaje de confianza y justificación.
     */
    @Transactional(readOnly = true)
    public DepartmentRecommendationResponse recommendDepartment(String title, String description, String category) {
        List<Departamento> departments = departamentoRepository.findAllByOrderByNameAsc();

        if (departments == null || departments.isEmpty()) {
            log.warn("El catálogo de departamentos municipales está vacío; no es posible sugerir asignación.");
            return DepartmentRecommendationResponse.builder()
                    .suggestedDepartmentId(null)
                    .suggestedDepartmentName(null)
                    .confidencePercentage(0.0)
                    .isLowConfidenceWarning(true)
                    .reasoning("No se encontraron departamentos municipales registrados en el catálogo.")
                    .build();
        }

        String normalizedTitle = normalize(title);
        String normalizedDesc = normalize(description);
        String normalizedCategory = normalize(category);

        int totalTextLength = (title != null ? title.trim().length() : 0)
                + (description != null ? description.trim().length() : 0);
        boolean isEscueto = totalTextLength < 15;

        List<DepartmentEvaluation> evaluations = new ArrayList<>();

        for (Departamento dept : departments) {
            Set<String> keywords = resolveKeywordsForDepartment(dept);
            String deptNameNorm = normalize(dept.getName());

            double score = 0.0;
            Set<String> matchedTerms = new LinkedHashSet<>();

            for (String keyword : keywords) {
                if (keyword.isBlank()) continue;
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b");

                boolean inTitle = !normalizedTitle.isEmpty() && pattern.matcher(normalizedTitle).find();
                boolean inCategory = !normalizedCategory.isEmpty() && pattern.matcher(normalizedCategory).find();
                boolean inDesc = !normalizedDesc.isEmpty() && pattern.matcher(normalizedDesc).find();

                if (inTitle) {
                    score += 3.0;
                    matchedTerms.add(keyword);
                }
                if (inCategory) {
                    score += 3.0;
                    matchedTerms.add(keyword);
                }
                if (inDesc) {
                    score += 1.5;
                    matchedTerms.add(keyword);
                }
            }

            // Bonificación si la categoría temática coincide explícitamente con el nombre del departamento
            if (!normalizedCategory.isEmpty() && !deptNameNorm.isEmpty()) {
                if (normalizedCategory.contains(deptNameNorm) || deptNameNorm.contains(normalizedCategory)) {
                    score += 3.0;
                    matchedTerms.add(dept.getName());
                }
            }

            evaluations.add(new DepartmentEvaluation(dept, score, matchedTerms));
        }

        // Ordenar descendentemente por puntuación obtenida
        evaluations.sort((a, b) -> Double.compare(b.score(), a.score()));

        DepartmentEvaluation best = evaluations.get(0);
        DepartmentEvaluation second = evaluations.size() > 1 ? evaluations.get(1) : null;

        // Caso 1: Sin coincidencias léxicas o texto completamente sin términos concluyentes
        if (best.score() <= 0.0) {
            String reasoning = isEscueto
                    ? "Texto demasiado escueto (< 15 caracteres) y sin coincidencias léxicas concluyentes. Requiere validación manual."
                    : "Sin coincidencias léxicas claras con los departamentos disponibles. Se sugiere revisión manual.";
            return DepartmentRecommendationResponse.builder()
                    .suggestedDepartmentId(best.department().getId())
                    .suggestedDepartmentName(best.department().getName())
                    .confidencePercentage(0.0)
                    .isLowConfidenceWarning(true)
                    .reasoning(reasoning)
                    .build();
        }

        // Caso 2: Texto extremadamente escueto (< 15 caracteres) con coincidencia léxica parcial
        if (isEscueto) {
            double confidence = Math.min(30.0, Math.max(15.0, Math.round(best.score() * 4.0 * 10.0) / 10.0));
            String termsSummary = best.matchedTerms().isEmpty()
                    ? "sin coincidencias concluyentes"
                    : String.join(", ", best.matchedTerms());
            return DepartmentRecommendationResponse.builder()
                    .suggestedDepartmentId(best.department().getId())
                    .suggestedDepartmentName(best.department().getName())
                    .confidencePercentage(confidence)
                    .isLowConfidenceWarning(true)
                    .reasoning("Texto demasiado escueto (< 15 caracteres) para determinar con certeza el departamento competente. Coincidencias: " + termsSummary)
                    .build();
        }

        // Caso 3: Ambigüedad por puntuaciones idénticas o muy próximas entre el primero y segundo
        boolean isAmbiguous = second != null && second.score() > 0.0 &&
                (best.score() == second.score() || ((best.score() - second.score()) / best.score() <= 0.25));

        if (isAmbiguous) {
            double margin = best.score() - second.score();
            double confidence = Math.min(45.0, Math.max(25.0, Math.round((35.0 + margin * 3.0) * 10.0) / 10.0));
            String termsSummary = String.join(", ", best.matchedTerms());
            return DepartmentRecommendationResponse.builder()
                    .suggestedDepartmentId(best.department().getId())
                    .suggestedDepartmentName(best.department().getName())
                    .confidencePercentage(confidence)
                    .isLowConfidenceWarning(true)
                    .reasoning(String.format("Confianza baja por ambigüedad léxica entre '%s' y '%s'. Coincidencias detectadas: %s",
                            best.department().getName(), second.department().getName(), termsSummary))
                    .build();
        }

        // Caso 4: Coincidencia concluyente con separación clara
        double margin = second != null ? (best.score() - second.score()) : best.score();
        double baseConfidence = 55.0 + Math.min(30.0, best.score() * 3.5) + Math.min(10.0, margin * 2.5);
        double confidence = Math.min(98.0, Math.max(50.0, baseConfidence));
        confidence = Math.round(confidence * 10.0) / 10.0;

        String termsSummary = String.join(", ", best.matchedTerms());
        return DepartmentRecommendationResponse.builder()
                .suggestedDepartmentId(best.department().getId())
                .suggestedDepartmentName(best.department().getName())
                .confidencePercentage(confidence)
                .isLowConfidenceWarning(false)
                .reasoning(String.format("Recomendación de alta confianza basada en coincidencias léxicas con %s: %s",
                        best.department().getName(), termsSummary))
                .build();
    }

    /**
     * Genera la sugerencia asistida de departamento a partir de una entidad de incidencia existente.
     *
     * @param incident Entidad de incidencia con título, descripción y categoría.
     * @return {@link DepartmentRecommendationResponse} con la recomendación calculada.
     */
    @Transactional(readOnly = true)
    public DepartmentRecommendationResponse recommendDepartment(Incidencia incident) {
        if (incident == null) {
            return DepartmentRecommendationResponse.builder()
                    .suggestedDepartmentId(null)
                    .suggestedDepartmentName(null)
                    .confidencePercentage(0.0)
                    .isLowConfidenceWarning(true)
                    .reasoning("La incidencia suministrada es nula.")
                    .build();
        }
        return recommendDepartment(incident.getTitle(), incident.getDescription(), incident.getCategory());
    }

    /**
     * Genera la sugerencia asistida de departamento a partir del identificador de una incidencia
     * utilizando un repositorio específico suministrado externamente.
     *
     * @param incidentId   Identificador de la incidencia.
     * @param customRepo   Repositorio de incidencias a utilizar (opcional, si es nulo utiliza el inyectado).
     * @return {@link DepartmentRecommendationResponse} con la recomendación calculada.
     * @throws IllegalArgumentException si {@code incidentId} es nulo.
     * @throws IncidentNotFoundException si no existe la incidencia con dicho identificador.
     */
    @Transactional(readOnly = true)
    public DepartmentRecommendationResponse recommendDepartment(Long incidentId, IncidenciaRepository customRepo) {
        if (incidentId == null) {
            throw new IllegalArgumentException("El identificador de incidencia no puede ser nulo");
        }
        IncidenciaRepository repoToUse = customRepo != null ? customRepo : this.incidenciaRepository;
        if (repoToUse == null) {
            throw new IllegalStateException("No hay un repositorio de incidencias configurado en el servicio");
        }
        Incidencia incident = repoToUse.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
        return recommendDepartment(incident);
    }

    /**
     * Genera la sugerencia asistida de departamento a partir del identificador de una incidencia
     * utilizando el repositorio inyectado en el servicio.
     *
     * @param incidentId Identificador único de la incidencia.
     * @return {@link DepartmentRecommendationResponse} con la recomendación calculada.
     */
    @Transactional(readOnly = true)
    public DepartmentRecommendationResponse recommendDepartment(Long incidentId) {
        return recommendDepartment(incidentId, this.incidenciaRepository);
    }

    /**
     * Genera la sugerencia asistida de departamento para la API v2 con identificador publico UUID.
     *
     * @param incidentPublicId Identificador publico de la incidencia (opcional).
     * @param title Titulo para inferencia ad-hoc si no hay id.
     * @param description Descripcion para inferencia ad-hoc si no hay id.
     * @param category Categoria para inferencia ad-hoc si no hay id.
     * @return DTO {@link DepartmentRecommendationResponseV2} con el UUID del departamento sugerido.
     */
    @Transactional(readOnly = true)
    public DepartmentRecommendationResponseV2 recommendDepartmentV2(java.util.UUID incidentPublicId, String title, String description, String category) {
        DepartmentRecommendationResponse v1Res;
        if (incidentPublicId != null) {
            Incidencia inc = incidenciaRepository.findByPublicId(incidentPublicId)
                    .orElseThrow(() -> new IncidentNotFoundException("Incidencia no encontrada con publicId: " + incidentPublicId));
            v1Res = recommendDepartment(inc);
        } else {
            v1Res = recommendDepartment(title, description, category);
        }

        String deptPublicId = null;
        if (v1Res.getSuggestedDepartmentId() != null) {
            deptPublicId = departamentoRepository.findById(v1Res.getSuggestedDepartmentId())
                    .map(d -> d.getPublicId() != null ? d.getPublicId().toString() : null)
                    .orElse(null);
        }

        return DepartmentRecommendationResponseV2.builder()
                .suggestedDepartmentId(deptPublicId)
                .suggestedDepartmentName(v1Res.getSuggestedDepartmentName())
                .confidencePercentage(v1Res.getConfidencePercentage())
                .isLowConfidenceWarning(v1Res.getIsLowConfidenceWarning())
                .rationale(v1Res.getReasoning())
                .build();
    }

    /**
     * Evalúa el rendimiento global y la tasa histórica de adopción de las recomendaciones asistidas
     * sobre un conjunto de incidencias frente a las asignaciones reales registradas en la base de datos (RF-18).
     * <p>
     * Se considera que una recomendación fue aceptada si el departamento sugerido coincide con alguno de los
     * departamentos asignados históricamente a la incidencia en la tabla de asignaciones.
     * Previene divisiones por cero ante colecciones vacías devolviendo métricas a 0.0.
     * </p>
     *
     * @param incidents Lista de incidencias a evaluar.
     * @return {@link AssistantPerformanceResponse} con total de recomendaciones, aceptadas, tasa de adopción y confianza promedio.
     */
    @Transactional(readOnly = true)
    public AssistantPerformanceResponse evaluatePerformance(List<Incidencia> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return AssistantPerformanceResponse.builder()
                    .totalRecommendations(0L)
                    .acceptedRecommendations(0L)
                    .adoptionRate(0.0)
                    .averageConfidence(0.0)
                    .build();
        }

        List<Long> incidentIds = incidents.stream()
                .map(Incidencia::getId)
                .filter(Objects::nonNull)
                .toList();

        List<Tareas> allAssignments = incidentIds.isEmpty()
                ? Collections.emptyList()
                : tareasRepository.findByIncidentIdIn(incidentIds);

        Map<Long, List<Tareas>> assignmentsByIncidentId = allAssignments.stream()
                .filter(t -> t.getIncident() != null && t.getIncident().getId() != null)
                .collect(Collectors.groupingBy(t -> t.getIncident().getId()));

        long totalEvaluated = 0L;
        long acceptedRecommendations = 0L;
        double totalConfidence = 0.0;

        for (Incidencia incident : incidents) {
            DepartmentRecommendationResponse recommendation = recommendDepartment(incident);
            totalEvaluated++;
            if (recommendation.getConfidencePercentage() != null) {
                totalConfidence += recommendation.getConfidencePercentage();
            }

            Long suggestedDeptId = recommendation.getSuggestedDepartmentId();
            if (suggestedDeptId != null && incident.getId() != null) {
                List<Tareas> incidentTasks = assignmentsByIncidentId.getOrDefault(incident.getId(), Collections.emptyList());
                boolean matched = incidentTasks.stream()
                        .anyMatch(t -> t.getDepartment() != null && suggestedDeptId.equals(t.getDepartment().getId()));
                if (matched) {
                    acceptedRecommendations++;
                }
            }
        }

        double adoptionRate = totalEvaluated > 0
                ? Math.round(((double) acceptedRecommendations / totalEvaluated * 100.0) * 10.0) / 10.0
                : 0.0;

        double averageConfidence = totalEvaluated > 0
                ? Math.round((totalConfidence / totalEvaluated) * 10.0) / 10.0
                : 0.0;

        return AssistantPerformanceResponse.builder()
                .totalRecommendations(totalEvaluated)
                .acceptedRecommendations(acceptedRecommendations)
                .adoptionRate(adoptionRate)
                .averageConfidence(averageConfidence)
                .build();
    }

    /**
     * Resuelve el conjunto consolidado de palabras clave aplicables a un departamento municipal,
     * fusionando términos canónicos según su especialidad con las palabras propias de su denominación.
     *
     * @param dept Departamento municipal a categorizar.
     * @return Conjunto no mutable o unificado de palabras clave normalizadas.
     */
    private Set<String> resolveKeywordsForDepartment(Departamento dept) {
        String deptNameNorm = normalize(dept.getName());
        Set<String> keywords = new LinkedHashSet<>();

        // Términos propios del nombre del departamento (longitud >= 3 omitiendo stopwords)
        for (String word : deptNameNorm.split("\\s+")) {
            if (word.length() >= 3 && !STOPWORDS.contains(word)) {
                keywords.add(word);
            }
        }
        if (!deptNameNorm.isBlank()) {
            keywords.add(deptNameNorm);
        }

        // Asociación con familias léxicas canónicas
        if (containsAny(deptNameNorm, "via", "public", "paviment", "calzad", "acera", "bache", "obra", "infraestructur")) {
            keywords.addAll(CANONICAL_KEYWORDS.get("VIAS_PUBLICAS"));
        }
        if (containsAny(deptNameNorm, "alumbr", "ilumin", "electric", "farol", "luminos")) {
            keywords.addAll(CANONICAL_KEYWORDS.get("ALUMBRADO"));
        }
        if (containsAny(deptNameNorm, "limpi", "residu", "basur", "recogid")) {
            keywords.addAll(CANONICAL_KEYWORDS.get("LIMPIEZA"));
        }
        if (containsAny(deptNameNorm, "parque", "jardin", "verde", "arbol", "forestal", "botanic")) {
            keywords.addAll(CANONICAL_KEYWORDS.get("PARQUES_JARDINES"));
        }
        if (containsAny(deptNameNorm, "movil", "trafic", "senal", "transp", "circul", "vial")) {
            keywords.addAll(CANONICAL_KEYWORDS.get("MOVILIDAD"));
        }
        if (containsAny(deptNameNorm, "saneam", "agua", "alcantarill", "inund", "hidraul")) {
            keywords.addAll(CANONICAL_KEYWORDS.get("SANEAMIENTO"));
        }
        if (containsAny(deptNameNorm, "ambient", "ecolog", "ruid", "plaga", "fauna")) {
            keywords.addAll(CANONICAL_KEYWORDS.get("MEDIO_AMBIENTE"));
        }

        return keywords;
    }

    /**
     * Comprueba si una cadena de texto contiene alguna de las subcadenas suministradas.
     *
     * @param text     Texto en el que buscar.
     * @param patterns Patrones o fragmentos a verificar.
     * @return {@code true} si al menos un patrón coincide; {@code false} en caso contrario.
     */
    private boolean containsAny(String text, String... patterns) {
        if (text == null || text.isBlank()) return false;
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normaliza una cadena de texto eliminando tildes, signos ortográficos y convirtiendo a minúsculas.
     *
     * @param input Cadena original de entrada.
     * @return Cadena normalizada en minúsculas y caracteres alfanuméricos con espacios simples.
     */
    private String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized.toLowerCase(Locale.ROOT);
        return NON_ALPHANUMERIC_PATTERN.matcher(normalized).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Registro auxiliar inmutable para almacenar la evaluación léxica de un departamento municipal.
     */
    private record DepartmentEvaluation(Departamento department, double score, Set<String> matchedTerms) {}
}
