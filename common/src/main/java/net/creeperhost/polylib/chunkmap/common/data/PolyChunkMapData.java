package net.creeperhost.polylib.chunkmap.common.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Immutable snapshot of a single chunk's server-side state, sent from server → client.
 *
 * <p>This is the primary data carrier for the PolyLib chunk-map feature.
 * One instance is produced per loaded chunk each time its state changes.
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li>{@link #position}           — XZ chunk coordinates</li>
 *   <li>{@link #stage}              — current {@link ChunkStatus} (null when fully loaded)</li>
 *   <li>{@link #tickets}            — active tickets holding this chunk loaded</li>
 *   <li>{@link #statusLevel}        — raw distance-manager level for full-chunk status</li>
 *   <li>{@link #tickingStatusLevel} — raw distance-manager level for entity/block ticking</li>
 *   <li>{@link #unloading}          — true while the chunk is being unloaded</li>
 * </ul>
 */
public record PolyChunkMapData(
        ChunkPos position,
        @Nullable ChunkStatus stage,
        List<PolyChunkTicket> tickets,
        int statusLevel,
        int tickingStatusLevel,
        boolean unloading
) {
    /** Single-chunk stream codec for use in collections. */
    public static final StreamCodec<RegistryFriendlyByteBuf, PolyChunkMapData> STREAM_CODEC =
            StreamCodec.of(PolyChunkMapData::encode, PolyChunkMapData::decode);

    /** Collection codec (ArrayList) for bulk chunk-data payloads. */
    public static final StreamCodec<RegistryFriendlyByteBuf, List<PolyChunkMapData>> LIST_STREAM_CODEC =
            STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new));

    // ── Convenience accessors ──────────────────────────────────────────────────

    /**
     * Returns the effective {@link FullChunkStatus} based on which distance-manager
     * level is lower (more loaded): statusLevel or tickingStatusLevel.
     */
    public FullChunkStatus status()
    {
        if (this.tickingStatusLevel > this.statusLevel) {
            return ChunkLevel.fullStatus(this.tickingStatusLevel);
        }
        return ChunkLevel.fullStatus(this.statusLevel);
    }

    /** Returns a copy of this data without the unloading flag set. */
    public PolyChunkMapData withoutUnloading()
    {
        return new PolyChunkMapData(position, stage, tickets, statusLevel, tickingStatusLevel, false);
    }

    // ── Serialization ──────────────────────────────────────────────────────────

    private static void encode(RegistryFriendlyByteBuf buf, PolyChunkMapData data)
    {
        buf.writeChunkPos(data.position);
        buf.writeInt(data.statusLevel);
        buf.writeInt(data.tickingStatusLevel);
        buf.writeBoolean(data.unloading);
        PolyChunkMapCodecs.OPTIONAL_CHUNK_STATUS.encode(buf, Optional.ofNullable(data.stage));
        PolyChunkMapCodecs.TICKETS.encode(buf, data.tickets);
    }

    private static PolyChunkMapData decode(RegistryFriendlyByteBuf buf)
    {
        ChunkPos pos = buf.readChunkPos();
        int statusLevel = buf.readInt();
        int tickingStatusLevel = buf.readInt();
        boolean unloading = buf.readBoolean();
        ChunkStatus stage = PolyChunkMapCodecs.OPTIONAL_CHUNK_STATUS.decode(buf).orElse(null);
        List<PolyChunkTicket> tickets = PolyChunkMapCodecs.TICKETS.decode(buf);
        return new PolyChunkMapData(pos, stage, tickets, statusLevel, tickingStatusLevel, unloading);
    }
}
