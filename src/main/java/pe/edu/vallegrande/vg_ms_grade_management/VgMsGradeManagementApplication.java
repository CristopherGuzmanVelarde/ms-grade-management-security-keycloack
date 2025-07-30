package pe.edu.vallegrande.vg_ms_grade_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.reactive.config.EnableWebFlux;
import pe.edu.vallegrande.vg_ms_grade_management.infrastructure.config.SecurityProperties;

/**
 * Clase principal de la aplicación de gestión de calificaciones
 */
@SpringBootApplication
@EnableWebFlux
@EnableConfigurationProperties(SecurityProperties.class)
public class VgMsGradeManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(VgMsGradeManagementApplication.class, args);
	}
}
