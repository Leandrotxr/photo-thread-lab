package br.edu.so.photolab.core;

public final class ThreadPalette {

    private static final int[] COLORS = {
            0xFFFF4D4D,
            0xFF4DD2FF,
            0xFF7CFF6B,
            0xFFFFD24D,
            0xFFC77DFF,
            0xFFFF8AD4,
            0xFF5A7CFF,
            0xFFFF9A4A,
            0xFF5AFFE6,
            0xFFE8FF5A,
            0xFF9AA4FF,
            0xFFFF6B8A
    };

    private ThreadPalette() {
    }

    public static int color(int workerId) {
        int index = Math.floorMod(workerId, COLORS.length);
        return COLORS[index];
    }

    public static int size() {
        return COLORS.length;
    }
}
