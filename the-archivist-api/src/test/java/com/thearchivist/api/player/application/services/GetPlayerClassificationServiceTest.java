package com.thearchivist.api.player.application.services;

import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import com.thearchivist.api.player.domain.models.Player;
import com.thearchivist.api.player.domain.models.PlayerClassification;
import com.thearchivist.api.player.domain.models.PlayerClassification.Activity;
import com.thearchivist.api.player.domain.models.PlayerClassification.PlaylistClassification;
import com.thearchivist.api.player.domain.models.PlayerClassification.Standing;
import com.thearchivist.api.player.domain.ports.in.GetPlayerHistoryUseCase;
import com.thearchivist.api.player.domain.ports.out.PlayerClassificationPort;
import com.thearchivist.api.player.domain.ports.out.PlayerClassificationPort.Percentile;
import com.thearchivist.api.player.domain.ports.out.PlayerClassificationPort.PlaylistRecord;
import com.thearchivist.api.player.domain.ports.out.PlayerRepositoryPort;
import com.thearchivist.api.stats.domain.models.WinRate;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetPlayerClassificationServiceTest {

    private static final UUID RANKED = UUID.randomUUID();
    private static final UUID SOCIAL = UUID.randomUUID();

    private static Standing standing(int remaining, double mmr) {
        return new Standing(5, "Diamond", 3, null, 40, remaining, mmr, 1.5, null, 0);
    }

    private static GetPlayerClassificationService service(boolean knownPlayer, List<PlaylistRecord> records,
                                                          long percentileSample, Integer leaderboardRank) {
        GetPlayerHistoryUseCase history = (GetPlayerHistoryUseCase) Proxy.newProxyInstance(
                GetPlayerClassificationServiceTest.class.getClassLoader(),
                new Class<?>[]{GetPlayerHistoryUseCase.class}, (p, m, a) -> null);
        PlayerRepositoryPort players = (PlayerRepositoryPort) Proxy.newProxyInstance(
                GetPlayerClassificationServiceTest.class.getClassLoader(),
                new Class<?>[]{PlayerRepositoryPort.class}, (p, m, a) ->
                        m.getName().equals("findByGamertag")
                                ? (knownPlayer ? Optional.of(new Player("Tester", null, Instant.now())) : Optional.empty())
                                : null);
        PlayerClassificationPort classification = new PlayerClassificationPort() {
            public List<PlaylistRecord> findPlaylistRecords(String gamertag) { return records; }
            public Optional<Integer> findLeaderboardRank(String gamertag, UUID playlistId) {
                return Optional.ofNullable(leaderboardRank);
            }
            public Optional<Percentile> findMmrPercentile(UUID playlistId, double mmr, Instant since) {
                return Optional.of(new Percentile(87.456, percentileSample));
            }
        };
        return new GetPlayerClassificationService(history, players, classification, 90);
    }

    @Test
    void unknownPlayerAfterSyncIsNotFound() {
        assertThrows(PlayerNotFoundException.class,
                () -> service(false, List.of(), 0, null).execute("Tester"));
    }

    @Test
    void activeProvisionalPlayerWithEnoughSampleGetsPercentileAndLeaderboardRank() {
        PlaylistRecord record = new PlaylistRecord(RANKED, "Ranked 3v3", 100, 60,
                Instant.now().minus(2, ChronoUnit.DAYS), standing(3, 25.0));
        PlayerClassification c = service(true, List.of(record), 500, 12).execute("Tester");

        PlaylistClassification p = c.playlists().get(0);
        assertEquals(100, p.matches());
        assertEquals(60, p.wins());
        assertEquals(40, p.losses());
        assertEquals(WinRate.Confidence.MEDIUM, p.confidence());
        assertEquals(Activity.ACTIVE, p.activity());
        assertTrue(p.provisional(), "con partidas de calibración pendientes el rango no es firme");
        assertEquals(12, p.leaderboardRank());
        assertEquals(87.5, p.standing().mmrPercentile(), 0.0001);   // redondeado a 1 decimal
        assertEquals(500, p.standing().percentileSample());
        assertEquals("Diamond", p.standing().designationName());
    }

    @Test
    void tooSmallSampleOmitsPercentileAndOldMatchesMeanInactive() {
        PlaylistRecord record = new PlaylistRecord(RANKED, "Ranked 3v3", 40, 20,
                Instant.now().minus(60, ChronoUnit.DAYS), standing(0, 10.0));
        PlaylistClassification p = service(true, List.of(record), 50, null).execute("Tester").playlists().get(0);

        assertNull(p.standing().mmrPercentile(), "con menos de 200 participantes el percentil no significa nada");
        assertEquals(0, p.standing().percentileSample());
        assertEquals(Activity.INACTIVE, p.activity());
        assertFalse(p.provisional());
        assertNull(p.leaderboardRank());
    }

    @Test
    void playlistWithoutRatingHasNoStanding() {
        PlaylistRecord record = new PlaylistRecord(SOCIAL, "Social", 10, 5, Instant.now(), null);
        PlaylistClassification p = service(true, List.of(record), 500, null).execute("Tester").playlists().get(0);
        assertNull(p.standing());
        assertFalse(p.provisional());
    }
}
