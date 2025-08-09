package art.boyko.fiatlux.mechamodule.context;

import art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import art.boyko.fiatlux.mechamodule.capability.ModuleConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Context interface providing modules with information about their environment.
 * This is the main way modules interact with the MechaGrid and other modules.
 */
public interface IModuleContext {
    
    // Position Information
    
    /**
     * Get the grid position of this module (0-3 for each axis)
     */
    GridPosition getGridPosition();
    
    /**
     * Get the world position of the MechaGrid containing this module
     */
    BlockPos getWorldPosition();
    
    /**
     * Get the world level
     */
    Level getLevel();
    
    /**
     * Get the MechaGrid block entity containing this module
     */
    MechaGridBlockEntity getMechaGrid();
    
    // Neighbor Access
    
    /**
     * Get a neighboring module in the specified direction
     * @param direction Direction to check
     * @return The neighboring module, or null if no module exists
     */
    @Nullable IMechaModule getNeighbor(Direction direction);
    
    /**
     * Get all neighboring modules
     * @return Map of directions to modules (only includes existing neighbors)
     */
    Map<Direction, IMechaModule> getAllNeighbors();
    
    /**
     * Check if there is a module in the specified direction
     */
    boolean hasNeighbor(Direction direction);
    
    /**
     * Get the number of neighboring modules
     */
    int getNeighborCount();
    
    // Capability Access
    
    /**
     * Get a capability from a neighboring module
     * @param direction Direction of the neighbor
     * @param capabilityType Type of capability to get
     * @return The capability if available
     */
    <T extends IModuleCapability> Optional<T> getNeighborCapability(Direction direction, Class<T> capabilityType);
    
    /**
     * Find all neighbors that have a specific capability
     * @param capabilityType Type of capability to find
     * @return Map of directions to capabilities
     */
    <T extends IModuleCapability> Map<Direction, T> findCapabilities(Class<T> capabilityType);
    
    // Connection Management
    
    /**
     * Get all active connections for this module
     */
    List<ModuleConnection> getConnections();
    
    /**
     * Get connections in a specific direction
     */
    List<ModuleConnection> getConnections(Direction direction);
    
    /**
     * Establish a new connection with a neighbor
     * @param direction Direction to connect
     * @param connectionType Type of connection
     * @return true if connection was established
     */
    boolean establishConnection(Direction direction, Class<? extends IModuleCapability> capabilityType);
    
    /**
     * Break a connection with a neighbor
     */
    void breakConnection(Direction direction, Class<? extends IModuleCapability> capabilityType);
    
    /**
     * Break all connections for this module
     */
    void breakAllConnections();
    
    // Grid Management
    
    /**
     * Schedule this module for an update on the next tick
     */
    void scheduleUpdate();
    
    /**
     * Request a neighbor update (notifies neighbors of changes)
     */
    void notifyNeighbors();
    
    /**
     * Mark the module for render update on next tick
     */
    void markForRenderUpdate();
    
    /**
     * Check if the grid is currently processing updates
     */
    boolean isUpdating();
    
    /**
     * Get the current game time
     */
    long getGameTime();
    
    // Utility Methods
    
    /**
     * Check if a grid position is valid (within 0-3 bounds)
     */
    boolean isValidGridPosition(int x, int y, int z);
    
    /**
     * Convert grid position to world position
     */
    BlockPos gridToWorldPosition(GridPosition gridPos);
    
    /**
     * Get the module at a specific grid position
     */
    @Nullable IMechaModule getModuleAt(GridPosition position);
    
    /**
     * Get the module at relative coordinates from this module
     */
    @Nullable IMechaModule getModuleAtRelative(int dx, int dy, int dz);
    
    /**
     * Simple record for grid positions
     */
    record GridPosition(int x, int y, int z) {
        
        public GridPosition {
            if (x < 0 || x >= 4 || y < 0 || y >= 4 || z < 0 || z >= 4) {
                throw new IllegalArgumentException("Grid position must be within 0-3 range: " + x + "," + y + "," + z);
            }
        }
        
        /**
         * Get a neighboring position in the specified direction
         */
        public @Nullable GridPosition getNeighbor(Direction direction) {
            int newX = x + direction.getStepX();
            int newY = y + direction.getStepY();
            int newZ = z + direction.getStepZ();
            
            if (newX < 0 || newX >= 4 || newY < 0 || newY >= 4 || newZ < 0 || newZ >= 4) {
                return null;
            }
            
            return new GridPosition(newX, newY, newZ);
        }
        
        /**
         * Calculate distance to another grid position
         */
        public double distanceTo(GridPosition other) {
            int dx = this.x - other.x;
            int dy = this.y - other.y;
            int dz = this.z - other.z;
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }
        
        /**
         * Check if this position is adjacent to another position
         */
        public boolean isAdjacentTo(GridPosition other) {
            int dx = Math.abs(this.x - other.x);
            int dy = Math.abs(this.y - other.y);
            int dz = Math.abs(this.z - other.z);
            
            // Adjacent means exactly one coordinate differs by 1, others are the same
            return (dx + dy + dz) == 1;
        }
    }
}