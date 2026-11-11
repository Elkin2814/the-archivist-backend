package com.thearchivist.api.player.domain.exceptions;

/**
 * Error pasajero del servidor de la Halo API (HTTP 500, 502, 503 o 504). A diferencia de un 429 o una llave inválida,
 * puede afectar solo a una petición concreta (por ejemplo, el historial de un jugador): quien llama puede anotar el
 * intento y seguir con otra cosa en vez de detenerse del todo.
 */
public class HaloApiTransientException extends HaloApiUnavailableException {

    public HaloApiTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
