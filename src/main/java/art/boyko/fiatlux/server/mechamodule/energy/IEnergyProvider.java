package art.boyko.fiatlux.server.mechamodule.energy;

/**
 * Interface for modules that can provide (output) energy
 * Модули которые могут выдавать энергию
 */
public interface IEnergyProvider {
    
    /**
     * Extract energy from this provider
     * @param maxExtract Maximum amount of energy to extract
     * @param simulate If true, extraction is only simulated
     * @return Amount of energy that was (or would be) extracted
     */
    int extractEnergy(int maxExtract, boolean simulate);
    
    /**
     * Check if this provider can extract any energy
     * @return true if can provide energy
     */
    boolean canExtract();
    
    /**
     * Get the amount of energy stored in this provider
     * @return Current energy amount
     */
    int getEnergyStored();
    
    /**
     * Get the maximum energy this provider can store
     * @return Maximum capacity
     */
    int getMaxEnergyStored();
    
    /**
     * Get the maximum rate at which energy can be extracted
     * @return Maximum extraction rate per operation
     */
    int getMaxExtractRate();
    
    /**
     * Check if this provider has energy available
     * @param amount Minimum amount to check for
     * @return true if has at least the specified amount
     */
    default boolean hasEnergyAvailable(int amount) {
        return getEnergyStored() >= amount;
    }
    
    /**
     * Get the energy generation rate (if applicable)
     * @return Energy generated per tick, 0 if not a generator
     */
    default int getEnergyGenerationRate() {
        return 0;
    }
}