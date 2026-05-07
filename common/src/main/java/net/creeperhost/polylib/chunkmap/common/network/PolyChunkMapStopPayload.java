package net.creeperhost.polylib.chunkmap.common.network;

import net.creeperhost.polylib.Constants;
import net.creeperhost.polylib.chunkmap.common.data.PolyChunkMapCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Client → Server: unsubscribe from chunk-state updates.
 *
 * <p>If {@link #dimensions} is empty, the client is unsubscribed from <em>all</em>
 * dimensions it was previously watching.
 *
 * @param dimensions Specific dimensions to stop watching, or empty for all.
 */
public record PolyChunkMapStopPayload(
        List<ResourceKey<Level>> dimensions
) implements CustomPacketPayload {

    public static final Type<PolyChunkMapStopPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chunkmap_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PolyChunkMapStopPayload> CODEC =
            StreamCodec.of(
                    (buf, p) -> PolyChunkMapCodecs.DIMENSIONS.encode(buf, p.dimensions),
                    buf -> new PolyChunkMapStopPayload(PolyChunkMapCodecs.DIMENSIONS.decode(buf)));

    /** Convenience factory — stop watching everything. */
    public static PolyChunkMapStopPayload stopAll() { return new PolyChunkMapStopPayload(List.of()); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
