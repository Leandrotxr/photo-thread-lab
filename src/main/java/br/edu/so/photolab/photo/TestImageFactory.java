package br.edu.so.photolab.photo;

import br.edu.so.photolab.core.PixelBuffer;

public final class TestImageFactory {

    private TestImageFactory() {
    }

    public static PixelBuffer colorful(int width, int height) {
        PixelBuffer buffer = new PixelBuffer(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = (int) (90 + 80 * Math.sin(x * 0.018));
                int g = (int) (70 + 90 * Math.sin(y * 0.015));
                int b = (int) (110 + 90 * Math.sin((x + y) * 0.011));
                if (((x / 64) + (y / 64)) % 2 == 0) {
                    r = Math.min(255, r + 50);
                    g = Math.min(255, g + 20);
                }
                if ((x / 8 + y / 8) % 7 == 0) {
                    r = 255;
                    g = 240;
                    b = 210;
                }
                int cx = x - width / 2;
                int cy = y - height / 2;
                int dist = (int) Math.sqrt(cx * cx + cy * cy);
                if (dist % 42 < 6) {
                    r = 40;
                    g = 210;
                    b = 255;
                }
                buffer.set(x, y, 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
            }
        }
        return buffer;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
