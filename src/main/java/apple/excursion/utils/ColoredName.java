package apple.excursion.utils;

import javax.annotation.Nullable;

public class ColoredName {
    private static final int DEFAULT_COLOR = 0x93a7a7;
    @Nullable private final String name;
    private final int color;
    private final boolean colorExists;

    public ColoredName(String name, int color) {
        this.name = name;
        this.color = color;
        this.colorExists = true;
    }

    public ColoredName(String name) {
        this.name = name;
        this.color = 0;
        this.colorExists = false;
    }

    public ColoredName() {
        this.name = null;
        this.color = 0;
        this.colorExists = false;
    }

    public int getColor() {
        return colorExists ? color : DEFAULT_COLOR;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public static String getGuestName(String name) {
        return "Guest " + Pretty.upperCaseFirst(name);
    }
    public static int getGuestColor() {
        return DEFAULT_COLOR;
    }
}
