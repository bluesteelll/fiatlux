package art.boyko.fiatlux.mechamodule.base;

import net.minecraft.world.item.Rarity;

/**
 * Properties that define behavior and characteristics of a MechaModule
 */
public class ModuleProperties {
    
    private final boolean needsTicking;
    private final boolean canRotate;
    private final boolean hasDirectionalConnections;
    private final int energyConsumption;
    private final int energyProduction; 
    private final int maxConnections;
    private final Rarity rarity;
    private final float hardness;
    private final boolean isTranslucent;
    
    private ModuleProperties(Builder builder) {
        this.needsTicking = builder.needsTicking;
        this.canRotate = builder.canRotate;
        this.hasDirectionalConnections = builder.hasDirectionalConnections;
        this.energyConsumption = builder.energyConsumption;
        this.energyProduction = builder.energyProduction;
        this.maxConnections = builder.maxConnections;
        this.rarity = builder.rarity;
        this.hardness = builder.hardness;
        this.isTranslucent = builder.isTranslucent;
    }
    
    /**
     * Whether this module needs to be ticked every game tick
     */
    public boolean needsTicking() {
        return needsTicking;
    }
    
    /**
     * Whether this module can be rotated when placed
     */
    public boolean canRotate() {
        return canRotate;
    }
    
    /**
     * Whether this module has directional connections (like pipes)
     */
    public boolean hasDirectionalConnections() {
        return hasDirectionalConnections;
    }
    
    /**
     * Energy consumed per tick (if applicable)
     */
    public int getEnergyConsumption() {
        return energyConsumption;
    }
    
    /**
     * Energy produced per tick (if applicable)
     */
    public int getEnergyProduction() {
        return energyProduction;
    }
    
    /**
     * Maximum number of connections this module can have
     */
    public int getMaxConnections() {
        return maxConnections;
    }
    
    /**
     * Rarity of this module (affects tooltip color)
     */
    public Rarity getRarity() {
        return rarity;
    }
    
    /**
     * Hardness for breaking time calculation
     */
    public float getHardness() {
        return hardness;
    }
    
    /**
     * Whether this module is translucent (affects lighting)
     */
    public boolean isTranslucent() {
        return isTranslucent;
    }
    
    /**
     * Create a new ModuleProperties builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for ModuleProperties
     */
    public static class Builder {
        private boolean needsTicking = false;
        private boolean canRotate = false;
        private boolean hasDirectionalConnections = false;
        private int energyConsumption = 0;
        private int energyProduction = 0;
        private int maxConnections = 6; // Default: can connect in all 6 directions
        private Rarity rarity = Rarity.COMMON;
        private float hardness = 1.0f;
        private boolean isTranslucent = false;
        
        /**
         * Set whether this module needs ticking
         */
        public Builder needsTicking(boolean needsTicking) {
            this.needsTicking = needsTicking;
            return this;
        }
        
        /**
         * Set whether this module can be rotated
         */
        public Builder canRotate(boolean canRotate) {
            this.canRotate = canRotate;
            return this;
        }
        
        /**
         * Set whether this module has directional connections
         */
        public Builder hasDirectionalConnections(boolean hasDirectionalConnections) {
            this.hasDirectionalConnections = hasDirectionalConnections;
            return this;
        }
        
        /**
         * Set energy consumption per tick
         */
        public Builder energyConsumption(int energyConsumption) {
            this.energyConsumption = energyConsumption;
            return this;
        }
        
        /**
         * Set energy production per tick
         */
        public Builder energyProduction(int energyProduction) {
            this.energyProduction = energyProduction;
            return this;
        }
        
        /**
         * Set maximum number of connections
         */
        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }
        
        /**
         * Set rarity
         */
        public Builder rarity(Rarity rarity) {
            this.rarity = rarity;
            return this;
        }
        
        /**
         * Set hardness
         */
        public Builder hardness(float hardness) {
            this.hardness = hardness;
            return this;
        }
        
        /**
         * Set whether module is translucent
         */
        public Builder translucent(boolean isTranslucent) {
            this.isTranslucent = isTranslucent;
            return this;
        }
        
        /**
         * Build the ModuleProperties
         */
        public ModuleProperties build() {
            return new ModuleProperties(this);
        }
    }
}