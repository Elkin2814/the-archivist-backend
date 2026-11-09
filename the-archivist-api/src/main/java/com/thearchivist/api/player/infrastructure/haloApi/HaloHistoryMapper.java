package com.thearchivist.api.player.infrastructure.haloApi;

import com.thearchivist.api.player.domain.models.Match;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloCsrDTO;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloMatchResultDTO;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloRatingProgressDTO;
import com.thearchivist.api.stats.domain.enums.GameMode;
import com.thearchivist.api.stats.domain.enums.MatchType;
import com.thearchivist.api.stats.domain.enums.StateMatch;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduce la respuesta de la Halo API (DTO externo) a objetos de dominio.
 */
public final class HaloHistoryMapper {

    private static final UUID EMPTY_GUID = new UUID(0L, 0L);

    private HaloHistoryMapper() {
    }

    /** Devuelve vacío si la partida no trae MatchId o fecha válidos (no se puede guardar). */
    public static Optional<PlayerMatch> toDomain(HaloMatchResultDTO dto) {
        UUID matchId = parseUuid(dto.matchId());
        Instant startDate = parseInstant(dto.matchStartDate() == null ? null : dto.matchStartDate().iso8601Date());
        if (matchId == null || startDate == null) {
            return Optional.empty();
        }

        Match match = new Match(
                matchId,
                MatchType.fromCode(dto.matchType()),
                GameMode.fromCode(dto.gameMode()),
                parseUuid(dto.seasonId()),
                parseUuid(dto.playlistId()),
                dto.mapId(),
                startDate
        );

        PlayerMatch pm = new PlayerMatch();
        pm.setMatch(match);
        pm.setStateMatch(StateMatch.fromOutcome(dto.outcome()));
        pm.setLeaderId(dto.leaderId() != null ? dto.leaderId() : 0);
        pm.setCompletedMatch(Boolean.TRUE.equals(dto.completedMatch()));
        pm.setTeamId(dto.teamId());
        pm.setPlayerIndex(dto.playerIndex());
        pm.setTeamPlayerIndex(dto.teamPlayerIndex());
        pm.setDuration(parseDuration(dto.duration()));

        if (dto.xpProgress() != null) {
            pm.setPreviousXp(dto.xpProgress().previousTotalXp());
            pm.setUpdatedXp(dto.xpProgress().updatedTotalXp());
        }

        HaloRatingProgressDTO rating = dto.ratingProgress();
        if (rating != null) {
            HaloCsrDTO before = rating.previousCsr();
            HaloCsrDTO after = rating.updatedCsr();
            if (before != null) {
                pm.setPreviousTier(before.tier());
                pm.setPreviousDesignation(before.designation());
            }
            if (after != null) {
                pm.setUpdatedTier(after.tier());
                pm.setUpdatedDesignation(after.designation());
            }
            if (rating.previousMmr() != null) pm.setPreviousMmr(rating.previousMmr().rating());
            if (rating.updatedMmr() != null) pm.setUpdatedMmr(rating.updatedMmr().rating());
        }
        return Optional.of(pm);
    }

    /** Acepta GUID con o sin guiones. Devuelve null si está vacío o es el GUID vacío. */
    static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() == 32 && !value.contains("-")) {
            value = value.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                    "$1-$2-$3-$4-$5");
        }
        try {
            UUID uuid = UUID.fromString(value);
            return EMPTY_GUID.equals(uuid) ? null : uuid;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Formato ISO 8601, por ejemplo "PT20M36.9956805S" (también "P1DT18H1M2.98S"). */
    private static Duration parseDuration(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Duration.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
