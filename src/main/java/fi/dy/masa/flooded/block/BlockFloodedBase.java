package fi.dy.masa.flooded.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;

public class BlockFloodedBase extends Block
{
    protected String blockName;
    protected String[] unlocalizedNames;
    protected boolean enabled = true;

    public BlockFloodedBase(String name, float hardness, float resistance, int harvestLevel, Material material)
    {
        super(material);

        this.setHardness(hardness);
        this.setResistance(resistance);
        this.setHarvestLevel("pickaxe", harvestLevel);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setSoundType(SoundType.STONE);
        this.blockName = name;
        this.unlocalizedNames = this.generateUnlocalizedNames();
    }

    public String getBlockName()
    {
        return this.blockName;
    }

    public boolean hasSpecialHitbox()
    {
        return false;
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return this.getMetaFromState(state);
    }

    protected String[] generateUnlocalizedNames()
    {
        return new String[] { this.blockName };
    }

    public String[] getUnlocalizedNames()
    {
        return this.unlocalizedNames;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public BlockFloodedBase setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        return this;
    }
}
