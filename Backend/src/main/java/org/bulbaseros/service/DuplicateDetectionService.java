package org.bulbaseros.service;

import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.entity.RolUsuario;
import org.bulbaseros.entity.Usuario;
import org.bulbaseros.repository.IncidenciaRepository;
import org.bulbaseros.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de detección espaciotemporal y semántica de incidencias urbanas duplicadas.
 * <p>
 * Evalúa candidatos a duplicado combinando distancia Haversine (&le; 60 m), ventana temporal
 * de creación (&le; 48 h), coincidencia estricta de categoría y similitud léxica normalizada (Jaccard &ge; 0.3)
 * sobre el título y la descripción de la incidencia. Emite notificaciones de tipo
 * {@code DUPLICATE_SUGGESTION} exclusivamente a usuarios con rol {@link RolUsuario#OPERATOR}
 * y {@link RolUsuario#TECHNICIAN}.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class DuplicateDetectionService {

    /** Radio de la Tierra en metros utilizado en el cálculo de la fórmula de Haversine. */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /** Distancia máxima en metros para considerar dos incidencias como posibles duplicados (60 m). */
    private static final double MAX_DISTANCE_METERS = 60.0;

    /** Ventana temporal máxima en horas para considerar dos incidencias como posibles duplicados (48 h). */
    private static final long MAX_HOURS_DIFF = 48L;

    /** Umbral mínimo de similitud de Jaccard para considerar coincidencia semántica (0.3). */
    private static final double SIMILARITY_THRESHOLD = 0.3;

    private final IncidenciaRepository incidenciaRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Construye una nueva instancia con las dependencias inyectadas por constructor.
     *
     * @param incidenciaRepository Repositorio de incidencias para consultar candidatos.
     * @param userRepository       Repositorio de usuarios para recuperar operadores y técnicos.
     * @param notificationService  Servicio de notificaciones para emitir alertas de duplicado.
     */
    public DuplicateDetectionService(IncidenciaRepository incidenciaRepository,
                                     UserRepository userRepository,
                                     NotificationService notificationService) {
        this.incidenciaRepository = incidenciaRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Busca incidencias candidatas a duplicado respecto a una nueva incidencia recién creada o recibida.
     * <p>
     * Aplica los cuatro filtros en orden: ventana temporal de 48 h, misma categoría,
     * distancia Haversine &le; 60 m y similitud léxica normalizada de Jaccard &ge; 0.3.
     * Excluye la propia incidencia si ya está persistida.
     * </p>
     *
     * @param lat               Latitud de la nueva incidencia.
     * @param lng               Longitud de la nueva incidencia.
     * @param category          Categoría de la nueva incidencia.
     * @param title             Título de la nueva incidencia.
     * @param description       Descripción de la nueva incidencia.
     * @param excludeIncidentId Identificador de la propia incidencia a excluir (puede ser {@code null}).
     * @return Lista de incidencias que superan todos los filtros de duplicado.
     */
    @Transactional(readOnly = true)
    public List<Incidencia> findPotentialDuplicates(Double lat, Double lng, String category,
                                                    String title, String description,
                                                    Long excludeIncidentId) {
        if (lat == null || lng == null) {
            return List.of();
        }

        OffsetDateTime since = OffsetDateTime.now().minusHours(MAX_HOURS_DIFF);
        OffsetDateTime now = OffsetDateTime.now();

        List<Incidencia> candidates = incidenciaRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(since, now);

        return candidates.stream()
                .filter(i -> excludeIncidentId == null || !i.getId().equals(excludeIncidentId))
                .filter(i -> i.getLatitude() != null && i.getLongitude() != null)
                .filter(i -> category != null && category.equalsIgnoreCase(i.getCategory()))
                .filter(i -> haversineDistance(lat, lng, i.getLatitude(), i.getLongitude()) <= MAX_DISTANCE_METERS)
                .filter(i -> jaccardSimilarity(title, description, i.getTitle(), i.getDescription()) >= SIMILARITY_THRESHOLD)
                .collect(Collectors.toList());
    }

    /**
     * Emite notificaciones de tipo {@code DUPLICATE_SUGGESTION} a todos los operadores y técnicos
     * del sistema cuando se detectan candidatos a duplicado de una incidencia nueva.
     * <p>
     * Solo se notifica si existe al menos un candidato a duplicado. Las notificaciones
     * se omiten para roles {@code CITIZEN}, {@code ADMIN} y {@code ANALYST}.
     * </p>
     *
     * @param newIncident Incidencia recién creada que podría ser duplicada.
     * @param candidates  Lista de incidencias candidatas a duplicado encontradas.
     */
    @Transactional
    public void notifyPotentialDuplicates(Incidencia newIncident, List<Incidencia> candidates) {
        if (newIncident == null || candidates == null || candidates.isEmpty() || notificationService == null) {
            return;
        }

        String title = "Posible incidencia duplicada detectada #" + newIncident.getId();
        String message = "La incidencia '" + newIncident.getTitle() + "' (id: " + newIncident.getId()
                + ") puede ser duplicada de " + candidates.size() + " incidencia(s) existente(s).";

        List<Usuario> operators = userRepository.findByRole(RolUsuario.OPERATOR);
        List<Usuario> technicians = userRepository.findByRole(RolUsuario.TECHNICIAN);

        if (operators != null) {
            for (Usuario operator : operators) {
                notificationService.createNotification(operator, newIncident, "DUPLICATE_SUGGESTION", title, message);
            }
        }
        if (technicians != null) {
            for (Usuario technician : technicians) {
                notificationService.createNotification(technician, newIncident, "DUPLICATE_SUGGESTION", title, message);
            }
        }
    }

    /**
     * Calcula la distancia en metros entre dos puntos geográficos usando la fórmula de Haversine.
     *
     * @param lat1 Latitud del primer punto en grados decimales.
     * @param lng1 Longitud del primer punto en grados decimales.
     * @param lat2 Latitud del segundo punto en grados decimales.
     * @param lng2 Longitud del segundo punto en grados decimales.
     * @return Distancia en metros entre ambos puntos.
     */
    public double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Calcula la similitud de Jaccard normalizada entre el texto combinado de dos incidencias.
     * <p>
     * Normaliza ambos textos (minúsculas, sin acentos, sin puntuación) y calcula
     * intersección / unión de conjuntos de palabras.
     * </p>
     *
     * @param title1       Título de la primera incidencia.
     * @param description1 Descripción de la primera incidencia.
     * @param title2       Título de la segunda incidencia.
     * @param description2 Descripción de la segunda incidencia.
     * @return Coeficiente de similitud de Jaccard entre [0.0, 1.0].
     */
    public double jaccardSimilarity(String title1, String description1, String title2, String description2) {
        Set<String> words1 = extractWords((title1 != null ? title1 : "") + " " + (description1 != null ? description1 : ""));
        Set<String> words2 = extractWords((title2 != null ? title2 : "") + " " + (description2 != null ? description2 : ""));

        if (words1.isEmpty() && words2.isEmpty()) {
            return 1.0;
        }
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Extrae el conjunto de palabras normalizadas de un texto dado.
     * <p>
     * Aplica: conversión a minúsculas, eliminación de acentos, eliminación de
     * caracteres no alfanuméricos y filtrado de palabras vacías.
     * </p>
     *
     * @param text Texto de entrada a normalizar.
     * @return Conjunto de palabras normalizadas y no vacías.
     */
    private Set<String> extractWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new HashSet<>();
        }
        String normalized = text.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ü", "u")
                .replace("ñ", "n").replace("à", "a").replace("è", "e")
                .replaceAll("[^a-z0-9\\s]", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .filter(w -> !w.isEmpty())
                .collect(Collectors.toSet());
    }
}
