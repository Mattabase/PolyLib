package net.creeperhost.polylib.chunkmap.server.tracker;

import net.creeperhost.polylib.chunkmap.common.data.PolyChunkMapData;

/**
 * Interface injected into {@code ServerLevel} to provide per-level chunk tracking.
 * Accessed by all server-side mixins via an interface cast.
 */
public interface PolyChunkTrackerHolder
{
    PolyChunkTracker polylib$getChunkTracker();
}
