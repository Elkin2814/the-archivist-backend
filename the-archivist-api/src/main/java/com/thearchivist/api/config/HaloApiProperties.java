package com.thearchivist.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de la Halo API (prefijo "halo.api" en application.properties).
 * La llave nunca va en el código: viene de la variable de entorno HALO_API_KEY.
 */
@ConfigurationProperties(prefix = "halo.api")
public record HaloApiProperties(String baseUrl, String key) {
}
