package org.bulbaseros.model;

/**
 * Enumerado que define los roles de usuario disponibles en la plataforma UrbanPulse.
 * <p>
 * Mapea los valores permitidos del tipo enumerado {@code user_role} de la base de datos PostgreSQL.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public enum UserRole {

    /**
     * Ciudadano registrado en la plataforma; puede reportar incidencias urbanas y consultar su estado.
     */
    CITIZEN,

    /**
     * Operador municipal del centro de control; realiza el triaje inicial y validación de reportes.
     */
    OPERATOR,

    /**
     * Técnico especializado de campo; asignado a cuadrillas para resolver incidencias específicas.
     */
    TECHNICIAN,

    /**
     * Administrador de la plataforma con privilegios globales de configuración y gestión de usuarios.
     */
    ADMIN,

    /**
     * Analista de datos urbanos con permisos de visualización de métricas e informes agregados.
     */
    ANALYST
}
