package net.creeperhost.polylib.client.modulargui.builder.editor;

import net.creeperhost.polylib.client.modulargui.builder.ConstraintSpec;
import net.creeperhost.polylib.client.modulargui.builder.GuiLayoutElement;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.elements.GuiText;
import net.creeperhost.polylib.client.modulargui.elements.GuiTextField;
import net.creeperhost.polylib.client.modulargui.lib.BackgroundRender;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.GuiRender;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import static net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint.*;
import static net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam.*;

/**
 * Right-side panel in the GUI builder.
 *
 * <p>Shows editable properties for the currently selected element:
 * <ul>
 *   <li>Element ID (read-only label)</li>
 *   <li>Element type (read-only label)</li>
 *   <li>Per-{@link GeoParam} constraint text fields (Literal values only in Phase 3)</li>
 * </ul>
 *
 * <p>Changes are applied when Enter is pressed in any constraint field.
 * Each applied change pushes an undo snapshot via {@link BuilderState#applyChange}.
 */
public class BuilderInspector extends GuiElement<BuilderInspector> implements BackgroundRender {

    private static final Logger LOGGER = LogManager.getLogger(BuilderInspector.class);

    private static final int BG_COLOUR    = 0xFF1E1E1E;
    private static final int TEXT_COLOUR  = 0xFFDDDDDD;
    private static final int LABEL_COLOUR = 0xFF888888;
    private static final int ROW_HEIGHT   = 14;
    private static final int PADDING      = 4;

    private static final GeoParam[] GEO_PARAMS = {
            LEFT, TOP, RIGHT, BOTTOM, WIDTH, HEIGHT
    };

    private final BuilderState state;

    // Current selection ID for change detection
    @Nullable
    private String displayedId;

    // Dynamic content container (rebuilt on selection change)
    private GuiElement<?> content;

    // Field references for refresh
    private final List<GeoParamRow> paramRows = new ArrayList<>();

    public BuilderInspector(@NotNull GuiParent<?> parent, BuilderState state) {
        super(parent);
        this.state = state;
        content = new GuiElement<>(this);
        Constraints.bind(content, this);
        refresh();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Refresh the panel to reflect the current selection. */
    public void refresh() {
        // Rebuild content children
        new ArrayList<>(content.getChildren()).forEach(content::removeChild);
        paramRows.clear();
        displayedId = state.selectedId;

        GuiLayoutElement selected = state.selectedElement();
        if (selected == null) {
            GuiText noSel = new GuiText(content,
                    () -> Component.literal("No element selected")).setTextColour(LABEL_COLOUR);
            noSel.constrain(LEFT,   dynamic(() -> content.xMin() + PADDING));
            noSel.constrain(TOP,    literal(PADDING));
            noSel.constrain(WIDTH,  dynamic(() -> content.xSize() - PADDING * 2));
            noSel.constrain(HEIGHT, literal(ROW_HEIGHT));
            return;
        }

        double cursor = PADDING;

        // ID row
        GuiText idLabel = addLabel(content, "ID:", cursor);
        cursor += ROW_HEIGHT + 2;
        GuiText idValue = addLabel(content, selected.id, cursor);
        idValue.setTextColour(TEXT_COLOUR);
        cursor += ROW_HEIGHT + 4;

        // Type row
        GuiText typeLabel = addLabel(content, "Type:", cursor);
        cursor += ROW_HEIGHT + 2;
        GuiText typeValue = addLabel(content, selected.type, cursor);
        typeValue.setTextColour(TEXT_COLOUR);
        cursor += ROW_HEIGHT + 6;

        // Constraints
        for (GeoParam param : GEO_PARAMS) {
            String paramKey = param.name().toLowerCase(Locale.ROOT);
            ConstraintSpec existing = selected.constraints != null
                    ? selected.constraints.get(paramKey) : null;

            // Only show literal constraints in Phase 3; show blank otherwise
            String currentValue = (existing != null && existing.type == ConstraintSpec.Type.LITERAL)
                    ? String.valueOf(existing.value) : "";

            // Label
            addLabel(content, param.name() + ":", cursor);
            cursor += ROW_HEIGHT + 1;

            // Text field
            final double fieldTop = cursor;
            final GeoParam capturedParam = param;
            GuiTextField field = new GuiTextField(content);
            field.setValue(currentValue);
            field.constrain(LEFT,   dynamic(() -> content.xMin() + PADDING));
            field.constrain(TOP,    literal(fieldTop));
            field.constrain(WIDTH,  dynamic(() -> content.xSize() - PADDING * 2));
            field.constrain(HEIGHT, literal(ROW_HEIGHT));
            field.setEnterPressed(() -> applyConstraint(capturedParam, field.getValue()));
            field.setOnEditComplete(() -> applyConstraint(capturedParam, field.getValue()));

            paramRows.add(new GeoParamRow(param, field));
            cursor += ROW_HEIGHT + 3;
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void renderBehind(GuiRender render, double mouseX, double mouseY, float partialTicks) {
        render.fill(xMin(), yMin(), xMax(), yMax(), BG_COLOUR);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GuiText addLabel(GuiElement<?> parent, String text, double yOffset) {
        GuiText label = new GuiText(parent, () -> Component.literal(text));
        label.setTextColour(LABEL_COLOUR);
        label.constrain(LEFT,   dynamic(() -> parent.xMin() + PADDING));
        label.constrain(TOP,    literal(yOffset));
        label.constrain(WIDTH,  dynamic(() -> parent.xSize() - PADDING * 2));
        label.constrain(HEIGHT, literal(ROW_HEIGHT));
        return label;
    }

    private void applyConstraint(GeoParam param, String rawValue) {
        GuiLayoutElement selected = state.selectedElement();
        if (selected == null) return;

        double parsed;
        try {
            parsed = Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("BuilderInspector: invalid literal value '{}' for {}", rawValue, param);
            return;
        }

        final double finalValue = parsed;
        final String key = param.name().toLowerCase(Locale.ROOT);
        state.applyChange(() -> {
            if (selected.constraints == null) {
                selected.constraints = new LinkedHashMap<>();
            }
            selected.constraints.put(key, ConstraintSpec.literal(finalValue));
        });
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    private record GeoParamRow(GeoParam param, GuiTextField field) {}
}
