package br.edu.so.photolab.core;

public final class WorkerIds {

    private static final ThreadLocal<Integer> ID = new ThreadLocal<>();

    private WorkerIds() {
    }

    public static void set(int workerId) {
        ID.set(workerId);
    }

    public static int current() {
        Integer value = ID.get();
        return value == null ? 0 : value;
    }

    public static void clear() {
        ID.remove();
    }
}
