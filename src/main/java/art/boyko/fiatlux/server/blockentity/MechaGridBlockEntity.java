package art.boyko.fiatlux.server.blockentity;

import art.boyko.fiatlux.server.init.ModBlockEntities;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.mechamodule.context.ModuleContext;
import art.boyko.fiatlux.mechamodule.registry.ModuleRegistry;
import art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability;
import art.boyko.fiatlux.ecs.EcsManager;
import art.boyko.fiatlux.ecs.RustEnergyBridge;
import art.boyko.fiatlux.ecs.EnergyEventSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MechaGridBlockEntity extends BlockEntity {
    public static final int GRID_SIZE = 4;
    public static final int TOTAL_POSITIONS = GRID_SIZE * GRID_SIZE * GRID_SIZE; // 64 positions
    
    // 3D array for storing modules - replacing BlockState grid
    private final IMechaModule[][][] moduleGrid = new IMechaModule[GRID_SIZE][GRID_SIZE][GRID_SIZE];
    
    // Module contexts for each position
    private final ModuleContext[][][] contextGrid = new ModuleContext[GRID_SIZE][GRID_SIZE][GRID_SIZE];
    
    // Bit field for tracking occupied positions (64 bits = 1 long)
    private long occupiedMask = 0L;
    
    // Cache for performance
    private int cachedModuleCount = 0;
    private boolean isDirty = false;
    
    // Collision shape cache
    private VoxelShape cachedCollisionShape = null;
    private boolean collisionShapeDirty = true;
    
    // Module ticking system
    private final List<IModuleContext> tickingModules = new ArrayList<>();
    private final Map<IModuleContext, Integer> updateSchedule = new HashMap<>();
    private int currentTick = 0;
    
    // Rust ECS initialization tracking
    private boolean rustEcsInitialized = false;
    private boolean rustEcsAvailable = false;

    public MechaGridBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MECHA_GRID_BE.get(), pos, blockState);
        System.out.println("🏭 MechaGridBlockEntity CONSTRUCTOR CALLED at " + pos);
        initializeGrid();
        
        // 🚀 RUST ECS SEPARATION: Initialize Rust ECS world for full logic
        // Defer Rust ECS registration until tick() when we know we're server-side
        System.out.println("🚀 MechaGridBlockEntity: Constructor complete, Rust ECS will be initialized on first tick");
    }

    private void initializeGrid() {
        // Initialize all positions as empty
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    moduleGrid[x][y][z] = null;
                    contextGrid[x][y][z] = null;
                }
            }
        }
        occupiedMask = 0L;
        cachedModuleCount = 0;
        tickingModules.clear();
        updateSchedule.clear();
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        
        currentTick++;
        
        // Initialize Rust ECS on first server-side tick
        if (!rustEcsInitialized) {
            try {
                RustEnergyBridge.registerWorldForSync(pos.getX(), pos.getY(), pos.getZ());
                
                // Initialize sync manager for real-time updates
                art.boyko.fiatlux.ecs.RustModuleManager.forceSyncAllModules(worldPosition);
                
                rustEcsAvailable = true;
                rustEcsInitialized = true;
                System.out.println("🚀 Successfully initialized Rust ECS for grid at " + pos);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("⚠️ Rust ECS not available: " + e.getMessage());
                System.err.println("📢 Running in fallback mode - logic will remain in Java");
                rustEcsAvailable = false;
                rustEcsInitialized = true; // Mark as initialized so we don't keep trying
            } catch (Exception e) {
                System.err.println("⚠️ Error initializing Rust ECS: " + e.getMessage());
                System.err.println("📢 Running in fallback mode - logic will remain in Java");
                rustEcsAvailable = false;
                rustEcsInitialized = true;
            }
        }
        
        // 🚀 ALL LOGIC NOW IN RUST ECS - Java is just presentation layer!
        // Tick Rust ECS world - this handles ALL module logic now
        if (rustEcsAvailable) {
            try {
                if (currentTick % 20 == 0) { // Log once per second
                    System.out.println("🚀 Ticking Rust ECS world at " + worldPosition + " (tick " + currentTick + ")");
                }
                art.boyko.fiatlux.ecs.RustModuleManager.tickWorldLogic(worldPosition);
            } catch (UnsatisfiedLinkError e) {
                // Fallback to Java logic if Rust call fails
                System.err.println("⚠️ Rust ECS call failed, falling back to Java logic: " + e.getMessage());
                rustEcsAvailable = false; // Disable further Rust calls
                if (currentTick % 20 == 0) { // Once per second for non-critical operations
                    processScheduledUpdates();
                    tickModules();
                }
            }
        } else {
            // Fallback to Java logic if Rust is not available
            if (currentTick % 20 == 0) { // Once per second for non-critical operations
                System.out.println("⚠️ Running in Java fallback mode at " + worldPosition + " (tick " + currentTick + ")");
                processScheduledUpdates();
                tickModules();
            }
        }
        
        // SIMPLE FIX: Force sync every 5 ticks regardless of Rust sync detection
        if (currentTick % 5 == 0) { // Every 5 ticks = 4 times per second (optimal for visual updates)
            
            System.out.println("🔄 Java: Forcing module cache update every 5 ticks for real-time sync");
            
            // ALWAYS update presentation layer from Rust data
            updateJavaPresentationFromRust();
            markDirty(); // ALWAYS force client update
            
            // Try to get sync data anyway (for debugging)
            try {
                long syncUpdates = art.boyko.fiatlux.ecs.RustModuleManager.retrieveSyncData(worldPosition);
                if (syncUpdates > 0) {
                    System.out.println("📤 Rust: Retrieved " + syncUpdates + " sync updates");
                }
            } catch (UnsatisfiedLinkError e) {
                System.out.println("⚠️ Rust sync data not available, using forced updates");
            }
            
            // Performance logging
            if (currentTick % 100 == 0) {
                System.out.println("🚀 Rust-powered grid at tick " + currentTick + " - forced sync every 5 ticks");
            }
        }
        
        // NO MORE JAVA MODULE TICKING - everything is in Rust now!
        // Legacy code removed: processScheduledUpdates(), tickModules()

        // Sync to client if visual state changed
        if (isDirty) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            isDirty = false;
        }
    }
    
    /**
     * Update Java presentation layer from Rust ECS data
     * This syncs visual states without affecting logic
     */
    private void updateJavaPresentationFromRust() {
        boolean hasVisualChanges = false;
        
        // Update each module's presentation state
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    IMechaModule module = moduleGrid[x][y][z];
                    if (module != null) {
                        
                        // CRITICAL FIX: Actually update cached data from Rust!
                        if (module instanceof art.boyko.fiatlux.server.modules.test.RustBackedTestModule rustTest) {
                            rustTest.forceCacheUpdateFromRust();
                            hasVisualChanges = true;
                        } else if (module instanceof art.boyko.fiatlux.server.modules.energy.RustBackedEnergyStorageModule rustStorage) {
                            rustStorage.forceCacheUpdateFromRust(); 
                            hasVisualChanges = true;
                        }
                    }
                }
            }
        }
        
        // Mark for client sync if visual state changed
        if (hasVisualChanges) {
            markDirty();
        }
    }
    
    private void processScheduledUpdates() {
        updateSchedule.entrySet().removeIf(entry -> {
            if (entry.getValue() <= currentTick) {
                // Process the update
                IModuleContext context = entry.getKey();
                ModuleContext moduleContext = (ModuleContext) context;
                moduleContext.clearUpdateFlag();
                return true;
            }
            return false;
        });
    }
    
    private void tickModules() {
        if (currentTick % 20 == 0) { // Every second
            System.out.println("🎯 MechaGridBlockEntity: Ticking " + tickingModules.size() + " modules at tick " + currentTick);
        }
        
        for (IModuleContext context : tickingModules) {
            ModuleContext moduleContext = (ModuleContext) context;
            IMechaModule module = moduleContext.getOwnerModule();
            if (module != null && module.needsTicking()) {
                if (currentTick % 20 == 0) { // Every second
                    System.out.println("⚡ MechaGridBlockEntity: Ticking module " + module.getModuleId());
                }
                module.tick(context);
            }
        }
    }

    /**
     * Place a module at the specified grid position
     * @param x Grid X coordinate (0-3)
     * @param y Grid Y coordinate (0-3) 
     * @param z Grid Z coordinate (0-3)
     * @param module The module to place
     * @return true if successfully placed, false if position was occupied
     */
    public boolean placeModule(int x, int y, int z, IMechaModule module) {
        System.out.println("🏗️ MechaGridBlockEntity.placeModule() CALLED at [" + x + "," + y + "," + z + "] with module: " + (module != null ? module.getModuleId() : "null"));
        
        if (!isValidPosition(x, y, z) || module == null) {
            System.out.println("❌ placeModule FAILED: invalid position or null module");
            return false;
        }

        int bitIndex = getPositionIndex(x, y, z);
        
        // Check if position is already occupied
        if (isPositionOccupied(bitIndex)) {
            return false;
        }

        // Create context for the module
        IModuleContext.GridPosition gridPos = new IModuleContext.GridPosition(x, y, z);
        ModuleContext context = new ModuleContext(this, gridPos, module);
        
        // Place the module
        moduleGrid[x][y][z] = module;
        contextGrid[x][y][z] = context;
        occupiedMask |= (1L << bitIndex);
        cachedModuleCount++;
        collisionShapeDirty = true;
        
        // Initialize the module
        module.onPlacedInGrid(context);
        
        // Register in Rust ECS for energy processing
        registerModuleInEcs(x, y, z, module);
        
        // Add to ticking list if needed (legacy - will be phased out)
        if (module.needsTicking()) {
            tickingModules.add(context);
            System.out.println("🔧 MechaGridBlockEntity: Added module " + module.getModuleId() + " to ticking list. Total ticking modules: " + tickingModules.size());
        } else {
            System.out.println("⚠️ MechaGridBlockEntity: Module " + module.getModuleId() + " does NOT need ticking!");
        }
        
        // Notify neighbors
        notifyNeighborsOfChange(x, y, z, module);
        
        markDirty();
        return true;
    }
    
    /**
     * Legacy method for backward compatibility - converts BlockState to module if possible
     */
    @Deprecated
    public boolean placeBlock(int x, int y, int z, BlockState blockState) {
        // This is for backward compatibility - in the future, this should not be used
        // For now, we'll ignore BlockState placement since modules are the new system
        return false;
    }

    /**
     * Remove module at the specified grid position
     * @param x Grid X coordinate (0-3)
     * @param y Grid Y coordinate (0-3)
     * @param z Grid Z coordinate (0-3)
     * @return The removed module, or null if position was empty
     */
    public @Nullable IMechaModule removeModule(int x, int y, int z) {
        if (!isValidPosition(x, y, z)) {
            return null;
        }

        int bitIndex = getPositionIndex(x, y, z);
        
        // Check if position is occupied
        if (!isPositionOccupied(bitIndex)) {
            return null;
        }

        // Get the module and context
        IMechaModule removedModule = moduleGrid[x][y][z];
        ModuleContext context = contextGrid[x][y][z];
        
        if (removedModule == null) {
            return null;
        }
        
        // Notify the module it's being removed
        removedModule.onRemovedFromGrid(context);
        
        // Unregister from Rust ECS
        unregisterModuleFromEcs(x, y, z, removedModule);
        
        // Remove from ticking list (legacy)
        tickingModules.remove(context);
        updateSchedule.remove(context);
        
        // Break all connections
        if (context != null) {
            context.breakAllConnections();
        }
        
        // Remove the module
        moduleGrid[x][y][z] = null;
        contextGrid[x][y][z] = null;
        occupiedMask &= ~(1L << bitIndex);
        cachedModuleCount--;
        collisionShapeDirty = true;
        
        // Notify neighbors
        notifyNeighborsOfChange(x, y, z, null);
        
        markDirty();
        return removedModule;
    }
    
    /**
     * Legacy method for backward compatibility - REMOVED
     * Use removeModule() instead to get proper ItemStack from toItemStack()
     */

    /**
     * Get module at specified grid position
     */
    public @Nullable IMechaModule getModule(int x, int y, int z) {
        if (!isValidPosition(x, y, z)) {
            return null;
        }
        return moduleGrid[x][y][z];
    }
    
    /**
     * Get module context at specified grid position
     */
    public @Nullable IModuleContext getModuleContext(int x, int y, int z) {
        if (!isValidPosition(x, y, z)) {
            return null;
        }
        return contextGrid[x][y][z];
    }
    
    /**
     * Legacy method for backward compatibility - returns render state of module
     */
    @Deprecated
    public BlockState getBlock(int x, int y, int z) {
        IMechaModule module = getModule(x, y, z);
        return module != null ? module.getRenderState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    /**
     * Check if position is occupied (more efficient than checking if block is air)
     */
    public boolean isPositionOccupied(int x, int y, int z) {
        if (!isValidPosition(x, y, z)) {
            return false;
        }
        int bitIndex = getPositionIndex(x, y, z);
        return isPositionOccupied(bitIndex);
    }

    private boolean isPositionOccupied(int bitIndex) {
        return (occupiedMask & (1L << bitIndex)) != 0;
    }

    /**
     * Get total number of modules placed in the grid
     */
    public int getTotalModules() {
        return cachedModuleCount;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    @Deprecated
    public int getTotalBlocks() {
        return getTotalModules();
    }

    /**
     * Check if grid is full
     */
    public boolean isFull() {
        return cachedModuleCount >= TOTAL_POSITIONS;
    }

    /**
     * Check if grid is empty
     */
    public boolean isEmpty() {
        return cachedModuleCount == 0;
    }

    /**
     * Drop all modules stored in the grid when block is broken
     */
    public void dropAllModules(Level level, BlockPos pos) {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    IMechaModule module = moduleGrid[x][y][z];
                    if (module != null) {
                        ItemStack moduleStack = module.toItemStack();
                        net.minecraft.world.level.block.Block.popResource(level, pos, moduleStack);
                    }
                }
            }
        }
    }
    
    /**
     * Legacy method for backward compatibility - REMOVED
     * Use dropAllModules() instead or rely on automatic onRemove()
     */

    /**
     * Gets the module grid array for direct access (use carefully)
     */
    public IMechaModule[][][] getModuleGrid() {
        return moduleGrid;
    }
    
    /**
     * Gets the context grid array for direct access (use carefully)
     */
    public ModuleContext[][][] getContextGrid() {
        return contextGrid;
    }
    
    /**
     * Legacy method for backward compatibility - creates virtual BlockState grid
     */
    @Deprecated
    public BlockState[][][] getGrid() {
        BlockState[][][] virtualGrid = new BlockState[GRID_SIZE][GRID_SIZE][GRID_SIZE];
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    IMechaModule module = moduleGrid[x][y][z];
                    virtualGrid[x][y][z] = module != null ? module.getRenderState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                }
            }
        }
        return virtualGrid;
    }
    
    /**
     * Gets the cached collision shape, recalculating if necessary
     */
    public VoxelShape getCachedCollisionShape() {
        if (collisionShapeDirty || cachedCollisionShape == null) {
            cachedCollisionShape = buildCollisionShape();
            collisionShapeDirty = false;
        }
        return cachedCollisionShape;
    }
    
    /**
     * Builds the collision shape from all modules in the grid
     */
    private VoxelShape buildCollisionShape() {
        if (isEmpty()) {
            return Shapes.empty();
        }
        
        VoxelShape combinedShape = Shapes.empty();
        double cellSize = 1.0 / GRID_SIZE;
        
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    int bitIndex = x + y * GRID_SIZE + z * GRID_SIZE * GRID_SIZE;
                    if ((occupiedMask & (1L << bitIndex)) == 0) {
                        continue;
                    }
                    
                    IMechaModule module = moduleGrid[x][y][z];
                    if (module == null) {
                        continue;
                    }
                    
                    // Create a small cube for each module
                    double minX = x * cellSize;
                    double minY = y * cellSize;
                    double minZ = z * cellSize;
                    double maxX = minX + cellSize;
                    double maxY = minY + cellSize;
                    double maxZ = minZ + cellSize;
                    
                    VoxelShape cellShape = Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
                    combinedShape = Shapes.or(combinedShape, cellShape);
                }
            }
        }
        
        return combinedShape;
    }

    /**
     * Get occupied positions mask for efficient iteration
     */
    public long getOccupiedMask() {
        return occupiedMask;
    }

    // Utility methods
    private boolean isValidPosition(int x, int y, int z) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE && z >= 0 && z < GRID_SIZE;
    }

    private int getPositionIndex(int x, int y, int z) {
        return x + y * GRID_SIZE + z * GRID_SIZE * GRID_SIZE;
    }

    public void markDirty() {
        setChanged();
        isDirty = true;
        
        // Notify the world that collision shape may have changed
        if (level != null && !level.isClientSide()) {
            BlockState blockState = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, blockState, blockState, 3);
            
            // Force neighbors to update their cached shapes
            level.neighborChanged(worldPosition, blockState.getBlock(), worldPosition);
        }
    }
    
    
    /**
     * Notify neighboring modules when a module is placed or removed
     */
    private void notifyNeighborsOfChange(int x, int y, int z, @Nullable IMechaModule placedModule) {
        for (Direction direction : Direction.values()) {
            int neighborX = x + direction.getStepX();
            int neighborY = y + direction.getStepY();
            int neighborZ = z + direction.getStepZ();
            
            if (isValidPosition(neighborX, neighborY, neighborZ)) {
                IMechaModule neighborModule = moduleGrid[neighborX][neighborY][neighborZ];
                IModuleContext neighborContext = contextGrid[neighborX][neighborY][neighborZ];
                
                if (neighborModule != null && neighborContext != null) {
                    neighborModule.onNeighborChanged(neighborContext, direction.getOpposite(), placedModule);
                }
            }
        }
    }
    
    /**
     * Schedule a module for update on the next tick
     */
    public void scheduleModuleUpdate(IModuleContext context) {
        updateSchedule.put(context, currentTick + 1);
    }
    
    /**
     * Forces recalculation of collision shape
     */
    public void invalidateCollisionCache() {
        collisionShapeDirty = true;
        cachedCollisionShape = null;
    }

    // NBT Serialization
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        // Save occupied mask for efficiency
        tag.putLong("OccupiedMask", occupiedMask);
        tag.putInt("ModuleCount", cachedModuleCount);
        tag.putInt("CurrentTick", currentTick);
        
        // Save only occupied positions to reduce NBT size
        ListTag modulesList = new ListTag();
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    IMechaModule module = moduleGrid[x][y][z];
                    if (module != null) {
                        CompoundTag moduleTag = new CompoundTag();
                        moduleTag.putInt("x", x);
                        moduleTag.putInt("y", y);
                        moduleTag.putInt("z", z);
                        
                        // Save module data
                        CompoundTag moduleData = module.saveToNBT();
                        moduleTag.put("ModuleData", moduleData);
                        
                        modulesList.add(moduleTag);
                    }
                }
            }
        }
        tag.put("Modules", modulesList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // Clear grid first
        initializeGrid();
        
        // Load occupied mask and count
        occupiedMask = tag.getLong("OccupiedMask");
        cachedModuleCount = tag.getInt("ModuleCount");
        currentTick = tag.getInt("CurrentTick");
        
        // Reset collision cache
        collisionShapeDirty = true;
        cachedCollisionShape = null;
        
        // Clear ticking lists
        tickingModules.clear();
        updateSchedule.clear();
        
        // Load modules
        ListTag modulesList = tag.getList("Modules", Tag.TAG_COMPOUND);
        for (int i = 0; i < modulesList.size(); i++) {
            CompoundTag moduleTag = modulesList.getCompound(i);
            
            int x = moduleTag.getInt("x");
            int y = moduleTag.getInt("y");
            int z = moduleTag.getInt("z");
            
            if (isValidPosition(x, y, z) && moduleTag.contains("ModuleData")) {
                CompoundTag moduleData = moduleTag.getCompound("ModuleData");
                
                // Create module from NBT
                IMechaModule module = ModuleRegistry.createModuleFromNBT(moduleData);
                if (module != null) {
                    // Create context
                    IModuleContext.GridPosition gridPos = new IModuleContext.GridPosition(x, y, z);
                    ModuleContext context = new ModuleContext(this, gridPos, module);
                    
                    // Place module in grid
                    moduleGrid[x][y][z] = module;
                    contextGrid[x][y][z] = context;
                    
                    // Add to ticking list if needed
                    if (module.needsTicking()) {
                        tickingModules.add(context);
                    }
                    
                    // Initialize module context
                    module.onPlacedInGrid(context);
                }
            }
        }
    }

    // Client sync
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    /**
     * Get all modules that need ticking
     */
    public List<IModuleContext> getTickingModules() {
        return new ArrayList<>(tickingModules);
    }
    
    /**
     * Check if a module at the given position exists and implements IModuleContext.GridPosition interface properly
     */
    public boolean hasValidModuleAt(int x, int y, int z) {
        return isValidPosition(x, y, z) && 
               moduleGrid[x][y][z] != null && 
               contextGrid[x][y][z] != null;
    }
    
    
    /**
     * NeoForge IEnergyStorage capability integration
     * Now delegates to Rust ECS for all energy operations
     */
    public IEnergyStorage getEnergyStorageCapability(@Nullable Direction direction) {
        return RustEnergyBridge.createEnergyCapability(this, direction);
    }
    
    /**
     * Register or update module in Rust ECS when placed
     */
    private void registerModuleInEcs(int x, int y, int z, IMechaModule module) {
        // Determine module type for ECS
        String ecsModuleType = determineEcsModuleType(module);
        System.out.println("🔧 Attempting to register module " + module.getModuleId() + " as type '" + ecsModuleType + "' at [" + x + "," + y + "," + z + "]");
        
        // Create entity in Rust ECS using the proper high-performance API
        long entityId = art.boyko.fiatlux.ecs.RustModuleManager.createModuleAtPosition(
            worldPosition, x, y, z, ecsModuleType);
            
        if (entityId != -1) {
            System.out.println("✅ Successfully registered module " + module.getModuleId() + " as ECS entity " + entityId);
        } else {
            System.err.println("❌ Failed to register module " + module.getModuleId() + " in Rust ECS");
        }
    }
    
    /**
     * Unregister module from Rust ECS when removed
     */
    private void unregisterModuleFromEcs(int x, int y, int z, IMechaModule module) {
        long worldId = EcsManager.getWorldId(worldPosition);
        if (worldId != -1) {
            // For now, we don't have a direct way to find entity by position
            // This would require a more sophisticated mapping system
            System.out.println("🦀 Would unregister module " + module.getModuleId() + " from ECS");
        }
    }
    
    /**
     * Map Java module types to Rust ECS module types
     */
    private String determineEcsModuleType(IMechaModule module) {
        String moduleId = module.getModuleId().getPath();
        System.out.println("🔍 Mapping module ID '" + moduleId + "' to ECS type");
        return switch (moduleId) {
            case "energy_generator" -> "energy_generator";
            case "energy_storage" -> "energy_storage";
            case "test_energy_consumer" -> "energy_consumer";
            case "test_module" -> "energy_consumer"; // Test module acts as energy consumer
            case "processor" -> "processor";
            case "display" -> "display";
            default -> {
                System.out.println("⚠️ Unknown module type: " + moduleId + ", using 'energy_consumer' as fallback");
                yield "energy_consumer"; // Default fallback with energy capability
            }
        };
    }
}