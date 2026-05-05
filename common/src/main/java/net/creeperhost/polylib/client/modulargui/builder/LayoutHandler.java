package net.creeperhost.polylib.client.modulargui.builder;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;

/**
 * Creates and configures a specific type of GUI element from a JSON properties object.
 *
 * <p>The {@link #build} method is invoked once per element declaration in the layout.
 * It should create the element, apply all properties from {@code props}, wire up any hooks
 * or bindings, and return the root element for that declaration.
 *
 * <p><b>Constraints</b> are applied by {@link JsonGuiProvider} <em>after</em> {@code build}
 * returns — do not apply constraints inside the handler.
 *
 * @param <E> the root element type returned by this handler
 */
public interface LayoutHandler<E extends GuiElement<?>> {

    /**
     * Create and configure an element of this type.
     *
     * @param parent   the parent element or GUI root that this element should be a child of
     * @param props    raw JSON properties object (never null, but may be empty)
     * @param hooks    resolver for named hook callbacks declared in the layout
     * @param bindings resolver for dynamic data bindings declared in the layout
     * @return the created and configured root element
     */
    E build(GuiParent<?> parent, JsonObject props, HookResolver hooks, DataBindingResolver bindings);
}
