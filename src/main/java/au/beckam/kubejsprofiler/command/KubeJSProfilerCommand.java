package au.beckam.kubejsprofiler.command;

import au.beckam.kubejsprofiler.Config;
import au.beckam.kubejsprofiler.trace.KubeJSProfilerRecorder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

public final class KubeJSProfilerCommand {

    private static final int PERMISSION_LEVEL = 2;

    private KubeJSProfilerCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("kubejsprofiler")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL));

        root.then(Commands.literal("status").executes(ctx -> {
            send(ctx.getSource(), status());
            return 1;
        }));

        root.then(Commands.literal("dump").executes(ctx -> {
            Path dir = KubeJSProfilerRecorder.dumpToDefaultLocation();

            if (dir == null) {
                send(ctx.getSource(), "nothing to dump");
                return 0;
            }

            send(ctx.getSource(), "dumped to " + dir);
            return 1;
        }));

        root.then(Commands.literal("reset").executes(ctx -> {
            KubeJSProfilerRecorder.reset();
            send(ctx.getSource(), "cleared");
            return 1;
        }));

        root.then(Commands.literal("toggle").executes(ctx -> {
            Config.enabled = !Config.enabled;
            send(ctx.getSource(), Config.enabled ? "profiler on" : "profiler off");
            return 1;
        }));

        dispatcher.register(root);
    }

    private static String status() {
        return "enabled=" + Config.enabled
                + " traceSpans=" + Config.traceSpans
                + " traceScriptLoads=" + Config.traceScriptLoads
                + " minNs=" + Config.minFunctionDurationNs
                + " maxEvents=" + Config.maxTraceEvents
                + " topN=" + Config.topNFunctions
                + " events=" + KubeJSProfilerRecorder.getEventCount()
                + " functions=" + KubeJSProfilerRecorder.getFunctionStatsCount();
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(
                () -> Component.literal(message).withStyle(ChatFormatting.GRAY),
                false
        );
    }
}
