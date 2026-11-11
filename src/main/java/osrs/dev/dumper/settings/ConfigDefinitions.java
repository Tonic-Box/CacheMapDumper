package osrs.dev.dumper.settings;

import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.StructManager;
import net.runelite.cache.definitions.EnumDefinition;
import net.runelite.cache.definitions.ParamDefinition;
import net.runelite.cache.definitions.StructDefinition;
import net.runelite.cache.definitions.loaders.EnumLoader;
import net.runelite.cache.definitions.loaders.ParamLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Store;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The enum, param and struct configs of a cache, read the way client scripts read them.
 */
final class ConfigDefinitions
{
    private final Map<Integer, EnumDefinition> enums = new HashMap<>();
    private final Map<Integer, ParamDefinition> params = new HashMap<>();
    private final StructManager structs;

    private ConfigDefinitions(StructManager structs)
    {
        this.structs = structs;
    }

    /**
     * Loads the configs of a cache.
     * @param store the loaded cache
     * @return the configs
     * @throws IOException if the cache cannot be read
     */
    static ConfigDefinitions load(Store store) throws IOException
    {
        StructManager structs = new StructManager(store);
        structs.load();
        ConfigDefinitions definitions = new ConfigDefinitions(structs);

        EnumLoader enumLoader = new EnumLoader();
        for (FSFile file : files(store, ConfigType.ENUM))
        {
            definitions.enums.put(file.getFileId(), enumLoader.load(file.getFileId(), file.getContents()));
        }

        ParamLoader paramLoader = new ParamLoader();
        for (FSFile file : files(store, ConfigType.PARAMS))
        {
            definitions.params.put(file.getFileId(), paramLoader.load(file.getContents()));
        }
        return definitions;
    }

    /**
     * @param id the enum id
     * @return the enum, or null when there is none
     */
    EnumDefinition enumDefinition(int id)
    {
        return enums.get(id);
    }

    /**
     * @param id the struct id
     * @return the struct, or null when there is none
     */
    StructDefinition struct(int id)
    {
        return structs.getStruct(id);
    }

    /**
     * Reads an int from an enum by key, as enum_get does.
     * @param definition the enum
     * @param key the key
     * @return the value, or the enum's default when the key is absent
     */
    static int enumInt(EnumDefinition definition, int key)
    {
        int[] keys = definition.getKeys();
        int[] values = definition.getIntVals();
        if (keys != null && values != null)
        {
            for (int i = 0; i < keys.length; i++)
            {
                if (keys[i] == key)
                {
                    return values[i];
                }
            }
        }
        return definition.getDefaultInt();
    }

    /**
     * Reads a string from an enum by key, as enum_get does.
     * @param definition the enum
     * @param key the key
     * @return the value, or the enum's default when the key is absent
     */
    static String enumString(EnumDefinition definition, int key)
    {
        int[] keys = definition.getKeys();
        String[] values = definition.getStringVals();
        if (keys != null && values != null)
        {
            for (int i = 0; i < keys.length; i++)
            {
                if (keys[i] == key)
                {
                    return values[i];
                }
            }
        }
        return definition.getDefaultString();
    }

    /**
     * Reads an int param from a struct, as param_struct does.
     * @param struct the struct
     * @param param the param id
     * @return the value, or the param's default when the struct does not set it
     */
    int intParam(StructDefinition struct, int param)
    {
        Object value = struct.getParams() == null ? null : struct.getParams().get(param);
        if (value instanceof Integer)
        {
            return (Integer) value;
        }
        ParamDefinition definition = params.get(param);
        return definition == null ? -1 : definition.getDefaultInt();
    }

    /**
     * Reads a string param from a struct, as param_struct does.
     * @param struct the struct
     * @param param the param id
     * @return the value, or the param's default when the struct does not set it
     */
    String stringParam(StructDefinition struct, int param)
    {
        Object value = struct.getParams() == null ? null : struct.getParams().get(param);
        if (value instanceof String)
        {
            return (String) value;
        }
        ParamDefinition definition = params.get(param);
        return definition == null ? null : definition.getDefaultString();
    }

    /**
     * @param struct the struct
     * @param param the param id
     * @return whether the struct sets the param itself
     */
    static boolean hasParam(StructDefinition struct, int param)
    {
        return struct.getParams() != null && struct.getParams().containsKey(param);
    }

    private static Iterable<FSFile> files(Store store, ConfigType type) throws IOException
    {
        Index index = store.getIndex(IndexType.CONFIGS);
        Archive archive = index.getArchive(type.getId());
        byte[] data = store.getStorage().loadArchive(archive);
        ArchiveFiles files = archive.getFiles(data);
        return files.getFiles();
    }
}
