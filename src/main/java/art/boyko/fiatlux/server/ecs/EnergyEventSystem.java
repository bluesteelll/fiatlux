package art.boyko.fiatlux.server.ecs;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Java-side energy event system that receives updates from Rust ECS
 * and synchronizes them with Java components
 */
public class EnergyEventSystem {
    
    // Cache for energy data to avoid constant JNI calls
    private static final ConcurrentMap<Long, EnergyData> energyCache = new ConcurrentHashMap<>();
    
    // Energy event data structure
    public static class EnergyData {
        public final long entityId;
        public final int currentEnergy;
        public final int maxCapacity;
        public final long lastUpdate;
        
        public EnergyData(long entityId, int currentEnergy, int maxCapacity) {
            this.entityId = entityId;
            this.currentEnergy = currentEnergy;
            this.maxCapacity = maxCapacity;
            this.lastUpdate = System.currentTimeMillis();
        }
        
        public double getFillPercentage() {
            return maxCapacity > 0 ? (double) currentEnergy / maxCapacity : 0.0;
        }
    }
    
    // Energy change event
    public static class EnergyChangeEvent {
        public final long entityId;
        public final int oldEnergy;
        public final int newEnergy;
        public final int maxCapacity;
        public final BlockPos blockPos;
        
        public EnergyChangeEvent(long entityId, int oldEnergy, int newEnergy, int maxCapacity, BlockPos blockPos) {
            this.entityId = entityId;
            this.oldEnergy = oldEnergy;
            this.newEnergy = newEnergy;
            this.maxCapacity = maxCapacity;
            this.blockPos = blockPos;
        }
        
        public boolean hasChanged() {
            return oldEnergy != newEnergy;
        }
    }
    
    /**
     * Process all energy events from Rust ECS for a specific world
     * This should be called every tick on the server
     */
    public static List<EnergyChangeEvent> processEnergyEvents(long worldId, BlockPos gridPos) {
        List<EnergyChangeEvent> events = new ArrayList<>();
        
        try {
            // Get number of pending energy events from Rust
            long eventInfo = BevyEcsWorld.getEnergyEvents(worldId);
            if (eventInfo <= 0) {
                return events; // No events or error
            }
            
            int eventCount = (int) (eventInfo >> 32); // Unpack event count from high bits
            
            // Process each event
            for (int i = 0; i < eventCount; i++) {
                long eventData = BevyEcsWorld.getEnergyEventData(worldId, i);
                if (eventData == -1) continue; // Skip invalid events
                
                // Unpack event data
                long entityId = eventData >> 32;
                int newEnergy = (int) ((eventData >> 16) & 0xFFFF);
                int maxCapacity = (int) (eventData & 0xFFFF);
                
                // Get old energy from cache or default to 0
                EnergyData cachedData = energyCache.get(entityId);
                int oldEnergy = cachedData != null ? cachedData.currentEnergy : 0;
                
                // Update cache
                energyCache.put(entityId, new EnergyData(entityId, newEnergy, maxCapacity));
                
                // Create event
                events.add(new EnergyChangeEvent(entityId, oldEnergy, newEnergy, maxCapacity, gridPos));
            }
            
        } catch (Exception e) {
            System.err.println("Error processing energy events: " + e.getMessage());
        }
        
        return events;
    }
    
    /**
     * Get cached energy data for an entity
     */
    public static EnergyData getCachedEnergyData(long entityId) {
        return energyCache.get(entityId);
    }
    
    /**
     * Force update energy data from Rust ECS (expensive operation)
     */
    public static EnergyData forceUpdateEnergyData(long worldId, long entityId) {
        try {
            int currentEnergy = BevyEcsWorld.getEnergyLevel(worldId, entityId);
            if (currentEnergy >= 0) {
                // We don't have max capacity from single call, so keep cached or use default
                EnergyData cached = energyCache.get(entityId);
                int maxCapacity = cached != null ? cached.maxCapacity : 1000; // Default fallback
                
                EnergyData newData = new EnergyData(entityId, currentEnergy, maxCapacity);
                energyCache.put(entityId, newData);
                return newData;
            }
        } catch (Exception e) {
            System.err.println("Error force updating energy data: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Batch update all energy data for a world (called periodically for sync)
     */
    public static void batchUpdateEnergyData(long worldId) {
        try {
            long entityCount = BevyEcsWorld.getAllEnergyData(worldId);
            if (entityCount <= 0) return;
            
            // Process each entity in batch
            for (int i = 0; i < entityCount; i++) {
                long packedData = BevyEcsWorld.getEnergyDataByIndex(worldId, i);
                if (packedData == -1) continue;
                
                // Unpack data
                long entityId = packedData >> 32;
                int currentEnergy = (int) ((packedData >> 16) & 0xFFFF);
                int maxCapacity = (int) (packedData & 0xFFFF);
                
                // Update cache
                energyCache.put(entityId, new EnergyData(entityId, currentEnergy, maxCapacity));
            }
            
        } catch (Exception e) {
            System.err.println("Error batch updating energy data: " + e.getMessage());
        }
    }
    
    /**
     * Set energy level for an entity (for external NeoForge capability integration)
     */
    public static boolean setEnergyLevel(long worldId, long entityId, int newEnergy) {
        try {
            boolean success = BevyEcsWorld.setEnergyLevel(worldId, entityId, newEnergy);
            if (success) {
                // Update cache immediately
                EnergyData cached = energyCache.get(entityId);
                if (cached != null) {
                    energyCache.put(entityId, new EnergyData(entityId, newEnergy, cached.maxCapacity));
                }
            }
            return success;
        } catch (Exception e) {
            System.err.println("Error setting energy level: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get transfer statistics for monitoring performance
     */
    public static TransferStats getTransferStats(long worldId) {
        try {
            long stats = BevyEcsWorld.getTransferStats(worldId);
            if (stats == -1) return null;
            
            int pendingCount = (int) (stats >> 32);
            int completedCount = (int) (stats & 0xFFFFFFFF);
            
            return new TransferStats(pendingCount, completedCount);
        } catch (Exception e) {
            System.err.println("Error getting transfer stats: " + e.getMessage());
            return null;
        }
    }
    
    public static class TransferStats {
        public final int pendingTransfers;
        public final int completedTransfers;
        
        public TransferStats(int pendingTransfers, int completedTransfers) {
            this.pendingTransfers = pendingTransfers;
            this.completedTransfers = completedTransfers;
        }
        
        @Override
        public String toString() {
            return String.format("TransferStats{pending=%d, completed=%d}", pendingTransfers, completedTransfers);
        }
    }
    
    /**
     * Clear cached data for specific entity (called when module is removed)
     */
    public static void clearCachedData(long entityId) {
        energyCache.remove(entityId);
    }
    
    /**
     * Clear all cached data (called on world unload)
     */
    public static void clearAllCachedData() {
        energyCache.clear();
    }
    
    /**
     * Get cache size for monitoring
     */
    public static int getCacheSize() {
        return energyCache.size();
    }
}