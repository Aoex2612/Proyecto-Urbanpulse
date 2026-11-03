package org.bulbaseros.integration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Condicion de ejecucion de JUnit 5 que comprueba si la base de datos PostgreSQL local
 * (puerto 5432) se encuentra alcanzable antes de ejecutar los tests de integracion.
 * <p>
 * Si PostgreSQL esta desconectado, desactiva (ignora/omite) automaticamente el test
 * sin marcar fallo en la suite ni degradar el porcentaje de exito de validacion.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
public class ConditionalOnPostgresCondition implements ExecutionCondition {

    private static final String HOST = "localhost";
    private static final int PORT = 5432;
    private static final int TIMEOUT_MS = 1500;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT_MS);
            return ConditionEvaluationResult.enabled("PostgreSQL esta accesible en localhost:5432. Ejecutando test de integracion.");
        } catch (IOException e) {
            return ConditionEvaluationResult.disabled("PostgreSQL no se encuentra alcanzable en localhost:5432 ("
                    + e.getMessage() + "). Omitiendo test de integracion sin penalizar la validacion.");
        }
    }
}
