package art.boyko.fiatlux.mechamodule.capability;

import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * Represents a connection between two modules through their capabilities
 */
public class ModuleConnection {
    
    private final IMechaModule sourceModule;
    private final IMechaModule targetModule;
    private final Direction sourceDirection;
    private final Direction targetDirection;
    private final ConnectionType connectionType;
    private final IModuleCapability sourceCapability;
    private final IModuleCapability targetCapability;
    
    // Connection properties
    private final float efficiency;
    private final int maxTransferRate;
    private final boolean bidirectional;
    
    // Runtime state
    private boolean isActive;
    private long lastTransferTime;
    private int currentTransferRate;
    
    private ModuleConnection(Builder builder) {
        this.sourceModule = builder.sourceModule;
        this.targetModule = builder.targetModule;
        this.sourceDirection = builder.sourceDirection;
        this.targetDirection = builder.targetDirection;
        this.connectionType = builder.connectionType;
        this.sourceCapability = builder.sourceCapability;
        this.targetCapability = builder.targetCapability;
        this.efficiency = builder.efficiency;
        this.maxTransferRate = builder.maxTransferRate;
        this.bidirectional = builder.bidirectional;
        this.isActive = true;
        this.lastTransferTime = 0;
        this.currentTransferRate = 0;
    }
    
    // Getters
    public IMechaModule getSourceModule() { return sourceModule; }
    public IMechaModule getTargetModule() { return targetModule; }
    public Direction getSourceDirection() { return sourceDirection; }
    public Direction getTargetDirection() { return targetDirection; }
    public ConnectionType getConnectionType() { return connectionType; }
    public IModuleCapability getSourceCapability() { return sourceCapability; }
    public IModuleCapability getTargetCapability() { return targetCapability; }
    public float getEfficiency() { return efficiency; }
    public int getMaxTransferRate() { return maxTransferRate; }
    public boolean isBidirectional() { return bidirectional; }
    public boolean isActive() { return isActive; }
    public long getLastTransferTime() { return lastTransferTime; }
    public int getCurrentTransferRate() { return currentTransferRate; }
    
    /**
     * Set the active state of this connection
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    /**
     * Record a transfer through this connection
     * @param amount Amount transferred
     * @param currentTime Current game time
     */
    public void recordTransfer(int amount, long currentTime) {
        this.lastTransferTime = currentTime;
        this.currentTransferRate = amount;
    }
    
    /**
     * Calculate the effective transfer rate considering efficiency
     */
    public int getEffectiveTransferRate(int requestedAmount) {
        int maxAmount = Math.min(requestedAmount, maxTransferRate);
        return Mth.floor(maxAmount * efficiency);
    }
    
    /**
     * Check if this connection can handle the specified amount
     */
    public boolean canTransfer(int amount) {
        return isActive && amount <= maxTransferRate;
    }
    
    /**
     * Get the reverse direction of this connection (for bidirectional connections)
     */
    public ModuleConnection getReverse() {
        if (!bidirectional) {
            return null;
        }
        
        return builder()
                .source(targetModule, targetDirection, targetCapability)
                .target(sourceModule, sourceDirection, sourceCapability)
                .connectionType(connectionType)
                .efficiency(efficiency)
                .maxTransferRate(maxTransferRate)
                .bidirectional(true)
                .build();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ModuleConnection that = (ModuleConnection) obj;
        return Objects.equals(sourceModule, that.sourceModule) &&
               Objects.equals(targetModule, that.targetModule) &&
               sourceDirection == that.sourceDirection &&
               targetDirection == that.targetDirection &&
               connectionType == that.connectionType;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(sourceModule, targetModule, sourceDirection, targetDirection, connectionType);
    }
    
    @Override
    public String toString() {
        return String.format("ModuleConnection{%s[%s] -> %s[%s] (%s)}",
                sourceModule.getModuleId().getPath(),
                sourceDirection.getName(),
                targetModule.getModuleId().getPath(),
                targetDirection.getName(),
                connectionType.getName()
        );
    }
    
    /**
     * Create a new ModuleConnection builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for ModuleConnection
     */
    public static class Builder {
        private IMechaModule sourceModule;
        private IMechaModule targetModule;
        private Direction sourceDirection;
        private Direction targetDirection;
        private ConnectionType connectionType = ConnectionType.NONE;
        private IModuleCapability sourceCapability;
        private IModuleCapability targetCapability;
        private float efficiency = 1.0f;
        private int maxTransferRate = Integer.MAX_VALUE;
        private boolean bidirectional = false;
        
        public Builder source(IMechaModule module, Direction direction, IModuleCapability capability) {
            this.sourceModule = module;
            this.sourceDirection = direction;
            this.sourceCapability = capability;
            return this;
        }
        
        public Builder target(IMechaModule module, Direction direction, IModuleCapability capability) {
            this.targetModule = module;
            this.targetDirection = direction;
            this.targetCapability = capability;
            return this;
        }
        
        public Builder connectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;
            return this;
        }
        
        public Builder efficiency(float efficiency) {
            this.efficiency = Mth.clamp(efficiency, 0.0f, 1.0f);
            return this;
        }
        
        public Builder maxTransferRate(int maxTransferRate) {
            this.maxTransferRate = Math.max(0, maxTransferRate);
            return this;
        }
        
        public Builder bidirectional(boolean bidirectional) {
            this.bidirectional = bidirectional;
            return this;
        }
        
        public ModuleConnection build() {
            if (sourceModule == null || targetModule == null) {
                throw new IllegalStateException("Source and target modules must be set");
            }
            if (sourceDirection == null || targetDirection == null) {
                throw new IllegalStateException("Source and target directions must be set");
            }
            if (sourceCapability == null || targetCapability == null) {
                throw new IllegalStateException("Source and target capabilities must be set");
            }
            
            return new ModuleConnection(this);
        }
    }
}