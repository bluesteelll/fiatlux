package art.boyko.fiatlux.ecs;

/**
 * Data class for energy synchronization between Rust ECS and Java
 * This represents a single module's energy state that needs to be synchronized to the client
 */
public class EnergySync {
    
    public final int x, y, z;  // Module position within grid
    public final int energy;   // Current energy
    public final int maxEnergy; // Maximum energy capacity
    
    /**
     * Constructor called from Rust JNI
     * @param x Module X position in grid
     * @param y Module Y position in grid  
     * @param z Module Z position in grid
     * @param energy Current energy stored
     * @param maxEnergy Maximum energy capacity
     */
    public EnergySync(int x, int y, int z, int energy, int maxEnergy) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.energy = energy;
        this.maxEnergy = maxEnergy;
    }
    
    /**
     * Get the grid position as a tuple
     */
    public Position getPosition() {
        return new Position(x, y, z);
    }
    
    /**
     * Get energy fill percentage (0.0 to 1.0)
     */
    public double getFillPercentage() {
        return maxEnergy > 0 ? (double) energy / maxEnergy : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format("EnergySync[pos=(%d,%d,%d), energy=%d/%d (%.1f%%)]", 
                x, y, z, energy, maxEnergy, getFillPercentage() * 100);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EnergySync)) return false;
        EnergySync other = (EnergySync) obj;
        return x == other.x && y == other.y && z == other.z;
    }
    
    @Override
    public int hashCode() {
        return ((x * 31 + y) * 31 + z);
    }
    
    /**
     * Simple position record for convenience
     */
    public record Position(int x, int y, int z) {
        @Override
        public String toString() {
            return String.format("(%d,%d,%d)", x, y, z);
        }
    }
}