package org.bulbaseros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal que inicializa y ejecuta la aplicación Spring Boot UrbanPulse.
 *
 * <p>Esta clase contiene el método de entrada principal ({@link #main(String[])})
 * para arrancar el contexto de Spring.</p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@SpringBootApplication
public class App {

    /**
     * Constructor por defecto de la clase principal.
     */
    public App() {
    }

    /**
     * Punto de entrada principal para la ejecución de la aplicación.
     *
     * @param args argumentos de línea de comandos suministrados al iniciar la aplicación.
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
