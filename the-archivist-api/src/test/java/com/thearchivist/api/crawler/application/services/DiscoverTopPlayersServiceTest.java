package com.thearchivist.api.crawler.application.services;

import com.thearchivist.api.crawler.domain.enums.CrawlSource;
import com.thearchivist.api.crawler.domain.models.LeaderboardEntry;
import com.thearchivist.api.crawler.domain.models.Reports;
import com.thearchivist.api.crawler.domain.ports.out.CrawlQueuePort;
import com.thearchivist.api.crawler.domain.ports.out.LeaderboardClientPort;
import com.thearchivist.api.crawler.domain.ports.out.LeaderboardRepositoryPort;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;
import com.thearchivist.api.metadata.domain.ports.out.MetadataRepositoryPort;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoverTopPlayersServiceTest {

    private static final UUID OLD_SEASON = UUID.randomUUID();
    private static final UUID CURRENT_SEASON = UUID.randomUUID();
    private static final UUID FUTURE_SEASON = UUID.randomUUID();
    private static final UUID RANKED = UUID.randomUUID();
    private static final UUID RANKED_BY_CODE = UUID.randomUUID();
    private static final UUID SOCIAL = UUID.randomUUID();
    private static final UUID HIDDEN_RANKED = UUID.randomUUID();

    private final List<CrawlQueuePort.Seed> enqueued = new ArrayList<>();
    private final List<UUID> savedBoards = new ArrayList<>();
    private final List<UUID> queriedSeasons = new ArrayList<>();
    private boolean apiDown;

    private DiscoverTopPlayersService service(String playlistIds, String seasonId) {
        Instant now = Instant.now();
        MetadataRepositoryPort metadata = (MetadataRepositoryPort) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{MetadataRepositoryPort.class}, (p, m, a) -> switch (m.getName()) {
                    case "findSeasons" -> List.of(
                            new SeasonInfo(OLD_SEASON, "S1", now.minus(400, ChronoUnit.DAYS)),
                            new SeasonInfo(CURRENT_SEASON, "S2", now.minus(30, ChronoUnit.DAYS)),
                            new SeasonInfo(FUTURE_SEASON, "S3", now.plus(60, ChronoUnit.DAYS)));
                    case "findPlaylists" -> List.of(
                            new PlaylistInfo(RANKED, "Ranked 3v3", "CP_RANKED_3v3_WAR", "ranked", true, false),
                            new PlaylistInfo(RANKED_BY_CODE, "Ranked 1v1", "CP_RANKED_1V1", null, false, false),
                            new PlaylistInfo(SOCIAL, "Social", "CP_SOCIAL", "social", true, false),
                            new PlaylistInfo(HIDDEN_RANKED, "Hidden", "CP_RANKED_HIDDEN", "ranked", true, true));
                    default -> null;
                });
        LeaderboardClientPort client = (seasonId1, playlistId, count) -> {
            if (apiDown) throw new HaloApiUnavailableException("429", null);
            queriedSeasons.add(seasonId1);
            return List.of(new LeaderboardEntry(1, "First", 7, 1, 2100, 50),
                    new LeaderboardEntry(2, "Second", 7, 1, 2050, 10));
        };
        LeaderboardRepositoryPort boards = (LeaderboardRepositoryPort) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{LeaderboardRepositoryPort.class}, (p, m, a) -> {
                    if (m.getName().equals("replaceEntries")) savedBoards.add((UUID) a[1]);
                    return null;
                });
        CrawlQueuePort queue = (CrawlQueuePort) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{CrawlQueuePort.class}, (p, m, a) -> {
                    if (!m.getName().equals("enqueue")) return null;
                    List<CrawlQueuePort.Seed> seeds = (List<CrawlQueuePort.Seed>) a[0];
                    enqueued.addAll(seeds);
                    return seeds.size();
                });
        return new DiscoverTopPlayersService(metadata, client, boards, queue, playlistIds, seasonId);
    }

    @Test
    void rankedDetectionUsesStatsClassificationOrCode() {
        assertTrue(DiscoverTopPlayersService.isRanked(new PlaylistInfo(RANKED, "n", "X", "ranked", true, false)));
        assertTrue(DiscoverTopPlayersService.isRanked(new PlaylistInfo(RANKED, "n", "cp_ranked_2v2", null, true, false)));
        // Metadata real: code nulo y statsClassification = modo; solo el nombre dice que es clasificatoria.
        assertTrue(DiscoverTopPlayersService.isRanked(new PlaylistInfo(RANKED, "RANKED 3v3 WAR", null, "War", true, false)));
        assertTrue(DiscoverTopPlayersService.isRanked(new PlaylistInfo(RANKED, "Ranked 1v1 Blitz", null, "Blitz", false, false)));
        assertFalse(DiscoverTopPlayersService.isRanked(new PlaylistInfo(SOCIAL, "3v3 WAR", null, "War", true, false)));
        assertFalse(DiscoverTopPlayersService.isRanked(new PlaylistInfo(SOCIAL, "n", "CP_SOCIAL", "social", true, false)));
        assertFalse(DiscoverTopPlayersService.isRanked(new PlaylistInfo(SOCIAL, "n", null, null, null, null)));
    }

    @Test
    void usesCurrentSeasonAndOnlyVisibleRankedPlaylists() {
        Reports.Discovery r = service("", "").discover(250, List.of());
        assertEquals(2, r.playlists());
        assertEquals(4, r.entries());
        assertEquals(List.of(CURRENT_SEASON, CURRENT_SEASON), queriedSeasons);
        assertTrue(savedBoards.contains(RANKED) && savedBoards.contains(RANKED_BY_CODE));
        assertFalse(savedBoards.contains(SOCIAL) || savedBoards.contains(HIDDEN_RANKED));
    }

    @Test
    void leaderboardIsStoredCompleteButOnlyTheBestAreCrawled() {
        Reports.Discovery r = service("", "").discover(1, List.of());
        assertEquals(4, r.entries());      // 2 colas x 2 puestos guardados, aunque solo se rastree 1 por cola
        assertEquals(2, enqueued.size());  // el primero de cada cola
        assertTrue(enqueued.stream().allMatch(s -> s.gamertag().equals("First")));
    }

    @Test
    void betterRankMeansHigherPriorityAndManualSeedsAreAdded() {
        service("", "").discover(250, List.of("  Manual Tag ", " "));
        CrawlQueuePort.Seed manual = enqueued.get(0);
        assertEquals("Manual Tag", manual.gamertag());
        assertEquals(CrawlSource.MANUAL, manual.source());
        CrawlQueuePort.Seed first = enqueued.stream().filter(s -> s.gamertag().equals("First")).findFirst().orElseThrow();
        CrawlQueuePort.Seed second = enqueued.stream().filter(s -> s.gamertag().equals("Second")).findFirst().orElseThrow();
        assertTrue(first.priority() > second.priority());
        assertEquals(CrawlSource.LEADERBOARD, first.source());
    }

    @Test
    void configuredPlaylistAndSeasonOverrideMetadata() {
        UUID custom = UUID.randomUUID();
        service(custom.toString(), FUTURE_SEASON.toString()).discover(10, List.of());
        assertEquals(List.of(FUTURE_SEASON), queriedSeasons);
        assertEquals(List.of(custom), savedBoards);
    }

    @Test
    void apiOutageStopsDiscoveryAndReportsIt() {
        DiscoverTopPlayersService s = service("", "");
        apiDown = true;
        Reports.Discovery r = s.discover(250, List.of());
        assertTrue(r.stoppedByApi());
        assertEquals(0, r.entries());
        assertTrue(savedBoards.isEmpty());
    }
}
