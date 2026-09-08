package br.edu.so.photolab.core;

public record RenderRequest(
        int threadCount,
        int tileSize,
        RenderStrategy strategy,
        TileRenderer renderer,
        boolean highlightThreads,
        int visualDelayMs
) {
    public RenderRequest {
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount deve ser >= 1");
        }
        if (tileSize < 1) {
            throw new IllegalArgumentException("tileSize deve ser >= 1");
        }
        if (strategy == null || renderer == null) {
            throw new IllegalArgumentException("strategy/renderer obrigatorios");
        }
        if (visualDelayMs < 0) {
            throw new IllegalArgumentException("visualDelayMs deve ser >= 0");
        }
    }
}
