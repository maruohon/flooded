package fi.dy.masa.flooded.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import fi.dy.masa.flooded.reference.Reference;
import fi.dy.masa.flooded.reference.ReferenceNames;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class FloodedBlocks
{
    public static final BlockFloodedBase WATER_LAYER = new BlockLiquidLayer(ReferenceNames.NAME_BLOCK_WATER_LAYER, 4.0f, 10f, 1, Material.WATER);

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event)
    {
        IForgeRegistry<Block> registry = event.getRegistry();

        registerBlock(registry, WATER_LAYER, false);
    }

    private static void registerBlock(IForgeRegistry<Block> registry, BlockFloodedBase block, boolean isDisabled)
    {
        if (isDisabled == false)
        {
            block.setRegistryName(Reference.MOD_ID + ":" + block.getBlockName());
            registry.register(block);
        }
        else
        {
            block.setEnabled(false);
        }
    }
}
