package fi.dy.masa.flooded.event;

import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.terraingen.ChunkGeneratorEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.flooded.capabilities.FloodedCapabilities;
import fi.dy.masa.flooded.capabilities.IFloodedChunkCapability;
import fi.dy.masa.flooded.config.Configs;
import fi.dy.masa.flooded.util.WorldUtil;

public class FloodedEventHandler
{
    private int waterLevel;

    @SubscribeEvent
    public void onChunkAttachCapabilities(AttachCapabilitiesEvent<Chunk> event)
    {
        
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event)
    {
        Chunk chunk = event.getChunk();

        if (chunk.isTerrainPopulated() && Configs.enabledInDimension(chunk.getWorld().provider.getDimension()))
        {
            IFloodedChunkCapability cap = chunk.getCapability(FloodedCapabilities.CAPABILITY_FLOODED_CHUNK, null);

            if (cap != null && cap.getLastWaterLevel() < this.waterLevel)
            {
                int lastLevel = cap.getLastWaterLevel();
                WorldUtil.replaceOldWaterLayer(event.getWorld(), chunk.x, chunk.z, lastLevel, this.waterLevel);
                WorldUtil.fillChunkWithWater(event.getWorld(), chunk.x, chunk.z, this.waterLevel, Configs.floodNewChunksUnderground);
                WorldUtil.fillChunkWithWaterLayer(event.getWorld(), chunk.x, chunk.z, this.waterLevel);
                cap.setWaterLevel(this.waterLevel);
            }
        }
    }

    @SubscribeEvent
    public void onReplaceBiomeBlocks(ChunkGeneratorEvent.ReplaceBiomeBlocks event)
    {
        if (event.getWorld() != null && Configs.enabledInDimension(event.getWorld().provider.getDimension()))
        {
            this.waterLevel = 80 * 16 + 3;
            WorldUtil.fillChunkPrimerWithWater(event.getWorld(), event.getPrimer(), this.waterLevel, Configs.floodNewChunksUnderground);
        }
    }

    @SubscribeEvent
    public void onChunkPopulate(PopulateChunkEvent.Post event)
    {
        if (Configs.enabledInDimension(event.getWorld().provider.getDimension()))
        {
            this.waterLevel = 80 * 16 + 3;
            WorldUtil.fillChunkWithWaterLayer(event.getWorld(), event.getChunkX(), event.getChunkZ(), this.waterLevel);

            Chunk chunk = event.getWorld().getChunkFromChunkCoords(event.getChunkX(), event.getChunkZ());
            IFloodedChunkCapability cap = chunk.getCapability(FloodedCapabilities.CAPABILITY_FLOODED_CHUNK, null);

            if (cap != null)
            {
                cap.setWaterLevel(this.waterLevel);
            }
        }
    }
}
