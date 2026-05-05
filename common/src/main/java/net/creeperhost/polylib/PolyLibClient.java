package net.creeperhost.polylib;

import com.mojang.blaze3d.platform.InputConstants;
import net.creeperhost.polylib.client.config.ConfigPanelRegistry;
import net.creeperhost.polylib.client.config.KeyboardShortcut;
import net.creeperhost.polylib.client.modulargui.builder.editor.BuilderScreen;
import net.creeperhost.polylib.client.modulargui.builder.editor.BuilderState;
import net.creeperhost.polylib.client.modulargui.builder.GuiLayout;
import net.creeperhost.polylib.init.InternalEventListenerClient;
import net.creeperhost.polylib.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public class PolyLibClient
{
    public static void init()
    {
        InternalEventListenerClient.init();
        registerBuilderScreen();
    }

    private static void registerBuilderScreen()
    {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "builder"));
        ConfigPanelRegistry.registerModularGui(
                Constants.MOD_ID + "_builder",
                Component.translatable("polylib.builder.title"),
                _parent -> new BuilderScreen(new BuilderState(new GuiLayout(), null)),
                KeyboardShortcut.suggested(GLFW.GLFW_KEY_F7, InputConstants.Type.KEYSYM, category));
    }

    public static Player getClientPlayer()
    {
        if (Services.PLATFORM.isClient())
        {
            return _getClientPlayer();
        }
        return null;
    }

    private static Player _getClientPlayer()
    {
        return Minecraft.getInstance().player;
    }
}
