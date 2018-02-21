package fi.dy.masa.flooded.capabilities;

public interface IFloodedChunkCapability
{
    /**
     * Returns the last water level this chunk was updated to
     * @return
     */
    int getLastWaterLevel();

    void setWaterLevel(int waterLevel);
}
