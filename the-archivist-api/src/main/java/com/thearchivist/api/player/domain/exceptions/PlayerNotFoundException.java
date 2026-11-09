package com.thearchivist.api.player.domain.exceptions;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(String gamertag) {
        super("No se encontró al jugador '" + gamertag + "' en Halo Wars 2");
    }
}
