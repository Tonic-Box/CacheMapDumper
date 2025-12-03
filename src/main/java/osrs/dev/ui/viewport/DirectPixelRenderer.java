package osrs.dev.ui.viewport;

import osrs.dev.Main;
import osrs.dev.reader.Flags;
import osrs.dev.ui.ViewerMode;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * High-performance tile renderer using direct pixel manipulation.
 * Bypasses Graphics2D for significantly faster rendering.
 */
public final class DirectPixelRenderer {
    private DirectPixelRenderer() {} // Prevent instantiation

    /**
     * Fills a rectangular region with a solid color.
     * @param pixels The pixel array (from BufferedImage)
     * @param imgWidth Width of the image
     * @param imgHeight Height of the image
     * @param x Starting X coordinate
     * @param y Starting Y coordinate
     * @param width Rectangle width
     * @param height Rectangle height
     * @param color RGB color value
     */
    public static void fillRect(int[] pixels, int imgWidth, int imgHeight,
                                 int x, int y, int width, int height, int color) {
        // Clamp to image bounds
        int x1 = Math.max(0, x);
        int y1 = Math.max(0, y);
        int x2 = Math.min(imgWidth, x + width);
        int y2 = Math.min(imgHeight, y + height);

        if (x1 >= x2 || y1 >= y2) return;

        int fillWidth = x2 - x1;
        for (int py = y1; py < y2; py++) {
            int rowStart = py * imgWidth + x1;
            Arrays.fill(pixels, rowStart, rowStart + fillWidth, color);
        }
    }

    /**
     * Draws a horizontal line.
     */
    public static void drawHLine(int[] pixels, int imgWidth, int imgHeight,
                                  int x, int y, int length, int thickness, int color) {
        fillRect(pixels, imgWidth, imgHeight, x, y, length, thickness, color);
    }

    /**
     * Draws a vertical line.
     */
    public static void drawVLine(int[] pixels, int imgWidth, int imgHeight,
                                  int x, int y, int length, int thickness, int color) {
        fillRect(pixels, imgWidth, imgHeight, x, y, thickness, length, color);
    }

    /**
     * Renders collision walls for a tile.
     * @param pixels The pixel array
     * @param imgWidth Width of the image
     * @param imgHeight Height of the image
     * @param screenX Screen X position of tile
     * @param screenY Screen Y position of tile (top-left, Y increases downward)
     * @param tileSize Size of tile in pixels
     * @param flags Collision flags from CollisionMap.all()
     * @param wallColor Color for walls
     */
    public static void renderWalls(int[] pixels, int imgWidth, int imgHeight,
                                    int screenX, int screenY, int tileSize,
                                    byte flags, int wallColor) {
        int thickness = Math.max(1, tileSize / 8);

        // North wall (top edge)
        if ((flags & Flags.NORTH) == 0) {
            drawHLine(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, thickness, wallColor);
        }
        // South wall (bottom edge)
        if ((flags & Flags.SOUTH) == 0) {
            drawHLine(pixels, imgWidth, imgHeight, screenX, screenY + tileSize - thickness, tileSize, thickness, wallColor);
        }
        // West wall (left edge)
        if ((flags & Flags.WEST) == 0) {
            drawVLine(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, thickness, wallColor);
        }
        // East wall (right edge)
        if ((flags & Flags.EAST) == 0) {
            drawVLine(pixels, imgWidth, imgHeight, screenX + tileSize - thickness, screenY, tileSize, thickness, wallColor);
        }
    }

    /**
     * Renders a single tile to the pixel array.
     * @param pixels The pixel array
     * @param imgWidth Width of the image
     * @param imgHeight Height of the image
     * @param screenX Screen X position
     * @param screenY Screen Y position
     * @param tileSize Size of tile in pixels
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param plane Plane/level
     * @param mode Viewer mode
     * @param bgColor Background color
     * @param collisionColor Collision fill color
     * @param wallColor Wall line color
     * @param drawWalls Whether to draw wall details (true when zoomed in)
     */
    public static void renderTile(int[] pixels, int imgWidth, int imgHeight,
                                   int screenX, int screenY, int tileSize,
                                   int worldX, int worldY, int plane,
                                   ViewerMode mode,
                                   int bgColor, int collisionColor, int wallColor,
                                   boolean drawWalls) {
        if (mode == ViewerMode.COLLISION) {
            renderCollisionTile(pixels, imgWidth, imgHeight, screenX, screenY, tileSize,
                    worldX, worldY, plane, bgColor, collisionColor, wallColor, drawWalls);
        } else if (mode == ViewerMode.TILE_TYPE) {
            renderTileTypeTile(pixels, imgWidth, imgHeight, screenX, screenY, tileSize,
                    worldX, worldY, plane, bgColor);
        } else if (mode == ViewerMode.COMBINED) {
            renderCombinedTile(pixels, imgWidth, imgHeight, screenX, screenY, tileSize,
                    worldX, worldY, plane, bgColor, collisionColor, wallColor, drawWalls);
        }
    }

    /**
     * Renders a single tile with LOD-based detail control.
     * LOD 0: Full detail - red fills for blocked tiles, black wall lines
     * LOD 1: Medium detail - black fills for blocked tiles, black wall lines
     * LOD 2: Minimal detail - black fills for blocked tiles, no wall lines
     */
    public static void renderTileWithLOD(int[] pixels, int imgWidth, int imgHeight,
                                          int screenX, int screenY, int tileSize,
                                          int worldX, int worldY, int plane,
                                          ViewerMode mode,
                                          int bgColor, int collisionColor, int wallColor,
                                          int lod) {
        // LOD 0 = zoomed in (full detail), higher LOD = zoomed out
        boolean drawWalls = (lod <= 1);  // Draw wall lines at LOD 0 and 1
        boolean drawRedFill = (lod == 0); // Only draw red fill at LOD 0 (highest detail)

        if (mode == ViewerMode.COLLISION) {
            renderCollisionTileWithLOD(pixels, imgWidth, imgHeight, screenX, screenY, tileSize,
                    worldX, worldY, plane, bgColor, collisionColor, wallColor, drawWalls, drawRedFill);
        } else if (mode == ViewerMode.TILE_TYPE) {
            renderTileTypeTile(pixels, imgWidth, imgHeight, screenX, screenY, tileSize,
                    worldX, worldY, plane, bgColor);
        } else if (mode == ViewerMode.COMBINED) {
            renderCombinedTileWithLOD(pixels, imgWidth, imgHeight, screenX, screenY, tileSize,
                    worldX, worldY, plane, bgColor, collisionColor, wallColor, drawWalls, drawRedFill);
        }
    }

    private static void renderCollisionTileWithLOD(int[] pixels, int imgWidth, int imgHeight,
                                                    int screenX, int screenY, int tileSize,
                                                    int worldX, int worldY, int plane,
                                                    int bgColor, int collisionColor, int wallColor,
                                                    boolean drawWalls, boolean drawRedFill) {
        if (Main.getCollision() == null) return;

        byte flags = Main.getCollision().all((short) worldX, (short) worldY, (byte) plane);

        if (flags == Flags.NONE) {
            // Fully blocked tile
            if (drawRedFill) {
                // Zoomed in: show red fill
                fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, collisionColor);
            } else {
                // Zoomed out: show black/wall color instead of red
                fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, wallColor);
            }
        } else if (flags != Flags.ALL && drawWalls) {
            // Has walls - draw them
            renderWalls(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, flags, wallColor);
        }
    }

    private static void renderCombinedTileWithLOD(int[] pixels, int imgWidth, int imgHeight,
                                                   int screenX, int screenY, int tileSize,
                                                   int worldX, int worldY, int plane,
                                                   int bgColor, int collisionColor, int wallColor,
                                                   boolean drawWalls, boolean drawRedFill) {
        boolean hasTileType = false;

        // First: render tile type as background
        if (Main.getTileTypeMap() != null) {
            byte tileType = Main.getTileTypeMap().getTileType(worldX, worldY, plane);
            if (tileType > 0) {
                hasTileType = true;
                int color = getTileTypeColor(tileType);
                fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, color);
            }
        }

        // Second: render collision on top
        if (Main.getCollision() != null) {
            byte flags = Main.getCollision().all((short) worldX, (short) worldY, (byte) plane);

            if (flags == Flags.NONE) {
                // Fully blocked tile
                if (!hasTileType) {
                    if (drawRedFill) {
                        // Zoomed in: show red fill
                        fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, collisionColor);
                    } else {
                        // Zoomed out: show black/wall color instead of red
                        fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, wallColor);
                    }
                }
            } else if (flags != Flags.ALL && drawWalls) {
                // Has walls - draw them
                renderWalls(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, flags, wallColor);
            }
        }
    }

    private static void renderCollisionTile(int[] pixels, int imgWidth, int imgHeight,
                                             int screenX, int screenY, int tileSize,
                                             int worldX, int worldY, int plane,
                                             int bgColor, int collisionColor, int wallColor,
                                             boolean drawWalls) {
        if (Main.getCollision() == null) return;

        byte flags = Main.getCollision().all((short) worldX, (short) worldY, (byte) plane);

        if (flags == Flags.NONE) {
            // Fully blocked - fill with collision color
            fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, collisionColor);
        } else if (flags != Flags.ALL && drawWalls) {
            // Has walls - draw them
            renderWalls(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, flags, wallColor);
        }
        // If flags == Flags.ALL (fully passable), background is already cleared
    }

    private static void renderTileTypeTile(int[] pixels, int imgWidth, int imgHeight,
                                            int screenX, int screenY, int tileSize,
                                            int worldX, int worldY, int plane,
                                            int bgColor) {
        if (Main.getTileTypeMap() == null) return;

        byte tileType = Main.getTileTypeMap().getTileType(worldX, worldY, plane);
        if (tileType > 0) {
            int color = getTileTypeColor(tileType);
            fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, color);
        }
    }

    private static void renderCombinedTile(int[] pixels, int imgWidth, int imgHeight,
                                            int screenX, int screenY, int tileSize,
                                            int worldX, int worldY, int plane,
                                            int bgColor, int collisionColor, int wallColor,
                                            boolean drawWalls) {
        boolean hasTileType = false;

        // First: render tile type as background
        if (Main.getTileTypeMap() != null) {
            byte tileType = Main.getTileTypeMap().getTileType(worldX, worldY, plane);
            if (tileType > 0) {
                hasTileType = true;
                int color = getTileTypeColor(tileType);
                fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, color);
            }
        }

        // Second: render collision on top
        if (Main.getCollision() != null) {
            byte flags = Main.getCollision().all((short) worldX, (short) worldY, (byte) plane);

            if (flags == Flags.NONE && !hasTileType) {
                // Fully blocked and no tile type - fill with collision color
                fillRect(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, tileSize, collisionColor);
            } else if (flags != Flags.ALL && drawWalls) {
                // Has walls - draw them
                renderWalls(pixels, imgWidth, imgHeight, screenX, screenY, tileSize, flags, wallColor);
            }
        }
    }

    /**
     * Gets the RGB color for a tile type.
     * Maps tile type bytes to colors from ViewPort.TILE_TYPE_COLORS.
     */
    private static int getTileTypeColor(byte tileType) {
        // These match the colors in ViewPort.TILE_TYPE_COLORS
        switch (tileType) {
            case 1: return 0x0064C8;   // WATER - Blue
            case 2: return 0x649664;   // CRANDOR_SMEGMA_WATER - Greenish
            case 3: return 0x505096;   // TEMPOR_STORM_WATER - Dark blue
            case 4: return 0x64B450;   // DISEASE_WATER - Sickly green
            case 5: return 0x009664;   // KELP_WATER - Teal
            case 6: return 0xC8B464;   // SUNBAKED_WATER - Sandy
            case 7: return 0x645050;   // JAGGED_REEFS_WATER - Brown
            case 8: return 0xB464C8;   // SHARP_CRYSTAL_WATER - Purple
            case 9: return 0x96C8DC;   // ICE_WATER - Light blue/cyan
            case 10: return 0x8C78A0;  // NE_PURPLE_GRAY_WATER - Purple-gray
            case 11: return 0x787882;  // NW_GRAY_WATER - Gray-blue
            case 12: return 0xA064B4;  // SE_PURPLE_WATER - Purple
            default: return 0x969696;  // Unknown - Gray
        }
    }

    /**
     * Gets the pixel array from a BufferedImage.
     * The image must be TYPE_INT_RGB or TYPE_INT_ARGB.
     */
    public static int[] getPixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    /**
     * Converts a java.awt.Color to RGB int.
     */
    public static int colorToRGB(java.awt.Color color) {
        return color.getRGB() & 0xFFFFFF;
    }
}
