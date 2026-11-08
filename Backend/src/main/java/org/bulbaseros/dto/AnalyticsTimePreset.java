package org.bulbaseros.dto;

/**
 * Enumeración que define los rangos temporales preconfigurados para el filtrado analítico.
 * <p>
 * Facilita a los analistas, administradores y operadores la selección rápida y estandarizada
 * de periodos de tiempo en el panel de analítica urbana, en cumplimiento de los requisitos
 * funcionales RF-8 y RF-9.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public enum AnalyticsTimePreset {

    /**
     * Periodo correspondiente al día en curso (desde las 00:00:00 del día actual).
     */
    TODAY,

    /**
     * Periodo correspondiente a los últimos 7 días naturales anteriores a la fecha actual.
     */
    LAST_7_DAYS,

    /**
     * Periodo correspondiente a los últimos 30 días naturales anteriores a la fecha actual.
     */
    LAST_30_DAYS,

    /**
     * Periodo correspondiente a los últimos 3 meses naturales anteriores a la fecha actual.
     */
    LAST_3_MONTHS,

    /**
     * Periodo correspondiente a los últimos 6 meses naturales anteriores a la fecha actual.
     */
    LAST_6_MONTHS,

    /**
     * Periodo correspondiente al último año natural completo (últimos 365 días).
     */
    LAST_YEAR,

    /**
     * Periodo correspondiente a un año calendario histórico específico indicado explícitamente.
     */
    SPECIFIC_YEAR
}
