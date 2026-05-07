package net.creeperhost.polylib.chunkmap.common.network;

import net.creeperhost.polylib.Constants;
import net.creeperhost.polylib.chunkmap.common.data.PolyChunkMapCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Server → Client: notifies the client that a set of chunk positions in a given
 * dimension have been unloaded.
 *
 * @param dimension  Dimension the chunks belong to
 * @param positions  Packed long[] of {@link net.minecraft.world.level.ChunkPos#pack(int,int)}
 */
public record PolyChunkMapUnloadPayload(
        ResourceKey<Level> dimension,
        long[] positions
) implements CustomPacketPayload {

    public static final Type<PolyChunkMapUnloadPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "chunkmap_unload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PolyChunkMapUnloadPayload> CODEC =
            StreamCodec.of(PolyChunkMapUnloadPayload::encode, PolyChunkMapUnloadPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, PolyChunkMapUnloadPayload p)
    {
        PolyChunkMapCodecs.DIMENSION.encode(buf, p.dimension);
        buf.writeInt(p.positions.length);
        for (long pos : p.positions) buf.writeLong(pos);
    }

    private static PolyChunkMapUnloadPayload decode(RegistryFriendlyByteBuf buf)
    {
        ResourceKey<Level> dim = PolyChunkMapCodecs.DIMENSION.decode(buf);
        int len = buf.readInt();
        long[] positions = new long[len];
        for (int i = 0; i < len; i++) positions[i] = buf.readLong();
        return new PolyChunkMapUnloadPayload(dim, positions);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
