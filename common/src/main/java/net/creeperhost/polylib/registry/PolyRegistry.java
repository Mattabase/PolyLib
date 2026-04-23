package net.creeperhost.polylib.registry;

import net.creeperhost.polylib.platform.Services;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

public abstract class PolyRegistry<T> {
    
    /**
     * Creates a new PolyRegistry for the given registry key and mod ID.
     */
    public static <T> PolyRegistry<T> create(ResourceKey<? extends Registry<T>> registryKey, String modId) {
        return Services.REGISTRY.create(registryKey, modId);
    }
    
    /**
     * Registers a new object to this registry queue.
     * @param name The registry name (path).
     * @param supplier A supplier providing the object to register.
     * @return A supplier that will return the registered object once initialized.
     */
    public abstract <I extends T> Supplier<I> register(String name, Supplier<I> supplier);
    
    /**
     * Triggers the final registration sequence.
     * On Fabric, this natively registers the queued objects.
     * On NeoForge, this is a no-op as objects are passed directly to the DeferredRegister.
     */
    public abstract void init();
}
