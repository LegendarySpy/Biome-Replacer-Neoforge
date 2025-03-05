[Fabric Version](https://modrinth.com/mod/biome-replacer)

## Description

Biome Replacer is a small, server-side mod that allows you to replace one biome with another without affecting other aspects of world generation. This versatile tool is perfect for:

- Preventing specific datapack biomes from generating
- Ensuring modded biomes appear correctly
- Fine-tuning an almost-perfect seed

## Key Features

- **Direct Biome Replacement**: Easily swap one biome for another using straightforward rules.
- **Biome Tag Replacement**: Replace entire groups of biomes at once using biome tags (e.g., `#minecraft:is_forest`).

## Mod Compaibility

Note that this mod is intended mostly for replacing vanilla and datapack biomes. Biomes added using libraries like TerraBlender or Biolith cannot be replaced; you should use the mod's config to tweak or remove them instead.
That said, this mod can safely be used alongside these libraries, and it's even possible to replace vanilla/datapack biomes with modded ones.
## Configuration

Setting up Biome Replacer is straightforward:

1. Locate the `biome_replacer_forge.properties` file in your config folder.
2. Add replacement rules using the following formats:
    - **Direct Replacement**:
      ```
      old_biome > new_biome
      ```
    - **Tag-Based Replacement**:
      ```
      #tag > new_biome
      ```
3. Optional: Disable chat notifications by setting:
```
muteChatInfo = true
```

## Examples

### Direct Biome Replacement

**Configuration:**
```
minecraft:dark_forest > minecraft:cherry_grove
```


**Result:**
![Dark forest is replaced by a cherry grove](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-1.png)

### Tag-Based Biome Replacement

Replace all biomes in the forest tag with deserts:

**Configuration:**
```
#minecraft:is_forest > minecraft:desert
```
[List of Biome tags](https://mcreator.net/wiki/minecraft-biome-tags-list)

**Result**: All forest-type biomes (e.g., Birch Forest, Dark Forest) are replaced by deserts.

### Fixing Mod Compatibility

When using Terralith, ensure Aurora's Decorations’ Lavender Plains generates correctly:

**Configuration:**
```
terralith:lavender_forest > aurorasdeco:lavender_plains 
terralith:lavender_valley > aurorasdeco:lavender_plains
```

**Results:**
![Lavender Forest is replaced by Lavender Plains](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-2.png)

### Removing Unwanted Biomes

To replace Terralith's Infested Caves with Dripstone Caves:

**Configuration:**
```
terralith:cave/infested_caves > minecraft:dripstone_caves
```

**Result:**
![Infested Caves are replaced by Dripstone Caves](https://raw.githubusercontent.com/WerDei/Biome-Replacer/master/readme-files/example-4.png)

## Credits

- Original mod created by [WerDei](https://modrinth.com/user/WerDei)
- This version is a port of the original mod, made possible with WerDei's permission and support
