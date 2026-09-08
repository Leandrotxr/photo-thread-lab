package br.edu.so.photolab.core;

public enum RenderStrategy {
    POOL("Pool de N threads"),
    THREAD_PER_TILE("Uma thread por tile"),
    STATIC_STRIPS("Faixas estaticas");

    private final String label;

    RenderStrategy(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
