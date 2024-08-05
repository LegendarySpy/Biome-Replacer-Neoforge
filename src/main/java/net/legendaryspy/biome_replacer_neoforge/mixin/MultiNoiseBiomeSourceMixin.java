package net.legendaryspy.biome_replacer_neoforge.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.legendaryspy.biome_replacer_neoforge.BiomeReplacerNeoforge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin extends BiomeSource {

    @Unique
    private Climate.ParameterList<Holder<Biome>> modifiedParameters;

    @Inject(method = "parameters", at = @At("RETURN"), cancellable = true)
    private void onParametersReturn(CallbackInfoReturnable<Climate.ParameterList<Holder<Biome>>> cir) {
        if (modifiedParameters == null) {
            findAndReplace(cir.getReturnValue());
        }
        cir.setReturnValue(modifiedParameters);
    }

    @Unique
    private void findAndReplace(Climate.ParameterList<Holder<Biome>> parameterList) {
        if (BiomeReplacerNeoforge.noReplacements()) {
            modifiedParameters = parameterList;
            BiomeReplacerNeoforge.log("No rules found, not replacing anything");
            return;
        }

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> newParameterList = parameterList.values().stream()
                .map(value -> new Pair<>(value.getFirst(), BiomeReplacerNeoforge.replaceIfNeeded(value.getSecond())))
                .collect(Collectors.toList());

        modifiedParameters = new Climate.ParameterList<>(newParameterList);
        BiomeReplacerNeoforge.log("Biomes replaced successfully");
    }
}
