package fi.dy.masa.flooded.capabilities;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;

public class FloodedChunkCapability implements INBTSerializable<NBTTagCompound>, ICapabilityProvider
{
    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        return false;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        return null;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        
    }
}
