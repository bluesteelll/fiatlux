# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Fiat Lux is a Minecraft mod for NeoForge 1.21.1 that introduces modular building systems, specifically the MechaGrid system. The mod allows players to place modular components within a 4x4x4 grid inside special blocks.

## Build System

### Common Commands
- `./gradlew build` - Build the mod
- `./gradlew runClient` - Launch Minecraft client for testing
- `./gradlew runServer` - Launch dedicated server for testing
- `./gradlew runGameTestServer` - Run automated game tests
- `./gradlew runData` - Generate data (recipes, loot tables, models)
- `./gradlew clean` - Clean build artifacts
- `./gradlew --refresh-dependencies` - Refresh cached dependencies

### Development Setup
- Java 21 is required (configured in build.gradle)
- Uses NeoForge 21.1.194 for Minecraft 1.21.1
- Parchment mappings for better parameter names
- Generated resources are in `src/generated/resources/`

## Architecture

### Core Module Structure
The mod follows a reorganized, logical structure with key packages:

**Client-side packages:**
- `art.boyko.fiatlux.client.gui` - All GUI components (screens, menus)
- `art.boyko.fiatlux.client.renderer` - Block entity renderers and client rendering

**Server-side packages:**
- `art.boyko.fiatlux.server` - Main mod class and server logic
- `art.boyko.fiatlux.server.init` - Registration of blocks, items, block entities, creative tabs
- `art.boyko.fiatlux.server.block` - Custom block implementations
- `art.boyko.fiatlux.server.blockentity` - Block entity implementations
- `art.boyko.fiatlux.server.datagen` - Data generation for assets and data
- `art.boyko.fiatlux.server.ecs` - ECS integration with Rust backend

**Common packages:**
- `art.boyko.fiatlux.common.module` - Module implementations and items
- `art.boyko.fiatlux.common.network` - Network handling for client-server communication

**Core architecture:**
- `art.boyko.fiatlux.mechamodule` - Complete MechaModule system framework

### MechaModule System
The central feature is a modular component system:

**Core Interfaces:**
- `IMechaModule` - Base interface for all modules
- `AbstractMechaModule` - Base implementation with common functionality
- `IModuleCapability` - System for module capabilities (energy, items, etc.)
- `IModuleContext` - Provides access to grid and world context

**Key Components:**
- `MechaGridBlock` - Main block that contains 4x4x4 module grid (in `server.block`)
- `MechaGridBlockEntity` - Stores and manages placed modules (in `server.blockentity`)
- `ModuleRegistry` - Registration system for module types (in `mechamodule.registry`)
- `MechaModuleItem` - Base class for item form of modules (in `mechamodule.base`)

**Module Types:** (in `common.module`)
- `TestModule` - Basic test module for development (in `common.module.test`)
- `EnergyGeneratorModule` - Produces energy
- `EnergyStorageModule` - Stores energy
- `ProcessorModule` - Processes items
- `DisplayModule` - Shows information
- Each module has a corresponding `*ModuleItem` class for the inventory item

### Key Features

**Ray Tracing System:**
- Complex 3D ray tracing for placing/removing modules within the grid
- Uses 3D DDA algorithm for accurate grid traversal
- Supports both placement (find empty spot) and removal (find occupied spot)

**Collision System:**
- Dynamic collision shapes based on placed modules
- Cached collision shapes for performance
- Light propagation through transparent areas

**Capability System:**
- Modules can provide capabilities (energy, item handling, etc.)
- Inter-module connections based on capability matching
- Extensible design for new capability types

## Data Generation

The mod uses Minecraft's data generation system (in `server.datagen`):
- `ModBlockStateProvider` - Block states and models
- `ModItemModelProvider` - Item models
- `ModLanguageProvider` - Translations
- `ModLootTableProvider` - Block loot tables
- `ModRecipeProvider` - Crafting recipes

Generated assets go to `src/generated/resources/` and are included in the build.

## ECS Integration

The mod includes a high-performance Rust ECS backend using Bevy ECS:

**Rust ECS (`rust-ecs/` directory):**
- Bevy ECS components for energy, storage, processing
- High-performance systems for batch operations
- JNI interface for Java integration

**Java ECS Integration (`server.ecs`):**
- `EcsManager` - Manages the Rust ECS world
- `BevyEcsWorld` - Java wrapper over Rust ECS
- `RustEnergyBridge` - Bridge for energy system integration
- `EcsIntegratedModule` - Modules that can use ECS backend

This allows modules to choose between Java logic (simpler) or Rust ECS (high-performance) implementations.

### Performance Priority and Rust ECS Migration
**CRITICAL: High performance is the top priority for this project.** When implementing any logic:

1. **Always prefer Rust ECS over Java implementations** when possible
2. **Migrate existing Java logic to Rust ECS** whenever feasible
3. **Use Java only for**:
   - Minecraft-specific integrations that require Java APIs
   - Simple UI logic
   - Registration and initialization code
   - JNI bridge implementations

4. **Move to Rust ECS for**:
   - All computational logic (energy calculations, processing)
   - Batch operations on multiple modules
   - Complex algorithms (pathfinding, optimization)
   - Data transformations and processing
   - Performance-critical game loop operations

The Rust ECS backend provides orders of magnitude better performance for batch operations and complex logic. Always consider the performance implications and choose the Rust implementation when possible.

## Event Handling

Key events handled:
- `PlayerInteractEvent.LeftClickBlock` - Module removal with cooldown system
- Right-click on MechaGridBlock - Module placement
- Server startup events for initialization

## Development Notes

### Adding New Modules
1. Create module class extending `AbstractMechaModule`
2. Implement required methods: `initializeCapabilities()`, `getRenderState()`, etc.
3. Create corresponding `MechaModuleItem` class
4. Register in `ModModules.registerModuleTypes()`
5. Add item registration in `ModItems`

### Module Registration Pattern
```java
ModuleRegistry.register(YourModule.MODULE_ID, YourModule.class, YourModule::new);
```

### Block Entity Ticking
- MechaGridBlockEntity ticks and updates modules that need ticking
- Only server-side ticking is implemented
- Module ticking is controlled by `ModuleProperties.needsTicking()`

### Client-Server Synchronization
- Block entity data is synchronized automatically
- Custom renderer handles visual representation of modules
- Client-side events are cancelled to prevent desync

## Configuration

Mod configuration is handled through `Config.java` using NeoForge's config system. Configuration screen is available in the mod list.

## Testing

The mod includes basic game test infrastructure, though specific tests may need to be implemented. Use `runGameTestServer` for automated testing.

## Refactoring Guidelines

When refactoring this codebase, follow these critical guidelines to avoid data loss and maintain functionality:

### Before Any Major Refactoring
1. **ALWAYS create a git commit** before starting any reorganization
2. **Verify all files are committed** - check git status to ensure no uncommitted changes
3. **Create a backup branch** for the current state
4. **Document the current working state** - ensure the project builds and runs correctly

### Safe Refactoring Process
1. **Plan the refactoring completely** before making any changes
2. **Use copy-first, then delete approach** when moving files:
   - Copy files to new locations first
   - Update package declarations and imports
   - Verify compilation works
   - Only then delete old files
3. **Never delete directories** until you're 100% certain all files are moved
4. **Update imports systematically** using search and replace
5. **Test compilation** after each major step

### Package Reorganization Rules
1. **Client-side code** goes in `client.*` packages
2. **Server-side code** goes in `server.*` packages  
3. **Shared code** goes in `common.*` packages
4. **Framework code** stays in `mechamodule.*` packages
5. **Keep related classes together** (e.g., Module + ModuleItem)

### Critical Recovery Procedures
If files are lost during refactoring:
1. **Stop immediately** and don't make more changes
2. **Use git history** to recover lost files: `git show HEAD:path/to/file.java`
3. **Check git log** for recent commits: `git log --oneline -10`
4. **Restore from specific commits** if needed: `git checkout <commit> -- <file>`
5. **Never assume files are "just missing"** - always recover from git

### Compilation Error Handling
When fixing import errors after refactoring:
1. **Fix package declarations first** in moved files
2. **Use systematic search and replace** for import paths
3. **Update registration classes** (ModItems, ModBlocks, etc.)
4. **Verify all ModuleItem classes** are present and registered
5. **Test incremental compilation** to catch errors early

### Module System Specific Rules
1. **Never separate Module from ModuleItem** classes during moves
2. **Update ModuleRegistry** if module packages change
3. **Verify all module items** are registered in ModItems.java
4. **Keep test modules** in separate subdirectories (e.g., `common.module.test`)
5. **Update CLAUDE.md** to reflect new structure

### ECS Integration Preservation
1. **Never modify rust-ecs/** directory structure
2. **Preserve JNI bindings** in server.ecs package
3. **Keep EcsManager and related classes** in server.ecs
4. **Verify Cargo.toml and build.gradle** Rust integration after any changes

### Final Verification Steps
1. **Run full clean build**: `./gradlew clean build`
2. **Test client launch**: `./gradlew runClient`
3. **Verify all modules load** in game
4. **Check log for registration errors**
5. **Test module placement/removal** functionality

### Emergency Rollback
If refactoring fails completely:
1. **Reset to last known good state**: `git reset --hard <commit>`
2. **Clean build artifacts**: `./gradlew clean`
3. **Verify functionality** before attempting refactoring again
4. **Learn from mistakes** and improve the plan

**REMEMBER: Always prioritize data preservation over speed. A slow, careful refactoring is infinitely better than losing work.**