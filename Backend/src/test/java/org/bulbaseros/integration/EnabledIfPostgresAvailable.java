package org.bulbaseros.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotacion para marcar tests de integracion contra base de datos PostgreSQL real.
 * <p>
 * Aplica la etiqueta JUnit {@code @Tag("postgres-integration")} y la extension
 * {@link ConditionalOnPostgresCondition}, haciendo que el test se ejecute solo si
 * PostgreSQL se encuentra encendido, omitiendose limpiamente si esta apagado.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("postgres-integration")
@ExtendWith(ConditionalOnPostgresCondition.class)
public @interface EnabledIfPostgresAvailable {
}
