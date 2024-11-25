package net.legendaryspy.biome_replacer;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.legendaryspy.biome_replacer.config.Config;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.Iterator;

@Mod(BiomeReplacer.MOD_ID)
public class BiomeReplacer {
    public static final String MOD_ID = "biome_replacer";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_PREFIX = "[BiomeReplacer] ";
    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> rules = new ConcurrentHashMap<>();
    private static final Map<TagKey<Biome>, List<ResourceKey<Biome>>> tagRules = new ConcurrentHashMap<>();
    private static Registry<Biome> biomeRegistry;
    private static final Random random = new Random();
    private static boolean rulesPrepared = false;

    public BiomeReplacer() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        log("Initializing BiomeReplacer");
        Config.createIfAbsent();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::onWorldLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
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
                TagKey<Biome> tagKey = TagKey.create(Registry.BIOME_REGISTRY, Objects.requireNonNull(ResourceLocation.tryParse(tagName)));
                List<ResourceKey<Biome>> replacementKeys = replacements.stream()
                        .map(BiomeReplacer::createBiomeKey)
                        .filter(Objects::nonNull)
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
            return ResourceKey.create(Registry.BIOME_REGISTRY, location);
        } catch (Exception e) {
            logError("Failed to create biome key for " + biomeId, e);
            return null;
        }
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        log("ServerAboutToStartEvent triggered. Initializing biome registry...");
        try {
            biomeRegistry = event.getServer().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
            verifyBiomes();
        } catch (Exception e) {
            logError("Failed to initialize biome registry", e);
        }
    }

    private void verifyBiomes() {
        log("Verifying biome existence...");
        Iterator<Map.Entry<ResourceKey<Biome>, ResourceKey<Biome>>> rulesIterator = rules.entrySet().iterator();
        while (rulesIterator.hasNext()) {
            Map.Entry<ResourceKey<Biome>, ResourceKey<Biome>> entry = rulesIterator.next();
            boolean oldExists = biomeRegistry.containsKey(entry.getKey().location());
            boolean newExists = biomeRegistry.containsKey(entry.getValue().location());
            if (!oldExists || !newExists) {
                logWarn("Removing invalid rule: " + entry.getKey() + " -> " + entry.getValue() + " (Old biome exists: " + oldExists + ", New biome exists: " + newExists + ")");
                rulesIterator.remove();
            }
        }
        Iterator<Map.Entry<TagKey<Biome>, List<ResourceKey<Biome>>>> tagRulesIterator = tagRules.entrySet().iterator();
        while (tagRulesIterator.hasNext()) {
            Map.Entry<TagKey<Biome>, List<ResourceKey<Biome>>> entry = tagRulesIterator.next();
            List<ResourceKey<Biome>> validReplacements = entry.getValue().stream()
                    .filter(key -> biomeRegistry.containsKey(key.location()))
                    .toList();
            if (validReplacements.isEmpty()) {
                logWarn("Removing invalid tag rule: " + entry.getKey() + " (No valid replacement biomes)");
                tagRulesIterator.remove();
            } else {
                entry.setValue(validReplacements);
            }
        }
        log("Verified " + rules.size() + " valid direct replacement rules and " + tagRules.size() + " valid tag rules");
        rules.forEach((key, value) -> log("Direct rule: " + key.location() + " -> " + value.location()));
        tagRules.forEach((key, value) -> log("Tag rule: " + key.location() + " -> " + value.stream().map(rk -> rk.location().toString()).toList()));
    }

    private void onWorldLoad(LevelEvent.Load event) {
        log("World loaded. Preparing for biome replacement...");
        prepareRulesIfNeeded();
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.muteChatInfo) {
            String message = "BiomeReplacer is active with " + rules.size() + " direct replacement rules and " + tagRules.size() + " tag rules.";
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