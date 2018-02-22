package fi.dy.masa.flooded.event;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.terraingen.ChunkGeneratorEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import fi.dy.masa.flooded.capabilities.FloodedCapabilities;
import fi.dy.masa.flooded.capabilities.FloodedCapabilities.FloodedChunkCapabilityProvider;
import fi.dy.masa.flooded.capabilities.IFloodedChunkCapability;
import fi.dy.masa.flooded.config.Configs;
import fi.dy.masa.flooded.reference.Reference;
import fi.dy.masa.flooded.util.WaterLevelManager;
import fi.dy.masa.flooded.util.WorldUtil;

public class FloodedEventHandler
{
    private static final ResourceLocation FLOODED_CHUNK_CAP_NAME = new ResourceLocation(Reference.MOD_ID, "chunk_cap");

    @SubscribeEvent
    public void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<Chunk> event)
    {
        event.addCapability(FLOODED_CHUNK_CAP_NAME, new FloodedChunkCapabilityProvider());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        final int dimension = event.getWorld().provider.getDimension();

        if (event.getWorld().isRemote == false && Configs.enabledInDimension(dimension))
        {
            // Initialize the water level if the worlds loads for the first time
            WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
        }
    }

    @SubscribeEvent
    public void onCreateSpawn(WorldEvent.CreateSpawnPosition event)
    {
        final int dimension = event.getWorld().provider.getDimension();

        if (event.getWorld().isRemote == false && Configs.enabledInDimension(dimension))
        {
            // Initialize the water level if the worlds loads for the first time
            WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event)
    {
        final int dimension = event.getWorld().provider.getDimension();

        if (event.getWorld().isRemote == false && Configs.enabledInDimension(dimension))
        {
            final int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
            Chunk chunk = event.getChunk();
            WorldUtil.updateWaterLevelInChunk((WorldServer) event.getWorld(), chunk, waterLevel, true);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event)
    {
        final int dimension = event.getWorld().provider.getDimension();

        // FIXME this event doesn't fire when stopping the server?
        if (event.getWorld().isRemote == false && Configs.enabledInDimension(dimension))
        {
            Chunk chunk = event.getChunk();
            int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
            IFloodedChunkCapability cap = chunk.getCapability(FloodedCapabilities.CAPABILITY_FLOODED_CHUNK, null);

            if (cap != null)
            {
                cap.setWaterLevel(chunk, waterLevel);
            }
        }
    }

    @SubscribeEvent
    public void onReplaceBiomeBlocks(ChunkGeneratorEvent.ReplaceBiomeBlocks event)
    {
        if (event.getWorld() != null)
        {
            final int dimension = event.getWorld().provider.getDimension();

            if (Configs.enabledInDimension(dimension))
            {
                int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
                WorldUtil.fillChunkPrimerWithWater(event.getWorld(), event.getPrimer(), waterLevel, Configs.floodNewChunksUnderground);
            }
        }
    }

    @SubscribeEvent
    public void onChunkPopulate(PopulateChunkEvent.Post event)
    {
        final int dimension = event.getWorld().provider.getDimension();

        if (event.getWorld().isRemote == false && Configs.enabledInDimension(dimension))
        {
            int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
            WorldUtil.fillChunkWithWaterLayer((WorldServer) event.getWorld(), event.getChunkX(), event.getChunkZ(), waterLevel);

            Chunk chunk = event.getWorld().getChunkFromChunkCoords(event.getChunkX(), event.getChunkZ());
            IFloodedChunkCapability cap = chunk.getCapability(FloodedCapabilities.CAPABILITY_FLOODED_CHUNK, null);

            if (cap != null)
            {
                cap.setWaterLevel(chunk, waterLevel);
            }
        }
    }

    @SubscribeEvent
    public void onWorldTick(WorldTickEvent event)
    {
        World world = event.world;

        if (world.isRemote == false && event.phase == TickEvent.Phase.END)
        {
            final int dimension = world.provider.getDimension();

            if (Configs.enabledInDimension(dimension) && world.isRaining())
            {
                WorldUtil.onWorldTick(dimension, world);
            }
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event)
    {
        if (event.getWorld().provider.getDimension() == 0)
        {
            WaterLevelManager.INSTANCE.setScheduleCount(WorldUtil.getScheduleCount());
        }

        WaterLevelManager.INSTANCE.writeToDisk();
    }
}
