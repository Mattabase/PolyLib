package net.creeperhost.polylib.client.modulargui.builder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single element node in a {@link GuiLayout}.
 *
 * <p>JSON example:
 * <pre>{@code
 * {
 *   "id": "bg",
 *   "type": "rectangle",
 *   "fill": { "ref": "parent", "inset": 0 },
 *   "properties": { "fill_colour": "0xFF1A2030" }
 * }
 * }</pre>
 */
public class GuiLayoutElement {

    /** Unique ID within the layout. Used as a constraint reference target. */
    public String id;

    /** Element type key as registered in {@link GuiLayoutRegistry}. */
    public String type;

    /**
     * ID of the parent element, or {@code null} / {@code "root"} to attach to the GUI root.
     */
    @Nullable
    public String parent;

    /**
     * Shorthand: bind all four sides of this element to a reference element with an optional inset.
     * Equivalent to calling {@code Constraints.bind(element, refElement, inset)}.
     */
    @Nullable
    public FillSpec fill;

    /**
     * Per-{@link net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam} constraint
     * overrides. Keys are GeoParam names (LEFT, RIGHT, WIDTH, TOP, BOTTOM, HEIGHT).
     * Applied after the {@code fill} shorthand.
     */
    @Nullable
    public Map<String, ConstraintSpec> constraints;

    /** Type-specific properties passed verbatim to the {@link LayoutHandler}. */
    @Nullable
    public JsonObject properties;

    // ── FillSpec ──────────────────────────────────────────────────────────────

    /** Shorthand for a full-bind to a reference element with an optional inset. */
    public static class FillSpec {
        /** Element ID to bind to, or {@code "parent"} / {@code "root"}. */
        public final String refId;
        /** Pixel inset on all sides (default 0). */
        public final double inset;

        public FillSpec(String refId, double inset) {
            this.refId = refId;
            this.inset = inset;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("ref", refId);
            if (inset != 0) obj.addProperty("inset", inset);
            return obj;
        }

        public static FillSpec fromJson(JsonElement el) {
            if (el.isJsonPrimitive()) {
                // Shorthand: "fill": "parent"
                return new FillSpec(el.getAsString(), 0);
            }
            JsonObject obj = el.getAsJsonObject();
            return new FillSpec(
                    obj.get("ref").getAsString(),
                    obj.has("inset") ? obj.get("inset").getAsDouble() : 0
            );
        }
    }

    // ── JSON I/O ──────────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("type", type);
        if (parent != null && !parent.isEmpty()) obj.addProperty("parent", parent);
        if (fill != null) obj.add("fill", fill.toJson());
        if (constraints != null && !constraints.isEmpty()) {
            JsonObject cObj = new JsonObject();
            for (Map.Entry<String, ConstraintSpec> entry : constraints.entrySet()) {
                cObj.add(entry.getKey().toUpperCase(), entry.getValue().toJson());
            }
            obj.add("constraints", cObj);
        }
        if (properties != null && properties.size() > 0) {
            obj.add("properties", properties);
        }
        return obj;
    }

    public static GuiLayoutElement fromJson(JsonObject obj) {
        GuiLayoutElement el = new GuiLayoutElement();
        el.id = obj.get("id").getAsString();
        el.type = obj.get("type").getAsString();
        el.parent = obj.has("parent") ? obj.get("parent").getAsString() : null;
        if (obj.has("fill")) el.fill = FillSpec.fromJson(obj.get("fill"));
        if (obj.has("constraints")) {
            el.constraints = new LinkedHashMap<>();
            JsonObject cObj = obj.getAsJsonObject("constraints");
            for (Map.Entry<String, JsonElement> entry : cObj.entrySet()) {
                el.constraints.put(entry.getKey().toUpperCase(),
                        ConstraintSpec.fromJson(entry.getValue().getAsJsonObject()));
            }
        }
        if (obj.has("properties")) el.properties = obj.getAsJsonObject("properties");
        return el;
    }
}
