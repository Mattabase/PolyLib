package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiItemStack;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * Handler for {@code "itemstack"} elements ({@link GuiItemStack}).
 *
 * <p>The displayed stack is empty by default. Provide a {@link Supplier}{@code <ItemStack>}
 * via the data-binding system to make it dynamic.
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code stack_binding}  – binding name that resolves to {@code Supplier<ItemStack>}</li>
 *   <li>{@code decorations}    – boolean; show stack count / durability decorations (default true)</li>
 *   <li>{@code tooltip}        – boolean; show item tooltip on hover (default true)</li>
 * </ul>
 */
public class ItemStackHandler implements LayoutHandler<GuiItemStack> {

    @Override
    public GuiItemStack build(GuiParent<?> parent, JsonObject props,
                              HookResolver hooks, DataBindingResolver bindings) {
        GuiItemStack itemStack;

        if (props.has("stack_binding")) {
            @SuppressWarnings("unchecked")
            Supplier<ItemStack> supplier = (Supplier<ItemStack>) bindings.resolve(
                    props.get("stack_binding").getAsString());
            itemStack = supplier != null
                    ? new GuiItemStack(parent, supplier)
                    : new GuiItemStack(parent);
        } else {
            itemStack = new GuiItemStack(parent);
        }

        if (props.has("decorations")) itemStack.enableStackDecoration(props.get("decorations").getAsBoolean());
        if (props.has("tooltip"))     itemStack.enableStackToolTip(props.get("tooltip").getAsBoolean());

        return itemStack;
    }
}
