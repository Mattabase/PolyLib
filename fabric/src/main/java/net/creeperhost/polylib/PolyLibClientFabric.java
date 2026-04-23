package net.creeperhost.polylib;

import net.creeperhost.polylib.accessibility.AccessibilityOptionsRegistry;
import net.creeperhost.polylib.client.config.ConfigPanelRegistry;
import net.creeperhost.polylib.client.modulargui.ModularGuiInjector;
import net.creeperhost.polylib.player.serverdata.PlayerServerDataClientCache;
import net.creeperhost.polylib.player.settings.PlayerClientSettingsClientCache;
import net.creeperhost.polylib.network.PolyLibNetwork;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;

public class PolyLibClientFabric
{
    public static void init()
    {
        PolyLibNetwork.initClient();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PlayerClientSettingsClientCache.clear();
            PlayerServerDataClientCache.clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            ModularGuiInjector.tick(mc);
            ConfigPanelRegistry.tickKeybinds();
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ModularGuiInjector.initPost(screen);
            AccessibilityOptionsRegistry.inject(screen);
            ConfigPanelRegistry.injectConfigButton(screen, w -> Screens.getWidgets(screen).add(w));
            ScreenEvents.afterExtract(screen).register(ModularGuiInjector::renderPost);
            ScreenKeyboardEvents.afterKeyPress(screen).register((screen1, event)
                    -> ModularGuiInjector.keyPressed(Minecraft.getInstance(), screen1, event));
            ScreenKeyboardEvents.afterKeyRelease(screen).register((screen1, event)
                    -> ModularGuiInjector.keyReleased(Minecraft.getInstance(), screen1, event));
            ScreenMouseEvents.afterMouseClick(screen).register((screen1, event, consumed)
                    -> ModularGuiInjector.mouseClicked(Minecraft.getInstance(), screen1, event, consumed));
            ScreenMouseEvents.afterMouseRelease(screen).register((screen1, event, consumed)
                    -> ModularGuiInjector.mouseReleased(Minecraft.getInstance(), screen1, event));
            ScreenMouseEvents.afterMouseScroll(screen).register((screen1, mouseX, mouseY, horizontalAmount, verticalAmount, consumed)
                    -> ModularGuiInjector.mouseScrolled(Minecraft.getInstance(), screen1, mouseX, mouseY, horizontalAmount, verticalAmount));
        });
    }
}
