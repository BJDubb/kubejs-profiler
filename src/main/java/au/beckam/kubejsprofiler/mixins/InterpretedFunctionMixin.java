package au.beckam.kubejsprofiler.mixins;

import au.beckam.kubejsprofiler.trace.KubeJSFunctionCallProfiler;
import au.beckam.kubejsprofiler.util.RhinoFunctionIntrospector;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.latvian.mods.rhino.InterpretedFunction")
public abstract class InterpretedFunctionMixin {

    @Inject(method = "call", at = @At("HEAD"), remap = false)
    private void kubejsProfiler$enter(Context cx, Scriptable scope, Scriptable thisObj, Object[] args, CallbackInfoReturnable<Object> cir) {
        Object self = this;

        KubeJSFunctionCallProfiler.enter(
            RhinoFunctionIntrospector.getSourceName(self),
            RhinoFunctionIntrospector.getFunctionName(self),
            RhinoFunctionIntrospector.getLineNumber(self)
        );
    }

    @Inject(method = "call", at = @At("RETURN"), remap = false)
    private void kubejsProfiler$return(Context cx, Scriptable scope, Scriptable thisObj, Object[] args, CallbackInfoReturnable<Object> cir) {
        KubeJSFunctionCallProfiler.exit(false, null);
    }
}