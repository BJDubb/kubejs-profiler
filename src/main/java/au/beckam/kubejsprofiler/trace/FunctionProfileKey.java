package au.beckam.kubejsprofiler.trace;

public record FunctionProfileKey(
        String sourceName,
        String functionName,
        int firstLine
) {}