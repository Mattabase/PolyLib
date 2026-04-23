package net.creeperhost.testmod;

import net.creeperhost.polylib.accessibility.AccessibilityOptionsRegistry;
import net.creeperhost.polylib.client.config.ConfigPanelRegistry;
import net.creeperhost.polylib.client.modulargui.ModularGuiInjector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class TestModClientCommon
{
    private static boolean reduceMotion = false;
    private static boolean highContrast = false;

    public static void init()
    {
        ModularGuiInjector.registerInjection(e -> e instanceof TitleScreen, e -> new MainMenuGuiInjection());

        AccessibilityOptionsRegistry.register("testmod.demo", builder -> builder
            .category("testmod.accessibility.demo_category", "PolyLib Testmod")
            .toggle(
                "testmod.accessibility.reduce_motion", "Reduce Motion",
                () -> reduceMotion, v -> reduceMotion = v
            )
            .toggle(
                "testmod.accessibility.high_contrast", "High Contrast (Demo)",
                () -> highContrast, v -> highContrast = v
            )
        );

        // Fabric: register config screen here (NeoForge uses NeoForgeConfigHelper in TestModNeoForge)
        if (!Services.PLATFORM.getPlatformName().equalsIgnoreCase("NeoForge")) {
            ConfigPanelRegistry.register(
                "testmod",
                parent -> new ConfirmScreen(
                    confirmed -> Minecraft.getInstance().setScreen(parent),
                    Component.literal("Testmod"),
                    Component.literal("No config screen registered yet."),
                    Component.literal("OK"),
                    Component.empty()
                )
            );
        }
    }
}
