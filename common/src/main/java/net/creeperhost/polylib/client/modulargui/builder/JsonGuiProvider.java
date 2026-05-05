package net.creeperhost.polylib.client.modulargui.builder;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.ModularGui;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.GuiProvider;
import net.creeperhost.polylib.client.modulargui.lib.geometry.ConstrainedGeometry;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.*;

/**
 * A {@link GuiProvider} that constructs a modular GUI from a {@link GuiLayout} JSON document.
 *
 * <p>Usage:
 * <pre>{@code
 * GuiLayout layout = GuiLayout.fromPath(path);
 * JsonGuiProvider provider = new JsonGuiProvider(layout)
 *     .setHookResolver(name -> switch (name) {
 *         case "onSave" -> this::handleSave;
 *         default -> null;
 *     });
 * Minecraft.getInstance().setScreen(new ModularGuiScreen(provider));
 * }</pre>
 *
 * <h3>Build order</h3>
 * <ol>
 *   <li>Elements are topologically sorted so parents are always built before their children.</li>
 *   <li>For each element the registered {@link LayoutHandler} is invoked to create the element.</li>
 *   <li>Any {@code fill} shorthand is applied via {@link Constraints#bind}.</li>
 *   <li>Individual per-{@link GeoParam} constraints from {@link ConstraintSpec} are applied.</li>
 *   <li>The element is stored by its ID for use as a constraint reference target.</li>
 * </ol>
 */
public class JsonGuiProvider implements GuiProvider {

    private static final Logger LOGGER = LogManager.getLogger(JsonGuiProvider.class);

    private GuiLayout layout;
    private HookResolver hookResolver = HookResolver.NOOP;
    private DataBindingResolver bindingResolver = DataBindingResolver.NOOP;

    /** Last ModularGui used in buildGui() — held weakly so we don't prevent GC. */
    @Nullable
    private WeakReference<ModularGui> lastGui;

    /** Last built element map, keyed by element ID. */
    private final Map<String, GuiElement<?>> builtElements = new LinkedHashMap<>();

    /** Active hot-reload watcher, if any. */
    @Nullable
    private LayoutWatcher watcher;

    public JsonGuiProvider(GuiLayout layout) {
        this.layout = layout;
    }

    // ── Factory helpers ───────────────────────────────────────────────────────

    @Nullable
    public static JsonGuiProvider fromPath(Path path) {
        GuiLayout layout = GuiLayout.fromPath(path);
        return layout != null ? new JsonGuiProvider(layout) : null;
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    public JsonGuiProvider setHookResolver(HookResolver resolver) {
        this.hookResolver = resolver;
        return this;
    }

    public JsonGuiProvider setBindingResolver(DataBindingResolver resolver) {
        this.bindingResolver = resolver;
        return this;
    }

    public GuiLayout getLayout() {
        return layout;
    }

    /**
     * @return the element map from the most recent {@link #buildGui} or {@link #buildInto} call,
     *         keyed by element ID. The {@code "root"} key is absent from this map.
     */
    public Map<String, GuiElement<?>> getBuiltElements() {
        return Collections.unmodifiableMap(builtElements);
    }

    // ── Hot reload ────────────────────────────────────────────────────────────

    /**
     * Start watching {@code path} for changes. When the file is modified, the layout is
     * reloaded and the live GUI (if still open) is rebuilt in-place.
     *
     * <p>At most one watcher is active per provider instance. Calling this again replaces
     * the previous watcher.
     *
     * @param path the JSON layout file to watch
     * @return {@code this} for chaining
     */
    public JsonGuiProvider watchForChanges(Path path) {
        stopWatching();
        watcher = new LayoutWatcher(path, () -> hotReload(path));
        return this;
    }

    /** Stop the active file watcher, if any. */
    public void stopWatching() {
        if (watcher != null) {
            watcher.close();
            watcher = null;
        }
    }

    /**
     * Reload the layout from {@code path} and rebuild the live GUI if one is open.
     * Safe to call from the client tick thread.
     */
    public void hotReload(Path path) {
        GuiLayout newLayout = GuiLayout.fromPath(path);
        if (newLayout == null) {
            LOGGER.warn("JsonGuiProvider.hotReload: failed to parse '{}'", path);
            return;
        }
        this.layout = newLayout;
        ModularGui gui = lastGui != null ? lastGui.get() : null;
        if (gui != null) {
            gui.rebuild();
            LOGGER.info("JsonGuiProvider: hot-reloaded '{}'", path.getFileName());
        }
    }

    // ── GuiProvider ───────────────────────────────────────────────────────────

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        return new GuiElement<>(gui);
    }

    @Override
    public void buildGui(ModularGui gui) {
        lastGui = new WeakReference<>(gui);
        // Initialise root dimensions / position constraints
        switch (layout.type) {
            case SCREEN -> gui.initStandardGui(layout.defaultWidth, layout.defaultHeight);
            case HUD, INJECTION -> gui.initFullscreenGui();
        }
        buildElements(gui.getRoot());
    }

    /**
     * Build this layout's elements as children of {@code root} without calling
     * {@link ModularGui#initStandardGui} or {@link ModularGui#initFullscreenGui}.
     * <p>
     * Useful for embedding a layout preview inside another GUI element (e.g. the builder canvas).
     *
     * @param root the element to use as the layout root; receives all top-level elements as children
     */
    public void buildInto(GuiElement<?> root) {
        buildElements(root);
    }

    // ── Internal build logic ──────────────────────────────────────────────────

    private void buildElements(GuiElement<?> guiRoot) {
        builtElements.clear();

        // Single map used for both parent resolution and constraint ref resolution.
        // "root" always maps to the GUI root element.
        Map<String, ConstrainedGeometry<?>> elementMap = new LinkedHashMap<>();
        elementMap.put("root", guiRoot);

        List<GuiLayoutElement> ordered = topologicalSort(layout.elements);
        for (GuiLayoutElement spec : ordered) {

            // ── Resolve parent ────────────────────────────────────────────────
            ConstrainedGeometry<?> parentGeo;
            if (spec.parent == null || spec.parent.isEmpty() || "root".equals(spec.parent)) {
                parentGeo = guiRoot;
            } else {
                parentGeo = elementMap.get(spec.parent);
                if (parentGeo == null) {
                    LOGGER.warn("JsonGuiProvider: element '{}' references unknown parent '{}', falling back to root",
                            spec.id, spec.parent);
                    parentGeo = guiRoot;
                }
            }

            // ── Get handler ───────────────────────────────────────────────────
            @SuppressWarnings("unchecked")
            LayoutHandler<GuiElement<?>> handler =
                    (LayoutHandler<GuiElement<?>>) GuiLayoutRegistry.get(spec.type);
            if (handler == null) {
                LOGGER.error("JsonGuiProvider: no handler registered for element type '{}' (element '{}')",
                        spec.type, spec.id);
                continue;
            }

            // ── Build element ─────────────────────────────────────────────────
            JsonObject props = spec.properties != null ? spec.properties : new JsonObject();
            GuiElement<?> element;
            try {
                element = handler.build((net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent<?>) parentGeo,
                        props, hookResolver, bindingResolver);
            } catch (Exception e) {
                LOGGER.error("JsonGuiProvider: handler for '{}' threw an exception building element '{}'",
                        spec.type, spec.id, e);
                continue;
            }

            // ── Apply fill shorthand ──────────────────────────────────────────
            if (spec.fill != null) {
                ConstrainedGeometry<?> fillTarget = "parent".equals(spec.fill.refId)
                        ? parentGeo
                        : elementMap.get(spec.fill.refId);
                if (fillTarget != null) {
                    Constraints.bind(element, fillTarget, spec.fill.inset);
                } else {
                    LOGGER.warn("JsonGuiProvider: fill ref '{}' not found for element '{}'",
                            spec.fill.refId, spec.id);
                }
            }

            // ── Apply individual constraints ──────────────────────────────────
            if (spec.constraints != null && !spec.constraints.isEmpty()) {
                for (Map.Entry<String, ConstraintSpec> entry : spec.constraints.entrySet()) {
                    GeoParam param;
                    try {
                        param = GeoParam.valueOf(entry.getKey().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("JsonGuiProvider: unknown GeoParam '{}' on element '{}'",
                                entry.getKey(), spec.id);
                        continue;
                    }
                    var constraint = entry.getValue().resolve(param, elementMap, parentGeo);
                    if (constraint != null) {
                        element.constrain(param, constraint);
                    }
                }
            }

            // ── Register for future references ────────────────────────────────
            elementMap.put(spec.id, element);
            builtElements.put(spec.id, element);
        }
    }

    // ── Topological sort ──────────────────────────────────────────────────────

    /**
     * Sorts elements so that each parent element precedes its children.
     * Elements with no parent (or {@code parent="root"}) come first.
     */
    private static List<GuiLayoutElement> topologicalSort(List<GuiLayoutElement> elements) {
        // Build adjacency: parentKey → list of children
        Map<String, List<GuiLayoutElement>> childrenOf = new LinkedHashMap<>();
        for (GuiLayoutElement e : elements) {
            String parentKey = (e.parent == null || e.parent.isEmpty() || "root".equals(e.parent))
                    ? "" : e.parent;
            childrenOf.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(e);
        }

        List<GuiLayoutElement> result = new ArrayList<>(elements.size());
        // BFS from root level (empty-string parent key)
        Queue<GuiLayoutElement> queue = new ArrayDeque<>(childrenOf.getOrDefault("", List.of()));
        while (!queue.isEmpty()) {
            GuiLayoutElement e = queue.poll();
            result.add(e);
            List<GuiLayoutElement> children = childrenOf.get(e.id);
            if (children != null) queue.addAll(children);
        }

        // Append any elements that were unreachable (cycles, missing parents)
        if (result.size() < elements.size()) {
            Set<GuiLayoutElement> visited = new HashSet<>(result);
            for (GuiLayoutElement e : elements) {
                if (!visited.contains(e)) {
                    LOGGER.warn("JsonGuiProvider: element '{}' (parent='{}') could not be sorted — appending at end",
                            e.id, e.parent);
                    result.add(e);
                }
            }
        }

        return result;
    }
}
