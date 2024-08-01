package net.werdei.biome_replacer.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.werdei.biome_replacer.BiomeReplacer;
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
    private Climate.ParameterList<Holder<Biome>> biomeReplacer$modifiedParameters;

    @Inject(method = "parameters", at = @At("RETURN"), cancellable = true)
    private void onParametersReturn(CallbackInfoReturnable<Climate.ParameterList<Holder<Biome>>> cir) {
        if (biomeReplacer$modifiedParameters == null) {
            biomeReplacer$findAndReplace(cir.getReturnValue());
        }
        cir.setReturnValue(biomeReplacer$modifiedParameters);
    }

    @Unique
    private void biomeReplacer$findAndReplace(Climate.ParameterList<Holder<Biome>> parameterList) {
        if (BiomeReplacer.noReplacements()) {
            biomeReplacer$modifiedParameters = parameterList;
            BiomeReplacer.log("No rules found, not replacing anything");
            return;
        }

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> newParameterList = parameterList.values().stream()
                .map(value -> new Pair<>(value.getFirst(), BiomeReplacer.replaceIfNeeded(value.getSecond())))
                .collect(Collectors.toList());

        biomeReplacer$modifiedParameters = new Climate.ParameterList<>(newParameterList);
        BiomeReplacer.log("Biomes replaced successfully");
    }
}
