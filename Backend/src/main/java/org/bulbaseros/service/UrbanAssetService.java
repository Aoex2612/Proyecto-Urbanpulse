package org.bulbaseros.service;

import org.bulbaseros.dto.IncidentAssetResponse;
import org.bulbaseros.dto.v2.IncidentAssetResponseV2;
import org.bulbaseros.entity.ActivoUrbano;
import org.bulbaseros.entity.AsociacionIncidenciaActivo;
import org.bulbaseros.entity.AsociacionIncidenciaActivoId;
import org.bulbaseros.entity.Incidencia;
import org.bulbaseros.mappers.UrbanAssetMapper;
import org.bulbaseros.mappers.UrbanAssetMapperV2;
import org.bulbaseros.repository.ActivoUrbanoRepository;
import org.bulbaseros.repository.AsociacionIncidenciaActivoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de lógica de negocio para la gestión, cálculo espacial de proximidad
 * y vinculación de activos urbanos con incidencias en la plataforma UrbanPulse.
 * <p>
 * Implementa el cálculo geodésico de distancias mediante la fórmula de Haversine,
 * la determinación probabilística del índice de confianza, el prefiltrado espacial
 * mediante cajas delimitadoras (bounding box) y las operaciones de confirmación
 * y desvinculación de mobiliario e infraestructuras urbanas.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Service
public class UrbanAssetService {

    /**
     * Radio medio de la Tierra en metros (WGS84 aproximado).
     */
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    /**
     * Radio máximo de proximidad en metros para sugerir activos urbanos contiguos (50.0 metros).
     */
    public static final double DEFAULT_MAX_RADIUS_METERS = 50.0;

    /**
     * Metros aproximados por grado de latitud terrestre.
     */
    private static final double METERS_PER_DEGREE_LATITUDE = 111320.0;

    private final ActivoUrbanoRepository activoUrbanoRepository;
    private final AsociacionIncidenciaActivoRepository asociacionRepository;
    private final UrbanAssetMapper urbanAssetMapper;
    private final UrbanAssetMapperV2 urbanAssetMapperV2;

    public UrbanAssetService(
            ActivoUrbanoRepository activoUrbanoRepository,
            AsociacionIncidenciaActivoRepository asociacionRepository,
            UrbanAssetMapper urbanAssetMapper
    ) {
        this(activoUrbanoRepository, asociacionRepository, urbanAssetMapper, new UrbanAssetMapperV2());
    }

    @Autowired
    public UrbanAssetService(
            ActivoUrbanoRepository activoUrbanoRepository,
            AsociacionIncidenciaActivoRepository asociacionRepository,
            UrbanAssetMapper urbanAssetMapper,
            @Autowired(required = false) UrbanAssetMapperV2 urbanAssetMapperV2
    ) {
        this.activoUrbanoRepository = activoUrbanoRepository;
        this.asociacionRepository = asociacionRepository;
        this.urbanAssetMapper = urbanAssetMapper;
        this.urbanAssetMapperV2 = urbanAssetMapperV2 != null ? urbanAssetMapperV2 : new UrbanAssetMapperV2();
    }

    /**
     * Calcula la distancia ortodrómica o geodésica sobre la superficie terrestre en metros
     * entre dos pares de coordenadas (latitud/longitud) empleando la fórmula trigonométrica de Haversine.
     *
     * @param lat1 Latitud del primer punto en grados decimales.
     * @param lon1 Longitud del primer punto en grados decimales.
     * @param lat2 Latitud del segundo punto en grados decimales.
     * @param lon2 Longitud del segundo punto en grados decimales.
     * @return Distancia geodésica entre ambos puntos expresada en metros, redondeada a dos decimales.
     */
    public double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_METERS * c;

        return round(distance, 2);
    }

    /**
     * Determina el índice o puntuación probabilística de confianza normalizada en el rango [0.0, 1.0]
     * asociada a un activo en función de su cercanía espacial respecto al epicentro de la incidencia.
     * <p>
     * Fórmula aplicada: {@code Math.max(0.0, Math.min(1.0, 1.0 - (distanceMeters / 50.0)))}
     * proporcionando 1.0 a distancia 0 metros, 0.5 a 25 metros y 0.0 a 50 metros o superior.
     * </p>
     *
     * @param distanceMeters Distancia calculada en metros entre la incidencia y el activo.
     * @return Nivel de confianza normalizado entre 0.0 y 1.0, redondeado a cuatro cifras decimales.
     */
    public double calculateConfidenceScore(double distanceMeters) {
        if (distanceMeters < 0.0) {
            distanceMeters = 0.0;
        }
        double rawScore = 1.0 - (distanceMeters / DEFAULT_MAX_RADIUS_METERS);
        double clampedScore = Math.max(0.0, Math.min(1.0, rawScore));

        return round(clampedScore, 4);
    }

    /**
     * Localiza, asocia y persiste los activos urbanos más próximos situados dentro del radio máximo
     * de 50 metros con respecto a las coordenadas geográficas de la incidencia reportada.
     * <p>
     * Realiza un prefiltrado espacial previo mediante caja delimitadora (bounding box) en base de datos
     * para optimizar el rendimiento y posteriormente computa la distancia exacta de Haversine y el score
     * de confianza de cada candidato. Las asociaciones se registran en estado no confirmado ({@code isConfirmed = false})
     * y se devuelven ordenadas de menor a mayor distancia.
     * Si no se encuentran elementos en el radio establecido, retorna una lista vacía de forma transparente (RF-6).
     * </p>
     *
     * @param incident Entidad de la incidencia reportada, con identificador y coordenadas válidas.
     * @return Lista de DTOs {@link IncidentAssetResponse} con los activos asociados ordenados por proximidad ascendente.
     */
    @Transactional
    public List<IncidentAssetResponse> associateNearbyAssets(Incidencia incident) {
        if (incident == null || incident.getId() == null || incident.getLatitude() == null || incident.getLongitude() == null) {
            return List.of();
        }

        double lat = incident.getLatitude();
        double lon = incident.getLongitude();

        // Cálculo de deltas de bounding box para un radio de búsqueda de 50 metros
        double deltaLat = DEFAULT_MAX_RADIUS_METERS / METERS_PER_DEGREE_LATITUDE;
        double cosLat = Math.cos(Math.toRadians(lat));
        // Protección contra división por cero en polos extremos
        double metersPerDegreeLon = (Math.abs(cosLat) > 0.00001) ? METERS_PER_DEGREE_LATITUDE * Math.abs(cosLat) : METERS_PER_DEGREE_LATITUDE;
        double deltaLon = DEFAULT_MAX_RADIUS_METERS / metersPerDegreeLon;

        double minLat = lat - deltaLat;
        double maxLat = lat + deltaLat;
        double minLon = lon - deltaLon;
        double maxLon = lon + deltaLon;

        List<ActivoUrbano> candidates = activoUrbanoRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLon, maxLon);

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<AsociacionIncidenciaActivo> validAssociations = new ArrayList<>();

        for (ActivoUrbano asset : candidates) {
            if (asset.getLatitude() == null || asset.getLongitude() == null) {
                continue;
            }

            double exactDistance = calculateDistanceMeters(lat, lon, asset.getLatitude(), asset.getLongitude());

            if (exactDistance <= DEFAULT_MAX_RADIUS_METERS) {
                double confidence = calculateConfidenceScore(exactDistance);

                AsociacionIncidenciaActivo association = new AsociacionIncidenciaActivo();
                AsociacionIncidenciaActivoId id = new AsociacionIncidenciaActivoId();
                id.setIncidentId(incident.getId());
                id.setAssetId(asset.getId());

                association.setId(id);
                association.setIncident(incident);
                association.setAsset(asset);
                association.setDistanceMeters(exactDistance);
                association.setConfidenceScore(confidence);
                association.setIsConfirmed(false);

                validAssociations.add(association);
            }
        }

        // Ordenar candidatos de menor a mayor distancia geodésica
        validAssociations.sort(Comparator.comparingDouble(AsociacionIncidenciaActivo::getDistanceMeters));

        // Persistir asociaciones en la base de datos
        List<AsociacionIncidenciaActivo> savedAssociations = asociacionRepository.saveAll(validAssociations);

        return urbanAssetMapper.toDtoList(savedAssociations);
    }

    /**
     * Confirma manualmente la vinculación definitiva de un activo urbano con una incidencia de servicio.
     * <p>
     * Localiza la relación en el repositorio; si no existe, arroja una excepción HTTP 404 (RF-7).
     * En caso afirmativo, actualiza el flag {@code isConfirmed = true} y persiste el cambio.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param assetId    Identificador único del activo urbano.
     * @return DTO {@link IncidentAssetResponse} representativo de la asociación actualizada.
     * @throws ResponseStatusException Si no existe la asociación para los identificadores indicados (HTTP 404 Not Found).
     */
    @Transactional
    public IncidentAssetResponse confirmAssetAssociation(Long incidentId, Long assetId) {
        if (incidentId == null || assetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los identificadores de incidencia y activo son obligatorios");
        }

        AsociacionIncidenciaActivo association = asociacionRepository.findByIncidentIdAndAssetId(incidentId, assetId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la asociación entre la incidencia " + incidentId + " y el activo " + assetId
                ));

        association.setIsConfirmed(true);
        AsociacionIncidenciaActivo updated = asociacionRepository.save(association);

        return urbanAssetMapper.toDto(updated);
    }

    /**
     * Desvincula y elimina la asociación entre un activo urbano y una incidencia de servicio.
     * <p>
     * Comprueba la existencia de la relación antes de la supresión; si no existe, lanza HTTP 404 Not Found.
     * </p>
     *
     * @param incidentId Identificador único de la incidencia.
     * @param assetId    Identificador único del activo urbano.
     * @throws ResponseStatusException Si la relación especificada no existe en la persistencia (HTTP 404 Not Found).
     */
    @Transactional
    public void removeAssetAssociation(Long incidentId, Long assetId) {
        if (incidentId == null || assetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los identificadores de incidencia y activo son obligatorios");
        }

        asociacionRepository.findByIncidentIdAndAssetId(incidentId, assetId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe asociación entre la incidencia " + incidentId + " y el activo " + assetId + " para eliminar"
                ));

        asociacionRepository.deleteByIncidentIdAndAssetId(incidentId, assetId);
    }

    /**
     * Recupera la colección completa de activos urbanos vinculados o sugeridos para una incidencia dada,
     * ordenados por distancia geodésica ascendente.
     *
     * @param incidentId Identificador de la incidencia consultada.
     * @return Lista de DTOs {@link IncidentAssetResponse} correspondientes a los activos vinculados.
     */
    @Transactional(readOnly = true)
    public List<IncidentAssetResponse> getAssetsByIncidentId(Long incidentId) {
        if (incidentId == null) {
            return List.of();
        }

        List<AsociacionIncidenciaActivo> associations = asociacionRepository.findByIncidentId(incidentId);
        if (associations == null || associations.isEmpty()) {
            return List.of();
        }

        associations.sort(Comparator.comparingDouble(
                assoc -> assoc.getDistanceMeters() != null ? assoc.getDistanceMeters() : Double.MAX_VALUE
        ));

        return urbanAssetMapper.toDtoList(associations);
    }

    @Transactional(readOnly = true)
    public List<IncidentAssetResponseV2> getAssetsByIncidentPublicId(UUID incidentPublicId) {
        if (incidentPublicId == null) {
            return List.of();
        }

        List<AsociacionIncidenciaActivo> associations = asociacionRepository.findByIncidentPublicId(incidentPublicId);
        if (associations == null || associations.isEmpty()) {
            return List.of();
        }

        associations.sort(Comparator.comparingDouble(
                assoc -> assoc.getDistanceMeters() != null ? assoc.getDistanceMeters() : Double.MAX_VALUE
        ));

        return urbanAssetMapperV2.toDtoList(associations);
    }

    @Transactional
    public IncidentAssetResponseV2 confirmAssetAssociationV2(UUID incidentPublicId, UUID assetPublicId) {
        if (incidentPublicId == null || assetPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los identificadores de incidencia y activo son obligatorios");
        }

        AsociacionIncidenciaActivo association = asociacionRepository.findByIncidentPublicIdAndAssetPublicId(incidentPublicId, assetPublicId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la asociación entre la incidencia " + incidentPublicId + " y el activo " + assetPublicId
                ));

        association.setIsConfirmed(true);
        AsociacionIncidenciaActivo updated = asociacionRepository.save(association);

        return urbanAssetMapperV2.toDto(updated);
    }

    @Transactional
    public void removeAssetAssociationV2(UUID incidentPublicId, UUID assetPublicId) {
        if (incidentPublicId == null || assetPublicId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los identificadores de incidencia y activo son obligatorios");
        }

        asociacionRepository.findByIncidentPublicIdAndAssetPublicId(incidentPublicId, assetPublicId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe asociación entre la incidencia " + incidentPublicId + " y el activo " + assetPublicId + " para eliminar"
                ));

        asociacionRepository.deleteByIncidentPublicIdAndAssetPublicId(incidentPublicId, assetPublicId);
    }

    /**
     * Redondea un valor decimal con el número de posiciones y modo half-up especificados.
     *
     * @param value  Valor numérico a redondear.
     * @param places Número de posiciones decimales deseadas.
     * @return Valor redondeado como {@code double}.
     */
    private double round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("El número de decimales no puede ser negativo");
        }
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
