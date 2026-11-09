package com.thearchivist.api.player.infrastructure.haloApi;

import com.thearchivist.api.config.HaloApiProperties;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.domain.ports.out.HaloMatchClientPort;
import com.thearchivist.api.player.infrastructure.haloApi.dto.HaloHistoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
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

    /** Máximo que permite la Halo API por llamada. */
    private static final int HISTORY_COUNT = 25;

    private final RestClient restClient;
    private final String apiKey;

    public HaloMatchHttpClient(HaloApiProperties properties) {
        this.apiKey = properties.key();

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
        if (apiKey == null || apiKey.isBlank()) {
            throw new HaloApiUnavailableException(
                    "La llave de la Halo API no está configurada (propiedad halo.api.key / variable HALO_API_KEY)");
        }

        HaloHistoryResponse response = null;
        try {
            response = restClient.get()
                    .uri(uri -> uri.path("/stats/hw2/players/{gamertag}/matches")
                            .queryParam("count", HISTORY_COUNT)
                            .build(gamertag))
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .retrieve()
                    .body(HaloHistoryResponse.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            switch (status) {
                case 404 -> throw new PlayerNotFoundException(gamertag);
                case 401, 403 -> throw new HaloApiUnavailableException(
                        "La Halo API rechazó la llave (HTTP " + status + "). Revisa HALO_API_KEY", e);
                case 429 -> throw new HaloApiUnavailableException(
                        "Se superó el límite de llamadas de la Halo API (HTTP 429)", e);
                default -> throw new HaloApiUnavailableException(
                        "La Halo API respondió con HTTP " + status, e);
            }
        } catch (RestClientException e) {
            throw new HaloApiUnavailableException("No se pudo contactar a la Halo API: " + e.getMessage(), e);
        }

        if (response == null || response.results() == null) {
            return List.of();
        }

        List<PlayerMatch> matches = response.results().stream()
                .map(HaloHistoryMapper::toDomain)
                .flatMap(java.util.Optional::stream)
                .toList();

        log.info("Halo API: {} partidas válidas de {} recibidas para '{}'",
                matches.size(), response.results().size(), gamertag);
        return matches;
    }
}
