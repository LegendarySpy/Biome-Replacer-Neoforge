# Biome Replacer

![Biome Replacer Banner](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-1.png)

> *Transform your Minecraft world, one biome at a time*
>
> [Fabric Version](https://modrinth.com/mod/biome-replacer)

## Overview

**Biome Replacer** is a lightweight, server-side mod that lets you precisely replace any biome with another without disrupting world generation mechanics. This preserves terrain features while swapping biome-specific characteristics like colors, mobs, and vegetation.

### Perfect For:

- ✅ Removing unwanted datapack biomes
- ✅ Fixing compatibility issues between mods
- ✅ Customizing that almost-perfect seed
- ✅ Creating unique world generation experiences

## Features

### 🔄 Direct Biome Replacement
Replace specific biomes with alternatives using simple, intuitive rules.

### 🏷️ Tag-Based Replacement
Transform entire categories of biomes at once using Minecraft's biome tag system.

### 🖥️ Server-Side Only
No need for clients to install anything - works 100% on the server side!

### ⚙️ Simple Configuration
Easy-to-edit properties file with straightforward syntax.

## Compatibility Notes

- **Works with**: Vanilla and datapack biomes, including Terralith and other world generation datapacks
- **Limited functionality with**: Biomes added via TerraBlender or Biolith (use their native configs instead)
- **Can be used alongside**: Any other biome or world generation mod

## Setting Up Biome Replacer

1. Install the mod on your server
2. Run the server once to generate the config file
3. Navigate to the `config` folder and locate `biome_replacer.properties`
4. Add your replacement rules using the formats below

### Configuration Format

#### Basic Replacement:
```
minecraft:dark_forest > minecraft:cherry_grove
```

#### Tag-Based Replacement:
```
#minecraft:is_forest > minecraft:desert
```

#### Optional Settings:
```
# Disable chat notifications
muteChatInfo = true
```

## Examples

### Example 1: Swapping Forest Types
**Config:**
```
minecraft:dark_forest > minecraft:cherry_grove
```

**Before & After:**
![Dark forest is replaced by a cherry grove](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-1.png)

### Example 2: Mass Biome Replacement
**Config:**
```
#minecraft:is_forest > minecraft:desert
```

**Result:** All forest biomes converted to desert landscapes!

### Example 3: Mod Compatibility
**Config:**
```
terralith:lavender_forest > aurorasdeco:lavender_plains 
terralith:lavender_valley > aurorasdeco:lavender_plains
```

**Before & After:**
![Lavender Forest is replaced by Lavender Plains](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-2.png)

### Example 4: Removing Problematic Biomes
**Config:**
```
terralith:cave/infested_caves > minecraft:dripstone_caves
```

**Before & After:**
![Infested Caves are replaced by Dripstone Caves](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-4.png)

## Resources

- [List of Minecraft Biome Tags](https://mcreator.net/wiki/minecraft-biome-tags-list)
- [Source Code on GitHub](https://github.com/WerDei/Biome-Replacer)
- [Report Issues](https://github.com/WerDei/Biome-Replacer/issues)

## Credits

- Original mod created by [WerDei](https://modrinth.com/user/WerDei)
    - This version ported with permission and support from them

---