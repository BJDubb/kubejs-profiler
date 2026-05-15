package au.beckam.kubejsprofiler.trace;

import java.util.concurrent.atomic.AtomicLong;

public final class FunctionProfileStats {

    private final AtomicLong totalNanos = new AtomicLong();
    private final AtomicLong callCount = new AtomicLong();
    private final AtomicLong maxNanos = new AtomicLong();

    public void record(long durationNanos) {
        totalNanos.addAndGet(durationNanos);
        callCount.incrementAndGet();
        maxNanos.accumulateAndGet(durationNanos, Math::max);
    }

    public long totalNanos() {
        return totalNanos.get();
    }

    public long callCount() {
        return callCount.get();
    }

    public long maxNanos() {
        return maxNanos.get();
    }

    public double totalMs() {
        return totalNanos() / 1_000_000.0;
    }

    public double averageMs() {
        long count = callCount();

        if (count == 0) {
            return 0.0;
        }

        return totalMs() / count;
    }

    public double maxMs() {
        return maxNanos() / 1_000_000.0;
    }
}