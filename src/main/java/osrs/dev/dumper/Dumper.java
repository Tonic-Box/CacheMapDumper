package osrs.dev.dumper;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.cache.ObjectManager;
import net.runelite.cache.OverlayManager;
import net.runelite.cache.UnderlayManager;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.definitions.OverlayDefinition;
import net.runelite.cache.definitions.UnderlayDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Location;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;
import net.runelite.cache.util.KeyProvider;
import net.runelite.cache.util.XteaKeyManager;
import osrs.dev.collisionmap.CollisionMapFactory;
import osrs.dev.collisionmap.ICollisionMapWriter;
import osrs.dev.tiletypemap.ITileTypeMapWriter;
import osrs.dev.tiletypemap.TileTypeMapFactory;
import osrs.dev.dumper.openrs2.OpenRS2;
import osrs.dev.tiletypemap.TileType;
import osrs.dev.util.OptionsParser;
import osrs.dev.util.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Dumps collision data from the cache.
 */
@Getter
@Slf4j
public class Dumper
{
    public static File OUTPUT_MAP = new File(System.getProperty("user.home") + "/VitaX/map_roaring.dat.gz");
    public static File OUTPUT_TILE_TYPES = new File(System.getProperty("user.home") + "/VitaX/tile_types_roaring.dat.gz");
    public static final String COLLISION_DIR = System.getProperty("user.home") + "/VitaX/cachedumper/";
    public static final String CACHE_DIR = COLLISION_DIR + "/cache/";
    public static final String XTEA_DIR = COLLISION_DIR + "/keys/";
    private final RegionLoader regionLoader;
    private final ObjectManager objectManager;
    private final OverlayManager overlayManager;
    private final UnderlayManager underlayManager;
    private final ICollisionMapWriter collisionMapWriter;
    private final ITileTypeMapWriter tileTypeMapWriter;

    // Coordinate bounds tracking
    private int minX = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    private static OptionsParser optionsParser;
    private static CollisionMapFactory.Format format = CollisionMapFactory.Format.ROARING;

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
        this.overlayManager = new OverlayManager(store);
        this.underlayManager = new UnderlayManager(store);
        this.collisionMapWriter = CollisionMapFactory.createWriter(format);
        // Convert CollisionMapFactory.Format to TileTypeMapFactory.Format
        TileTypeMapFactory.Format tileTypeFormat = format == CollisionMapFactory.Format.SPARSE_BITSET
                ? TileTypeMapFactory.Format.SPARSE_BITSET
                : TileTypeMapFactory.Format.ROARING;
        this.tileTypeMapWriter = TileTypeMapFactory.createWriter(tileTypeFormat);
        objectManager.load();
        overlayManager.load();
        underlayManager.load();
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
     * Finds an overlay definition by id.
     *
     * @param id the id
     * @return the overlay definition, or {@code null} if not found
     */
    private OverlayDefinition findOverlay(int id)
    {
        return overlayManager.provide(id);
    }

    /**
     * Finds an underlay definition by id.
     *
     * @param id the id
     * @return the underlay definition, or {@code null} if not found
     */
    private UnderlayDefinition findUnderlay(int id)
    {
        return underlayManager.provide(id);
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
        format = optionsParser.getFormat();
        OUTPUT_MAP = new File(optionsParser.getCollisionMapPath());
        OUTPUT_TILE_TYPES = new File(optionsParser.getTileTypeMapPath());

        log.info("Dumper options - dir: {}, format: {}", optionsParser.getOutputDir(), format);
        log.info("Collision map path: {}", OUTPUT_MAP.getPath());
        log.info("Tile type map path: {}", OUTPUT_TILE_TYPES.getPath());
        ensureDirectory(optionsParser.getOutputDir());
        ensureDirectory(COLLISION_DIR);
        ensureDirectory(XTEA_DIR);
        boolean fresh = optionsParser.isFreshCache();
        boolean emptyDir = isDirectoryEmpty(new File(COLLISION_DIR));
        boolean cacheDoesntExist = !(new File(COLLISION_DIR)).exists();
        if(fresh || emptyDir || cacheDoesntExist)
        {
            log.debug("Downloading fresh cache and XTEA keys (fresh={}, emptyDir={}, cacheDoesntExist={})", fresh, emptyDir, cacheDoesntExist);
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
            log.info("Wrote collision map to {}", OUTPUT_MAP.getPath());
            dumper.tileTypeMapWriter.save(OUTPUT_TILE_TYPES.getPath());
            log.info("Wrote tile type map to {}", OUTPUT_TILE_TYPES.getPath());

            // Log coordinate bounds and calculate bits needed
            log.info("=== COORDINATE BOUNDS ===");
            log.info("X range: {} to {} (span: {})", dumper.minX, dumper.maxX, dumper.maxX - dumper.minX + 1);
            log.info("Y range: {} to {} (span: {})", dumper.minY, dumper.maxY, dumper.maxY - dumper.minY + 1);
            log.info("Z range: {} to {} (span: {})", dumper.minZ, dumper.maxZ, dumper.maxZ - dumper.minZ + 1);

            // Calculate bits needed for unsigned representation
            int bitsX = 32 - Integer.numberOfLeadingZeros(dumper.maxX);
            int bitsY = 32 - Integer.numberOfLeadingZeros(dumper.maxY);
            int bitsZ = dumper.maxZ == 0 ? 1 : 32 - Integer.numberOfLeadingZeros(dumper.maxZ);
            log.info("Bits needed (unsigned): X={}, Y={}, Z={}", bitsX, bitsY, bitsZ);

            // If using signed representation (for relative offsets)
            int maxAbsX = Math.max(Math.abs(dumper.minX), Math.abs(dumper.maxX));
            int maxAbsY = Math.max(Math.abs(dumper.minY), Math.abs(dumper.maxY));
            int maxAbsZ = Math.max(Math.abs(dumper.minZ), Math.abs(dumper.maxZ));
            int bitsXSigned = maxAbsX == 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxAbsX) + 1; // +1 for sign
            int bitsYSigned = maxAbsY == 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxAbsY) + 1;
            int bitsZSigned = maxAbsZ == 0 ? 1 : 32 - Integer.numberOfLeadingZeros(maxAbsZ) + 1;
            log.info("Bits needed (signed): X={}, Y={}, Z={}", bitsXSigned, bitsYSigned, bitsZSigned);
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
                    // Track coordinate bounds
                    minX = Math.min(minX, regionX);
                    maxX = Math.max(maxX, regionX);
                    minY = Math.min(minY, regionY);
                    maxY = Math.max(maxY, regionY);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);

                    // processDebugging(region, localX, localY, z, regionX, regionY);
                    processCollisionOfRegionCoordinate(region, localX, localY, z, regionX, regionY);
                    processTileTypesOfRegionCoordinate(region, localX, localY, z, regionX, regionY);
                }
            }
        }
    }

    private final Map<Integer, String> DEBUG_TILES = ImmutableMap.<Integer, String>builder()
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(2177, 3040, 0, 0), "Disease water 1")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(2177, 3043, 0, 0), "Disease water 2")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(3427, 2719, 0, 0), "Storm water 1")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(3427, 2715, 0, 0), "Storm water 2")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(3427, 2712, 0, 0), "Storm water 3")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(3427, 2707, 0, 0), "Storm water 4")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(3427, 2702, 0, 0), "Storm water 5")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(1799, 2366, 0, 0), "Kelp water 1")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(1799, 2364, 0, 0), "Kelp water 2")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(1799, 2362, 0, 0), "Kelp water 3")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(1799, 2358, 0, 0), "Kelp water 4")
            .put(ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(1799, 2355, 0, 0), "Kelp water 5")
            .build();


    private void processDebugging(Region region, int localX, int localY, int plane, int regionX, int regionY) {
        int packed = ConfigurableCoordIndexer.ROARINGBITMAP_5BIT_DATA_COORD_INDEXER.packToBitmapIndex(regionX, regionY, plane, 0);
        if (!DEBUG_TILES.containsKey(packed)) {
            return;
        }

        StringBuilder output = new StringBuilder();
        String tileName = DEBUG_TILES.get(packed);

        output.append("=== DEBUG TILE: ").append(tileName).append(" (").append(regionX).append(", ").append(regionY).append(", ").append(plane).append(") ===\n");
        output.append("Local coords: (").append(localX).append(", ").append(localY).append(")\n");
        output.append("Region base: (").append(region.getBaseX()).append(", ").append(region.getBaseY()).append(")\n");

        // Bridge detection
        boolean isBridge = (region.getTileSetting(1, localX, localY) & 2) != 0;
        int tileZ = plane + (isBridge ? 1 : 0);
        output.append("Is bridge: ").append(isBridge).append(", tileZ: ").append(tileZ).append("\n");

        // Raw Region array values for the calculated tileZ
        int effectivePlane = plane < 3 ? tileZ : plane;
        output.append("RAW REGION VALUES (using effectivePlane=").append(effectivePlane).append(")\n");
        output.append("  tileHeights[").append(effectivePlane).append("][").append(localX).append("][").append(localY).append("] = ")
                .append(region.getTileHeight(effectivePlane, localX, localY)).append("\n");
        output.append("  tileSettings[").append(effectivePlane).append("][").append(localX).append("][").append(localY).append("] = ")
                .append(region.getTileSetting(effectivePlane, localX, localY)).append("\n");
        output.append("  underlayIds[").append(effectivePlane).append("][").append(localX).append("][").append(localY).append("] = ")
                .append(region.getUnderlayId(effectivePlane, localX, localY)).append("\n");
        output.append("  overlayIds[").append(effectivePlane).append("][").append(localX).append("][").append(localY).append("] = ")
                .append(region.getOverlayId(effectivePlane, localX, localY)).append("\n");
        output.append("  overlayPaths[").append(effectivePlane).append("][").append(localX).append("][").append(localY).append("] = ")
                .append(region.getOverlayPath(effectivePlane, localX, localY)).append("\n");
        output.append("  overlayRotations[").append(effectivePlane).append("][").append(localX).append("][").append(localY).append("] = ")
                .append(region.getOverlayRotation(effectivePlane, localX, localY)).append("\n");

        // Also log for all planes to show full context
        output.append("RAW VALUES FOR ALL PLANES:\n");
        for (int z = 0; z < 4; z++) {
            output.append("  Plane ").append(z).append(": height=").append(region.getTileHeight(z, localX, localY))
                    .append(", settings=").append(region.getTileSetting(z, localX, localY))
                    .append(", underlay=").append(region.getUnderlayId(z, localX, localY))
                    .append(", overlay=").append(region.getOverlayId(z, localX, localY))
                    .append(", overlayPath=").append(region.getOverlayPath(z, localX, localY))
                    .append(", overlayRot=").append(region.getOverlayRotation(z, localX, localY)).append("\n");
        }

        // Processed values
        int floorType = region.getTileSetting(effectivePlane, localX, localY);
        int underlayId = region.getUnderlayId(effectivePlane, localX, localY);
        int overlayId = region.getOverlayId(effectivePlane, localX, localY);
        boolean noFloor = underlayId == 0 && overlayId == 0;
        output.append("PROCESSED: Floor type/settings: ").append(floorType).append(", No floor: ").append(noFloor).append("\n");

        /*
        // Underlay definition
        if (underlayId > 0) {
            UnderlayDefinition underlayDef = findUnderlay(underlayId - 1);
            if (underlayDef != null) {
                // Calculate HSL values to populate transient fields
                underlayDef.calculateHsl();

                output.append("UNDERLAY DEFINITION (ID ").append(underlayId).append("):\n");
                output.append("  Color (RGB): ").append(underlayDef.getColor()).append("\n");
                output.append("  Hue: ").append(underlayDef.getHue()).append("\n");
                output.append("  Saturation: ").append(underlayDef.getSaturation()).append("\n");
                output.append("  Lightness: ").append(underlayDef.getLightness()).append("\n");
                output.append("  Hue Multiplier: ").append(underlayDef.getHueMultiplier()).append("\n");
            } else {
                output.append("UNDERLAY DEFINITION (ID ").append(underlayId).append("): Not found\n");
            }
        }
        */
        // Overlay definition
        if (overlayId > 0) {
            OverlayDefinition overlayDef = findOverlay(overlayId - 1);
            if (overlayDef != null) {
                // Calculate HSL values to populate transient fields
                overlayDef.calculateHsl();

                output.append("OVERLAY DEFINITION (ID ").append(overlayId).append("):\n");
                output.append("  Id inside: ").append(overlayDef.getId()).append("\n");
                output.append("  Texture/Sprite ID: ").append(overlayDef.getTexture()).append("\n");
                output.append("  RGB Color: ").append(overlayDef.getRgbColor()).append("\n");
                output.append("  Secondary RGB Color: ").append(overlayDef.getSecondaryRgbColor()).append("\n");
                /*

                output.append("  Hide Underlay: ").append(overlayDef.isHideUnderlay()).append("\n");
                output.append("  Hue: ").append(overlayDef.getHue()).append("\n");
                output.append("  Saturation: ").append(overlayDef.getSaturation()).append("\n");
                output.append("  Lightness: ").append(overlayDef.getLightness()).append("\n");
                output.append("  Other Hue (from secondary color): ").append(overlayDef.getOtherHue()).append("\n");
                output.append("  Other Saturation (from secondary color): ").append(overlayDef.getOtherSaturation()).append("\n");
                output.append("  Other Lightness (from secondary color): ").append(overlayDef.getOtherLightness()).append("\n");
                 */
            } else {
                output.append("OVERLAY DEFINITION (ID ").append(overlayId).append("): Not found\n");
            }
        }

        /*

        // Objects on this tile
        output.append("Objects on this tile:\n");
        int objectCount = 0;
        for (Location loc : region.getLocations()) {
            Position pos = loc.getPosition();
            if (pos.getX() != regionX || pos.getY() != regionY || pos.getZ() != tileZ) {
                continue;
            }

            objectCount++;
            ObjectDefinition object = findObject(loc.getId());

            output.append("  Object #").append(objectCount).append(": ID=").append(loc.getId())
                    .append(", Type=").append(loc.getType())
                    .append(", Orientation=").append(loc.getOrientation()).append("\n");

            if (object != null) {
                boolean isExcluded = Exclusion.matches(loc.getId()) != null;
                Boolean exclusionValue = Exclusion.matches(loc.getId());
                boolean block = (exclusionValue == null)
                        ? !(object.getName().toLowerCase().contains("door") || object.getName().toLowerCase().contains("gate"))
                        : Boolean.FALSE.equals(exclusionValue);
                block = object.getName().toLowerCase().contains("trapdoor") || block;

                int sizeX = (loc.getOrientation() == 1 || loc.getOrientation() == 3) ? object.getSizeY() : object.getSizeX();
                int sizeY = (loc.getOrientation() == 1 || loc.getOrientation() == 3) ? object.getSizeX() : object.getSizeY();

                output.append("    Name: '").append(object.getName()).append("', Size: ").append(sizeX).append("x").append(sizeY)
                        .append(", InteractType: ").append(object.getInteractType())
                        .append(", WallOrDoor: ").append(object.getWallOrDoor()).append("\n");
                output.append("    Block: ").append(block).append(", Excluded: ").append(isExcluded)
                        .append(" (value: ").append(exclusionValue).append(")\n");
            } else {
                output.append("    Object definition not found\n");
            }
        }

        if (objectCount == 0) {
            output.append("  (No objects on this tile)\n");
        }

         */
        output.append("=== END DEBUG TILE ===\n");

        // Write to file
        String debugDir = COLLISION_DIR + "/debug/";
        ensureDirectory(debugDir);
        String fileName = String.format("debug_%s_%d_%d_%d.txt", tileName.replaceAll("[^a-zA-Z0-9]", "_"), regionX, regionY, plane);
        File debugFile = new File(debugDir + fileName);

        try (java.io.FileWriter writer = new java.io.FileWriter(debugFile)) {
            writer.write(output.toString());
            log.info("Debug tile info written to: {}", debugFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write debug file for tile {} at ({}, {}, {})", tileName, regionX, regionY, plane, e);
        }
    }

    private void processArbitraryDataOfRegionCoordinate(Region region, int localX, int localY, int plane, int regionX, int regionY) {

    }

    private void processTileTypesOfRegionCoordinate(Region region, int localX, int localY, int plane, int regionX, int regionY) {
        boolean isBridge = (region.getTileSetting(1, localX, localY) & 2) != 0;
        int tileZ = plane + (isBridge ? 1 : 0);
        int effectivePlane = plane < 3 ? tileZ : plane;

        int overlayId = region.getOverlayId(effectivePlane, localX, localY);
        if (overlayId <= 0) {
            return;
        }

        OverlayDefinition overlayDef = findOverlay(overlayId - 1);
        if (overlayDef == null) {
            return;
        }

        int textureId = overlayDef.getTexture();
        Byte tileType = TileType.SPRITE_ID_TO_TILE_TYPE.get(textureId);
        if (tileType != null && tileType > 0) {
            tileTypeMapWriter.setTileType(regionX, regionY, plane, tileType);
        }
    }

    private void processCollisionOfRegionCoordinate(Region region, int localX, int localY, int plane, int regionX, int regionY) {
        boolean isBridge = (region.getTileSetting(1, localX, localY) & 2) != 0;
        int tileZ = plane + (isBridge ? 1 : 0);

        for (Location loc : region.getLocations()) {
            Position pos = loc.getPosition();
            if (pos.getX() != regionX || pos.getY() != regionY || pos.getZ() != tileZ)
            {
                continue;
            }

            int type = loc.getType();
            int orientation = loc.getOrientation();
            ObjectDefinition object = findObject(loc.getId());

            if (object == null)
            {
                continue;
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
                            collisionMapWriter.westBlocking(regionX, regionY, plane, block);
                            break;
                        case 1: // wall on north
                            collisionMapWriter.northBlocking(regionX, regionY, plane, block);
                            break;
                        case 2: // wall on east
                            collisionMapWriter.eastBlocking(regionX, regionY, plane, block);
                            break;
                        case 3: // wall on south
                            collisionMapWriter.southBlocking(regionX, regionY, plane, block);
                            break;
                    }
                }
            }

            // Handle double walls
            if (type == 2)
            {
                if (orientation == 3) //west
                {
                    collisionMapWriter.westBlocking(regionX, regionY, plane, block);
                }
                else if (orientation == 0) //north
                {
                    collisionMapWriter.northBlocking(regionX, regionY, plane, block);
                }
                else if (orientation == 1) //east
                {
                    collisionMapWriter.eastBlocking(regionX, regionY, plane, block);
                }
                else if (orientation == 2) //south
                {
                    collisionMapWriter.southBlocking(regionX, regionY, plane, block);
                }
            }

            // Handle diagonal walls (simplified)
            if (type == 9)
            {
                collisionMapWriter.fullBlocking(regionX, regionY, plane, block);
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
                            collisionMapWriter.fullBlocking(regionX + x, regionY + y, plane, block);
                        }
                    }
                }
            }
        }

        // Handle tiles without a floor
        int underlayId = region.getUnderlayId(plane < 3 ? tileZ : plane, localX, localY);
        int overlayId = region.getOverlayId(plane < 3 ? tileZ : plane, localX, localY);
        boolean noFloor = underlayId == 0 && overlayId == 0;

        if(noFloor)
        {
            collisionMapWriter.fullBlocking(regionX, regionY, plane, true);
        }

        // Handle no-move tiles
        int floorType = region.getTileSetting(plane < 3 ? tileZ : plane, localX, localY);
        if (floorType == 1 || // water, rooftop wall
                floorType == 3 || // bridge wall
                floorType == 5 || // house wall/roof
                floorType == 7 || // house wall
                noFloor)
        {
            collisionMapWriter.fullBlocking(regionX, regionY, plane, true);
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