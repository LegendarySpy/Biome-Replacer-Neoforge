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

import java.util.ArrayList;
import java.util.List;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin extends BiomeSource
{
    @Unique
    private Climate.ParameterList<Holder<Biome>> biome_Replacer_forge$modifiedParameters = null;


    @Inject(method = "parameters", at = @At("RETURN"), cancellable = true)
    private void parameters(CallbackInfoReturnable<Climate.ParameterList<Holder<Biome>>> cir)
    {
        if (biome_Replacer_forge$modifiedParameters == null)
            biome_Replacer_forge$findAndReplace(cir.getReturnValue());
        cir.setReturnValue(biome_Replacer_forge$modifiedParameters);
    }

    @Unique
    private void biome_Replacer_forge$findAndReplace(Climate.ParameterList<Holder<Biome>> parameterList)
    {
        if (BiomeReplacer.noReplacements())
        {
            biome_Replacer_forge$modifiedParameters = parameterList;
            BiomeReplacer.log("No rules found, not replacing anything");
            return;
        }

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> newParameterList = new ArrayList<>();

        for (var value : parameterList.values())
            newParameterList.add(new Pair<>(
                    value.getFirst(),
                    BiomeReplacer.replaceIfNeeded(value.getSecond())
            ));

        biome_Replacer_forge$modifiedParameters = new Climate.ParameterList<>(newParameterList);
        BiomeReplacer.log("Biomes replaced successfully");
    }
}
