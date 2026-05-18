package net.creeperhost.testmod.screens;

import net.creeperhost.polylib.client.modulargui.ModularGui;
import net.creeperhost.polylib.client.modulargui.ModularGuiScreen;
import net.creeperhost.polylib.client.modulargui.elements.*;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.GuiProvider;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint;
import net.creeperhost.polylib.client.modulargui.nodegraph.graph.*;
import net.creeperhost.testmod.nodegraph.*;
import net.minecraft.network.chat.Component;

import java.util.UUID;

import static net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint.*;
import static net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam.*;

/**
 * Demonstrates {@link GuiNodeCanvas} with all four port data types and
 * pre-built wired connections covering:
 *
 * <ul>
 *   <li>SIGNAL wire: SignalSource → SignalSink</li>
 *   <li>ITEMS wire: ItemPassthrough (in) → ItemPassthrough (out) — two nodes wired together</li>
 *   <li>FLUID wire: FluidBuffer in → out (one node's ports shown; manual wiring to another)</li>
 *   <li>ANY wire: AnyRelay accepts a SIGNAL input, demonstrating wildcard compatibility</li>
 * </ul>
 *
 * <p>Open in-game: KP_4 (numpad 4).
 *
 * <p>Expected:
 * <ul>
 *   <li>Five nodes visible; three bezier wires pre-wired</li>
 *   <li>Middle-drag to pan, scroll to zoom</li>
 *   <li>Click a port dot and drag to another compatible port to wire more connections</li>
 *   <li>Incompatible wires (e.g. SIGNAL → ITEMS) are silently rejected</li>
 * </ul>
 */
public class ScreenNodeGraphDemo extends ModularGuiScreen
{
    public ScreenNodeGraphDemo()
    {
        super(new Provider());
    }

    private static class Provider implements GuiProvider
    {
        @Override
        public void buildGui(ModularGui gui)
        {
            gui.initFullscreenGui();
            gui.setGuiTitle(Component.literal("Node Graph Demo"));

            GuiElement<?> root = gui.getRoot();

            new GuiText(root, Component.literal(
                    "Node Graph — pan: middle-drag | zoom: scroll | wire: click port | Esc to close"))
                    .setShadow(true).setTextColour(0xFFFFFFFF)
                    .constrain(TOP,    literal(4))
                    .constrain(HEIGHT, literal(8))
                    .constrain(LEFT,   literal(4))
                    .constrain(RIGHT,  relative(root.get(RIGHT), -4));

            // ── Build graph ───────────────────────────────────────────────────
            NodeGraph graph = new NodeGraph();

            UUID sourceId      = UUID.randomUUID();   // SIGNAL source
            UUID sinkId        = UUID.randomUUID();   // SIGNAL sink  — pre-wired to source
            UUID itemInId      = UUID.randomUUID();   // ITEMS passthrough A
            UUID itemOutId     = UUID.randomUUID();   // ITEMS passthrough B — pre-wired to A
            UUID fluidId       = UUID.randomUUID();   // FLUID buffer (standalone, wire manually)
            UUID relayId       = UUID.randomUUID();   // ANY relay — pre-wired to source (ANY accepts SIGNAL)

            // Row 1: signal chain (y=60)
            graph.addNode(new NodeDef(sourceId,  TestSignalSourceNodeType.ID,   40,  60, null));
            graph.addNode(new NodeDef(sinkId,    TestSignalSinkNodeType.ID,    220,  60, null));

            // Row 2: item chain (y=160)
            graph.addNode(new NodeDef(itemInId,  TestItemPassthroughNodeType.ID, 40, 160, null));
            graph.addNode(new NodeDef(itemOutId, TestItemPassthroughNodeType.ID, 220, 160, null));

            // Row 3: fluid + any relay (y=260)
            graph.addNode(new NodeDef(fluidId,  TestFluidBufferNodeType.ID,  40, 260, null));
            graph.addNode(new NodeDef(relayId,  TestAnyRelayNodeType.ID,    220, 260, null));

            // ── Pre-wire connections ──────────────────────────────────────────
            // SIGNAL: source.out(0) → sink.in(0)
            graph.addConnection(new ConnectionDef(UUID.randomUUID(), sourceId, 0, sinkId, 0));

            // ITEMS: itemIn.out(1) → itemOut.in(0)
            graph.addConnection(new ConnectionDef(UUID.randomUUID(), itemInId, 1, itemOutId, 0));

            // ANY: source.out(0) → relay.in(0)  — demonstrates SIGNAL is compatible with ANY
            graph.addConnection(new ConnectionDef(UUID.randomUUID(), sourceId, 0, relayId, 0));

            // ── Canvas ───────────────────────────────────────────────────────
            GuiNodeCanvas canvas = new GuiNodeCanvas(root);
            canvas.setGraph(graph);
            canvas.constrain(TOP,    literal(16))
                    .constrain(BOTTOM, match(root.get(BOTTOM)))
                    .constrain(LEFT,   match(root.get(LEFT)))
                    .constrain(RIGHT,  match(root.get(RIGHT)));

            GuiRectangle canvasBg = new GuiRectangle(canvas).fill(0xFF0D0D0D);
            Constraints.bind(canvasBg, canvas);
        }
    }
}

