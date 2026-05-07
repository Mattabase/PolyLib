package net.creeperhost.polylib.chunkmap.server;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.creeperhost.polylib.Constants;
import net.creeperhost.polylib.chunkmap.common.network.*;
import net.creeperhost.polylib.chunkmap.common.data.PolyChunkMapData;
import net.creeperhost.polylib.chunkmap.server.tracker.PolyChunkTracker;
import net.creeperhost.polylib.chunkmap.server.tracker.PolyChunkTrackerHolder;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Server-side coordinator for the PolyLib chunk-map feature.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Maintains a per-dimension set of watching player UUIDs.</li>
 *   <li>Sends {@link PolyChunkMapDataPayload} diffs each tick.</li>
 *   <li>Handles {@link PolyChunkMapStartPayload}, {@link PolyChunkMapStopPayload},
 *       and {@link PolyChunkMapRefreshPayload} from clients.</li>
 *   <li>Sends {@link PolyChunkMapHelloPayload}/{@link PolyChunkMapByePayload}
 *       on join/op-change based on permission checks.</li>
 * </ul>
 *
 * <h3>Permission model</h3>
 * On dedicated servers, access is gated by OP level 3+ <em>unless</em> the
 * {@link PolyChunkMapGamerule#OPEN_TO_ALL} gamerule is set to {@code true}.
 * In single-player/LAN everyone is permitted.
 */
public final class PolyChunkMapServer
{
    private static final Logger LOGGER = LogManager.getLogger(Constants.MOD_NAME + "/ChunkMap");
    private static final int PACKET_PARTITION = 20_000;

    private static @Nullable PolyChunkMapServer INSTANCE;

    /** dimension → set of watching player UUIDs (thread-safe). */
    private final Multimap<ResourceKey<Level>, UUID> watching =
            Multimaps.synchronizedSetMultimap(HashMultimap.create());

    private PolyChunkMapServer() {}

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static void init()
    {
        INSTANCE = new PolyChunkMapServer();
    }

    public static @Nullable PolyChunkMapServer getInstance()
    {
        return INSTANCE;
    }

    // ── Permission ────────────────────────────────────────────────────────────

    public boolean isPermitted(ServerPlayer player)
    {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        // Single-player / integrated server → always permitted
        if (!server.isDedicatedServer()) return true;
        // Gamerule overrides OP requirement
        if (PolyChunkMapGamerule.isOpenToAll(server)) return true;
        // Dedicated server default: require OP ADMINS (old level 3)
        return isAtLeastAdmin(player);
    }

    /** Returns true if the player has at least {@link PermissionLevel#ADMINS} OP. */
    private static boolean isAtLeastAdmin(ServerPlayer player)
    {
        var perms = player.permissions();
        if (perms instanceof LevelBasedPermissionSet lbps) {
            return lbps.level().isEqualOrHigherThan(PermissionLevel.ADMINS);
        }
        // Fallback: allow if ALL_PERMISSIONS granted
        return perms == PermissionSet.ALL_PERMISSIONS;
    }

    // ── Lifecycle events (called from platform-specific event handlers) ────────

    /** Called when a player joins — sends Hello if permitted. */
    public void onPlayerJoin(ServerPlayer player, MinecraftServer server)
    {
        // Defer one tick so permission plugins (e.g. LuckPerms) finish loading
        server.execute(() -> {
            if (isPermitted(player))
                player.connection.send(new ClientboundCustomPayloadPacket(PolyChunkMapHelloPayload.INSTANCE));
        });
    }

    /** Called when a player is granted OP. */
    public void onOpPlayer(ServerPlayer player)
    {
        player.connection.send(new ClientboundCustomPayloadPacket(PolyChunkMapHelloPayload.INSTANCE));
    }

    /** Called when a player's OP is revoked. */
    public void onDeOpPlayer(ServerPlayer player)
    {
        // Stop watching and revoke access
        UUID uuid = player.getUUID();
        synchronized (watching) {
            for (ResourceKey<Level> dim : new ArrayList<>(watching.keySet())) {
                watching.remove(dim, uuid);
            }
        }
        player.connection.send(new ClientboundCustomPayloadPacket(PolyChunkMapByePayload.INSTANCE));
    }

    /** Called when a level is unloaded. */
    public void onLevelUnload(ServerLevel level)
    {
        watching.removeAll(level.dimension());
    }

    /**
     * Called at the end of each level tick to flush dirty chunks to watching clients.
     * Must be called on the level's server thread.
     */
    public void onLevelTick(ServerLevel level)
    {
        ResourceKey<Level> dimension = level.dimension();
        if (!watching.containsKey(dimension)) return;

        List<ServerPlayer> players = new ArrayList<>();
        List<Runnable> cleanup = new ArrayList<>();

        synchronized (watching) {
            for (UUID uuid : watching.get(dimension)) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
                if (player == null) {
                    cleanup.add(() -> watching.remove(dimension, uuid));
                } else {
                    players.add(player);
                }
            }
        }
        if (players.isEmpty()) { cleanup.forEach(Runnable::run); return; }

        PolyChunkTracker tracker = ((PolyChunkTrackerHolder) level).polylib$getChunkTracker();
        PolyChunkTracker.DirtyChunks dirty = tracker.getDirty();

        // Send updated chunks
        partitionInto(dirty.updated(), partition -> {
            PolyChunkMapDataPayload payload = new PolyChunkMapDataPayload(
                    dimension, partition, level.getServer().getTickCount(), false);
            ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
            for (ServerPlayer player : players) player.connection.send(packet);
        });

        // Send unloaded chunk positions
        if (!dirty.removed().isEmpty()) {
            PolyChunkMapUnloadPayload payload = new PolyChunkMapUnloadPayload(
                    dimension, dirty.removed().toLongArray());
            ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
            for (ServerPlayer player : players) player.connection.send(packet);
        }

        cleanup.forEach(Runnable::run);
    }

    // ── Payload handlers (called from network registration) ───────────────────

    public void handleStart(PolyChunkMapStartPayload payload, ServerPlayer player)
    {
        if (!isPermitted(player)) {
            LOGGER.warn("Player {} attempted chunk-map without permission", player.getScoreboardName());
            return;
        }
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        int tick = server.getTickCount();

        for (ResourceKey<Level> dimension : payload.dimensions()) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                LOGGER.warn("Player {} requested unknown dimension {}", player.getScoreboardName(), dimension);
                continue;
            }
            if (watching.put(dimension, player.getUUID())) {
                // Send full initial snapshot
                Collection<PolyChunkMapData> all =
                        ((PolyChunkTrackerHolder) level).polylib$getChunkTracker().getAll();
                partitionInto(all, partition -> {
                    PolyChunkMapDataPayload p = new PolyChunkMapDataPayload(dimension, partition, tick, true);
                    player.connection.send(new ClientboundCustomPayloadPacket(p));
                });
            }
        }
    }

    public void handleStop(PolyChunkMapStopPayload payload, ServerPlayer player)
    {
        UUID uuid = player.getUUID();
        if (payload.dimensions().isEmpty()) {
            synchronized (watching) {
                for (ResourceKey<Level> dim : new ArrayList<>(watching.keySet())) {
                    watching.remove(dim, uuid);
                }
            }
        } else {
            for (ResourceKey<Level> dim : payload.dimensions()) {
                watching.remove(dim, uuid);
            }
        }
    }

    public void handleRefresh(PolyChunkMapRefreshPayload payload, ServerPlayer player)
    {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        // Refresh must run on each level's tick thread; schedule via the server
        for (ServerLevel level : server.getAllLevels()) {
            server.execute(() -> ((PolyChunkTrackerHolder) level).polylib$getChunkTracker().refresh());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> void partitionInto(Collection<T> data, Consumer<Collection<T>> consumer)
    {
        if (data.isEmpty()) return;
        if (data.size() <= PACKET_PARTITION) { consumer.accept(data); return; }
        for (Collection<T> partition : Iterables.partition(data, PACKET_PARTITION)) {
            consumer.accept(partition);
        }
    }
}
