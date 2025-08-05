package art.boyko.fiatlux.mechamodule.capability;

/**
 * Defines different types of connections between modules
 */
public enum ConnectionType {
    
    /**
     * No connection - modules are not connected
     */
    NONE("none"),
    
    /**
     * Item transport connection - for moving items between modules
     */
    ITEM_TRANSPORT("item_transport"),
    
    /**
     * Energy connection - for transferring energy between modules
     */
    ENERGY("energy"),
    
    /**
     * Fluid connection - for transferring fluids between modules
     */
    FLUID("fluid"),
    
    /**
     * Data connection - for sharing information/signals between modules
     */
    DATA("data"),
    
    /**
     * Mechanical connection - for transferring mechanical power
     */
    MECHANICAL("mechanical"),
    
    /**
     * Universal connection - can handle multiple types of transfer
     */
    UNIVERSAL("universal");
    
    private final String name;
    
    ConnectionType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Check if this connection type is compatible with another
     */
    public boolean isCompatibleWith(ConnectionType other) {
        // Universal connections are compatible with everything
        if (this == UNIVERSAL || other == UNIVERSAL) {
            return true;
        }
        
        // Same types are compatible
        if (this == other) {
            return true;
        }
        
        // No other compatibility rules by default
        return false;
    }
    
    /**
     * Get connection type by name
     */
    public static ConnectionType fromName(String name) {
        for (ConnectionType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return NONE;
    }
    
    /**
     * Check if this connection type can transfer items
     */
    public boolean canTransferItems() {
        return this == ITEM_TRANSPORT || this == UNIVERSAL;
    }
    
    /**
     * Check if this connection type can transfer energy
     */
    public boolean canTransferEnergy() {
        return this == ENERGY || this == UNIVERSAL;
    }
    
    /**
     * Check if this connection type can transfer fluids
     */
    public boolean canTransferFluids() {
        return this == FLUID || this == UNIVERSAL;
    }
    
    /**
     * Check if this connection type can transfer data
     */
    public boolean canTransferData() {
        return this == DATA || this == UNIVERSAL;
    }
    
    /**
     * Check if this connection type can transfer mechanical power
     */
    public boolean canTransferMechanicalPower() {
        return this == MECHANICAL || this == UNIVERSAL;
    }
}