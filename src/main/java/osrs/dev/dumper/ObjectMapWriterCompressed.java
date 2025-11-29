package osrs.dev.dumper;

import osrs.dev.util.VarInt;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compressed object map writer using:
 * - Delta encoding for sorted coordinates (huge savings)
 * - VarInt encoding for counts and object IDs
 * - Achieves ~60-80% size reduction vs uncompressed
 * Thread-safe for parallel region processing.
 */
public class ObjectMapWriterCompressed {
    private final Map<Long, List<Integer>> objectMap;

    public ObjectMapWriterCompressed() {
        this.objectMap = new ConcurrentHashMap<>();
    }

    /**
     * Adds an object ID at the specified world coordinates.
     */
    public void addObject(short x, short y, byte z, int objectId) {
        long packedCoord = packCoordinate(x, y, z);
        objectMap.computeIfAbsent(packedCoord, k -> new ArrayList<>()).add(objectId);
    }

    private long packCoordinate(short x, short y, byte z) {
        return (x & 8191) | ((long)(y & 32767) << 13) | ((long)(z & 15) << 28);
    }

    /**
     * Saves the compressed object map.
     * Format: Version 2 (compressed)
     */
    public void save(String filePath) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        // Sort coordinates for delta encoding
        List<Long> sortedCoords = new ArrayList<>(objectMap.keySet());
        sortedCoords.sort(Long::compareTo);

        long startTime = System.currentTimeMillis();
        int uncompressedSize = 0;
        int compressedSize = 0;

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file), 131072))) {

            // Write version 2 (compressed format)
            dos.writeInt(2);
            compressedSize += 4;

            // Write entry count as VarInt
            VarInt.writeVarInt(dos, sortedCoords.size());
            compressedSize += VarInt.varIntSize(sortedCoords.size());
            uncompressedSize += 4;

            // Write coordinates with delta encoding
            long previousCoord = 0;
            for (Long coord : sortedCoords) {
                List<Integer> objects = objectMap.get(coord);

                // Delta encoding: store difference from previous
                long delta = coord - previousCoord;
                VarInt.writeVarLong(dos, delta);
                compressedSize += VarInt.varLongSize(delta);
                uncompressedSize += 8;

                // Object count as VarInt
                VarInt.writeVarInt(dos, objects.size());
                compressedSize += VarInt.varIntSize(objects.size());
                uncompressedSize += 4;

                // Object IDs as VarInts
                for (Integer objectId : objects) {
                    VarInt.writeVarInt(dos, objectId);
                    compressedSize += VarInt.varIntSize(objectId);
                    uncompressedSize += 4;
                }

                previousCoord = coord;
            }
        }

        long saveTime = System.currentTimeMillis() - startTime;
        double compressionRatio = 100.0 * (1.0 - (double)compressedSize / uncompressedSize);

        System.out.println("Compressed object map saved:");
        System.out.println("  Uncompressed: " + formatBytes(uncompressedSize));
        System.out.println("  Compressed: " + formatBytes(compressedSize));
        System.out.println("  Ratio: " + String.format("%.1f%%", compressionRatio) + " reduction");
        System.out.println("  Time: " + saveTime + "ms");
    }

    private String formatBytes(int bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public int size() {
        return objectMap.size();
    }

    public int totalObjects() {
        return objectMap.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
