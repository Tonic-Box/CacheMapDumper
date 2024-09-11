package osrs.dev;

import com.formdev.flatlaf.FlatDarkLaf;
import lombok.Getter;
import osrs.dev.reader.GlobalCollisionMap;
import osrs.dev.ui.UIFrame;

import javax.swing.*;
import java.io.IOException;

public class Main
{
    @Getter
    private static GlobalCollisionMap collision;
    private static UIFrame frame;
    public static void main(String[] args) throws Exception
    {
        FlatDarkLaf.setup();
        if(Dumper.OUTPUT_MAP.exists())
        {
            collision = GlobalCollisionMap.load(Dumper.OUTPUT_MAP.getPath());
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

    public static void load() throws IOException, ClassNotFoundException {
        if(Dumper.OUTPUT_MAP.exists())
        {
            collision = GlobalCollisionMap.load(Dumper.OUTPUT_MAP.getPath());
        }
    }
}
