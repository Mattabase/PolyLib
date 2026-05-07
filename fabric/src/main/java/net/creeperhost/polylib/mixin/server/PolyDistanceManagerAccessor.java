package net.creeperhost.polylib.mixin.server.chunkmap;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.SimulationChunkTracker;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes private fields of {@link DistanceManager} needed by
 * {@link PolyChunkHolderMixin} to read current ticket and simulation levels.
 */
@Mixin(DistanceManager.class)
public interface PolyDistanceManagerAccessor
{
    @Accessor("ticketStorage")
    TicketStorage polylib$getTicketStorage();

    @Accessor("simulationChunkTracker")
    SimulationChunkTracker polylib$getSimulationTracker();
}
