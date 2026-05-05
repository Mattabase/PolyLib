package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiRectangle;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Phase 1 placeholder handler for {@code "slots"} elements.
 *
 * <p>Full slot rendering via {@link net.creeperhost.polylib.client.modulargui.elements.GuiSlots}
 * requires a live {@code SlotGroup} and {@code ContainerScreenAccess}, which are runtime-only
 * objects from the container menu.  Phase 1 renders a magenta rectangle sized to
 * {@code columns × 18} by {@code rows × 18} as a visual stand-in.
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code columns}    – int, number of slot columns (default 9)</li>
 *   <li>{@code rows}       – int, number of slot rows (default 3)</li>
 *   <li>{@code slot_group} – slot group name (stored for Phase 3 implementation)</li>
 * </ul>
 */
public class SlotsHandler implements LayoutHandler<GuiRectangle> {

    private static final Logger LOGGER = LogManager.getLogger(SlotsHandler.class);
    /** Placeholder colour: semi-transparent magenta. */
    private static final int PLACEHOLDER_COLOUR = 0x80FF00FF;

    @Override
    public GuiRectangle build(GuiParent<?> parent, JsonObject props,
                              HookResolver hooks, DataBindingResolver bindings) {
        int columns = props.has("columns") ? props.get("columns").getAsInt() : 9;
        int rows    = props.has("rows")    ? props.get("rows").getAsInt()    : 3;
        double w    = columns * 18.0;
        double h    = rows    * 18.0;

        if (props.has("slot_group")) {
            LOGGER.debug("SlotsHandler: slot_group '{}' noted — full implementation pending Phase 3",
                    props.get("slot_group").getAsString());
        }

        GuiRectangle placeholder = new GuiRectangle(parent);
        placeholder.fill(PLACEHOLDER_COLOUR);
        Constraints.size(placeholder, w, h);
        return placeholder;
    }
}
