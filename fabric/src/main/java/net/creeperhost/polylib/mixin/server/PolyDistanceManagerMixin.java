package net.creeperhost.polylib.mixin.server.chunkmap;

import net.creeperhost.polylib.chunkmap.server.tracker.PolyChunkTracker;
import net.creeperhost.polylib.chunkmap.server.tracker.PolyChunkTrackerHolder;
import net.creeperhost.polylib.chunkmap.server.tracker.PolyChunkTrackerReference;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

/**
 * DistanceManager mixin: wires the {@link PolyChunkTracker} into the
 * {@link PolyTicketStorageMixin} so ticket events reach the tracker.
 */
@Mixin(DistanceManager.class)
public class PolyDistanceManagerMixin
{
    @Shadow @Final private TicketStorage ticketStorage;
    @Shadow @Final private net.minecraft.server.level.SimulationChunkTracker simulationChunkTracker;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void polylib$onInit(TicketStorage ticketStorage,
                                 Executor background,
                                 Executor main,
                                 CallbackInfo ci)
    {
        // DistanceManager is an inner class of ChunkMap; get level via ChunkMap
        if (this instanceof PolyChunkTrackerHolder holder) {
            PolyChunkTracker tracker = holder.polylib$getChunkTracker();
            ((PolyChunkTrackerReference) this.ticketStorage).polylib$setTracker(tracker);
        }
    }
}
