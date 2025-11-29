# Tile Type Dumping System

This document explains how the tile type dumping mechanism works in the CacheMapDumper project.

## Overview

The tile type system identifies and categorizes special terrain types (primarily water variants) by examining overlay sprite/texture IDs from the OSRS cache and mapping them to predefined tile type constants.

## Architecture

```
Cache Store
    │
    ▼
RegionLoader (extracts regions)
    │
    ▼
For each Region tile:
    ├─ Get Overlay ID from region data
    ├─ Lookup OverlayDefinition by ID
    ├─ Extract Texture/Sprite ID
    └─ Check SPRITE_ID_TO_TILE_TYPE mapping
    │
    ▼
TileTypeMapWriter (decomposes byte → 4 bits)
    │
    ▼
ITileDataMapWriter (stores bits in bitmap)
    ├─ RoaringTileDataMapWriter (RoaringBitmap)
    └─ SparseTileDataMapWriter (SparseBitSet)
    │
    ▼
Serialize + GZIP compress → File
```

## Core Components

### 1. Tile Type Definitions (`TileType.java`)

Defines 12 water-type constants as byte values:

| Constant | Value | Description |
|----------|-------|-------------|
| `WATER` | 1 | Standard water |
| `CRANDOR_SMEGMA_WATER` | 2 | Crandor variant |
| `TEMPOR_STORM_WATER` | 3 | Tempoross storm water |
| `DISEASE_WATER` | 4 | Diseased water |
| `KELP_WATER` | 5 | Kelp variant |
| `SUNBAKED_WATER` | 6 | Sun-baked variant |
| `JAGGED_REEFS_WATER` | 7 | Jagged reefs |
| `SHARP_CRYSTAL_WATER` | 8 | Sharp crystal water |
| `ICE_WATER` | 9 | Ice water |
| `NE_PURPLE_GRAY_WATER` | 10 | Northeast purple-gray |
| `NW_GRAY_WATER` | 11 | Northwest gray |
| `SE_PURPLE_WATER` | 12 | Southeast purple |

### 2. Sprite-to-Type Mapping

An `ImmutableMap<Integer, Byte> SPRITE_ID_TO_TILE_TYPE` maps overlay sprite IDs to tile types:

```java
// Example mappings
Sprite ID 1, 130-133       → WATER
Sprite ID 136-139          → TEMPOR_STORM_WATER
Sprite ID 140-144          → SUNBAKED_WATER
Sprite ID 160-161, 163-164 → ICE_WATER
```

### 3. Extraction Logic (`Dumper.java:449-469`)

```java
private void processTileTypesOfRegionCoordinate(
    Region region, int localX, int localY, int plane,
    int regionX, int regionY) {

    // Step 1: Adjust for bridges
    boolean isBridge = (region.getTileSetting(1, localX, localY) & 2) != 0;
    int tileZ = plane + (isBridge ? 1 : 0);
    int effectivePlane = plane < 3 ? tileZ : plane;

    // Step 2: Get overlay ID from region data
    int overlayId = region.getOverlayId(effectivePlane, localX, localY);
    if (overlayId <= 0) return;

    // Step 3: Lookup overlay definition from cache
    OverlayDefinition overlayDef = findOverlay(overlayId - 1);
    if (overlayDef == null) return;

    // Step 4: Extract texture/sprite ID
    int textureId = overlayDef.getTexture();

    // Step 5: Map sprite ID to tile type
    Byte tileType = TileType.SPRITE_ID_TO_TILE_TYPE.get(textureId);

    // Step 6: Write to map if found
    if (tileType != null && tileType > 0) {
        tileTypeMapWriter.setTileType(regionX, regionY, plane, tileType);
    }
}
```

**Key insight:** Water tiles are identified by their overlay **texture IDs**, not by explicit water flags.

## Data Storage Layer

### Coordinate Packing (`ConfigurableCoordIndexer.java`)

3D coordinates are bit-packed into a single 32-bit integer:

```
Bits 0-11:  X coordinate (12 bits, base offset 480)
Bits 12-25: Y coordinate (14 bits)
Bits 26-27: Plane       (2 bits, 0-3)
Bits 28-31: Data bits   (4-5 bits for tile type value)
```

**Preset configurations:**
- `ROARINGBITMAP_5BIT_DATA_COORD_INDEXER` - 32-bit, 5 data bits (values 0-31)
- `SPARSEBITSET_4BIT_DATA_COORD_INDEXER` - 31-bit, 4 data bits (values 0-15)

### Tile Type Encoding (`TileTypeMapWriter.java:22-27`)

Tile types are decomposed into individual bits:

```java
public void setTileType(int x, int y, int plane, byte type) {
    if ((type & 0b0001) != 0) setDataBit(x, y, plane, 0);
    if ((type & 0b0010) != 0) setDataBit(x, y, plane, 1);
    if ((type & 0b0100) != 0) setDataBit(x, y, plane, 2);
    if ((type & 0b1000) != 0) setDataBit(x, y, plane, 3);
}
```

Each coordinate can store up to 4 bits → values 0-15 (sufficient for 12 water types).

### Storage Implementations

#### RoaringBitmap (Default)

**Files:** `RoaringTileDataMap.java`, `RoaringTileDataMapWriter.java`

- Uses `RoaringBitmap` native binary serialization
- `runOptimize()` called before save for maximum compression
- Internally uses run-length encoding for dense regions, arrays for sparse
- **Typical compression:** 50-80% for sparse water data

#### SparseBitSet (Legacy)

**Files:** `SparseTileDataMap.java`, `SparseTileDataMapWriter.java`

- Uses Java `ObjectOutputStream` serialization
- Limited to 31-bit indices (signed int limitation)
- Generally larger file sizes than RoaringBitmap

## File Format

### Naming Conventions

| Filename Pattern | Format | Compression |
|------------------|--------|-------------|
| `*roaring*.dat` | RoaringBitmap | None |
| `*roaring*.dat.gz` | RoaringBitmap | GZIP |
| `*sparse*.dat` | SparseBitSet | None |
| `*sparse*.dat.gz` | SparseBitSet | GZIP |

**Default output:** `~/VitaX/tile_types_roaring.dat.gz`

### Format Detection (`TileTypeMapFactory.java:47-73`)

1. Check filename for "roaring" or "sparse"
2. Detect `.gz` extension for GZIP
3. Create appropriate input stream wrapper
4. Load via format-specific deserializer

## Reading Tile Types

### Loading (`TileTypeMapFactory.load()`)

```java
// Detects format from filename, creates appropriate loader
ITileDataMap dataMap = TileTypeMapFactory.load(path);
TileTypeMap tileTypeMap = new TileTypeMap(dataMap);
```

### Querying (`TileTypeMap.java:25-33`)

```java
public byte getTileType(int x, int y, int plane) {
    int data = 0;
    for (int i = 0; i <= getIndexer().getMaxDataBitIndex(); i++) {
        if (isDataBitSet(x, y, plane, i)) {
            data |= (1 << i);  // Reconstruct byte from bits
        }
    }
    return (byte) data;
}
```

## Class Hierarchy

```
                    ┌─────────────────────┐
                    │  ITileDataMapWriter │  (bit-level write interface)
                    └─────────────────────┘
                              ▲
              ┌───────────────┴───────────────┐
              │                               │
┌─────────────────────────┐    ┌─────────────────────────┐
│ RoaringTileDataMapWriter│    │ SparseTileDataMapWriter │
└─────────────────────────┘    └─────────────────────────┘
              ▲
              │
┌─────────────────────────┐
│   TileTypeMapWriter     │  (byte → 4-bit decomposition)
└─────────────────────────┘


                    ┌─────────────────────┐
                    │    ITileDataMap     │  (bit-level read interface)
                    └─────────────────────┘
                              ▲
              ┌───────────────┴───────────────┐
              │                               │
┌─────────────────────────┐    ┌─────────────────────────┐
│    RoaringTileDataMap   │    │    SparseTileDataMap    │
└─────────────────────────┘    └─────────────────────────┘
              ▲
              │
┌─────────────────────────┐
│      TileTypeMap        │  (4-bit → byte reconstruction)
└─────────────────────────┘
```

## Key File Locations

| Component | Path |
|-----------|------|
| Type Constants | `src/main/java/osrs/dev/tiletypemap/TileType.java` |
| Type Map Reader | `src/main/java/osrs/dev/tiletypemap/TileTypeMap.java` |
| Type Map Writer | `src/main/java/osrs/dev/tiletypemap/TileTypeMapWriter.java` |
| Factory | `src/main/java/osrs/dev/tiletypemap/TileTypeMapFactory.java` |
| Coord Indexer | `src/main/java/osrs/dev/dumper/ConfigurableCoordIndexer.java` |
| Main Dumper | `src/main/java/osrs/dev/dumper/Dumper.java` |
| Roaring Impl | `src/main/java/osrs/dev/tiledatamap/roaring/` |
| Sparse Impl | `src/main/java/osrs/dev/tiledatamap/sparse/` |

## Summary

1. **Detection:** Overlay sprite IDs from cache → mapped via `SPRITE_ID_TO_TILE_TYPE`
2. **Encoding:** Byte tile type → 4 individual bits stored per coordinate
3. **Packing:** (X, Y, Plane, DataBitIndex) → single 32-bit index
4. **Storage:** RoaringBitmap (compressed) or SparseBitSet (legacy)
5. **Output:** GZIP-compressed `.dat.gz` file

The system is designed for efficient sparse data storage - most tiles have no type, so only non-zero positions are stored in the bitmap.

---

## Visualization & Rendering

The project includes a UI viewer that can render tile type data as a color-coded map.

### Viewer Mode Selection (`ViewerMode.java`)

```java
public enum ViewerMode {
    COLLISION("Collision"),
    TILE_TYPE("Tile Type");
}
```

Users can switch between collision and tile type visualization via a combo box in the UI.

### Color Palette (`ViewPort.java:30-45`)

Each tile type has a distinct RGB color for visualization:

| Tile Type | RGB Value | Color Description |
|-----------|-----------|-------------------|
| `WATER` | `(0, 100, 200)` | Blue |
| `CRANDOR_SMEGMA_WATER` | `(100, 150, 100)` | Greenish |
| `TEMPOR_STORM_WATER` | `(80, 80, 150)` | Dark blue-purple |
| `DISEASE_WATER` | `(100, 180, 80)` | Sickly green |
| `KELP_WATER` | `(0, 150, 100)` | Teal |
| `SUNBAKED_WATER` | `(200, 180, 100)` | Sandy/tan |
| `JAGGED_REEFS_WATER` | `(100, 80, 80)` | Brown |
| `SHARP_CRYSTAL_WATER` | `(180, 100, 200)` | Purple |
| `ICE_WATER` | `(150, 200, 220)` | Light cyan |
| `NE_PURPLE_GRAY_WATER` | `(140, 120, 160)` | Purple-gray |
| `NW_GRAY_WATER` | `(120, 120, 130)` | Gray-blue |
| `SE_PURPLE_WATER` | `(160, 100, 180)` | Purple |
| *(unknown/default)* | `(150, 150, 150)` | Gray |

Defined as an `ImmutableMap<Byte, Color> TILE_TYPE_COLORS`.

### Rendering Pipeline

**Entry Point:** `ViewPort.render()` (lines 65-106)

```java
public void render(WorldPoint base, int width, int height,
                   int cellDim, ViewerMode viewerMode)
```

**Parameters:**
- `base` - Top-left world coordinate of viewport
- `width/height` - Canvas dimensions in pixels
- `cellDim` - Zoom level (5-2000 cells displayed)
- `viewerMode` - `COLLISION` or `TILE_TYPE`

**Tile Type Render Method:** `renderTileTypeMode()` (lines 127-153)

```java
private void renderTileTypeMode(Graphics2D g2d, int width, int height) {
    int cellWidth = width / cellDim;
    int cellHeight = height / cellDim;

    for (int cx = 0; cx < cellDim; cx++) {
        for (int cy = 0; cy < cellDim; cy++) {
            int x = base.getX() + cx;
            int y = base.getY() + (cellDim - 1 - cy);  // Y inverted

            byte tileType = Main.getTileTypeMap().getTileType(x, y, displayPlane);

            if (tileType > 0) {
                g2d.setColor(getTileTypeColor(tileType));
                g2d.fillRect(cx * cellWidth, cy * cellHeight, cellWidth, cellHeight);
            }
        }
    }
}
```

**Color Lookup:**

```java
private Color getTileTypeColor(byte tileType) {
    return TILE_TYPE_COLORS.getOrDefault(tileType, DEFAULT_COLOR);
}
```

### Canvas & Display

- **Canvas:** `BufferedImage` with `TYPE_INT_RGB`
- **Display:** Rendered to `JLabel` via `ImageIcon`
- **Grid:** Drawn when cell width >= 10 pixels (configurable color)

### UI Integration (`UIFrame.java`)

**Mode Selection (lines 247-259):**
- Combo box populated from `ViewerMode.values()`
- Selection triggers `update()` to re-render

**Render Trigger (lines 363-382):**
```java
viewPort.render(base, mapView.getWidth(), mapView.getHeight(),
                zoomSlider.getValue(), currentViewerMode);
mapView.setIcon(new ImageIcon(viewPort.getCanvas()));
```

### Configurable UI Colors (`ConfigManager.java`)

| Setting | Default | Purpose |
|---------|---------|---------|
| `bg_color` | `#F8F8F8` | Background (light gray) |
| `grid_color` | `#00FFFF` | Grid lines (cyan) |
| `collision_color` | `#FF0000` | Collision blocks (red) |
| `wall_color` | `#000000` | Walls (black) |

These are used in collision mode; tile type mode uses the hardcoded `TILE_TYPE_COLORS` palette.

### Rendering File Locations

| Component | Path |
|-----------|------|
| ViewPort (rendering) | `src/main/java/osrs/dev/ui/viewport/ViewPort.java` |
| ViewerMode enum | `src/main/java/osrs/dev/ui/ViewerMode.java` |
| Main UI Frame | `src/main/java/osrs/dev/ui/UIFrame.java` |
| Settings UI | `src/main/java/osrs/dev/ui/SettingsFrame.java` |
| Config Manager | `src/main/java/osrs/dev/util/ConfigManager.java` |
