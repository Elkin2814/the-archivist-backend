package com.thearchivist.api.stats.domain.models;

import com.thearchivist.api.stats.domain.models.WinRate.Confidence;
import com.thearchivist.api.stats.domain.models.WinRate.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Intervalo de Wilson al 95 %: los valores de referencia salen de la fórmula calculada a mano. */
class WinRateTest {

    private static final double DELTA = 0.001;

    @Test
    void emptySampleHasNoRateAndInsufficientData() {
        WinRate wr = new WinRate(0, 0);
        assertEquals(0.0, wr.rate(), DELTA);
        assertEquals(0.0, wr.low(), DELTA);
        assertEquals(0.0, wr.high(), DELTA);
        assertEquals(Verdict.INSUFFICIENT_DATA, wr.verdict(30));
        assertEquals(Confidence.LOW, wr.confidence());
    }

    @Test
    void evenHundredGamesIntervalIsAroundFiftyPercent() {
        WinRate wr = new WinRate(100, 50);
        assertEquals(0.5, wr.rate(), DELTA);
        assertEquals(0.404, wr.low(), DELTA);
        assertEquals(0.596, wr.high(), DELTA);
        assertEquals(Verdict.AVERAGE, wr.verdict(30));
    }

    @Test
    void threeWinsInThreeIsNotTreatedAsHundredPercent() {
        WinRate wr = new WinRate(3, 3);
        assertEquals(1.0, wr.rate(), DELTA);
        assertEquals(0.439, wr.low(), DELTA);
        assertTrue(wr.high() <= 1.0 + DELTA);
        // Con el mínimo de muestra habitual no se emite veredicto; aun relajándolo, el 100 % "crudo" no basta.
        assertEquals(Verdict.INSUFFICIENT_DATA, wr.verdict(30));
        assertEquals(Verdict.AVERAGE, wr.verdict(1));
    }

    @Test
    void clearEdgeGivesAboveOrBelowAverage() {
        assertEquals(Verdict.ABOVE_AVERAGE, new WinRate(1000, 600).verdict(30));
        assertEquals(Verdict.BELOW_AVERAGE, new WinRate(1000, 400).verdict(30));
    }

    @Test
    void intervalNeverLeavesZeroToOne() {
        for (long n : new long[]{1, 2, 5, 50, 5000}) {
            for (long w : new long[]{0, n / 2, n}) {
                WinRate wr = new WinRate(n, w);
                assertTrue(wr.low() >= -DELTA && wr.high() <= 1 + DELTA, "n=" + n + " w=" + w);
                assertTrue(wr.low() <= wr.rate() + DELTA && wr.rate() <= wr.high() + DELTA, "n=" + n + " w=" + w);
            }
        }
    }

    @Test
    void confidenceGrowsWithSampleSize() {
        assertEquals(Confidence.LOW, new WinRate(30, 15).confidence());
        assertEquals(Confidence.MEDIUM, new WinRate(100, 50).confidence());
        assertEquals(Confidence.HIGH, new WinRate(1000, 500).confidence());
    }

    @Test
    void percentagesRoundToOneDecimal() {
        assertEquals(66.7, WinRate.pct(2.0 / 3.0), 0.0001);
        assertEquals(50.0, new WinRate(100, 50).ratePct(), 0.0001);
    }
}
