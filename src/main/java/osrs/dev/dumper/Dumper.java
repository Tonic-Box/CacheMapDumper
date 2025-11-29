package osrs.dev.dumper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import net.runelite.cache.ObjectManager;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Location;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;
import net.runelite.cache.util.KeyProvider;
import net.runelite.cache.util.XteaKeyManager;
import osrs.dev.dumper.openrs2.OpenRS2;
import osrs.dev.util.OptionsParser;
import osrs.dev.util.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Dumps collision data from the cache.
 */
@Getter
public class Dumper
{
    public static File OUTPUT_MAP = new File(System.getProperty("user.home") + "/VitaX/collision/map.dat");
    public static File OUTPUT_OBJECT_MAP = new File(System.getProperty("user.home") + "/VitaX/objects.dat");
    public static final String COLLISION_DIR = System.getProperty("user.home") + "/VitaX/collision/";
    public static final String CACHE_DIR = COLLISION_DIR + "/cache/";
    public static final String XTEA_DIR = COLLISION_DIR + "/keys/";
    private final RegionLoader regionLoader;
    private final ObjectManager objectManager;
    private final CollisionMapWriter collisionMapWriter;
    private final ObjectMapWriter objectMapWriter;
    private final ObjectMapWriterCompressed objectMapWriterCompressed;

    private static OptionsParser optionsParser;

    /**
     * Creates a new dumper.
     *
     * @param store the cache store
     * @param keyProvider the XTEA key provider
     */
    public Dumper(Store store, KeyProvider keyProvider) throws IOException
    {
        this.regionLoader = new RegionLoader(store, keyProvider);
        this.objectManager = new ObjectManager(store);
        this.collisionMapWriter = new CollisionMapWriter();
        this.objectMapWriter = new ObjectMapWriter();
        this.objectMapWriterCompressed = new ObjectMapWriterCompressed();
        objectManager.load();
        regionLoader.loadRegions();
        regionLoader.calculateBounds();
    }

    /**
     * Finds an object definition by id.
     *
     * @param id the id
     * @return the object definition, or {@code null} if not found
     */
    private ObjectDefinition findObject(int id)
    {
        return objectManager.getObject(id);
    }

    /**
     * Dumps the collision data.
     *
     * @param args the command-line arguments
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException
    {
        optionsParser = new OptionsParser(args);
        OUTPUT_MAP = new File(optionsParser.getPath());
        OUTPUT_OBJECT_MAP = new File(optionsParser.getObjectPath());
        ensureDirectory(COLLISION_DIR);
        ensureDirectory(XTEA_DIR);
        if(optionsParser.isFreshCache() || isDirectoryEmpty(new File(CACHE_DIR)) || !(new File(CACHE_DIR)).exists())
        {
            OpenRS2.update();
        }

        XteaKeyManager xteaKeyManager = new XteaKeyManager();
        try (FileInputStream fin = new FileInputStream(XTEA_DIR + "keys.json")) {
            Field keys = xteaKeyManager.getClass().getDeclaredField("keys");
            keys.setAccessible(true);
            Map<Integer, int[]> cKeys = (Map<Integer, int[]>) keys.get(xteaKeyManager);
            List<Map<String, Object>> keyList = new Gson().fromJson(new InputStreamReader(fin, StandardCharsets.UTF_8), new TypeToken<List<Map<String, Object>>>() {
            }.getType());

            for (Map<String, Object> entry : keyList) {
                int regionId = ((Double) entry.get("mapsquare")).intValue();  // mapsquare is your regionId
                List<Double> keyListDoubles = (List<Double>) entry.get("key");
                int[] keysArray = keyListDoubles.stream().mapToInt(Double::intValue).toArray();
                cKeys.put(regionId, keysArray);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        File base = new File(CACHE_DIR);

        try (Store store = new Store(base))
        {
            store.load();

            Dumper dumper = new Dumper(store, xteaKeyManager);

            Collection<Region> regions = dumper.regionLoader.getRegions();

            int total = regions.size();

            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<Void>> futures = new ArrayList<>();

            for (Region region : regions)
            {
                futures.add(executor.submit(() ->
                {
                    dumper.processRegion(region);
                    return null;
                }));
            }

            executor.shutdown();

            int n = 0;
            ProgressBar progressBar = new ProgressBar(total, 50);
            for (Future<Void> future : futures)
            {
                future.get(); // wait for task to complete
                progressBar.update(++n);
            }
            dumper.collisionMapWriter.save(OUTPUT_MAP.getPath());
            System.out.println("Wrote collision map to " + OUTPUT_MAP);

            // Save compressed version (v2)
            String compressedPath = OUTPUT_OBJECT_MAP.getPath();
            dumper.objectMapWriterCompressed.save(compressedPath);
            System.out.println("Wrote compressed object map to " + compressedPath);
        }
        catch (ExecutionException | InterruptedException e)
        {
            System.err.println("Error processing region");
            e.printStackTrace();
        }
    }

    /**
     * Processes a region.
     *
     * @param region the region
     */
    private void processRegion(Region region)
    {
        int baseX = region.getBaseX();
        int baseY = region.getBaseY();
        for (int z = 0; z < Region.Z; z++) {
            for (int localX = 0; localX < Region.X; localX++) {
                int regionX = baseX + localX;
                for (int localY = 0; localY < Region.Y; localY++) {
                    int regionY = baseY + localY;

                    boolean isBridge = (region.getTileSetting(1, localX, localY) & 2) != 0;
                    int tileZ = z + (isBridge ? 1 : 0);

                    for (Location loc : region.getLocations()) {
                        Position pos = loc.getPosition();
                        if (pos.getX() != regionX || pos.getY() != regionY || pos.getZ() != tileZ)
                        {
                            continue;
                        }

                        // Store the object ID at this coordinate (both formats)
                        objectMapWriter.addObject((short) regionX, (short) regionY, (byte) z, loc.getId());
                        objectMapWriterCompressed.addObject((short) regionX, (short) regionY, (byte) z, loc.getId());

                        int type = loc.getType();
                        int orientation = loc.getOrientation();
                        ObjectDefinition object = findObject(loc.getId());

                        if (object == null)
                        {
                            continue;
                        }

                        // Handle tiles without a floor
                        int underlayId = region.getUnderlayId(z < 3 ? tileZ : z, localX, localY);
                        int overlayId = region.getOverlayId(z < 3 ? tileZ : z, localX, localY);
                        boolean noFloor = underlayId == 0 && overlayId == 0;

                        if(noFloor)
                        {
                            collisionMapWriter.fullBlocking((short) regionX, (short) regionY, (byte) z, true);
                        }

                        // Handle no-move tiles
                        int floorType = region.getTileSetting(z < 3 ? tileZ : z, localX, localY);
                        if (floorType == 1 || // water, rooftop wall
                                floorType == 3 || // bridge wall
                                floorType == 5 || // house wall/roof
                                floorType == 7 || // house wall
                                noFloor)
                        {
                            collisionMapWriter.fullBlocking((short) regionX, (short) regionY, (byte) z, true);
                        }

                        boolean block = (Exclusion.matches(loc.getId()) == null)
                                ? !(object.getName().toLowerCase().contains("door") || object.getName().toLowerCase().contains("gate"))
                                : Boolean.FALSE.equals(Exclusion.matches(loc.getId()));

                        block = object.getName().toLowerCase().contains("trapdoor") || block;

                        if (Exclusion.matches(loc.getId()) != null && Boolean.TRUE.equals(Exclusion.matches(loc.getId()))) {
                            continue;
                        }

                        int sizeX = (orientation == 1 || orientation == 3) ? object.getSizeY() : object.getSizeX();
                        int sizeY = (orientation == 1 || orientation == 3) ? object.getSizeX() : object.getSizeY();

                        // Handle walls and doors
                        if (type >= 0 && type <= 3)
                        {
                            if (type == 0 || type == 2)
                            {
                                switch (orientation)
                                {
                                    case 0: // wall on west
                                        collisionMapWriter.westBlocking((short) regionX, (short) regionY, (byte) z, block);
                                        break;
                                    case 1: // wall on north
                                        collisionMapWriter.northBlocking((short) regionX, (short) regionY, (byte) z, block);
                                        break;
                                    case 2: // wall on east
                                        collisionMapWriter.eastBlocking((short) regionX, (short) regionY, (byte) z, block);
                                        break;
                                    case 3: // wall on south
                                        collisionMapWriter.southBlocking((short) regionX, (short) regionY, (byte) z, block);
                                        break;
                                }
                            }
                        }

                        // Handle double walls
                        if (type == 2)
                        {
                            if (orientation == 3) //west
                            {
                                collisionMapWriter.westBlocking((short) regionX, (short) regionY, (byte) z, block);
                            }
                            else if (orientation == 0) //north
                            {
                                collisionMapWriter.northBlocking((short) regionX, (short) regionY, (byte) z, block);
                            }
                            else if (orientation == 1) //east
                            {
                                collisionMapWriter.eastBlocking((short) regionX, (short) regionY, (byte) z, block);
                            }
                            else if (orientation == 2) //south
                            {
                                collisionMapWriter.southBlocking((short) regionX, (short) regionY, (byte) z, block);
                            }
                        }

                        // Handle diagonal walls (simplified)
                        if (type == 9)
                        {
                            collisionMapWriter.fullBlocking((short) regionX, (short) regionY, (byte) z, block);
                        }

                        //objects
                        if (type == 22 || (type >= 9 && type <= 11) || (type >= 12 && type <= 21))
                        {
                            for (int x = 0; x < sizeX; x++)
                            {
                                for (int y = 0; y < sizeY; y++)
                                {
                                    if (object.getInteractType() != 0 && (object.getWallOrDoor() == 1 || (type >= 10 && type <= 21)))
                                    {
                                        collisionMapWriter.fullBlocking((short) (regionX + x), (short) (regionY + y), (byte) z, block);
                                    }
                                }
                            }
                        }
                    }

                    // Handle tiles without a floor
                    int underlayId = region.getUnderlayId(z < 3 ? tileZ : z, localX, localY);
                    int overlayId = region.getOverlayId(z < 3 ? tileZ : z, localX, localY);
                    boolean noFloor = underlayId == 0 && overlayId == 0;

                    if(noFloor)
                    {
                        collisionMapWriter.fullBlocking((short) regionX, (short) regionY, (byte) z, true);
                    }

                    // Handle no-move tiles
                    int floorType = region.getTileSetting(z < 3 ? tileZ : z, localX, localY);
                    if (floorType == 1 || // water, rooftop wall
                            floorType == 3 || // bridge wall
                            floorType == 5 || // house wall/roof
                            floorType == 7 || // house wall
                            noFloor)
                    {
                        collisionMapWriter.fullBlocking((short) regionX, (short) regionY, (byte) z, true);
                    }
                }
            }
        }
    }

    /**
     * Ensures a directory exists.
     *
     * @param dir the directory
     */
    private static void ensureDirectory(String dir)
    {
        File file = new File(dir);
        if (!file.exists())
        {
            if (!file.mkdirs())
            {
                throw new RuntimeException("Unable to create directory " + dir);
            }
        }
    }

    /**
     * Checks if a directory is empty.
     *
     * @param directory the directory
     * @return {@code true} if the directory is empty, otherwise {@code false}
     */
    public static boolean isDirectoryEmpty(File directory) {
        if (directory.isDirectory()) {
            String[] files = directory.list();
            return files != null && files.length == 0;
        }
        return false;
    }
}