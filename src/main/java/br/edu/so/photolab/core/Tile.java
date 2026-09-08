package br.edu.so.photolab.core;

import java.util.ArrayList;
import java.util.List;

public record Tile(int x, int y, int width, int height, int id) {

    public static List<Tile> split(int imageWidth, int imageHeight, int tileSize) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            throw new IllegalArgumentException("imagem invalida");
        }
        int size = Math.max(1, tileSize);
        List<Tile> tiles = new ArrayList<>();
        int id = 0;
        for (int y = 0; y < imageHeight; y += size) {
            for (int x = 0; x < imageWidth; x += size) {
                int w = Math.min(size, imageWidth - x);
                int h = Math.min(size, imageHeight - y);
                tiles.add(new Tile(x, y, w, h, id++));
            }
        }
        return tiles;
    }

    public static List<Tile> strips(int imageWidth, int imageHeight, int threadCount) {
        int n = Math.max(1, threadCount);
        n = Math.min(n, imageHeight);
        List<Tile> tiles = new ArrayList<>(n);
        int base = imageHeight / n;
        int remainder = imageHeight % n;
        int y = 0;
        for (int i = 0; i < n; i++) {
            int h = base + (i < remainder ? 1 : 0);
            tiles.add(new Tile(0, y, imageWidth, h, i));
            y += h;
        }
        return tiles;
    }
}
