package com.thearchivist.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas (@Scheduled), como la limpieza de la caché de jugadores. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
