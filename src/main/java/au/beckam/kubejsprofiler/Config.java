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
            .comment("Whether or not to trace spans")
            .define("traceSpans", false);

    private static final ForgeConfigSpec.LongValue MIN_FUNCTION_DURATION_NANOS = BUILDER
            .comment("Min duration for function to trace in nanoseconds")
            .defineInRange("minFunctionDurationNs", 5_000_000L, 0, Long.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue MAX_TRACE_EVENTS = BUILDER
            .comment("Max events to trace")
            .defineInRange("maxTraceEvents", 25_000, 0, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enabled;
    public static boolean traceSpans;
    public static long minFunctionDurationNs;
    public static int maxTraceEvents;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        enabled = ENABLED.get();
        traceSpans = TRACE_SPANS.get();
        minFunctionDurationNs = MIN_FUNCTION_DURATION_NANOS.get();
        maxTraceEvents = MAX_TRACE_EVENTS.get();
    }
}
