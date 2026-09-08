package br.edu.so.photolab.metrics;

import br.edu.so.photolab.core.RenderStrategy;

public record RunResult(
        int threads,
        int tileSize,
        RenderStrategy strategy,
        long elapsedMs,
        int tileCount,
        int tilesCompleted,
        boolean cancelled
) {
    public double speedupAgainst(long baselineMs) {
        if (cancelled || elapsedMs <= 0 || baselineMs <= 0) {
            return Double.NaN;
        }
        return baselineMs / (double) elapsedMs;
    }

    public double efficiencyAgainst(long baselineMs) {
        double speedup = speedupAgainst(baselineMs);
        if (Double.isNaN(speedup) || threads <= 0) {
            return Double.NaN;
        }
        return speedup / threads;
    }
}
