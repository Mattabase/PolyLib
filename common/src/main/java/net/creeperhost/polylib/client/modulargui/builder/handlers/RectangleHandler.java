package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiRectangle;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;

/**
 * Handler for {@code "rectangle"} elements ({@link GuiRectangle}).
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code fill_colour}        – ARGB colour string (e.g. {@code "0xFF1A2030"})</li>
 *   <li>{@code border_colour}      – ARGB colour string</li>
 *   <li>{@code border_width}       – double, default 1.0</li>
 *   <li>{@code shade_top_left}     – ARGB; enables shaded-rect mode (requires {@code shade_bottom_right})</li>
 *   <li>{@code shade_bottom_right} – ARGB</li>
 *   <li>{@code shade_corners}      – ARGB corner mix override (optional; auto-computed if omitted)</li>
 * </ul>
 */
public class RectangleHandler implements LayoutHandler<GuiRectangle> {

    @Override
    public GuiRectangle build(GuiParent<?> parent, JsonObject props,
                              HookResolver hooks, DataBindingResolver bindings) {
        GuiRectangle rect = new GuiRectangle(parent);

        if (props.has("shade_top_left") && props.has("shade_bottom_right")) {
            int tl   = ColorUtil.parse(props.get("shade_top_left").getAsString());
            int br   = ColorUtil.parse(props.get("shade_bottom_right").getAsString());
            int fill = props.has("fill_colour") ? ColorUtil.parse(props.get("fill_colour").getAsString()) : 0;
            if (props.has("shade_corners")) {
                rect.shadedRect(tl, br, ColorUtil.parse(props.get("shade_corners").getAsString()), fill);
            } else {
                rect.shadedRect(tl, br, fill);
            }
        } else {
            if (props.has("fill_colour"))   rect.fill(ColorUtil.parse(props.get("fill_colour").getAsString()));
            if (props.has("border_colour")) rect.border(ColorUtil.parse(props.get("border_colour").getAsString()));
        }

        if (props.has("border_width")) rect.borderWidth(props.get("border_width").getAsDouble());

        return rect;
    }
}
