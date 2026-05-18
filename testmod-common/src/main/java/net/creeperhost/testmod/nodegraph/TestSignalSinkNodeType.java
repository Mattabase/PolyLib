package net.creeperhost.testmod.nodegraph;

import net.creeperhost.polylib.client.modulargui.nodegraph.graph.INodeType;
import net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeDef;
import net.creeperhost.polylib.client.modulargui.nodegraph.graph.PortDescriptor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Test node: receives a SIGNAL input and silently consumes it.
 * Useful for creating a visually complete wired-up graph in the demo
 * (SignalSource → SignalSink shows a live bezier wire).
 */
public class TestSignalSinkNodeType implements INodeType
{
    public static final Identifier ID = Identifier.fromNamespaceAndPath("testmod", "signal_sink");
    public static final TestSignalSinkNodeType INSTANCE = new TestSignalSinkNodeType();

    private static final List<PortDescriptor> PORTS = List.of(
            PortDescriptor.signalIn(0, "Signal")
    );

    private TestSignalSinkNodeType() {}

    @Override public Identifier getTypeId()                       { return ID; }
    @Override public Component  getDisplayName()                  { return Component.literal("Signal Sink"); }
    @Override public List<PortDescriptor> getPorts(NodeDef node)  { return PORTS; }
}
