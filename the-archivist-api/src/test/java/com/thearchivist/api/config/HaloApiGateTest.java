package com.thearchivist.api.config;

import com.thearchivist.api.player.domain.exceptions.HaloApiTransientException;
import com.thearchivist.api.player.domain.exceptions.HaloApiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaloApiGateTest {

    private static RestClientResponseException http(int status, String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfter != null) headers.add("Retry-After", retryAfter);
        return new RestClientResponseException("HTTP " + status, status, "x", headers, null, null);
    }

    @Test
    void callsAreSpacedByTheMinimumInterval() {
        HaloApiGate gate = new HaloApiGate(80);
        long start = System.nanoTime();
        gate.acquire();
        gate.acquire();
        gate.acquire();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        // La primera pasa sin espera; las otras dos esperan 80 ms cada una.
        assertTrue(elapsedMs >= 150, "esperado >= 150 ms, fue " + elapsedMs);
    }

    @Test
    void zeroIntervalNeverWaits() {
        HaloApiGate gate = new HaloApiGate(0);
        long start = System.nanoTime();
        for (int i = 0; i < 50; i++) gate.acquire();
        assertTrue((System.nanoTime() - start) / 1_000_000 < 500);
    }

    @Test
    void backoffDelaysTheNextCallForEveryone() {
        HaloApiGate gate = new HaloApiGate(0);
        gate.acquire();
        gate.backoff(Duration.ofMillis(250));
        long start = System.nanoTime();
        gate.acquire();
        long waitedMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(waitedMs >= 200, "esperado >= 200 ms, fue " + waitedMs);
    }

    @Test
    void retryAfterIsReadFromTheHeaderAndDefaultsWhenMissingOrInvalid() {
        assertEquals(Duration.ofSeconds(7), HaloApiGate.retryAfter(http(429, "7")));
        assertEquals(Duration.ofSeconds(15), HaloApiGate.retryAfter(http(429, null)));
        assertEquals(Duration.ofSeconds(15), HaloApiGate.retryAfter(http(429, "Wed, 21 Oct 2026 07:28:00 GMT")));
        assertEquals(Duration.ofSeconds(15), HaloApiGate.retryAfter(http(429, "0")));
    }

    @Test
    void errorsAreTranslatedWithoutExposingTechnicalDetailsAsStatus() {
        HaloApiGate gate = new HaloApiGate(0);
        HaloApiUnavailableException keyError = gate.unavailable(401, http(401, null));
        assertTrue(keyError.getMessage().contains("HALO_API_KEY"));
        assertTrue(gate.unavailable(503, http(503, null)).getMessage().contains("503"));
    }

    @Test
    void serverErrorsAreTransientButKeyAndRateLimitErrorsAreNot() {
        HaloApiGate gate = new HaloApiGate(0);
        for (int status : new int[]{500, 502, 503, 504}) {
            assertTrue(gate.unavailable(status, http(status, null)) instanceof HaloApiTransientException, "HTTP " + status);
        }
        assertFalse(gate.unavailable(401, http(401, null)) instanceof HaloApiTransientException);
        assertFalse(gate.unavailable(429, http(429, "1")) instanceof HaloApiTransientException);
    }
}
