package net.creeperhost.polylib.client.modulargui.builder;

import net.creeperhost.polylib.client.modulargui.builder.handlers.BuiltinHandlers;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Registry for {@link LayoutHandler} implementations.
 *
 * <p>Built-in handlers are registered automatically via {@link BuiltinHandlers#register()} on
 * the first access. Third-party mods can register custom element types via
 * {@link #register(String, LayoutHandler)}.
 *
 * <p>Type keys are normalised to lowercase on registration and lookup.
 */
public class GuiLayoutRegistry {

    private static final Logger LOGGER = LogManager.getLogger(GuiLayoutRegistry.class);
    private static final Map<String, LayoutHandler<?>> HANDLERS = new LinkedHashMap<>();
    private static boolean builtinsRegistered = false;

    private GuiLayoutRegistry() {}

    /**
     * Register a custom element type.
     *
     * @param typeKey the JSON {@code "type"} string (normalised to lowercase)
     * @param handler the handler that creates and configures elements of this type
     */
    public static void register(String typeKey, LayoutHandler<?> handler) {
        String key = typeKey.toLowerCase(Locale.ROOT);
        if (HANDLERS.containsKey(key)) {
            LOGGER.warn("GuiLayoutRegistry: overwriting existing handler for type '{}'", key);
        }
        HANDLERS.put(key, handler);
    }

    /**
     * @return the handler for {@code typeKey}, or {@code null} if not registered
     */
    @Nullable
    public static LayoutHandler<?> get(String typeKey) {
        ensureBuiltins();
        return HANDLERS.get(typeKey.toLowerCase(Locale.ROOT));
    }

    /** @return an unmodifiable view of all registered type keys */
    public static Set<String> types() {
        ensureBuiltins();
        return Collections.unmodifiableSet(HANDLERS.keySet());
    }

    /** @return {@code true} if a handler is registered for {@code typeKey} */
    public static boolean isRegistered(String typeKey) {
        ensureBuiltins();
        return HANDLERS.containsKey(typeKey.toLowerCase(Locale.ROOT));
    }

    private static void ensureBuiltins() {
        if (!builtinsRegistered) {
            builtinsRegistered = true;
            BuiltinHandlers.register();
        }
    }
}
