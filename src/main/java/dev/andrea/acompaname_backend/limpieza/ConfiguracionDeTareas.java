package dev.andrea.acompaname_backend.limpieza;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Activa las tareas programadas (@Scheduled). Sin esto, Spring las ignora sin avisar.
@Configuration
@EnableScheduling
public class ConfiguracionDeTareas {
}
