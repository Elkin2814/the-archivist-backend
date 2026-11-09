package com.thearchivist.api.player.infrastructure.controllers;

import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PlayerNotFoundException.class)
    public ProblemDetail handlePlayerNotFound(PlayerNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Jugador no encontrado");
        return problem;
    }

    @ExceptionHandler(HaloApiUnavailableException.class)
    public ProblemDetail handleHaloUnavailable(HaloApiUnavailableException ex) {
        // El detalle técnico (llave inválida, límite, etc.) queda en el log, no en la respuesta.
        log.error("Halo API no disponible: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de datos de Halo no está disponible por ahora. Intenta de nuevo más tarde.");
        problem.setTitle("Halo API no disponible");
        return problem;
    }
}
