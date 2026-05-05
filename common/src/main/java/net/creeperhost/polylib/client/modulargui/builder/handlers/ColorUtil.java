package net.creeperhost.polylib.client.modulargui.builder.handlers;

/**
 * Shared colour-parsing utility for {@link net.creeperhost.polylib.client.modulargui.builder.LayoutHandler}
 * implementations.
 *
 * <p>Accepts the following string formats:
 * <ul>
 *   <li>{@code 0xAARRGGBB} or {@code 0XAARRGGBB} — hex with {@code 0x} prefix</li>
 *   <li>{@code #AARRGGBB} — hex with {@code #} prefix</li>
 *   <li>A plain decimal integer string</li>
 * </ul>
 */
public final class ColorUtil {

    private ColorUtil() {}

    /**
     * Parse a colour from a JSON property string.
     *
     * @param s the string value (may be null)
     * @return the parsed ARGB integer, or {@code 0} if {@code s} is null or empty
     * @throws NumberFormatException if the string is non-null but not parseable
     */
    public static int parse(String s) {
        if (s == null || s.isBlank()) return 0;
        String t = s.trim();
        if (t.startsWith("0x") || t.startsWith("0X")) {
            return (int) Long.parseLong(t.substring(2), 16);
        }
        if (t.startsWith("#")) {
            return (int) Long.parseLong(t.substring(1), 16);
        }
        return Integer.parseInt(t);
    }
}
