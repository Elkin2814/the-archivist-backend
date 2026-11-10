package com.thearchivist.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración inicial para la lllave
 */
@ConfigurationProperties(prefix = "halo.api")
public record HaloApiProperties(String baseUrl, String key) {
}
