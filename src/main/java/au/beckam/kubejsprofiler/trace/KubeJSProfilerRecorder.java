package au.beckam.kubejsprofiler.trace;

import au.beckam.kubejsprofiler.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class KubeJSProfilerRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger("kubejs-profiler");

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final AtomicInteger TRACE_EVENT_COUNT = new AtomicInteger();
    private static final long TRACE_START_NANOS = System.nanoTime();

    private static final ConcurrentLinkedQueue<TraceEvent> EVENTS = new ConcurrentLinkedQueue<>();

    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean SHUTDOWN_WRITTEN = new AtomicBoolean(false);

    private static final ConcurrentMap<FunctionProfileKey, FunctionProfileStats> FUNCTION_STATS = new ConcurrentHashMap<>();

    private KubeJSProfilerRecorder() {
    }

    public static void recordScriptLoad(String scriptType, String file, long startNanos, long durationNanos) {
        if (!Config.enabled || !Config.traceScriptLoads) {
            return;
        }

        if (!reserveTraceSlot()) {
            return;
        }

        long tsMicros = (startNanos - TRACE_START_NANOS) / 1_000;
        long durMicros = durationNanos / 1_000;

        EVENTS.add(new TraceEvent.ScriptLoad(
                file,
                scriptType,
                tsMicros,
                durMicros,
                Thread.currentThread().getId(),
                Map.of(
                        "scriptType", scriptType,
                        "file", file,
                        "durationMs", durationNanos / 1_000_000.0
                )
        ));
    }

    public static void recordFunctionSpan(String sourceName, String functionName, int lineNumber,
                                          long startNanos, long durationNanos) {
        if (!Config.enabled) {
            return;
        }

        if (!reserveTraceSlot()) {
            return;
        }

        long tsMicros = (startNanos - TRACE_START_NANOS) / 1_000;
        long durMicros = durationNanos / 1_000;

        EVENTS.add(new TraceEvent.FunctionSpan(
                sourceName,
                functionName,
                lineNumber,
                tsMicros,
                durMicros,
                Thread.currentThread().getId()
        ));
    }

    private static boolean reserveTraceSlot() {
        int max = Config.maxTraceEvents;

        while (true) {
            int current = TRACE_EVENT_COUNT.get();

            if (current >= max) {
                return false;
            }

            if (TRACE_EVENT_COUNT.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    public static void recordFunctionAggregate(String sourceName, String functionName, int firstLine, long durationNanos) {
        if (!Config.enabled) {
            return;
        }

        FunctionProfileKey key = new FunctionProfileKey(
            sourceName,
            functionName,
            firstLine
        );

        FUNCTION_STATS
            .computeIfAbsent(key, ignored -> new FunctionProfileStats())
            .record(durationNanos);
    }

    public static List<TraceEvent> getEvents() {
        return new ArrayList<>(EVENTS);
    }

    public static int getEventCount() {
        return TRACE_EVENT_COUNT.get();
    }

    public static int getFunctionStatsCount() {
        return FUNCTION_STATS.size();
    }

    public static void writeOnShutdown() {
        if (!SHUTDOWN_WRITTEN.compareAndSet(false, true)) {
            return;
        }

        dumpToDefaultLocation();
    }

    public static Path dumpToDefaultLocation() {
        boolean haveEvents = !EVENTS.isEmpty();
        boolean haveStats = !FUNCTION_STATS.isEmpty();

        if (!haveEvents && !haveStats) {
            LOGGER.info("No KubeJS profiler data recorded.");
            return null;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        Path outputDir = Path.of("").toAbsolutePath().resolve("kubejs-profiler");

        boolean wroteAny = false;

        if (haveEvents) {
            Path tracePath = outputDir.resolve("trace-" + timestamp + ".json");

            try {
                writeTrace(tracePath);
                LOGGER.info("KubeJS profiler wrote trace: {}", tracePath);
                wroteAny = true;
            } catch (IOException e) {
                LOGGER.error("Failed to write KubeJS profiler trace", e);
            }
        }

        if (haveStats) {
            Path summaryPath = outputDir.resolve("summary-" + timestamp + ".json");

            try {
                writeSummary(summaryPath);
                LOGGER.info("KubeJS profiler wrote summary: {}", summaryPath);
                wroteAny = true;
            } catch (IOException e) {
                LOGGER.error("Failed to write KubeJS profiler summary", e);
            }
        }

        return wroteAny ? outputDir : null;
    }

    public static void writeTrace(Path path) throws IOException {
        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("{\n  \"traceEvents\": [\n");

            StringBuilder eventBuffer = new StringBuilder(512);
            boolean first = true;

            for (TraceEvent event : EVENTS) {
                eventBuffer.setLength(0);
                eventBuffer.append("    ");
                event.writeJson(eventBuffer);

                if (!first) {
                    writer.write(",\n");
                }

                writer.append(eventBuffer);
                first = false;
            }

            writer.write("\n  ]\n}\n");
        }
    }

    public static void writeSummary(Path path) throws IOException {
        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(path, summaryJson(), StandardCharsets.UTF_8);
    }

    public static void reset() {
        EVENTS.clear();
        FUNCTION_STATS.clear();
        TRACE_EVENT_COUNT.set(0);
        SHUTDOWN_WRITTEN.set(false);
    }

    public static void registerShutdownHook() {
        if (!SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                writeOnShutdown();
            } catch (Throwable throwable) {
                LOGGER.error("Failed during KubeJS profiler shutdown hook", throwable);
            }
        }, "KubeJS Profiler Trace Writer"));
    }

    private static String summaryJson() {
        int topN = Math.max(0, Config.topNFunctions);

        List<Map.Entry<FunctionProfileKey, FunctionProfileStats>> entries = new ArrayList<>(FUNCTION_STATS.entrySet());

        entries.sort(Comparator
                .comparingLong((Map.Entry<FunctionProfileKey, FunctionProfileStats> e) -> e.getValue().totalNanos())
                .thenComparingLong(e -> e.getValue().callCount())
                .reversed());

        int totalFunctions = entries.size();
        int limit = topN == 0 ? totalFunctions : Math.min(topN, totalFunctions);

        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"generatedAt\": ").append(TraceJson.string(LocalDateTime.now().toString())).append(",\n");
        json.append("  \"totalFunctions\": ").append(totalFunctions).append(",\n");
        json.append("  \"reportedFunctions\": ").append(limit).append(",\n");
        json.append("  \"functions\": [\n");

        for (int i = 0; i < limit; i++) {
            Map.Entry<FunctionProfileKey, FunctionProfileStats> entry = entries.get(i);

            FunctionProfileKey key = entry.getKey();
            FunctionProfileStats stats = entry.getValue();

            json.append("    {\n");
            json.append("      \"sourceName\": ").append(TraceJson.string(key.sourceName())).append(",\n");
            json.append("      \"functionName\": ").append(TraceJson.string(key.functionName())).append(",\n");
            json.append("      \"firstLine\": ").append(key.firstLine()).append(",\n");
            json.append("      \"callCount\": ").append(stats.callCount()).append(",\n");
            json.append("      \"totalMs\": ").append(stats.totalMs()).append(",\n");
            json.append("      \"averageMs\": ").append(stats.averageMs()).append(",\n");
            json.append("      \"maxMs\": ").append(stats.maxMs()).append("\n");
            json.append("    }");

            if (i < limit - 1) {
                json.append(",");
            }

            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        return json.toString();
    }
}
