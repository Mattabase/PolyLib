# PolyLib PR Plan

## Step 0 — Pre-work (do once)

### 0a. Wire the fork
```powershell
cd C:\Antigravity\PolyLib
git remote rename origin upstream
git remote add origin https://github.com/Mattabase/PolyLib.git
git push -u origin multi/26.1.2   # push WIP commit to your fork
```

### 0b. Delete AI plan files from the repo
These should never appear in any PR branch:
- `POLYLIB_CONFIG_PANEL_PLAN.md`
- `POLYLIB_EXTENSION_PLAN.md`

---

## Step 1 — Code Review & Cleaning

All new files must pass this checklist before going into any branch:

### 1a. Remove `──` section divider comments
These are the `// ── Foo ───────────` lines scattered through many files.
They are a clear AI fingerprint. Replace with a blank line or a plain `// --- Foo ---` at most.

**Files with dividers to remove:**
- `accessibility/AccessibilityOptionsRegistry.java`
- `accessibility/AccessibilityPrefsManager.java`
- `accessibility/OptionsListTarget.java`
- `client/config/ConfigPanelRegistry.java`
- `platform/services/IPlayerDataHelper.java`
- `player/serverdata/PlayerServerDataManager.java`
- `player/settings/PlayerClientSettingsManager.java`
- `fabric/platform/FabricPlayerDataHelper.java`
- `neoforge/config/NeoForgeConfigHelper.java`
- `neoforge/platform/NeoForgePlayerDataHelper.java`

### 1b. Trim over-documented Javadoc
JavaDoc that restates what the method signature already says should be removed or shortened.
Patterns to cut:
- "This method..." / "This class..." opening sentences
- `@param` entries that just echo the param name
- Long prose `<p>` blocks on trivial getters/setters
- `<h3>Example</h3>` blocks in `AccessibilityOptionsRegistry` (move to README or keep only in `AccessibilityRegistrationBuilder`)

**Worst offenders:**
- `AccessibilityOptionsRegistry.java` — builder overload Javadoc is very long
- `AccessibilityRegistrationBuilder.java` — each method has full prose; trim to one line
- `PlayerClientSettingsRegistry.java` — the 7-param overload doc is verbose
- `PolyRegistry.java` — `register(name, defaultEnglish, supplier)` doc repeats itself
- `PolyLangContributions.java` — good structure but several `<p>` blocks are redundant

### 1c. Remove/trim inline narrative comments
Single-line comments that explain the obvious or sound like a tutorial:
- `PlayerClientSettingsManager.java:47-48` — "We can't persist without..." — rephrase to concise note
- `PlayerClientSettingsManager.java:197` — "But we should still send to self on login" — OK to keep, just drop "But"
- `ModularGuiContainer.java:272` — commented-out block with narrative — already commented out, just clean

### 1d. Remove `// TODO` stubs that were not part of original upstream
`ModularGuiContainer.java` has several `//TODO` lines — check each one: keep only ones that existed in upstream, remove any newly added by the AI session.

### 1e. Check `PolyConfig.java` comment tone
The `@Comment` annotations go into JSON config files — they must sound like human-written config tooltips, not AI descriptions. Review:
- `radicalAccessibility` comment — currently reads like a feature spec, shorten it
- `configPanelKeybinds` comment — acceptable, minor trim

---

## Step 2 — Branch Creation

Each branch is created clean from upstream, then only its specific files are applied from the WIP commit using `git checkout <wip-sha> -- <files>`.

**WIP commit SHA** (the "wip: all local changes" commit on `multi/26.1.2`):
```powershell
git log --oneline -3  # grab the top SHA
```

### Branch creation pattern
```powershell
git checkout -b feat/<name> upstream/multi/26.1.2
git checkout <wip-sha> -- <file1> <file2> ...
# apply cleaning from Step 1 to only those files
git add -A
git commit -m "<message>"
git push origin feat/<name>
```

---

## Step 3 — Branches (in creation order)

### Branch 1: `feat/build-modernize`
**Commit message:** `build: bump fabric deps, add ModMenu compileOnly, fix shadow artifact publishing`

Files:
- `gradle.properties`
- `buildSrc/src/main/groovy/multiloader-common.gradle`
- `fabric/build.gradle`
- `neoforge/build.gradle`

No cleaning needed — no comments.

---

### Branch 2: `feat/inline-testmod`
**Commit message:** `test: inline testmod into main source sets, remove separate subprojects`

Files:
- `settings.gradle`
- `common/src/main/java/net/creeperhost/polylib/testmod/TestModCommon.java`
- `common/src/main/java/net/creeperhost/polylib/testmod/TestModClientCommon.java`
- `common/src/main/java/net/creeperhost/polylib/testmod/MainMenuGuiInjection.java`
- `fabric/src/main/java/net/creeperhost/polylib/testmod/TestModFabric.java`
- `neoforge/src/main/java/net/creeperhost/polylib/testmod/TestModNeoForge.java`
- Delete: `testmod-common/`, `testmod-fabric/`, `testmod-neoforge/` trees

```powershell
git rm -r testmod-common testmod-fabric testmod-neoforge
```

---

### Branch 3: `feat/modular-gui-fix`
**Commit message:** `fix: comment out extractSlot override incompatible with current MC version`

Files:
- `common/src/main/java/net/creeperhost/polylib/client/modulargui/ModularGuiContainer.java`

Clean: remove any newly added `//TODO` lines; keep only pre-existing ones.

---

### Branch 8: `feat/lang-datagen`  ← PR this before 4/5/6/7
**Commit message:** `feat: add PolyLangContributions and PolyLibLangProvider for automatic lang datagen`

Files:
- `common/src/main/java/net/creeperhost/polylib/data/lang/PolyLangContributions.java`
- `neoforge/src/main/java/net/creeperhost/polylib/datagen/PolyLibLangProvider.java`

Clean: trim verbose `<p>` Javadoc blocks in `PolyLangContributions`; keep the API summary at the top, drop redundant per-method prose.

---

### Branch 4: `feat/registry-abstraction`
**Commit message:** `feat: add IRegistryFactory/PolyRegistry service-loader abstraction for cross-loader registries`

Files:
- `common/src/main/java/net/creeperhost/polylib/registry/IRegistryFactory.java`
- `common/src/main/java/net/creeperhost/polylib/registry/PolyRegistry.java`
- `fabric/src/main/java/net/creeperhost/polylib/fabric/registry/FabricPolyRegistry.java`
- `fabric/src/main/java/net/creeperhost/polylib/fabric/registry/FabricRegistryFactory.java`
- `neoforge/src/main/java/net/creeperhost/polylib/neoforge/registry/NeoPolyRegistry.java`
- `neoforge/src/main/java/net/creeperhost/polylib/neoforge/registry/NeoRegistryFactory.java`
- `fabric/src/main/resources/META-INF/services/net.creeperhost.polylib.registry.IRegistryFactory`
- `neoforge/src/main/resources/META-INF/services/net.creeperhost.polylib.registry.IRegistryFactory`
- `common/src/main/java/net/creeperhost/polylib/platform/Services.java` — `REGISTRY` line only
- `common/src/main/java/net/creeperhost/polylib/testmod/TestModCommon.java` (PolyRegistry usage)

Clean: trim `PolyRegistry.register(name, defaultEnglish, supplier)` Javadoc; remove `langKeyPrefix` field comment.

---

### Branch 5: `feat/player-data`
**Commit message:** `feat: add PlayerClientSettings and PlayerServerData sync systems`

Files:
- `common/src/main/java/net/creeperhost/polylib/platform/services/IPlayerDataHelper.java`
- `common/src/main/java/net/creeperhost/polylib/player/settings/` — all 7 files
- `common/src/main/java/net/creeperhost/polylib/player/serverdata/` — all 6 files
- `fabric/src/main/java/net/creeperhost/polylib/platform/FabricPlayerDataHelper.java`
- `fabric/src/main/java/net/creeperhost/polylib/FabricServerEvents.java`
- `neoforge/src/main/java/net/creeperhost/polylib/platform/NeoForgePlayerDataHelper.java`
- `neoforge/src/main/java/net/creeperhost/polylib/NeoForgeEvents.java`
- `fabric/src/main/resources/META-INF/services/net.creeperhost.polylib.platform.services.IPlayerDataHelper`
- `neoforge/src/main/resources/META-INF/services/net.creeperhost.polylib.platform.services.IPlayerDataHelper`
- `common/src/main/java/net/creeperhost/polylib/platform/Services.java` — `PLAYER_DATA` line only
- `fabric/src/main/java/net/creeperhost/polylib/platform/FabricNetworkHelper.java` — player data payload registrations only
- `neoforge/src/main/java/net/creeperhost/polylib/platform/NeoForgeNetworkHelper.java` — player data payload registrations only

Clean:
- Remove all `// ── ...` / `// ─── ...` divider lines
- `PlayerClientSettingsManager`: rephrase the two inline notes at lines 47-48 and 197
- `IPlayerDataHelper`: remove divider comments, the section split is clear from method names
- Trim 7-param `PlayerClientSettingsRegistry.register` Javadoc to essentials

---

### Branch 6: `feat/accessibility`
**Commit message:** `feat: add AccessibilityOptionsRegistry with server policy and client override support`

Files:
- `common/src/main/java/net/creeperhost/polylib/accessibility/` — all files
- `fabric/src/main/java/net/creeperhost/polylib/platform/FabricNetworkHelper.java` — accessibility payload registration only
- `neoforge/src/main/java/net/creeperhost/polylib/platform/NeoForgeNetworkHelper.java` — accessibility payload registration only
- `common/src/main/java/net/creeperhost/polylib/config/PolyConfig.java` — `radicalAccessibility` field only

Clean:
- Remove `// ── ...` dividers from `AccessibilityOptionsRegistry`, `AccessibilityPrefsManager`, `OptionsListTarget`
- Trim builder-overload Javadoc in `AccessibilityOptionsRegistry` — keep the `@param` lines, cut the prose
- Trim `AccessibilityRegistrationBuilder` method docs to one-liners
- Shorten `PolyConfig.radicalAccessibility` comment to one sentence

---

### Branch 7: `feat/config-panel`
**Commit message:** `feat: add ConfigPanelRegistry with ModMenu integration and optional keybind shortcuts`

Files:
- `common/src/main/java/net/creeperhost/polylib/client/config/` — all 4 files
- `fabric/src/main/java/net/creeperhost/polylib/ModMenuCompat.java`
- `neoforge/src/main/java/net/creeperhost/polylib/config/NeoForgeConfigHelper.java`
- `neoforge/src/main/java/net/creeperhost/polylib/NeoForgeClientEvents.java`
- `fabric/src/main/java/net/creeperhost/polylib/PolyLibClientFabric.java` — keybind tick hook only
- `neoforge/src/main/java/net/creeperhost/polylib/PolyLibClientNeoForge.java` — keybind tick hook only
- `fabric/src/main/resources/fabric.mod.json` — ModMenu entrypoint entry
- `common/src/main/java/net/creeperhost/polylib/config/PolyConfig.java` — `configPanelKeybinds` field only

Clean:
- Remove all `// ── ...` dividers from `ConfigPanelRegistry` and `NeoForgeConfigHelper`
- Trim `ConfigPanelRegistry` lang-datagen overload Javadoc — one sentence each is enough
- `ModMenuCompat` class-level Javadoc is fine; trim to 2 sentences max

---

## Step 4 — PR Descriptions

Each PR should be opened from `Mattabase/PolyLib:<branch>` → `CreeperHost/PolyLib:multi/26.1.2`.

| PR | Branch | One-line description |
|----|--------|----------------------|
| 1 | `feat/build-modernize` | Bump Fabric deps, add ModMenu compileOnly, fix shadow artifact |
| 2 | `feat/inline-testmod` | Move testmod into main source sets, remove subprojects |
| 3 | `feat/modular-gui-fix` | Fix ModularGuiContainer slot render override for current MC |
| 4 | `feat/lang-datagen` | Add PolyLangContributions + PolyLibLangProvider for datagen |
| 5 | `feat/registry-abstraction` | Add IRegistryFactory/PolyRegistry cross-loader abstraction |
| 6 | `feat/player-data` | Add PlayerClientSettings + PlayerServerData sync systems |
| 7 | `feat/accessibility` | Add AccessibilityOptionsRegistry with server/player policy |
| 8 | `feat/config-panel` | Add ConfigPanelRegistry with ModMenu + keybind support |

Open in order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8.
Do not open a PR until the one it depends on is merged.

---

## Dependency Graph

```
1 (build-modernize) ──────────────────────────┐
2 (inline-testmod)  ──────────────────────────┤
3 (modular-gui-fix) ──────────────────────────┤
4 (lang-datagen)    ──────────────────────────┤
                                              ↓
5 (registry-abstraction) ← depends on 4 ─────┤
6 (player-data)          ← depends on 5 ─────┤
7 (accessibility)        ← depends on 6, 4 ──┤
8 (config-panel)         ← depends on 7, 1 ──┘
                                         all → CreeperHost/PolyLib:multi/26.1.2
```
