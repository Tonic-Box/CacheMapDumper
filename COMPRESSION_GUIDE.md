# Object Map Compression & Optimization Guide

## Available Formats

### Version 1: Uncompressed (ObjectMapOptimized)
**File**: `objects.dat`
- Sorted coordinates for binary search
- Fixed-size encoding (8 bytes/coord, 4 bytes/int)
- Fast to read, moderate file size
- **Use case**: When load speed matters more than disk space

### Version 2: Compressed (ObjectMapCompressed)
**File**: `objects_compressed.dat`
- Delta encoding for coordinates (huge savings!)
- VarInt encoding for all integers
- **60-80% smaller** than uncompressed
- Slightly slower to parse (still <2s)
- **Use case**: Distributing in JARs, limited disk space

## Compression Techniques Explained

### 1. Delta Encoding (Coordinates)
**Before:**
```
Coord 1: 10000000 (8 bytes)
Coord 2: 10000001 (8 bytes)
Coord 3: 10000005 (8 bytes)
Total: 24 bytes
```

**After:**
```
Coord 1: 10000000 (VarLong ~4 bytes)
Delta 2: +1       (VarLong ~1 byte)
Delta 3: +4       (VarLong ~1 byte)
Total: ~6 bytes
```
**Savings: 75%** on coordinates!

### 2. VarInt Encoding
Variable-length integer encoding:
- 0-127: 1 byte
- 128-16383: 2 bytes
- 16384-2097151: 3 bytes
- etc.

**Examples:**
```
Value    Fixed   VarInt  Savings
-----    -----   ------  -------
1        4 bytes 1 byte  75%
100      4 bytes 1 byte  75%
1000     4 bytes 2 bytes 50%
50000    4 bytes 3 bytes 25%
```

Most OSRS object IDs are <16384, so we get **50-75% savings** on IDs!

## Size Comparison

Estimated for ~1M coordinates with ~3M objects:

| Format | Size | Load Time | Query Time |
|--------|------|-----------|------------|
| HashMap (Old) | ~80 MB | OOM / 5s+ | 2-3s |
| Uncompressed (v1) | ~40 MB | ~1-2s | <1ms |
| Compressed (v2) | ~12-16 MB | ~1.5-2.5s | <1ms |

**Compression ratio: 60-80% reduction!**

## Performance Breakdown

### Delta Encoding Impact
```
Coordinates: ~1M entries
Before: 1M * 8 bytes = 8 MB
After:  1M * ~1.5 bytes avg = 1.5 MB
Savings: ~6.5 MB (81% reduction)
```

### VarInt Impact
```
Object counts: ~1M entries, avg value ~3
Before: 1M * 4 bytes = 4 MB
After:  1M * 1 byte = 1 MB
Savings: 3 MB (75% reduction)

Object IDs: ~3M entries, avg ID ~5000
Before: 3M * 4 bytes = 12 MB
After:  3M * 2 bytes avg = 6 MB
Savings: 6 MB (50% reduction)
```

**Total savings: ~15.5 MB from ~24 MB = 65% reduction**

## Usage

### In Dumper (Automatic)
Both formats are saved automatically:
```java
// Dumper saves both formats
objects.dat            // Version 1 (uncompressed)
objects_compressed.dat // Version 2 (compressed)
```

### Loading Uncompressed (v1)
```java
ObjectMapOptimized map = ObjectMapOptimized.load("objects.dat");
List<Integer> objects = map.getObjects((short)x, (short)y, (byte)z);
```

### Loading Compressed (v2)
```java
ObjectMapCompressed map = ObjectMapCompressed.load("objects_compressed.dat");
List<Integer> objects = map.getObjects((short)x, (short)y, (byte)z);
// Same API!
```

### From Resources
```java
// Uncompressed
InputStream stream = getClass().getResourceAsStream("/objects.dat");
ObjectMapOptimized map = ObjectMapOptimized.load(stream);

// Compressed (recommended for JARs)
InputStream stream = getClass().getResourceAsStream("/objects_compressed.dat");
ObjectMapCompressed map = ObjectMapCompressed.load(stream);
```

## When to Use Which

### Use Uncompressed (v1) when:
- ✅ Disk space is not a concern
- ✅ Absolute fastest load time needed
- ✅ Debugging/development (easier to validate)
- ✅ File stored locally (not distributed)

### Use Compressed (v2) when:
- ✅ Distributing in JAR files
- ✅ Disk space matters
- ✅ Bandwidth costs (downloads)
- ✅ Load time <2.5s is acceptable
- ✅ **Recommended for production**

## File Format Details

### Version 1 (Uncompressed)
```
[4 bytes] Version = 1
[4 bytes] Entry count
For each entry (sorted):
  [8 bytes] Packed coordinate
  [4 bytes] Object count
  [4 bytes * count] Object IDs
```

### Version 2 (Compressed)
```
[4 bytes] Version = 2
[VarInt] Entry count
For each entry (sorted):
  [VarLong] Coordinate delta (from previous)
  [VarInt] Object count
  [VarInt * count] Object IDs
```

## Further Optimizations Available

### 1. LZ4 Compression Wrapper
Add LZ4 compression on top for another 30-50% reduction:
- Requires lz4-java dependency
- Adds decompression overhead (~100-200ms)
- File size: ~8-10 MB
- **Not implemented** (diminishing returns)

### 2. Dictionary Compression
Build dictionary of common object IDs:
- Replace frequent IDs with 1-byte codes
- Saves ~20% on object IDs
- Adds complexity to parsing
- **Not implemented** (complexity vs benefit)

### 3. Regional Chunking
Split world into 64x64 chunks, load on demand:
- Zero upfront load time
- ~1-2ms per chunk loaded
- More complex file format
- **Could implement if needed**

### 4. Bloom Filters
Add bloom filter for quick existence checks:
- Fast "not found" checks
- ~1-2 MB overhead
- Only useful for sparse queries
- **Not needed** (binary search is fast enough)

## Recommendations

**For your pathfinding JAR:**
1. Use **ObjectMapCompressed** (v2)
2. Include `objects_compressed.dat` in resources
3. Load once at startup: `ObjectMapCompressed.load(stream)`
4. Benefits:
   - 60-80% smaller JAR
   - <2 second load time
   - Sub-millisecond queries

**For the dumper:**
- Keep saving both formats
- Allows testing/comparison
- Users can choose based on needs

## Migration Path

1. **Re-dump** with updated code → generates both formats
2. **Test** both formats work correctly
3. **Choose** based on your requirements:
   - Development: uncompressed
   - Production/Distribution: compressed
4. **Update** your other project to use chosen format

## Validation

Both formats include validation:
- Entry count sanity check (max 10M)
- Object count sanity check (max 1000/tile)
- VarInt overflow protection
- Clear error messages on corruption

## Expected Output

When dumping, you'll see:
```
Wrote object map to objects.dat (1234567 coordinates, 3456789 objects)
Compressed object map saved:
  Uncompressed: 38.5 MB
  Compressed: 14.2 MB
  Ratio: 63.1% reduction
  Time: 523ms
Wrote compressed object map to objects_compressed.dat
```

This tells you exactly how much space you're saving!
