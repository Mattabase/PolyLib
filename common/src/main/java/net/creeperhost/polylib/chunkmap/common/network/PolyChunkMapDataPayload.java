package net.creeperhost.polylib.chunkmap.common.network;

import net.creeperhost.polylib.Constants;
import net.creeperhost.polylib.chunkmap.common.data.PolyChunkMapCodecs;
import net.creeperhost.polylib.chunkmap.common.data.PolyChunkMapData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Server → Client: streams a batch of chunk-state updates for a given dimension.
 *
 * <p>On initial subscription, {@link #initial} is {@code true} and the payload
 * contains the full current chunk set.  On subsequent ticks it contains only
 * the chunks whose state changed since the last tick (dirty set).
 *
 * @param dimension Dimension the chunks belong to
 * @param chunks    Batch of chunk data (up to 20 000 per packet)
 * @param tick      Server tick count at time of send (for ordering/debugging)
 * @param initial   Whether this is the initial full-sync burst
 */
public record PolyChunkMapDataPayload(
        ResourceKey<Level> dimension,
        List<PolyChunkMapData> chunks,
        int tick,
        boolean initial
) implements CustomPacketPayload {

    public static final Type<PolyChunkMapDataPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chunkmap_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PolyChunkMapDataPayload> CODEC =
            StreamCodec.of(PolyChunkMapDataPayload::encode, PolyChunkMapDataPayload::decode);

    /**
     * Convenience constructor accepting any Collection (converts to ArrayList).
     */
    public PolyChunkMapDataPayload(ResourceKey<Level> dimension, Collection<PolyChunkMapData> chunks,
                                   int tick, boolean initial)
    {
        this(dimension, new ArrayList<>(chunks), tick, initial);
    }

    private static void encode(RegistryFriendlyByteBuf buf, PolyChunkMapDataPayload p)
    {
        PolyChunkMapCodecs.DIMENSION.encode(buf, p.dimension);
        PolyChunkMapData.LIST_STREAM_CODEC.encode(buf, p.chunks);
        buf.writeInt(p.tick);
        buf.writeBoolean(p.initial);
    }

    private static PolyChunkMapDataPayload decode(RegistryFriendlyByteBuf buf)
    {
        ResourceKey<Level> dim = PolyChunkMapCodecs.DIMENSION.decode(buf);
        List<PolyChunkMapData> chunks = PolyChunkMapData.LIST_STREAM_CODEC.decode(buf);
        int tick = buf.readInt();
        boolean initial = buf.readBoolean();
        return new PolyChunkMapDataPayload(dim, chunks, tick, initial);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
