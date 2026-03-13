# ❄️ Frozen

**Frozen** is a Minecraft mod (Fabric 1.21.11) that plunges the entire world into a ruthless ice age. Your main goal is to find warmth. Without heat sources, you will freeze to death.

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![Modloader](https://img.shields.io/badge/Modloader-Fabric-blue)

## 🥶 Core Mechanics

### 1. Global Temperature
The world is no longer a safe place. The game features a dynamic global temperature cycle. 
Depending on the current in-game day, the temperature smoothly changes ranging from **-20°C to -120°C**. The colder it is outside, the faster you freeze.

### 2. Freezing System
Forget about regular snow. Now freezing (the vanilla frost screen effect) begins at temperatures of **-10°C and below**.
* At -20°C, freezing happens slowly, giving you time to reach shelter.
* At -120°C, the cold strikes instantly, and without a strong heat source, you will die in a matter of seconds.

### 3. Heat Sources
To survive, you will have to build shelters and heat them. Different blocks emit varying amounts of heat over different distances. The mod uses a complex gradient system: **the closer you are to the source, the warmer it is**.

| Block | Max. Heating Radius | Note |
| :--- | :--- | :--- |
| 🌋 **Lava** | 16 blocks | The most powerful heat source. |
| 🧱 **Lit Furnace** | 12 blocks | A great option for heating a small house. |
| 🔥 **Campfire** | 8 blocks | A basic camp heater. Must be lit. |
| 🕯️ **Torch** | 4 blocks | Saves you only when nearby. |

### 4. Handheld Items
Some items can save your life on the go:
* A **Lava Bucket** in your hand will always keep you warm.
* A **Torch** (or Soul Torch) in your hand will warm you **ONLY** if the outside temperature is above **-50°C**. If extreme frosts hit, a handheld torch becomes useless.

### 5. Interactive Thermometers 🌡️
You can measure the temperature inside your builds! 
Place any sign and write `[temp]` on the first line. 
The mod will automatically turn this sign into a working thermometer that updates every 3 seconds and displays:
* **Local temperature** (in your 4x4x4 cube, taking into account all nearby campfires and furnaces).
* **Global temperature** (the weather outside).

## 📥 Installation

1. Install **[Fabric Loader](https://fabricmc.net/)** for version 1.21.11.
2. Download and install **[Fabric API](https://modrinth.com/mod/fabric-api)**.
3. Download the `frozen-1.x.x.jar` mod file from the [Releases](../../releases) section.
4. Place the `.jar` file into your game's `mods` folder.
5. Dress warmly and launch the game!