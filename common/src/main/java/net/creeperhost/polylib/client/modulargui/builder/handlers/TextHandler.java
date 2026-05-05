package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiText;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Align;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * Handler for {@code "text"} elements ({@link GuiText}).
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code text}         – literal text string</li>
 *   <li>{@code translatable} – MC translation key</li>
 *   <li>{@code binding}      – dynamic binding name, resolves to {@code Supplier<Component>}</li>
 *   <li>{@code colour}       – ARGB colour string</li>
 *   <li>{@code shadow}       – boolean (default true)</li>
 *   <li>{@code align}        – "left"/"min", "center"/"centre", "right"/"max"</li>
 *   <li>{@code wrap}         – boolean</li>
 *   <li>{@code trim}         – boolean</li>
 *   <li>{@code scroll}       – boolean</li>
 *   <li>{@code auto_height}  – boolean; adds a dynamic height constraint when wrapping (default true)</li>
 * </ul>
 */
public class TextHandler implements LayoutHandler<GuiText> {

    @Override
    public GuiText build(GuiParent<?> parent, JsonObject props,
                         HookResolver hooks, DataBindingResolver bindings) {
        GuiText text = new GuiText(parent);

        // Content — precedence: binding > translatable > text literal
        if (props.has("binding")) {
            @SuppressWarnings("unchecked")
            Supplier<Component> supplier = (Supplier<Component>) bindings.resolve(
                    props.get("binding").getAsString());
            if (supplier != null) {
                text.setTextSupplier(supplier);
            }
        } else if (props.has("translatable")) {
            text.setTranslatable(props.get("translatable").getAsString());
        } else if (props.has("text")) {
            text.setText(props.get("text").getAsString());
        }

        if (props.has("colour"))  text.setTextColour(ColorUtil.parse(props.get("colour").getAsString()));
        if (props.has("shadow"))  text.setShadow(props.get("shadow").getAsBoolean());
        if (props.has("align"))   text.setAlignment(parseAlign(props.get("align").getAsString()));
        if (props.has("trim"))    text.setTrim(props.get("trim").getAsBoolean());
        if (props.has("scroll"))  text.setScroll(props.get("scroll").getAsBoolean());

        if (props.has("wrap")) {
            boolean wrap = props.get("wrap").getAsBoolean();
            text.setWrap(wrap);
            boolean autoHeight = !props.has("auto_height") || props.get("auto_height").getAsBoolean();
            if (wrap && autoHeight) text.autoHeight();
        }

        return text;
    }

    private static Align parseAlign(String s) {
        return switch (s.toLowerCase()) {
            case "min", "left", "top"           -> Align.MIN;
            case "center", "centre", "middle"   -> Align.CENTER;
            case "max", "right", "bottom"       -> Align.MAX;
            default                              -> Align.MIN;
        };
    }
}
