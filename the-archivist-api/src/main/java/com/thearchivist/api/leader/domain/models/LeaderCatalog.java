package com.thearchivist.api.leader.domain.models;

import com.thearchivist.api.leader.domain.enums.Faction;
import com.thearchivist.api.leader.domain.enums.Language;

import java.util.Arrays;
import java.util.Optional;

/**
 * Los 16 líderes de Halo Wars 2. No saldrán más, por eso viven en el código.
 *
 * <p>{@code apiId} es el "LeaderId" que entregan Match History / Match Result.
 * Los IDs 1 a 7 están documentados en la Metadata API (Leaders). Los demás están en
 * {@code null} hasta confirmarlos con la API real: un líder sin ID no se asocia a ninguna
 * partida, y las partidas con un ID desconocido se muestran como "Líder #N".
 */
public enum LeaderCatalog {

    // --- UNSC ---
    CUTTER(1, Faction.UNSC,
            "Captain Cutter", "CPT. Cutter", "Capitán Cutter", "CPT. Cutter"),
    ISABEL(2, Faction.UNSC,
            "Isabel", "Isabel", "Isabel", "Isabel"),
    ANDERS(3, Faction.UNSC,
            "Professor Anders", "PROF. Anders", "Profesora Anders", "PROF. Anders"),
    FORGE(7, Faction.UNSC,
            "Sergeant Forge", "SGT. Forge", "Sargento Forge", "SGT. Forge"),
    KINSANO(null, Faction.UNSC,
            "Kinsano", "Kinsano", "Kinsano", "Kinsano"),
    JOHNSON(null, Faction.UNSC,
            "Sergeant Johnson", "SGT. Johnson", "Sargento Johnson", "SGT. Johnson"),
    JEROME(null, Faction.UNSC,
            "Commander Jerome", "CDR. Jerome", "Comandante Jerome", "CDTE. Jerome"),
    SERINA(null, Faction.UNSC,
            "Serina", "Serina", "Serina", "Serina"),

    // --- Banished / Desterrados ---
    DECIMUS(4, Faction.BANISHED,
            "Decimus", "Decimus", "Decimus", "Decimus"),
    ATRIOX(5, Faction.BANISHED,
            "Atriox", "Atriox", "Atriox", "Atriox"),
    SHIPMASTER(6, Faction.BANISHED,
            "Shipmaster", "Shipmaster", "Maestro de Naves", "MTRO. de Naves"),
    COLONY(null, Faction.BANISHED,
            "Colony", "Colony", "Colonia", "Colonia"),
    ARBITER(null, Faction.BANISHED,
            "Arbiter", "Arbiter", "Inquisidor", "Inquisidor"),
    YAPYAP(null, Faction.BANISHED,
            "Yapyap the Destroyer", "Yapyap", "Yapyap el Destructor", "Yapyap"),
    VORIDUS(null, Faction.BANISHED,
            "Voridus", "Voridus", "Voridus", "Voridus"),
    PAVIUM(null, Faction.BANISHED,
            "Pavium", "Pavium", "Pavium", "Pavium");

    private final Integer apiId;
    private final Faction faction;
    private final String nameEn;
    private final String aliasEn;
    private final String nameEs;
    private final String aliasEs;

    LeaderCatalog(Integer apiId, Faction faction,
                  String nameEn, String aliasEn, String nameEs, String aliasEs) {
        this.apiId = apiId;
        this.faction = faction;
        this.nameEn = nameEn;
        this.aliasEn = aliasEn;
        this.nameEs = nameEs;
        this.aliasEs = aliasEs;
    }

    /** ID del líder en la Halo API; nulo mientras no esté confirmado. */
    public Integer getApiId() {
        return apiId;
    }

    public Faction getFaction() {
        return faction;
    }

    /** Nombre completo para mostrar. */
    public String name(Language language) {
        return language == Language.EN ? nameEn : nameEs;
    }

    /** Apodo corto para listas y tablas (CPT. Cutter, SGT. Forge...). */
    public String alias(Language language) {
        return language == Language.EN ? aliasEn : aliasEs;
    }

    public static Optional<LeaderCatalog> findByApiId(Integer apiId) {
        if (apiId == null) return Optional.empty();
        return Arrays.stream(values()).filter(l -> apiId.equals(l.apiId)).findFirst();
    }
}
