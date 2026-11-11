package com.thearchivist.api.config;

import com.thearchivist.api.player.domain.exceptions.HaloApiTransientException;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Puerta única de salida hacia la Halo API. Todas las llamadas (historial, Match Result, leaderboard y
 * Metadata) comparten la misma llave, y la llave tiene un solo límite: 10 llamadas cada 10 segundos
 * (producto "Developer Access" del portal). Por eso el ritmo se controla en un solo lugar y no en cada
 * proceso por separado:
 * <ul>
 *   <li>{@link #acquire()} separa las llamadas al menos {@code halo.api.min-interval-ms} (1,2 s por defecto,
 *       por debajo del tope). Es justo: quien llega primero pasa primero, así una consulta de un usuario
 *       no se queda esperando detrás de todo el rastreo.</li>
 *   <li>{@link #unavailable(int, RestClientResponseException)} traduce los errores HTTP y, ante un 429,
 *       frena a todos los procesos durante el tiempo que pida la API (cabecera Retry-After).</li>
 * </ul>
 */
@Component
public class HaloApiGate {

    private static final Logger log = LoggerFactory.getLogger(HaloApiGate.class);

    private static final Duration DEFAULT_BACKOFF = Duration.ofSeconds(15);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(2);

    private final long minIntervalNanos;
    private final ReentrantLock lock = new ReentrantLock(true);
    private long nextAllowedNanos = System.nanoTime();

    public HaloApiGate(@Value("${halo.api.min-interval-ms:1200}") long minIntervalMs) {
        this.minIntervalNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0, minIntervalMs));
    }

    /** Bloquea hasta que toque hacer la siguiente llamada a la Halo API. */
    public void acquire() {
        lock.lock();
        try {
            long wait = nextAllowedNanos - System.nanoTime();
            if (wait > 0) {
                TimeUnit.NANOSECONDS.sleep(wait);
            }
            nextAllowedNanos = Math.max(System.nanoTime(), nextAllowedNanos) + minIntervalNanos;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HaloApiUnavailableException("Espera interrumpida antes de llamar a la Halo API", e);
        } finally {
            lock.unlock();
        }
    }

    /** Impide nuevas llamadas durante {@code duration} (por ejemplo tras un 429). */
    public void backoff(Duration duration) {
        Duration bounded = duration.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : duration;
        lock.lock();
        try {
            nextAllowedNanos = Math.max(nextAllowedNanos, System.nanoTime() + bounded.toNanos());
        } finally {
            lock.unlock();
        }
        log.warn("Halo API: nuevas llamadas en pausa {} s", bounded.toSeconds());
    }

    /**
     * Traduce un error HTTP inesperado de la Halo API (todo menos 404, que cada llamada interpreta a su manera).
     * Un 429 activa la pausa global para que ningún proceso siga insistiendo.
     */
    public HaloApiUnavailableException unavailable(int status, RestClientResponseException e) {
        return switch (status) {
            case 401, 403 -> new HaloApiUnavailableException(
                    "La Halo API rechazó la llave (HTTP " + status + "). Revisa HALO_API_KEY", e);
            case 429 -> {
                backoff(retryAfter(e));
                yield new HaloApiUnavailableException("Se superó el límite de llamadas de la Halo API (HTTP 429)", e);
            }
            case 500, 502, 503, 504 -> new HaloApiTransientException("La Halo API respondió con HTTP " + status, e);
            default -> new HaloApiUnavailableException("La Halo API respondió con HTTP " + status, e);
        };
    }

    static Duration retryAfter(RestClientResponseException e) {
        String header = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
        if (header != null) {
            try {
                long seconds = Long.parseLong(header.trim());
                if (seconds > 0) return Duration.ofSeconds(seconds);
            } catch (NumberFormatException ignored) {
                // Retry-After también puede ser una fecha; se usa la pausa por defecto.
            }
        }
        return DEFAULT_BACKOFF;
    }
}
