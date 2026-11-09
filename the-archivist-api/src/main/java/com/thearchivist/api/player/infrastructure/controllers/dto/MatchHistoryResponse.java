package com.thearchivist.api.player.infrastructure.controllers.dto;

import java.time.Instant;
import java.util.List;

public record MatchHistoryResponse(
        String gamerTag,
        Instant lastSync,
        Summary summary,
        Pagination pagination,
        List<PlayerMatchDTO> matches
) {
    public record Summary(long totalMatches, long wins, long losses, double winRate) {}

    public record Pagination(int page, int size, long totalElements, int totalPages) {}
}
