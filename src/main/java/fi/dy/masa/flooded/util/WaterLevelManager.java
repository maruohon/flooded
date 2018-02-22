package fi.dy.masa.flooded.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import fi.dy.masa.flooded.Flooded;
import fi.dy.masa.flooded.block.BlockLiquidLayer;
import fi.dy.masa.flooded.reference.Reference;

public class WaterLevelManager
{
    public static final WaterLevelManager INSTANCE = new WaterLevelManager();
    private final Map<Integer, Integer> waterLevels = new HashMap<>();
    private int scheduleCount;
    private boolean dirty;

    public int getWaterLevelInDimension(int dimension)
    {
        Integer storedLevel = this.waterLevels.get(dimension);

        if (storedLevel != null)
        {
            return storedLevel.intValue();
        }

        World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dimension);
        int level = world != null ? world.getSeaLevel() * BlockLiquidLayer.DIVISOR : 63 * BlockLiquidLayer.DIVISOR;
        Flooded.logInfo("Initialized the water level in dimension {} to {}", dimension, WorldUtil.getWaterLevelString(level));

        this.setWaterLevelInDimension(dimension, level);

        return level;
    }

    public int getScheduleCount()
    {
        return this.scheduleCount;
    }

    public void setScheduleCount(int count)
    {
        this.dirty |= (count != this.scheduleCount);
        this.scheduleCount = count;
    }

    public void setWaterLevelInDimension(int dimension, int level)
    {
        this.waterLevels.put(dimension, level);
        this.dirty = true;
    }

    public void readFromDisk(@Nullable File saveDir)
    {
        // Clear the data structures when reading the data for a world/save, so that data
        // from another world won't carry over to a world/save that doesn't have the file yet.
        this.waterLevels.clear();

        if (saveDir != null)
        {
            File floodedDataDir = getModDataDirectoryInWorld(saveDir);
            File file = new File(floodedDataDir, "data_tracker.dat");

            if (file.exists() && file.isFile() && file.canRead())
            {
                try
                {
                    FileInputStream is = new FileInputStream(file);
                    this.readFromNBT(CompressedStreamTools.readCompressed(is));
                    is.close();
                }
                catch (Exception e)
                {
                    Flooded.logger.warn("Failed to read WaterLevelManager data from file '{}'", file.getAbsolutePath());
                }
            }
        }
    }

    public void writeToDisk()
    {
        if (this.dirty)
        {
            try
            {
                File saveDir = DimensionManager.getCurrentSaveRootDirectory();

                if (saveDir == null)
                {
                    return;
                }

                File floodedDataDir = getModDataDirectoryInWorld(saveDir);

                if (floodedDataDir.exists() == false && floodedDataDir.mkdirs() == false)
                {
                    Flooded.logger.warn("Failed to create the save directory '{}'", floodedDataDir.getAbsolutePath());
                    return;
                }

                File fileTmp = new File(floodedDataDir, "data_tracker.dat.tmp");
                File fileReal = new File(floodedDataDir, "data_tracker.dat");
                FileOutputStream os = new FileOutputStream(fileTmp);
                CompressedStreamTools.writeCompressed(this.writeToNBT(new NBTTagCompound()), os);
                os.close();

                if (fileReal.exists())
                {
                    fileReal.delete();
                }

                fileTmp.renameTo(fileReal);
                this.dirty = false;
            }
            catch (Exception e)
            {
                Flooded.logger.warn("Failed to write WaterLevelManager data to file", e);
            }
        }
    }

    private void readFromNBT(NBTTagCompound nbt)
    {
        if (nbt != null)
        {
            if (nbt.hasKey("WaterLevels", Constants.NBT.TAG_LIST))
            {
                NBTTagList tagList = nbt.getTagList("WaterLevels", Constants.NBT.TAG_COMPOUND);
                final int count = tagList.tagCount();

                for (int i = 0; i < count; ++i)
                {
                    NBTTagCompound tag = tagList.getCompoundTagAt(i);

                    if (tag.hasKey("Dimension", Constants.NBT.TAG_INT) &&
                        tag.hasKey("WaterLevel", Constants.NBT.TAG_INT))
                    {
                        this.waterLevels.put(tag.getInteger("Dimension"), tag.getInteger("WaterLevel"));
                    }
                }
            }

            this.scheduleCount = nbt.getInteger("ScheduleCount");
        }
    }

    private NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<Integer, Integer> entry : this.waterLevels.entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Dimension", entry.getKey());
            tag.setInteger("WaterLevel", entry.getValue());

            tagList.appendTag(tag);
        }

        nbt.setTag("WaterLevels", tagList);
        nbt.setInteger("ScheduleCount", this.scheduleCount);

        return nbt;
    }

    public static File getModDataDirectoryInWorld(File worldDir)
    {
        return new File(new File(worldDir, "data"), Reference.MOD_ID);
    }
}
