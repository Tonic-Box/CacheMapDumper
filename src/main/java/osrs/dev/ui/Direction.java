package osrs.dev.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Direction
{
    NORTH("^"),
    SOUTH("v"),
    EAST(">"),
    WEST("<");

    private final String symbol;
}
