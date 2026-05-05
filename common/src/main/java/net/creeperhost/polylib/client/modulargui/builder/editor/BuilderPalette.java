package net.creeperhost.polylib.client.modulargui.builder.editor;

import net.creeperhost.polylib.client.modulargui.builder.GuiLayoutElement;
import net.creeperhost.polylib.client.modulargui.builder.GuiLayoutRegistry;
import net.creeperhost.polylib.client.modulargui.elements.GuiElement;
import net.creeperhost.polylib.client.modulargui.elements.GuiList;
import net.creeperhost.polylib.client.modulargui.elements.GuiText;
import net.creeperhost.polylib.client.modulargui.lib.Constraints;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import static net.creeperhost.polylib.client.modulargui.lib.geometry.Constraint.*;
import static net.creeperhost.polylib.client.modulargui.lib.geometry.GeoParam.*;

/**
 * Left-side panel in the GUI builder.
 *
 * <p>Displays the list of registered element types. Clicking a type creates a new element
 * of that type at the centre of the preview and selects it.
 */
public class BuilderPalette extends GuiElement<BuilderPalette> {

    private final BuilderState state;
    private final GuiList<String> list;

    public BuilderPalette(@NotNull GuiParent<?> parent, BuilderState state) {
        super(parent);
        this.state = state;
        this.list = new GuiList<>(this);
        Constraints.bind(list, this);

        // Populate with all registered element types (sorted alphabetically)
        java.util.List<String> types = new java.util.ArrayList<>(GuiLayoutRegistry.types());
        java.util.Collections.sort(types);
        for (String type : types) {
            list.add(type);
        }

        // Custom display builder: clickable row per type
        list.setDisplayBuilder((listParent, typeName) -> {
            PaletteRow row = new PaletteRow(listParent, typeName);
            row.constrain(HEIGHT, literal(12));
            return row;
        });
    }

    // ── Inner clickable row ───────────────────────────────────────────────────

    private class PaletteRow extends GuiElement<PaletteRow>
            implements net.creeperhost.polylib.client.modulargui.lib.BackgroundRender {
        private final String typeName;
        private static final int NORMAL_BG  = 0x88222222;
        private static final int HOVER_BG   = 0x88444444;
        private static final int TEXT_COLOUR = 0xFFDDDDDD;

        PaletteRow(@NotNull GuiParent<?> parent, String typeName) {
            super(parent);
            this.typeName = typeName;
            GuiText label = new GuiText(this, () -> Component.literal(typeName));
            label.setTextColour(TEXT_COLOUR).setAlignment(net.creeperhost.polylib.client.modulargui.lib.geometry.Align.CENTER);
            Constraints.bind(label, this);
        }

        @Override
        public void renderBehind(net.creeperhost.polylib.client.modulargui.lib.GuiRender render,
                                  double mouseX, double mouseY, float partialTicks) {
            int colour = isMouseOver() ? HOVER_BG : NORMAL_BG;
            render.fill(xMin(), yMin(), xMax(), yMax(), colour);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !isMouseOver()) return false;
            addElement(typeName);
            return true;
        }
    }

    // ── Element creation ──────────────────────────────────────────────────────

    private void addElement(String typeName) {
        String id = state.generateId(typeName);
        GuiLayoutElement spec = new GuiLayoutElement();
        spec.id = id;
        spec.type = typeName;
        spec.parent = "root";

        // Place the new element at a reasonable default size in the preview
        spec.constraints = new java.util.LinkedHashMap<>();
        spec.constraints.put("left",   net.creeperhost.polylib.client.modulargui.builder.ConstraintSpec.relative("root", "LEFT", 20));
        spec.constraints.put("top",    net.creeperhost.polylib.client.modulargui.builder.ConstraintSpec.relative("root", "TOP", 20));
        spec.constraints.put("width",  net.creeperhost.polylib.client.modulargui.builder.ConstraintSpec.literal(60));
        spec.constraints.put("height", net.creeperhost.polylib.client.modulargui.builder.ConstraintSpec.literal(20));

        state.applyChange(() -> {
            state.layout.elements.add(spec);
            state.selectedId = id;
        });
    }
}
