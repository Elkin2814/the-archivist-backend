package com.thearchivist.api.metadata.infrastructure.haloApi;

import com.thearchivist.api.config.HaloApiProperties;
import com.thearchivist.api.metadata.domain.models.CsrDesignationInfo;
import com.thearchivist.api.metadata.domain.models.LeaderMetadataInfo;
import com.thearchivist.api.metadata.domain.models.MapInfo;
import com.thearchivist.api.metadata.domain.models.PlaylistInfo;
import com.thearchivist.api.metadata.domain.models.SeasonInfo;
import com.thearchivist.api.metadata.domain.ports.out.HaloMetadataClientPort;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adaptador de salida hacia la Metadata de Halo Wars 2 (siempre /metadata/hw2/...;
 * nunca se consultan datos de otros juegos).
 */
@Component
public class HaloMetadataHttpClient implements HaloMetadataClientPort {

    private static final Logger log = LoggerFactory.getLogger(HaloMetadataHttpClient.class);

    /** La Metadata pagina de 100 en 100 (startAt debe ser múltiplo de 100). */
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;

    private final RestClient restClient;
    private final String apiKey;

    public HaloMetadataHttpClient(HaloApiProperties properties) {
        this.apiKey = properties.key();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public List<LeaderMetadataInfo> fetchLeaders() {
        return fetchAll("leaders", HaloMetadataParser::toLeader);
    }

    @Override
    public List<MapInfo> fetchMaps() {
        return fetchAll("maps", HaloMetadataParser::toMap);
    }

    @Override
    public List<PlaylistInfo> fetchPlaylists() {
        return fetchAll("playlists", HaloMetadataParser::toPlaylist);
    }

    @Override
    public List<SeasonInfo> fetchSeasons() {
        return fetchAll("seasons", HaloMetadataParser::toSeason);
    }

    @Override
    public List<CsrDesignationInfo> fetchCsrDesignations() {
        return fetchAll("csr-designations", HaloMetadataParser::toCsrDesignation);
    }

    private <T> List<T> fetchAll(String resource, Function<Map<String, Object>, Optional<T>> parser) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new HaloApiUnavailableException(
                    "La llave de la Halo API no está configurada (propiedad halo.api.key / variable HALO_API_KEY)");
        }

        List<T> result = new ArrayList<>();
        int received = 0;
        Map<String, Object> sample = null;
        int startAt = 0;
        for (int pageNumber = 0; pageNumber < MAX_PAGES; pageNumber++) {
            Map<String, Object> page = fetchPage(resource, startAt);
            List<Map<String, Object>> items = HaloMetadataParser.contentItems(page);
            received += items.size();
            if (sample == null && !items.isEmpty()) sample = items.get(0);
            items.stream().map(parser).flatMap(Optional::stream).forEach(result::add);

            startAt += PAGE_SIZE;
            if (items.isEmpty() || startAt >= HaloMetadataParser.totalCount(page)) break;
        }
        log.info("Halo Metadata /{}: {} registros", resource, result.size());
        if (result.isEmpty() && received > 0) {
            String text = String.valueOf(sample);
            log.warn("Halo Metadata /{}: llegaron {} ítems pero ninguno se pudo interpretar. Primer ítem: {}",
                    resource, received, text.length() > 1500 ? text.substring(0, 1500) + "…" : text);
        } else if (received == 0) {
            log.warn("Halo Metadata /{}: la API no devolvió ítems (ContentItems vacío)", resource);
        }
        return result;
    }

    private Map<String, Object> fetchPage(String resource, int startAt) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(uri -> uri.path("/metadata/hw2/" + resource)
                            .queryParam("startAt", startAt)
                            .build())
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .header("Accept-Language", "en")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return body == null ? Map.of() : body;
        } catch (RestClientResponseException e) {
            throw new HaloApiUnavailableException(
                    "La Metadata de la Halo API respondió HTTP " + e.getStatusCode().value() + " en /" + resource, e);
        } catch (RestClientException e) {
            throw new HaloApiUnavailableException("No se pudo contactar a la Halo API: " + e.getMessage(), e);
        }
    }
}
