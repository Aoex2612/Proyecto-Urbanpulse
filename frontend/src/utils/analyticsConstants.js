/**
 * @fileoverview Constantes y catálogos de referencia para el módulo de Analítica Urbana.
 * Incluye la delimitación oficial de distritos de Málaga, barrios asociados y presets temporales.
 */

/**
 * Distritos administrativos oficiales del término municipal de Málaga.
 * @type {ReadonlyArray<string>}
 */
export const MALAGA_DISTRICTS = Object.freeze([
  'Centro',
  'Este',
  'Ciudad Jardín',
  'Bailén-Miraflores',
  'Palma-Palmilla',
  'Cruz de Humilladero',
  'Carretera de Cádiz',
  'Churriana',
  'Campanillas',
  'Puerto de la Torre',
  'Teatinos-Universidad',
]);

/**
 * Catálogo referencial de barrios representativos por distrito municipal de Málaga.
 * @type {Readonly<Record<string, string[]>>}
 */
export const MALAGA_NEIGHBORHOODS_BY_DISTRICT = Object.freeze({
  Centro: [
    'Centro Histórico',
    'La Merced',
    'La Goleta',
    'El Molinillo',
    'Capuchinos',
    'Lagunillas',
    'Perchel Norte',
    'Perchel Sur',
    'La Trinidad',
  ],
  Este: [
    'El Palo',
    'Pedregalejo',
    'El Limonar',
    'Miraflores del Palo',
    'Cerrado de Calderón',
    'Pinares de San Antón',
    'Olías',
  ],
  'Ciudad Jardín': [
    'Ciudad Jardín',
    'Los Naranjos',
    'Alegría de la Huerta',
    'Mangas Verdes',
    'Parque del Sur',
  ],
  'Bailén-Miraflores': [
    'Gamarra',
    'Bailén',
    'Miraflores de los Ángeles',
    'Nueva Málaga',
    'Los Castillejos',
    'San Alberto',
  ],
  'Palma-Palmilla': [
    'La Palma',
    'La Palmilla',
    'Virreina',
    '26 de Febrero',
    'Huerta Central',
  ],
  'Cruz de Humilladero': [
    'La Unión',
    'Cruz de Humilladero',
    'Los Tilos',
    'San Rafael',
    'Polígono Alameda',
    'Portada Alta',
  ],
  'Carretera de Cádiz': [
    'Huelin',
    'La Paz',
    'La Luz',
    'Las Delicias',
    'Parque Mediterráneo',
    'San Andrés',
    'Tabacalera',
    'El Torcal',
  ],
  Churriana: [
    'Churriana',
    'Guadalmar',
    'La Colina',
    'San Julián',
    'El Pizarrillo',
  ],
  Campanillas: [
    'Campanillas',
    'Santa Rosalía',
    'Maqueda',
    'El Tarajal',
  ],
  'Puerto de la Torre': [
    'Puerto de la Torre',
    'Los Morales',
    'Salinas',
    'Fuente Alegre',
    'Torre Atalaya',
  ],
  'Teatinos-Universidad': [
    'Teatinos',
    'Universidad',
    'El Cónsul',
    'Romeral',
    'Colonia Santa Inés',
    'Soliva',
  ],
});

/**
 * Lista de opciones de presets temporales soportados.
 * @type {ReadonlyArray<{id: string, label: string}>}
 */
export const PRESET_OPTIONS = Object.freeze([
  { id: 'TODAY', label: 'Hoy' },
  { id: 'LAST_7_DAYS', label: 'Últimos 7 días' },
  { id: 'LAST_30_DAYS', label: 'Últimos 30 días' },
  { id: 'LAST_3_MONTHS', label: 'Últimos 3 meses' },
  { id: 'LAST_6_MONTHS', label: 'Últimos 6 meses' },
  { id: 'LAST_YEAR', label: 'Último año' },
  { id: 'SPECIFIC_YEAR', label: 'Año específico' },
]);
