package art.boyko.fiatlux.ecs;

/**
 * Represents an entity in the Bevy ECS world.
 * Acts as a wrapper around the raw entity ID.
 */
public class EcsEntity {
    
    private final long entityId;
    final BevyEcsWorld world; // package-private for EcsManager access
    private final String moduleType;
    private final int x, y, z;
    private boolean isValid = true;
    
    EcsEntity(long entityId, BevyEcsWorld world, String moduleType, int x, int y, int z) {
        this.entityId = entityId;
        this.world = world;
        this.moduleType = moduleType;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * Get the raw entity ID
     */
    public long getId() {
        return entityId;
    }
    
    /**
     * Get the module type
     */
    public String getModuleType() {
        return moduleType;
    }
    
    /**
     * Get grid position
     */
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    
    /**
     * Check if this entity is still valid
     */
    public boolean isValid() {
        return isValid && world.isValid();
    }
    
    /**
     * Get energy level (if entity has energy component)
     */
    public int getEnergyLevel() {
        if (!isValid()) return -1;
        return world.getEnergyLevel(entityId);
    }
    
    /**
     * Set display text (if entity has display component)
     */
    public boolean setDisplayText(String text) {
        if (!isValid()) return false;
        return world.setDisplayText(entityId, text);
    }
    
    /**
     * Despawn this entity
     */
    public boolean despawn() {
        if (!isValid()) return false;
        boolean success = world.despawnEntity(entityId);
        if (success) {
            isValid = false;
        }
        return success;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EcsEntity other)) return false;
        return entityId == other.entityId && world == other.world;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(entityId);
    }
    
    @Override
    public String toString() {
        return String.format("EcsEntity{id=%d, type='%s', pos=(%d,%d,%d), valid=%s}", 
                           entityId, moduleType, x, y, z, isValid());
    }
}