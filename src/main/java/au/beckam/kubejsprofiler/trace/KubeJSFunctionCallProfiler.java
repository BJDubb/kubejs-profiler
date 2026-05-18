package au.beckam.kubejsprofiler.trace;

import au.beckam.kubejsprofiler.Config;
import au.beckam.kubejsprofiler.util.RhinoFunctionIntrospector;

public final class KubeJSFunctionCallProfiler {

    private static final ThreadLocal<FrameStack> STACK = ThreadLocal.withInitial(FrameStack::new);

    private KubeJSFunctionCallProfiler() {
    }

    public static void enterFromCallFrame(Object callFrame) {
        Object idata = RhinoFunctionIntrospector.getInterpreterDataFromCallFrame(callFrame);

        enter(
            RhinoFunctionIntrospector.getSourceNameFromInterpreterData(idata),
            RhinoFunctionIntrospector.getFunctionNameFromInterpreterData(idata),
            RhinoFunctionIntrospector.getLineNumberFromInterpreterData(idata)
        );
    }

    public static void enter(String sourceName, String functionName, int lineNumber) {
        STACK.get().push(sourceName, functionName, lineNumber, System.nanoTime());
    }

    public static void exit(boolean threw, Throwable throwable) {
        FrameStack stack = STACK.get();

        if (stack.isEmpty()) {
            return;
        }

        String sourceName = stack.peekSourceName();
        String functionName = stack.peekFunctionName();
        int lineNumber = stack.peekLineNumber();
        long startNanos = stack.peekStartNanos();

        stack.pop();

        long durationNanos = System.nanoTime() - startNanos;

        KubeJSProfilerRecorder.recordFunctionAggregate(sourceName, functionName, lineNumber, durationNanos);

        if (!Config.traceSpans) {
            return;
        }

        if (durationNanos < Config.minFunctionDurationNs) {
            return;
        }

        KubeJSProfilerRecorder.recordFunctionSpan(
                sourceName,
                functionName,
                lineNumber,
                startNanos,
                durationNanos
        );
    }
}
