package org.bulbaseros.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración web de Spring MVC para UrbanPulse.
 * <p>
 * Configura los manejadores de recursos estáticos para servir los archivos multimedia
 * adjuntos almacenados en el directorio físico del servidor.
 * </p>
 *
 * @author UrbanPulse Team
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Directorio físico donde se almacenan los archivos multimedia adjuntos.
     */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Registra el manejador de recursos estáticos para servir archivos multimedia subidos.
     * Mapea las peticiones {@code /uploads/**} hacia la ubicación física en disco.
     *
     * @param registry Registro de manejadores de recursos de Spring MVC.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadUri = uploadPath.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri);
    }
}
