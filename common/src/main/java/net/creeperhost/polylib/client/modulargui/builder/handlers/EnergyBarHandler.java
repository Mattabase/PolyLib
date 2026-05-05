package net.creeperhost.polylib.client.modulargui.builder.handlers;

import com.google.gson.JsonObject;
import net.creeperhost.polylib.client.modulargui.builder.DataBindingResolver;
import net.creeperhost.polylib.client.modulargui.builder.HookResolver;
import net.creeperhost.polylib.client.modulargui.builder.LayoutHandler;
import net.creeperhost.polylib.client.modulargui.elements.GuiEnergyBar;
import net.creeperhost.polylib.client.modulargui.lib.geometry.GuiParent;

import java.util.function.Supplier;

/**
 * Handler for {@code "energybar"} elements ({@link GuiEnergyBar}).
 *
 * <p>Supported properties:
 * <ul>
 *   <li>{@code energy_binding}   – binding name resolving to {@code Supplier<Long>}</li>
 *   <li>{@code capacity_binding} – binding name resolving to {@code Supplier<Long>}</li>
 *   <li>{@code energy}           – static long energy value (fallback if no binding)</li>
 *   <li>{@code capacity}         – static long capacity value (fallback if no binding)</li>
 * </ul>
 */
public class EnergyBarHandler implements LayoutHandler<GuiEnergyBar> {

    @Override
    public GuiEnergyBar build(GuiParent<?> parent, JsonObject props,
                              HookResolver hooks, DataBindingResolver bindings) {
        GuiEnergyBar bar = new GuiEnergyBar(parent);

        if (props.has("energy_binding")) {
            @SuppressWarnings("unchecked")
            Supplier<Long> energySupplier = (Supplier<Long>) bindings.resolve(
                    props.get("energy_binding").getAsString());
            if (energySupplier != null) {
                bar.setEnergy(energySupplier);
            }
        } else if (props.has("energy")) {
            bar.setEnergy(props.get("energy").getAsLong());
        }

        if (props.has("capacity_binding")) {
            @SuppressWarnings("unchecked")
            Supplier<Long> capacitySupplier = (Supplier<Long>) bindings.resolve(
                    props.get("capacity_binding").getAsString());
            if (capacitySupplier != null) {
                bar.setCapacity(capacitySupplier);
            }
        } else if (props.has("capacity")) {
            bar.setCapacity(props.get("capacity").getAsLong());
        }

        return bar;
    }
}
