package au.beckam.kubejsprofiler;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = KubeJSProfiler.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Whether profiling is enabled")
            .define("enabled", true);

    private static final ForgeConfigSpec.BooleanValue TRACE_SPANS = BUILDER
            .comment("Emit per-function spans into the trace JSON")
            .define("traceSpans", false);

    private static final ForgeConfigSpec.BooleanValue TRACE_SCRIPT_LOADS = BUILDER
            .comment("Record script load durations")
            .define("traceScriptLoads", true);

    private static final ForgeConfigSpec.LongValue MIN_FUNCTION_DURATION_NANOS = BUILDER
            .comment("Min function span duration in ns (traceSpans only)")
            .defineInRange("minFunctionDurationNs", 5_000_000L, 0, Long.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue MAX_TRACE_EVENTS = BUILDER
            .comment("Max events buffered before new ones are dropped")
            .defineInRange("maxTraceEvents", 25_000, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue TOP_N_FUNCTIONS = BUILDER
            .comment("Max functions in the summary (0 = all)")
            .defineInRange("topNFunctions", 200, 0, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static volatile boolean enabled;
    public static volatile boolean traceSpans;
    public static volatile boolean traceScriptLoads;
    public static volatile long minFunctionDurationNs;
    public static volatile int maxTraceEvents;
    public static volatile int topNFunctions;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enabled = ENABLED.get();
        traceSpans = TRACE_SPANS.get();
        traceScriptLoads = TRACE_SCRIPT_LOADS.get();
        minFunctionDurationNs = MIN_FUNCTION_DURATION_NANOS.get();
        maxTraceEvents = MAX_TRACE_EVENTS.get();
        topNFunctions = TOP_N_FUNCTIONS.get();
    }
}
