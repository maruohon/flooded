package fi.dy.masa.flooded.proxy;

import net.minecraftforge.common.MinecraftForge;
import fi.dy.masa.flooded.event.FloodedEventHandler;

public class CommonProxy
{
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new FloodedEventHandler());
    }
}
