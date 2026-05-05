package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiTextField;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.minecraft.network.chat.Component;

/**
 * Handler for {@code "textfield"} elements ({@link GuiTextField}).
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code value}            – initial text value</li>
 *   <li>{@code max_length}       – int (default 32)</li>
 *   <li>{@code text_colour}      – ARGB colour string</li>
 *   <li>{@code shadow}           – boolean</li>
 *   <li>{@code editable}         – boolean (default true)</li>
 *   <li>{@code suggestion}       – placeholder suggestion text</li>
 *   <li>{@code on_edit_complete} – hook name fired when editing is finished</li>
 *   <li>{@code on_enter}         – hook name fired on the Enter key</li>
 * </ul>
 *
 * <p>Note: constraints are applied to the returned {@link GuiTextField} element directly.
 * Add a sibling {@code rectangle} element behind it if a visible background is needed.
 */
public class TextFieldHandler implements LayoutHandler<GuiTextField> {

    @Override
    public GuiTextField build(GuiParent<?> parent, JsonObject props,
                              HookResolver hooks, DataBindingResolver bindings) {
        GuiTextField field = new GuiTextField(parent);

        if (props.has("value"))       field.setValue(props.get("value").getAsString());
        if (props.has("max_length"))  field.setMaxLength(props.get("max_length").getAsInt());
        if (props.has("text_colour")) field.setTextColor(ColorUtil.parse(props.get("text_colour").getAsString()));
        if (props.has("shadow"))      field.setShadow(props.get("shadow").getAsBoolean());
        if (props.has("editable"))    field.setEditable(props.get("editable").getAsBoolean());

        if (props.has("suggestion")) {
            field.setSuggestion(Component.literal(props.get("suggestion").getAsString()));
        }

        if (props.has("on_edit_complete")) {
            Runnable cb = hooks.resolve(props.get("on_edit_complete").getAsString());
            if (cb != null) field.setOnEditComplete(cb);
        }
        if (props.has("on_enter")) {
            Runnable cb = hooks.resolve(props.get("on_enter").getAsString());
            if (cb != null) field.setEnterPressed(cb);
        }

        return field;
    }
}
