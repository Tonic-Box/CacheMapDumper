package osrs.dev.ui;

/**
 * Enum representing the different viewer modes available.
 */
public enum ViewerMode {
    COLLISION("Collision"),
    TILE_TYPE("Tile Type"),
    COMBINED("Combined");

    private final String displayName;

    ViewerMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Gets a ViewerMode from its display name.
     * @param displayName the display name
     * @return the ViewerMode, or COLLISION if not found
     */
    public static ViewerMode fromDisplayName(String displayName) {
        for (ViewerMode mode : values()) {
            if (mode.displayName.equalsIgnoreCase(displayName)) {
                return mode;
            }
        }
        return COLLISION;
    }
}
