package com.thearchivist.api.crawler.application.services;

import com.thearchivist.api.crawler.domain.enums.CrawlStatus;
import com.thearchivist.api.crawler.domain.models.RankedPlayer;
import com.thearchivist.api.crawler.domain.models.RankedPlaylist;
import com.thearchivist.api.crawler.domain.models.TrackedPlayer;
import com.thearchivist.api.crawler.domain.ports.in.GetRankingsUseCase;
import com.thearchivist.api.crawler.domain.ports.out.CrawlQueuePort;
import com.thearchivist.api.crawler.domain.ports.out.LeaderboardRepositoryPort;
import com.thearchivist.api.crawler.domain.ports.out.TrackedRankingPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RankingService implements GetRankingsUseCase {

    private final LeaderboardRepositoryPort leaderboards;
    private final CrawlQueuePort queue;
    private final TrackedRankingPort trackedRanking;

    public RankingService(LeaderboardRepositoryPort leaderboards, CrawlQueuePort queue,
                          TrackedRankingPort trackedRanking) {
        this.leaderboards = leaderboards;
        this.queue = queue;
        this.trackedRanking = trackedRanking;
    }

    @Override
    public List<RankedPlaylist> playlists() {
        return leaderboards.findPlaylists();
    }

    @Override
    public List<RankedPlayer> players(UUID playlistId, int limit, int offset) {
        return leaderboards.findPlayers(playlistId, limit, offset);
    }

    @Override
    public long countPlayers(UUID playlistId) {
        return leaderboards.countPlayers(playlistId);
    }

    @Override
    public List<TrackedPlayer> tracked(UUID playlistId, int minMatches, int activeDays, int limit, int offset) {
        return trackedRanking.findTracked(playlistId, minMatches, since(activeDays), limit, offset);
    }

    @Override
    public long countTracked(UUID playlistId, int minMatches, int activeDays) {
        return trackedRanking.countTracked(playlistId, minMatches, since(activeDays));
    }

    private static Instant since(int activeDays) {
        return Instant.now().minus(activeDays, ChronoUnit.DAYS);
    }

    @Override
    public Map<String, Long> queueStatus() {
        Map<CrawlStatus, Long> counts = queue.countByStatus();
        Map<String, Long> result = new LinkedHashMap<>();
        for (CrawlStatus status : CrawlStatus.values()) {
            result.put(status.name(), counts.getOrDefault(status, 0L));
        }
        return result;
    }
}
