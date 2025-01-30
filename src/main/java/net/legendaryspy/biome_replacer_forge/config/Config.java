package net.legendaryspy.biome_replacer_forge.config;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class Config {

    // Config file name and location
    private static final String FILE_NAME = "biome_replacer_neoforge.properties";
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

        try (PrintWriter writer = new PrintWriter(file)) {
            // Default settings and instructions for users
            writer.println("muteChatInfo = false");
            writer.println("! Mute chat info when a player joins (true/false, default: false)");
            writer.println("! ");
            writer.println("! Define biome replacement rules below:");
            writer.println("! Syntax: old_biome > new_biome");
            writer.println("! ");
            writer.println("! Example rules (remove '!' to activate):");
            writer.println("! minecraft:dark_forest > minecraft:cherry_grove");
            writer.println("! terralith:lavender_forest > aurorasdeco:lavender_plains");
            writer.println("! ");
            writer.println("! For biome tags, use '#' as prefix:");
            writer.println("! #minecraft:is_forest > minecraft:desert");
            writer.println("! #minecraft:is_mountain > minecraft:badlands");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file", e);
        }
    }

    /**
     * Loads and processes the config file.
     * Clears existing rules and reloads them from the file.
     */
    public static void reload() {
        createIfAbsent();
        File file = FILE_PATH.toFile();

        try (Scanner reader = new Scanner(file)) {
            rules.clear();
            tagRules.clear();

            while (reader.hasNextLine()) {
                String line = reader.nextLine().trim();

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