package com.thearchivist.api.crawler.application.services;

import com.thearchivist.api.crawler.domain.models.CrawlPlayer;
import com.thearchivist.api.crawler.domain.models.Reports;
import com.thearchivist.api.crawler.domain.ports.in.CrawlPlayersUseCase;
import com.thearchivist.api.crawler.domain.ports.out.CrawlQueuePort;
import com.thearchivist.api.player.domain.exceptions.HaloApiTransientException;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import com.thearchivist.api.player.domain.models.HistoryPage;
import com.thearchivist.api.player.domain.models.Player;
import com.thearchivist.api.player.domain.ports.out.HaloMatchClientPort;
import com.thearchivist.api.player.domain.ports.out.PlayerRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Recorre el historial de los jugadores de la cola, de la partida más reciente a la más antigua, y guarda sus
 * partidas (capa 2 y, sin gamertag, capa 3). El detalle de cada partida lo completa después la ingesta de Match Result.
 *
 * <ul>
 *   <li><b>Primer recorrido:</b> páginas de 25 desde {@code history_cursor}. Si la corrida se acaba antes de llegar al
 *       final, el avance queda guardado y continúa en la siguiente. Termina al llegar a la partida más antigua o
 *       al tope {@code archivist.crawl.max-history-matches}.</li>
 *   <li><b>Actualización:</b> una vez completo, cada {@code archivist.crawl.refresh-hours} horas se leen páginas desde la más
 *       reciente hasta que una página no trae partidas nuevas.</li>
 *   <li><b>Jugadores dormidos:</b> el leaderboard oficial incluye a gente que dejó de jugar hace años. Si la partida más
 *       reciente de un jugador es anterior a {@code archivist.crawl.inactive-after-days}, solo se guardan sus
 *       {@code dormant-max-matches} últimas partidas y se revisa de nuevo cada {@code dormant-refresh-days}: así la
 *       cuota de llamadas (Match Result es una por partida) se gasta sobre todo en jugadores activos.</li>
 *   <li>Solo partidas de matchmaking ({@code matchType=matchmaking}): las personalizadas y la campaña no cuentan.</li>
 * </ul>
 * Si la Halo API se niega a responder (límite, llave, caída) la corrida se corta sin marcar errores al jugador.
 */
@Service
public class PlayerCrawlService implements CrawlPlayersUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlayerCrawlService.class);

    /** Tope de páginas en una actualización (1.500 partidas nuevas): evita bucles con respuestas raras. */
    private static final int MAX_REFRESH_PAGES = 60;

    /** Errores 5xx seguidos (de jugadores distintos) a partir de los cuales se da la API por caída y se corta la corrida. */
    private static final int MAX_CONSECUTIVE_SERVER_ERRORS = 3;

    private final CrawlQueuePort queue;
    private final HaloMatchClientPort client;
    private final PlayerRepositoryPort playerRepository;
    private final int maxAttempts;
    private final int maxHistoryMatches;
    private final Duration refreshEvery;
    private final Duration inactiveAfter;
    private final int dormantMaxMatches;
    private final Duration dormantRefreshEvery;

    public PlayerCrawlService(CrawlQueuePort queue,
                              HaloMatchClientPort client,
                              PlayerRepositoryPort playerRepository,
                              @Value("${archivist.crawl.max-attempts:3}") int maxAttempts,
                              @Value("${archivist.crawl.max-history-matches:300}") int maxHistoryMatches,
                              @Value("${archivist.crawl.refresh-hours:24}") long refreshHours,
                              @Value("${archivist.crawl.inactive-after-days:90}") long inactiveAfterDays,
                              @Value("${archivist.crawl.dormant-max-matches:50}") int dormantMaxMatches,
                              @Value("${archivist.crawl.dormant-refresh-days:30}") long dormantRefreshDays) {
        this.queue = queue;
        this.client = client;
        this.playerRepository = playerRepository;
        this.maxAttempts = maxAttempts;
        this.maxHistoryMatches = maxHistoryMatches;
        this.refreshEvery = Duration.ofHours(refreshHours);
        this.inactiveAfter = Duration.ofDays(inactiveAfterDays);
        this.dormantMaxMatches = dormantMaxMatches;
        this.dormantRefreshEvery = Duration.ofDays(dormantRefreshDays);
    }

    @Override
    public Reports.Crawl crawl(int maxPlayers, int maxPagesPerPlayer) {
        int players = 0;
        int pages = 0;
        int newMatches = 0;
        int completed = 0;
        boolean stopped = false;
        int consecutiveServerErrors = 0;

        for (int i = 0; i < maxPlayers; i++) {
            Optional<CrawlPlayer> next = queue.claimNext(Instant.now());
            if (next.isEmpty()) break;
            CrawlPlayer player = next.get();
            players++;
            try {
                Outcome outcome = crawlOne(player, maxPagesPerPlayer);
                consecutiveServerErrors = 0;
                pages += outcome.pages;
                newMatches += outcome.newMatches;
                if (outcome.completed) completed++;
            } catch (PlayerNotFoundException e) {
                queue.markNotFound(player.id(), "La Halo API no conoce este gamertag");
                log.warn("Rastreo: la Halo API no conoce a '{}'", player.gamertag());
            } catch (HaloApiTransientException e) {
                // Un 5xx puede ser de este jugador y no de toda la API, y puede pasar solo: el jugador va al final de la
                // cola (sin gastar intentos ni quedar excluido) y se sigue con el siguiente. Si fallan varios seguidos,
                // la API está caída de verdad y se corta la corrida.
                queue.postpone(player.id(), truncate(e.getMessage()));
                log.warn("Rastreo: '{}' devolvió un error del servidor: {}", player.gamertag(), e.getMessage());
                if (++consecutiveServerErrors >= MAX_CONSECUTIVE_SERVER_ERRORS) {
                    stopped = true;
                    log.warn("Rastreo detenido: {} errores del servidor seguidos", consecutiveServerErrors);
                    break;
                }
                continue;
            } catch (HaloApiUnavailableException e) {
                stopped = true;
                log.warn("Rastreo detenido: {}", e.getMessage());
                break;
            } catch (RuntimeException e) {
                queue.markFailed(player.id(), truncate(e.getMessage()), maxAttempts);
                log.error("Rastreo: error con '{}': {}", player.gamertag(), e.getMessage(), e);
            }
        }
        return new Reports.Crawl(players, pages, newMatches, completed, stopped);
    }

    private Outcome crawlOne(CrawlPlayer cp, int maxPages) {
        boolean refresh = cp.historyComplete();
        int pageLimit = refresh ? MAX_REFRESH_PAGES : Math.max(maxPages, 1);
        int cursor = refresh ? 0 : cp.historyCursor();
        int pageSize = HaloMatchClientPort.MAX_PAGE_SIZE;
        Player player = new Player(cp.gamertag(), null, null);

        int pages = 0;
        int added = 0;
        boolean finished = false;
        boolean dormant = false;
        int historyCap = maxHistoryMatches;
        while (pages < pageLimit) {
            HistoryPage page = client.fetchHistoryPage(cp.gamertag(), cursor, pageSize, true);
            if (!refresh && pages == 0 && cursor == 0 && isDormant(page)) {
                dormant = true;
                historyCap = Math.min(maxHistoryMatches, dormantMaxMatches);
            }
            pages++;
            player.setLastSync(Instant.now());
            int saved = playerRepository.savePlayerHistory(player, page.matches());
            added += saved;
            cursor += page.received();

            if (page.isLast(pageSize)) { finished = true; break; }
            if (refresh && saved == 0) { finished = true; break; }          // ya llegó a lo conocido
            if (!refresh && cursor >= historyCap) { finished = true; break; }
        }

        if (finished || refresh) {
            queue.markCompleted(cp.id(), added, Instant.now().plus(dormant ? dormantRefreshEvery : refreshEvery));
            log.info("Rastreo: '{}' al día ({} páginas, {} partidas nuevas{})", cp.gamertag(), pages, added,
                    dormant ? ", sin actividad reciente: historial limitado" : "");
            return new Outcome(pages, added, true);
        }
        queue.saveProgress(cp.id(), cursor, added);
        log.info("Rastreo: '{}' sigue en la posición {} ({} páginas, {} partidas nuevas)", cp.gamertag(), cursor, pages, added);
        return new Outcome(pages, added, false);
    }

    /** La página más reciente del historial no trae nada posterior a {@code inactiveAfter}. */
    private boolean isDormant(HistoryPage page) {
        Instant newest = page.matches().stream()
                .map(pm -> pm.getMatch() == null ? null : pm.getMatch().getStartDate())
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        return newest != null && newest.isBefore(Instant.now().minus(inactiveAfter));
    }

    private static String truncate(String message) {
        if (message == null) return "error sin mensaje";
        return message.length() > 250 ? message.substring(0, 250) : message;
    }

    private record Outcome(int pages, int newMatches, boolean completed) {}
}
