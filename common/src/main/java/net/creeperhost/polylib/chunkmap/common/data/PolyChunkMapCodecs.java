package net.creeperhost.polylib.chunkmap.common.data;

import com.google.common.collect.HashBiMap;
import io.netty.buffer.ByteBuf;
import net.creeperhost.polylib.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stream codec helpers for chunk-map network serialization.
 *
 * <p>Handles optional {@link ChunkStatus}, ticket lists, dimension keys, and the
 * custom-ticket-type registry needed to round-trip modded tickets across the wire.
 */
public final class PolyChunkMapCodecs
{
    // Fallback id used when a ticket type is not found in the vanilla registry
    private static final Identifier UNREGISTERED = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "unknown_ticket");

    /** Tracks custom (non-vanilla) TicketType ↔ Identifier mappings seen during decoding. */
    private static final HashBiMap<TicketType, Identifier> CUSTOM_TICKET_TYPES = HashBiMap.create();

    // ── Codec constants used by PolyChunkMapData ───────────────────────────────

    /** Optional ChunkStatus — written as boolean present flag + serialized-name string. */
    public static final StreamCodec<ByteBuf, Optional<ChunkStatus>> OPTIONAL_CHUNK_STATUS =
            StreamCodec.of(PolyChunkMapCodecs::encodeOptionalStatus, PolyChunkMapCodecs::decodeOptionalStatus);

    /** Single PolyChunkTicket over a FriendlyByteBuf. */
    public static final StreamCodec<FriendlyByteBuf, PolyChunkTicket> TICKET =
            StreamCodec.of(PolyChunkMapCodecs::encodeTicket, PolyChunkMapCodecs::decodeTicket);

    /** List of PolyChunkTicket. */
    public static final StreamCodec<FriendlyByteBuf, List<PolyChunkTicket>> TICKETS =
            ByteBufCodecs.<FriendlyByteBuf, PolyChunkTicket>list().apply(TICKET);

    /** ResourceKey<Level> over a RegistryFriendlyByteBuf. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceKey<Level>> DIMENSION =
            StreamCodec.of(
                    (buf, dim) -> buf.writeResourceKey(dim),
                    buf -> buf.readResourceKey(Registries.DIMENSION));

    /** List<ResourceKey<Level>>. */
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ResourceKey<Level>>> DIMENSIONS =
            ByteBufCodecs.<RegistryFriendlyByteBuf, ResourceKey<Level>>list().apply(DIMENSION);

    private PolyChunkMapCodecs() {}

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns the string representation of a {@link TicketType} for display purposes.
     * Falls back to the namespace:path of custom-registered types.
     */
    public static String ticketTypeName(TicketType type)
    {
        Identifier custom = CUSTOM_TICKET_TYPES.get(type);
        if (custom != null) return custom.toString();
        return Util.getRegisteredName(BuiltInRegistries.TICKET_TYPE, type);
    }

    // ── Optional<ChunkStatus> ──────────────────────────────────────────────────

    private static void encodeOptionalStatus(ByteBuf buf, Optional<ChunkStatus> status)
    {
        buf.writeBoolean(status.isPresent());
        status.ifPresent(s -> {
            byte[] bytes = s.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        });
    }

    private static Optional<ChunkStatus> decodeOptionalStatus(ByteBuf buf)
    {
        if (!buf.readBoolean()) return Optional.empty();
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        String name = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        // Look up by name in the built-in chunk status registry
        return BuiltInRegistries.CHUNK_STATUS.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst();
    }

    // ── PolyChunkTicket ────────────────────────────────────────────────────────

    private static void encodeTicket(FriendlyByteBuf buf, PolyChunkTicket ticket)
    {
        Identifier id = BuiltInRegistries.TICKET_TYPE.getKey(ticket.type());
        if (id == null) {
            id = CUSTOM_TICKET_TYPES.getOrDefault(ticket.type(), UNREGISTERED);
        }
        buf.writeIdentifier(id);
        buf.writeInt(ticket.ticketLevel());
        buf.writeInt((int) ticket.ticksLeft());
    }

    private static PolyChunkTicket decodeTicket(FriendlyByteBuf buf)
    {
        Identifier id = buf.readIdentifier();
        TicketType type = BuiltInRegistries.TICKET_TYPE.getOptional(id).orElseGet(() -> {
            // Lazily create/reuse a placeholder for unknown ticket types
            TicketType existing = CUSTOM_TICKET_TYPES.inverse().get(id);
            if (existing != null) return existing;
            TicketType placeholder = new TicketType(0L, TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING);
            CUSTOM_TICKET_TYPES.put(placeholder, id);
            return placeholder;
        });
        int level = buf.readInt();
        int ticks = buf.readInt();
        return new PolyChunkTicket(type, level, ticks);
    }
}
