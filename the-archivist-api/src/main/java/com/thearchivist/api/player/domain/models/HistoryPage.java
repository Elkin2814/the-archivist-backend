package com.thearchivist.api.player.domain.models;

import java.util.List;

/**
 * Una página del historial de la Halo API.
 *
 * @param matches  partidas que se pudieron interpretar
 * @param received partidas que la API devolvió (puede ser mayor que {@code matches.size()} si alguna venía
 *                 incompleta); sirve para saber si hay más páginas: si llegan menos de las pedidas, era la última
 */
public record HistoryPage(List<PlayerMatch> matches, int received) {

    public HistoryPage {
        matches = matches == null ? List.of() : List.copyOf(matches);
    }

    /** True si la API devolvió menos partidas de las pedidas: no hay páginas más antiguas. */
    public boolean isLast(int requested) {
        return received < requested;
    }
}
