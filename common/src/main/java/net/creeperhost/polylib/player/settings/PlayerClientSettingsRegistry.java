package net.creeperhost.polylib.player.settings;

import net.creeperhost.polylib.platform.Services;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Registry for {@link PlayerClientSettingsType} tokens.
 * All registration must happen before the server starts (typically in mod constructor or init).
 */
public final class PlayerClientSettingsRegistry
{
    private static final Map<String, PlayerClientSettingsType<?>> BY_ID = new LinkedHashMap<>();

    private PlayerClientSettingsRegistry() {}

    /**
     * Register a new player client setting type.
     *
     * @param id            Namespaced ID, e.g. {@code "discrafthonored:appearance"}
     * @param codec         StreamCodec for serialization
     * @param defaultFactory Supplier for default value when no data exists
     * @param scope         Who receives S2C sync when the value changes
     * @param copyOnDeath   Whether the value is preserved through player death/respawn
     * @return Typed token — store as a {@code public static final} constant
     */
    public static <T> PlayerClientSettingsType<T> register(String id,
                                                           StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                           Supplier<T> defaultFactory,
                                                           BroadcastScope scope,
                                                           boolean copyOnDeath)
    {
        if (BY_ID.containsKey(id))
            throw new IllegalStateException("PlayerClientSettingsType already registered: " + id);

        PlayerClientSettingsType<T> type = new PlayerClientSettingsType<>(id, codec, defaultFactory, scope, copyOnDeath);
        BY_ID.put(id, type);
        // Notify IPlayerDataHelper so it can allocate its platform storage slot
        Services.PLAYER_DATA.registerType(type);
        return type;
    }

    /** Returns all registered types — used internally by PolyLib event handlers. */
    public static Collection<PlayerClientSettingsType<?>> getAll()
    {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    /** Lookup by namespaced ID — used by packet handlers. */
    public static Optional<PlayerClientSettingsType<?>> byId(String id)
    {
        return Optional.ofNullable(BY_ID.get(id));
    }
}
