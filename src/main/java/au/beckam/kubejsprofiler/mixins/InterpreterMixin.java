package au.beckam.kubejsprofiler.mixins;

import au.beckam.kubejsprofiler.trace.KubeJSFunctionCallProfiler;
import dev.latvian.mods.rhino.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.latvian.mods.rhino.Interpreter")
public abstract class InterpreterMixin {

    @Inject(
            method = "enterFrame(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Interpreter$CallFrame;[Ljava/lang/Object;Z)V",
            at = @At("HEAD"),
            remap = false
    )
    private static void kubejsProfiler$enterFrame(Context cx, @Coerce Object frame, Object[] args, boolean continuationRestart, CallbackInfo ci) {
        KubeJSFunctionCallProfiler.enterFromCallFrame(frame);
    }

    @Inject(
            method = "exitFrame(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Interpreter$CallFrame;Ljava/lang/Object;)V",
            at = @At("HEAD"),
            remap = false
    )
    private static void kubejsProfiler$exitFrame(Context cx, @Coerce Object frame, Object result, CallbackInfo ci) {
        KubeJSFunctionCallProfiler.exit(false, null);
    }
}