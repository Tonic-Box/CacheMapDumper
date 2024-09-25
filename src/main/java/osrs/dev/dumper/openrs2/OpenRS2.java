package osrs.dev.dumper.openrs2;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import osrs.dev.dumper.Dumper;
import osrs.dev.dumper.openrs2.struct.GameInfo;
import osrs.dev.util.OptionsParser;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class is responsible for fetching the latest game info from the OpenRS2 API and downloading the cache and XTEA keys.
 */
public class OpenRS2
{
    private static final Gson gson = new Gson();
    private static final String API_BASE = "https://archive.openrs2.org/";

    /**
     * Fetches the latest game info from the OpenRS2 API and downloads the cache and XTEA keys.
     *
     * @throws IOException if an I/O error occurs
     */
    public static void update() throws IOException {
        GameInfo latest = latest();

        System.out.println("Fetched latest game info: REV_" + latest.getBuilds().get(0).getMajor());
        downloadCache(latest);
        System.out.println("Downloaded cache for REV_" + latest.getBuilds().get(0).getMajor());
        downloadXTEA(latest);
        System.out.println("Downloaded XTEA keys for REV_" + latest.getBuilds().get(0).getMajor());
    }

    /**
     * Downloads the XTEA keys for the specified game info.
     *
     * @param gameInfo the game info
     * @throws IOException if an I/O error occurs
     */
    public static void downloadXTEA(GameInfo gameInfo) throws IOException {
        String api = API_BASE + "/caches/" + gameInfo.getScope() + "/" + gameInfo.getId() + "/keys.json";

        URL url = new URL(api);
        try (InputStream in = url.openStream();
             FileOutputStream fos = new FileOutputStream(new File(Dumper.XTEA_DIR, "keys.json"))) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Downloads the cache for the specified game info.
     *
     * @param gameInfo the game info
     */
    private static void downloadCache(GameInfo gameInfo) {
        String api = API_BASE + "/caches/" + gameInfo.getScope() + "/" + gameInfo.getId() + "/disk.zip";
        try (InputStream in = new URL(api).openStream();
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                File outFile = new File(Dumper.COLLISION_DIR, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!outFile.isDirectory() && !outFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + outFile);
                    }
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (OutputStream out = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches the latest game info from the OpenRS2 API.
     *
     * @return the latest game info
     * @throws IOException if an I/O error occurs
     */
    private static GameInfo latest() throws IOException {
        List<GameInfo> gameInfos = fetch();
        GameInfo latest = null;
        for (GameInfo gameInfo : gameInfos) {
            if(!gameInfo.getGame().equals("oldschool") || !gameInfo.getEnvironment().equals("live") || gameInfo.getBuilds() == null || gameInfo.getBuilds().isEmpty())
                continue;

            if(latest == null || gameInfo.getId() > latest.getId())
            {
                latest = gameInfo;
            }
        }

        System.out.println("Latest game info: " + latest.getGame() + " " + latest.getEnvironment() + " " + latest.getBuilds().get(0).getMajor());
        return latest;
    }

    /**
     * Fetches the game info from the OpenRS2 API.
     *
     * @return the game info
     * @throws IOException if an I/O error occurs
     */
    private static List<GameInfo> fetch() throws IOException {
        Type gameInfoListType = new TypeToken<List<GameInfo>>() {}.getType();
        System.out.println("Fetching game info from " + API_BASE + "caches.json");
        InputStreamReader reader = new InputStreamReader(new URL(API_BASE + "caches.json").openStream());
        return gson.fromJson(reader, gameInfoListType);
    }
}