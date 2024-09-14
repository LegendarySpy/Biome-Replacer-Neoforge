package net.legendaryspy.biome_replacer_neoforge.config;

import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_PREFIX = "[Biome_Replacer_Neoforge] ";
    private static final String FILE_NAME = "biome_replacer_neoforge.properties";
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

    public static Map<String, BiomeReplacement> rules = new HashMap<>();
    public static Map<String, List<BiomeReplacement>> tagRules = new HashMap<>();
    public static boolean muteChatInfo = false;

    public static void createIfAbsent() {
        if (Files.exists(FILE_PATH)) return;

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(FILE_PATH))) {
            writer.println("muteChatInfo = false");
            writer.println("# Mute chat info on player join (true/false, default: false)");
            writer.println("# Put your rules here in the format:");
            writer.println("# old_biome > new_biome,blend_range=5 (blend range is optional)");
            writer.println("# ");
            writer.println("# Examples (remove # in front of one to activate it):");
            writer.println("# minecraft:dark_forest > minecraft:cherry_grove,blend_range=5");
            writer.println("# terralith:lavender_forest > aurorasdeco:lavender_plains");
            writer.println("# terralith:cave/infested_caves > minecraft:dripstone_caves");
            writer.println("# ");
            writer.println("# for mass biome replacement use biome tags");
            writer.println("# #minecraft:is_forest > minecraft:desert,blend_range=10");
            writer.println("# #minecraft:is_mountain > minecraft:badlands");
        } catch (IOException e) {
            LOGGER.error(LOG_PREFIX + "Failed to create config file", e);
        }
    }

    public static void reload() {
        createIfAbsent();

        rules.clear();
        tagRules.clear();

        try {
            Files.lines(FILE_PATH)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(Config::parseLine);
        } catch (IOException e) {
            LOGGER.error(LOG_PREFIX + "Failed to read config file", e);
        }
    }

    private static void parseLine(String line) {
        if (line.contains("=")) {
            parseConfigOption(line);
        } else {
            parseBiomeRule(line);
        }
    }

    private static void parseConfigOption(String line) {
        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            String key = parts[0].trim();
            String value = parts[1].trim();

            if (key.equals("muteChatInfo")) {
                muteChatInfo = Boolean.parseBoolean(value);
            }
        }
    }

    private static void parseBiomeRule(String line) {
        String[] result = line.split(">", 2);
        if (result.length != 2) return;

        String oldBiome = result[0].trim();
        String newBiome = result[1].trim();

        int blendRange = extractBlendRange(newBiome);
        newBiome = newBiome.split(",")[0].trim();

        if (oldBiome.startsWith("#")) {
            String tagName = oldBiome.substring(1);
            tagRules.computeIfAbsent(tagName, k -> new ArrayList<>()).add(new BiomeReplacement(newBiome, blendRange));
        } else {
            rules.put(oldBiome, new BiomeReplacement(newBiome, blendRange));
        }
    }

    private static int extractBlendRange(String biomeString) {
        Pattern pattern = Pattern.compile(",blend_range=(\\d+)");
        Matcher matcher = pattern.matcher(biomeString);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                LOGGER.warn(LOG_PREFIX + "Invalid blend range format: " + biomeString);
            }
        }
        return 0; // Default blend range
    }

    public static class BiomeReplacement {
        public final String biomeId;
        public final int blendRange;

        public BiomeReplacement(String biomeId, int blendRange) {
            this.biomeId = biomeId;
            this.blendRange = blendRange;
        }
    }
}