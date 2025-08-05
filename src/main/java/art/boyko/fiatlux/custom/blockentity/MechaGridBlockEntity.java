package art.boyko.fiatlux.custom.blockentity;

import art.boyko.fiatlux.init.ModBlockEntities;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.mechamodule.context.ModuleContext;
import art.boyko.fiatlux.mechamodule.registry.ModuleRegistry;
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

    public MechaGridBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MECHA_GRID_BE.get(), pos, blockState);
        initializeGrid();
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
        
        // Process scheduled updates
        processScheduledUpdates();
        
        // Tick all modules that need ticking
        tickModules();

        // Sync to client if dirty
        if (isDirty) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            isDirty = false;
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
        for (IModuleContext context : tickingModules) {
            ModuleContext moduleContext = (ModuleContext) context;
            IMechaModule module = moduleContext.getOwnerModule();
            if (module != null && module.needsTicking()) {
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
        if (!isValidPosition(x, y, z) || module == null) {
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
        
        // Add to ticking list if needed
        if (module.needsTicking()) {
            tickingModules.add(context);
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
        
        // Remove from ticking list
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
}