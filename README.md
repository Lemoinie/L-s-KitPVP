# L-s-KitPVP

A highly customizable PVP plugin for Paper 1.21.1, featuring a unique room-based matchmaking system and modular kit configuration.

## 🚀 Features

- **Matchmaking Hub**: Custom GUI for browsing, joining, and creating PVP rooms.
- **Room Types**: Support for 1v1, 2v2, 4v4, and Chaos (up to 16 players) matches.
- **Modular Kit System**: 
    - Kits are stored in individual `.yml` files in the `kits/` directory.
    - Support for item-specific enchantments, customizable amounts, and icons.
    - **Potion Support**: Ability to specify potion types (SPEED, STRENGTH, etc.).
- **Room Management**: 
    - Creators can kick players and force-start matches.
    - Per-room settings (CombatLog, Block Breaking/Placing, Item Dropping, Pearl Usage).
- **Communication Control**: Toggle Chat and Commands on/off per room.
- **Anti-Exploit Protection**:
    - Blocks container interaction (Chests, Ender Chests, etc.) during matches.
    - Prevents kit items from being moved into non-player inventories.
    - Restores player inventories automatically after matches.

## 📜 Commands

- `/lpvp hub` - Opens the PVP Hub GUI to join or create rooms.
- `/lpvp room` - Opens the Room Management GUI (for room creators only).
- `/lpvp set spawn` - Sets the teleport location for PVP matches.
- `/lpvp reload` - Reloads the plugin configuration and kits.

## 🛠️ Configuration

### Global Settings (`config.yml`)
Configure default values for room settings and general plugin behavior.

### Kit Configuration (`kits/`)
Each kit is defined in its own file. Use `src/main/resources/kits/example_kit_structure.yml` as a template.

**Example Kit Item:**
```yaml
items:
  - material: DIAMOND_SWORD
    enchantments:
      sharpness: 5
  - material: POTION
    amount: 3
    potion-type: SPEED
```

## 🏗️ Building

The project uses Gradle. To build the plugin:
```bash
./gradlew build
```
The JAR will be located in `build/libs/`.

## 📄 License
This project is licensed under the Apache License 2.0.