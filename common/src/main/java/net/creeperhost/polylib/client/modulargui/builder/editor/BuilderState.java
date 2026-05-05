package net.creeperhost.polylib.client.modulargui.builder.editor;

import net.creeperhost.polylib.client.modulargui.builder.GuiLayout;
import net.creeperhost.polylib.client.modulargui.builder.GuiLayoutElement;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Shared mutable state for a single in-game GUI builder session.
 *
 * <p>All editor components ({@link BuilderCanvas}, {@link BuilderPalette},
 * {@link BuilderInspector}) hold a reference to the same {@code BuilderState} instance
 * so changes in one panel are immediately reflected elsewhere.
 */
public class BuilderState {

    /** The layout currently being edited. */
    public GuiLayout layout;

    /** ID of the currently selected element, or {@code null} if nothing is selected. */
    @Nullable
    public String selectedId;

    /** Where to save the layout on disk, or {@code null} for an unsaved new layout. */
    @Nullable
    public Path filePath;

    /** Whether there are unsaved changes. */
    public boolean dirty;

    /** Undo/redo history. */
    public final BuilderUndoStack undoStack = new BuilderUndoStack();

    /** Callback invoked whenever the layout is changed (element added/removed/modified). */
    @Nullable
    public Runnable onLayoutChanged;

    public BuilderState(GuiLayout layout, @Nullable Path filePath) {
        this.layout = layout;
        this.filePath = filePath;
        undoStack.push(layout);
    }

    // ── Mutation helpers ──────────────────────────────────────────────────────

    /**
     * Apply a change to the layout: updates state, marks dirty, pushes an undo snapshot,
     * and fires the {@link #onLayoutChanged} callback.
     */
    public void applyChange(Runnable change) {
        change.run();
        dirty = true;
        undoStack.push(layout);
        if (onLayoutChanged != null) onLayoutChanged.run();
    }

    /**
     * Replace the current layout (e.g. after undo/redo).
     * Does NOT push to undo stack — caller manages that.
     */
    public void restoreLayout(GuiLayout restored) {
        this.layout = restored;
        // Clear selection if the selected element no longer exists
        if (selectedId != null) {
            boolean found = layout.elements.stream().anyMatch(e -> selectedId.equals(e.id));
            if (!found) selectedId = null;
        }
        if (onLayoutChanged != null) onLayoutChanged.run();
    }

    public void undo() {
        GuiLayout restored = undoStack.undo();
        if (restored != null) {
            dirty = true;
            restoreLayout(restored);
        }
    }

    public void redo() {
        GuiLayout restored = undoStack.redo();
        if (restored != null) {
            dirty = true;
            restoreLayout(restored);
        }
    }

    /** @return the currently selected {@link GuiLayoutElement}, or {@code null}. */
    @Nullable
    public GuiLayoutElement selectedElement() {
        if (selectedId == null) return null;
        return layout.elements.stream()
                .filter(e -> selectedId.equals(e.id))
                .findFirst().orElse(null);
    }

    /**
     * Generate a unique element ID with the given prefix (e.g. "button_1", "button_2", …).
     */
    public String generateId(String prefix) {
        int n = 1;
        loop:
        while (true) {
            String candidate = prefix + "_" + n;
            for (GuiLayoutElement e : layout.elements) {
                if (candidate.equals(e.id)) { n++; continue loop; }
            }
            return candidate;
        }
    }
}
