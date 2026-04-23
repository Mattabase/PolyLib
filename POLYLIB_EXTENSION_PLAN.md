# PolyLib Extension Plan
## Accessibility, Config Panels, and Player Client Settings

**Status:** Design complete — ready for implementation  
**Repos affected:** `C:\Antigravity\PolyLib` (primary), `DisCraftHonored` (consumer migration)

---

## Overview

This document covers three interconnected systems being added to PolyLib:

| System | Purpose |
|---|---|
| **Accessibility Options** | Inject toggles into vanilla's Accessibility Options screen; enforce player-sovereign prefs server-side |
| **Config Panel Registry** | Register per-mod settings screens that appear in NeoForge Config / Mod Menu / keybind |
| **PlayerClientSettings** | Type-safe, loader-agnostic system for client-authored settings that persist server-side and sync to other clients |

All consumer-facing API lives in `common`. Platform specifics (NeoForge/Fabric) are hidden behind `Services`.

---

## Part 1 — Accessibility Options System

### Design intent

Accessibility preferences registered through this system are **player-sovereign by default**: the server stores and applies the player's value exactly as received, without consulting server configuration. This is the correct default for gameplay-safety toggles (fall rescue, auto-rescue, etc.).

A mod that wants server config to remain in control must explicitly opt out using `AccessibilityPolicy.RESPECTS_SERVER_CONFIG`.

A **Radical Accessibility** config flag in `PolyConfig` upgrades all `RESPECTS_SERVER_CONFIG` entries to `PLAYER_OVERRIDES_SERVER` at runtime, allowing a dedicated accessibility-focused modpack or server to guarantee all player prefs are honored unconditionally.

---

### New package: `net.creeperhost.polylib.accessibility`

#### `AccessibilityPolicy.java`
```java
public enum AccessibilityPolicy {
    /**
     * Default. Player's value is stored and applied server-side regardless of
     * server configuration. Use for gameplay-safety toggles (fall rescue, etc.).
     */
    PLAYER_OVERRIDES_SERVER,

    /**
     * Server config may clamp or override the player's preference.
     * Use for purely cosmetic/visual accessibility options.
     * Can be upgraded to PLAYER_OVERRIDES_SERVER by PolyConfig.radicalAccessibility.
     */
    RESPECTS_SERVER_CONFIG
}
```

#### `OptionsWidgetProvider.java`
```java
@FunctionalInterface
public interface OptionsWidgetProvider {
    void addOptions(OptionsListTarget target);
}
```

#### `OptionsListTarget.java`
Wraps the reflected `OptionsList` and exposes a rich builder API. Reflection is done once, cached, logged on failure.

```java
public final class OptionsListTarget {
    // Raw slots
    void addSmall(AbstractWidget left, @Nullable AbstractWidget right)
    void addBig(AbstractWidget widget)

    // High-level builders (use vanilla CycleButton / AbstractSliderButton internally)
    void addToggle(Component label, BooleanSupplier getter, Consumer<Boolean> setter)
    void addCycle(Component label, Supplier<Component> valueLabel, Runnable onCycle)
    void addSlider(Component label, DoubleSupplier getter, DoubleConsumer setter, double min, double max)
    void addButton(Component label, Runnable onClick)

    // Section header row (full-width text divider)
    void addCategory(Component title)

    // Conditional — uses Services.PLATFORM.isModLoaded internally
    void addIfModLoaded(String modId, Runnable addCall)
}
```

#### `AccessibilityOptionsRegistry.java`
Ordered static registry. Called from both loader event layers.

```java
public final class AccessibilityOptionsRegistry {
    // Default policy: PLAYER_OVERRIDES_SERVER
    public static void register(String key, OptionsWidgetProvider provider)

    // Explicit opt-out
    public static void register(String key, AccessibilityPolicy policy, OptionsWidgetProvider provider)

    // Conditional: only registers if mod is present; skips packet for absent mods
    public static void registerIfModLoaded(String modId, String key, OptionsWidgetProvider provider)
    public static void registerIfModLoaded(String modId, String key, AccessibilityPolicy policy, OptionsWidgetProvider provider)

    // Called from loader event layers (NeoForge: ScreenEvent.Init.Post, Fabric: ScreenEvents.AFTER_INIT)
    public static void inject(Screen screen)

    // Returns all PLAYER_OVERRIDES_SERVER keys (effective — respects radicalAccessibility flag)
    public static Set<String> getServerSyncedKeys()
}
```

`inject(screen)` checks if screen is `AccessibilityOptionsScreen`, reflects `OptionsList` (cached), wraps it in `OptionsListTarget`, iterates all registered providers. If `PolyConfig.radicalAccessibility` is true, all policies are treated as `PLAYER_OVERRIDES_SERVER`.

#### `AccessibilityPrefsC2SPayload.java`
Generic C2S packet — map of changed accessibility pref keys to boolean values. Only keys with policy `PLAYER_OVERRIDES_SERVER` (effective) are included.

```java
public record AccessibilityPrefsC2SPayload(Map<String, Boolean> values)
    implements CustomPacketPayload { ... }
```

#### `AccessibilityPrefsManager.java`
Server-side per-player store. Pure common — no loader API. Backed by `ConcurrentHashMap<UUID, Map<String, Boolean>>`.

```java
public final class AccessibilityPrefsManager {
    // Server handler calls this — stores values as-is, no config gate
    public static void applyFromClient(UUID playerUUID, Map<String, Boolean> values)

    // Read on server tick / gameplay logic
    public static boolean getOrDefault(UUID playerUUID, String key, boolean def)
    public static Optional<Boolean> get(UUID playerUUID, String key)

    // Called from player logout event
    public static void clearPlayer(UUID playerUUID)
}
```

---

### PolyConfig additions

```java
// PolyConfig.java
@Comment("When true, ALL registered accessibility preferences are treated as " +
         "PLAYER_OVERRIDES_SERVER regardless of how they were registered. " +
         "Enables full player-sovereign accessibility for modpacks/servers.")
public boolean radicalAccessibility = false;
```

---

### Loader wiring

**NeoForge** (`NeoForgeClientEvents.java`):
```java
@SubscribeEvent
public static void eventInitScreenEvent(ScreenEvent.Init.Post event) {
    ModularGuiInjector.initPost(event.getScreen());  // existing
    AccessibilityOptionsRegistry.inject(event.getScreen());  // new
    ConfigPanelRegistry.injectConfigButton(event);  // new (Part 2)
}
```

**NeoForge** (`NeoForgeNetworkHelper.java`):
Add one `registrar.playToServer(AccessibilityPrefsC2SPayload.TYPE, ...)` handler calling `AccessibilityPrefsManager.applyFromClient`.

**NeoForge** (new `NeoForgeServerEvents.java`):
```java
@SubscribeEvent
public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    AccessibilityPrefsManager.clearPlayer(event.getEntity().getUUID());
    PlayerClientSettingsManager.clearPlayer(event.getEntity().getUUID());  // Part 3
}
```

**Fabric** (`FabricClientEvents.java`):
Same `AccessibilityOptionsRegistry.inject` call via `ScreenEvents.AFTER_INIT`.

**Fabric** (`FabricServerEvents.java`):
Same logout cleanup via Fabric API `ServerPlayConnectionEvents.DISCONNECT`.

---

## Part 2 — Config Panel Registry

### Design intent

Any mod can register a settings `Screen` factory with PolyLib once. PolyLib then automatically surfaces it via all available mechanisms depending on the loader and installed mods — without the mod needing to know which mechanism is active.

| Loader | Mechanism | Condition |
|---|---|---|
| NeoForge | Button injected into `ConfigurationScreen` / `ConfigurationSectionScreen` | Always |
| Fabric | `ModMenuApi.getModConfigScreenFactory()` | If Mod Menu present |
| Both | Keybind (visible in Controls) | If dev provided `KeyboardShortcut` AND user config enables it |

---

### New package: `net.creeperhost.polylib.client.config`

#### `ConfigPanelProvider.java`
```java
public interface ConfigPanelProvider {
    /** Button label shown in NeoForge Config screen and Mod Menu. */
    Component getButtonLabel();

    /** Creates and returns the settings screen. parent is the screen to return to on close. */
    Screen createScreen(@Nullable ModContainer modContainer, Screen parent);
}
```

#### `KeyboardShortcut.java`
```java
public record KeyboardShortcut(
    int defaultKey,
    InputConstants.Type inputType,
    String category,
    boolean suggestedOnly  // true = UNBOUND default; false = use defaultKey as actual default
) {
    /** Unbound by default — user assigns in Controls. */
    public static KeyboardShortcut unbound(String category)

    /** Has a suggested default key, but still UNBOUND by default (recommended). */
    public static KeyboardShortcut suggested(int key, InputConstants.Type type, String category)

    /** Bound by default — use sparingly, consumes user's key space. */
    public static KeyboardShortcut bound(int key, InputConstants.Type type, String category)
}
```

#### `ConfigPanelRegistry.java`
```java
public final class ConfigPanelRegistry {
    // No keybind
    public static void register(String modId, ConfigPanelProvider provider)

    // With optional keybind
    public static void register(String modId, ConfigPanelProvider provider, KeyboardShortcut shortcut)

    // Returns all registrations — used by ModMenuCompat and NeoForge button injection
    public static Map<String, ConfigPanelEntry> getAll()

    // Called from NeoForge ScreenEvent.Init.Post — injects button(s) into ConfigurationScreen
    public static void injectConfigButton(ScreenEvent.Init.Post event)

    // Called from ClientTickEvent — checks all active keybinds
    public static void tickKeybinds()

    // Record holding registered data
    public record ConfigPanelEntry(
        ConfigPanelProvider provider,
        @Nullable KeyMapping keyMapping,  // null if no shortcut or user disabled
        boolean keybindEnabled  // read from PolyConfig.configPanelKeybinds
    ) {}
}
```

`register(...)` with a `KeyboardShortcut`:
1. Reads `PolyConfig.configPanelKeybinds.getOrDefault(modId, true)`
2. If enabled: creates `KeyMapping` using the shortcut's default key (or UNKNOWN if `suggestedOnly`)
3. If disabled: creates `KeyMapping` with `InputConstants.UNKNOWN` (appears unbound; optionally hidden from Controls list)
4. Writes the entry to `PolyConfig.configPanelKeybinds` if not already present, saves config

---

### PolyConfig additions

```java
@Comment("Controls which mod config panel keybinds are active. " +
         "Entries are added automatically when mods register config panels with keybinds. " +
         "Set to false to hide a keybind from the Controls screen.")
public Map<String, Boolean> configPanelKeybinds = new LinkedHashMap<>();
```

---

### NeoForge button injection

`injectConfigButton(event)` checks if the screen is `ConfigurationScreen` or `ConfigurationSectionScreen`. For each registered panel, adds a button at the bottom-left, stacking upward. Uses `event.addListener(...)` (same pattern as current `ScreenEventHandler`).

The existing `ScreenEventHandler.injectAppearanceButton` in DisCraftHonored is deleted — replaced by this.

---

### Fabric: Mod Menu soft dependency

`fabric/src/main/java/.../ModMenuCompat.java`:
```java
public class ModMenuCompat implements ModMenuApi {
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> map = new LinkedHashMap<>();
        ConfigPanelRegistry.getAll().forEach((modId, entry) ->
            map.put(modId, parent -> entry.provider().createScreen(null, parent))
        );
        return map;
    }
}
```

`fabric.mod.json`:
```json
"entrypoints": {
  "modmenu": ["net.creeperhost.polylib.fabric.ModMenuCompat"]
},
"suggests": {
  "modmenu": "*"
}
```

Uses `"suggests"` not `"depends"` — Mod Menu is fully optional.

---

### Tick handler (both loaders)

```java
// Called from ClientTickEvent (NeoForge) / ClientTickEvents.END_CLIENT_TICK (Fabric)
ConfigPanelRegistry.tickKeybinds();
```

Inside `tickKeybinds()`:
```java
for (ConfigPanelEntry entry : getAll().values()) {
    if (entry.keyMapping() != null && entry.keybindEnabled() && entry.keyMapping().consumeClick()) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {  // only when no screen is open
            mc.setScreen(entry.provider().createScreen(null, mc.screen));
        }
    }
}
```

---

## Part 3 — PlayerClientSettings System

### Design intent

Type-safe, loader-transparent system for settings that are:
- **Authored on the client** (the player controls them)
- **Persisted server-side** (survive world reload, server restart, etc.)
- **Synced to other clients** based on a declared broadcast scope

The consumer writes all registration and access code in `common`. PolyLib handles persistence (NeoForge AttachmentType or Fabric SavedData) and networking transparently.

---

### New package: `net.creeperhost.polylib.player.settings`

#### `BroadcastScope.java`
```java
public enum BroadcastScope {
    /**
     * Stored server-side only. Never sent to any client.
     * Use for server-computed preferences that don't affect rendering.
     */
    SERVER_ONLY,

    /**
     * Sent to the owning player only (e.g. per-player UI state, accessibility prefs).
     */
    SELF_ONLY,

    /**
     * Sent to all players currently online when changed.
     * Also sent to newly joining players (full backfill on login).
     * Use when all players need the data regardless of proximity.
     */
    ALL_ONLINE,

    /**
     * Sent only to players currently tracking the owner (within render/entity range).
     * Backfill is automatic via PlayerEvent.StartTracking — no explicit login sync needed.
     * Recommended for appearance/cosmetic data. More efficient than ALL_ONLINE.
     */
    TRACKING_RANGE
}
```

#### `PlayerClientSettingsType<T>.java`
The typed token. Returned by `register(...)`, stored as a `public static final` constant by the consumer mod.

```java
public final class PlayerClientSettingsType<T> {
    private final String id;  // namespaced, e.g. "discrafthonored:appearance"
    private final StreamCodec<RegistryFriendlyByteBuf, T> codec;
    private final Supplier<T> defaultFactory;
    private final BroadcastScope scope;
    private final boolean copyOnDeath;

    // package-private constructor — created only by PlayerClientSettingsRegistry
}
```

#### `PlayerClientSettingsRegistry.java`
```java
public final class PlayerClientSettingsRegistry {

    /**
     * Register a new player client setting type.
     *
     * @param id            Namespaced ID, e.g. "discrafthonored:appearance"
     * @param codec         StreamCodec for serialization
     * @param defaultFactory Supplier for default value (used when no data exists)
     * @param scope         Who receives S2C sync when the value changes
     * @param copyOnDeath   Whether the value is preserved through player death/respawn
     * @return Typed token — store as a public static final constant
     */
    public static <T> PlayerClientSettingsType<T> register(
        String id,
        StreamCodec<RegistryFriendlyByteBuf, T> codec,
        Supplier<T> defaultFactory,
        BroadcastScope scope,
        boolean copyOnDeath
    )

    // Returns all registered types — used internally by PolyLib's event handlers
    public static Collection<PlayerClientSettingsType<?>> getAll()

    // Lookup by ID — used by packet handlers
    public static Optional<PlayerClientSettingsType<?>> byId(String id)
}
```

#### `PlayerClientSettingsManager.java`
The single access point for consumer mod code.

```java
public final class PlayerClientSettingsManager {

    // ── Server-side ──────────────────────────────────────────────────────────

    /**
     * Get the current value for a player. Reads from in-memory typed cache.
     * Zero deserialization cost on repeated reads.
     */
    public static <T> T get(UUID playerUUID, PlayerClientSettingsType<T> type)

    /**
     * Set a value on the server (e.g. from a command or server-side logic).
     * Persists via IPlayerDataHelper and broadcasts per scope.
     */
    public static <T> void set(ServerPlayer player, PlayerClientSettingsType<T> type, T value)

    // ── Client-side ──────────────────────────────────────────────────────────

    /**
     * Send an updated value to the server. Called from client UI (e.g. config screen Save button).
     */
    public static <T> void sendToServer(PlayerClientSettingsType<T> type, T value)

    // ── PolyLib-internal lifecycle (called from server event handlers) ────────

    /** Called on login: loads persisted data, sends SELF_ONLY/ALL_ONLINE types to client, backfills tracker data */
    public static void onPlayerLogin(ServerPlayer player)

    /** Called on logout: saves dirty data, removes from in-memory cache */
    public static void onPlayerLogout(UUID uuid)

    /** Called on respawn: copies types where copyOnDeath=true */
    public static void onPlayerRespawn(UUID oldUUID, ServerPlayer newPlayer)

    /** Called from StartTracking: sends TRACKING_RANGE types of tracked player to the new tracker */
    public static void syncTrackingRange(ServerPlayer tracked, ServerPlayer tracker)

    /** Called from C2S packet handler: applies value, triggers broadcast */
    public static void applyFromClient(ServerPlayer player, String typeId, byte[] data)
}
```

#### `PlayerClientSettingsStore.java`
Per-player in-memory store. Not part of public API — used internally by `PlayerClientSettingsManager`.

```
Map<PlayerClientSettingsType<?>, Object>  — typed runtime cache (zero-deserialize reads)
Set<PlayerClientSettingsType<?>> dirty    — tracks which types need persisting on logout
```

#### Payloads

```java
// C2S: client updated a setting
public record UpdatePlayerClientSettingC2SPayload(String typeId, byte[] data)
    implements CustomPacketPayload { ... }

// S2C: server syncing a setting to one or more clients
public record PlayerClientSettingSyncS2CPayload(UUID playerUUID, String typeId, byte[] data)
    implements CustomPacketPayload { ... }
```

---

### Platform service: `IPlayerDataHelper`

Abstracts persistence behind the existing `Services` pattern.

**Interface** (`common/platform/services/IPlayerDataHelper.java`):
```java
public interface IPlayerDataHelper {
    /** Called during registration — platform allocates its storage slot */
    <T> void registerType(PlayerClientSettingsType<T> type);

    /** Load all persisted data for a player into the provided store */
    void loadAll(UUID playerUUID, ServerPlayer player, PlayerClientSettingsStore store);

    /** Persist all dirty entries in the store for this player */
    void saveAll(UUID playerUUID, ServerPlayer player, PlayerClientSettingsStore store);
}
```

**NeoForge impl** (`NeoForgePlayerDataHelper.java`):
- `registerType` → allocates a `DeferredHolder<AttachmentType<byte[]>>` per type (all types share one registration call shape; data stored as serialized bytes in attachment, deserialized into `PlayerClientSettingsStore` on load)
- `loadAll` → reads all attachment data into store's typed cache
- `saveAll` → writes all dirty typed values back to attachments via `player.setData(...)`

**Fabric impl** (`FabricPlayerDataHelper.java`):
- Uses `ServerLevel.getDataStorage()` `SavedData` — one `SavedData` per type, keyed by `playerUUID`. This is the same pattern PolyLib uses for tile data. No Fabric API hard dependency for persistence.
- `copyOnDeath` handled via `ServerPlayNetworking` respawn callbacks instead of attachment flag

**`Services.java` addition:**
```java
public static final IPlayerDataHelper PLAYER_DATA = load(IPlayerDataHelper.class);
```

---

### Startup wiring

**NeoForge** (`PolyLibNeoForge.java`):
```java
// In constructor, alongside existing registrations:
PlayerClientSettingsRegistry.getAll().forEach(Services.PLAYER_DATA::registerType);
// Note: registerType is called lazily — types registered after PolyLib init
// are handled via a post-registration hook in PlayerClientSettingsRegistry
```

**NeoForge** (`NeoForgeServerEvents.java` — new file):
```java
@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoForgeServerEvents {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerClientSettingsManager.onPlayerLogin(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        AccessibilityPrefsManager.clearPlayer(uuid);
        PlayerClientSettingsManager.onPlayerLogout(uuid);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerClientSettingsManager.onPlayerRespawn(event.getOriginalPlayer().getUUID(), sp);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer tracked
                && event.getEntity() instanceof ServerPlayer tracker) {
            PlayerClientSettingsManager.syncTrackingRange(tracked, tracker);
        }
    }
}
```

**NeoForge** (`NeoForgeNetworkHelper.java`):
```java
// In onRegisterPayloads, alongside existing registrations:
registrar.playToServer(UpdatePlayerClientSettingC2SPayload.TYPE,
    UpdatePlayerClientSettingC2SPayload.CODEC,
    (payload, ctx) -> ctx.enqueueWork(() -> {
        if (ctx.player() instanceof ServerPlayer sp) {
            PlayerClientSettingsManager.applyFromClient(sp, payload.typeId(), payload.data());
        }
    }));

registrar.playToClient(PlayerClientSettingSyncS2CPayload.TYPE,
    PlayerClientSettingSyncS2CPayload.CODEC,
    (payload, ctx) -> ctx.enqueueWork(() -> {
        // Client receives and caches other players' settings for rendering
        // Dispatched to a client-side cache (see below)
        PlayerClientSettingsClientCache.receive(payload.playerUUID(), payload.typeId(), payload.data());
    }));

registrar.playToServer(AccessibilityPrefsC2SPayload.TYPE,
    AccessibilityPrefsC2SPayload.CODEC,
    (payload, ctx) -> ctx.enqueueWork(() ->
        AccessibilityPrefsManager.applyFromClient(ctx.player().getUUID(), payload.values())));
```

---

### Client-side cache

```java
// common — used on logical client
public final class PlayerClientSettingsClientCache {
    // Called when S2C sync packet received
    public static void receive(UUID playerUUID, String typeId, byte[] data)

    // Read by rendering code, returns deserialized value
    public static <T> T getFor(UUID playerUUID, PlayerClientSettingsType<T> type)

    // Called on disconnect
    public static void clear()
}
```

This replaces `PlayerAppearanceCache` in DisCraftHonored.

---

## Complete file list

### PolyLib — new files

#### `common`
| File | Package |
|---|---|
| `AccessibilityPolicy.java` | `accessibility` |
| `OptionsWidgetProvider.java` | `accessibility` |
| `OptionsListTarget.java` | `accessibility` |
| `AccessibilityOptionsRegistry.java` | `accessibility` |
| `AccessibilityPrefsC2SPayload.java` | `accessibility` |
| `AccessibilityPrefsManager.java` | `accessibility` |
| `ConfigPanelProvider.java` | `client.config` |
| `KeyboardShortcut.java` | `client.config` |
| `ConfigPanelRegistry.java` | `client.config` |
| `BroadcastScope.java` | `player.settings` |
| `PlayerClientSettingsType.java` | `player.settings` |
| `PlayerClientSettingsRegistry.java` | `player.settings` |
| `PlayerClientSettingsManager.java` | `player.settings` |
| `PlayerClientSettingsStore.java` | `player.settings` (internal) |
| `PlayerClientSettingsClientCache.java` | `player.settings` |
| `UpdatePlayerClientSettingC2SPayload.java` | `player.settings` |
| `PlayerClientSettingSyncS2CPayload.java` | `player.settings` |
| `IPlayerDataHelper.java` | `platform.services` |

#### `common` — modified
| File | Change |
|---|---|
| `PolyConfig.java` | + `radicalAccessibility`, + `configPanelKeybinds` map |
| `Services.java` | + `PLAYER_DATA` service |

#### `neoforge`
| File | Change |
|---|---|
| `NeoForgeClientEvents.java` | + accessibility inject, + config panel inject, + keybind tick |
| `NeoForgeNetworkHelper.java` | + 3 new payload registrations |
| `NeoForgePlayerDataHelper.java` | **New** — `IPlayerDataHelper` NeoForge impl |
| `NeoForgeServerEvents.java` | **New** — login/logout/respawn/tracking handlers |
| `PolyLibNeoForge.java` | + register types with `IPlayerDataHelper` |

#### `fabric`
| File | Change |
|---|---|
| `FabricClientEvents.java` | + accessibility inject, + keybind tick |
| `FabricServerEvents.java` | **New** — same lifecycle hooks via Fabric API events |
| `FabricPlayerDataHelper.java` | **New** — `IPlayerDataHelper` Fabric impl (SavedData) |
| `ModMenuCompat.java` | **New** — optional `ModMenuApi` impl |
| `fabric.mod.json` | + `modmenu` entrypoint, + `suggests: modmenu` |

---

### DisCraftHonored — changes

#### New files
| File | Purpose |
|---|---|
| `neoforge/.../client/AccessibilityOptionsSetup.java` | Registers all 6 toggles with `AccessibilityOptionsRegistry` |
| `neoforge/.../client/AppearancePanelRegistration.java` | Registers appearance screen with `ConfigPanelRegistry` |

#### Modified files
| File | Change |
|---|---|
| `DisCraftHonoredNeoForgeClient.java` | + call `AccessibilityOptionsSetup.register()` + `AppearancePanelRegistration.register()` |
| `ScreenEventHandler.java` | Delete accessibility block (~55 lines); delete appearance button injection block; class may become empty and be removed |
| `SaveMePower.java` | Replace `abilities.isBlinkRescueOn()` → `AccessibilityPrefsManager.getOrDefault(uuid, "discrafthonored.blink_rescue", true)` in `tryRescue` and `wouldRescue` |
| `DisCraftHonoredConfigScreen.java` | Replace `ClientPacketDistributor.sendToServer(new UpdateAppearanceC2SPayload(...))` → `PlayerClientSettingsManager.sendToServer(ModPlayerSettings.APPEARANCE, editConfig)` |
| `WorldRenderHandler.java` | `PlayerAppearanceCache.getFor(uuid)` → `PlayerClientSettingsClientCache.getFor(uuid, ModPlayerSettings.APPEARANCE)` |
| `VoidAegisRenderer.java` | Same cache swap |
| `AppearanceHandlers.java` | Gutted — `handleUpdateAppearance` and `handleAppearanceSync` removed (PolyLib handles both) |
| `PlayerSyncService.java` | Remove `broadcastAppearanceSync` and `syncAppearance` (PolyLib handles both) |
| `MovementHandlers.java` | Remove `handleUpdateRescuePrefs` |
| `NetworkRegistration.java` | Remove 3 registrations: `UpdateAppearanceC2SPayload`, `PlayerAppearanceSyncS2CPayload`, `UpdateRescuePrefsC2SPayload` |

#### New common file
| File | Purpose |
|---|---|
| `common/.../ModPlayerSettings.java` | Central constants file: `public static final PlayerClientSettingsType<PlayerAppearanceConfig> APPEARANCE = ...` |

#### Deleted files
| File | Replaced by |
|---|---|
| `UpdateAppearanceC2SPayload.java` | `UpdatePlayerClientSettingC2SPayload` (PolyLib) |
| `PlayerAppearanceSyncS2CPayload.java` | `PlayerClientSettingSyncS2CPayload` (PolyLib) |
| `UpdateRescuePrefsC2SPayload.java` | `AccessibilityPrefsC2SPayload` (PolyLib) |
| `client/PlayerAppearanceCache.java` | `PlayerClientSettingsClientCache` (PolyLib) |

#### Data migration (on player login)
In `DisCraftHonoredNeoForgeClient` or a `PlayerLifecycleListeners` login handler:
- If `PlayerClientSettingsManager.get(uuid, APPEARANCE)` equals defaults AND `PLAYER_APPEARANCE` attachment has non-default data → copy old attachment data into new system → clear old attachment
- One-time, self-healing, no player action required

---

### DisCraftHonored — accessibility registration example

```java
// AccessibilityOptionsSetup.java
public static void register() {

    // Gameplay-safety toggles — default policy (PLAYER_OVERRIDES_SERVER)
    AccessibilityOptionsRegistry.register("discrafthonored.rescue", target -> {
        target.addCategory(Component.translatable("discrafthonored.options.category.rescue"));
        target.addToggle(
            Component.literal("Blink Rescue"),
            () -> getLocalPrefs().isBlinkRescueOn(),
            val -> sendPref("discrafthonored.blink_rescue", val));
        target.addToggle(
            Component.literal("Far Reach Rescue"),
            () -> getLocalPrefs().isFarReachRescueOn(),
            val -> sendPref("discrafthonored.far_reach_rescue", val));
        target.addToggle(
            Component.literal("Void Grave"),
            () -> getLocalPrefs().isVoidGraveOn(),
            val -> sendPref("discrafthonored.void_grave", val));
    });

    // Pure client-side toggles — explicit opt-out
    AccessibilityOptionsRegistry.register("discrafthonored.input",
        AccessibilityPolicy.RESPECTS_SERVER_CONFIG, target -> {
            target.addCategory(Component.translatable("discrafthonored.options.category.input"));
            target.addToggle(
                Component.literal("Blink Toggle Mode"),
                ClientState.playerAbilities::isBlinkToggleMode,
                ClientState.playerAbilities::setBlinkToggleMode);
            target.addToggle(
                Component.literal("Void Gaze Toggle"),
                ClientState.playerAbilities::isVoidGazeToggleMode,
                ClientState.playerAbilities::setVoidGazeToggleMode);
        });
}
```

---

### DisCraftHonored — PlayerClientSettings registration example

```java
// ModPlayerSettings.java (common)
public final class ModPlayerSettings {
    private ModPlayerSettings() {}

    public static final PlayerClientSettingsType<PlayerAppearanceConfig> APPEARANCE =
        PlayerClientSettingsRegistry.register(
            "discrafthonored:appearance",
            PlayerAppearanceConfig.STREAM_CODEC,
            PlayerAppearanceConfig::defaults,
            BroadcastScope.TRACKING_RANGE,
            false  // copyOnDeath — cosmetics don't need to survive death
        );
}
```

Read on server:
```java
PlayerAppearanceConfig cfg = PlayerClientSettingsManager.get(player.getUUID(), ModPlayerSettings.APPEARANCE);
```

Read on client for rendering:
```java
PlayerAppearanceConfig cfg = PlayerClientSettingsClientCache.getFor(uuid, ModPlayerSettings.APPEARANCE);
```

Send from client:
```java
PlayerClientSettingsManager.sendToServer(ModPlayerSettings.APPEARANCE, editConfig);
```

---

## Implementation order

1. **PolyLib — accessibility package** (6 files + PolyConfig change)
2. **PolyLib — config panel package** (3 files + KeyboardShortcut + PolyConfig change)
3. **PolyLib — player settings package** (9 files)
4. **PolyLib — platform services** (IPlayerDataHelper + NeoForge impl + Fabric impl)
5. **PolyLib — loader wiring** (NeoForgeClientEvents, NeoForgeNetworkHelper, NeoForgeServerEvents, FabricClientEvents, FabricServerEvents, ModMenuCompat, fabric.mod.json)
6. **PolyLib — compile + test**
7. **DisCraftHonored — new files** (AccessibilityOptionsSetup, AppearancePanelRegistration, ModPlayerSettings)
8. **DisCraftHonored — migration** (all modified/deleted files)
9. **DisCraftHonored — compile + build**
10. **Deploy + runtime test**
