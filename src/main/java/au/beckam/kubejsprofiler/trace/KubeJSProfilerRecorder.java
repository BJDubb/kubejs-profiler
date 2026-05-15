package au.beckam.kubejsprofiler.trace;

import au.beckam.kubejsprofiler.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class KubeJSProfilerRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger("kubejs-profiler");

    private static final AtomicInteger TRACE_EVENT_COUNT = new AtomicInteger();
    private static final long TRACE_START_NANOS = System.nanoTime();
    private static final List<TraceEvent> EVENTS = new CopyOnWriteArrayList<>();

    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
    private static final AtomicBoolean WRITTEN = new AtomicBoolean(false);

    private static final ConcurrentMap<FunctionProfileKey, FunctionProfileStats> FUNCTION_STATS = new ConcurrentHashMap<>();

    public static void recordScriptLoad(String scriptType, String file, long startNanos, long durationNanos) {
        long tsMicros = (startNanos - TRACE_START_NANOS) / 1_000;
        long durMicros = durationNanos / 1_000;

        TraceEvent event = new TraceEvent(
            file,
            "kubejs:" + scriptType,
            "X",
            tsMicros,
            durMicros,
            1,
            Thread.currentThread().getId(),
            Map.of(
                "scriptType", scriptType,
                "file", file,
                "durationMs", durationNanos / 1_000_000.0
            )
        );

        EVENTS.add(event);
    }

    public static void recordSpan(String category, String name, long startNanos, long durationNanos, Map<String, Object> args) {
        if (!Config.enabled) {
            return;
        }

        if (TRACE_EVENT_COUNT.incrementAndGet() > Config.maxTraceEvents) {
            return;
        }

        long tsMicros = (startNanos - TRACE_START_NANOS) / 1_000;
        long durMicros = durationNanos / 1_000;

        EVENTS.add(new TraceEvent(
            name,
            category,
            "X",
            tsMicros,
            durMicros,
            1,
            Thread.currentThread().getId(),
            args
        ));
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
        return List.copyOf(EVENTS);
    }

    public static void writeTraceToDefaultLocation() {
        if (EVENTS.isEmpty()) {
            LOGGER.info("No KubeJS profiler events recorded.");
            return;
        }

        if (!WRITTEN.compareAndSet(false, true)) {
            return;
        }

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        Path gameDir = Path.of("").toAbsolutePath();

        Path outputPath = gameDir
                .resolve("kubejs-profiler")
                .resolve("trace-" + timestamp + ".json");

        try {
            writeTrace(outputPath);
            LOGGER.info("KubeJS profiler wrote trace: {}", outputPath);
        } catch (IOException e) {
            LOGGER.error("Failed to write KubeJS profiler trace", e);
        }
    }

    public static void writeTrace(Path path) throws IOException {
        Files.createDirectories(path.getParent());

        String json = toJson();

        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    public static void reset() {
        EVENTS.clear();
        FUNCTION_STATS.clear();
        TRACE_EVENT_COUNT.set(0);
        WRITTEN.set(false);
    }

    public static void registerShutdownHook() {
        if (!SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                writeTraceToDefaultLocation();
            } catch (Throwable throwable) {
                LOGGER.error("Failed during KubeJS profiler shutdown hook", throwable);
            }
        }, "KubeJS Profiler Trace Writer"));
    }

    private static String toJson() {
        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"traceEvents\": [\n");

        for (int i = 0; i < KubeJSProfilerRecorder.EVENTS.size(); i++) {
            TraceEvent event = KubeJSProfilerRecorder.EVENTS.get(i);

            json.append("    {\n");
            json.append("      \"name\": ").append(jsonString(event.name())).append(",\n");
            json.append("      \"cat\": ").append(jsonString(event.cat())).append(",\n");
            json.append("      \"ph\": ").append(jsonString(event.ph())).append(",\n");
            json.append("      \"ts\": ").append(event.ts()).append(",\n");
            json.append("      \"dur\": ").append(event.dur()).append(",\n");
            json.append("      \"pid\": ").append(event.pid()).append(",\n");
            json.append("      \"tid\": ").append(event.tid()).append(",\n");
            json.append("      \"args\": ").append(mapToJson(event.args())).append("\n");
            json.append("    }");

            if (i < KubeJSProfilerRecorder.EVENTS.size() - 1) {
                json.append(",");
            }

            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        return json.toString();
    }

    private static String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder();

        json.append("{");

        int i = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            json.append(jsonString(entry.getKey()));
            json.append(": ");
            json.append(valueToJson(entry.getValue()));

            if (i < map.size() - 1) {
                json.append(", ");
            }

            i++;
        }

        json.append("}");

        return json.toString();
    }

    private static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        return jsonString(value.toString());
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder();
        escaped.append("\"");

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }

        escaped.append("\"");
        return escaped.toString();
    }
}
