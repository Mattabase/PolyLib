package net.creeperhost.polylib;

import net.creeperhost.polylib.client.config.ConfigPanelRegistry;
import net.creeperhost.polylib.client.modulargui.sprite.PolyTextures;
import net.creeperhost.polylib.debug.neoforge.NeoForgeDebugBridge;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

public class PolyLibClientNeoForge
{
    public static void init(IEventBus eventBus)
    {
        eventBus.addListener(PolyLibClientNeoForge::atlasStitched);
        eventBus.addListener(PolyLibClientNeoForge::registerTextureAtlas);
        eventBus.addListener(PolyLibClientNeoForge::registerKeyMappings);
        // F3 debug screen entry system — NeoForge bridge
        eventBus.addListener(NeoForgeDebugBridge::onRegisterDebugEntries);
    }

    private static void registerTextureAtlas(RegisterTextureAtlasesEvent event) {
        AtlasManager.AtlasConfig config = new AtlasManager.AtlasConfig(PolyTextures.TEXTURE_ID, PolyTextures.DEFINITION_LOCATION, false);
        event.register(config);
    }

    private static void atlasStitched(TextureAtlasStitchedEvent event) {
        if (event.getAtlas().location().equals(PolyTextures.TEXTURE_ID)) {
            PolyTextures.setAtlas(event.getAtlas());
        }
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping km : ConfigPanelRegistry.getAllKeyMappings()) {
            event.register(km);
        }
    }
}
