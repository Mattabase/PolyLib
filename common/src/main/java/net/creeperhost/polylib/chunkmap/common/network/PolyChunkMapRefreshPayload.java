package net.creeperhost.polylib.chunkmap.common.network;

import io.netty.buffer.ByteBuf;
import net.creeperhost.polylib.Constants;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client → Server: request a full chunk-state resync for all watched dimensions.
 * The server marks all tracked chunks dirty and re-sends the entire state.
 */
public final class PolyChunkMapRefreshPayload implements CustomPacketPayload
{
    public static final PolyChunkMapRefreshPayload INSTANCE = new PolyChunkMapRefreshPayload();
    public static final Type<PolyChunkMapRefreshPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chunkmap_refresh"));
    public static final StreamCodec<ByteBuf, PolyChunkMapRefreshPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    private PolyChunkMapRefreshPayload() {}

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
