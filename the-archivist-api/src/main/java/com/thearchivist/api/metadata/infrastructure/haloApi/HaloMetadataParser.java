package com.thearchivist.api.metadata.infrastructure.haloApi;

import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.MapInfo;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lee los cuerpos de la Metadata API. Son muy anidados (cada ítem trae "View" y dentro el bloque
 * propio: "HW2Map", "HW2Playlist"...), así que se recorren como mapas y se toleran campos ausentes.
 */
final class HaloMetadataParser {

    private HaloMetadataParser() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> contentItems(Map<String, Object> page) {
        Object items = page.get("ContentItems");
        return items instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    static int totalCount(Map<String, Object> page) {
        Object total = asMap(page.get("Paging")).get("TotalCount");
        return total instanceof Number n ? n.intValue() : 0;
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static UUID uuid(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant instant(Object value) {
        if (value == null) return null;
        try {
            return Instant.parse(value.toString().trim());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Boolean bool(Object value) {
        return value instanceof Boolean b ? b : null;
    }

    /** Nombre localizado: View.&lt;bloque&gt;.DisplayInfo.View.&lt;HW2...DisplayInfo&gt;.Name; si no hay, el Title. */
    private static String displayName(Map<String, Object> view, Map<String, Object> block) {
        Map<String, Object> displayView = asMap(asMap(block.get("DisplayInfo")).get("View"));
        for (Object candidate : displayView.values()) {
            Object name = asMap(candidate).get("Name");
            if (name != null && !name.toString().isBlank()) return name.toString();
        }
        return text(view.get("Title"));
    }

    static Optional<MapInfo> toMap(Map<String, Object> item) {
        Map<String, Object> view = asMap(item.get("View"));
        Map<String, Object> block = asMap(view.get("HW2Map"));
        String id = text(block.get("ID"));
        String name = displayName(view, block);
        if (id == null || id.isBlank() || name == null) return Optional.empty();
        return Optional.of(new MapInfo(id, uuid(view.get("Identity")), name));
    }

    static Optional<PlaylistInfo> toPlaylist(Map<String, Object> item) {
        Map<String, Object> view = asMap(item.get("View"));
        Map<String, Object> block = asMap(view.get("HW2Playlist"));
        UUID id = uuid(view.get("Identity"));
        String name = displayName(view, block);
        if (id == null || name == null) return Optional.empty();
        return Optional.of(new PlaylistInfo(id, name,
                text(block.get("StatsClassification")),
                bool(block.get("IsTeamGamePlaylist")),
                bool(block.get("Hide"))));
    }

    static Optional<SeasonInfo> toSeason(Map<String, Object> item) {
        Map<String, Object> view = asMap(item.get("View"));
        Map<String, Object> block = asMap(view.get("HW2Season"));
        UUID id = uuid(view.get("Identity"));
        String name = displayName(view, block);
        if (id == null || name == null) return Optional.empty();
        return Optional.of(new SeasonInfo(id, name, instant(block.get("StartDate"))));
    }

    static Optional<CsrDesignationInfo> toCsrDesignation(Map<String, Object> item) {
        Map<String, Object> view = asMap(item.get("View"));
        Map<String, Object> block = asMap(view.get("HW2CsrDesignation"));
        Object id = block.get("ID");
        String name = displayName(view, block);
        if (!(id instanceof Number number) || name == null) return Optional.empty();
        return Optional.of(new CsrDesignationInfo(number.intValue(), name));
    }
}
