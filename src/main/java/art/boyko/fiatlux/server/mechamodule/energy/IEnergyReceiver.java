package art.boyko.fiatlux.server.mechamodule.energy;

/**
 * Interface for modules that can receive (input) energy
 * Модули которые могут принимать энергию
 */
public interface IEnergyReceiver {
    
    /**
     * Receive energy into this receiver
     * @param maxReceive Maximum amount of energy to receive
     * @param simulate If true, receiving is only simulated
     * @return Amount of energy that was (or would be) received
     */
    int receiveEnergy(int maxReceive, boolean simulate);
    
    /**
     * Check if this receiver can receive any energy
     * @return true if can accept energy
     */
    boolean canReceive();
    
    /**
     * Get the amount of energy stored in this receiver
     * @return Current energy amount
     */
    int getEnergyStored();
    
    /**
     * Get the maximum energy this receiver can store
     * @return Maximum capacity
     */
    int getMaxEnergyStored();
    
    /**
     * Get the maximum rate at which energy can be received
     * @return Maximum receiving rate per operation
     */
    int getMaxReceiveRate();
    
    /**
     * Check if this receiver can accept more energy
     * @param amount Amount to check for
     * @return true if can accept at least the specified amount
     */
    default boolean canAcceptEnergy(int amount) {
        return canReceive() && (getEnergyStored() + amount <= getMaxEnergyStored());
    }
    
    /**
     * Get remaining capacity
     * @return Amount of energy that can still be stored
     */
    default int getRemainingCapacity() {
        return getMaxEnergyStored() - getEnergyStored();
    }
    
    /**
     * Get the energy consumption rate (if applicable)
     * @return Energy consumed per tick, 0 if not a consumer
     */
    default int getEnergyConsumptionRate() {
        return 0;
    }
}