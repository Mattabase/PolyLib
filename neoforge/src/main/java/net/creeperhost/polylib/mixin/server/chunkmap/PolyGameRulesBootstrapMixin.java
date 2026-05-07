package net.creeperhost.polylib.mixin.server.chunkmap;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import net.creeperhost.polylib.chunkmap.server.PolyChunkMapGamerule;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects the {@code polylib:chunkMapOpenToAll} gamerule into the vanilla
 * gamerule registry at bootstrap time, compatible with both NeoForge and Fabric.
 */
@Mixin(GameRules.class)
public abstract class PolyGameRulesBootstrapMixin
{
    @Inject(method = "bootstrap", at = @At("RETURN"))
    private static void polylib$registerChunkMapGamerule(
            Registry<GameRule<?>> registry,
            CallbackInfoReturnable<GameRule<?>> cir)
    {
        GameRule<Boolean> rule = Registry.register(
                registry,
                Identifier.fromNamespaceAndPath("polylib", "chunkmapopentoall"),
                new GameRule<>(
                        GameRuleCategory.MISC,
                        GameRuleType.BOOL,
                        BoolArgumentType.bool(),
                        /* visitorCaller */ (v, rule1) -> {},
                        Codec.BOOL,
                        /* toInt */ b -> b ? 1 : 0,
                        /* default */ false,
                        /* flags */ net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS
                )
        );
        PolyChunkMapGamerule.register(rule);
    }
}
