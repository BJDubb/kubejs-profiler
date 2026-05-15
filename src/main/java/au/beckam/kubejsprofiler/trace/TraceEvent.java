package au.beckam.kubejsprofiler.trace;

import java.util.Map;

public record TraceEvent(
        String name,
        String cat,
        String ph,
        long ts,
        long dur,
        int pid,
        long tid,
        Map<String, Object> args
) {}
