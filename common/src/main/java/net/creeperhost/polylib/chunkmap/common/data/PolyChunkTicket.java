package net.creeperhost.polylib.chunkmap.common.data;

import net.minecraft.server.level.TicketType;

/**
 * Immutable snapshot of a single chunk ticket, transmitted from server → client.
 *
 * @param type        The ticket type (e.g. PLAYER, FORCED, PORTAL…)
 * @param ticketLevel The distance-manager level associated with this ticket
 * @param ticksLeft   Ticks remaining before the ticket expires; 0 = permanent
 */
public record PolyChunkTicket(TicketType type, int ticketLevel, long ticksLeft) {}
