package net.creeperhost.testmod.nodegraph;

import net.creeperhost.polylib.client.modulargui.nodegraph.graph.INodeType;
import net.creeperhost.polylib.client.modulargui.nodegraph.graph.NodeDef;
import net.creeperhost.polylib.client.modulargui.nodegraph.graph.PortDescriptor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Test node: accepts ANY data type on input and re-emits it on output unchanged.
 * Exercises the ANY port wildcard compatibility — an ANY port can accept wires
 * from SIGNAL, ITEMS, FLUID, or any other type.
 */
public class TestAnyRelayNodeType implements INodeType
{
    public static final Identifier ID = Identifier.fromNamespaceAndPath("testmod", "any_relay");
    public static final TestAnyRelayNodeType INSTANCE = new TestAnyRelayNodeType();

    private static final List<PortDescriptor> PORTS = List.of(
            PortDescriptor.anyIn(0, "In"),
            PortDescriptor.anyOut(1, "Out")
    );

    private TestAnyRelayNodeType() {}

    @Override public Identifier getTypeId()                       { return ID; }
    @Override public Component  getDisplayName()                  { return Component.literal("Any Relay"); }
    @Override public List<PortDescriptor> getPorts(NodeDef node)  { return PORTS; }
}
