package br.edu.so.photolab.photo;

import br.edu.so.photolab.core.PixelBuffer;
import br.edu.so.photolab.core.Tile;
import br.edu.so.photolab.core.TileRenderer;

public final class GaussianBlurRenderer implements TileRenderer {

    private final int radius;
    private final float[] kernel;

    public GaussianBlurRenderer(int radius) {
        this.radius = Math.max(0, radius);
        this.kernel = buildKernel(this.radius);
    }

    public int radius() {
        return radius;
    }

    @Override
    public void renderTile(Tile tile, PixelBuffer src, PixelBuffer dst) {
        if (radius == 0) {
            copyTile(tile, src, dst);
            return;
        }
        int maxX = tile.x() + tile.width();
        int maxY = tile.y() + tile.height();
        for (int y = tile.y(); y < maxY; y++) {
            for (int x = tile.x(); x < maxX; x++) {
                dst.set(x, y, blurPixel(src, x, y));
            }
        }
    }

    private int blurPixel(PixelBuffer src, int px, int py) {
        float rAcc = 0;
        float gAcc = 0;
        float bAcc = 0;
        float wAcc = 0;
        int center = src.get(px, py);
        int alpha = (center >>> 24) & 0xFF;
        for (int ky = -radius; ky <= radius; ky++) {
            float wy = kernel[ky + radius];
            for (int kx = -radius; kx <= radius; kx++) {
                float w = wy * kernel[kx + radius];
                int pixel = src.getClamped(px + kx, py + ky);
                rAcc += w * ((pixel >>> 16) & 0xFF);
                gAcc += w * ((pixel >>> 8) & 0xFF);
                bAcc += w * (pixel & 0xFF);
                wAcc += w;
            }
        }
        int r = clamp(Math.round(rAcc / wAcc));
        int g = clamp(Math.round(gAcc / wAcc));
        int b = clamp(Math.round(bAcc / wAcc));
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private static void copyTile(Tile tile, PixelBuffer src, PixelBuffer dst) {
        int maxX = tile.x() + tile.width();
        int maxY = tile.y() + tile.height();
        for (int y = tile.y(); y < maxY; y++) {
            for (int x = tile.x(); x < maxX; x++) {
                dst.set(x, y, src.get(x, y));
            }
        }
    }

    private static float[] buildKernel(int radius) {
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        if (radius == 0) {
            kernel[0] = 1f;
            return kernel;
        }
        float sigma = Math.max(0.6f, radius / 2.5f);
        float twoSigmaSq = 2f * sigma * sigma;
        float sum = 0f;
        for (int i = 0; i < size; i++) {
            int x = i - radius;
            float value = (float) Math.exp(-(x * x) / twoSigmaSq);
            kernel[i] = value;
            sum += value;
        }
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
