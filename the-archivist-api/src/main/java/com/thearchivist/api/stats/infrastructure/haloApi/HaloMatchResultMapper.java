package com.thearchivist.api.stats.infrastructure.haloApi;

import com.thearchivist.api.player.infrastructure.haloApi.HaloHistoryMapper;
import com.thearchivist.api.player.infrastructure.haloApi.HaloRatingMapper;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloRatingProgressDTO;
import com.thearchivist.api.stats.domain.enums.GameMode;
import com.thearchivist.api.stats.domain.enums.MatchType;
import com.thearchivist.api.stats.domain.enums.StateMatch;
import com.thearchivist.api.stats.domain.models.MatchDetail;
import com.thearchivist.api.stats.domain.models.MatchParticipant;
import com.thearchivist.api.stats.infrastructure.haloApi.dto.HaloMatchDetailDTO;
import com.thearchivist.api.stats.infrastructure.haloApi.dto.HaloMatchPlayerDTO;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Traduce "Match Result" a {@link MatchDetail}. Nunca lee gamertags.
 */
public final class HaloMatchResultMapper {

    private HaloMatchResultMapper() {
    }

    /**
     * @param matchId la partida que se pidió; se usa como identidad para no depender del eco de la API
     */
    public static MatchDetail toDomain(UUID matchId, HaloMatchDetailDTO dto) {
        Map<Integer, Integer> teamOutcomes = teamOutcomes(dto.teams());
        Map<Integer, Integer> teamSizes = teamSizes(dto.teams());
        return new MatchDetail(
                matchId,
                MatchType.fromCode(dto.matchType()),
                GameMode.fromCode(dto.gameMode()),
                HaloHistoryMapper.parseUuid(dto.seasonId()),
                HaloHistoryMapper.parseUuid(dto.playlistId()),
                dto.mapId(),
                HaloHistoryMapper.parseInstant(dto.matchStartDate() == null ? null : dto.matchStartDate().iso8601Date()),
                seconds(HaloHistoryMapper.parseDuration(dto.matchDuration())),
                dto.matchComplete(),
                dto.matchEndReason(),
                dto.victoryCondition(),
                participants(dto.players(), teamOutcomes, teamSizes),
                teamOutcomes
        );
    }

    private static Map<Integer, Integer> teamOutcomes(Map<String, HaloMatchDetailDTO.TeamDTO> teams) {
        Map<Integer, Integer> result = new HashMap<>();
        if (teams == null) return result;
        teams.forEach((key, team) -> {
            Integer teamId = parseIndex(key);
            if (teamId != null && team != null && team.matchOutcome() != null) {
                result.put(teamId, team.matchOutcome());
            }
        });
        return result;
    }

    private static Map<Integer, Integer> teamSizes(Map<String, HaloMatchDetailDTO.TeamDTO> teams) {
        Map<Integer, Integer> result = new HashMap<>();
        if (teams == null) return result;
        teams.forEach((key, team) -> {
            Integer teamId = parseIndex(key);
            if (teamId != null && team != null && team.teamSize() != null && team.teamSize() > 0) {
                result.put(teamId, team.teamSize());
            }
        });
        return result;
    }

    private static List<MatchParticipant> participants(Map<String, HaloMatchPlayerDTO> players,
                                                       Map<Integer, Integer> teamOutcomes,
                                                       Map<Integer, Integer> teamSizes) {
        List<MatchParticipant> result = new ArrayList<>();
        if (players == null) return result;
        for (Map.Entry<String, HaloMatchPlayerDTO> entry : players.entrySet()) {
            Integer index = parseIndex(entry.getKey());
            HaloMatchPlayerDTO p = entry.getValue();
            if (index == null || p == null) continue;
            HaloRatingProgressDTO rating = p.ratingProgress();
            result.add(new MatchParticipant(
                    index,
                    p.teamId(),
                    p.teamPlayerIndex(),
                    p.leaderId() != null ? p.leaderId() : 0,
                    StateMatch.fromOutcome(p.outcome()).getCode(),
                    Boolean.TRUE.equals(p.completedMatch()),
                    p.human() == null || p.human(),
                    seconds(HaloHistoryMapper.parseDuration(p.timeInMatch())),
                    rating == null ? null : HaloRatingMapper.toCsr(rating.previousCsr()),
                    rating == null ? null : HaloRatingMapper.toCsr(rating.updatedCsr()),
                    rating == null ? null : HaloRatingMapper.toMmr(rating.previousMmr()),
                    rating == null ? null : HaloRatingMapper.toMmr(rating.updatedMmr()),
                    powers(p.leaderPowerStats()),
                    p.teamId() == null ? null : teamOutcomes.get(p.teamId()),
                    p.teamId() == null ? null : teamSizes.get(p.teamId())));
        }
        result.sort(Comparator.comparingInt(MatchParticipant::playerIndex));
        return result;
    }

    private static Map<String, Integer> powers(Map<String, HaloMatchPlayerDTO.PowerStat> stats) {
        Map<String, Integer> result = new HashMap<>();
        if (stats == null) return result;
        stats.forEach((power, stat) -> {
            if (power != null && !power.isBlank() && stat != null && stat.timesCast() != null && stat.timesCast() > 0) {
                result.put(power, stat.timesCast());
            }
        });
        return result;
    }

    private static Integer parseIndex(String key) {
        try {
            return Integer.valueOf(key.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Long seconds(Duration duration) {
        return duration == null ? null : duration.getSeconds();
    }
}
