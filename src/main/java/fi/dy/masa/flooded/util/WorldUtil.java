package fi.dy.masa.flooded.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.base.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import fi.dy.masa.flooded.Flooded;
import fi.dy.masa.flooded.block.BlockLiquidLayer;
import fi.dy.masa.flooded.block.FloodedBlocks;
import fi.dy.masa.flooded.capabilities.FloodedCapabilities;
import fi.dy.masa.flooded.capabilities.IFloodedChunkCapability;
import fi.dy.masa.flooded.config.Configs;

public class WorldUtil
{
    private static int scheduleCount;
    private static Map<Integer, Set<ChunkPos>> chunksToUpdateMap = new HashMap<>();
    private static boolean isSpreadingInFullChunks;

    public static void setScheduleCount(int count)
    {
        scheduleCount = count;
    }

    public static int getScheduleCount()
    {
        return scheduleCount;
    }

    private static void storeLoadedChunkLocations(World world)
    {
        final int dimension = world.provider.getDimension();
        Set<ChunkPos> chunksToUpdate = chunksToUpdateMap.get(dimension);

        if (chunksToUpdate == null)
        {
            chunksToUpdate = new HashSet<>();
            chunksToUpdateMap.put(dimension, chunksToUpdate);
        }

        chunksToUpdate.clear();

        Collection<Chunk> chunks = ((WorldServer) world).getChunkProvider().getLoadedChunks();

        for (Chunk chunk : chunks)
        {
            chunksToUpdate.add(chunk.getPos());
        }

        isSpreadingInFullChunks = true;
    }

    private static void updateWaterLevelInLoadedChunks(WorldServer world, int chunkLimit)
    {
        final int dimension = world.provider.getDimension();
        Set<ChunkPos> chunksToUpdate = chunksToUpdateMap.get(dimension);

        if (chunksToUpdate != null && chunksToUpdate.isEmpty() == false)
        {
            final int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);
            Iterator<ChunkPos> iter = chunksToUpdate.iterator();
            int count = 0;

            while (iter.hasNext())
            {
                ChunkPos pos = iter.next();
                BlockPos blockPos = new BlockPos(pos.x << 4, 0, pos.z << 4);

                if (world.isBlockLoaded(blockPos))
                {
                    Chunk chunk = world.getChunkFromChunkCoords(pos.x, pos.z);
                    updateWaterLevelInChunk(world, chunk, waterLevel, false);
                    //sendChunkToWatchers(world, chunk);
                    count++;
                }

                iter.remove();

                if (count >= chunkLimit)
                {
                    break;
                }
            }

            if (chunksToUpdate.isEmpty())
            {
                isSpreadingInFullChunks = false;
            }
        }
    }

    public static void updateWaterLevelInChunk(WorldServer world, Chunk chunk, int waterLevel, boolean fillWithWater)
    {
        if (chunk.isTerrainPopulated())
        {
            IFloodedChunkCapability cap = chunk.getCapability(FloodedCapabilities.CAPABILITY_FLOODED_CHUNK, null);

            if (cap != null && cap.getWaterLevel() < waterLevel)
            {
                int lastLevel = cap.getWaterLevel();
                //Flooded.logInfo("Updating water level in Chunk [{}, {}] from {} to {}", chunk.x, chunk.z, (float) lastLevel / 16f, (float) waterLevel / 16f);

                // No need to fill with regular water in loaded chunks (thus the fillWithWater option).
                // Simply update/replace the old layer first, then add a new layer if needed.
                // "Larger changes" (like filling with full water blocks) happens only on chunk load.

                if ((lastLevel & BlockLiquidLayer.LEVEL_BITMASK) != 0)
                {
                    replaceOldWaterLayer(world, chunk.x, chunk.z, lastLevel, waterLevel);
                }

                if (fillWithWater)
                {
                    fillChunkWithWater(world, chunk.x, chunk.z, waterLevel, false);
                }

                if ((waterLevel & BlockLiquidLayer.LEVEL_BITMASK) != 0)
                {
                    fillChunkWithWaterLayer(world, chunk.x, chunk.z, waterLevel);
                }

                cap.setWaterLevel(chunk, waterLevel);
            }
        }
    }

    public static void fillChunkPrimerWithWater(World world, ChunkPrimer primer, int waterLevel, boolean fillUnderGround)
    {
        IBlockState water = Blocks.WATER.getDefaultState();
        final int waterLevelBlocks = waterLevel >> BlockLiquidLayer.BITMASK_SIZE;

        // NOTE: The ChunkPrimer can only work with block states whose ID fits into a char!
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                for (int y = waterLevelBlocks; y >= 0; y--)
                {
                    IBlockState state = primer.getBlockState(x, y, z);

                    // FIXME Should check for sky access?
                    if (state.getMaterial() == Material.AIR)
                    {
                        primer.setBlockState(x, y, z, water);
                    }
                    else if (fillUnderGround == false)
                    {
                        break;
                    }
                }
            }
        }
    }

    private static void fillChunkWithWater(World world, int chunkX, int chunkZ, int waterLevel, boolean fillUnderGround)
    {
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        IBlockState water = Blocks.WATER.getDefaultState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        final int waterLevelBlocks = waterLevel >> BlockLiquidLayer.BITMASK_SIZE;
        final int xBase = chunkX << 4;
        final int zBase = chunkZ << 4;

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                for (int y = waterLevelBlocks; y >= 0; y--)
                {
                    posMutable.setPos(xBase + x, y, zBase + z);
                    IBlockState state = chunk.getBlockState(x, y, z);
                    Block blockOld = state.getBlock();

                    if (
                           blockOld != Blocks.WATER
                        && blockOld.isReplaceable(world, posMutable)
                        && chunk.canSeeSky(posMutable)
                    )
                    {
                        chunk.setBlockState(posMutable, water);
                    }
                    else if (fillUnderGround == false)
                    {
                        break;
                    }
                }
            }
        }
    }

    public static void fillChunkWithWaterLayer(WorldServer world, int chunkX, int chunkZ, int waterLevel)
    {
        final int layerLevel = waterLevel & BlockLiquidLayer.LEVEL_BITMASK;

        if (layerLevel != 0)
        {
            Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            IBlockState layerBase = FloodedBlocks.WATER_LAYER.getDefaultState();
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
            IBlockState stateLayer = BlockLiquidLayer.getStateWithSurfaceLevelOf(layerBase, layerLevel);
            final int waterLevelBlocks = waterLevel >> BlockLiquidLayer.BITMASK_SIZE;
            final int y = waterLevelBlocks + 1;
            final int xBase = chunkX << 4;
            final int zBase = chunkZ << 4;

            for (int x = 0; x < 16; x++)
            {
                for (int z = 0; z < 16; z++)
                {
                    posMutable.setPos(xBase + x, y, zBase + z);

                    IBlockState stateOld = chunk.getBlockState(x, y, z);
                    Block blockOld = stateOld.getBlock();

                    if (
                            (
                                   blockOld == FloodedBlocks.WATER_LAYER
                                && BlockLiquidLayer.isLevelHigher(stateLayer, stateOld)
                            )
                            ||
                            (
                                   canFlowInto(world, posMutable, stateOld, stateLayer)
                                && chunk.canSeeSky(posMutable)
                            )
                    )
                    {
                        chunk.setBlockState(posMutable, stateLayer);
                        world.getPlayerChunkMap().markBlockForUpdate(posMutable);
                    }
                }
            }
        }
    }

    /**
     * <b>This should only be called when the water level has risen!</b>
     * It will replace an old water layer at the given level with either full
     * regular water blocks or new higher level water layer blocks.
     * <b>Note:</b> This method doesn't create the new water layer
     * if it should be at a higher y-level!
     * @param world
     * @param chunkX
     * @param chunkZ
     * @param oldWaterLevel
     * @param newWaterLevel
     */
    private static void replaceOldWaterLayer(WorldServer world, int chunkX, int chunkZ, int oldWaterLevel, int newWaterLevel)
    {
        final int layerLevelOld = oldWaterLevel & BlockLiquidLayer.LEVEL_BITMASK;

        if (layerLevelOld != 0)
        {
            Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            IBlockState water = Blocks.WATER.getDefaultState();
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
            final int waterBlocksLevelOld = (oldWaterLevel >> BlockLiquidLayer.BITMASK_SIZE);
            final int waterBlocksLevelNew = (newWaterLevel >> BlockLiquidLayer.BITMASK_SIZE);
            final int layerLevelNew = newWaterLevel & BlockLiquidLayer.LEVEL_BITMASK;
            final int y = waterBlocksLevelOld + 1;
            final int xBase = chunkX << 4;
            final int zBase = chunkZ << 4;
            IBlockState stateNew = waterBlocksLevelNew > waterBlocksLevelOld ?
                    water : BlockLiquidLayer.getStateWithSurfaceLevelOf(FloodedBlocks.WATER_LAYER.getDefaultState(), layerLevelNew);

            for (int x = 0; x < 16; x++)
            {
                for (int z = 0; z < 16; z++)
                {
                    IBlockState stateOld = chunk.getBlockState(x, y, z);

                    if (stateOld.getBlock() == FloodedBlocks.WATER_LAYER)
                    {
                        posMutable.setPos(xBase + x, y, zBase + z);
                        chunk.setBlockState(posMutable, stateNew);
                        world.getPlayerChunkMap().markBlockForUpdate(posMutable);
                    }
                }
            }
        }
    }

    private static void seedWaterLayers(WorldServer world, int waterLevel, int maxBlockCount)
    {
        List<Chunk> chunks = new ArrayList<>(world.getChunkProvider().getLoadedChunks());
        Collections.shuffle(chunks);
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        final int maxPerChunk = MathHelper.clamp(maxBlockCount / chunks.size(), 1, maxBlockCount);
        final int waterLevelBlocks = waterLevel >> BlockLiquidLayer.BITMASK_SIZE;
        final int layerLevel = waterLevel & BlockLiquidLayer.LEVEL_BITMASK;

        if (layerLevel > 0)
        {
            final int y = waterLevelBlocks + 1;
            IBlockState water = Blocks.WATER.getDefaultState();
            IBlockState layerBase = FloodedBlocks.WATER_LAYER.getDefaultState();
            IBlockState stateLayer = BlockLiquidLayer.getStateWithSurfaceLevelOf(layerBase, layerLevel);
            int count = 0;

            for (Chunk chunk : chunks)
            {
                for (int i = 0; i < maxPerChunk; i++)
                {
                    int x = (chunk.x << 4) + world.rand.nextInt(16);
                    int z = (chunk.z << 4) + world.rand.nextInt(16);
                    posMutable.setPos(x, y, z);

                    if (chunk.canSeeSky(posMutable))
                    {
                        IBlockState stateOld = chunk.getBlockState(x, y, z);
                        IBlockState stateDown = chunk.getBlockState(x, y - 1, z);
                        Block blockDown = stateDown.getBlock();
                        BlockPos posDown = posMutable.down();

                        if (
                                canFlowInto(world, posMutable, stateOld, stateLayer)
                                &&
                                (
                                       blockDown == Blocks.WATER
                                    || blockDown == FloodedBlocks.WATER_LAYER
                                    || stateDown.isSideSolid(world, posDown, EnumFacing.UP)
                                )
                        )
                        {
                            world.setBlockState(posMutable, stateLayer);

                            if (blockDown == FloodedBlocks.WATER_LAYER)
                            {
                                world.setBlockState(posDown, water);
                            }

                            trySpreadWaterLayer(world, posMutable, stateLayer, false);
                        }
                    }

                    if (++count >= maxBlockCount)
                    {
                        return;
                    }
                }
            }
        }
    }

    public static void trySpreadWaterLayer(World world, BlockPos pos, IBlockState state, boolean decrementScheduleCount)
    {
        // Don't spread while the water level is being risen full chunks at a time,
        // to avoid unnecessary lag/extra updates.
        if (isSpreadingInFullChunks)
        {
            return;
        }

        for (int i = 0, index = world.rand.nextInt(4); i < 4; i++, index++)
        {
            EnumFacing side = EnumFacing.HORIZONTALS[index & 0x3];
            BlockPos posSide = pos.offset(side);

            if (world.isBlockLoaded(posSide) == false)
            {
                continue;
            }

            BlockPos posSideDown = posSide.down();
            IBlockState stateSide = world.getBlockState(posSide);
            IBlockState stateSideDown = world.getBlockState(posSideDown);
            Block blockSideDown = stateSideDown.getBlock();

            if (
                       world.getBlockState(pos.up()).getMaterial() != state.getMaterial()
                    && canFlowInto(world, posSide, stateSide, state)
                    &&
                    (
                        (
                            blockSideDown == Blocks.WATER
                         || blockSideDown == FloodedBlocks.WATER_LAYER
                         || stateSideDown.isSideSolid(world, posSideDown, EnumFacing.UP)
                        )
                    )
            )
            {
                world.setBlockState(posSide, state);

                if (blockSideDown == FloodedBlocks.WATER_LAYER)
                {
                    world.setBlockState(posSideDown, Blocks.WATER.getDefaultState());
                }

                if (scheduleCount < Configs.waterSpreadScheduleLimit)
                {
                    world.scheduleUpdate(posSide, state.getBlock(), state.getBlock().tickRate(world));
                    scheduleCount++;
                }

                if (world.rand.nextFloat() < 0.8)
                {
                    break;
                }
            }
        }

        if (decrementScheduleCount && scheduleCount > 0)
        {
            --scheduleCount;
        }
    }

    public static void sendChunkToWatchers(final WorldServer world, final Chunk chunk)
    {
        Predicate<EntityPlayerMP> predicate = new Predicate<EntityPlayerMP>()
        {
            @Override
            public boolean apply(EntityPlayerMP playerIn)
            {
                return world.getPlayerChunkMap().isPlayerWatchingChunk(playerIn, chunk.x, chunk.z);
            }
        };

        for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, predicate))
        {
            player.connection.sendPacket(new SPacketUnloadChunk(chunk.x, chunk.z));
            Packet<?> packet = new SPacketChunkData(chunk, 65535);
            player.connection.sendPacket(packet);
            world.getEntityTracker().sendLeashedEntitiesInChunk(player, chunk);
        }
    }

    public static void onWorldTick(final int dimension, final World world)
    {
        int waterLevel = WaterLevelManager.INSTANCE.getWaterLevelInDimension(dimension);

        if (waterLevel < world.getActualHeight() * BlockLiquidLayer.DIVISOR)
        {
            if ((world.getTotalWorldTime() % Configs.waterRiseInterval) == 0)
            {
                waterLevel++;
                Flooded.logInfo("Water level rising, new level = {}", getWaterLevelString(waterLevel));
                WaterLevelManager.INSTANCE.setWaterLevelInDimension(dimension, waterLevel);

                if (Configs.spreadWaterFullChunksAtOnce)
                {
                    Flooded.logInfo("Water layer spreading in full chunks, water level = {}", getWaterLevelString(waterLevel));
                    storeLoadedChunkLocations(world);
                }
            }

            if (Configs.spreadWaterFullChunksAtOnce)
            {
                updateWaterLevelInLoadedChunks((WorldServer) world, Configs.waterSpreadChunksPerTick);
            }
            else if (Configs.spreadWaterFullChunksAtOnce == false &&
                     (world.getTotalWorldTime() % Configs.waterLayerSeedingInterval) == 0)
            {
                Flooded.logInfo("Water layer seeding attempt, water level = {}", getWaterLevelString(waterLevel));
                seedWaterLayers((WorldServer) world, waterLevel, Configs.waterLayerSeedingCount);
            }
        }
    }

    private static boolean canFlowInto(World world, BlockPos pos, IBlockState stateTarget, IBlockState stateLayer)
    {
        Material materialTarget = stateTarget.getMaterial();

        // && stateTarget.getProperties().containsKey(BlockLiquidLayer.LEVEL)
        return (materialTarget != stateLayer.getMaterial()
                    || (stateTarget.getBlock() == stateLayer.getBlock()
                        && BlockLiquidLayer.isLevelHigher(stateLayer, stateTarget)))
                && materialTarget != Material.LAVA
                && isBlocked(world, pos, stateTarget) == false;
    }

    /**
     * This is from vanilla BlockDynamicLiquid...
     */
    private static boolean isBlocked(World world, BlockPos pos, IBlockState stateTarget)
    {
        Block block = stateTarget.getBlock();
        Material material = stateTarget.getMaterial();

        if ((block instanceof BlockDoor) == false && block != Blocks.STANDING_SIGN && block != Blocks.LADDER && block != Blocks.REEDS)
        {
            return material != Material.PORTAL && material != Material.STRUCTURE_VOID ? material.blocksMovement() : true;
        }
        else
        {
            return true;
        }
    }

    public static String getWaterLevelString(int waterLevel)
    {
        return String.valueOf((float) waterLevel / (float) BlockLiquidLayer.DIVISOR);
    }
}
