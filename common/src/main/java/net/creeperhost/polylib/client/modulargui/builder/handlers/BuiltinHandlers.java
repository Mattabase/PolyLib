package net.creeperhost.polylib.client.modulargui.builder.handlers;

import net.creeperhost.polylib.client.modulargui.builder.GuiLayoutRegistry;

/**
 * Registers all built-in {@link net.creeperhost.polylib.client.modulargui.builder.LayoutHandler}
 * implementations with the {@link GuiLayoutRegistry}.
 *
 * <p>Called automatically the first time the registry is accessed.
 */
public final class BuiltinHandlers {

    private BuiltinHandlers() {}

    public static void register() {
        GuiLayoutRegistry.register("rectangle",  new RectangleHandler());
        GuiLayoutRegistry.register("text",       new TextHandler());
        GuiLayoutRegistry.register("textfield",  new TextFieldHandler());
        GuiLayoutRegistry.register("button",     new ButtonHandler());
        GuiLayoutRegistry.register("texture",    new TextureHandler());
        GuiLayoutRegistry.register("list",       new ListHandler());
        GuiLayoutRegistry.register("itemstack",  new ItemStackHandler());
        GuiLayoutRegistry.register("energybar",  new EnergyBarHandler());
        GuiLayoutRegistry.register("slots",      new SlotsHandler());
    }
}
