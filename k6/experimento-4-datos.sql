-- Crea incidencias sintéticas para el Experimento 4.
-- cantidad es el tamaño final deseado de la tabla incidents.
\set ON_ERROR_STOP on

SELECT COUNT(*) > :cantidad AS requested_size_below_existing
FROM incidents
\gset
\if :requested_size_below_existing
\echo 'La tabla incidents ya supera el tamaño solicitado; no se eliminarán datos existentes.'
\quit 1
\endif

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'citizen@urbanpulse.es' AND role = 'CITIZEN') THEN
        RAISE EXCEPTION 'No existe citizen@urbanpulse.es; comprueba que la base de datos de desarrollo está inicializada';
    END IF;
    IF EXISTS (SELECT 1 FROM incidents WHERE title LIKE 'EXP4-%') THEN
        RAISE EXCEPTION 'Ya existen filas EXP4; ejecuta primero experimento-4-limpiar.sql';
    END IF;
END;
$$;

INSERT INTO incidents (
    public_id, title, description, category, latitude, longitude, address,
    district, neighborhood, status, priority, citizen_id, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    'EXP4-' || lpad(generated.i::text, 12, '0'),
    'Incidencia sintética del experimento de volumen de datos.',
    (ARRAY['ALUMBRADO', 'LIMPIEZA', 'VIA_PUBLICA', 'PARQUES', 'AGUA'])
        [1 + (generated.i % 5)],
    36.7213 + (generated.i % 1000) * 0.000001,
    -4.4214 - (generated.i % 1000) * 0.000001,
    'Ubicación sintética de prueba',
    (ARRAY['Centro', 'Este', 'Carretera de Cádiz', 'Cruz de Humilladero'])
        [1 + (generated.i % 4)],
    'Barrio de prueba',
    'REPORTED',
    'MEDIUM',
    citizen.id,
    CURRENT_TIMESTAMP
        - ((generated.i % 29) * INTERVAL '1 day')
        - ((generated.i % 86400) * INTERVAL '1 second'),
    CURRENT_TIMESTAMP
FROM generate_series(
    1,
    :cantidad - (SELECT COUNT(*) FROM incidents)
) AS generated(i)
CROSS JOIN (
    SELECT id FROM users WHERE email = 'citizen@urbanpulse.es' AND role = 'CITIZEN'
) AS citizen;

ANALYZE incidents;

SELECT COUNT(*) AS filas_totales
FROM incidents;

SELECT COUNT(*) AS incidencias_sinteticas
FROM incidents
WHERE title LIKE 'EXP4-%';
