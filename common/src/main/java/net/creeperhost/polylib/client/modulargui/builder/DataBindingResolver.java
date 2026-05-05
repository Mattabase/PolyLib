package net.creeperhost.polylib.client.modulargui.builder;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Resolves named data bindings declared in a JSON GUI layout.
 * <p>
 * Bindings are referenced in element properties (e.g. {@code "binding": "energyStored"}) and
 * resolved to live {@link Supplier}s at GUI-build time, enabling dynamic content without
 * requiring Java lambdas in the JSON.
 */
@FunctionalInterface
public interface DataBindingResolver {

    /**
     * @param bindingName the name of the binding as written in the JSON layout
     * @return a live Supplier for the bound value, or {@code null} if unresolved
     */
    @Nullable
    Supplier<?> resolve(String bindingName);

    /** No-op resolver — all bindings return {@code null}. */
    DataBindingResolver NOOP = name -> null;
}
