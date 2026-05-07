package net.creeperhost.polylib.chunkmap.common.network;

import io.netty.buffer.ByteBuf;
import net.creeperhost.polylib.Constants;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → Client: signals that the server has granted the client permission
 * to use the chunk-map viewer.  Sent on join (if permitted) and on op grant.
 */
public final class PolyChunkMapHelloPayload implements CustomPacketPayload
{
    public static final PolyChunkMapHelloPayload INSTANCE = new PolyChunkMapHelloPayload();
    public static final Type<PolyChunkMapHelloPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chunkmap_hello"));
    public static final StreamCodec<ByteBuf, PolyChunkMapHelloPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    private PolyChunkMapHelloPayload() {}

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
