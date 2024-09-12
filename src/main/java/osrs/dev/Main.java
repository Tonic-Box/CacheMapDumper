package osrs.dev;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.Getter;
import osrs.dev.reader.CollisionMap;
import osrs.dev.ui.UIFrame;
import osrs.dev.util.ThreadPool;

import javax.swing.*;
import java.io.IOException;

/**
 * Main class for the application
 */
public class Main
{
    @Getter
    private static CollisionMap collision;
    private static UIFrame frame;

    /**
     * Main method
     * @param args command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception
    {
        FlatDarkLaf.setup();
        Runtime.getRuntime().addShutdownHook(new Thread(ThreadPool::shutdown));
        if(Dumper.OUTPUT_MAP.exists())
        {
            collision = CollisionMap.load(Dumper.OUTPUT_MAP.getPath());
        }
        SwingUtilities.invokeLater(() -> {
            frame = new UIFrame();
            frame.setVisible(true);
            if(collision != null)
            {
                frame.update();
            }
            frame.requestInitialFocus();
        });
    }

    /**
     * Load the collision map
     * @throws IOException if an error occurs
     * @throws ClassNotFoundException if an error occurs
     */
    public static void load() throws IOException, ClassNotFoundException {
        if(Dumper.OUTPUT_MAP.exists())
        {
            collision = CollisionMap.load(Dumper.OUTPUT_MAP.getPath());
        }
    }
}
