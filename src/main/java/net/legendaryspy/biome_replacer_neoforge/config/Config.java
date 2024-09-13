package net.legendaryspy.biome_replacer_neoforge.config;

import net.neoforged.fml.loading.FMLPaths;

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
    private static final String FILE_NAME = "biome_replacer_neoforge.properties";
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

    public static Map<String, String> rules = new HashMap<>();
    public static Map<String, List<String>> tagRules = new HashMap<>();
    public static boolean muteChatInfo = false; // New config option

    public static void createIfAbsent() {
        File file = FILE_PATH.toFile();
        if (file.exists()) return;

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("muteChatInfo = false");
            writer.println("# Mute chat info on player join (true/false, default: false)");
            writer.println("# Put your rules here in the format:");
            writer.println("# old_biome > new_biome");
            writer.println("# ");
            writer.println("# Examples (remove # in front of one to activate it):");
            writer.println("# minecraft:dark_forest > minecraft:cherry_grove");
            writer.println("# terralith:lavender_forest > aurorasdeco:lavender_plains");
            writer.println("# terralith:cave/infested_caves > minecraft:dripstone_caves");
            writer.println("# ");
            writer.println("# for mass biome replacement use biome tags");
            writer.println("# #minecraft:is_forest > minecraft:desert");
            writer.println("# #minecraft:is_mountain > minecraft:badlands");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file", e);
        }
    }

    public static void reload() {
        createIfAbsent();
        File file = FILE_PATH.toFile();

        try (Scanner reader = new Scanner(file)) {
            rules.clear();
            tagRules.clear();
            while (reader.hasNextLine()) {
                String line = reader.nextLine().trim();
                if (line.isEmpty() || line.startsWith("# ")) {
                    continue;
                }

                // Check for config options
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();

                        if (key.equals("muteChatInfo")) {
                            muteChatInfo = Boolean.parseBoolean(value);
                        }
                    }
                    continue;
                }

                // Handle biome replacement rules
                String[] result = line.split(">", 2);
                if (result.length == 2) {
                    String oldBiome = result[0].trim();
                    String newBiome = result[1].trim();

                    if (oldBiome.startsWith("#")) {
                        String tagName = oldBiome.substring(1);
                        tagRules.computeIfAbsent(tagName, k -> new ArrayList<>()).add(newBiome);
                    } else {
                        rules.put(oldBiome, newBiome);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config file", e);
        }
    }
}