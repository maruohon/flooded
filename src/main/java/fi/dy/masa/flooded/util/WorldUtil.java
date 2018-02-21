package fi.dy.masa.flooded.util;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import fi.dy.masa.flooded.block.BlockLiquidLayer;
import fi.dy.masa.flooded.block.FloodedBlocks;

public class WorldUtil
{
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
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        IBlockState layerBase = FloodedBlocks.WATER_LAYER.getDefaultState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        final int waterLevelBlocks = waterLevel >> 4;
        final int layerLevel = waterLevel & 0xF;
        final int xBase = chunkX << 4;
        final int zBase = chunkZ << 4;

        if (layerLevel != 0)
        {
            final int y = waterLevelBlocks + 1;
            IBlockState layer = layerBase.withProperty(BlockLiquidLayer.LEVEL, layerLevel);

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
                        //System.out.printf("replacing %s with %s\n", stateOld, layer);
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
        final int waterLevelBlocksNew = (newWaterLevel >> 4);
        final int layerLevel = oldWaterLevel & 0xF;

        if (layerLevel != 0)
        {
            final int y = waterLevelBlocksOld + 1;
            IBlockState layer = layerBase.withProperty(BlockLiquidLayer.LEVEL, layerLevel);

            for (int x = 0; x < 16; x++)
            {
                for (int z = 0; z < 16; z++)
                {
                    posMutable.setPos(x, y, z);

                    IBlockState stateOld = chunk.getBlockState(x, y, z);

                    if (stateOld.getBlock() == FloodedBlocks.WATER_LAYER)
                    {
                        if (waterLevelBlocksNew != waterLevelBlocksOld)
                        {
                            chunk.setBlockState(posMutable, water);
                        }
                        else
                        {
                            chunk.setBlockState(posMutable, layer);
                        }
                    }
                }
            }
        }
    }
}
