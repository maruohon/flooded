package fi.dy.masa.flooded.capabilities;

import net.minecraft.world.chunk.Chunk;

public interface IFloodedChunkCapability
{
    /**
     * Returns the last water level this chunk was updated to
     * @return
     */
    int getWaterLevel();

    /**
     * Sets the current water level in this chunk
     * @param waterLevel
     */
    void setWaterLevel(Chunk chunk, int waterLevel);

    /**
     * Sets the current water level in this chunk read from the stored capability NBT
     * @param waterLevel
     */
    void setWaterLevelFromNBT(int waterLevel);
}
