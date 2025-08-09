package art.boyko.fiatlux.mechamodule.capability;

/**
 * Interface for modules that can both store and transfer energy
 * Модули которые могут и принимать, и выдавать энергию (хранилища)
 */
public interface IEnergyStorage extends IEnergyProvider, IEnergyReceiver {
    
    /**
     * Set the energy stored in this storage
     * @param energy New energy amount (will be clamped to valid range)
     */
    void setEnergyStored(int energy);
    
    /**
     * Get the energy fill percentage
     * @return Fill percentage from 0.0 to 1.0
     */
    default double getEnergyFillPercentage() {
        int max = getMaxEnergyStored();
        return max > 0 ? (double) getEnergyStored() / max : 0.0;
    }
    
    /**
     * Check if storage is empty
     * @return true if no energy stored
     */
    default boolean isEmpty() {
        return getEnergyStored() == 0;
    }
    
    /**
     * Check if storage is full
     * @return true if at maximum capacity
     */
    default boolean isFull() {
        return getEnergyStored() >= getMaxEnergyStored();
    }
    
    /**
     * Transfer energy to another energy storage
     * @param target Target storage to transfer to
     * @param maxTransfer Maximum amount to transfer
     * @param simulate If true, transfer is only simulated
     * @return Amount of energy transferred
     */
    default int transferTo(IEnergyReceiver target, int maxTransfer, boolean simulate) {
        if (!canExtract() || !target.canReceive()) {
            return 0;
        }
        
        int extractable = Math.min(maxTransfer, Math.min(getMaxExtractRate(), getEnergyStored()));
        int receivable = target.receiveEnergy(extractable, true);
        
        if (receivable > 0) {
            if (!simulate) {
                extractEnergy(receivable, false);
                target.receiveEnergy(receivable, false);
            }
            return receivable;
        }
        
        return 0;
    }
    
    /**
     * Get the maximum transfer rate (minimum of extract and receive rates)
     * @return Maximum bidirectional transfer rate
     */
    default int getMaxTransferRate() {
        return Math.min(getMaxExtractRate(), getMaxReceiveRate());
    }
}