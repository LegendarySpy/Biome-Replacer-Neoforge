package net.werdei.biome_replacer;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.werdei.biome_replacer.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod(BiomeReplacer.MODID)
public class BiomeReplacer {
    public static final String MODID = "biome_replacer";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_PREFIX = "[BiomeReplacer] ";

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> rules = new HashMap<>();
    private static Registry<Biome> biomeRegistry;

    public BiomeReplacer(IEventBus modEventBus) {
        Config.createIfAbsent();
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        log("FMLCommonSetupEvent triggered.");
        Config.reload();
        rules.clear();

        Config.rules.forEach((key, value) -> {
            ResourceKey<Biome> oldBiome = createBiomeKey(key);
            ResourceKey<Biome> newBiome = createBiomeKey(value);
            if (oldBiome != null && newBiome != null) {
                rules.put(oldBiome, newBiome);
                log("Rule added: " + oldBiome + " -> " + newBiome);
            } else {
                logWarn("Invalid biome key(s): " + key + " or " + value);
            }
        });

        log("Loaded " + rules.size() + " biome replacement rules");
    }

    private ResourceKey<Biome> createBiomeKey(String biomeId) {
        ResourceLocation location = ResourceLocation.tryParse(biomeId);
        if (location == null) {
            logWarn("Invalid biome ID: " + biomeId);
            return null;
        }
        return ResourceKey.create(Registries.BIOME, location);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        log("ServerAboutToStartEvent triggered. Verifying biome existence.");
        biomeRegistry = event.getServer().registryAccess().registryOrThrow(Registries.BIOME);

        rules.entrySet().removeIf(entry -> {
            boolean oldExists = biomeRegistry.containsKey(entry.getKey().location());
            boolean newExists = biomeRegistry.containsKey(entry.getValue().location());
            if (!oldExists || !newExists) {
                logWarn("Removing invalid rule: " + entry.getKey() + " -> " + entry.getValue() +
                        " (Old biome exists: " + oldExists + ", New biome exists: " + newExists + ")");
                return true;
            }
            return false;
        });

        log("Verified " + rules.size() + " valid biome replacement rules");

        // Debug: print all rules
        rules.forEach((key, value) -> log("Rule: " + key.location() + " -> " + value.location()));
    }

    public static Holder<Biome> replaceIfNeeded(Holder<Biome> original) {
        if (biomeRegistry == null) {
            logWarn("Biome registry is not initialized. Skipping replacement.");
            return original;
        }

        ResourceKey<Biome> originalKey = original.unwrapKey().orElse(null);
        if (originalKey == null) {
            log("Unable to unwrap ResourceKey for biome: " + original);
            return original;
        }

        log("Checking replacement for biome: " + originalKey.location());

        ResourceKey<Biome> replacementKey = rules.get(originalKey);
        if (replacementKey != null) {
            log("Found replacement: " + originalKey.location() + " -> " + replacementKey.location());
            return biomeRegistry.getHolder(replacementKey)
                    .flatMap(holder -> holder.unwrapKey().map(key -> (Holder<Biome>) holder)) // Ensure compatibility
                    .orElseGet(() -> {
                        logWarn("Failed to get holder for replacement biome: " + replacementKey.location());
                        return original;
                    });
        } else {
            log("No replacement found for biome: " + originalKey.location());
            return original;
        }
    }

    public static boolean noReplacements() {
        return rules.isEmpty();
    }

    public static void log(String message) {
        LOGGER.info(LOG_PREFIX + "{}", message);
    }

    public static void logWarn(String message) {
        LOGGER.warn(LOG_PREFIX + "{}", message);
    }
}
