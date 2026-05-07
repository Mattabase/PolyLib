package net.creeperhost.polylib.mixin.server.chunkmap;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code level} field of {@link ChunkMap}, needed by
 * {@link PolyChunkHolderMixin} to retrieve the owning {@link ServerLevel}.
 */
@Mixin(ChunkMap.class)
public interface PolyChunkMapAccessor
{
    @Accessor("level")
    ServerLevel polylib$getLevel();
}
