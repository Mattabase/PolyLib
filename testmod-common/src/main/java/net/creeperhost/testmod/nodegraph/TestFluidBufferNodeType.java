package net.creeperhost.testmod.nodegraph;

import net.creeperhost.polylib.client.modulargui.nodegraph.graph.INodeType;
import net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeDef;
import net.creeperhost.polylib.client.modulargui.nodegraph.graph.PortDescriptor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Test node: accepts FLUID on one input port and emits it unchanged on one output port.
 * Exercises the FLUID port data type wire colour and compatibility checks.
 */
public class TestFluidBufferNodeType implements INodeType
{
    public static final Identifier ID = Identifier.fromNamespaceAndPath("testmod", "fluid_buffer");
    public static final TestFluidBufferNodeType INSTANCE = new TestFluidBufferNodeType();

    private static final List<PortDescriptor> PORTS = List.of(
            PortDescriptor.fluidIn(0, "In"),
            PortDescriptor.fluidOut(1, "Out")
    );

    private TestFluidBufferNodeType() {}

    @Override public Identifier getTypeId()                       { return ID; }
    @Override public Component  getDisplayName()                  { return Component.literal("Fluid Buffer"); }
    @Override public List<PortDescriptor> getPorts(NodeDef node)  { return PORTS; }
}
