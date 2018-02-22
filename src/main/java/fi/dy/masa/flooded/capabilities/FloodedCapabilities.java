package fi.dy.masa.flooded.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;

public class FloodedCapabilities
{
    @CapabilityInject(IFloodedChunkCapability.class)
    public static Capability<IFloodedChunkCapability> CAPABILITY_FLOODED_CHUNK = null;

    public static void register()
    {
        CapabilityManager.INSTANCE.register(IFloodedChunkCapability.class, new DefaultChunkWaterLevelStorage<>(), () -> new FloodedChunkCapability());
    }

    public static class FloodedChunkCapabilityProvider implements ICapabilityProvider, INBTSerializable<NBTBase>
    {
        private final IFloodedChunkCapability cap;
        private static final DefaultChunkWaterLevelStorage<IFloodedChunkCapability> STORAGE = new DefaultChunkWaterLevelStorage<>();

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

        @Override
        public NBTBase serializeNBT()
        {
            return STORAGE.writeNBT(CAPABILITY_FLOODED_CHUNK, this.cap, null);
        }

        @Override
        public void deserializeNBT(NBTBase nbt)
        {
            STORAGE.readNBT(CAPABILITY_FLOODED_CHUNK, this.cap, null, nbt);
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
            IFloodedChunkCapability cap = capability.cast(instance);
            cap.setWaterLevelFromNBT(tags.getInteger("WaterLevel"));
        }
    }
}
