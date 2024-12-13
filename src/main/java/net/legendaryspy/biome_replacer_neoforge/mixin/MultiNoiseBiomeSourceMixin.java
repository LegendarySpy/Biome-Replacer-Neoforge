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

@Mixin(value = MultiNoiseBiomeSource.class, priority = Integer.MIN_VALUE)
public abstract class MultiNoiseBiomeSourceMixin extends BiomeSource{

    @Unique
    private Climate.ParameterList<Holder<Biome>> modifiedParameters;

    @Inject(method = "parameters", at = @At("TAIL"), cancellable = true)
    private void onParametersReturn(CallbackInfoReturnable<Climate.ParameterList<Holder<Biome>>> cir) {
        if (modifiedParameters == null) {
            // Lazy-load the biome replacement rules if they haven't been prepared yet
            BiomeReplacerNeoforge.prepareRulesIfNeeded();
            findAndReplace(cir.getReturnValue());
        }
        cir.setReturnValue(modifiedParameters);
    }

    @Unique
    private void findAndReplace(Climate.ParameterList<Holder<Biome>> parameterList) {
        if (BiomeReplacerNeoforge.noReplacements()) {
            modifiedParameters = parameterList;
            BiomeReplacerNeoforge.log("No rules found, skipping replacements");
            return;
        }

        // Replace biomes in the parameter list based on the replacement rules
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> updatedParameterList = parameterList.values().stream()
                .map(entry -> new Pair<>(entry.getFirst(), BiomeReplacerNeoforge.replaceIfNeeded(entry.getSecond())))
                .collect(Collectors.toList());

        modifiedParameters = new Climate.ParameterList<>(updatedParameterList);
        BiomeReplacerNeoforge.log("Successfully applied biome replacements after all other mods");
    }
}