package osrs.dev.util;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigManager
{
    @Getter
    private final File configFile = new File(System.getProperty("user.home") + "/VitaX/collision/config.cfg");
    private FileBasedConfigurationBuilder<FileBasedConfiguration> builder;

    public ConfigManager()
    {
        loadConfigFromFile();
        var map = new HashMap<String, Object>();
        map.put("output_dir", System.getProperty("user.home") + "/VitaX/");
        map.put("fresh_cache", true);
        map.put("format", "RoaringBitmap");
        map.put("bg_color", "#F8F8F8");
        map.put("grid_color", "#00FFFF");
        map.put("collision_color", "#FF0000");
        map.put("wall_color", "#000000");
        ensure(map);
    }

    public String outputDir() {
        return getString("output_dir");
    }

    /**
     * Constructs the collision map file path based on output directory and format.
     * @param format "RoaringBitmap" or "SparseBitSet"
     * @return the full file path
     */
    public String getCollisionMapPath(String format) {
        String dir = outputDir();
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            dir += "/";
        }
        if ("SparseBitSet".equalsIgnoreCase(format)) {
            return dir + "map_sparse.dat.gz";
        } else {
            return dir + "map_roaring.dat.gz";
        }
    }

    /**
     * Constructs the tile type map file path based on output directory and format.
     * @param format "RoaringBitmap" or "SparseBitSet"
     * @return the full file path
     */
    public String getTileTypeMapPath(String format) {
        String dir = outputDir();
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            dir += "/";
        }
        if ("SparseBitSet".equalsIgnoreCase(format)) {
            return dir + "tile_types_sparse.dat.gz";
        } else {
            return dir + "tile_types_roaring.dat.gz";
        }
    }

    public boolean freshCache() {
        return getBoolean("fresh_cache");
    }

    public String format() {
        return getString("format");
    }

    public Color bgColor() {
        return Color.decode(getString("bg_color"));
    }

    public String bgColorText() {
        return getString("bg_color");
    }

    public Color gridColor() {
        return Color.decode(getString("grid_color"));
    }

    public String gridColorText() {
        return getString("grid_color");
    }

    public Color collisionColor() {
        return Color.decode(getString("collision_color"));
    }

    public String collisionColorText() {
        return getString("collision_color");
    }

    public Color wallColor() {
        return Color.decode(getString("wall_color"));
    }

    public String wallColorText() {
        return getString("wall_color");
    }

    public void setOutputDir(String dir) {
        setProperty("output_dir", dir);
    }

    public void setFreshCache(boolean fresh) {
        setProperty("fresh_cache", fresh);
    }

    public void setFormat(String format) {
        setProperty("format", format);
    }

    public void setBgColor(String colorHex) {
        setProperty("bg_color", colorHex);
    }

    public void setGridColor(String colorHex) {
        setProperty("grid_color", colorHex);
    }

    public void setCollisionColor(String colorHex) {
        setProperty("collision_color", colorHex);
    }

    public void setWallColor(String colorHex) {
        setProperty("wall_color", colorHex);
    }

    /**
     * Loads the config
     */
    private void loadConfigFromFile() {
        Parameters params = new Parameters();
        if(!configFile.exists())
        {
            try {
                configFile.getParentFile().mkdirs();
                FileWriter writer = new FileWriter(configFile);
                writer.close();
            } catch (IOException ignored) {
            }
        }
        builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class).configure(params.fileBased().setFile(configFile));
        builder.setAutoSave(true);
    }

    /**
     * saves the config
     */
    @SneakyThrows
    public void saveConfig()
    {
        builder.save();
    }

    /**
     * set a config property
     * @param propertyName name
     * @param value value
     */
    @SneakyThrows
    private void setProperty(String propertyName, Object value) {
        builder.getConfiguration().setProperty(propertyName, value);
    }

    /**
     * add a new property to config
     * @param propertyName name
     * @param value value
     */
    @SneakyThrows
    private void addProperty(String propertyName, Object value) {
        builder.getConfiguration().addProperty(propertyName, value);
    }

    /**
     * get a config property
     * @param propertyName name
     * @return value
     */
    private Object getProperty(String propertyName) {
        Object property = null;
        try {
            if (builder.getConfiguration().containsKey(propertyName))
                property = builder.getConfiguration().getProperty(propertyName);
        } catch (ConfigurationException ignored) {
        }
        return property;
    }

    /**
     * get a property as a string
     * @param propertyName name
     * @return value
     */
    private String getString(String propertyName) {
        String property = null;
        try {
            if (builder.getConfiguration().containsKey(propertyName))
                property = builder.getConfiguration().getString(propertyName);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return property;
    }

    /**
     * get a property as a boolean
     * @param propertyName property
     * @return value
     */
    private boolean getBoolean(String propertyName) {
        try {
            if (!builder.getConfiguration().containsKey(propertyName)) return false;
            return builder.getConfiguration().getBoolean(propertyName);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ensures values exist in a config, if they do not it created them with the default
     * value supplied.
     *
     * @param configMap map of key -> value
     */
    private void ensure(Map<String,Object> configMap)
    {
        configMap.forEach((key, value) -> {
            if (getProperty(key) == null) {
                addProperty(key, value);
            }
        });
    }
}
