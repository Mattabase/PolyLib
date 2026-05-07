package net.creeperhost.testmod.init;

import com.mojang.serialization.Codec;
import net.creeperhost.polylib.player.serverdata.PlayerServerDataRegistry;
import net.creeperhost.polylib.player.serverdata.PlayerServerDataType;
import net.creeperhost.polylib.player.settings.BroadcastScope;
import net.creeperhost.polylib.player.settings.PlayerClientSettingsRegistry;
import net.creeperhost.polylib.player.settings.PlayerClientSettingsType;
import net.creeperhost.testmod.TestModCommon;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Testmod registrations for PlayerClientSettings and PlayerServerData.
 * Exercises the API added in feat/player-data (PR #105).
 *
 * NOTE: depends on feat/player-data PR being merged before this compiles standalone.
 */
public final class TestPlayerData
{
    /**
     * A simple boolean client setting — e.g. whether the player has opted into something.
     * Synced to all nearby players (TRACKING_RANGE) and preserved on death.
     */
    public static final PlayerClientSettingsType<Boolean> TEST_TOGGLE = PlayerClientSettingsRegistry.register(
            Identifier.fromNamespaceAndPath(TestModCommon.MOD_ID, "test_toggle"),
            StreamCodec.of(
                    (buf, val) -> buf.writeBoolean(val),
                    buf -> buf.readBoolean()
            ),
            () -> false,
            BroadcastScope.TRACKING_RANGE,
            true
    );

    /**
     * A simple integer server data value — e.g. a kill/use counter.
     * Synced to the owning player's client and lost on death.
     */
    public static final PlayerServerDataType<Integer> TEST_COUNTER = PlayerServerDataRegistry.register(
            Identifier.fromNamespaceAndPath(TestModCommon.MOD_ID, "test_counter"),
            Codec.INT,
            StreamCodec.of(
                    (buf, val) -> buf.writeInt(val),
                    buf -> buf.readInt()
            ),
            () -> 0,
            false
    );

    /**
     * A String server data value — tests StringData-equivalent persistence in the player data system.
     * Synced to the owning player's client and preserved on death.
     */
    public static final PlayerServerDataType<String> TEST_NAME = PlayerServerDataRegistry.register(
            Identifier.fromNamespaceAndPath(TestModCommon.MOD_ID, "test_name"),
            Codec.STRING,
            StreamCodec.of(
                    (buf, val) -> buf.writeUtf(val),
                    buf -> buf.readUtf()
            ),
            () -> "",
            true
    );

    // ── Offline-accessor coverage types ──────────────────────────────────────────
    // Used by /polytest playerdata to exercise the full login/logout lifecycle.
    // Also consumed by the DisCraftHonored testmod for OfflinePlayerDataAccessor tests.

    /** Lifecycle test: plain int, no sync, no copyOnDeath. */
    public static final PlayerServerDataType<Integer> OFFLINE_INT =
        PlayerServerDataRegistry.register(
            Identifier.fromNamespaceAndPath(TestModCommon.MOD_ID, "offline_int"),
            Codec.INT,
            null,
            () -> 0,
            false);

    /** Lifecycle test: string — round-trip and modify() coverage. */
    public static final PlayerServerDataType<String> OFFLINE_STRING =
        PlayerServerDataRegistry.register(
            Identifier.fromNamespaceAndPath(TestModCommon.MOD_ID, "offline_string"),
            Codec.STRING,
            null,
            () -> "unset",
            false);

    /** Lifecycle test: boolean — default-value coverage. */
    public static final PlayerServerDataType<Boolean> OFFLINE_BOOL =
        PlayerServerDataRegistry.register(
            Identifier.fromNamespaceAndPath(TestModCommon.MOD_ID, "offline_bool"),
            Codec.BOOL,
            null,
            () -> false,
            false);

    /** Lifecycle test: copyOnDeath=true — must not be corrupted by writes to sibling keys. */
    public static final PlayerServerDataType<Integer> OFFLINE_COPY_ON_DEATH =
        PlayerServerDataRegistry.register(
            Identifier.fromNamespaceAndPath(TestModCommon.MOD_ID, "offline_copy_on_death"),
            Codec.INT,
            null,
            () -> 0,
            true);

    /** Lifecycle test: syncsToClient=true — set() must still trigger S2C sync. */
    public static final PlayerServerDataType<Integer> OFFLINE_SYNCABLE =
        PlayerServerDataRegistry.register(
            Identifier.fromNamespaceAndPath(TestModCommon.MOD_ID, "offline_syncable"),
            Codec.INT,
            StreamCodec.of(
                (buf, v) -> buf.writeInt(v),
                buf -> buf.readInt()),
            () -> 0,
            false);


    public static void init()
    {
        // Static fields are initialised above; this method exists so TestModCommon can
        // trigger classloading at the right time (before the server starts).
        // No-op body is intentional.
    }
}
