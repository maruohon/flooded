package fi.dy.masa.flooded.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.flooded.config.Configs;
import fi.dy.masa.flooded.util.WorldUtil;

public class BlockLiquidLayer extends BlockFloodedBase
{
    public static final int DIVISOR = 8;
    public static final int LEVEL_BITMASK = 0x7;
    public static final int BITMASK_SIZE = 3;

    public static final AxisAlignedBB BOUNDS_LAYER_01 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  1D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_02 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  2D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_03 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  3D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_04 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  4D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_05 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  5D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_06 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  6D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_07 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  7D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_08 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  8D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_09 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,  9D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_10 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 10D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_11 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 11D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_12 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 12D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_13 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 13D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_14 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 14D / 16D, 1.0D);
    public static final AxisAlignedBB BOUNDS_LAYER_15 = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 15D / 16D, 1.0D);

    public static final PropertyInteger LEVEL = BlockLiquid.LEVEL;
    private final List<AxisAlignedBB> boundsList = new ArrayList<>();

    public BlockLiquidLayer(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(name, hardness, resistance, harvestLevel, material);

        // dummy, needed because we have to use BlockLiquid.LEVEL which is 0..15
        // And we have to use that property because some parts of vanilla code assume
        // that a block with Material.WATER has that LEVEL property...
        this.boundsList.add(BOUNDS_LAYER_01); // level 0 = height 8/8 (not used)

        this.boundsList.add(BOUNDS_LAYER_14); // level 1 = height 7/8
        this.boundsList.add(BOUNDS_LAYER_12); // level 2 = height 6/8
        this.boundsList.add(BOUNDS_LAYER_10); // level 3 = height 5/8
        this.boundsList.add(BOUNDS_LAYER_08); // level 4 = height 4/8
        this.boundsList.add(BOUNDS_LAYER_06); // level 5 = height 3/8
        this.boundsList.add(BOUNDS_LAYER_04); // level 6 = height 2/8
        this.boundsList.add(BOUNDS_LAYER_02); // level 7 = height 1/8

        this.boundsList.add(BOUNDS_LAYER_08); // The rest are not used
        this.boundsList.add(BOUNDS_LAYER_09);
        this.boundsList.add(BOUNDS_LAYER_10);
        this.boundsList.add(BOUNDS_LAYER_11);
        this.boundsList.add(BOUNDS_LAYER_12);
        this.boundsList.add(BOUNDS_LAYER_13);
        this.boundsList.add(BOUNDS_LAYER_14);
        this.boundsList.add(BOUNDS_LAYER_15);

        this.setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, Integer.valueOf(7)));
        this.setTickRandomly(Configs.enableWaterLayerRandomSpread);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] { LEVEL });
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(LEVEL, Integer.valueOf(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(LEVEL).intValue();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        return this.boundsList.get(state.getValue(LEVEL).intValue());
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        return NULL_AABB;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos)
    {
        return this.blockMaterial != Material.LAVA;
    }

    @Override
    public boolean isFullCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isCollidable()
    {
        return false;
    }

    @Override
    public int tickRate(World worldIn)
    {
        if (this.blockMaterial == Material.WATER)
        {
            return 10;
        }
        else if (this.blockMaterial == Material.LAVA)
        {
            return worldIn.provider.isNether() ? 10 : 30;
        }
        else
        {
            return 0;
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand)
    {
        if (world.isRemote == false)
        {
            WorldUtil.trySpreadWaterLayer(world, pos, state, true);
        }
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random)
    {
        if (world.isRemote == false)
        {
            WorldUtil.trySpreadWaterLayer(world, pos, state, false);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        if (world.isRemote == false)
        {
            WorldUtil.trySpreadWaterLayer(world, pos, state, false);
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random)
    {
        return 0;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
    {
        return BlockFaceShape.UNDEFINED;
    }

    @SuppressWarnings("deprecation")
    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side)
    {
        BlockPos posSide = pos.offset(side);
        IBlockState stateSide = blockAccess.getBlockState(posSide);

        if (stateSide.getBlock() == this && stateSide.getValue(LEVEL) >= state.getValue(LEVEL))
        {
            return false;
        }
        else
        {
            return side == EnumFacing.UP ? true : super.shouldSideBeRendered(state, blockAccess, pos, side);
        }
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer()
    {
        return this.blockMaterial == Material.WATER ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.SOLID;
    }

    @SideOnly (Side.CLIENT)
    @Override
    public Vec3d getFogColor(World world, BlockPos pos, IBlockState state, Entity entity, Vec3d originalColor, float partialTicks)
    {
        Vec3d viewport = net.minecraft.client.renderer.ActiveRenderInfo.projectViewFromEntity(entity, partialTicks);

        if (state.getMaterial().isLiquid())
        {
            float height = 0.0F;

            if (state.getBlock() == this)
            {
                height = (float) state.getValue(LEVEL) / DIVISOR;
            }

            float f1 = (pos.getY() + 1) - height;

            if (viewport.y > f1)
            {
                BlockPos upPos = pos.up();
                IBlockState upState = world.getBlockState(upPos);
                return upState.getBlock().getFogColor(world, upPos, upState, entity, originalColor, partialTicks);
            }
        }

        return super.getFogColor(world, pos, state, entity, originalColor, partialTicks);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand)
    {
        double x = (double) pos.getX();
        double y = (double) pos.getY();
        double z = (double) pos.getZ();

        if (this.blockMaterial == Material.WATER)
        {
            /*int level = stateIn.getValue(LEVEL).intValue();

            if (level > 0 && level < 8)
            {
                if (rand.nextInt(64) == 0)
                {
                    worldIn.playSound(x + 0.5D, y + 0.5D, z + 0.5D, SoundEvents.BLOCK_WATER_AMBIENT, SoundCategory.BLOCKS, rand.nextFloat() * 0.25F + 0.75F, rand.nextFloat() + 0.5F, false);
                }
            }
            else*/ if (rand.nextInt(10) == 0)
            {
                worldIn.spawnParticle(EnumParticleTypes.SUSPENDED, x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(), 0.0D, 0.0D, 0.0D);
            }
        }

        if (this.blockMaterial == Material.LAVA &&
            worldIn.getBlockState(pos.up()).getMaterial() == Material.AIR &&
            worldIn.getBlockState(pos.up()).isOpaqueCube() == false)
        {
            if (rand.nextInt(100) == 0)
            {
                double rx = x + rand.nextFloat();
                double ry = y + stateIn.getBoundingBox(worldIn, pos).maxY;
                double rz = z + rand.nextFloat();
                worldIn.spawnParticle(EnumParticleTypes.LAVA, rx, ry, rz, 0.0D, 0.0D, 0.0D);
                worldIn.playSound(rx, ry, rz, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }

            if (rand.nextInt(200) == 0)
            {
                worldIn.playSound(x, y, z, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        }

        if (rand.nextInt(10) == 0 && worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP))
        {
            Material material = worldIn.getBlockState(pos.down(2)).getMaterial();

            if (material.blocksMovement() ==false && material.isLiquid() == false)
            {
                double rx = x + rand.nextFloat();
                double ry = y - 1.05D;
                double rz = z + rand.nextFloat();

                if (this.blockMaterial == Material.WATER)
                {
                    worldIn.spawnParticle(EnumParticleTypes.DRIP_WATER, rx, ry, rz, 0.0D, 0.0D, 0.0D);
                }
                else
                {
                    worldIn.spawnParticle(EnumParticleTypes.DRIP_LAVA, rx, ry, rz, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    /**
     * Returns the surface height in the range 1..8, where 8 is full block and 1 is 1/8
     * @param state
     * @return
     */
    public static int getSurfaceHeight(IBlockState state)
    {
        return DIVISOR - state.getValue(LEVEL);
    }

    /**
     * Return true if the liquid surface level of the block <b>stateTarget</b>
     * is higher than that of <b>stateReference</b>.
     * @param stateTarget
     * @param stateReference
     * @return
     */
    public static boolean isLevelHigher(IBlockState stateTarget, IBlockState stateReference)
    {
        // The LEVEL property is inversed compared to the surface level in vanilla
        // (LEVEL == 0 is a source block in vanilla), and we are also following that convention.
        return stateTarget.getValue(LEVEL) < stateReference.getValue(LEVEL);
    }

    /**
     * Returns the state with the requested surface level, where surfaceLevel is the actual
     * height of the surface in 1/8ths of the block. Valid range for surfaceLevel is thus 1..7
     * @param stateOriginal
     * @param surfaceLevel
     * @return
     */
    public static IBlockState getStateWithSurfaceLevelOf(IBlockState stateOriginal, int surfaceLevel)
    {
        return stateOriginal.withProperty(LEVEL, DIVISOR - surfaceLevel);
    }
}
