# PolyLib Config Panel System — Revised Plan

**Supersedes:** The Config Panel section of `POLYLIB_EXTENSION_PLAN.md`  
**Guidance:** Gigabit101 — "make it work with PolyLib's config system using json5"  
**Key additions over original plan:**
1. Auto-hooks into NeoForge's native `IConfigScreenFactory` so mods don't write that boilerplate
2. Auto-hooks into Fabric's Mod Menu via `ModMenuCompat` entrypoint
3. Two registration tiers: vanilla `Screen` and PolyLib `ModularGui`/`GuiProvider`
4. ModularGuiInjector registration supported for config panels that inject into existing screens
5. Everything backed by `PolyConfig` / Jankson json5

---

## Problems the previous plan had

- `ConfigPanelProvider.createScreen(ModContainer, Screen)` — `ModContainer` is loader-specific; doesn't belong in common interface
- Devs still had to write their own NeoForge `IConfigScreenFactory` extension point registration — one more thing to forget
- No path for config panels that are ModularGui-based, even though PolyLib already ships ModularGui

---

## Design

### Three registration methods

```java
// ── Tier 1: vanilla Screen ────────────────────────────────────────────────
// Works on any loader. Screen backed by whatever the dev provides.
ConfigPanelRegistry.register(String modId, ScreenFactory screenFactory)
ConfigPanelRegistry.register(String modId, ScreenFactory screenFactory, KeyboardShortcut shortcut)

// ── Tier 2: ModularGui / GuiProvider ─────────────────────────────────────
// PolyLib wraps the provider in ModularGuiScreen automatically.
// Keybind and button injection are identical to Tier 1 — only the screen type differs.
ConfigPanelRegistry.registerModularGui(String modId, GuiProviderFactory guiFactory)
ConfigPanelRegistry.registerModularGui(String modId, GuiProviderFactory guiFactory, KeyboardShortcut shortcut)

// ── Tier 3: ModularGuiInjector overlay ───────────────────────────────────
// Config panel is injected directly into an existing vanilla screen rather than
// opening as a separate screen (e.g. overlaying the NeoForge ConfigurationScreen itself).
// Only for advanced cases — most mods will use Tier 1 or 2.
ConfigPanelRegistry.registerInjection(String modId, Predicate<Screen> screenPredicate, Function<Screen, GuiProvider> guiFunction)
```

**Functional interfaces** (in `net.creeperhost.polylib.client.config`):
```java
@FunctionalInterface public interface ScreenFactory {
    Screen create(Screen parent);
}

@FunctionalInterface public interface GuiProviderFactory {
    GuiProvider create(Screen parent);
}
```

---

### NeoForge native hook — `NeoForgeConfigHelper`

Lives in `neoforge` module. Mods call this **once** from their NeoForge constructor, instead of calling `ConfigPanelRegistry.register` + `modContainer.registerExtensionPoint` separately:

```java
// net.creeperhost.polylib.config.NeoForgeConfigHelper (neoforge module only)
public final class NeoForgeConfigHelper {

    /** Vanilla screen — registers with ConfigPanelRegistry AND NeoForge IConfigScreenFactory. */
    public static void register(net.neoforged.fml.ModContainer container, ScreenFactory factory) {
        ConfigPanelRegistry.register(container.getModId(), factory);
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mc, parent) -> factory.create(parent));
    }

    public static void register(net.neoforged.fml.ModContainer container, ScreenFactory factory, KeyboardShortcut shortcut) {
        ConfigPanelRegistry.register(container.getModId(), factory, shortcut);
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mc, parent) -> factory.create(parent));
    }

    /** ModularGui variant — wraps GuiProvider in ModularGuiScreen for both PolyLib registry and NeoForge. */
    public static void registerModularGui(net.neoforged.fml.ModContainer container, GuiProviderFactory factory) {
        ConfigPanelRegistry.registerModularGui(container.getModId(), factory);
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mc, parent) -> new ModularGuiScreen(factory.create(parent), parent));
    }

    public static void registerModularGui(net.neoforged.fml.ModContainer container, GuiProviderFactory factory, KeyboardShortcut shortcut) {
        ConfigPanelRegistry.registerModularGui(container.getModId(), factory, shortcut);
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mc, parent) -> new ModularGuiScreen(factory.create(parent), parent));
    }
}
```

**Why mods call this from their constructor:** `IConfigScreenFactory`/extension point registration in NeoForge must happen during mod construction — it's bound to the active `ModLoadingContext`. There is no deferred registration mechanism for this. `NeoForgeConfigHelper` is a single-call convenience that handles both PolyLib registration and NeoForge native registration together.

**On Fabric:** Mods call `ConfigPanelRegistry.register(modId, factory)` directly (no `ModContainer` needed). `ModMenuCompat` picks it up automatically.

---

### Fabric: Mod Menu auto-hook

`ModMenuCompat` (in `fabric` module, loaded only when Mod Menu is present):

```java
public class ModMenuCompat implements ModMenuApi {
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> out = new LinkedHashMap<>();
        ConfigPanelRegistry.getAll().forEach((modId, entry) ->
            out.put(modId, parent -> entry.createScreen(parent))
        );
        return out;
    }
}
```

`entry.createScreen(parent)` handles both tiers:
- Tier 1: calls `screenFactory.create(parent)` 
- Tier 2: calls `new ModularGuiScreen(guiFactory.create(parent), parent)`
- Tier 3 (injection): not surfaced via Mod Menu — injection is screen-specific

---

### Button injection fallback — still present

`ConfigPanelRegistry.injectConfigButton(ScreenEvent.Init.Post event)` still injects a button into:
- `ConfigurationSectionScreen` (NeoForge nested config screen — NeoForge's extension point only covers `ConfigurationScreen`)
- Any other screen a consumer mod requests via `registerInjection`

This is the "inject things the menus don't already support" path. PolyLib's `ScreenEvent.Init.Post` handler does:
1. Check if screen is `ConfigurationScreen` or `ConfigurationSectionScreen` → inject button for registered mods
2. Delegate to `ModularGuiInjector` for Tier 3 injection registrations

---

### PolyConfig backing

Config panel keybind enable/disable map stored in `polylib.json5` via Jankson:

```java
// PolyConfig.java
@Comment("Controls which mod config panel keybinds are active. " +
         "Added automatically when mods register config panels with keyboard shortcuts. " +
         "Set false to hide the keybind from Controls.")
public Map<String, Boolean> configPanelKeybinds = new LinkedHashMap<>();
```

`ConfigPanelRegistry` reads/writes this map via `PolylibCommon.configData.configPanelKeybinds` and calls `PolylibCommon.configBuilder.save(PolylibCommon.configData)` when a new entry is added.

---

### `ConfigPanelRegistry` internals

```java
public final class ConfigPanelRegistry {

    // Ordered — insertion order preserved for button stacking
    private static final LinkedHashMap<String, ConfigPanelEntry> ENTRIES = new LinkedHashMap<>();

    public sealed interface ConfigPanelEntry {
        String modId();
        @Nullable KeyMapping keyMapping();
        Screen createScreen(Screen parent);

        record VanillaEntry(String modId, ScreenFactory factory, @Nullable KeyMapping keyMapping)
            implements ConfigPanelEntry {
            public Screen createScreen(Screen parent) { return factory.create(parent); }
        }

        record ModularEntry(String modId, GuiProviderFactory factory, @Nullable KeyMapping keyMapping)
            implements ConfigPanelEntry {
            public Screen createScreen(Screen parent) {
                return new ModularGuiScreen(factory.create(parent), parent);
            }
        }
    }

    // Called from NeoForgeConfigHelper.register or directly on Fabric
    public static void register(String modId, ScreenFactory factory) { ... }
    public static void register(String modId, ScreenFactory factory, KeyboardShortcut shortcut) { ... }
    public static void registerModularGui(String modId, GuiProviderFactory factory) { ... }
    public static void registerModularGui(String modId, GuiProviderFactory factory, KeyboardShortcut shortcut) { ... }

    // Tier 3: delegates directly to ModularGuiInjector
    public static void registerInjection(String modId, Predicate<Screen> predicate, Function<Screen, GuiProvider> fn) {
        ModularGuiInjector.registerInjection(predicate, fn);
    }

    // Used by ModMenuCompat and button injection
    public static Map<String, ConfigPanelEntry> getAll() { return Collections.unmodifiableMap(ENTRIES); }

    // NeoForge ScreenEvent.Init.Post + Fabric AFTER_INIT
    public static void injectConfigButton(Screen screen, Consumer<AbstractWidget> addListener) { ... }

    // ClientTickEvent / ClientTickEvents.END_CLIENT_TICK
    public static void tickKeybinds() { ... }
}
```

---

### `KeyboardShortcut` record (unchanged from original plan)

```java
public record KeyboardShortcut(int defaultKey, InputConstants.Type inputType, String category, boolean suggestedOnly) {
    public static KeyboardShortcut unbound(String category) { ... }
    public static KeyboardShortcut suggested(int key, InputConstants.Type type, String category) { ... }
    public static KeyboardShortcut bound(int key, InputConstants.Type type, String category) { ... }
}
```

---

## File list

### New files
| Module | File | Purpose |
|---|---|---|
| `common` | `client/config/ScreenFactory.java` | `@FunctionalInterface Screen create(Screen parent)` |
| `common` | `client/config/GuiProviderFactory.java` | `@FunctionalInterface GuiProvider create(Screen parent)` |
| `common` | `client/config/KeyboardShortcut.java` | Record for keybind config |
| `common` | `client/config/ConfigPanelRegistry.java` | Central registry — all three tiers |
| `neoforge` | `config/NeoForgeConfigHelper.java` | NeoForge-specific: registers with PolyLib AND `IConfigScreenFactory` |
| `fabric` | `ModMenuCompat.java` | Mod Menu entrypoint — reads `ConfigPanelRegistry.getAll()` |

### Modified files
| Module | File | Change |
|---|---|---|
| `common` | `config/PolyConfig.java` | + `configPanelKeybinds`, + `radicalAccessibility` |
| `neoforge` | `NeoForgeClientEvents.java` | + `ConfigPanelRegistry.injectConfigButton`, + `tickKeybinds` |
| `fabric` | `PolyLibClientFabric.java` | + `ConfigPanelRegistry.injectConfigButton` in AFTER_INIT, + `tickKeybinds` in tick |
| `fabric` | `fabric.mod.json` | + `modmenu` entrypoint, + `suggests: modmenu` |

---

## DisCraftHonored usage

```java
// DisCraftHonoredNeoForge.java — NeoForge constructor (one call does everything)
NeoForgeConfigHelper.registerModularGui(
    modContainer,
    parent -> new DisCraftHonoredConfigLayout(modContainer, parent),
    KeyboardShortcut.suggested(GLFW.GLFW_KEY_F6, InputConstants.Type.KEYSYM, "key.categories.discrafthonored")
);

// DisCraftHonoredFabric.java — Fabric constructor
ConfigPanelRegistry.registerModularGui(
    "discrafthonored",
    parent -> new DisCraftHonoredConfigLayout(null, parent),
    KeyboardShortcut.suggested(GLFW.GLFW_KEY_F6, InputConstants.Type.KEYSYM, "key.categories.discrafthonored")
);
```

OR if keeping the existing `DisCraftHonoredConfigScreen` (a vanilla Screen subclass):
```java
// NeoForge
NeoForgeConfigHelper.register(
    modContainer,
    parent -> new DisCraftHonoredConfigScreen(modContainer, parent)
);
```

---

## Implementation order for config panel section

1. `PolyConfig.java` — add two fields
2. `ScreenFactory.java`, `GuiProviderFactory.java`, `KeyboardShortcut.java` — three tiny files
3. `ConfigPanelRegistry.java` — core registry with all three tiers
4. `NeoForgeConfigHelper.java` — NeoForge module helper
5. `ModMenuCompat.java` — Fabric module entrypoint
6. `NeoForgeClientEvents.java` — add two calls
7. `PolyLibClientFabric.java` — add two calls
8. `fabric.mod.json` — add entrypoint + suggests
