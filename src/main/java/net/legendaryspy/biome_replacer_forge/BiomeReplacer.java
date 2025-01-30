package net.legendaryspy.biome_replacer_forge;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.legendaryspy.biome_replacer_forge.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod(BiomeReplacer.MOD_ID)
public class BiomeReplacer {
    public static final String MOD_ID = "biome_replacer";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_PREFIX = "[BiomeReplacer] ";

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> directRules = new HashMap<>();
    private static final Map<TagKey<Biome>, List<ResourceKey<Biome>>> tagRules = new HashMap<>();
    private static Registry<Biome> biomeRegistry;
    private static final Random random = new Random();
    private static boolean rulesPrepared = false;

    public BiomeReplacer() {
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Create config if it doesn't exist
        Config.createIfAbsent();
    }

    private void setup(final FMLCommonSetupEvent event) {
        log("Initializing BiomeReplacer");
        loadConfig();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        log("Server starting - initializing biome registry...");
        try {
            biomeRegistry = event.getServer().registryAccess().registryOrThrow(Registries.BIOME);
            prepareReplacementRules();
        } catch (Exception e) {
            logError("Failed to initialize biome registry", e);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.muteChatInfo) {
            String message = "Biome Replacer is active with " + directRules.size() +
                    " direct replacement rules and " + tagRules.size() + " tag rules.";
            event.getEntity().sendSystemMessage(Component.literal(message));
            log("Sent startup message to player: " + event.getEntity().getName().getString());
        }
    }

    private void prepareReplacementRules() {
        if (!rulesPrepared) {
            directRules.clear();
            tagRules.clear();
            loadConfig();
            verifyBiomes();
            rulesPrepared = true;
        }
    }

    private static void loadConfig() {
        try {
            Config.reload();

            // Load direct replacement rules
            Config.rules.forEach((key, value) -> {
                ResourceKey<Biome> oldBiome = createBiomeKey(key);
                ResourceKey<Biome> newBiome = createBiomeKey(value);
                if (oldBiome != null && newBiome != null) {
                    directRules.put(oldBiome, newBiome);
                    log("Rule added: " + oldBiome + " -> " + newBiome);
                }
            });

            // Load tag-based replacement rules
            if (Config.tagRules != null) {
                Config.tagRules.forEach((tagName, replacements) -> {
                    try {
                        TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, new ResourceLocation(tagName));
                        List<ResourceKey<Biome>> replacementKeys = replacements.stream()
                                .map(BiomeReplacer::createBiomeKey)
                                .filter(Objects::nonNull)
                                .toList();
                        if (!replacementKeys.isEmpty()) {
                            tagRules.put(tagKey, replacementKeys);
                            log("Tag rule added: " + tagKey + " -> " + replacementKeys);
                        }
                    } catch (Exception e) {
                        logWarn("Invalid tag rule: " + tagName);
                    }
                });
            }

            log("Loaded " + directRules.size() + " direct biome replacement rules and " +
                    tagRules.size() + " tag rules");
        } catch (Exception e) {
            logError("Failed to load configuration", e);
        }
    }

    private static void verifyBiomes() {
        if (biomeRegistry == null) {
            logWarn("Cannot verify biomes - registry not initialized");
            return;
        }

        log("Verifying biome existence...");
        directRules.entrySet().removeIf(entry -> {
            boolean oldExists = biomeRegistry.containsKey(entry.getKey().location());
            boolean newExists = biomeRegistry.containsKey(entry.getValue().location());
            if (!oldExists || !newExists) {
                logWarn("Removing invalid rule: " + entry.getKey() + " -> " + entry.getValue());
                return true;
            }
            return false;
        });

        tagRules.entrySet().removeIf(entry -> {
            List<ResourceKey<Biome>> validReplacements = entry.getValue().stream()
                    .filter(key -> biomeRegistry.containsKey(key.location()))
                    .toList();
            if (validReplacements.isEmpty()) {
                logWarn("Removing invalid tag rule: " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    private static ResourceKey<Biome> createBiomeKey(String biomeId) {
        try {
            ResourceLocation location = new ResourceLocation(biomeId);
            return ResourceKey.create(Registries.BIOME, location);
        } catch (Exception e) {
            logWarn("Invalid biome ID: " + biomeId);
            return null;
        }
    }

    public static Holder<Biome> replaceIfNeeded(Holder<Biome> original) {
        if (biomeRegistry == null || original == null) {
            return original;
        }

        try {
            ResourceKey<Biome> originalKey = original.unwrapKey().orElse(null);
            if (originalKey == null) {
                return original;
            }

            // Check direct replacements first
            ResourceKey<Biome> directReplacement = directRules.get(originalKey);
            if (directReplacement != null) {
                return getBiomeHolder(directReplacement, original);
            }

            // Check tag replacements
            for (Map.Entry<TagKey<Biome>, List<ResourceKey<Biome>>> entry : tagRules.entrySet()) {
                if (original.is(entry.getKey())) {
                    List<ResourceKey<Biome>> possibleReplacements = entry.getValue();
                    if (!possibleReplacements.isEmpty()) {
                        ResourceKey<Biome> tagReplacement = possibleReplacements.get(
                                random.nextInt(possibleReplacements.size())
                        );
                        return getBiomeHolder(tagReplacement, original);
                    }
                }
            }

            return original;
        } catch (Exception e) {
            logError("Error during biome replacement", e);
            return original;
        }
    }

    private static Holder<Biome> getBiomeHolder(ResourceKey<Biome> replacementKey, Holder<Biome> fallback) {
        if (biomeRegistry == null) return fallback;

        return biomeRegistry.getHolder(replacementKey)
                .map(holder -> (Holder<Biome>) holder)
                .orElseGet(() -> {
                    logWarn("Failed to get holder for replacement biome: " + replacementKey.location());
                    return fallback;
                });
    }

    public static boolean noReplacements() {
        return directRules.isEmpty() && tagRules.isEmpty();
    }

    public static void log(String message) {
        LOGGER.info(LOG_PREFIX + message);
    }

    public static void logWarn(String message) {
        LOGGER.warn(LOG_PREFIX + message);
    }

    public static void logError(String message, Throwable t) {
        LOGGER.error(LOG_PREFIX + message, t);
    }
}