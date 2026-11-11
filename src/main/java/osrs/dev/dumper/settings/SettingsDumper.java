package osrs.dev.dumper.settings;

import net.runelite.cache.fs.Store;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Generates the SettingID source from a cache's settings menu.
 */
public final class SettingsDumper
{
    private SettingsDumper()
    {
    }

    /**
     * Writes the SettingID source for a cache.
     * @param store the loaded cache
     * @param output the file to write
     * @throws IOException if the cache cannot be read or the file cannot be written
     */
    public static void write(Store store, File output) throws IOException
    {
        SettingsMenu menu = SettingsMenu.read(ConfigDefinitions.load(store));
        File parent = output.getAbsoluteFile().getParentFile();
        if (parent != null)
        {
            Files.createDirectories(parent.toPath());
        }
        Files.write(output.toPath(), new SettingIdWriter(menu).source().getBytes(StandardCharsets.UTF_8));
    }
}
