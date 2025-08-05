package art.boyko.fiatlux.mechamodule.context;

import art.boyko.fiatlux.custom.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import art.boyko.fiatlux.mechamodule.capability.ModuleConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of IModuleContext providing modules with access to their environment
 */
public class ModuleContext implements IModuleContext {
    
    private final MechaGridBlockEntity mechaGrid;
    private final GridPosition gridPosition;
    private final IMechaModule ownerModule;
    
    // Connection tracking
    private final Map<Direction, List<ModuleConnection>> connections;
    private boolean needsUpdate = false;
    
    public ModuleContext(MechaGridBlockEntity mechaGrid, GridPosition gridPosition, IMechaModule ownerModule) {
        this.mechaGrid = mechaGrid;
        this.gridPosition = gridPosition;
        this.ownerModule = ownerModule;
        this.connections = new EnumMap<>(Direction.class);
        
        // Initialize connection lists for all directions
        for (Direction direction : Direction.values()) {
            connections.put(direction, new ArrayList<>());
        }
    }
    
    @Override
    public GridPosition getGridPosition() {
        return gridPosition;
    }
    
    @Override
    public BlockPos getWorldPosition() {
        return mechaGrid.getBlockPos();
    }
    
    @Override
    public Level getLevel() {
        return mechaGrid.getLevel();
    }
    
    @Override
    public MechaGridBlockEntity getMechaGrid() {
        return mechaGrid;
    }
    
    @Override
    public @Nullable IMechaModule getNeighbor(Direction direction) {
        GridPosition neighborPos = gridPosition.getNeighbor(direction);
        if (neighborPos == null) {
            return null;
        }
        
        return getModuleAt(neighborPos);
    }
    
    @Override
    public Map<Direction, IMechaModule> getAllNeighbors() {
        Map<Direction, IMechaModule> neighbors = new EnumMap<>(Direction.class);
        
        for (Direction direction : Direction.values()) {
            IMechaModule neighbor = getNeighbor(direction);
            if (neighbor != null) {
                neighbors.put(direction, neighbor);
            }
        }
        
        return neighbors;
    }
    
    @Override
    public boolean hasNeighbor(Direction direction) {
        return getNeighbor(direction) != null;
    }
    
    @Override
    public int getNeighborCount() {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (hasNeighbor(direction)) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public <T extends IModuleCapability> Optional<T> getNeighborCapability(Direction direction, Class<T> capabilityType) {
        IMechaModule neighbor = getNeighbor(direction);
        if (neighbor == null) {
            return Optional.empty();
        }
        
        return neighbor.getCapability(direction.getOpposite(), capabilityType);
    }
    
    @Override
    public <T extends IModuleCapability> Map<Direction, T> findCapabilities(Class<T> capabilityType) {
        Map<Direction, T> capabilities = new EnumMap<>(Direction.class);
        
        for (Direction direction : Direction.values()) {
            Optional<T> capability = getNeighborCapability(direction, capabilityType);
            capability.ifPresent(cap -> capabilities.put(direction, cap));
        }
        
        return capabilities;
    }
    
    @Override
    public List<ModuleConnection> getConnections() {
        List<ModuleConnection> allConnections = new ArrayList<>();
        for (List<ModuleConnection> directionConnections : connections.values()) {
            allConnections.addAll(directionConnections);
        }
        return allConnections;
    }
    
    @Override
    public List<ModuleConnection> getConnections(Direction direction) {
        return new ArrayList<>(connections.get(direction));
    }
    
    @Override
    public boolean establishConnection(Direction direction, Class<? extends IModuleCapability> capabilityType) {
        IMechaModule neighbor = getNeighbor(direction);
        if (neighbor == null) {
            return false;
        }
        
        // Get capabilities from both modules
        Optional<? extends IModuleCapability> sourceCapability = ownerModule.getCapability(direction, capabilityType);
        Optional<? extends IModuleCapability> targetCapability = neighbor.getCapability(direction.getOpposite(), capabilityType);
        
        if (sourceCapability.isEmpty() || targetCapability.isEmpty()) {
            return false;
        }
        
        IModuleCapability sourceCap = sourceCapability.get();
        IModuleCapability targetCap = targetCapability.get();
        
        // Check if they can connect
        if (!sourceCap.canConnectTo(direction, targetCap) || !targetCap.canConnectTo(direction.getOpposite(), sourceCap)) {
            return false;
        }
        
        // Create connection
        ModuleConnection connection = ModuleConnection.builder()
                .source(ownerModule, direction, sourceCap)
                .target(neighbor, direction.getOpposite(), targetCap)
                .connectionType(getConnectionTypeFromCapability(capabilityType))
                .efficiency(1.0f)
                .maxTransferRate(Integer.MAX_VALUE)
                .bidirectional(true)
                .build();
        
        // Add to connections list
        connections.get(direction).add(connection);
        
        // Notify capabilities about the connection
        sourceCap.onConnectionEstablished(direction, targetCap, connection);
        targetCap.onConnectionEstablished(direction.getOpposite(), sourceCap, connection);
        
        return true;
    }
    
    @Override
    public void breakConnection(Direction direction, Class<? extends IModuleCapability> capabilityType) {
        List<ModuleConnection> directionConnections = connections.get(direction);
        
        directionConnections.removeIf(connection -> {
            if (connection.getSourceCapability().getClass().equals(capabilityType)) {
                // Notify capabilities about disconnection
                connection.getSourceCapability().onConnectionBroken(direction, connection.getTargetCapability());
                connection.getTargetCapability().onConnectionBroken(direction.getOpposite(), connection.getSourceCapability());
                return true;
            }
            return false;
        });
    }
    
    @Override
    public void breakAllConnections() {
        for (Direction direction : Direction.values()) {
            List<ModuleConnection> directionConnections = connections.get(direction);
            
            for (ModuleConnection connection : directionConnections) {
                connection.getSourceCapability().onConnectionBroken(direction, connection.getTargetCapability());
                connection.getTargetCapability().onConnectionBroken(direction.getOpposite(), connection.getSourceCapability());
            }
            
            directionConnections.clear();
        }
    }
    
    @Override
    public void scheduleUpdate() {
        needsUpdate = true;
        mechaGrid.scheduleModuleUpdate(this);
    }
    
    @Override
    public void notifyNeighbors() {
        for (Direction direction : Direction.values()) {
            GridPosition neighborPos = gridPosition.getNeighbor(direction);
            if (neighborPos != null) {
                IMechaModule neighbor = getNeighbor(direction);
                IModuleContext neighborContext = mechaGrid.getModuleContext(neighborPos.x(), neighborPos.y(), neighborPos.z());
                
                if (neighbor != null && neighborContext != null) {
                    neighbor.onNeighborChanged(neighborContext, direction.getOpposite(), ownerModule);
                }
            }
        }
    }
    
    @Override
    public void markForRenderUpdate() {
        mechaGrid.markDirty();
    }
    
    @Override
    public boolean isUpdating() {
        return needsUpdate;
    }
    
    @Override
    public long getGameTime() {
        return getLevel() != null ? getLevel().getGameTime() : 0;
    }
    
    @Override
    public boolean isValidGridPosition(int x, int y, int z) {
        return x >= 0 && x < 4 && y >= 0 && y < 4 && z >= 0 && z < 4;
    }
    
    @Override
    public BlockPos gridToWorldPosition(GridPosition gridPos) {
        // Convert grid position to world position relative to MechaGrid
        BlockPos worldPos = getWorldPosition();
        double scale = 1.0 / 4.0; // Each grid cell is 1/4 of a block
        
        return worldPos.offset(
                (int) (gridPos.x() * scale),
                (int) (gridPos.y() * scale),
                (int) (gridPos.z() * scale)
        );
    }
    
    @Override
    public @Nullable IMechaModule getModuleAt(GridPosition position) {
        return mechaGrid.getModule(position.x(), position.y(), position.z());
    }
    
    @Override
    public @Nullable IMechaModule getModuleAtRelative(int dx, int dy, int dz) {
        int newX = gridPosition.x() + dx;
        int newY = gridPosition.y() + dy;
        int newZ = gridPosition.z() + dz;
        
        if (!isValidGridPosition(newX, newY, newZ)) {
            return null;
        }
        
        return getModuleAt(new GridPosition(newX, newY, newZ));
    }
    
    // Helper methods
    
    /**
     * Determine connection type from capability class
     */
    private art.boyko.fiatlux.mechamodule.capability.ConnectionType getConnectionTypeFromCapability(Class<? extends IModuleCapability> capabilityType) {
        String className = capabilityType.getSimpleName().toLowerCase();
        
        if (className.contains("item")) {
            return art.boyko.fiatlux.mechamodule.capability.ConnectionType.ITEM_TRANSPORT;
        } else if (className.contains("energy")) {
            return art.boyko.fiatlux.mechamodule.capability.ConnectionType.ENERGY;
        } else if (className.contains("fluid")) {
            return art.boyko.fiatlux.mechamodule.capability.ConnectionType.FLUID;
        } else if (className.contains("data")) {
            return art.boyko.fiatlux.mechamodule.capability.ConnectionType.DATA;
        } else if (className.contains("mechanical")) {
            return art.boyko.fiatlux.mechamodule.capability.ConnectionType.MECHANICAL;
        }
        
        return art.boyko.fiatlux.mechamodule.capability.ConnectionType.UNIVERSAL;
    }
    
    /**
     * Clear update flag (called by MechaGrid after processing updates)
     */
    public void clearUpdateFlag() {
        needsUpdate = false;
    }
    
    /**
     * Get the owner module of this context
     */
    public IMechaModule getOwnerModule() {
        return ownerModule;
    }
}