package net.creeperhost.polylib.client.modulargui.builder.editor;

import net.creeperhost.polylib.client.modulargui.builder.JsonGuiProvider;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.lib.BackgroundRender;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.ForegroundRender;
import net.creeperhost.polylib.client.modulargui.lib.GuiRender;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.creeperhost.polylib.client.modulargui.lib.geometry.Rectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint.*;
import static net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam.*;

/**
 * The central canvas panel in the GUI builder.
 *
 * <p>Renders a dark background, then a preview of the current layout inside a visible
 * border.  The selected element is highlighted in front of all preview children.
 *
 * <p>Click anywhere inside the preview to select an element (or deselect if nothing is hit).
 */
public class BuilderCanvas extends GuiElement<BuilderCanvas> implements BackgroundRender, ForegroundRender {

    private static final int BG_COLOUR       = 0xFF1A1A1A;
    private static final int PREVIEW_BG      = 0xFF2A2A2A;
    private static final int PREVIEW_BORDER  = 0xFF555555;
    private static final int SELECT_FILL     = 0x33FFFFFF;
    private static final int SELECT_BORDER   = 0xFFFFFF00;

    private final BuilderState state;
    private final JsonGuiProvider provider;
    /** Invisible element whose children are the live layout preview. */
    private final GuiElement<?> previewRoot;

    /** Callback to notify the inspector when selection changes. */
    @Nullable
    private Runnable onSelectionChanged;

    // ── Drag state ────────────────────────────────────────────────────────────
    private boolean dragging = false;
    private boolean dragPushedUndo = false;
    private double dragStartMouseX, dragStartMouseY;
    private double dragStartLeft, dragStartTop;

    public BuilderCanvas(@NotNull GuiParent<?> parent, BuilderState state, JsonGuiProvider provider) {
        super(parent);
        this.state = state;
        this.provider = provider;

        // Create the preview root as a child element filling this canvas.
        // We give it a fixed inner margin so there is a visible border around the preview.
        previewRoot = new GuiElement<>(this);
        double margin = 12;
        previewRoot.constrain(LEFT,   dynamic(() -> xMin() + margin));
        previewRoot.constrain(RIGHT,  dynamic(() -> xMax() - margin));
        previewRoot.constrain(TOP,    dynamic(() -> yMin() + margin));
        previewRoot.constrain(BOTTOM, dynamic(() -> yMax() - margin));

        rebuildPreview();

        // Register for layout-change notifications
        state.onLayoutChanged = this::rebuildPreview;
    }

    public BuilderCanvas setOnSelectionChanged(Runnable cb) {
        this.onSelectionChanged = cb;
        return this;
    }

    // ── Preview management ────────────────────────────────────────────────────

    /** Rebuild the preview children from the current state layout. */
    public void rebuildPreview() {
        // Sync provider layout with current state (may have changed via undo/redo)
        provider.setLayout(state.layout);
        // Remove all existing children from previewRoot
        new java.util.ArrayList<>(previewRoot.getChildren()).forEach(previewRoot::removeChild);
        // Build into the preview root
        provider.buildInto(previewRoot);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void renderBehind(GuiRender render, double mouseX, double mouseY, float partialTicks) {
        // Canvas background
        render.fill(xMin(), yMin(), xMax(), yMax(), BG_COLOUR);
        // Preview area
        render.borderFill(
                previewRoot.xMin(), previewRoot.yMin(),
                previewRoot.xMax(), previewRoot.yMax(),
                1, PREVIEW_BG, PREVIEW_BORDER);
    }

    @Override
    public void renderInFront(GuiRender render, double mouseX, double mouseY, float partialTicks) {
        if (state.selectedId == null) return;
        GuiElement<?> selected = provider.getBuiltElements().get(state.selectedId);
        if (selected == null) return;
        Rectangle r = selected.getRectangle();
        render.borderFill(r.x(), r.y(), r.xMax(), r.yMax(), 1, SELECT_FILL, SELECT_BORDER);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        // Hit-test against built elements (last element wins — topmost visual)
        @Nullable String hit = null;
        for (var entry : provider.getBuiltElements().entrySet()) {
            Rectangle r = entry.getValue().getRectangle();
            if (r.contains(mouseX, mouseY)) {
                hit = entry.getKey();
            }
        }
        boolean selectionChanged = !java.util.Objects.equals(state.selectedId, hit);
        state.selectedId = hit;
        if (selectionChanged && onSelectionChanged != null) onSelectionChanged.run();

        // Start drag if we hit an element
        if (hit != null) {
            GuiElement<?> hitElem = provider.getBuiltElements().get(hit);
            if (hitElem != null) {
                dragging = true;
                dragPushedUndo = false;
                dragStartMouseX = mouseX;
                dragStartMouseY = mouseY;
                dragStartLeft = hitElem.xMin() - previewRoot.xMin();
                dragStartTop  = hitElem.yMin() - previewRoot.yMin();
            }
            return true;
        }
        return selectionChanged;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dragging && state.selectedId != null) {
            net.creeperhost.polylib.client.modulargui.builder.GuiLayoutElement elem = state.selectedElement();
            if (elem != null) {
                // Push undo snapshot before first mutation
                if (!dragPushedUndo) {
                    state.undoStack.push(state.layout);
                    dragPushedUndo = true;
                }
                double newLeft = dragStartLeft + (mouseX - dragStartMouseX);
                double newTop  = dragStartTop  + (mouseY - dragStartMouseY);
                if (elem.constraints == null) elem.constraints = new java.util.LinkedHashMap<>();
                elem.constraints.put("left", net.creeperhost.polylib.client.modulargui.builder.ConstraintSpec.relative("root", "LEFT", newLeft));
                elem.constraints.put("top",  net.creeperhost.polylib.client.modulargui.builder.ConstraintSpec.relative("root", "TOP",  newTop));
                rebuildPreview();
                if (onSelectionChanged != null) onSelectionChanged.run();
            }
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            if (dragPushedUndo) {
                state.dirty = true;
            }
            return true;
        }
        return false;
    }
}
