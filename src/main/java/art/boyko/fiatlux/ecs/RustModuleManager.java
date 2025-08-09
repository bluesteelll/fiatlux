package art.boyko.fiatlux.ecs;

import net.minecraft.core.BlockPos;

/**
 * Java interface to Rust ECS module management system.
 * 
 * This class provides complete separation of concerns:
 * - Rust ECS handles ALL logic (energy generation, transfer, consumption)
 * - Java only handles presentation (rendering, UI, client synchronization)
 */
public class RustModuleManager {

    // ===== NATIVE METHODS (JNI to Rust) =====
    
    /**
     * Create a new module entity in Rust ECS with full logic
     * @param worldX World X coordinate
     * @param worldY World Y coordinate  
     * @param worldZ World Z coordinate
     * @param moduleX Module X position in grid
     * @param moduleY Module Y position in grid
     * @param moduleZ Module Z position in grid
     * @param moduleType Type of module ("test_module", "energy_storage", etc.)
     * @return Rust entity ID or -1 on failure
     */
    public static long createModule(int worldX, int worldY, int worldZ, 
                                         int moduleX, int moduleY, int moduleZ, 
                                         String moduleType) {
        if (!NativeLibraryLoader.isLibraryLoaded()) {
            return -1; // Return failure if library not loaded
        }
        try {
            return createModuleNative(worldX, worldY, worldZ, moduleX, moduleY, moduleZ, moduleType);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("⚠️ RustModuleManager.createModule failed: " + e.getMessage());
            return -1;
        }
    }
    
    private static native long createModuleNative(int worldX, int worldY, int worldZ, 
                                            int moduleX, int moduleY, int moduleZ, 
                                            String moduleType);

    /**
     * Remove a module entity from Rust ECS
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @param entityId Rust entity ID to remove
     * @return true if removed successfully
     */
    public static native boolean removeModule(int worldX, int worldY, int worldZ, long entityId);

    /**
     * Tick all Rust ECS systems for a world - ALL LOGIC HAPPENS HERE
     * This replaces individual Java module ticking
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     */
    public static void tickWorld(int worldX, int worldY, int worldZ) {
        if (!NativeLibraryLoader.isLibraryLoaded()) {
            return; // Silently return if library not loaded
        }
        try {
            tickWorldNative(worldX, worldY, worldZ);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("⚠️ RustModuleManager.tickWorld failed: " + e.getMessage());
        }
    }
    
    private static native void tickWorldNative(int worldX, int worldY, int worldZ);

    /**
     * Get energy data for a specific module from Rust
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @param entityId Rust entity ID
     * @return Packed energy data (current, max, flags, generation) or -1 if not found
     */
    public static native long getModuleEnergy(int worldX, int worldY, int worldZ, long entityId);

    /**
     * Get count of modules that need synchronization with Java
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Number of modules needing sync
     */
    public static native long getModulesToSync(int worldX, int worldY, int worldZ);

    /**
     * Get sync data count for a world (clears the data after retrieval)
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Number of energy updates that were processed or 0 if none
     */
    public static native long getSyncData(int worldX, int worldY, int worldZ);

    /**
     * Force all modules to sync (called when Java needs fresh data)
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     */
    public static native void forceSyncAll(int worldX, int worldY, int worldZ);

    // ===== HELPER METHODS =====

    /**
     * Unpack energy data returned by getModuleEnergy()
     */
    public static EnergyData unpackEnergyData(long packedData) {
        if (packedData == -1) return null;
        
        int current = (int) (packedData & 0xFFFF);
        int maxCapacity = (int) ((packedData >> 16) & 0xFFFF);
        int flags = (int) ((packedData >> 32) & 0xFFFF);
        int generationRate = (int) ((packedData >> 48) & 0xFFFF);
        
        boolean canReceive = (flags & 1) != 0;
        boolean canExtract = (flags & 2) != 0;
        
        return new EnergyData(current, maxCapacity, generationRate, canReceive, canExtract);
    }

    /**
     * Create a module in Rust ECS and return the entity ID
     */
    public static long createModuleAtPosition(BlockPos worldPos, int x, int y, int z, String moduleType) {
        long entityId = createModule(worldPos.getX(), worldPos.getY(), worldPos.getZ(), 
                                   x, y, z, moduleType);
        
        if (entityId != -1) {
            System.out.println("🏗️ Java: Created Rust module '" + moduleType + 
                             "' at [" + x + "," + y + "," + z + "] -> Entity " + entityId);
        } else {
            System.err.println("❌ Java: Failed to create Rust module '" + moduleType + "'");
        }
        
        return entityId;
    }

    /**
     * Remove a module from Rust ECS
     */
    public static boolean removeModuleAtPosition(BlockPos worldPos, long entityId) {
        boolean success = removeModule(worldPos.getX(), worldPos.getY(), worldPos.getZ(), entityId);
        
        if (success) {
            System.out.println("🗑️ Java: Removed Rust module entity " + entityId);
        } else {
            System.err.println("❌ Java: Failed to remove Rust module entity " + entityId);
        }
        
        return success;
    }

    /**
     * Tick all logic in Rust ECS for a world
     */
    public static void tickWorldLogic(BlockPos worldPos) {
        tickWorld(worldPos.getX(), worldPos.getY(), worldPos.getZ());
    }

    /**
     * Get energy data for a module
     */
    public static EnergyData getModuleEnergyData(BlockPos worldPos, long entityId) {
        long packedData = getModuleEnergy(worldPos.getX(), worldPos.getY(), worldPos.getZ(), entityId);
        return unpackEnergyData(packedData);
    }

    /**
     * Check if any modules need synchronization
     */
    public static boolean hasModulesNeedingSync(BlockPos worldPos) {
        return getModulesToSync(worldPos.getX(), worldPos.getY(), worldPos.getZ()) > 0;
    }

    /**
     * Get and clear sync data for a world
     */
    public static long retrieveSyncData(BlockPos worldPos) {
        if (!NativeLibraryLoader.isLibraryLoaded()) {
            return 0;
        }
        try {
            return getSyncData(worldPos.getX(), worldPos.getY(), worldPos.getZ());
        } catch (UnsatisfiedLinkError e) {
            System.err.println("⚠️ RustModuleManager.retrieveSyncData failed: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Force sync all modules for fresh data
     */
    public static void forceSyncAllModules(BlockPos worldPos) {
        if (!NativeLibraryLoader.isLibraryLoaded()) {
            return;
        }
        try {
            forceSyncAll(worldPos.getX(), worldPos.getY(), worldPos.getZ());
            System.out.println("🔄 Java: Forced sync for all modules at " + worldPos);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("⚠️ RustModuleManager.forceSyncAllModules failed: " + e.getMessage());
        }
    }

    // ===== PERFORMANCE MONITORING =====

    /**
     * Get performance statistics for monitoring
     */
    public static void logRustPerformanceStats(BlockPos worldPos) {
        long syncCount = getModulesToSync(worldPos.getX(), worldPos.getY(), worldPos.getZ());
        System.out.println("📊 Rust Performance - World " + worldPos + ": " + syncCount + " modules need sync");
    }

    /**
     * Data class for unpacked energy information
     */
    public static class EnergyData {
        public final int current;
        public final int maxCapacity;
        public final int generationRate;
        public final boolean canReceive;
        public final boolean canExtract;

        public EnergyData(int current, int maxCapacity, int generationRate, 
                         boolean canReceive, boolean canExtract) {
            this.current = current;
            this.maxCapacity = maxCapacity;
            this.generationRate = generationRate;
            this.canReceive = canReceive;
            this.canExtract = canExtract;
        }

        public double getFillPercentage() {
            return maxCapacity > 0 ? (double) current / maxCapacity : 0.0;
        }

        @Override
        public String toString() {
            return String.format("EnergyData[%d/%d RF (%.1f%%), gen=%d/t, recv=%s, ext=%s]",
                    current, maxCapacity, getFillPercentage() * 100,
                    generationRate, canReceive, canExtract);
        }
    }
}