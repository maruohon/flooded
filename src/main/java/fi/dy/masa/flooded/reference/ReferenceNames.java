package fi.dy.masa.flooded.reference;

public class ReferenceNames
{
    public static final String NAME_BLOCK_WATER_LAYER = "water_layer";

    public static String getPrefixedName(String name)
    {
        return Reference.MOD_ID + "_" + name;
    }

    public static String getDotPrefixedName(String name)
    {
        return Reference.MOD_ID + "." + name;
    }
}
