package net.creeperhost.polylib.client.modulargui.builder.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.creeperhost.polylib.client.modulargui.builder.GuiLayout;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple undo/redo stack for the GUI builder.
 *
 * <p>Snapshots are serialised to JSON so that each undo entry is a truly independent
 * deep copy of the layout.  The stack keeps at most {@value #MAX_DEPTH} entries.
 */
public class BuilderUndoStack {

    private static final int MAX_DEPTH = 50;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<String> snapshots = new ArrayList<>();
    /** Index of the snapshot that represents the current state. */
    private int cursor = -1;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Push a new snapshot of {@code layout} onto the stack.
     * Snapshots that were ahead of {@link #cursor} (i.e. redo history) are discarded.
     */
    public void push(GuiLayout layout) {
        // Discard redo history
        if (cursor < snapshots.size() - 1) {
            snapshots.subList(cursor + 1, snapshots.size()).clear();
        }
        // Evict oldest entry when at capacity
        if (snapshots.size() >= MAX_DEPTH) {
            snapshots.remove(0);
        } else {
            cursor++;
        }
        snapshots.add(GSON.toJson(layout));
    }

    /**
     * Undo the last change.
     *
     * @return the layout to restore, or {@code null} if already at the oldest snapshot
     */
    @Nullable
    public GuiLayout undo() {
        if (!canUndo()) return null;
        cursor--;
        return parse(snapshots.get(cursor));
    }

    /**
     * Redo a previously undone change.
     *
     * @return the layout to restore, or {@code null} if there is nothing to redo
     */
    @Nullable
    public GuiLayout redo() {
        if (!canRedo()) return null;
        cursor++;
        return parse(snapshots.get(cursor));
    }

    public boolean canUndo() {
        return cursor > 0;
    }

    public boolean canRedo() {
        return cursor < snapshots.size() - 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private static GuiLayout parse(String json) {
        try {
            return GSON.fromJson(json, GuiLayout.class);
        } catch (Exception e) {
            return null;
        }
    }
}
