package osrs.dev.util;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses command line options.
 */
@Getter
public class OptionsParser
{
    private String path = System.getProperty("user.home") + "/VitaX/collision/map.dat";
    private boolean freshCache = true;
    public OptionsParser(String[] args) {
        for(int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-path":
                    path = args[++i];
                    break;
                case "-fresh":
                    freshCache = args[++i].toLowerCase().startsWith("y");
                    break;
            }
        }
    }
}
