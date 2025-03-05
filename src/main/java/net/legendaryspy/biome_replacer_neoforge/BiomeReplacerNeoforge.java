package net.legendaryspy.biome_replacer_neoforge;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.legendaryspy.biome_replacer_neoforge.config.Config;
import net.legendaryspy.biome_replacer_neoforge.config.Config.BiomeReplacement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod(BiomeReplacerNeoforge.MODID)
public class BiomeReplacerNeoforge {
    public static final String MODID = "biome_replacer_neoforge";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_PREFIX = "[Biome_Replacer_Neoforge] ";

    // Changed from simple mapping to biome with probability info
    private static final Map<ResourceKey<Biome>, List<ReplacementEntry>> directRules = new HashMap<>();
    private static final Map<TagKey<Biome>, List<ReplacementEntry>> tagRules = new HashMap<>();
    private static Registry<Biome> biomeRegistry;
    private static Random seedBasedRandom;
    private static boolean rulesPrepared = false;
    private static long worldSeed = 0; // Default seed value

    // Class to store biome replacement with probability
    private static class ReplacementEntry {
        public final ResourceKey<Biome> targetBiome;
        public final double probability;

        public ReplacementEntry(ResourceKey<Biome> targetBiome, double probability) {
            this.targetBiome = targetBiome;
            this.probability = probability;
        }
    }

    public BiomeReplacerNeoforge(IEventBus modEventBus) {
        log("Initializing Biome-Replacer-Neoforge");
        // Register ourselves for server and other game events
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::setup);

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

            // Initialize rules without requiring the world seed yet
            seedBasedRandom = new Random(); // Use a temporary random until we get the real seed
            prepareReplacementRules();
        } catch (Exception e) {
            logError("Failed to initialize biome registry", e);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            // Now the overworld should be available
            if (event.getServer().overworld() != null) {
                worldSeed = event.getServer().overworld().getSeed();
                log("Using world seed for biome replacement: " + worldSeed);
                seedBasedRandom = new Random(worldSeed);
            } else {
                logWarn("Overworld still not available, using default random seed");
                // Use a fixed seed if we still can't get the world seed
                worldSeed = 12345L;
                seedBasedRandom = new Random(worldSeed);
            }
        } catch (Exception e) {
            logError("Failed to get world seed, using default", e);
            worldSeed = 12345L;
            seedBasedRandom = new Random(worldSeed);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.muteChatInfo) {
            int totalDirectRules = directRules.values().stream()
                    .mapToInt(List::size)
                    .sum();

            int totalTagRules = tagRules.values().stream()
                    .mapToInt(List::size)
                    .sum();

            String message = "Biome Replacer is active with " + totalDirectRules +
                    " direct replacement rules and " + totalTagRules + " tag rules.";
            event.getEntity().sendSystemMessage(Component.literal(message));
            log("Sent startup message to player: " + event.getEntity().getName().getString());
        }
    }

    // Added this public method to be called from the mixin
    public static synchronized void prepareRulesIfNeeded() {
        if (!rulesPrepared) {
            log("Preparing biome replacement rules from mixin call");
            directRules.clear();
            tagRules.clear();
            loadConfig();

            // Only verify biomes if registry is available
            if (biomeRegistry != null) {
                verifyBiomes();
            } else {
                log("Biome registry not yet available, skipping verification");
            }

            rulesPrepared = true;
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
                ResourceKey<Biome> newBiome = createBiomeKey(value.targetBiome);
                if (oldBiome != null && newBiome != null) {
                    directRules.computeIfAbsent(oldBiome, k -> new ArrayList<>())
                            .add(new ReplacementEntry(newBiome, value.probability));
                    log("Rule added: " + oldBiome + " -> " + newBiome + " (prob: " + value.probability + ")");
                }
            });

            // Load tag-based replacement rules
            if (Config.tagRules != null) {
                Config.tagRules.forEach((tagName, replacements) -> {
                    try {
                        // Using ResourceLocation.parse instead of constructor
                        ResourceLocation tagLocation = ResourceLocation.parse(tagName);
                        TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, tagLocation);

                        for (BiomeReplacement replacement : replacements) {
                            ResourceKey<Biome> replacementKey = createBiomeKey(replacement.targetBiome);
                            if (replacementKey != null) {
                                tagRules.computeIfAbsent(tagKey, k -> new ArrayList<>())
                                        .add(new ReplacementEntry(replacementKey, replacement.probability));
                                log("Tag rule added: " + tagKey + " -> " + replacementKey +
                                        " (prob: " + replacement.probability + ")");
                            }
                        }
                    } catch (Exception e) {
                        logWarn("Invalid tag rule: " + tagName);
                    }
                });
            }

            int totalDirectRules = directRules.values().stream().mapToInt(List::size).sum();
            int totalTagRules = tagRules.values().stream().mapToInt(List::size).sum();

            log("Loaded " + totalDirectRules + " direct biome replacement rules and " +
                    totalTagRules + " tag rules");
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

        // Verify direct rules
        directRules.entrySet().removeIf(entry -> {
            boolean sourceExists = biomeRegistry.containsKey(entry.getKey().location());
            if (!sourceExists) {
                logWarn("Removing invalid source biome: " + entry.getKey());
                return true;
            }

            // Filter invalid target biomes
            entry.getValue().removeIf(replacement -> {
                boolean targetExists = biomeRegistry.containsKey(replacement.targetBiome.location());
                if (!targetExists) {
                    logWarn("Removing invalid target biome: " + replacement.targetBiome);
                    return true;
                }
                return false;
            });

            return entry.getValue().isEmpty();
        });

        // Verify tag rules
        tagRules.entrySet().removeIf(entry -> {
            // Filter invalid target biomes
            entry.getValue().removeIf(replacement -> {
                boolean targetExists = biomeRegistry.containsKey(replacement.targetBiome.location());
                if (!targetExists) {
                    logWarn("Removing invalid target biome: " + replacement.targetBiome);
                    return true;
                }
                return false;
            });

            return entry.getValue().isEmpty();
        });
    }

    private static ResourceKey<Biome> createBiomeKey(String biomeId) {
        try {
            // Use ResourceLocation.parse instead of constructor
            ResourceLocation location = ResourceLocation.parse(biomeId);
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

            // Use biome location to create a deterministic hash
            String biomeId = originalKey.location().toString();
            int biomeHash = biomeId.hashCode();

            // Check direct replacements first
            List<ReplacementEntry> directReplacements = directRules.get(originalKey);
            if (directReplacements != null && !directReplacements.isEmpty()) {
                return handleSeedBasedReplacement(directReplacements, original, biomeHash);
            }

            // Check tag replacements
            for (Map.Entry<TagKey<Biome>, List<ReplacementEntry>> entry : tagRules.entrySet()) {
                if (original.is(entry.getKey())) {
                    List<ReplacementEntry> tagReplacements = entry.getValue();
                    if (!tagReplacements.isEmpty()) {
                        return handleSeedBasedReplacement(tagReplacements, original, biomeHash);
                    }
                }
            }

            return original;
        } catch (Exception e) {
            logError("Error during biome replacement", e);
            return original;
        }
    }

    private static Holder<Biome> handleSeedBasedReplacement(List<ReplacementEntry> replacements, Holder<Biome> original, int biomeHash) {
        // Create a new Random instance seeded with a combination of world seed and biome hash
        // This ensures the same biome is consistently replaced the same way for a given world seed
        long combinedSeed = worldSeed ^ biomeHash;
        Random localRandom = new Random(combinedSeed);

        // Roll for each replacement based on probability
        for (ReplacementEntry entry : replacements) {
            if (localRandom.nextDouble() <= entry.probability) {
                // This replacement was selected
                return getBiomeHolder(entry.targetBiome, original);
            }
        }

        // If no replacement was selected, keep the original
        return original;
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