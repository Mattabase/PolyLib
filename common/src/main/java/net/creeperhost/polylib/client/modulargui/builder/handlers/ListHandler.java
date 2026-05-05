package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiList;
import net.creeperhost.polylib.client.modulargui.elements.GuiText;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Align;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.minecraft.network.chat.Component;

/**
 * Handler for {@code "list"} elements ({@link GuiList}).
 *
 * <p>This handler creates a {@code GuiList<String>} whose entries are rendered as centred
 * {@link GuiText} labels. Supply items either as a static JSON array or via a binding.
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code items}        – JSON array of string values to populate the list</li>
 *   <li>{@code item_spacing} – double, pixels between items (default 2)</li>
 *   <li>{@code scroll_bar}   – boolean; adds a hidden scroll bar when {@code true}</li>
 * </ul>
 */
public class ListHandler implements LayoutHandler<GuiList<String>> {

    @Override
    public GuiList<String> build(GuiParent<?> parent, JsonObject props,
                                 HookResolver hooks, DataBindingResolver bindings) {
        GuiList<String> list = new GuiList<>(parent);

        list.setDisplayBuilder((listEl, entry) -> {
            GuiText label = new GuiText(listEl);
            label.setText(Component.literal(entry));
            label.setAlignment(Align.CENTER);
            return label;
        });

        if (props.has("item_spacing")) {
            list.setItemSpacing(props.get("item_spacing").getAsDouble());
        }

        if (props.has("scroll_bar") && props.get("scroll_bar").getAsBoolean()) {
            list.addHiddenScrollBar();
        }

        if (props.has("items")) {
            JsonArray arr = props.getAsJsonArray("items");
            for (JsonElement el : arr) {
                list.getList().add(el.getAsString());
            }
        }

        return list;
    }
}
