package com.thearchivist.api.metadata.infrastructure.haloApi;

import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.LeaderMetadataInfo;
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

    /** Acepta el UUID con guiones o como la Metadata lo publica: 32 hexadecimales seguidos. */
    private static UUID uuid(Object value) {
        if (value == null) return null;
        String raw = value.toString().trim();
        if (raw.length() == 32) {
            raw = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16)
                    + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Solo contenido publicado; si la Metadata no trae el campo "Status" se da por publicado. */
    private static boolean isPublished(Map<String, Object> view) {
        Object status = view.get("Status");
        return status == null || "Published".equalsIgnoreCase(status.toString().trim());
    }

    /** Primer valor que sea un UUID válido (la Metadata no siempre pone el ID en el mismo sitio). */
    private static UUID firstUuid(Object... candidates) {
        for (Object candidate : candidates) {
            UUID id = uuid(candidate);
            if (id != null) return id;
        }
        return null;
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

    /** URL pública de la imagen del ítem: &lt;bloque&gt;.Image.View.Media.MediaUrl. */
    private static String imageUrl(Map<String, Object> block) {
        Object url = asMap(asMap(asMap(block.get("Image")).get("View")).get("Media")).get("MediaUrl");
        return url == null || url.toString().isBlank() ? null : url.toString();
    }

    static Optional<LeaderMetadataInfo> toLeader(Map<String, Object> item) {
        Map<String, Object> view = asMap(item.get("View"));
        Map<String, Object> block = asMap(view.get("HW2Leader"));
        Integer id;
        try {
            id = Integer.valueOf(String.valueOf(block.get("ID")).trim());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        String name = displayName(view, block);
        if (name == null) return Optional.empty();
        return Optional.of(new LeaderMetadataInfo(id, name, text(block.get("Faction")), imageUrl(block)));
    }

    static Optional<MapInfo> toMap(Map<String, Object> item) {
        Map<String, Object> view = asMap(item.get("View"));
        Map<String, Object> block = asMap(view.get("HW2Map"));
        String id = text(block.get("ID"));
        String name = displayName(view, block);
        if (id == null || id.isBlank() || name == null) return Optional.empty();
        return Optional.of(new MapInfo(id, uuid(view.get("Identity")), name, imageUrl(block)));
    }

    static Optional<PlaylistInfo> toPlaylist(Map<String, Object> item) {
        Map<String, Object> view = asMap(item.get("View"));
        Map<String, Object> block = asMap(view.get("HW2Playlist"));
        if (!isPublished(view)) return Optional.empty();
        UUID id = firstUuid(view.get("Identity"), block.get("ID"), block.get("Id"), block.get("id"));
        String name = displayName(view, block);
        if (id == null || name == null) return Optional.empty();
        return Optional.of(new PlaylistInfo(id, name,
                playlistCode(block),
                text(block.get("StatsClassification")),
                bool(block.get("IsTeamGamePlaylist")),
                bool(block.get("Hide"))));
    }

    /** Código de la cola (HW2Playlist.Id, p. ej. CP_RANKED_3v3_WAR). Si ese campo trae un GUID, no es un código. */
    private static String playlistCode(Map<String, Object> block) {
        for (Map.Entry<String, Object> entry : block.entrySet()) {
            if ("id".equalsIgnoreCase(entry.getKey()) && entry.getValue() instanceof String s
                    && !s.isBlank() && uuid(s) == null) {
                return s.trim();
            }
        }
        return null;
    }

    static Optional<SeasonInfo> toSeason(Map<String, Object> item) {
        Map<String, Object> view = asMap(item.get("View"));
        Map<String, Object> block = asMap(view.get("HW2Season"));
        UUID id = firstUuid(view.get("Identity"), block.get("ID"), block.get("Id"), block.get("id"));
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
