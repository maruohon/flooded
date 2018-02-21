package fi.dy.masa.flooded.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class FloodedCapabilities
{
    @CapabilityInject(IFloodedChunkCapability.class)
    public static Capability<IFloodedChunkCapability> CAPABILITY_FLOODED_CHUNK = null;

    public static void register()
    {
        CapabilityManager.INSTANCE.register(IFloodedChunkCapability.class, new DefaultChunkWaterLevelStorage<>(), () -> new FloodedChunkCapability());
    }

    public static class FloodedChunkCapabilityProvider implements ICapabilityProvider
    {
        private final FloodedChunkCapability cap;

        public FloodedChunkCapabilityProvider()
        {
            this.cap = new FloodedChunkCapability();
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing)
        {
            return capability == CAPABILITY_FLOODED_CHUNK;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing)
        {
            return capability == CAPABILITY_FLOODED_CHUNK ? CAPABILITY_FLOODED_CHUNK.cast(this.cap) : null;
        }
    }

    private static class DefaultChunkWaterLevelStorage<T extends IFloodedChunkCapability> implements Capability.IStorage<T>
    {
        @Override
        public NBTBase writeNBT(Capability<T> capability, T instance, EnumFacing side)
        {
            if ((instance instanceof IFloodedChunkCapability) == false)
            {
                throw new RuntimeException(instance.getClass().getName() + " does not implement IFloodedChunkCapability");
            }

            NBTTagCompound nbt = new NBTTagCompound();
            IFloodedChunkCapability cap = (IFloodedChunkCapability) instance;
            nbt.setInteger("WaterLevel", cap.getWaterLevel());

            return nbt;
        }

        @Override
        public void readNBT(Capability<T> capability, T instance, EnumFacing side, NBTBase nbt)
        {
            if ((instance instanceof IFloodedChunkCapability) == false)
            {
                throw new RuntimeException(instance.getClass().getName() + " does not implement IFloodedChunkCapability");
            }

            NBTTagCompound tags = (NBTTagCompound) nbt;
            IFloodedChunkCapability cap = (IFloodedChunkCapability) instance;
            cap.setWaterLevelFromNBT(tags.getInteger("WaterLevel"));
        }
    }
}
