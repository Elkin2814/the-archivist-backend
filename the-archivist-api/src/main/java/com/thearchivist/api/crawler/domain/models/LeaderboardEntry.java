package com.thearchivist.api.crawler.domain.models;

/** Un puesto del leaderboard oficial de CSR de una cola y temporada. */
public record LeaderboardEntry(int rank, String gamertag, Integer csrDesignation, Integer csrTier, Integer csr,
                               Integer percentToNextTier) {}
