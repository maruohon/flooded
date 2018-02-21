package fi.dy.masa.flooded.config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.flooded.Flooded;
import fi.dy.masa.flooded.reference.Reference;

public class Configs
{
    public static final String CATEGORY_GENERIC = "Generic";

    public static String configurationFileName;
    public static Configuration config;

    public static boolean enableLoggingInfo;
    public static boolean enableWaterLayerRandomSpread;
    public static boolean floodNewChunksUnderground;
    public static boolean dimensionListIsBlacklist;
    public static int waterRiseInterval;
    private static String dimensionsStr;
    private static final Set<Integer> DIMENSIONS = new HashSet<>();

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (Reference.MOD_ID.equals(event.getModID()))
        {
            loadConfigs(config);
        }
    }

    public static void loadConfigsFromMainConfigFile(File configDirCommon)
    {
        File configFile = new File(configDirCommon, Reference.MOD_ID + ".cfg");

        loadConfigsFromFile(configFile);
    }

    private static void loadConfigsFromFile(File configFile)
    {
        configurationFileName = configFile.toString();
        config = new Configuration(configFile, null, true);

        reloadConfigsFromFile();
    }

    public static boolean reloadConfigsFromFile()
    {
        if (config != null)
        {
            Flooded.logger.info("Reloading the main configs from file '{}'", config.getConfigFile().getAbsolutePath());
            config.load();
            loadConfigs(config);

            return true;
        }

        return false;
    }

    public static void loadConfigs(Configuration conf)
    {
        Property prop;

        prop = conf.get(CATEGORY_GENERIC, "dimensionList", "0").setRequiresMcRestart(false);
        prop.setComment("The white- or blacklist of dimensions to affect. Use a comma to separate the IDs. Example: 0,4,9");
        dimensionsStr = prop.getString();

        prop = conf.get(CATEGORY_GENERIC, "dimensionListIsBlacklist", false).setRequiresMcRestart(false);
        prop.setComment("If true, then 'dimensionList' is a blacklist. If false");
        dimensionListIsBlacklist = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableLoggingInfo", false).setRequiresMcRestart(false);
        prop.setComment("Enables a bunch of extra (debug) logging on the INFO level");
        enableLoggingInfo = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "enableWaterLayerRandomSpread", true).setRequiresMcRestart(true);
        prop.setComment("If enabled, the water layers will try to spread to adjacent lower positions with random ticks");
        enableWaterLayerRandomSpread = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "floodNewChunksUnderground", true).setRequiresMcRestart(false);
        prop.setComment("If enabled, then newly generated chunks will get flooded entirely in every air\n" +
                        "space that is below the current global water level.");
        floodNewChunksUnderground = prop.getBoolean();

        prop = conf.get(CATEGORY_GENERIC, "waterRiseInterval", 6000).setRequiresMcRestart(false);
        prop.setComment("The interval in game ticks, how often the water level should rise by 1/16th of a block");
        waterRiseInterval = prop.getInt();

        if (conf.hasChanged())
        {
            conf.save();
        }

        try
        {
            DIMENSIONS.clear();
            String[] strs = dimensionsStr.split(",");

            for (String str : strs)
            {
                DIMENSIONS.add(Integer.parseInt(str.trim()));
            }
        }
        catch (Exception e)
        {
            Flooded.logger.warn("Exception while parsing the dimensionList config value", e);
        }
    }

    public static boolean enabledInDimension(int dimension)
    {
        return dimensionListIsBlacklist != DIMENSIONS.contains(dimension);
    }
}
