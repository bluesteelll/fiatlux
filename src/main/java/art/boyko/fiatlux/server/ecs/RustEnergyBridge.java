package art.boyko.fiatlux.server.ecs;

import art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Bridge between Rust ECS energy system and Java NeoForge capabilities.
 * This class provides a stateless interface that delegates all energy operations to Rust.
 */
public class RustEnergyBridge {
    
    // Map BlockPos to ECS entity IDs
    private static final ConcurrentMap<BlockPos, Long> blockToEntityMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, BlockPos> entityToBlockMap = new ConcurrentHashMap<>();
    
    /**
     * Register a MechaGrid with its ECS world and entities
     */
    public static void registerGrid(BlockPos blockPos, long worldId, ConcurrentMap<String, Long> moduleEntities) {
        // For now, we'll use a simple mapping approach
        // In a more sophisticated system, we'd maintain entity-to-position mapping
        System.out.println("🔗 RustEnergyBridge: Registered grid at " + blockPos + " with world " + worldId);
    }
    
    /**
     * Unregister a MechaGrid
     */
    public static void unregisterGrid(BlockPos blockPos) {
        Long entityId = blockToEntityMap.remove(blockPos);
        if (entityId != null) {
            entityToBlockMap.remove(entityId);
        }
        System.out.println("🔗 RustEnergyBridge: Unregistered grid at " + blockPos);
    }
    
    /**
     * Create a NeoForge IEnergyStorage capability that delegates to Rust ECS
     */
    public static IEnergyStorage createEnergyCapability(MechaGridBlockEntity gridEntity, Direction direction) {
        return new RustBackedEnergyStorage(gridEntity, direction);
    }
    
    /**
     * IEnergyStorage implementation that delegates all operations to Rust ECS
     */
    private static class RustBackedEnergyStorage implements IEnergyStorage {
        private final MechaGridBlockEntity gridEntity;
        private final Direction accessDirection;
        
        public RustBackedEnergyStorage(MechaGridBlockEntity gridEntity, Direction accessDirection) {
            this.gridEntity = gridEntity;
            this.accessDirection = accessDirection;
        }
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // Get ECS world ID for this grid
            long worldId = EcsManager.getWorldId(gridEntity.getBlockPos());
            if (worldId == -1) return 0;
            
            // Process events to get latest state
            EnergyEventSystem.processEnergyEvents(worldId, gridEntity.getBlockPos());
            
            int totalReceived = 0;
            
            // Get all energy entities in this grid and try to receive energy
            long entityCount = BevyEcsWorld.getAllEnergyData(worldId);
            for (int i = 0; i < entityCount; i++) {
                long packedData = BevyEcsWorld.getEnergyDataByIndex(worldId, i);
                if (packedData == -1) continue;
                
                // Unpack data
                long entityId = packedData >> 32;
                int currentEnergy = (int) ((packedData >> 16) & 0xFFFF);
                int maxCapacity = (int) (packedData & 0xFFFF);
                
                // Calculate how much this entity can receive
                int canReceive = maxCapacity - currentEnergy;
                int toReceive = Math.min(maxReceive - totalReceived, canReceive);
                
                if (toReceive > 0 && !simulate) {
                    // Set new energy level in Rust
                    BevyEcsWorld.setEnergyLevel(worldId, entityId, currentEnergy + toReceive);
                    totalReceived += toReceive;
                    
                    if (totalReceived >= maxReceive) {
                        break;
                    }
                }
            }
            
            return totalReceived;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            // Get ECS world ID for this grid
            long worldId = EcsManager.getWorldId(gridEntity.getBlockPos());
            if (worldId == -1) return 0;
            
            // Process events to get latest state
            EnergyEventSystem.processEnergyEvents(worldId, gridEntity.getBlockPos());
            
            int totalExtracted = 0;
            
            // Get all energy entities in this grid and try to extract energy
            long entityCount = BevyEcsWorld.getAllEnergyData(worldId);
            for (int i = 0; i < entityCount; i++) {
                long packedData = BevyEcsWorld.getEnergyDataByIndex(worldId, i);
                if (packedData == -1) continue;
                
                // Unpack data
                long entityId = packedData >> 32;
                int currentEnergy = (int) ((packedData >> 16) & 0xFFFF);
                
                // Calculate how much this entity can extract
                int toExtract = Math.min(maxExtract - totalExtracted, currentEnergy);
                
                if (toExtract > 0 && !simulate) {
                    // Set new energy level in Rust
                    BevyEcsWorld.setEnergyLevel(worldId, entityId, currentEnergy - toExtract);
                    totalExtracted += toExtract;
                    
                    if (totalExtracted >= maxExtract) {
                        break;
                    }
                }
            }
            
            return totalExtracted;
        }
        
        @Override
        public int getEnergyStored() {
            // Get ECS world ID for this grid
            long worldId = EcsManager.getWorldId(gridEntity.getBlockPos());
            if (worldId == -1) return 0;
            
            int totalStored = 0;
            
            // Sum energy from all entities in this grid
            long entityCount = BevyEcsWorld.getAllEnergyData(worldId);
            for (int i = 0; i < entityCount; i++) {
                long packedData = BevyEcsWorld.getEnergyDataByIndex(worldId, i);
                if (packedData == -1) continue;
                
                // Unpack data
                int currentEnergy = (int) ((packedData >> 16) & 0xFFFF);
                totalStored += currentEnergy;
            }
            
            return totalStored;
        }
        
        @Override
        public int getMaxEnergyStored() {
            // Get ECS world ID for this grid
            long worldId = EcsManager.getWorldId(gridEntity.getBlockPos());
            if (worldId == -1) return 0;
            
            int totalCapacity = 0;
            
            // Sum max capacity from all entities in this grid
            long entityCount = BevyEcsWorld.getAllEnergyData(worldId);
            for (int i = 0; i < entityCount; i++) {
                long packedData = BevyEcsWorld.getEnergyDataByIndex(worldId, i);
                if (packedData == -1) continue;
                
                // Unpack data
                int maxCapacity = (int) (packedData & 0xFFFF);
                totalCapacity += maxCapacity;
            }
            
            return totalCapacity;
        }
        
        @Override
        public boolean canExtract() {
            // Can extract if any entity has energy
            return getEnergyStored() > 0;
        }
        
        @Override
        public boolean canReceive() {
            // Can receive if any entity has capacity
            return getEnergyStored() < getMaxEnergyStored();
        }
    }
    
    /**
     * Batch synchronize energy events from Rust to Java for a specific world
     */
    public static void synchronizeEnergyEvents(long worldId, BlockPos gridPos) {
        try {
            var events = EnergyEventSystem.processEnergyEvents(worldId, gridPos);
            
            if (!events.isEmpty()) {
                System.out.println("⚡ RustEnergyBridge: Processed " + events.size() + " energy events for grid at " + gridPos);
                
                // Optionally trigger block updates for visual changes
                for (var event : events) {
                    if (event.hasChanged()) {
                        // Could trigger render updates here if needed
                        System.out.println("⚡ Energy changed for entity " + event.entityId + ": " + event.oldEnergy + " -> " + event.newEnergy);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error synchronizing energy events: " + e.getMessage());
        }
    }
    
    /**
     * Get transfer statistics for monitoring performance
     */
    public static void logTransferStats(long worldId) {
        try {
            var stats = EnergyEventSystem.getTransferStats(worldId);
            if (stats != null) {
                System.out.println("📊 Transfer Stats: " + stats);
            }
        } catch (Exception e) {
            System.err.println("Error getting transfer stats: " + e.getMessage());
        }
    }
    
    /**
     * Clear cached data when a grid is unloaded
     */
    public static void clearGridData(BlockPos gridPos) {
        // Clear from mapping
        Long entityId = blockToEntityMap.remove(gridPos);
        if (entityId != null) {
            entityToBlockMap.remove(entityId);
            EnergyEventSystem.clearCachedData(entityId);
        }
    }
}