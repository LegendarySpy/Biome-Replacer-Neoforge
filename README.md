# Biome Replacer

## Description

Biome Replacer is a small, server-side mod that allows you to replace one biome with another without affecting other aspects of world generation. This versatile tool is perfect for:

- Preventing specific datapack biomes from generating
- Ensuring modded biomes appear correctly
- Fine-tuning an almost-perfect seed

## Configuration

Setting up Biome Replacer is straightforward:

1. Locate the `biome-replacer.properties` file in your config folder
2. Add replacement rules using the following format:
   ```
   old_biome > new_biome
   ```

**Note**: This mod is primarily designed for replacing vanilla and datapack biomes. While biomes added by libraries like TerraBlender or Biolith cannot be directly replaced, Biome Replacer can be used alongside these libraries. You can replace vanilla or datapack biomes with modded ones.

## Examples

### Vanilla Biome Replacement

**Configuration:**
```
minecraft:dark_forest > minecraft:cherry_grove
```

**Result:**
![Dark forest is replaced by a cherry grove](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-1.png)

### Fixing Mod Compatibility

When using Terralith, the Lavender Plains biome from Aurora's Decorations may not generate. Here's how to fix it:

**Configuration:**
```
terralith:lavender_forest > aurorasdeco:lavender_plains
terralith:lavender_valley > aurorasdeco:lavender_plains
```

**Results:**
![Lavender Forest is replaced by Lavender Plains](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-2.png)
![Same area, but from a higher perspective](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-3.png)

### Removing Unwanted Biomes

To remove Terralith's Infested caves:

**Configuration:**
```
terralith:cave/infested_caves > minecraft:dripstone_caves
```

**Result:**
![Infested Caves are replaced by Dropstone Caves](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-4.png)

## Credits

- Original mod created by [WerDei](https://modrinth.com/user/WerDei)
- This version is a port of the original mod, made possible with WerDei's permission and support
