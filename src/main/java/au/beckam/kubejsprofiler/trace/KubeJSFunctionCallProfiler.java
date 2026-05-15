package au.beckam.kubejsprofiler.trace;

import au.beckam.kubejsprofiler.Config;
import au.beckam.kubejsprofiler.util.RhinoFunctionIntrospector;

import java.util.ArrayDeque;
import java.util.Deque;

public final class KubeJSFunctionCallProfiler {
    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    public static void enterFromCallFrame(Object callFrame) {
        Object idata = RhinoFunctionIntrospector.getInterpreterDataFromCallFrame(callFrame);

        enter(
            RhinoFunctionIntrospector.getSourceNameFromInterpreterData(idata),
            RhinoFunctionIntrospector.getFunctionNameFromInterpreterData(idata),
            RhinoFunctionIntrospector.getLineNumberFromInterpreterData(idata)
        );
    }

    public static void enter(String sourceName, String functionName, int lineNumber) {
        STACK.get().push(new Frame(
            sourceName,
            functionName,
            lineNumber,
            System.nanoTime()
        ));
    }

    public static void exit(boolean threw, Throwable throwable) {
        Deque<Frame> stack = STACK.get();

        if (stack.isEmpty()) {
            return;
        }

        Frame frame = stack.pop();

        long durationNanos = System.nanoTime() - frame.startNanos();

        // always aggregate. This is cheap-ish and gives a useful summary
        KubeJSProfilerRecorder.recordFunctionAggregate(
            frame.sourceName(),
            frame.functionName(),
            frame.lineNumber(),
            durationNanos
        );

        // dont emit trace spans unless explicitly enabled
        if (!Config.traceSpans) {
            cleanup(stack);
            return;
        }

        // Only trace slow function calls.
        if (durationNanos < Config.minFunctionDurationNs) {
            cleanup(stack);
            return;
        }

        KubeJSProfilerRecorder.recordSpan(
            "rhino.function",
            buildDisplayName(frame),
            frame.startNanos(),
            durationNanos,
            TraceMaps.mapOf(
                "sourceName", frame.sourceName(),
                "functionName", frame.functionName(),
                "lineNumber", frame.lineNumber(),
                "durationMs", durationNanos / 1_000_000.0,
                "threw", threw,
                "exception", throwable == null ? null : throwable.toString()
            )
        );

        cleanup(stack);
    }

    private static void cleanup(Deque<Frame> stack) {
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    private static String buildDisplayName(Frame frame) {
        String sourceName = frame.sourceName() == null || frame.sourceName().isBlank()
                ? "<unknown source>"
                : frame.sourceName();

        String functionName = frame.functionName() == null || frame.functionName().isBlank()
                ? "<anonymous>"
                : frame.functionName();

        String lineSuffix = frame.lineNumber() > 0
                ? ":" + frame.lineNumber()
                : "";

        return sourceName + lineSuffix + " :: " + functionName;
    }

    private record Frame(
            String sourceName,
            String functionName,
            int lineNumber,
            long startNanos
    ) {}
}