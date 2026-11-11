package com.thearchivist.api.player.application.services;

import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import com.thearchivist.api.player.domain.models.Player;
import com.thearchivist.api.player.domain.models.PlayerClassification;
import com.thearchivist.api.player.domain.models.PlayerClassification.Activity;
import com.thearchivist.api.player.domain.models.PlayerClassification.PlaylistClassification;
import com.thearchivist.api.player.domain.models.PlayerClassification.Standing;
import com.thearchivist.api.player.domain.ports.in.GetPlayerClassificationUseCase;
import com.thearchivist.api.player.domain.ports.in.GetPlayerHistoryUseCase;
import com.thearchivist.api.player.domain.ports.out.PlayerClassificationPort;
import com.thearchivist.api.player.domain.ports.out.PlayerClassificationPort.PlaylistRecord;
import com.thearchivist.api.player.domain.ports.out.PlayerRepositoryPort;
import com.thearchivist.api.stats.domain.models.WinRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class GetPlayerClassificationService implements GetPlayerClassificationUseCase {

    /** Sin partidas desde hace más de esto, el rango guardado puede estar viejo: el jugador se marca INACTIVE. */
    static final Duration ACTIVE_WINDOW = Duration.ofDays(30);

    /** El percentil de MMR solo se ofrece con una muestra mínima; con pocos participantes no significa nada. */
    static final long MIN_PERCENTILE_SAMPLE = 200;

    private final GetPlayerHistoryUseCase history;
    private final PlayerRepositoryPort players;
    private final PlayerClassificationPort classification;
    private final Duration percentileWindow;

    public GetPlayerClassificationService(GetPlayerHistoryUseCase history,
                                          PlayerRepositoryPort players,
                                          PlayerClassificationPort classification,
                                          @Value("${archivist.classification.percentile-window-days:90}") long percentileDays) {
        this.history = history;
        this.players = players;
        this.classification = classification;
        this.percentileWindow = Duration.ofDays(percentileDays);
    }

    @Override
    public PlayerClassification execute(String gamertag) {
        String tag = gamertag.trim();
        // Misma sincronización que el resumen: si el jugador no está o está desactualizado, se trae de la API.
        history.execute(tag, 0, 1);
        Player player = players.findByGamertag(tag).orElseThrow(() -> new PlayerNotFoundException(tag));

        Instant now = Instant.now();
        List<PlaylistClassification> result = new ArrayList<>();
        for (PlaylistRecord record : classification.findPlaylistRecords(tag)) {
            result.add(classify(tag, record, now));
        }
        return new PlayerClassification(player, result);
    }

    private PlaylistClassification classify(String tag, PlaylistRecord record, Instant now) {
        WinRate winRate = new WinRate(record.matches(), record.wins());
        Standing standing = withPercentile(record, now);
        Activity activity = record.lastMatch() != null && record.lastMatch().isAfter(now.minus(ACTIVE_WINDOW))
                ? Activity.ACTIVE : Activity.INACTIVE;
        boolean provisional = standing != null && standing.measurementMatchesRemaining() != null
                && standing.measurementMatchesRemaining() > 0;
        Integer leaderboardRank = classification.findLeaderboardRank(tag, record.playlistId()).orElse(null);
        return new PlaylistClassification(record.playlistId(), record.playlistName(), record.matches(),
                record.wins(), record.matches() - record.wins(), winRate, winRate.confidence(), record.lastMatch(),
                activity, provisional, leaderboardRank, standing);
    }

    /** Añade el percentil de MMR al rango si hay MMR y muestra suficiente. */
    private Standing withPercentile(PlaylistRecord record, Instant now) {
        Standing s = record.standing();
        if (s == null || s.mmr() == null) return s;
        return classification.findMmrPercentile(record.playlistId(), s.mmr(), now.minus(percentileWindow))
                .filter(p -> p.sample() >= MIN_PERCENTILE_SAMPLE)
                .map(p -> new Standing(s.designation(), s.designationName(), s.tier(), s.csr(),
                        s.percentToNextTier(), s.measurementMatchesRemaining(), s.mmr(), s.mmrVariance(),
                        Math.round(p.value() * 10.0) / 10.0, p.sample()))
                .orElse(s);
    }
}
