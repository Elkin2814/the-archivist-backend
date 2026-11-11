package com.thearchivist.api.player.infrastructure.persistence;

import com.thearchivist.api.player.domain.models.Match;
import com.thearchivist.api.player.domain.models.Player;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.infrastructure.persistence.entity.MatchEntity;
import com.thearchivist.api.player.infrastructure.persistence.entity.PlayerEntity;
import com.thearchivist.api.player.infrastructure.persistence.entity.PlayerMatchEntity;
import com.thearchivist.api.stats.domain.enums.GameMode;
import com.thearchivist.api.stats.domain.enums.MatchType;
import com.thearchivist.api.stats.domain.enums.StateMatch;
import com.thearchivist.api.stats.domain.models.CsrReading;
import com.thearchivist.api.stats.domain.models.MmrReading;

import java.time.Duration;

/**
 * Traduce entre el dominio y las entidades JPA.
 */
final class PlayerPersistenceMapper {

    private PlayerPersistenceMapper() {
    }

    static Player toDomain(PlayerEntity entity) {
        return new Player(entity.getGamerTag(), null, entity.getLastSync());
    }

    static MatchEntity toEntity(Match match) {
        MatchEntity entity = new MatchEntity();
        entity.setMatchId(match.getMatchId());
        entity.setMatchType(match.getMatchType().getCode());
        entity.setGameMode(match.getGameMode().getCode());
        entity.setSeasonId(match.getSeasonId());
        entity.setPlaylistId(match.getPlaylistId());
        entity.setMapId(match.getMapId());
        entity.setStartDate(match.getStartDate());
        return entity;
    }

    static PlayerMatchEntity toEntity(PlayerMatch pm, PlayerEntity player, MatchEntity match) {
        PlayerMatchEntity entity = new PlayerMatchEntity();
        entity.setPlayer(player);
        entity.setMatch(match);
        entity.setLeaderId(pm.getLeaderId());
        entity.setTeamId(pm.getTeamId());
        entity.setPlayerIndex(pm.getPlayerIndex());
        entity.setTeamPlayerIndex(pm.getTeamPlayerIndex());
        entity.setCompletedMatch(pm.isCompletedMatch());
        entity.setOutcome(pm.getStateMatch().getCode());
        entity.setDurationSeconds(pm.getDuration() == null ? null : pm.getDuration().getSeconds());
        CsrReading before = pm.getPreviousCsr();
        if (before != null) {
            entity.setCsrTierBefore(before.tier());
            entity.setCsrDesignationBefore(before.designation());
            entity.setCsrRawBefore(before.raw());
            entity.setCsrPercentBefore(before.percentToNextTier());
            entity.setCsrMeasurementRemainingBefore(before.measurementMatchesRemaining());
            entity.setCsrRankBefore(before.rank());
        }
        CsrReading after = pm.getUpdatedCsr();
        if (after != null) {
            entity.setCsrTierAfter(after.tier());
            entity.setCsrDesignationAfter(after.designation());
            entity.setCsrRawAfter(after.raw());
            entity.setCsrPercentAfter(after.percentToNextTier());
            entity.setCsrMeasurementRemainingAfter(after.measurementMatchesRemaining());
            entity.setCsrRankAfter(after.rank());
        }
        if (pm.getPreviousMmr() != null) {
            entity.setMmrBefore(pm.getPreviousMmr().rating());
            entity.setMmrVarianceBefore(pm.getPreviousMmr().variance());
        }
        if (pm.getUpdatedMmr() != null) {
            entity.setMmrAfter(pm.getUpdatedMmr().rating());
            entity.setMmrVarianceAfter(pm.getUpdatedMmr().variance());
        }
        entity.setXpBefore(pm.getPreviousXp());
        entity.setXpAfter(pm.getUpdatedXp());
        return entity;
    }

    static PlayerMatch toDomain(PlayerMatchEntity entity) {
        MatchEntity m = entity.getMatch();
        Match match = new Match(
                m.getMatchId(),
                MatchType.fromCode(m.getMatchType()),
                GameMode.fromCode(m.getGameMode()),
                m.getSeasonId(),
                m.getPlaylistId(),
                m.getMapId(),
                m.getStartDate()
        );

        PlayerMatch pm = new PlayerMatch();
        pm.setMatch(match);
        pm.setStateMatch(StateMatch.fromOutcome(entity.getOutcome()));
        pm.setLeaderId(entity.getLeaderId());
        pm.setCompletedMatch(entity.isCompletedMatch());
        pm.setTeamId(entity.getTeamId());
        pm.setPlayerIndex(entity.getPlayerIndex());
        pm.setTeamPlayerIndex(entity.getTeamPlayerIndex());
        pm.setDuration(entity.getDurationSeconds() == null ? null : Duration.ofSeconds(entity.getDurationSeconds()));
        pm.setPreviousCsr(CsrReading.ofNullable(entity.getCsrTierBefore(), entity.getCsrDesignationBefore(),
                entity.getCsrRawBefore(), entity.getCsrPercentBefore(),
                entity.getCsrMeasurementRemainingBefore(), entity.getCsrRankBefore()));
        pm.setUpdatedCsr(CsrReading.ofNullable(entity.getCsrTierAfter(), entity.getCsrDesignationAfter(),
                entity.getCsrRawAfter(), entity.getCsrPercentAfter(),
                entity.getCsrMeasurementRemainingAfter(), entity.getCsrRankAfter()));
        pm.setPreviousMmr(MmrReading.ofNullable(entity.getMmrBefore(), entity.getMmrVarianceBefore()));
        pm.setUpdatedMmr(MmrReading.ofNullable(entity.getMmrAfter(), entity.getMmrVarianceAfter()));
        pm.setPreviousXp(entity.getXpBefore());
        pm.setUpdatedXp(entity.getXpAfter());
        return pm;
    }
}
