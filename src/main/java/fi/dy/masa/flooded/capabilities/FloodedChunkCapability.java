package fi.dy.masa.flooded.capabilities;

import net.minecraft.world.chunk.Chunk;

public class FloodedChunkCapability implements IFloodedChunkCapability
{
    private int waterLevel;

    @Override
    public int getWaterLevel()
    {
        return this.waterLevel;
    }

    @Override
    public void setWaterLevel(Chunk chunk, int waterLevel)
    {
        this.waterLevel = waterLevel;
        chunk.markDirty();
    }

    @Override
    public void setWaterLevelFromNBT(int waterLevel)
    {
        this.waterLevel = waterLevel;
    }
}
