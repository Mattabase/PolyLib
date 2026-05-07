package net.creeperhost.polylib.chunkmap.common.network;

import io.netty.buffer.ByteBuf;
import net.creeperhost.polylib.Constants;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: signals that the player's permission to use the chunk-map has
 * been revoked (e.g. they were de-opped).  The client should close the screen.
 */
public final class PolyChunkMapByePayload implements CustomPacketPayload
{
    public static final PolyChunkMapByePayload INSTANCE = new PolyChunkMapByePayload();
    public static final Type<PolyChunkMapByePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chunkmap_bye"));
    public static final StreamCodec<ByteBuf, PolyChunkMapByePayload> CODEC =
            StreamCodec.unit(INSTANCE);

    private PolyChunkMapByePayload() {}

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
