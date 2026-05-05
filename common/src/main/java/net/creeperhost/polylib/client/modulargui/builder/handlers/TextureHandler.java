package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiTexture;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;
import net.creeperhost.polylib.client.modulargui.sprite.Material;
import net.creeperhost.polylib.client.modulargui.sprite.PolyTextures;
import net.minecraft.resources.Identifier;

/**
 * Handler for {@code "texture"} elements ({@link GuiTexture}).
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code poly_texture}   – name of a PolyLib named texture (via {@link PolyTextures})</li>
 *   <li>{@code texture}        – resource location string {@code "namespace:path"} for a raw texture</li>
 *   <li>{@code colour}         – ARGB tint (default {@code 0xFFFFFFFF} = no tint)</li>
 *   <li>{@code dynamic}        – boolean; enables 9-slice dynamic resizing</li>
 *   <li>{@code dynamic_border} – int border width for 9-slice mode (default 5)</li>
 * </ul>
 */
public class TextureHandler implements LayoutHandler<GuiTexture> {

    @Override
    public GuiTexture build(GuiParent<?> parent, JsonObject props,
                            HookResolver hooks, DataBindingResolver bindings) {
        GuiTexture texture = new GuiTexture(parent);

        if (props.has("poly_texture")) {
            texture.setMaterial(PolyTextures.getUncached(props.get("poly_texture").getAsString()));
        } else if (props.has("texture")) {
            texture.setMaterial(Material.fromRawTexture(parseIdentifier(props.get("texture").getAsString())));
        }

        if (props.has("colour")) {
            texture.setColour(ColorUtil.parse(props.get("colour").getAsString()));
        }

        boolean dynamic = props.has("dynamic") && props.get("dynamic").getAsBoolean();
        if (dynamic) {
            int border = props.has("dynamic_border") ? props.get("dynamic_border").getAsInt() : 5;
            texture.dynamicTexture(border);
        }

        return texture;
    }

    private static Identifier parseIdentifier(String s) {
        String[] parts = s.split(":", 2);
        return parts.length == 2
                ? Identifier.fromNamespaceAndPath(parts[0], parts[1])
                : Identifier.fromNamespaceAndPath("minecraft", parts[0]);
    }
}
