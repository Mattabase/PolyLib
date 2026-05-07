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
 * Client → Server: subscribe to chunk-state updates for the given dimensions.
 *
 * <p>The server responds with an initial full-sync {@link PolyChunkMapDataPayload}
 * followed by incremental dirty-chunk updates each tick.
 *
 * @param dimensions Dimensions to subscribe to; may contain multiple entries.
 */
public record PolyChunkMapStartPayload(
        List<ResourceKey<Level>> dimensions
) implements CustomPacketPayload {

    public static final Type<PolyChunkMapStartPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chunkmap_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PolyChunkMapStartPayload> CODEC =
            StreamCodec.of(
                    (buf, p) -> PolyChunkMapCodecs.DIMENSIONS.encode(buf, p.dimensions),
                    buf -> new PolyChunkMapStartPayload(PolyChunkMapCodecs.DIMENSIONS.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
