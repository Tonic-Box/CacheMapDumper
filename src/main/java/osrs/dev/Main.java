package osrs.dev;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import osrs.dev.collision.CollisionMapFactory;
import osrs.dev.collision.ICollisionMap;
import osrs.dev.dumper.Dumper;
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
     * Load the collision map
     * @throws Exception if an error occurs loading the collision map
     */
    public static void load() throws Exception {
        configManager = new ConfigManager();
        Dumper.OUTPUT_MAP = new File(configManager.outputPath());
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
    }
}
