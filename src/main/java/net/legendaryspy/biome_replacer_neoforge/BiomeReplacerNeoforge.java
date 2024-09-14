package net.legendaryspy.biome_replacer_neoforge;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.legendaryspy.biome_replacer_neoforge.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public BiomeReplacerNeoforge(IEventBus modEventBus) {
        log("Initializing Biome-Replacer-Neoforge");
        Config.createIfAbsent();
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        log("Loading configuration...");
        Config.reload();
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        log("Initializing biome registry...");
        biomeRegistry = event.getServer().registryAccess().registryOrThrow(Registries.BIOME);
        prepareRules();
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.muteChatInfo) {
            String message = "Biome Replacer is active with " + rules.size() + " direct replacement rules and " + tagRules.size() + " tag rules.";
            event.getEntity().sendSystemMessage(Component.literal(message));
            log("Sent startup message to player: " + event.getEntity().getName().getString());
        }
    }

    private void prepareRules() {
        log("Loading and verifying biome replacement rules...");

        rules.clear();
        tagRules.clear();

        Config.rules.forEach((key, value) -> {
            ResourceKey<Biome> oldBiome = createBiomeKey(key);
            ResourceKey<Biome> newBiome = createBiomeKey(value.biomeId);
            if (oldBiome != null && newBiome != null && biomeRegistry.containsKey(oldBiome.location()) && biomeRegistry.containsKey(newBiome.location())) {
                rules.put(oldBiome, newBiome);
                log("Rule added: " + oldBiome + " -> " + newBiome + " (Blend Range: " + value.blendRange + ")");
            } else {
                logWarn("Invalid or nonexistent biome key(s): " + key + " or " + value.biomeId);
            }
        });

        Config.tagRules.forEach((tagName, replacements) -> {
            ResourceLocation tagLocation = ResourceLocation.tryParse(tagName);
            if (tagLocation == null) {
                logWarn("Invalid tag name: " + tagName);
                return;
            }
            TagKey<Biome> tagKey = TagKey.create(Registries.BIOME, tagLocation);
            List<ResourceKey<Biome>> replacementKeys = replacements.stream()
                    .map(replacement -> createBiomeKey(replacement.biomeId))
                    .filter(Objects::nonNull)
                    .filter(key -> biomeRegistry.containsKey(key.location()))
                    .toList();
            if (!replacementKeys.isEmpty()) {
                tagRules.put(tagKey, replacementKeys);
                log("Tag rule added: " + tagKey + " -> " + replacementKeys);
            } else {
                logWarn("Invalid or nonexistent tag rule: " + tagName);
            }
        });

        log("Loaded " + rules.size() + " direct biome replacement rules and " + tagRules.size() + " tag rules");
    }

    public static Holder<Biome> getBiome(Holder<Biome> original, ChunkPos chunkPos, int x, int z) {
        if (biomeRegistry == null) {
            logWarn("Biome registry is not initialized. Skipping replacement.");
            return original;
        }

        ResourceKey<Biome> originalKey = original.unwrapKey().orElse(null);
        if (originalKey == null) {
            return original;
        }

        ResourceKey<Biome> directReplacement = rules.get(originalKey);
        if (directReplacement != null) {
            Config.BiomeReplacement replacement = Config.rules.get(originalKey.location().toString());
            if (replacement != null && shouldApplyReplacement(replacement.blendRange, chunkPos, x, z)) {
                return getBiomeHolder(directReplacement, original);
            }
        }

        for (Map.Entry<TagKey<Biome>, List<ResourceKey<Biome>>> entry : tagRules.entrySet()) {
            if (original.is(entry.getKey())) {
                List<ResourceKey<Biome>> possibleReplacements = entry.getValue();
                if (!possibleReplacements.isEmpty()) {
                    ResourceKey<Biome> tagReplacement = possibleReplacements.get(random.nextInt(possibleReplacements.size()));
                    return getBiomeHolder(tagReplacement, original);
                }
            }
        }

        return original;
    }

    private static boolean shouldApplyReplacement(int blendRange, ChunkPos chunkPos, int x, int z) {
        if (blendRange <= 0) {
            return true;
        }

        int centerX = chunkPos.getMinBlockX() + 8;
        int centerZ = chunkPos.getMinBlockZ() + 8;
        double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));
        return distance <= blendRange;
    }

    private static ResourceKey<Biome> createBiomeKey(String biomeId) {
        try {
            ResourceLocation location = ResourceLocation.tryParse(biomeId);
            if (location == null) {
                logWarn("Invalid biome ID format: " + biomeId);
                return null;
            }
            return ResourceKey.create(Registries.BIOME, location);
        } catch (Exception e) {
            LOGGER.error(LOG_PREFIX + "Failed to create biome key for " + biomeId, e);
            return null;
        }
    }

    private static Holder<Biome> getBiomeHolder(ResourceKey<Biome> replacementKey, Holder<Biome> fallback) {
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