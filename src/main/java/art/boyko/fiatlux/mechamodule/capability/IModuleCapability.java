package art.boyko.fiatlux.mechamodule.capability;

import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Base interface for all module capabilities.
 * Capabilities define how modules can interact with each other.
 * Examples: IItemTransport, IEnergyStorage, IFluidHandler, etc.
 */
public interface IModuleCapability {
    
    /**
     * Get the unique identifier for this capability type
     */
    ResourceLocation getCapabilityId();
    
    /**
     * Get the module that owns this capability
     */
    IMechaModule getOwnerModule();
    
    /**
     * Check if this capability can connect to another capability in the specified direction
     * @param direction Direction of connection
     * @param other Other capability to connect to
     * @return true if connection is possible
     */
    boolean canConnectTo(Direction direction, IModuleCapability other);
    
    /**
     * Called when a connection is established with another capability
     * @param direction Direction of connection
     * @param other Connected capability
     * @param connection Connection details
     */
    void onConnectionEstablished(Direction direction, IModuleCapability other, ModuleConnection connection);
    
    /**
     * Called when a connection is broken
     * @param direction Direction of disconnection
     * @param other Disconnected capability
     */
    void onConnectionBroken(Direction direction, IModuleCapability other);
    
    /**
     * Called every tick if the capability needs updates
     */
    default void tick() {
        // Default implementation does nothing
    }
    
    /**
     * Check if this capability needs to be ticked
     */
    default boolean needsTicking() {
        return false;
    }
    
    /**
     * Get the priority of this capability for connection resolution
     * Higher priority capabilities are connected first
     */
    default int getConnectionPriority() {
        return 0;
    }
    
    /**
     * Check if this capability supports the specified connection type
     */
    boolean supportsConnectionType(ConnectionType connectionType);
}