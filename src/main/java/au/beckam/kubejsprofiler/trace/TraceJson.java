package au.beckam.kubejsprofiler.trace;

import java.util.Map;

final class TraceJson {

    private TraceJson() {
    }

    static String string(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        appendEscaped(out, value);
        out.append('"');
        return out.toString();
    }

    static void appendEscaped(StringBuilder out, String value) {
        if (value == null) {
            return;
        }

        for (int i = 0, n = value.length(); i < n; i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder out = new StringBuilder(value.length());
        appendEscaped(out, value);
        return out.toString();
    }

    static void writeMap(StringBuilder out, Map<String, Object> map) {
        if (map == null) {
            out.append("{}");
            return;
        }

        out.append('{');

        int i = 0;
        int size = map.size();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            out.append(string(entry.getKey())).append(':');
            writeValue(out, entry.getValue());

            if (++i < size) {
                out.append(',');
            }
        }

        out.append('}');
    }

    private static void writeValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
            return;
        }

        if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
            return;
        }

        out.append(string(value.toString()));
    }
}
