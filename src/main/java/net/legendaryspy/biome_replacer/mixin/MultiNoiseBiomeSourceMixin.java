package net.legendaryspy.biome_replacer.mixin;

import com.mojang.datafixers.util.Pair;
import net.legendaryspy.biome_replacer.BiomeReplacer;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
    @Unique
    private static boolean initialized = false;

    @Unique
    private Climate.ParameterList<Holder<Biome>> modifiedParameters;

    @Inject(method = "parameters", at = @At("RETURN"), cancellable = true)
    private void onParametersReturn(CallbackInfoReturnable<Climate.ParameterList<Holder<Biome>>> cir) {
        if (!initialized) {
            BiomeReplacer.log("Initializing biome replacements for the first time");
            BiomeReplacer.prepareRulesIfNeeded();
            initialized = true;
        }

        if (modifiedParameters == null) {
            findAndReplace(cir.getReturnValue());
        }

        if (modifiedParameters != null) {
            cir.setReturnValue(modifiedParameters);
        }
    }

    @Unique
    private void findAndReplace(Climate.ParameterList<Holder<Biome>> parameterList) {
        if (BiomeReplacer.noReplacements()) {
            BiomeReplacer.log("No replacement rules found, using original parameters");
            modifiedParameters = parameterList;
            return;
        }

        try {
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> newParameterList = parameterList.values().stream()
                    .map(value -> {
                        Holder<Biome> original = value.getSecond();
                        Holder<Biome> replacement = BiomeReplacer.replaceIfNeeded(original);
                        if (replacement != original) {
                            BiomeReplacer.log("Replaced biome: " + original.unwrapKey().get().location() +
                                    " -> " + replacement.unwrapKey().get().location());
                        }
                        return new Pair<>(value.getFirst(), replacement);
                    })
                    .collect(Collectors.toList());

            modifiedParameters = new Climate.ParameterList<>(newParameterList);
            BiomeReplacer.log("Biome parameter list modified successfully");
        } catch (Exception e) {
            BiomeReplacer.logError("Error during biome replacement", e);
            modifiedParameters = parameterList;
        }
    }
}