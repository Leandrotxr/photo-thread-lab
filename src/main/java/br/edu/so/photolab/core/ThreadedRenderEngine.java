package br.edu.so.photolab.core;

import br.edu.so.photolab.metrics.RunResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class ThreadedRenderEngine {

    public RunResult render(
            RenderRequest request,
            PixelBuffer src,
            PixelBuffer dst,
            AtomicBoolean cancel,
            AtomicInteger tilesCompleted,
            AtomicReference<String> lastEvent
    ) {
        List<Tile> tiles = switch (request.strategy()) {
            case STATIC_STRIPS -> Tile.strips(src.width(), src.height(), request.threadCount());
            case POOL, THREAD_PER_TILE -> Tile.split(src.width(), src.height(), request.tileSize());
        };
        if (request.highlightThreads()) {
            TileHighlighter.prepareCanvas(dst, request.tileSize());
        } else {
            dst.copyFrom(src);
        }
        tilesCompleted.set(0);
        long started = System.nanoTime();
        try {
            switch (request.strategy()) {
                case THREAD_PER_TILE -> runOneThreadPerTile(request, src, dst, tiles, cancel, tilesCompleted, lastEvent);
                case POOL, STATIC_STRIPS -> runPool(request, src, dst, tiles, cancel, tilesCompleted, lastEvent);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancel.set(true);
        }
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        return new RunResult(
                request.threadCount(),
                request.tileSize(),
                request.strategy(),
                elapsedMs,
                tiles.size(),
                tilesCompleted.get(),
                cancel.get()
        );
    }

    private static void runPool(
            RenderRequest request,
            PixelBuffer src,
            PixelBuffer dst,
            List<Tile> tiles,
            AtomicBoolean cancel,
            AtomicInteger tilesCompleted,
            AtomicReference<String> lastEvent
    ) throws InterruptedException {
        AtomicInteger nextId = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(request.threadCount(), runnable -> {
            int workerId = nextId.getAndIncrement();
            Thread thread = new Thread(() -> {
                WorkerIds.set(workerId);
                try {
                    runnable.run();
                } finally {
                    WorkerIds.clear();
                }
            }, "photolab-worker-" + workerId);
            thread.setDaemon(true);
            return thread;
        });
        List<Future<?>> futures = new ArrayList<>(tiles.size());
        try {
            for (Tile tile : tiles) {
                futures.add(pool.submit(() -> processTile(request, tile, src, dst, cancel, tilesCompleted, lastEvent)));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    cancel.set(true);
                    throw new IllegalStateException("falha ao renderizar tile", e.getCause());
                }
            }
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private static void runOneThreadPerTile(
            RenderRequest request,
            PixelBuffer src,
            PixelBuffer dst,
            List<Tile> tiles,
            AtomicBoolean cancel,
            AtomicInteger tilesCompleted,
            AtomicReference<String> lastEvent
    ) throws InterruptedException {
        AtomicInteger nextId = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>(tiles.size());
        for (Tile tile : tiles) {
            int workerId = nextId.getAndIncrement();
            Thread thread = new Thread(() -> {
                WorkerIds.set(workerId);
                try {
                    processTile(request, tile, src, dst, cancel, tilesCompleted, lastEvent);
                } finally {
                    WorkerIds.clear();
                }
            }, "photolab-tile-" + tile.id());
            thread.setDaemon(true);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private static void processTile(
            RenderRequest request,
            Tile tile,
            PixelBuffer src,
            PixelBuffer dst,
            AtomicBoolean cancel,
            AtomicInteger tilesCompleted,
            AtomicReference<String> lastEvent
    ) {
        if (cancel.get()) {
            return;
        }
        request.renderer().renderTile(tile, src, dst);
        int workerId = WorkerIds.current();
        if (request.highlightThreads()) {
            TileHighlighter.paint(dst, tile, workerId);
        }
        tilesCompleted.incrementAndGet();
        lastEvent.set("Thread " + workerId + " terminou tile #" + tile.id()
                + "  (" + tile.x() + ", " + tile.y() + ")");
        sleepVisually(request.visualDelayMs(), cancel);
    }

    private static void sleepVisually(int delayMs, AtomicBoolean cancel) {
        if (delayMs <= 0) {
            return;
        }
        long deadline = System.nanoTime() + delayMs * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (cancel.get()) {
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
