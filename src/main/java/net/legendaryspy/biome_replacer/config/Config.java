package net.legendaryspy.biome_replacer.config;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Config {

    // Config file name and location
    private static final String FILE_NAME = "biome_replacer_forge.properties";
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

    // Biome replacement rules
    public static Map<String, String> rules = new HashMap<>(); // Direct biome replacements
    public static Map<String, List<String>> tagRules = new HashMap<>(); // Tag-based biome replacements
    public static boolean muteChatInfo = false; // Option to mute chat notifications

    /**
     * Creates a default config file if it doesn't exist.
     */
    public static void createIfAbsent() {
        File file = FILE_PATH.toFile();
        if (file.exists()) return; // No need to create if it already exists

        try {
            List<String> lines = List.of(
                    "muteChatInfo = false",
                    "! Mute chat info when a player joins (true/false, default: false)",
                    "! ",
                    "! Define biome replacement rules below:",
                    "! Syntax: old_biome > new_biome",
                    "! ",
                    "! Example rules (remove '!' to activate):",
                    "! minecraft:dark_forest > minecraft:cherry_grove",
                    "! terralith:lavender_forest > aurorasdeco:lavender_plains",
                    "! ",
                    "! For biome tags, use '#' as prefix:",
                    "! #minecraft:is_forest > minecraft:desert",
                    "! #minecraft:is_mountain > minecraft:badlands"
            );
            Files.write(FILE_PATH, lines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file", e);
        }
    }
    
    public static void reload() {
        createIfAbsent();

        try {
            List<String> lines = Files.readAllLines(FILE_PATH);
            rules.clear();
            tagRules.clear();

            for (String line : lines) {
                line = line.trim();

                // Skip comments (starting with '!') and empty lines
                if (line.isEmpty() || line.startsWith("!")) continue;

                // Handle configuration options (e.g., muteChatInfo)
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();

                        // Update muteChatInfo setting
                        if (key.equals("muteChatInfo")) {
                            muteChatInfo = Boolean.parseBoolean(value);
                        }
                    }
                    continue;
                }

                // Handle biome replacement rules (old_biome > new_biome)
                String[] result = line.split(">", 2);
                if (result.length == 2) {
                    String oldBiome = result[0].trim();
                    String newBiome = result[1].trim();

                    // Handle tag-based rules (e.g., #minecraft:is_forest)
                    if (oldBiome.startsWith("#")) {
                        String tagName = oldBiome.substring(1); // Remove '#' prefix
                        tagRules.computeIfAbsent(tagName, k -> new ArrayList<>()).add(newBiome);
                    } else {
                        // Add direct biome replacement rule
                        rules.put(oldBiome, newBiome);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config file", e);
        }
    }
}