package net.creeperhost.polylib.client.modulargui.builder;

import org.jetbrains.annotations.Nullable;

/**
 * Resolves named hook callbacks declared in a JSON GUI layout.
 * <p>
 * Hooks are referenced in element properties (e.g. {@code "on_click": "saveClicked"}) and
 * resolved to live {@link Runnable}s at GUI-build time.
 */
@FunctionalInterface
public interface HookResolver {

    /**
     * @param hookName the name of the hook as written in the JSON layout
     * @return the Runnable to invoke, or {@code null} to ignore this hook
     */
    @Nullable
    Runnable resolve(String hookName);

    /** No-op resolver — all hooks are silently ignored. */
    HookResolver NOOP = name -> null;
}
