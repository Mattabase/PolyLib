package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiButton;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.minecraft.network.chat.Component;

/**
 * Handler for {@code "button"} elements ({@link GuiButton}).
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code preset}         – {@code "vanilla"} (default), {@code "vanilla_animated"},
 *                                {@code "flat_colour"}, or {@code "bare"}</li>
 *   <li>{@code label}          – button label text</li>
 *   <li>{@code colour}         – fill colour for {@code flat_colour} preset (default {@code 0xFF404040})</li>
 *   <li>{@code hover_colour}   – hover fill colour for {@code flat_colour} preset (default {@code 0xFF606060})</li>
 *   <li>{@code border_colour}  – optional border colour for {@code flat_colour} preset</li>
 *   <li>{@code on_click}       – hook name for left-click</li>
 *   <li>{@code on_right_click} – hook name for right-click</li>
 *   <li>{@code on_press}       – hook name for left-press (fires on mouse-up over button)</li>
 * </ul>
 */
public class ButtonHandler implements LayoutHandler<GuiButton> {

    @Override
    public GuiButton build(GuiParent<?> parent, JsonObject props,
                           HookResolver hooks, DataBindingResolver bindings) {
        String preset = props.has("preset") ? props.get("preset").getAsString() : "vanilla";
        Component label = props.has("label")
                ? Component.literal(props.get("label").getAsString())
                : null;

        GuiButton button = switch (preset) {
            case "vanilla_animated" -> GuiButton.vanillaAnimated(parent, label);
            case "flat_colour" -> {
                int colour      = props.has("colour")
                        ? ColorUtil.parse(props.get("colour").getAsString()) : 0xFF404040;
                int hoverColour = props.has("hover_colour")
                        ? ColorUtil.parse(props.get("hover_colour").getAsString()) : 0xFF606060;
                Component finalLabel = label;
                if (props.has("border_colour")) {
                    int borderColour = ColorUtil.parse(props.get("border_colour").getAsString());
                    yield GuiButton.flatColourButton(parent,
                            finalLabel == null ? null : () -> finalLabel,
                            hover -> hover ? hoverColour : colour,
                            hover -> borderColour);
                } else {
                    yield GuiButton.flatColourButton(parent,
                            finalLabel == null ? null : () -> finalLabel,
                            hover -> hover ? hoverColour : colour);
                }
            }
            case "bare" -> new GuiButton(parent);
            default    -> GuiButton.vanilla(parent, label); // "vanilla"
        };

        if (props.has("on_click")) {
            Runnable cb = hooks.resolve(props.get("on_click").getAsString());
            if (cb != null) button.onClick(cb, GuiButton.LEFT_CLICK);
        }
        if (props.has("on_right_click")) {
            Runnable cb = hooks.resolve(props.get("on_right_click").getAsString());
            if (cb != null) button.onClick(cb, GuiButton.RIGHT_CLICK);
        }
        if (props.has("on_press")) {
            Runnable cb = hooks.resolve(props.get("on_press").getAsString());
            if (cb != null) button.onPress(cb);
        }

        return button;
    }
}
