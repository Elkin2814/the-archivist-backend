package com.thearchivist.api.crawler.application.services;

import com.thearchivist.api.crawler.domain.enums.CrawlSource;
import com.thearchivist.api.crawler.domain.models.LeaderboardEntry;
import com.thearchivist.api.crawler.domain.models.Reports;
import com.thearchivist.api.crawler.domain.ports.in.DiscoverTopPlayersUseCase;
import com.thearchivist.api.crawler.domain.ports.out.CrawlQueuePort;
import com.thearchivist.api.crawler.domain.ports.out.LeaderboardClientPort;
import com.thearchivist.api.crawler.domain.ports.out.LeaderboardRepositoryPort;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;
import com.thearchivist.api.metadata.domain.ports.out.MetadataRepositoryPort;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Baja el leaderboard oficial de cada cola clasificatoria de la temporada actual, lo guarda como clasificación de
 * jugadores y mete a los mejores en la cola de rastreo (mejor puesto = mayor prioridad).
 *
 * <p>La API solo entrega el top 250 con CSR de 1800 o más por cola: no existe forma de listar jugadores de rangos
 * bajos, así que la muestra "de élite" sale de aquí.
 */
@Service
public class DiscoverTopPlayersService implements DiscoverTopPlayersUseCase {

    private static final Logger log = LoggerFactory.getLogger(DiscoverTopPlayersService.class);

    /** El leaderboard no pasa de 250 puestos por cola. */
    static final int MAX_LEADERBOARD_SIZE = 250;
    private static final int BASE_PRIORITY = 1000;

    private final MetadataRepositoryPort metadata;
    private final LeaderboardClientPort client;
    private final LeaderboardRepositoryPort leaderboards;
    private final CrawlQueuePort queue;
    private final List<UUID> configuredPlaylists;
    private final UUID configuredSeason;

    public DiscoverTopPlayersService(MetadataRepositoryPort metadata,
                                     LeaderboardClientPort client,
                                     LeaderboardRepositoryPort leaderboards,
                                     CrawlQueuePort queue,
                                     @Value("${archivist.crawl.playlist-ids:}") String playlistIds,
                                     @Value("${archivist.crawl.season-id:}") String seasonId) {
        this.metadata = metadata;
        this.client = client;
        this.leaderboards = leaderboards;
        this.queue = queue;
        this.configuredPlaylists = parseUuids(playlistIds);
        this.configuredSeason = parseUuids(seasonId).stream().findFirst().orElse(null);
    }

    /**
     * @param perPlaylist cuántos de los mejores de cada cola entran a la cola de rastreo. El leaderboard se baja
     *                    siempre completo (hasta 250 puestos): cuesta la misma llamada y así la clasificación
     *                    oficial guardada es el top real, no solo los que se van a rastrear.
     */
    @Override
    public Reports.Discovery discover(int perPlaylist, List<String> extraGamertags) {
        int toCrawl = Math.min(Math.max(perPlaylist, 1), MAX_LEADERBOARD_SIZE);
        int newPlayers = 0;

        List<CrawlQueuePort.Seed> manual = new ArrayList<>();
        for (String tag : extraGamertags == null ? List.<String>of() : extraGamertags) {
            if (tag != null && !tag.isBlank()) manual.add(new CrawlQueuePort.Seed(tag.trim(), CrawlSource.MANUAL, BASE_PRIORITY));
        }
        newPlayers += queue.enqueue(manual);

        Optional<UUID> season = currentSeason();
        List<UUID> playlists = rankedPlaylists();
        if (season.isEmpty() || playlists.isEmpty()) {
            log.warn("Descubrimiento omitido: sin temporada actual ({}) o sin colas clasificatorias ({}) en la Metadata",
                    season.isPresent(), playlists.size());
            return new Reports.Discovery(0, 0, newPlayers, false);
        }

        int queried = 0;
        int entries = 0;
        for (UUID playlist : playlists) {
            List<LeaderboardEntry> top;
            try {
                top = client.fetchTopPlayers(season.get(), playlist, MAX_LEADERBOARD_SIZE);
            } catch (HaloApiUnavailableException e) {
                log.warn("Descubrimiento detenido: {}", e.getMessage());
                return new Reports.Discovery(queried, entries, newPlayers, true);
            }
            queried++;
            if (top.isEmpty()) continue;
            leaderboards.replaceEntries(season.get(), playlist, top);
            entries += top.size();
            newPlayers += queue.enqueue(top.stream()
                    .sorted(Comparator.comparingInt(LeaderboardEntry::rank))
                    .limit(toCrawl)
                    .map(e -> new CrawlQueuePort.Seed(e.gamertag(), CrawlSource.LEADERBOARD,
                            Math.max(BASE_PRIORITY - e.rank(), 1)))
                    .toList());
        }
        log.info("Descubrimiento: {} colas, {} puestos guardados, {} jugadores nuevos en la cola", queried, entries, newPlayers);
        return new Reports.Discovery(queried, entries, newPlayers, false);
    }

    /** Temporada que ya empezó y es la más reciente; si la Metadata no trae fechas, la última conocida. */
    Optional<UUID> currentSeason() {
        if (configuredSeason != null) return Optional.of(configuredSeason);
        Instant now = Instant.now();
        List<SeasonInfo> seasons = metadata.findSeasons();
        Optional<SeasonInfo> started = seasons.stream()
                .filter(s -> s.startDate() != null && !s.startDate().isAfter(now))
                .max(Comparator.comparing(SeasonInfo::startDate));
        if (started.isPresent()) return started.map(SeasonInfo::id);
        return seasons.stream()
                .filter(s -> s.startDate() != null)
                .max(Comparator.comparing(SeasonInfo::startDate))
                .map(SeasonInfo::id);
    }

    List<UUID> rankedPlaylists() {
        if (!configuredPlaylists.isEmpty()) return configuredPlaylists;
        return metadata.findPlaylists().stream()
                .filter(p -> !Boolean.TRUE.equals(p.hidden()))
                .filter(DiscoverTopPlayersService::isRanked)
                .map(PlaylistInfo::id)
                .toList();
    }

    /**
     * En la Metadata real las colas clasificatorias llegan con {@code code = null} y {@code statsClassification}
     * igual al modo ("Blitz", "War"…), no "ranked": lo único que las distingue es el nombre ("RANKED 3v3 WAR").
     * Por eso se mira la clasificación, el código y el nombre. Las colas sociales ("3v3 WAR") no lo contienen.
     */
    static boolean isRanked(PlaylistInfo playlist) {
        if ("ranked".equalsIgnoreCase(playlist.statsClassification())) return true;
        return containsRanked(playlist.code()) || containsRanked(playlist.name());
    }

    private static boolean containsRanked(String text) {
        return text != null && text.toUpperCase().contains("RANKED");
    }

    private static List<UUID> parseUuids(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
    }
}
