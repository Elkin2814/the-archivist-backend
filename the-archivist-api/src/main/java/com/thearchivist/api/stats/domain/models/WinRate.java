package com.thearchivist.api.stats.domain.models;

/**
 * Proporción de victorias con su intervalo de confianza de Wilson al 95 %. Con pocas partidas el porcentaje
 * "crudo" engaña (3 de 3 = 100 %); el intervalo dice cuánto se puede confiar en él.
 *
 * <p>Supuesto: las partidas se tratan como independientes. En partidas por equipos los compañeros comparten
 * resultado, así que el intervalo real es algo más ancho; sirve para ordenar y descartar muestras pequeñas,
 * no como prueba estadística exacta.
 */
public record WinRate(long matches, long wins) {

    private static final double Z = 1.96;

    /** Cómo se compara con el 50 % esperado (en partidas con equipos iguales la media de todos es 50 %). */
    public enum Verdict { ABOVE_AVERAGE, AVERAGE, BELOW_AVERAGE, INSUFFICIENT_DATA }

    /** Qué tan fiable es el porcentaje según lo ancho que quedó el intervalo (menos de 12 puntos = alta). */
    public enum Confidence { HIGH, MEDIUM, LOW }

    public double rate() {
        return matches == 0 ? 0 : (double) wins / matches;
    }

    public double low() {
        return matches == 0 ? 0 : center() - margin();
    }

    public double high() {
        return matches == 0 ? 0 : center() + margin();
    }

    private double center() {
        double n = matches;
        return (rate() + Z * Z / (2 * n)) / (1 + Z * Z / n);
    }

    private double margin() {
        double n = matches;
        double p = rate();
        return Z * Math.sqrt(p * (1 - p) / n + Z * Z / (4 * n * n)) / (1 + Z * Z / n);
    }

    public Confidence confidence() {
        if (matches == 0) return Confidence.LOW;
        double width = high() - low();
        if (width < 0.12) return Confidence.HIGH;
        if (width < 0.25) return Confidence.MEDIUM;
        return Confidence.LOW;
    }

    /** Porcentajes con un decimal, para mostrar. */
    public double ratePct() { return pct(rate()); }

    public double lowPct() { return pct(low()); }

    public double highPct() { return pct(high()); }

    public static double pct(double fraction) {
        return Math.round(fraction * 1000.0) / 10.0;
    }

    public Verdict verdict(long minMatches) {
        if (matches < Math.max(minMatches, 1)) return Verdict.INSUFFICIENT_DATA;
        if (low() > 0.5) return Verdict.ABOVE_AVERAGE;
        if (high() < 0.5) return Verdict.BELOW_AVERAGE;
        return Verdict.AVERAGE;
    }
}
