package art.boyko.fiatlux.custom.blockentity;

import art.boyko.fiatlux.init.ModBlockEntities;
import art.boyko.fiatlux.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class MechaGridBlockEntity extends BlockEntity {
    public static final int GRID_SIZE = 4;
    public static final int TOTAL_POSITIONS = GRID_SIZE * GRID_SIZE * GRID_SIZE; // 64 positions
    
    // 3D array for storing block states - optimized access pattern
    private final BlockState[][][] grid = new BlockState[GRID_SIZE][GRID_SIZE][GRID_SIZE];
    
    // Bit field for tracking occupied positions (64 bits = 1 long)
    // More memory-efficient than checking BlockState.isAir() for each position
    private long occupiedMask = 0L;
    
    // Cache for performance
    private int cachedBlockCount = 0;
    private boolean isDirty = false;

    public MechaGridBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MECHA_GRID_BE.get(), pos, blockState);
        initializeGrid();
    }

    private void initializeGrid() {
        // Initialize all positions with air
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    grid[x][y][z] = Blocks.AIR.defaultBlockState();
                }
            }
        }
        occupiedMask = 0L;
        cachedBlockCount = 0;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        // Sync to client if dirty
        if (isDirty) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            isDirty = false;
        }
    }

    /**
     * Place a block at the specified grid position
     * @param x Grid X coordinate (0-3)
     * @param y Grid Y coordinate (0-3) 
     * @param z Grid Z coordinate (0-3)
     * @param blockState The block state to place
     * @return true if successfully placed, false if position was occupied
     */
    public boolean placeBlock(int x, int y, int z, BlockState blockState) {
        if (!isValidPosition(x, y, z)) {
            return false;
        }

        int bitIndex = getPositionIndex(x, y, z);
        
        // Check if position is already occupied
        if (isPositionOccupied(bitIndex)) {
            return false;
        }

        // Place the block
        grid[x][y][z] = blockState;
        occupiedMask |= (1L << bitIndex);
        cachedBlockCount++;
        
        markDirty();
        return true;
    }

    /**
     * Remove block at the specified grid position
     * @param x Grid X coordinate (0-3)
     * @param y Grid Y coordinate (0-3)
     * @param z Grid Z coordinate (0-3)
     * @return The removed block state, or null if position was empty
     */
    public @Nullable BlockState removeBlock(int x, int y, int z) {
        if (!isValidPosition(x, y, z)) {
            return null;
        }

        int bitIndex = getPositionIndex(x, y, z);
        
        // Check if position is occupied
        if (!isPositionOccupied(bitIndex)) {
            return null;
        }

        // Remove the block
        BlockState removedBlock = grid[x][y][z];
        grid[x][y][z] = Blocks.AIR.defaultBlockState();
        occupiedMask &= ~(1L << bitIndex);
        cachedBlockCount--;
        
        markDirty();
        return removedBlock;
    }

    /**
     * Get block state at specified grid position
     */
    public BlockState getBlock(int x, int y, int z) {
        if (!isValidPosition(x, y, z)) {
            return Blocks.AIR.defaultBlockState();
        }
        return grid[x][y][z];
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
     * Get total number of blocks placed in the grid
     */
    public int getTotalBlocks() {
        return cachedBlockCount;
    }

    /**
     * Check if grid is full
     */
    public boolean isFull() {
        return cachedBlockCount >= TOTAL_POSITIONS;
    }

    /**
     * Check if grid is empty
     */
    public boolean isEmpty() {
        return cachedBlockCount == 0;
    }

    /**
     * Drop all blocks stored in the grid when block is broken
     */
    public void dropAllBlocks(Level level, BlockPos pos) {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    BlockState blockState = grid[x][y][z];
                    if (!blockState.isAir()) {
                        net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(blockState.getBlock()));
                    }
                }
            }
        }
    }

    /**
     * Get 3D grid for rendering
     */
    public BlockState[][][] getGrid() {
        return grid;
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

    private void markDirty() {
        setChanged();
        isDirty = true;
    }

    // NBT Serialization
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        
        // Save occupied mask for efficiency
        tag.putLong("OccupiedMask", occupiedMask);
        tag.putInt("BlockCount", cachedBlockCount);
        
        // Save only occupied positions to reduce NBT size
        ListTag blocksList = new ListTag();
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                for (int z = 0; z < GRID_SIZE; z++) {
                    BlockState blockState = grid[x][y][z];
                    if (!blockState.isAir()) {
                        CompoundTag blockTag = new CompoundTag();
                        blockTag.putInt("x", x);
                        blockTag.putInt("y", y);
                        blockTag.putInt("z", z);
                        
                        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
                        blockTag.putString("block", blockId.toString());
                        
                        // Save block state properties if any
                        if (!blockState.getValues().isEmpty()) {
                            CompoundTag propertiesTag = new CompoundTag();
                            blockState.getValues().forEach((property, value) -> {
                                propertiesTag.putString(property.getName(), value.toString());
                            });
                            blockTag.put("properties", propertiesTag);
                        }
                        
                        blocksList.add(blockTag);
                    }
                }
            }
        }
        tag.put("Blocks", blocksList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        
        // Clear grid first
        initializeGrid();
        
        // Load occupied mask and count
        occupiedMask = tag.getLong("OccupiedMask");
        cachedBlockCount = tag.getInt("BlockCount");
        
        // Load blocks
        ListTag blocksList = tag.getList("Blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocksList.size(); i++) {
            CompoundTag blockTag = blocksList.getCompound(i);
            
            int x = blockTag.getInt("x");
            int y = blockTag.getInt("y");
            int z = blockTag.getInt("z");
            
            if (isValidPosition(x, y, z)) {
                String blockId = blockTag.getString("block");
                ResourceLocation blockLocation = ResourceLocation.parse(blockId);
                
                if (BuiltInRegistries.BLOCK.containsKey(blockLocation)) {
                    net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.get(blockLocation);
                    BlockState blockState = block.defaultBlockState();
                    
                    // Load properties if any
                    if (blockTag.contains("properties")) {
                        CompoundTag propertiesTag = blockTag.getCompound("properties");
                        // Note: Full property restoration would require more complex logic
                        // For now, we use default state
                    }
                    
                    grid[x][y][z] = blockState;
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
}