package com.thearchivist.api.stats.infrastructure.haloApi;

import com.thearchivist.api.config.HaloApiGate;
import com.thearchivist.api.config.HaloApiProperties;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import com.thearchivist.api.stats.domain.exceptions.MatchResultNotFoundException;
import com.thearchivist.api.stats.domain.models.MatchDetail;
import com.thearchivist.api.stats.domain.ports.out.MatchResultClientPort;
import com.thearchivist.api.stats.infrastructure.haloApi.dto.HaloMatchDetailDTO;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

/**
 * Adaptador de salida hacia "Halo Wars 2 - Match Result" (GET /stats/hw2/matches/{matchId}).
 * Usa la misma llave que el resto de llamadas (halo.api.key / HALO_API_KEY).
 */
@Component
public class HaloMatchResultHttpClient implements MatchResultClientPort {

    private final RestClient restClient;
    private final String apiKey;
    private final HaloApiGate gate;

    public HaloMatchResultHttpClient(HaloApiProperties properties, HaloApiGate gate) {
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
    public MatchDetail fetchMatchResult(UUID matchId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new HaloApiUnavailableException(
                    "La llave de la Halo API no está configurada (propiedad halo.api.key / variable HALO_API_KEY)");
        }

        HaloMatchDetailDTO response;
        try {
            response = request(matchId);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 404) throw new MatchResultNotFoundException(matchId);
            throw gate.unavailable(status, e);
        } catch (RestClientException e) {
            throw new HaloApiUnavailableException("No se pudo contactar a la Halo API: " + e.getMessage(), e);
        }

        if (response == null || response.players() == null || response.players().isEmpty()) {
            throw new MatchResultNotFoundException(matchId);
        }
        return HaloMatchResultMapper.toDomain(matchId, response);
    }

    /**
     * Una llamada respetando el ritmo del gate. Si el servidor cierra la conexión HTTP/2 (GOAWAY) o se corta antes
     * de responder, se reintenta una vez: la segunda llamada abre una conexión nueva y la corrida no se detiene.
     */
    private HaloMatchDetailDTO request(UUID matchId) {
        gate.acquire();
        try {
            return call(matchId);
        } catch (ResourceAccessException e) {
            gate.acquire();
            return call(matchId);
        }
    }

    private HaloMatchDetailDTO call(UUID matchId) {
        return restClient.get()
                .uri("/stats/hw2/matches/{matchId}", matchId)
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .retrieve()
                .body(HaloMatchDetailDTO.class);
    }
}
