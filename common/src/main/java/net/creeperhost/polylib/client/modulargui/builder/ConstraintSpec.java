package net.creeperhost.polylib.client.modulargui.builder;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint;
import net.creeperhost.polylib.client.modulargui.lib.geometry.ConstrainedGeometry;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GeoRef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Serialisable specification for a single {@link Constraint} on one {@link GeoParam}.
 * <p>
 * At GUI-build time, {@link #resolve} is called with the live element map to produce a real
 * {@link Constraint} instance.
 *
 * <h3>Supported types</h3>
 * <ul>
 *   <li>{@code LITERAL}  – fixed pixel value</li>
 *   <li>{@code RELATIVE} – {@code ref.param + offset}</li>
 *   <li>{@code BETWEEN}  – fractional position between two GeoRefs (0.0–1.0)</li>
 *   <li>{@code MIDPOINT} – mid-point between two GeoRefs with an optional pixel offset</li>
 * </ul>
 *
 * <p>Full-element bind is handled at the {@link GuiLayoutElement} level via the {@code fill}
 * shorthand, not here.
 */
public class ConstraintSpec {

    private static final Logger LOGGER = LogManager.getLogger(ConstraintSpec.class);

    public enum Type { LITERAL, RELATIVE, BETWEEN, MIDPOINT }

    public final Type type;

    // LITERAL
    public final double value;

    // RELATIVE – ref.param + offset
    public final String refId;      // element ID, "parent", or "root"
    public final String refParam;   // GeoParam name (case-insensitive)
    public final double offset;

    // BETWEEN / MIDPOINT
    public final String startRefId;
    public final String startParam;
    public final String endRefId;
    public final String endParam;
    /**
     * For BETWEEN: fractional position in [0.0, 1.0].
     * For MIDPOINT: pixel offset from the mid-point.
     */
    public final double pos;

    private ConstraintSpec(Type type, double value,
                           String refId, String refParam, double offset,
                           String startRefId, String startParam,
                           String endRefId, String endParam,
                           double pos) {
        this.type = type;
        this.value = value;
        this.refId = refId;
        this.refParam = refParam;
        this.offset = offset;
        this.startRefId = startRefId;
        this.startParam = startParam;
        this.endRefId = endRefId;
        this.endParam = endParam;
        this.pos = pos;
    }

    // ── Factories ─────────────────────────────────────────────────────────────

    public static ConstraintSpec literal(double value) {
        return new ConstraintSpec(Type.LITERAL, value, null, null, 0, null, null, null, null, 0);
    }

    public static ConstraintSpec relative(String refId, String refParam, double offset) {
        return new ConstraintSpec(Type.RELATIVE, 0, refId, refParam, offset, null, null, null, null, 0);
    }

    public static ConstraintSpec between(String startRefId, String startParam,
                                         String endRefId, String endParam, double pos) {
        return new ConstraintSpec(Type.BETWEEN, 0, null, null, 0,
                startRefId, startParam, endRefId, endParam, pos);
    }

    /** {@code pos} is used as the pixel offset from the mid-point. */
    public static ConstraintSpec midPoint(String startRefId, String startParam,
                                          String endRefId, String endParam, double offset) {
        return new ConstraintSpec(Type.MIDPOINT, 0, null, null, 0,
                startRefId, startParam, endRefId, endParam, offset);
    }

    // ── Resolution ────────────────────────────────────────────────────────────

    /**
     * Resolves this spec into a live {@link Constraint}.
     *
     * @param param      the GeoParam this constraint will be applied to (informational)
     * @param elementMap map of element ID → ConstrainedGeometry (all elements built so far,
     *                   including the "root" key for the GUI root)
     * @param parent     the direct parent geometry of the element being built
     * @return the resolved constraint, or {@code null} if a required reference could not be found
     */
    @Nullable
    public Constraint resolve(GeoParam param,
                              Map<String, ConstrainedGeometry<?>> elementMap,
                              ConstrainedGeometry<?> parent) {
        return switch (type) {
            case LITERAL -> Constraint.literal(value);
            case RELATIVE -> {
                GeoRef ref = resolveRef(refId, refParam, elementMap, parent);
                if (ref == null) {
                    LOGGER.warn("ConstraintSpec: could not resolve ref '{}' param '{}' for RELATIVE", refId, refParam);
                    yield null;
                }
                yield Constraint.relative(ref, offset);
            }
            case BETWEEN -> {
                GeoRef start = resolveRef(startRefId, startParam, elementMap, parent);
                GeoRef end = resolveRef(endRefId, endParam, elementMap, parent);
                if (start == null || end == null) {
                    LOGGER.warn("ConstraintSpec: could not resolve refs for BETWEEN ({}.{} … {}.{})",
                            startRefId, startParam, endRefId, endParam);
                    yield null;
                }
                yield Constraint.between(start, end, pos);
            }
            case MIDPOINT -> {
                GeoRef start = resolveRef(startRefId, startParam, elementMap, parent);
                GeoRef end = resolveRef(endRefId, endParam, elementMap, parent);
                if (start == null || end == null) {
                    LOGGER.warn("ConstraintSpec: could not resolve refs for MIDPOINT ({}.{} … {}.{})",
                            startRefId, startParam, endRefId, endParam);
                    yield null;
                }
                // pos doubles as offset for MIDPOINT
                yield Constraint.midPoint(start, end, pos);
            }
        };
    }

    @Nullable
    private static GeoRef resolveRef(String refId, String paramName,
                                     Map<String, ConstrainedGeometry<?>> elementMap,
                                     ConstrainedGeometry<?> parent) {
        if (refId == null || paramName == null) return null;
        ConstrainedGeometry<?> target = "parent".equals(refId) ? parent : elementMap.get(refId);
        if (target == null) return null;
        try {
            GeoParam param = GeoParam.valueOf(paramName.toUpperCase());
            return target.get(param);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("ConstraintSpec: unknown GeoParam '{}'", paramName);
            return null;
        }
    }

    // ── JSON I/O ──────────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", type.name().toLowerCase());
        switch (type) {
            case LITERAL -> obj.addProperty("value", value);
            case RELATIVE -> {
                obj.addProperty("ref", refId);
                obj.addProperty("param", refParam);
                if (offset != 0) obj.addProperty("offset", offset);
            }
            case BETWEEN -> {
                obj.addProperty("start_ref", startRefId);
                obj.addProperty("start_param", startParam);
                obj.addProperty("end_ref", endRefId);
                obj.addProperty("end_param", endParam);
                obj.addProperty("pos", pos);
            }
            case MIDPOINT -> {
                obj.addProperty("start_ref", startRefId);
                obj.addProperty("start_param", startParam);
                obj.addProperty("end_ref", endRefId);
                obj.addProperty("end_param", endParam);
                if (pos != 0) obj.addProperty("offset", pos);
            }
        }
        return obj;
    }

    public static ConstraintSpec fromJson(JsonObject obj) {
        String typeStr = obj.get("type").getAsString().toUpperCase();
        Type type = Type.valueOf(typeStr);
        return switch (type) {
            case LITERAL -> literal(obj.get("value").getAsDouble());
            case RELATIVE -> relative(
                    obj.get("ref").getAsString(),
                    obj.get("param").getAsString(),
                    obj.has("offset") ? obj.get("offset").getAsDouble() : 0
            );
            case BETWEEN -> between(
                    obj.get("start_ref").getAsString(), obj.get("start_param").getAsString(),
                    obj.get("end_ref").getAsString(), obj.get("end_param").getAsString(),
                    obj.get("pos").getAsDouble()
            );
            case MIDPOINT -> midPoint(
                    obj.get("start_ref").getAsString(), obj.get("start_param").getAsString(),
                    obj.get("end_ref").getAsString(), obj.get("end_param").getAsString(),
                    obj.has("offset") ? obj.get("offset").getAsDouble() : 0
            );
        };
    }
}
