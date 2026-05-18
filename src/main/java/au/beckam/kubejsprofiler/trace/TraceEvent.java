package au.beckam.kubejsprofiler.trace;

import java.util.Map;

public sealed interface TraceEvent permits TraceEvent.ScriptLoad, TraceEvent.FunctionSpan {

    int PID = 1;

    String PH_COMPLETE = "X";

    long ts();

    long dur();

    long tid();

    void writeJson(StringBuilder out);

    record ScriptLoad(
            String file,
            String scriptType,
            long ts,
            long dur,
            long tid,
            Map<String, Object> args
    ) implements TraceEvent {

        @Override
        public void writeJson(StringBuilder out) {
            out.append("{");
            out.append("\"name\":").append(TraceJson.string(file)).append(",");
            out.append("\"cat\":\"kubejs:").append(TraceJson.escape(scriptType)).append("\",");
            out.append("\"ph\":\"").append(PH_COMPLETE).append("\",");
            out.append("\"ts\":").append(ts).append(",");
            out.append("\"dur\":").append(dur).append(",");
            out.append("\"pid\":").append(PID).append(",");
            out.append("\"tid\":").append(tid).append(",");
            out.append("\"args\":");
            TraceJson.writeMap(out, args);
            out.append("}");
        }
    }

    record FunctionSpan(
            String sourceName,
            String functionName,
            int lineNumber,
            long ts,
            long dur,
            long tid
    ) implements TraceEvent {

        @Override
        public void writeJson(StringBuilder out) {
            out.append("{");
            out.append("\"name\":\"");
            appendDisplayName(out);
            out.append("\",");
            out.append("\"cat\":\"rhino.function\",");
            out.append("\"ph\":\"").append(PH_COMPLETE).append("\",");
            out.append("\"ts\":").append(ts).append(",");
            out.append("\"dur\":").append(dur).append(",");
            out.append("\"pid\":").append(PID).append(",");
            out.append("\"tid\":").append(tid);
            out.append("}");
        }

        private void appendDisplayName(StringBuilder out) {
            String src = (sourceName == null || sourceName.isBlank()) ? "<unknown source>" : sourceName;
            String name = (functionName == null || functionName.isBlank()) ? "<anonymous>" : functionName;

            TraceJson.appendEscaped(out, src);

            if (lineNumber > 0) {
                out.append(':').append(lineNumber);
            }

            out.append(" :: ");
            TraceJson.appendEscaped(out, name);
        }
    }
}
