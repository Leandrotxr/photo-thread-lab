package br.edu.so.photolab.core;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public final class PixelBuffer {

    private final int width;
    private final int height;
    private final int[] argb;

    public PixelBuffer(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("dimensoes invalidas");
        }
        this.width = width;
        this.height = height;
        this.argb = new int[Math.multiplyExact(width, height)];
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int get(int x, int y) {
        return argb[y * width + x];
    }

    public int getClamped(int x, int y) {
        int cx = Math.max(0, Math.min(width - 1, x));
        int cy = Math.max(0, Math.min(height - 1, y));
        return argb[cy * width + cx];
    }

    public void set(int x, int y, int color) {
        argb[y * width + x] = color;
    }

    public void fill(int color) {
        java.util.Arrays.fill(argb, color);
    }

    public void drawHorizontalLine(int y, int color) {
        if (y < 0 || y >= height) {
            return;
        }
        int row = y * width;
        for (int x = 0; x < width; x++) {
            argb[row + x] = color;
        }
    }

    public void drawVerticalLine(int x, int color) {
        if (x < 0 || x >= width) {
            return;
        }
        for (int y = 0; y < height; y++) {
            argb[y * width + x] = color;
        }
    }

    public void drawRect(int x, int y, int w, int h, int color, int thickness) {
        int maxX = Math.min(width, x + w);
        int maxY = Math.min(height, y + h);
        int minX = Math.max(0, x);
        int minY = Math.max(0, y);
        int t = Math.max(1, thickness);
        for (int py = minY; py < maxY; py++) {
            for (int px = minX; px < maxX; px++) {
                boolean edge = px < minX + t || px >= maxX - t || py < minY + t || py >= maxY - t;
                if (edge) {
                    argb[py * width + px] = color;
                }
            }
        }
    }

    public void tintRect(int x, int y, int w, int h, int color, float amount) {
        int maxX = Math.min(width, x + w);
        int maxY = Math.min(height, y + h);
        int minX = Math.max(0, x);
        int minY = Math.max(0, y);
        int tr = (color >>> 16) & 0xFF;
        int tg = (color >>> 8) & 0xFF;
        int tb = color & 0xFF;
        for (int py = minY; py < maxY; py++) {
            for (int px = minX; px < maxX; px++) {
                int pixel = argb[py * width + px];
                int r = Math.round(((pixel >>> 16) & 0xFF) * (1f - amount) + tr * amount);
                int g = Math.round(((pixel >>> 8) & 0xFF) * (1f - amount) + tg * amount);
                int b = Math.round((pixel & 0xFF) * (1f - amount) + tb * amount);
                argb[py * width + px] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    public PixelBuffer copy() {
        PixelBuffer clone = new PixelBuffer(width, height);
        System.arraycopy(argb, 0, clone.argb, 0, argb.length);
        return clone;
    }

    public void copyFrom(PixelBuffer other) {
        if (other.width != width || other.height != height) {
            throw new IllegalArgumentException("tamanhos diferentes");
        }
        System.arraycopy(other.argb, 0, argb, 0, argb.length);
    }

    public void writeTo(WritableImage image) {
        PixelWriter writer = image.getPixelWriter();
        writer.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), argb, 0, width);
    }

    public static PixelBuffer fromBufferedImage(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage argbImage = image;
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            argbImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = argbImage.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }
        PixelBuffer buffer = new PixelBuffer(w, h);
        argbImage.getRGB(0, 0, w, h, buffer.argb, 0, w);
        return buffer;
    }
}
