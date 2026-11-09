package com.thearchivist.api.player.domain.exceptions;

/**
 * La Halo API no se pudo usar: caída, límite de llamadas, llave inválida o sin configurar.
 */
public class HaloApiUnavailableException extends RuntimeException {

    public HaloApiUnavailableException(String message) {
        super(message);
    }

    public HaloApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
