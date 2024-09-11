package osrs.dev;

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
import osrs.dev.dumper.Exclusion;
import osrs.dev.dumper.GlobalCollisionMapWriter;
import osrs.dev.dumper.TileExclusion;
import osrs.dev.dumper.openrs2.OpenRS2;
import osrs.dev.util.OptionsParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Getter
public class Dumper
{
    //new File("C:\\Users\\zacke\\IdeaProjects\\VitaX-fixed\\src\\main\\resources\\VitaX\\services\\local\\pathfinder", "map.dat");
    public static File OUTPUT_MAP = new File(System.getProperty("user.home") + "/VitaX/collision/map.dat");
    public static final String COLLISION_DIR = System.getProperty("user.home") + "/VitaX/collision/";
    public static final String CACHE_DIR = COLLISION_DIR + "/cache/";
    public static final String XTEA_DIR = COLLISION_DIR + "/keys/";
    private final RegionLoader regionLoader;
    private final ObjectManager objectManager;
    private final GlobalCollisionMapWriter collisionMapWriter;

    private static OptionsParser optionsParser;

    public Dumper(Store store, KeyProvider keyProvider)
    {
        this(store, new RegionLoader(store, keyProvider));
    }

    public Dumper(Store store, RegionLoader regionLoader)
    {
        this.regionLoader = regionLoader;
        this.objectManager = new ObjectManager(store);
        this.collisionMapWriter = new GlobalCollisionMapWriter();
    }

    public Dumper load() throws IOException
    {
        objectManager.load();
        regionLoader.loadRegions();
        regionLoader.calculateBounds();
        return this;
    }

    private ObjectDefinition findObject(int id)
    {
        return objectManager.getObject(id);
    }

    public static void main(String[] args) throws IOException
    {
        optionsParser = new OptionsParser(args);
        OUTPUT_MAP = new File(optionsParser.getPath());
        ensureDirectory(COLLISION_DIR);
        ensureDirectory(XTEA_DIR);
        if(optionsParser.isFreshCache())
        {
            OpenRS2.update();
        }

        XteaKeyManager xteaKeyManager = new XteaKeyManager();
        try (FileInputStream fin = new FileInputStream(XTEA_DIR + "keys.json"))
        {
            xteaKeyManager.loadKeys(fin);
        }

        File base = new File(CACHE_DIR);

        try (Store store = new Store(base))
        {
            store.load();

            Dumper dumper = new Dumper(store, xteaKeyManager);
            dumper.load();

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
            for (Future<Void> future : futures)
            {
                future.get(); // wait for task to complete
                if (++n % 100 == 0)
                {
                    System.out.println("Processed " + n + " / " + total + " regions");
                }
            }

           dumper.collisionMapWriter.save(OUTPUT_MAP.getPath());
            System.out.println("Wrote collision map to " + OUTPUT_MAP);
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

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

                        int type = loc.getType();
                        int orientation = loc.getOrientation();
                        ObjectDefinition object = findObject(loc.getId());

                        if (object == null || TileExclusion.isExcluded(regionX, regionY, z))
                        {
                            continue;
                        }

                        boolean block = (Exclusion.matches(loc.getId()) == null)
                                ? !(object.getName().toLowerCase().contains("door") || object.getName().toLowerCase().contains("gate"))
                                : Boolean.FALSE.equals(Exclusion.matches(loc.getId()));

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
}