package net.creeperhost.polylib.player.settings;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Supplier;

/**
 * Typed token representing a registered PlayerClientSetting type.
 * Create via {@link PlayerClientSettingsRegistry#register} and store as a
 * {@code public static final} constant in your mod.
 *
 * @param <T> The value type
 */
public final class PlayerClientSettingsType<T>
{
    private final String id;
    private final StreamCodec<RegistryFriendlyByteBuf, T> codec;
    private final Supplier<T> defaultFactory;
    private final BroadcastScope scope;
    private final boolean copyOnDeath;

    PlayerClientSettingsType(String id,
                             StreamCodec<RegistryFriendlyByteBuf, T> codec,
                             Supplier<T> defaultFactory,
                             BroadcastScope scope,
                             boolean copyOnDeath)
    {
        this.id = id;
        this.codec = codec;
        this.defaultFactory = defaultFactory;
        this.scope = scope;
        this.copyOnDeath = copyOnDeath;
    }

    public String id()
    {
        return id;
    }

    public StreamCodec<RegistryFriendlyByteBuf, T> codec()
    {
        return codec;
    }

    public Supplier<T> defaultFactory()
    {
        return defaultFactory;
    }

    public BroadcastScope scope()
    {
        return scope;
    }

    public boolean copyOnDeath()
    {
        return copyOnDeath;
    }

    @Override
    public String toString()
    {
        return "PlayerClientSettingsType[" + id + "]";
    }
}
