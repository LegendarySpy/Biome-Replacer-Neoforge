package net.werdei.biome_replacer.config;

import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Config {
    public static final String FILE_NAME = "biome_replacer.properties";
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Map<String, String> rules = new HashMap<>();

    public static void createIfAbsent() {
        File file = new File(FMLPaths.CONFIGDIR.get().toFile(), FILE_NAME);
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
            LOGGER.info("Default config file created at {}", file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create default config file", e);
        }
    }

    public static void reload() {
        createIfAbsent();
        File file = new File(FMLPaths.CONFIGDIR.get().toFile(), FILE_NAME);

        try (Scanner reader = new Scanner(file)) {
            rules.clear();
            while (reader.hasNextLine()) {
                var line = reader.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                var result = Arrays.stream(line.split(">"))
                        .map(String::trim)
                        .toArray(String[]::new);

                if (result.length == 2) {
                    rules.put(result[0], result[1]);
                    LOGGER.info("Rule loaded: {} -> {}", result[0], result[1]); // Add logging
                } else {
                    LOGGER.warn("Invalid rule format: '{}'. Each rule must be in the format 'old_biome > new_biome'.", line);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error reading config file at {}", file.getAbsolutePath(), e);
        }
    }
}
