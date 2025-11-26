package osrs.dev;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import osrs.dev.collisionmap.CollisionMapFactory;
import osrs.dev.collisionmap.ICollisionMap;
import osrs.dev.dumper.Dumper;
import osrs.dev.tiletypemap.ITileTypeMap;
import osrs.dev.tiletypemap.TileTypeMapFactory;
import osrs.dev.ui.UIFrame;
import osrs.dev.util.ConfigManager;
import osrs.dev.util.ThreadPool;
import javax.swing.*;
import java.io.File;

/**
 * Launches the collision map viewer
 */
@Slf4j
public class Main
{
    @Getter
    private static ICollisionMap collision;
    @Getter
    private static ITileTypeMap tileTypeMap;
    @Getter
    private static ConfigManager configManager;
    private static UIFrame frame;

    /**
     * Main method
     * @param args command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception
    {
        log.info("Starting CacheMapDumper");
        FlatDarkLaf.setup();
        load();
        SwingUtilities.invokeLater(() -> {
            frame = new UIFrame();
            frame.setVisible(true);
            if(collision != null)
            {
                frame.update();
            }
            frame.requestInitialFocus();
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ThreadPool.shutdown();
            configManager.saveConfig();
        }));
    }

    /**
     * Load the collision map and tile type map
     * @throws Exception if an error occurs loading the maps
     */
    public static void load() throws Exception {
        configManager = new ConfigManager();
        String format = configManager.format();

        // Load collision map
        String collisionMapPath = configManager.getCollisionMapPath(format);
        Dumper.OUTPUT_MAP = new File(collisionMapPath);
        log.info("Looking for collision map at: {}", Dumper.OUTPUT_MAP.getPath());
        if(Dumper.OUTPUT_MAP.exists())
        {
            log.info("Loading existing collision map");
            collision = CollisionMapFactory.load(Dumper.OUTPUT_MAP.getPath());
            log.info("Collision map loaded successfully");
        }
        else
        {
            log.warn("No collision map found - viewer will start empty");
        }

        // Load tile type map
        String tileTypeMapPath = configManager.getTileTypeMapPath(format);
        Dumper.OUTPUT_TILE_TYPES = new File(tileTypeMapPath);
        log.info("Looking for tile type map at: {}", Dumper.OUTPUT_TILE_TYPES.getPath());
        if(Dumper.OUTPUT_TILE_TYPES.exists())
        {
            log.info("Loading existing tile type map");
            tileTypeMap = TileTypeMapFactory.load(Dumper.OUTPUT_TILE_TYPES.getPath());
            log.info("Tile type map loaded successfully");
        }
        else
        {
            log.warn("No tile type map found");
        }
    }
}
