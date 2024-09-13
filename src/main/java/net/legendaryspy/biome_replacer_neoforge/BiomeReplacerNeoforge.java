package net.legendaryspy.biome_replacer_neoforge;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.legendaryspy.biome_replacer_neoforge.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Mod(BiomeReplacerNeoforge.MODID)
public class BiomeReplacerNeoforge {
    public static final String MODID = "biome_replacer_neoforge";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_PREFIX = "[Biome_Replacer_Neoforge] ";

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> rules = new ConcurrentHashMap<>();
    private static final Map<TagKey<Biome>, List<ResourceKey<Biome>>> tagRules = new ConcurrentHashMap<>();
    private static Registry<Biome> biomeRegistry;
    private static final Random random = new Random();
    private static boolean rulesPrepared = false;

    public BiomeReplacerNeoforge(IEventBus modEventBus) {
        log("Initializing Biome-Replacer-Neoforge");
        Config.createIfAbsent();
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::onWorldLoad);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        log("FMLCommonSetupEvent triggered. Loading configuration...");
        loadConfig();
    }

    private static void loadConfig() {
        try {
            Config.reload();
            rules.clear();
            tagRules.clear();

            // Load direct replacement rules
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

            // Load tag-based replacement rules
            Config.tagRules.forEach((tagName, replacements) -> {
                TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, ResourceLocation.tryParse(tagName));
                List<ResourceKey<Biome>> replacementKeys = replacements.stream()
                        .map(BiomeReplacerNeoforge::createBiomeKey)
                        .filter(key -> key != null)
                        .toList();
                if (!replacementKeys.isEmpty()) {
                    tagRules.put(tagKey, replacementKeys);
                    log("Tag rule added: " + tagKey + " -> " + replacementKeys);
                } else {
                    logWarn("Invalid tag rule: " + tagName);
                }
            });

            log("Loaded " + rules.size() + " direct biome replacement rules and " + tagRules.size() + " tag rules");
        } catch (Exception e) {
            logError("Failed to load configuration", e);
        }
    }

    private static ResourceKey<Biome> createBiomeKey(String biomeId) {
        try {
            ResourceLocation location = ResourceLocation.tryParse(biomeId);
            if (location == null) {
                logWarn("Invalid biome ID: " + biomeId);
                return null;
            }
            return ResourceKey.create(Registries.BIOME, location);
        } catch (Exception e) {
            logError("Failed to create biome key for " + biomeId, e);
            return null;
        }
    }


    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        log("ServerAboutToStartEvent triggered. Initializing biome registry...");
        try {
            biomeRegistry = event.getServer().registryAccess().registryOrThrow(Registries.BIOME);
            verifyBiomes();
        } catch (Exception e) {
            logError("Failed to initialize biome registry", e);
        }
    }

    private void verifyBiomes() {
        log("Verifying biome existence...");
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

        tagRules.entrySet().removeIf(entry -> {
            List<ResourceKey<Biome>> validReplacements = entry.getValue().stream()
                    .filter(key -> biomeRegistry.containsKey(key.location()))
                    .toList();
            if (validReplacements.isEmpty()) {
                logWarn("Removing invalid tag rule: " + entry.getKey() + " (No valid replacement biomes)");
                return true;
            }
            entry.setValue(validReplacements);
            return false;
        });

        log("Verified " + rules.size() + " valid direct replacement rules and " + tagRules.size() + " valid tag rules");
        rules.forEach((key, value) -> log("Direct rule: " + key.location() + " -> " + value.location()));
        tagRules.forEach((key, value) -> log("Tag rule: " + key.location() + " -> " + value.stream().map(rk -> rk.location().toString()).toList()));
    }

    private void onWorldLoad(LevelEvent.Load event) {
        log("World loaded. Preparing for biome replacement...");
        prepareRulesIfNeeded();
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.muteChatInfo) { // Check if chat info is muted
            String message = "Biome Replacer is active with " + rules.size() + " direct replacement rules and " + tagRules.size() + " tag rules.";
            event.getEntity().sendSystemMessage(Component.literal(message));
            log("Sent startup message to player: " + event.getEntity().getName().getString());
        }
    }

    public static void prepareRulesIfNeeded() {
        if (!rulesPrepared) {
            loadConfig();
            rulesPrepared = true;
        }
    }

    public static Holder<Biome> replaceIfNeeded(Holder<Biome> original) {
        if (biomeRegistry == null) {
            logWarn("Biome registry is not initialized. Skipping replacement.");
            return original;
        }

        try {
            ResourceKey<Biome> originalKey = original.unwrapKey().orElse(null);
            if (originalKey == null) {
                log("Unable to unwrap ResourceKey for biome: " + original);
                return original;
            }

            log("Checking replacement for biome: " + originalKey.location());

            // Check direct replacements first
            ResourceKey<Biome> directReplacement = rules.get(originalKey);
            if (directReplacement != null) {
                return getBiomeHolder(directReplacement, original);
            }

            // Check tag replacements
            for (Map.Entry<TagKey<Biome>, List<ResourceKey<Biome>>> entry : tagRules.entrySet()) {
                if (original.is(entry.getKey())) {
                    List<ResourceKey<Biome>> possibleReplacements = entry.getValue();
                    if (!possibleReplacements.isEmpty()) {
                        ResourceKey<Biome> tagReplacement = possibleReplacements.get(random.nextInt(possibleReplacements.size()));
                        return getBiomeHolder(tagReplacement, original);
                    }
                }
            }

            log("No replacement found for biome: " + originalKey.location());
            return original;
        } catch (Exception e) {
            logError("Error during biome replacement", e);
            return original;
        }
    }

    private static Holder<Biome> getBiomeHolder(ResourceKey<Biome> replacementKey, Holder<Biome> fallback) {
        log("Applying replacement: " + fallback.unwrapKey().orElse(null) + " -> " + replacementKey.location());
        return biomeRegistry.getHolder(replacementKey)
                .map(holder -> (Holder<Biome>) holder)
                .orElseGet(() -> {
                    logWarn("Failed to get holder for replacement biome: " + replacementKey.location());
                    return fallback;
                });
    }

    public static boolean noReplacements() {
        return rules.isEmpty() && tagRules.isEmpty();
    }

    public static void log(String message) {
        LOGGER.info(LOG_PREFIX + "{}", message);
    }

    public static void logWarn(String message) {
        LOGGER.warn(LOG_PREFIX + "{}", message);
    }

    public static void logError(String message, Throwable t) {
        LOGGER.error(LOG_PREFIX + "{}", message, t);
    }
}
