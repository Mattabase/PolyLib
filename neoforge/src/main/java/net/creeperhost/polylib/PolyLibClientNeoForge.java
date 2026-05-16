package net.creeperhost.polylib;

import net.creeperhost.polylib.client.modulargui.nodegraph.render.BezierWirePiPRenderer;
import net.creeperhost.polylib.client.modulargui.nodegraph.render.BezierWireRenderState;
import net.creeperhost.polylib.client.modulargui.nodegraph.render.BezierWireUniform;
import net.creeperhost.polylib.client.modulargui.nodegraph.render.NodeCanvasRenderPipelines;
import net.creeperhost.polylib.client.screen.chunkmap.PolyChunkMapKeys;
import net.creeperhost.polylib.debug.neoforge.NeoForgeDebugBridge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

public class PolyLibClientNeoForge
{
    public static void init(IEventBus eventBus)
    {
        // F3 debug screen entry system — NeoForge bridge
        eventBus.addListener(NeoForgeDebugBridge::onRegisterDebugEntries);
        // Chunk map keybind
        eventBus.addListener((RegisterKeyMappingsEvent e) ->
                e.register(PolyChunkMapKeys.OPEN_CHUNK_MAP));
        // Node canvas GPU bezier wires
        eventBus.addListener(PolyLibClientNeoForge::registerNodeCanvasPipelines);
        eventBus.addListener(PolyLibClientNeoForge::registerNodeCanvasPiPs);
        NeoForge.EVENT_BUS.addListener(PolyLibClientNeoForge::onRenderFrameEnd);
    }

    private static void registerNodeCanvasPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(NodeCanvasRenderPipelines.BEZIER_WIRE);
    }

    private static void registerNodeCanvasPiPs(RegisterPictureInPictureRenderersEvent event) {
        event.register(BezierWireRenderState.class, BezierWirePiPRenderer::new);
    }

    private static void onRenderFrameEnd(RenderFrameEvent.Post event) {
        BezierWireUniform.STORAGE.get().endFrame();
    }



}
