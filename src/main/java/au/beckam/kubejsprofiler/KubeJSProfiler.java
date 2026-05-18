package au.beckam.kubejsprofiler;

import au.beckam.kubejsprofiler.command.KubeJSProfilerCommand;
import au.beckam.kubejsprofiler.trace.KubeJSProfilerRecorder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KubeJSProfiler.MODID)
public class KubeJSProfiler
{
    public static final String MODID = "kubejsprofiler";

    public KubeJSProfiler(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        KubeJSProfilerRecorder.registerShutdownHook();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event)
    {
        KubeJSProfilerRecorder.writeOnShutdown();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        KubeJSProfilerCommand.register(event.getDispatcher());
    }
}
