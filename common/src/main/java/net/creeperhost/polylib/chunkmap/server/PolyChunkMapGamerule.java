package net.creeperhost.polylib.chunkmap.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Holds the PolyLib chunk-map access gamerule.
 *
 * <p>By default the chunk map is restricted to server operators on dedicated servers
 * (mirroring ChunkDebug).  Setting the {@code polylib:chunkMapOpenToAll} gamerule
 * to {@code true} bypasses the OP check and allows any connected player to
 * subscribe to chunk data.
 *
 * <p>Each platform module registers the gamerule using its own loader-specific API
 * and calls {@link #register(GameRule)} once with the resulting key.
 *
 * <ul>
 *   <li>NeoForge: subscribe to {@code RegisterGameRulesEvent} on the MOD bus.</li>
 *   <li>Fabric: use {@code FabricGameRuleRegistry} (fabric-game-rule-api-v1).</li>
 * </ul>
 */
public final class PolyChunkMapGamerule
{
    /** GameRule key — set once from the platform-specific registration pathway. */
    public static GameRule<Boolean> OPEN_TO_ALL;

    private PolyChunkMapGamerule() {}

    /**
     * Called by the platform registration hook with the freshly created rule.
     */
    public static void register(GameRule<Boolean> rule)
    {
        OPEN_TO_ALL = rule;
    }

    /**
     * Returns {@code true} if all players are allowed to use the chunk map
     * (i.e. the gamerule is enabled on the given server).
     */
    public static boolean isOpenToAll(MinecraftServer server)
    {
        if (OPEN_TO_ALL == null) return false;
        return server.overworld().getGameRules().get(OPEN_TO_ALL);
    }
}
