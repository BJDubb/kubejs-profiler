package au.beckam.kubejsprofiler.mixins;

import au.beckam.kubejsprofiler.trace.KubeJSProfilerRecorder;
import dev.latvian.mods.kubejs.script.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScriptFile.class)
public class ScriptFileMixin {

    @Shadow(remap = false)
    @Final
    public ScriptFileInfo info;

    @Shadow(remap = false)
    @Final
    public ScriptPack pack;

    @Unique
    private long kubejsProfiler$startNanos;

    @Inject(method = "load", at = @At("HEAD"), remap = false)
    private void kubejsProfiler$loadHead(CallbackInfo ci) {
        this.kubejsProfiler$startNanos = System.nanoTime();
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void kubejsProfiler$loadReturn(CallbackInfo ci) {
        long endNanos = System.nanoTime();
        long durationNanos = endNanos - this.kubejsProfiler$startNanos;

        String scriptType = "unknown";
        String file = "unknown";

        try {
            if (this.pack != null && this.pack.manager != null && this.pack.manager.scriptType != null) {
                scriptType = this.pack.manager.scriptType.name;
            }

            if (this.info != null && this.info.location != null) {
                file = this.info.location;
            }
        } catch (Throwable ignored) {
            // avoid profiler crashing the game if KubeJS internals change
        }

        KubeJSProfilerRecorder.recordScriptLoad(
            scriptType,
            file,
            this.kubejsProfiler$startNanos,
            durationNanos
        );
    }
}