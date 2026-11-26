-- Elimina exclusivamente las incidencias sintéticas reservadas al Experimento 4.
BEGIN;
DELETE FROM incidents
WHERE title LIKE 'EXP4-%';
ANALYZE incidents;
COMMIT;
