package net.creeperhost.polylib.mixin.server.chunkmap;

import net.creeperhost.polylib.chunkmap.server.tracker.PolyChunkTracker;
import net.creeperhost.polylib.chunkmap.server.tracker.PolyChunkTrackerHolder;
import net.creeperhost.polylib.chunkmap.server.tracker.PolyChunkTrackerReference;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * TicketStorage mixin: notifies the tracker whenever tickets are added or removed
 * so the client-side ticket colouring stays accurate.
 *
 * <p>This mixin also holds a reference to the {@link PolyChunkTracker} set by
 * {@link PolyDistanceManagerMixin} during {@code DistanceManager} init.
 */
@Mixin(TicketStorage.class)
public class PolyTicketStorageMixin implements PolyChunkTrackerReference
{
    @Unique private PolyChunkTracker polylib$tracker;

    @Shadow @Final
    @SuppressWarnings("rawtypes")
    private it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<List<Ticket>> tickets;

    // ── Ticket add ────────────────────────────────────────────────────────────

    @Inject(
            method = "addTicket(JLnet/minecraft/server/level/Ticket;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;iterator()Ljava/util/Iterator;"
            )
    )
    private void polylib$onAddTicket(long pos, Ticket ticket,
                                     CallbackInfoReturnable<Boolean> cir)
    {
        if (polylib$tracker != null) {
            polylib$tracker.setTickets(pos, tickets.getOrDefault(pos, List.of()));
        }
    }

    // ── Ticket remove ─────────────────────────────────────────────────────────

    @Inject(
            method = "removeTicket(JLnet/minecraft/server/level/Ticket;)Z",
            at = @At("RETURN")
    )
    private void polylib$onRemoveTicket(long pos, Ticket ticket,
                                         CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValue() && polylib$tracker != null) {
            polylib$tracker.setTickets(pos, tickets.getOrDefault(pos, List.of()));
        }
    }

    // ── PolyChunkTrackerReference ─────────────────────────────────────────────

    @Override
    public void polylib$setTracker(PolyChunkTracker tracker)
    {
        this.polylib$tracker = tracker;
    }

    @Override
    public PolyChunkTracker polylib$getTracker()
    {
        return this.polylib$tracker;
    }
}
