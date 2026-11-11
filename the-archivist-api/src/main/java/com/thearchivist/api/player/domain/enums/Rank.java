package com.thearchivist.api.player.domain.enums;

import java.util.Optional;

/**
 * Designaciones de CSR de Halo Wars 2. El ID es el de la Metadata (csr-designations) y el que
 * llega en "Designation" de RatingProgress.
 */
public enum Rank {
    UNRANKED(0),
    BRONZE(1),
    SILVER(2),
    GOLD(3),
    PLATINUM(4),
    DIAMOND(5),
    ONYX(6),
    CHAMPION(7);

    private final int designationId;

    Rank(int designationId) {
        this.designationId = designationId;
    }

    public int getDesignationId() {
        return designationId;
    }

    public static Optional<Rank> fromDesignation(Integer designationId) {
        if (designationId == null) return Optional.empty();
        for (Rank rank : values()) {
            if (rank.designationId == designationId) return Optional.of(rank);
        }
        return Optional.empty();
    }
}
