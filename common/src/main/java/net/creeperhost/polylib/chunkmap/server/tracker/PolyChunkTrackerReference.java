package net.creeperhost.polylib.chunkmap.server.tracker;

/**
 * Internal interface applied to {@link net.minecraft.world.level.TicketStorage}
 * (and potentially other tracker-holding classes) by PolyLib mixins.
 *
 * <p>Allows {@link net.creeperhost.polylib.mixin.server.chunkmap.PolyDistanceManagerMixin}
 * to inject the tracker reference after {@code DistanceManager} construction
 * without casting to a concrete mixin class.
 *
 * <p>This interface intentionally lives <em>outside</em> the mixin package so
 * that non-mixin code can reference it freely (the Mixin framework forbids
 * direct cross-references to classes inside a declared mixin package).
 */
public interface PolyChunkTrackerReference
{
    void polylib$setTracker(PolyChunkTracker tracker);
    PolyChunkTracker polylib$getTracker();
}
