# Weekly World Mod

## Overview

Weekly World Mod is a server-side Minecraft mod that creates custom challenges and objectives for players. It tracks individual player progress across different worlds and enforces configurable restrictions to create unique gameplay experiences.

## Features

- **Custom Objectives**: Set challenges like visiting dimensions, completing advancements, or collecting items
- **Flexible Restrictions**: Enforce hardcore mode, specific game modes, or difficulty levels
- **Per-Player Progress**: Each player's progress is tracked individually
- **Multi-World Support**: Progress is tracked separately for each world
- **Server-Only**: Only needs to be installed on the server - no client installation required
- **Automatic Tracking**: Objectives are automatically detected and completed as players play

## How It Works

The mod monitors player actions and automatically detects when objectives are completed. It uses several systems to track progress:

- **Dimension Travel**: Detects when players enter specific dimensions
- **Advancement Tracking**: Monitors advancement completions
- **Item Collection**: Tracks when players pick up specific items
- **Restriction Validation**: Continuously checks that world and player restrictions are met

## Getting Started

### Installation

1. Download the mod and place it in your server's `mods` folder
2. Start the server once to generate the default configuration
3. Configure your objectives and restrictions (see Configuration section)
4. Restart the server
5. Players can join and start completing objectives immediately

## Configuration

### Setting Up Objectives

Create or edit the `weekly_world_objectives.json` file in your server's `config` directory:

```json
{
  "tasks": [
    {
      "type": "dimension",
      "content": "minecraft:the_nether"
    },
    {
      "type": "dimension",
      "content": "minecraft:the_end"
    },
    {
      "type": "advancement",
      "content": "minecraft:story/mine_diamond"
    },
    {
      "type": "item",
      "content": "minecraft:diamond"
    }
  ],
  "restrictions": [
    {
      "type": "hardcore",
      "content": "true"
    },
    {
      "type": "gamemode",
      "content": "survival"
    }
  ]
}
```

### Supported Objective Types

- **`dimension`**: Player must enter a specific dimension
- **`advancement`**: Player must complete a specific advancement
- **`item`**: Player must pick up a specific item

### Supported Restriction Types

- **`hardcore`**: World must be in hardcore mode
- **`gamemode`**: Player must be in a specific game mode
- **`difficulty`**: World must be on a specific difficulty

## Data Storage

The mod creates several files in the `config` directory:

- **`weekly_world_objectives.json`**: Objective and restriction definitions
- **`weekly_world_player_data.json`**: Player progress data
- **`world_identifiers.json`**: World identifier mappings

## Player Experience

### Joining the Server

When players join a server with the mod installed, they will see:

1. **Welcome Message**: A greeting explaining the current challenge
2. **Current Restrictions**: List of world/player requirements with status indicators (☑/☐)
3. **Available Objectives**: List of tasks to complete with progress indicators (☑/☐)
4. **Helpful Warnings**: Notifications if restrictions aren't met or commands are enabled

### Completing Objectives

As players play normally, the mod automatically detects and records progress:

1. **Automatic Detection**: No special commands needed - just play the game
2. **Progress Notifications**: Players receive messages when objectives are completed
3. **Completion Celebration**: Special message when all objectives are finished
4. **Persistent Progress**: All progress is saved and persists across server restarts

### Restrictions and Rules

- **Operator Limitations**: Players with operator permissions (level 2+) cannot complete objectives
- **Restriction Enforcement**: All defined restrictions must be met for objectives to count
- **Fair Play**: Players are warned when restrictions aren't being followed

## Technical Details

### Data Storage

The mod automatically creates and manages several files in the `config` directory:

- **`weekly_world_objectives.json`**: Defines your custom objectives and restrictions
- **`weekly_world_player_data.json`**: Stores individual player progress data
- **`world_identifiers.json`**: Maps world names to unique identifiers for tracking

### System Requirements

- **Server-Side Only**: Install only on the server - clients don't need the mod
- **Minecraft Version**: Compatible with Fabric/Quilt mod loaders
- **Permissions**: Server must have write access to the config directory

### How It Works Internally

The mod uses several systems to track player progress:

- **Event Monitoring**: Listens for dimension changes, advancement completions, and item pickups
- **Data Persistence**: Automatically saves progress to JSON files
- **World Tracking**: Maintains separate progress for each world/dimension
- **Restriction Validation**: Continuously monitors world and player state

## Troubleshooting

### Common Issues

**"Could not find objectives file"**

- Solution: Create the `weekly_world_objectives.json` file in the config directory
- The mod will generate a default file if none exists

**Objectives not being completed**

- Check that all restrictions are met (hardcore mode, game mode, etc.)
- Verify the player doesn't have operator permissions (level 2+)
- Ensure the objective configuration is correct

**Progress not saving**

- Verify the server has write permissions to the config directory
- Check server logs for any error messages
- Make sure the server isn't running out of disk space

**Network errors (EncoderException: "Sending unknown packet")**

This error typically indicates a version compatibility issue:

- Verify your Minecraft version matches the mod's target version (1.21.7)
- Check that your Fabric Loader version is compatible (0.16.14+)
- Ensure your Fabric API version matches your Minecraft version
- Try updating to the latest versions of all components:
  - Minecraft Server
  - Fabric Loader
  - Fabric API
  - Yarn Mappings

If the error persists, check the [Fabric versions page](https://fabricmc.net/develop) for compatible version combinations.

### Debug Information

The mod provides detailed logging to help with troubleshooting:

- **Player Progress**: Logs when objectives are completed
- **Restriction Checks**: Reports when restrictions aren't met
- **File Operations**: Tracks data saving and loading
- **World Management**: Logs world cleanup and identifier operations

All debug information is written to the server console and log files.

## Contributing

### For Developers

The mod is built with a clean, modular architecture:

- **Data Management**: Handles player progress and challenge configuration
- **Event System**: Monitors game events and triggers objective completion
- **Mixins**: Integrates with Minecraft's core systems for seamless tracking
- **Persistence**: Manages JSON file storage with automatic cleanup

### Extending the Mod

The architecture makes it easy to add new features:

- **New Objective Types**: Add custom objective detection systems
- **Additional Restrictions**: Implement new restriction validation logic
- **Enhanced Events**: Monitor additional game events for tracking
- **Custom Storage**: Integrate with different data storage systems

### Code Structure

- **Data Layer**: `ServerPlayerData`, `ServerChallengeData`
- **Event Layer**: `ServerEventHandler`
- **Integration Layer**: Mixins for game event detection
- **Persistence Layer**: JSON file storage with cleanup

This separation makes the codebase maintainable and extensible for future enhancements.
