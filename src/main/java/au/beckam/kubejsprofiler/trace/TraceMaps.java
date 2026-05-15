package au.beckam.kubejsprofiler.trace;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TraceMaps {
    private TraceMaps() {
    }

    public static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < values.length - 1; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }

        return map;
    }
}