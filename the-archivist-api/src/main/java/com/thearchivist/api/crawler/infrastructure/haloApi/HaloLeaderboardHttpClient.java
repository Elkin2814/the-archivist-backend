package com.thearchivist.api.crawler.infrastructure.haloApi;

import com.thearchivist.api.config.HaloApiGate;
import com.thearchivist.api.config.HaloApiProperties;
import com.thearchivist.api.crawler.domain.models.LeaderboardEntry;
import com.thearchivist.api.crawler.domain.ports.out.LeaderboardClientPort;
import com.thearchivist.api.crawler.infrastructure.haloApi.dto.HaloLeaderboardResponse;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador de salida hacia "Halo Wars 2 - Leaderboard - Player CSR"
 * (GET /stats/hw2/player-leaderboards/csr/{seasonId}/{playlistId}). La API solo lista a los jugadores con
 * CSR de 1800 o más y entrega como máximo 250 puestos.
 */
@Component
public class HaloLeaderboardHttpClient implements LeaderboardClientPort {

    private final RestClient restClient;
    private final String apiKey;
    private final HaloApiGate gate;

    public HaloLeaderboardHttpClient(HaloApiProperties properties, HaloApiGate gate) {
        this.apiKey = properties.key();
        this.gate = gate;

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
    public List<LeaderboardEntry> fetchTopPlayers(UUID seasonId, UUID playlistId, int count) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new HaloApiUnavailableException(
                    "La llave de la Halo API no está configurada (propiedad halo.api.key / variable HALO_API_KEY)");
        }

        HaloLeaderboardResponse response;
        gate.acquire();
        try {
            response = restClient.get()
                    .uri(uri -> uri.path("/stats/hw2/player-leaderboards/csr/{seasonId}/{playlistId}")
                            .queryParam("count", count)
                            .build(seasonId, playlistId))
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .retrieve()
                    .body(HaloLeaderboardResponse.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            // Colas sin leaderboard (o temporada sin datos): no es un fallo, simplemente no hay lista.
            if (status == 404 || status == 400) return List.of();
            throw gate.unavailable(status, e);
        } catch (RestClientException e) {
            throw new HaloApiUnavailableException("No se pudo contactar a la Halo API: " + e.getMessage(), e);
        }

        if (response == null || response.results() == null) return List.of();

        List<LeaderboardEntry> entries = new ArrayList<>();
        Set<Integer> usedRanks = new HashSet<>();
        int position = 0;
        for (HaloLeaderboardResponse.Item item : response.results()) {
            position++;
            if (item == null || item.player() == null) continue;
            String gamertag = item.player().gamertag();
            if (gamertag == null || gamertag.isBlank()) continue;
            int rank = item.rank() != null ? item.rank() : position;
            if (!usedRanks.add(rank)) continue;
            HaloLeaderboardResponse.ScoreDTO score = item.score();
            entries.add(new LeaderboardEntry(rank, gamertag.trim(),
                    score == null ? null : score.designationId(),
                    score == null ? null : score.tier(),
                    score == null ? null : score.csr(),
                    score == null ? null : score.percentToNextTier()));
        }
        return entries;
    }
}
