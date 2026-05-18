package au.beckam.kubejsprofiler.trace;

import java.util.Arrays;

final class FrameStack {

    private static final int INITIAL_CAPACITY = 16;

    private String[] sourceNames = new String[INITIAL_CAPACITY];
    private String[] functionNames = new String[INITIAL_CAPACITY];
    private int[] lineNumbers = new int[INITIAL_CAPACITY];
    private long[] startNanos = new long[INITIAL_CAPACITY];
    private int depth;

    void push(String sourceName, String functionName, int lineNumber, long startNs) {
        int d = depth;

        if (d == sourceNames.length) {
            grow();
        }

        sourceNames[d] = sourceName;
        functionNames[d] = functionName;
        lineNumbers[d] = lineNumber;
        startNanos[d] = startNs;

        depth = d + 1;
    }

    boolean isEmpty() {
        return depth == 0;
    }

    String peekSourceName() {
        return sourceNames[depth - 1];
    }

    String peekFunctionName() {
        return functionNames[depth - 1];
    }

    int peekLineNumber() {
        return lineNumbers[depth - 1];
    }

    long peekStartNanos() {
        return startNanos[depth - 1];
    }

    void pop() {
        int d = --depth;
        sourceNames[d] = null;
        functionNames[d] = null;
    }

    private void grow() {
        int newLen = sourceNames.length * 2;
        sourceNames = Arrays.copyOf(sourceNames, newLen);
        functionNames = Arrays.copyOf(functionNames, newLen);
        lineNumbers = Arrays.copyOf(lineNumbers, newLen);
        startNanos = Arrays.copyOf(startNanos, newLen);
    }
}
