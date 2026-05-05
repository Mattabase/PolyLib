package net.creeperhost.polylib.client.modulargui.builder.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.creeperhost.polylib.client.modulargui.ModularGui;
import net.creeperhost.polylib.client.modulargui.builder.GuiLayout;
import net.creeperhost.polylib.client.modulargui.builder.JsonGuiProvider;
import net.creeperhost.polylib.client.modulargui.elements.GuiButton;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.elements.GuiRectangle;
import net.creeperhost.polylib.client.modulargui.elements.GuiText;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.GuiProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint.*;
import static net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam.*;

/**
 * {@link GuiProvider} for the PolyGuiBuilder screen.
 *
 * <h3>Layout</h3>
 * <pre>
 * +--------------------------------------------------+
 * | Palette (120) | Canvas (flex)       | Inspector (240) |
 * |               |                     |                 |
 * +---------------+---------------------+-----------------+
 * | Save | Undo | Redo |           status                  |
 * +--------------------------------------------------+
 * </pre>
 */
public class BuilderScreen implements GuiProvider {

    private static final Logger LOGGER = LogManager.getLogger(BuilderScreen.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int PALETTE_WIDTH   = 120;
    private static final int INSPECTOR_WIDTH = 240;
    private static final int TOOLBAR_HEIGHT  = 20;
    private static final int PANEL_BG        = 0xFF222222;
    private static final int TOOLBAR_BG      = 0xFF1A1A1A;
    private static final int DIVIDER         = 0xFF444444;

    private final BuilderState state;
    private final JsonGuiProvider previewProvider;

    private BuilderCanvas canvas;
    private BuilderInspector inspector;
    private GuiText statusText;

    // ── Construction ──────────────────────────────────────────────────────────

    /** Create a builder screen with a fresh empty layout. */
    public BuilderScreen() {
        this(new BuilderState(new GuiLayout(), null));
    }

    public BuilderScreen(BuilderState state) {
        this.state = state;
        this.previewProvider = new JsonGuiProvider(state.layout);
    }

    @Override
    public GuiElement<?> createRootElement(ModularGui gui) {
        // Use a custom root element that intercepts Ctrl+Z/Y/S
        // Anonymous subclass needs raw type to satisfy GuiElement<T extends GuiElement<T>>
        @SuppressWarnings("rawtypes")
        GuiElement root = new GuiElement(gui) {
            @Override
            public boolean keyPressed(KeyEvent event) {
                boolean ctrl = Minecraft.getInstance().hasControlDown();
                if (ctrl && event.key() == GLFW.GLFW_KEY_Z) {
                    state.undo();
                    if (inspector != null) inspector.refresh();
                    return true;
                }
                if (ctrl && event.key() == GLFW.GLFW_KEY_Y) {
                    state.redo();
                    if (inspector != null) inspector.refresh();
                    return true;
                }
                if (ctrl && event.key() == GLFW.GLFW_KEY_S) {
                    save();
                    return true;
                }
                return false;
            }
        };
        //noinspection unchecked
        return root;
    }

    // ── GuiProvider ───────────────────────────────────────────────────────────

    @Override
    public void buildGui(ModularGui gui) {
        gui.initFullscreenGui();
        GuiElement<?> root = gui.getRoot();

        // ── Background ────────────────────────────────────────────────────────
        new GuiRectangle(root).fill(PANEL_BG);

        // ── Work area (everything above toolbar) ─────────────────────────────
        GuiElement<?> workArea = new GuiElement<>(root);
        workArea.constrain(LEFT,   literal(0));
        workArea.constrain(TOP,    literal(0));
        workArea.constrain(RIGHT,  dynamic(root::xMax));
        workArea.constrain(BOTTOM, dynamic(() -> root.yMax() - TOOLBAR_HEIGHT));

        // ── Palette (left) ────────────────────────────────────────────────────
        BuilderPalette palette = new BuilderPalette(workArea, state);
        palette.constrain(LEFT,   literal(0));
        palette.constrain(TOP,    literal(0));
        palette.constrain(WIDTH,  literal(PALETTE_WIDTH));
        palette.constrain(BOTTOM, dynamic(workArea::yMax));

        // Divider after palette
        new GuiRectangle(workArea)
                .fill(DIVIDER)
                .constrain(LEFT,   literal(PALETTE_WIDTH))
                .constrain(WIDTH,  literal(1))
                .constrain(TOP,    literal(0))
                .constrain(BOTTOM, dynamic(workArea::yMax));

        // ── Inspector (right) ─────────────────────────────────────────────────
        inspector = new BuilderInspector(workArea, state);
        inspector.constrain(RIGHT,  dynamic(workArea::xMax));
        inspector.constrain(TOP,    literal(0));
        inspector.constrain(WIDTH,  literal(INSPECTOR_WIDTH));
        inspector.constrain(BOTTOM, dynamic(workArea::yMax));

        // Divider before inspector
        new GuiRectangle(workArea)
                .fill(DIVIDER)
                .constrain(RIGHT,  dynamic(() -> workArea.xMax() - INSPECTOR_WIDTH))
                .constrain(WIDTH,  literal(1))
                .constrain(TOP,    literal(0))
                .constrain(BOTTOM, dynamic(workArea::yMax));

        // ── Canvas (centre) ───────────────────────────────────────────────────
        canvas = new BuilderCanvas(workArea, state, previewProvider);
        canvas.constrain(LEFT,   literal(PALETTE_WIDTH + 1));
        canvas.constrain(RIGHT,  dynamic(() -> workArea.xMax() - INSPECTOR_WIDTH - 1));
        canvas.constrain(TOP,    literal(0));
        canvas.constrain(BOTTOM, dynamic(workArea::yMax));

        canvas.setOnSelectionChanged(() -> inspector.refresh());

        // ── Toolbar ───────────────────────────────────────────────────────────
        GuiElement<?> toolbar = new GuiElement<>(root);
        toolbar.constrain(LEFT,   literal(0));
        toolbar.constrain(RIGHT,  dynamic(root::xMax));
        toolbar.constrain(BOTTOM, dynamic(root::yMax));
        toolbar.constrain(HEIGHT, literal(TOOLBAR_HEIGHT));
        new GuiRectangle(toolbar).fill(TOOLBAR_BG);

        double btnX = 4;

        // Save button
        GuiButton saveBtn = makeToolbarButton(toolbar, "Save", btnX);
        saveBtn.onClick(this::save);
        btnX += 44;

        // Undo button
        GuiButton undoBtn = makeToolbarButton(toolbar, "Undo", btnX);
        undoBtn.onClick(() -> {
            state.undo();
            inspector.refresh();
        });
        btnX += 44;

        // Redo button
        GuiButton redoBtn = makeToolbarButton(toolbar, "Redo", btnX);
        redoBtn.onClick(() -> {
            state.redo();
            inspector.refresh();
        });
        btnX += 44;

        // Status text
        final double statusLeft = btnX + 8;
        statusText = new GuiText(toolbar,
                () -> Component.literal(buildStatusText()));
        statusText.setTextColour(0xFF999999);
        statusText.constrain(LEFT,   literal(statusLeft));
        statusText.constrain(TOP,    relative(toolbar.get(TOP), 2));
        statusText.constrain(RIGHT,  dynamic(() -> toolbar.xMax() - 4));
        statusText.constrain(HEIGHT, literal(16));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void save() {
        if (state.filePath == null) {
            // No file path — output to game dir for now
            state.filePath = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("polylib_builder_output.json");
        }
        try {
            String json = GSON.toJson(state.layout);
            Files.writeString(state.filePath, json, StandardCharsets.UTF_8);
            state.dirty = false;
            LOGGER.info("BuilderScreen: saved layout to {}", state.filePath);
        } catch (IOException e) {
            LOGGER.error("BuilderScreen: failed to save layout to {}", state.filePath, e);
        }
    }

    private GuiButton makeToolbarButton(GuiElement<?> parent, String label, double xOffset) {
        GuiButton btn = new GuiButton(parent);
        btn.constrain(LEFT,   literal(xOffset));
        btn.constrain(TOP,    relative(parent.get(TOP), 2));
        btn.constrain(WIDTH,  literal(40));
        btn.constrain(HEIGHT, literal(16));
        new GuiRectangle(btn).rectangle(0xFF333333, 0xFF555555);
        GuiText lbl = new GuiText(btn, () -> Component.literal(label));
        lbl.setTextColour(0xFFDDDDDD).setAlignment(net.creeperhost.polylib.client.modulargui.lib.geometry.Align.CENTER);
        Constraints.bind(lbl, btn);
        btn.setLabel(lbl);
        return btn;
    }

    private String buildStatusText() {
        StringBuilder sb = new StringBuilder();
        if (state.filePath != null) {
            sb.append(state.filePath.getFileName());
        } else {
            sb.append("<unsaved>");
        }
        if (state.dirty) sb.append(" *");
        int count = state.layout.elements.size();
        sb.append("  |  Elements: ").append(count);
        if (state.selectedId != null) sb.append("  |  Selected: ").append(state.selectedId);
        return sb.toString();
    }
}
