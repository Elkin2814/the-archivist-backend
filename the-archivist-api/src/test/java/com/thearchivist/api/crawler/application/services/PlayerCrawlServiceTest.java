package com.thearchivist.api.crawler.application.services;

import com.thearchivist.api.crawler.domain.enums.CrawlStatus;
import com.thearchivist.api.crawler.domain.models.CrawlPlayer;
import com.thearchivist.api.crawler.domain.models.Reports;
import com.thearchivist.api.crawler.domain.ports.out.CrawlQueuePort;
import com.thearchivist.api.player.domain.exceptions.HaloApiTransientException;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import com.thearchivist.api.player.domain.exceptions.PlayerNotFoundException;
import com.thearchivist.api.player.domain.models.HistoryPage;
import com.thearchivist.api.player.domain.models.Match;
import com.thearchivist.api.player.domain.models.PlayerMatch;
import com.thearchivist.api.player.domain.ports.out.HaloMatchClientPort;
import com.thearchivist.api.player.domain.ports.out.PlayerRepositoryPort;
import com.thearchivist.api.stats.domain.enums.GameMode;
import com.thearchivist.api.stats.domain.enums.MatchType;
import com.thearchivist.api.stats.domain.enums.StateMatch;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El recorrido del historial con ports en memoria: la Halo API simulada entrega {@code total} partidas. */
class PlayerCrawlServiceTest {

    /** Cola en memoria (un jugador, o varios en orden) que anota qué se le hizo. */
    private static class FakeQueue {
        final List<String> events = new ArrayList<>();
        Instant lastNextCrawl;
        private final java.util.ArrayDeque<CrawlPlayer> waiting = new java.util.ArrayDeque<>();

        FakeQueue(CrawlPlayer player) { waiting.add(player); }

        FakeQueue(List<CrawlPlayer> players) { waiting.addAll(players); }

        CrawlQueuePort port() {
            return (CrawlQueuePort) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{CrawlQueuePort.class}, (p, m, a) -> switch (m.getName()) {
                        case "claimNext" -> Optional.ofNullable(waiting.poll());
                        case "saveProgress" -> { events.add("progress:" + a[1] + ":" + a[2]); yield null; }
                        case "markCompleted" -> { events.add("completed:" + a[1]); lastNextCrawl = (Instant) a[2]; yield null; }
                        case "markNotFound" -> { events.add("notFound"); yield null; }
                        case "markFailed" -> { events.add("failed"); yield null; }
                        case "postpone" -> { events.add("postponed"); yield null; }
                        default -> null;
                    });
        }
    }

    /** Historial de {@code total} partidas (ids estables); {@code pagesServed} cuenta las llamadas. */
    private static class FakeHalo {
        final int total;
        int pagesServed;
        RuntimeException failure;
        Instant newestMatch = Instant.now();   // fecha de la partida más reciente; las siguientes son anteriores

        FakeHalo(int total) { this.total = total; }

        HaloMatchClientPort port() {
            return (HaloMatchClientPort) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{HaloMatchClientPort.class}, (p, m, a) -> {
                        if (!m.getName().equals("fetchHistoryPage")) return null;
                        pagesServed++;
                        if (failure != null) throw failure;
                        int start = (Integer) a[1];
                        int count = Math.min((Integer) a[2], HaloMatchClientPort.MAX_PAGE_SIZE);
                        assertTrue((Boolean) a[3], "el rastreo debe pedir solo matchmaking");
                        int end = Math.min(start + count, total);
                        List<PlayerMatch> page = new ArrayList<>();
                        for (int i = start; i < end; i++) page.add(match(i, newestMatch.minusSeconds(3600L * i)));
                        return new HistoryPage(page, page.size());
                    });
        }
    }

    private static PlayerMatch match(int i, Instant date) {
        PlayerMatch pm = new PlayerMatch();
        pm.setMatch(new Match(new UUID(0, i), MatchType.MATCHMAKING, GameMode.DEATHMATCH, null, null, "map", date));
        pm.setStateMatch(StateMatch.VICTORY);
        pm.setLeaderId(1);
        return pm;
    }

    /** Repositorio que "recuerda" las partidas ya guardadas y devuelve cuántas eran nuevas. */
    private static PlayerRepositoryPort repo(Set<UUID> known) {
        return (PlayerRepositoryPort) Proxy.newProxyInstance(PlayerCrawlServiceTest.class.getClassLoader(),
                new Class<?>[]{PlayerRepositoryPort.class}, (p, m, a) -> {
                    if (!m.getName().equals("savePlayerHistory")) return null;
                    int added = 0;
                    for (PlayerMatch pm : (List<PlayerMatch>) a[1]) {
                        if (known.add(pm.getMatch().getMatchId())) added++;
                    }
                    return added;
                });
    }

    private static CrawlPlayer player(int cursor, boolean complete) {
        return new CrawlPlayer(7, "Tester", CrawlStatus.PENDING, cursor, complete, 0);
    }

    private static PlayerCrawlService service(FakeQueue q, FakeHalo h, Set<UUID> known, int maxHistory) {
        return new PlayerCrawlService(q.port(), h.port(), repo(known), 3, maxHistory, 24, 90, 25, 30);
    }

    @Test
    void firstCrawlWalksWholeHistoryAndMarksItComplete() {
        FakeQueue q = new FakeQueue(player(0, false));
        FakeHalo halo = new FakeHalo(60);
        Reports.Crawl r = service(q, halo, new HashSet<>(), 1000).crawl(5, 10);
        assertEquals(3, halo.pagesServed);                 // 25 + 25 + 10 (la última trae menos de 25)
        assertEquals(60, r.newMatches());
        assertEquals(1, r.completed());
        assertEquals(List.of("completed:60"), q.events);
    }

    @Test
    void runEndingEarlySavesProgressAndKeepsPlayerPending() {
        FakeQueue q = new FakeQueue(player(0, false));
        FakeHalo halo = new FakeHalo(200);
        Reports.Crawl r = service(q, halo, new HashSet<>(), 1000).crawl(5, 2);
        assertEquals(2, halo.pagesServed);
        assertEquals(0, r.completed());
        assertEquals(List.of("progress:50:50"), q.events);
    }

    @Test
    void crawlResumesFromTheSavedCursor() {
        FakeQueue q = new FakeQueue(player(50, false));
        FakeHalo halo = new FakeHalo(60);
        Set<UUID> known = new HashSet<>();
        Reports.Crawl r = service(q, halo, known, 1000).crawl(5, 10);
        assertEquals(1, halo.pagesServed);                 // solo quedaban 10 por recorrer
        assertEquals(10, r.newMatches());
        assertFalse(known.contains(new UUID(0, 0)), "no debe volver a pedir lo ya recorrido");
        assertEquals(List.of("completed:10"), q.events);
    }

    @Test
    void historyCapStopsTheFirstCrawl() {
        FakeQueue q = new FakeQueue(player(0, false));
        FakeHalo halo = new FakeHalo(500);
        Reports.Crawl r = service(q, halo, new HashSet<>(), 50).crawl(5, 10);
        assertEquals(2, halo.pagesServed);
        assertEquals(50, r.newMatches());
        assertEquals(1, r.completed());
    }

    @Test
    void dormantPlayerKeepsOnlyAShortHistoryAndIsRevisitedMuchLater() {
        FakeQueue q = new FakeQueue(player(0, false));
        FakeHalo halo = new FakeHalo(500);
        halo.newestMatch = Instant.now().minus(java.time.Duration.ofDays(400));   // dejó de jugar hace más de un año
        Reports.Crawl r = service(q, halo, new HashSet<>(), 300).crawl(5, 10);
        assertEquals(1, halo.pagesServed);                 // tope de 25 partidas = una sola página
        assertEquals(25, r.newMatches());
        assertEquals(1, r.completed());
        assertTrue(q.lastNextCrawl.isAfter(Instant.now().plus(java.time.Duration.ofDays(29))), "revisión espaciada");
    }

    @Test
    void activePlayerKeepsTheFullHistoryCapAndDailyRefresh() {
        FakeQueue q = new FakeQueue(player(0, false));
        FakeHalo halo = new FakeHalo(500);                 // partida más reciente: ahora
        Reports.Crawl r = service(q, halo, new HashSet<>(), 100).crawl(5, 10);
        assertEquals(4, halo.pagesServed);                 // tope de 100 partidas, no el de dormidos
        assertEquals(100, r.newMatches());
        assertTrue(q.lastNextCrawl.isBefore(Instant.now().plus(java.time.Duration.ofHours(25))));
    }

    @Test
    void refreshStopsAtTheFirstPageWithNothingNew() {
        // La API entrega primero lo más reciente: las 10 primeras partidas son nuevas y las 100 siguientes ya estaban.
        Set<UUID> known = new HashSet<>();
        for (int i = 10; i < 110; i++) known.add(new UUID(0, i));
        FakeQueue q = new FakeQueue(player(0, true));
        FakeHalo halo = new FakeHalo(110);
        Reports.Crawl r = service(q, halo, known, 1000).crawl(5, 10);
        // Página 1 (0..24): 10 nuevas. Página 2 (25..49): ninguna -> ya llegó a lo conocido y se detiene.
        assertEquals(2, halo.pagesServed);
        assertEquals(10, r.newMatches());
        assertEquals(1, r.completed());
    }

    @Test
    void apiUnavailableStopsTheRunWithoutMarkingThePlayer() {
        FakeQueue q = new FakeQueue(player(0, false));
        FakeHalo halo = new FakeHalo(60);
        halo.failure = new HaloApiUnavailableException("429", null);
        Reports.Crawl r = service(q, halo, new HashSet<>(), 1000).crawl(5, 10);
        assertTrue(r.stoppedByApi());
        assertTrue(q.events.isEmpty());
    }

    @Test
    void serverErrorOfOnePlayerPostponesItWithoutExcludingAndTheCrawlMovesOnToTheNext() {
        // El primer jugador da 503 siempre; el segundo se recorre bien: la cola no se queda atascada en el primero.
        FakeQueue q = new FakeQueue(List.of(player(0, false), new CrawlPlayer(8, "Otro", CrawlStatus.PENDING, 0, false, 0)));
        FakeHalo halo = new FakeHalo(30);
        halo.failure = new HaloApiTransientException("HTTP 503", null);
        PlayerCrawlService service = service(q, halo, new HashSet<>(), 1000);
        Reports.Crawl first = service.crawl(1, 10);
        assertFalse(first.stoppedByApi());
        assertEquals(List.of("postponed"), q.events);

        halo.failure = null;
        Reports.Crawl second = service.crawl(1, 10);
        assertEquals(30, second.newMatches());
        assertEquals(List.of("postponed", "completed:30"), q.events);
    }

    @Test
    void threeServerErrorsInARowStopTheRunBecauseTheApiIsDown() {
        List<CrawlPlayer> five = new ArrayList<>();
        for (int i = 0; i < 5; i++) five.add(new CrawlPlayer(i + 1, "P" + i, CrawlStatus.PENDING, 0, false, 0));
        FakeQueue q = new FakeQueue(five);
        FakeHalo halo = new FakeHalo(30);
        halo.failure = new HaloApiTransientException("HTTP 503", null);
        Reports.Crawl r = service(q, halo, new HashSet<>(), 1000).crawl(5, 10);
        assertTrue(r.stoppedByApi());
        assertEquals(3, halo.pagesServed);
        assertEquals(List.of("postponed", "postponed", "postponed"), q.events);
    }

    @Test
    void unknownGamertagIsMarkedNotFound() {
        FakeQueue q = new FakeQueue(player(0, false));
        FakeHalo halo = new FakeHalo(60);
        halo.failure = new PlayerNotFoundException("Tester");
        Reports.Crawl r = service(q, halo, new HashSet<>(), 1000).crawl(5, 10);
        assertFalse(r.stoppedByApi());
        assertEquals(List.of("notFound"), q.events);
    }

    @Test
    void unexpectedErrorCountsAnAttemptAndContinues() {
        FakeQueue q = new FakeQueue(player(0, false));
        FakeHalo halo = new FakeHalo(60);
        halo.failure = new IllegalStateException("boom");
        Reports.Crawl r = service(q, halo, new HashSet<>(), 1000).crawl(5, 10);
        assertFalse(r.stoppedByApi());
        assertEquals(List.of("failed"), q.events);
    }
}
