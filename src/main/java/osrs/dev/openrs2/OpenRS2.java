package osrs.dev.openrs2;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import osrs.dev.Dumper;
import osrs.dev.openrs2.struct.GameInfo;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class OpenRS2
{
    private static final Gson gson = new Gson();
    private static final String API_BASE = "https://archive.openrs2.org/";

    public static void update() throws IOException {
        GameInfo latest = latest();

        System.out.println("Fetched latest game info: REV_" + latest.getBuilds().get(0).getMajor());
        downloadCache(latest);
        System.out.println("Downloaded cache for REV_" + latest.getBuilds().get(0).getMajor());
        downloadXTEA(latest);
        System.out.println("Downloaded XTEA keys for REV_" + latest.getBuilds().get(0).getMajor());
    }

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
                    // Ensure parent directories exist
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // Write file contents
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

    private static GameInfo latest() throws IOException {
        List<GameInfo> gameInfos = fetch();
        GameInfo latest = null;
        for (GameInfo gameInfo : gameInfos) {
            if(!gameInfo.getGame().equals("oldschool") || !gameInfo.getEnvironment().equals("live"))
                continue;

            if(latest == null || gameInfo.getId() > latest.getId())
            {
                latest = gameInfo;
            }
        }
        return latest;
    }

    private static List<GameInfo> fetch() throws IOException {
        Type gameInfoListType = new TypeToken<List<GameInfo>>() {}.getType();
        System.out.println("Fetching game info from " + API_BASE + "caches.json");
        InputStreamReader reader = new InputStreamReader(new URL(API_BASE + "caches.json").openStream());
        return gson.fromJson(reader, gameInfoListType);
    }
}