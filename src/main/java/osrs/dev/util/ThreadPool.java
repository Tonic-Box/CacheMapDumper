package osrs.dev.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * A simple wrapper around an {@link ExecutorService} to make it easier to submit tasks and retrieve their results.
 */
public class ThreadPool
{
    private final static ExecutorService executor = Executors.newCachedThreadPool();

    public static Future<?> submit(Runnable runnable)
    {
        return executor.submit(runnable);
    }

    public static void shutdown()
    {
        executor.shutdown();
    }
}