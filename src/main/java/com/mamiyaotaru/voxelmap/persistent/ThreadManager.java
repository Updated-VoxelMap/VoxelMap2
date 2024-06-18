package com.mamiyaotaru.voxelmap.persistent;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadManager {
    static final int concurrentThreads = Math.min(Math.max(Runtime.getRuntime().availableProcessors() / 2, 1), 4);
    static final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    public static final ThreadPoolExecutor executorService = new ThreadPoolExecutor(0, concurrentThreads, 60L, TimeUnit.SECONDS, queue);
    public static final ThreadPoolExecutor saveExecutorService = new ThreadPoolExecutor(0, concurrentThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private ThreadManager() {}

    public static void emptyQueue() {
        for (Runnable runnable : queue) {
            if (runnable instanceof FutureTask) {
                ((FutureTask<?>) runnable).cancel(false);
            }
        }

        executorService.purge();
    }

    static {
        executorService.setThreadFactory(new NamedThreadFactory("Voxelmap WorldMap Calculation Thread"));
        saveExecutorService.setThreadFactory(new NamedThreadFactory("Voxelmap WorldMap Saver Thread"));
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String name;
        private final AtomicInteger threadCount = new AtomicInteger(1);

        private NamedThreadFactory(String name) { this.name = name; }

        public Thread newThread(@NotNull Runnable r) { return new Thread(r, this.name + " " + this.threadCount.getAndIncrement()); }
    }
}