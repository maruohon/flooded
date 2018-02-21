package fi.dy.masa.flooded.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import fi.dy.masa.flooded.block.BlockLiquidLayer;
import fi.dy.masa.flooded.block.FloodedBlocks;
import fi.dy.masa.flooded.config.Configs;

public class WorldUtil
{
    private static int scheduleCount;

    public static void setScheduleCount(int count)
    {
        scheduleCount = count;
    }

    public static int getScheduleCount()
    {
        return scheduleCount;
    }

    public static void fillChunkPrimerWithWater(World world, ChunkPrimer primer, int waterLevel, boolean fillUnderGround)
    {
        IBlockState water = Blocks.WATER.getDefaultState();
        final int waterLevelBlocks = waterLevel >> 4;

        // NOTE: The ChunkPrimer can only work with block states whose ID fits into a char!
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                for (int y = waterLevelBlocks; y >= 0; y--)
                {
                    IBlockState state = primer.getBlockState(x, y, z);

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

    public static void fillChunkWithWater(World world, int chunkX, int chunkZ, int waterLevel, boolean fillUnderGround)
    {
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        IBlockState water = Blocks.WATER.getDefaultState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        final int waterLevelBlocks = waterLevel >> 4;

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                for (int y = waterLevelBlocks; y >= 0; y--)
                {
                    IBlockState state = chunk.getBlockState(x, y, z);

                    if (state.getMaterial() == Material.AIR)
                    {
                        posMutable.setPos(x, y, z);
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

    public static void fillChunkWithWaterLayer(World world, int chunkX, int chunkZ, int waterLevel)
    {
        final int waterLevelBlocks = waterLevel >> 4;
        final int layerLevel = waterLevel & 0xF;

        if (layerLevel != 0)
        {
            Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
            IBlockState layerBase = FloodedBlocks.WATER_LAYER.getDefaultState();
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
            IBlockState layer = layerBase.withProperty(BlockLiquidLayer.LEVEL, layerLevel);
            final int y = waterLevelBlocks + 1;
            final int xBase = chunkX << 4;
            final int zBase = chunkZ << 4;

            for (int x = 0; x < 16; x++)
            {
                for (int z = 0; z < 16; z++)
                {
                    posMutable.setPos(xBase + x, y, zBase + z);

                    IBlockState stateOld = chunk.getBlockState(x, y, z);

                    // Or World#canBlockSeeSky()?
                    if (stateOld.getBlock() == FloodedBlocks.WATER_LAYER ||
                        (stateOld.getBlock().isReplaceable(world, posMutable) && world.canSeeSky(posMutable)))
                    {
                        chunk.setBlockState(posMutable, layer);
                    }
                }
            }
        }
    }

    public static void replaceOldWaterLayer(World world, int chunkX, int chunkZ, int oldWaterLevel, int newWaterLevel)
    {
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        IBlockState water = Blocks.WATER.getDefaultState();
        IBlockState layerBase = FloodedBlocks.WATER_LAYER.getDefaultState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        final int waterLevelBlocksOld = (oldWaterLevel >> 4);
        final int oldLayerLevel = oldWaterLevel & 0xF;

        if (oldLayerLevel != 0)
        {
            final int newLayerLevel = newWaterLevel & 0xF;
            final int y = waterLevelBlocksOld + 1;
            IBlockState stateNew = newLayerLevel > 0 ? layerBase.withProperty(BlockLiquidLayer.LEVEL, newLayerLevel) : water;

            for (int x = 0; x < 16; x++)
            {
                for (int z = 0; z < 16; z++)
                {
                    posMutable.setPos(x, y, z);

                    IBlockState stateOld = chunk.getBlockState(x, y, z);

                    if (stateOld.getBlock() == FloodedBlocks.WATER_LAYER)
                    {
                        chunk.setBlockState(posMutable, stateNew);
                    }
                }
            }
        }
    }

    public static void seedWaterLayers(World world, int waterLevel, int maxBlockCount)
    {
        List<Chunk> chunks = new ArrayList<>(((WorldServer) world).getChunkProvider().getLoadedChunks());
        Collections.shuffle(chunks);
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        final int maxPerChunk = MathHelper.clamp(maxBlockCount / chunks.size(), 1, maxBlockCount);
        final int waterLevelBlocks = waterLevel >> 4;
        final int layerLevel = waterLevel & 0xF;

        if (layerLevel > 0)
        {
            final int y = waterLevelBlocks + 1;
            IBlockState layerBase = FloodedBlocks.WATER_LAYER.getDefaultState();
            IBlockState water = Blocks.WATER.getDefaultState();
            IBlockState newState = layerBase.withProperty(BlockLiquidLayer.LEVEL, layerLevel);
            int count = 0;

            for (Chunk chunk : chunks)
            {
                for (int i = 0; i < maxPerChunk; i++)
                {
                    int x = (chunk.x << 4) + world.rand.nextInt(16);
                    int z = (chunk.z << 4) + world.rand.nextInt(16);
                    posMutable.setPos(x, y, z);

                    if (world.canSeeSky(posMutable))
                    {
                        IBlockState state = chunk.getBlockState(x, y, z);
                        IBlockState stateDown = chunk.getBlockState(x, y - 1, z);
                        BlockPos posDown = posMutable.down();

                        if (
                                (
                                    (
                                        state.getBlock() == FloodedBlocks.WATER_LAYER
                                     && state.getValue(BlockLiquidLayer.LEVEL) < layerLevel
                                    )
                                    ||
                                    (
                                        state.getBlock() != FloodedBlocks.WATER_LAYER
                                     && state.getBlock().isReplaceable(world, posMutable)
                                    )
                                )
                                &&
                                (
                                    (
                                        stateDown.getBlock() == Blocks.WATER
                                     || stateDown.getBlock() == FloodedBlocks.WATER_LAYER
                                     || stateDown.isSideSolid(world, posDown, EnumFacing.UP)
                                    )
                                )
                        )
                        {
                            world.setBlockState(posMutable, newState);

                            if (stateDown.getBlock() == FloodedBlocks.WATER_LAYER)
                            {
                                world.setBlockState(posDown, water);
                            }
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

            if (
                    world.getBlockState(pos.up()).getMaterial() != state.getMaterial()
                    &&
                    (
                        (
                            stateSide.getBlock() == FloodedBlocks.WATER_LAYER
                         && stateSide.getValue(BlockLiquidLayer.LEVEL) < state.getValue(BlockLiquidLayer.LEVEL)
                        )
                        ||
                        (
                            stateSide.getBlock() != FloodedBlocks.WATER_LAYER
                         && stateSide.getBlock().isReplaceable(world, posSide)
                        )
                    )
                    &&
                    (
                        (
                            stateSideDown.getBlock() == Blocks.WATER
                         || stateSideDown.getBlock() == FloodedBlocks.WATER_LAYER
                         || stateSideDown.isSideSolid(world, posSideDown, EnumFacing.UP)
                        )
                    )
            )
            {
                world.setBlockState(posSide, state);

                if (stateSideDown.getBlock() == FloodedBlocks.WATER_LAYER)
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
}
