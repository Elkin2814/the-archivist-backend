package com.thearchivist.api.leader.domain.models;

import com.thearchivist.api.leader.domain.enums.Difficulty;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dificultad de cada líder. La Halo API no la entrega y no se puede deducir de las partidas sin una muestra de
 * todos los rangos, así que es un dato editorial: se rellena aquí a mano (LOW, MEDIUM o HIGH) y la web lo muestra
 * en cuanto exista. Mientras un líder no tenga valor, la API responde {@code null} y la web dice "Por definir".
 *
 * <p>Ejemplo: {@code PROFILES.put(LeaderCatalog.CUTTER, Difficulty.LOW);}
 */
public final class LeaderDifficulty {

    private static final Map<LeaderCatalog, Difficulty> PROFILES = new EnumMap<>(LeaderCatalog.class);

    private LeaderDifficulty() {}

    public static Optional<Difficulty> of(LeaderCatalog leader) {
        return Optional.ofNullable(PROFILES.get(leader));
    }
}
