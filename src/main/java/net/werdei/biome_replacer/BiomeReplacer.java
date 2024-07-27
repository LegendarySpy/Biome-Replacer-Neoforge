package net.werdei.biome_replacer;

import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.werdei.biome_replacer.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod(BiomeReplacer.MODID)
@EventBusSubscriber(modid = BiomeReplacer.MODID, bus = EventBusSubscriber.Bus.GAME)
public class BiomeReplacer {
    public static final String MODID = "biome_replacer";

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_PREFIX = "[BiomeReplacer] ";

    private static Map<Holder<Biome>, Holder<Biome>> rules;

    public BiomeReplacer() {
        Config.createIfAbsent();
    }

    @SubscribeEvent
    public static void ServerAboutToStart(ServerAboutToStartEvent event) {
        log("ServerAboutToStartEvent triggered.");
        prepareReplacementRules(event.getServer().registries());
    }

    public static void prepareReplacementRules(LayeredRegistryAccess<RegistryLayer> registryAccess) {
        log("Preparing replacement rules.");
        rules = new HashMap<>();
        var registry = registryAccess.compositeAccess().registryOrThrow(Registries.BIOME);

        Config.reload();
        for (var rule : Config.rules.entrySet()) {
            log("Processing rule: " + rule.getKey() + " -> " + rule.getValue());
            var oldBiome = getBiomeHolder(rule.getKey(), registry);
            var newBiome = getBiomeHolder(rule.getValue(), registry);
            if (oldBiome != null && newBiome != null) {
                rules.put(oldBiome, newBiome);
                log("Rule added: " + rule.getKey() + " -> " + rule.getValue());
            } else {
                logWarn("Failed to add rule for: " + rule.getKey() + " -> " + rule.getValue());
            }
        }

        log("Loaded " + rules.size() + " biome replacement rules");
    }

    private static Holder<Biome> getBiomeHolder(String id, Registry<Biome> registry) {
        try {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(id);
            if (resourceLocation != null) {
                log("Attempting to fetch biome with ResourceLocation: " + resourceLocation);
                Biome biome = registry.get(resourceLocation);
                if (biome != null) {
                    log("Biome found: " + id);
                    var resourceKey = registry.getResourceKey(biome);
                    if (resourceKey.isPresent()) {
                        log("Resource key found for biome: " + id);
                        return registry.getHolderOrThrow(resourceKey.get());
                    }
                } else {
                    logWarn("Biome " + id + " not found in registry.");
                }
            } else {
                logWarn("Invalid ResourceLocation for biome ID: " + id);
            }
        } catch (Exception e) {
            logWarn("Error fetching biome holder for ID " + id + ": " + e.getMessage());
        }

        logWarn("Biome " + id + " not found. The rule will be ignored.");
        return null;
    }

    public static Holder<Biome> replaceIfNeeded(Holder<Biome> original) {
        var replacement = rules.get(original);
        if (replacement != null) {
            log("Replacing biome " + original + " with " + replacement);
        } else {
            log("No replacement found for biome " + original);
        }
        return replacement == null ? original : replacement;
    }

    public static boolean noReplacements() {
        return rules == null || rules.isEmpty();
    }

    public static void log(String message) {
        LOGGER.info(LOG_PREFIX + "{}", message);
    }

    public static void logWarn(String message) {
        LOGGER.warn(LOG_PREFIX + "{}", message);
    }
}
