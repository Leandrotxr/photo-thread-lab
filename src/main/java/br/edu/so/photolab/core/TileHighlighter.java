package br.edu.so.photolab.core;

public final class TileHighlighter {

    private static final int EMPTY = 0xFF10131A;
    private static final int GRID = 0xFF2C3448;

    private TileHighlighter() {
    }

    public static void prepareCanvas(PixelBuffer dst, int tileSize) {
        dst.fill(EMPTY);
        int step = Math.max(8, tileSize);
        for (int x = 0; x < dst.width(); x += step) {
            dst.drawVerticalLine(x, GRID);
        }
        for (int y = 0; y < dst.height(); y += step) {
            dst.drawHorizontalLine(y, GRID);
        }
    }

    public static void paint(PixelBuffer dst, Tile tile, int workerId) {
        int color = ThreadPalette.color(workerId);
        dst.tintRect(tile.x(), tile.y(), tile.width(), tile.height(), color, 0.28f);
        dst.drawRect(tile.x(), tile.y(), tile.width(), tile.height(), color, 4);
    }
}
