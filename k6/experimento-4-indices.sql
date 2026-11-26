-- Índices adicionales reversibles para comparar la configuración base y optimizada.
CREATE INDEX IF NOT EXISTS idx_incidents_exp4_search
    ON incidents (lower(category), lower(btrim(district)), created_at DESC);
CREATE INDEX IF NOT EXISTS idx_incidents_exp4_page
    ON incidents (citizen_id, created_at DESC);
ANALYZE incidents;
