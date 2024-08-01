package net.legendaryspy.biome_replacer_neoforge.config;

import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Config {
    private static final String FILE_NAME = "biome_replacer_neoforge.properties";
    private static final Path FILE_PATH = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

    public static Map<String, String> rules = new HashMap<>();

    public static void createIfAbsent() {
        File file = FILE_PATH.toFile();
        if (file.exists()) return;

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("# Put your rules here in the format:");
            writer.println("# old_biome > new_biome");
            writer.println("# ");
            writer.println("# Examples (remove # in front of one to activate it):");
            writer.println("# minecraft:dark_forest > minecraft:cherry_grove");
            writer.println("# terralith:lavender_forest > aurorasdeco:lavender_plains");
            writer.println("# terralith:lavender_valley > aurorasdeco:lavender_plains");
            writer.println("# terralith:cave/infested_caves > minecraft:dripstone_caves");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file", e);
        }
    }

    public static void reload() {
        createIfAbsent();

        try (Scanner reader = new Scanner(FILE_PATH.toFile())) {
            rules.clear();
            while (reader.hasNextLine()) {
                String line = reader.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(">", 2);
                if (parts.length == 2) {
                    String oldBiome = parts[0].trim();
                    String newBiome = parts[1].trim();
                    rules.put(oldBiome, newBiome);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config file", e);
        }
    }
}
