-- Retira únicamente los índices añadidos para el Experimento 4.
DROP INDEX IF EXISTS idx_incidents_exp4_search;
DROP INDEX IF EXISTS idx_incidents_exp4_page;
ANALYZE incidents;
