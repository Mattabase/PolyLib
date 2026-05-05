package net.creeperhost.polylib.client.modulargui.builder;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * Root document for a JSON-defined GUI layout.
 *
 * <p>JSON example:
 * <pre>{@code
 * {
 *   "format_version": 1,
 *   "type": "screen",
 *   "default_size": [200, 150],
 *   "elements": [ ... ],
 *   "hooks": {
 *     "onSave": { "description": "Called when the save button is clicked" }
 *   }
 * }
 * }</pre>
 */
public class GuiLayout {

    private static final Logger LOGGER = LogManager.getLogger(GuiLayout.class);

    public enum Type { SCREEN, HUD, INJECTION }

    public int formatVersion = 1;
    public Type type = Type.SCREEN;
    public int defaultWidth = 200;
    public int defaultHeight = 150;
    public List<GuiLayoutElement> elements = new ArrayList<>();
    /** Hook name → human-readable description (for tooling). */
    public Map<String, String> hooks = new LinkedHashMap<>();

    // ── Deserialization ───────────────────────────────────────────────────────

    public static GuiLayout fromJson(JsonObject obj) {
        GuiLayout layout = new GuiLayout();
        if (obj.has("format_version")) layout.formatVersion = obj.get("format_version").getAsInt();
        if (obj.has("type")) {
            try {
                layout.type = Type.valueOf(obj.get("type").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("GuiLayout: unknown type '{}', defaulting to SCREEN", obj.get("type").getAsString());
            }
        }
        if (obj.has("default_size")) {
            JsonArray size = obj.getAsJsonArray("default_size");
            layout.defaultWidth = size.get(0).getAsInt();
            layout.defaultHeight = size.get(1).getAsInt();
        }
        if (obj.has("elements")) {
            for (JsonElement el : obj.getAsJsonArray("elements")) {
                layout.elements.add(GuiLayoutElement.fromJson(el.getAsJsonObject()));
            }
        }
        if (obj.has("hooks")) {
            JsonObject hooksObj = obj.getAsJsonObject("hooks");
            for (Map.Entry<String, JsonElement> entry : hooksObj.entrySet()) {
                String desc = entry.getValue().isJsonObject()
                        ? (entry.getValue().getAsJsonObject().has("description")
                                ? entry.getValue().getAsJsonObject().get("description").getAsString()
                                : "")
                        : entry.getValue().getAsString();
                layout.hooks.put(entry.getKey(), desc);
            }
        }
        return layout;
    }

    public static GuiLayout fromReader(Reader reader) {
        return fromJson(JsonParser.parseReader(reader).getAsJsonObject());
    }

    public static GuiLayout fromString(String json) {
        return fromJson(JsonParser.parseString(json).getAsJsonObject());
    }

    @Nullable
    public static GuiLayout fromPath(Path path) {
        try (Reader reader = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8)) {
            return fromReader(reader);
        } catch (IOException e) {
            LOGGER.error("GuiLayout: failed to read layout from '{}'", path, e);
            return null;
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("format_version", formatVersion);
        obj.addProperty("type", type.name().toLowerCase());
        JsonArray sizeArr = new JsonArray();
        sizeArr.add(defaultWidth);
        sizeArr.add(defaultHeight);
        obj.add("default_size", sizeArr);
        JsonArray elementsArr = new JsonArray();
        for (GuiLayoutElement e : elements) elementsArr.add(e.toJson());
        obj.add("elements", elementsArr);
        if (!hooks.isEmpty()) {
            JsonObject hooksObj = new JsonObject();
            for (Map.Entry<String, String> entry : hooks.entrySet()) {
                JsonObject h = new JsonObject();
                h.addProperty("description", entry.getValue());
                hooksObj.add(entry.getKey(), h);
            }
            obj.add("hooks", hooksObj);
        }
        return obj;
    }

    public String toJsonString(boolean pretty) {
        Gson gson = pretty ? new GsonBuilder().setPrettyPrinting().create() : new Gson();
        return gson.toJson(toJson());
    }
}
