package net.legendaryspy.biome_replacer_neoforge.mixin;

import net.legendaryspy.biome_replacer_neoforge.BiomeReplacerNeoforge;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiNoiseBiomeSource.class)
public class MultiNoiseBiomeSourceMixin {
    @Inject(
            method = "getNoiseBiome(ILnet/minecraft/world/level/ChunkPos;II)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetNoiseBiome(int i, ChunkPos chunkPos, int x, int z, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!BiomeReplacerNeoforge.noReplacements()) {
            Holder<Biome> originalBiome = cir.getReturnValue();
            Holder<Biome> replacedBiome = BiomeReplacerNeoforge.getBiome(originalBiome, chunkPos, x, z);
            if (replacedBiome != originalBiome) {
                cir.setReturnValue(replacedBiome);
            }
        }
    }
}