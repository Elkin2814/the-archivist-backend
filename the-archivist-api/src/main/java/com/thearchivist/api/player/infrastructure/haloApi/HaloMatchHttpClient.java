package com.thearchivist.api.player.infrastructure.haloApi;

import com.thearchivist.api.config.HaloApiGate;
import com.thearchivist.api.config.HaloApiProperties;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import com.thearchivist.api.player.domain.models.HistoryPage;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.domain.ports.out.HaloMatchClientPort;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloHistoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * Adaptador de salida hacia "Halo Wars 2 - Player Match History".
 * La llave se toma de la propiedad halo.api.key (variable de entorno HALO_API_KEY).
 */
@Component
public class HaloMatchHttpClient implements HaloMatchClientPort {

    private static final Logger log = LoggerFactory.getLogger(HaloMatchHttpClient.class);

    private final RestClient restClient;
    private final String apiKey;
    private final HaloApiGate gate;

    public HaloMatchHttpClient(HaloApiProperties properties, HaloApiGate gate) {
        this.apiKey = properties.key();
        this.gate = gate;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public List<PlayerMatch> fetchMatchHistory(String gamertag) {
        return fetchHistoryPage(gamertag, 0, MAX_PAGE_SIZE, false).matches();
    }

    @Override
    public HistoryPage fetchHistoryPage(String gamertag, int start, int count, boolean matchmakingOnly) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new HaloApiUnavailableException(
                    "La llave de la Halo API no está configurada (propiedad halo.api.key / variable HALO_API_KEY)");
        }

        int pageSize = Math.min(Math.max(count, 1), MAX_PAGE_SIZE);
        HaloHistoryResponse response;
        try {
            response = request(gamertag, start, pageSize, matchmakingOnly);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) throw new PlayerNotFoundException(gamertag);
            throw gate.unavailable(status, e);
        } catch (RestClientException e) {
            throw new HaloApiUnavailableException("No se pudo contactar a la Halo API: " + e.getMessage(), e);
        }

        if (response == null || response.results() == null) {
            return new HistoryPage(List.of(), 0);
        }

        List<PlayerMatch> matches = response.results().stream()
                .map(HaloHistoryMapper::toDomain)
                .flatMap(java.util.Optional::stream)
                .toList();

        log.info("Halo API: {} partidas válidas de {} recibidas para '{}' (start={})",
                matches.size(), response.results().size(), gamertag, start);
        return new HistoryPage(matches, response.results().size());
    }

    /**
     * Una llamada respetando el ritmo del gate. Si el servidor cierra la conexión HTTP/2 (GOAWAY) o se corta antes
     * de responder, se reintenta una vez: la segunda llamada abre una conexión nueva.
     */
    private HaloHistoryResponse request(String gamertag, int start, int pageSize, boolean matchmakingOnly) {
        gate.acquire();
        try {
            return call(gamertag, start, pageSize, matchmakingOnly);
        } catch (ResourceAccessException e) {
            gate.acquire();
            return call(gamertag, start, pageSize, matchmakingOnly);
        }
    }

    private HaloHistoryResponse call(String gamertag, int start, int pageSize, boolean matchmakingOnly) {
        return restClient.get()
                .uri(uri -> {
                    uri.path("/stats/hw2/players/{gamertag}/matches")
                            .queryParam("start", Math.max(start, 0))
                            .queryParam("count", pageSize);
                    if (matchmakingOnly) uri.queryParam("matchType", "matchmaking");
                    return uri.build(gamertag);
                })
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .retrieve()
                .body(HaloHistoryResponse.class);
    }
}
