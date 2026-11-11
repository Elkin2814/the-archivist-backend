package com.thearchivist.api.stats.domain.ports.out;

import com.thearchivist.api.stats.domain.enums.BreakdownDimension;
import com.thearchivist.api.stats.domain.models.BreakdownRow;
import com.thearchivist.api.stats.domain.models.LeaderRow;
import com.thearchivist.api.stats.domain.models.PairRow;
import com.thearchivist.api.stats.domain.models.StatsFilter;
import com.thearchivist.api.stats.domain.models.StatsSample;

import java.util.List;

/** Consultas de estadísticas sobre la capa de análisis (solo partidas válidas, sin gamertags). */
public interface StatsRepositoryPort {

    StatsSample sample(StatsFilter filter);

    /** Partidas y victorias por líder. El filtro de rango se aplica al jugador que se mide. */
    List<LeaderRow> leaderRows(StatsFilter filter);

    /** Líder {@code leaderId} contra cada líder del equipo contrario (cada partida cuenta una vez por pareja). */
    List<PairRow> matchups(StatsFilter filter, int leaderId);

    /** Líder {@code leaderId} junto a cada líder de su propio equipo. */
    List<PairRow> synergies(StatsFilter filter, int leaderId);

    /** Partidas y victorias del líder {@code leaderId}, separadas por cada valor del eje {@code dimension}. */
    List<BreakdownRow> breakdown(StatsFilter filter, int leaderId, BreakdownDimension dimension);
}
