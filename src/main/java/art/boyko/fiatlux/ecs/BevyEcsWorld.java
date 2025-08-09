package art.boyko.fiatlux.ecs;

/**
 * Java wrapper for Bevy ECS World through JNI.
 * Provides access to Rust-based Bevy ECS functionality.
 */
public class BevyEcsWorld {
    
    // Native library loading
    static {
        if (!NativeLibraryLoader.loadNativeLibrary()) {
            String error = NativeLibraryLoader.getLoadError();
            System.err.println("Failed to load native library: " + error);
            // Don't throw exception, let the code handle graceful fallback
            System.err.println("Rust ECS will be disabled, using Java fallback mode");
        }
    }
    
    private long worldId;
    private boolean isValid = false;
    
    public BevyEcsWorld() {
        if (!NativeLibraryLoader.isLibraryLoaded()) {
            System.err.println("⚠️ BevyEcsWorld: Native library not loaded, cannot create ECS world");
            this.worldId = -1;
            this.isValid = false;
            return;
        }
        
        try {
            this.worldId = createWorld();
            if (this.worldId >= 0) {
                this.isValid = true;
                System.out.println("🦀 Created Bevy ECS world with ID: " + this.worldId);
            } else {
                System.err.println("⚠️ Failed to create Bevy ECS world - native method returned -1");
            }
        } catch (UnsatisfiedLinkError e) {
            System.err.println("⚠️ Failed to create Bevy ECS world: " + e.getMessage());
            this.worldId = -1;
            this.isValid = false;
        }
    }
    
    /**
     * Check if this ECS world is valid and ready to use
     */
    public boolean isValid() {
        return isValid;
    }
    
    /**
     * Get the internal world ID
     */
    public long getWorldId() {
        return worldId;
    }
    
    /**
     * Spawn a new entity in the ECS world
     * @param moduleType Type of module (e.g., "energy_generator", "processor")
     * @param x Grid X coordinate
     * @param y Grid Y coordinate
     * @param z Grid Z coordinate
     * @return Entity ID, or -1 on error
     */
    public long spawnEntity(String moduleType, int x, int y, int z) {
        if (!isValid) return -1;
        return spawnEntity(worldId, moduleType, x, y, z);
    }
    
    /**
     * Despawn an entity from the ECS world
     * @param entityId Entity ID to despawn
     * @return true if successful
     */
    public boolean despawnEntity(long entityId) {
        if (!isValid) return false;
        return despawnEntity(worldId, entityId);
    }
    
    /**
     * Tick the ECS world, running all systems once
     */
    public void tick() {
        if (!isValid) return;
        tickWorld(worldId);
    }
    
    /**
     * Get energy level of an entity
     * @param entityId Entity to query
     * @return Energy level, or -1 if entity has no energy component
     */
    public int getEnergyLevel(long entityId) {
        if (!isValid) return -1;
        return getEnergyLevel(worldId, entityId);
    }
    
    /**
     * Set display text for a display module
     * @param entityId Entity with display component
     * @param text Text to display
     * @return true if successful
     */
    public boolean setDisplayText(long entityId, String text) {
        if (!isValid) return false;
        return setDisplayText(worldId, entityId, text);
    }
    
    /**
     * Get energy events for synchronization
     * @return Packed long with event count in high bits
     */
    public long getEnergyEvents() {
        if (!isValid) return -1;
        return getEnergyEvents(worldId);
    }
    
    /**
     * Get specific energy event data by index
     * @param eventIndex Index of the event to get
     * @return Packed energy event data
     */
    public long getEnergyEventData(int eventIndex) {
        if (!isValid) return -1;
        return getEnergyEventData(worldId, eventIndex);
    }
    
    /**
     * Get all energy data for batch synchronization
     * @return Number of entities with energy data
     */
    public long getAllEnergyData() {
        if (!isValid) return -1;
        return getAllEnergyData(worldId);
    }
    
    /**
     * Get energy data by index for batch transfer
     * @param index Index of the entity
     * @return Packed energy data
     */
    public long getEnergyDataByIndex(int index) {
        if (!isValid) return -1;
        return getEnergyDataByIndex(worldId, index);
    }
    
    /**
     * Set energy level for an entity (external integration)
     * @param entityId Target entity
     * @param newEnergy New energy value
     * @return true if successful
     */
    public boolean setEnergyLevel(long entityId, int newEnergy) {
        if (!isValid) return false;
        return setEnergyLevel(worldId, entityId, newEnergy);
    }
    
    /**
     * Get energy transfer statistics
     * @return Packed transfer statistics
     */
    public long getTransferStats() {
        if (!isValid) return -1;
        return getTransferStats(worldId);
    }
    
    /**
     * Get batch processing statistics
     * @return Packed long with tick count (high 32 bits) and pending changes (low 32 bits)
     */
    public long getBatchStats() {
        if (!isValid) return -1;
        return getBatchStats(worldId);
    }
    
    /**
     * Get current tick count from batch statistics
     * @return Current tick count
     */
    public long getTickCount() {
        long stats = getBatchStats();
        if (stats == -1) return 0;
        return stats >>> 32; // Extract high 32 bits
    }
    
    /**
     * Get pending changes count from batch statistics
     * @return Number of pending changes
     */
    public int getPendingChanges() {
        long stats = getBatchStats();
        if (stats == -1) return 0;
        return (int)(stats & 0xFFFFFFFF); // Extract low 32 bits
    }
    
    /**
     * Check if this tick will process ECS systems (every 4th tick)
     * @return true if systems will run this tick
     */
    public boolean willProcessThisTick() {
        return getTickCount() % 4 == 0;
    }
    
    /**
     * Clean up and destroy the ECS world
     */
    public void destroy() {
        if (isValid) {
            destroyWorld(worldId);
            isValid = false;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }
    
    // Native methods - implemented in Rust
    private static native long createWorld();
    private static native void destroyWorld(long worldId);
    private static native long spawnEntity(long worldId, String moduleType, int x, int y, int z);
    private static native boolean despawnEntity(long worldId, long entityId);
    private static native void tickWorld(long worldId);
    private static native long getBatchStats(long worldId);
    public static native int getEnergyLevel(long worldId, long entityId);
    private static native boolean setDisplayText(long worldId, long entityId, String text);
    
    // New energy system native methods
    public static native long getEnergyEvents(long worldId);
    public static native long getEnergyEventData(long worldId, int eventIndex);
    public static native long getAllEnergyData(long worldId);
    public static native long getEnergyDataByIndex(long worldId, int index);
    public static native boolean setEnergyLevel(long worldId, long entityId, int newEnergy);
    public static native long getTransferStats(long worldId);
}