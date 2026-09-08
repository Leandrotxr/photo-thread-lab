package br.edu.so.photolab.core;

public interface TileRenderer {

    void renderTile(Tile tile, PixelBuffer src, PixelBuffer dst);
}
