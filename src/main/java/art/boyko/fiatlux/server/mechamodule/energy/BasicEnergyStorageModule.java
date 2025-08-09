package art.boyko.fiatlux.server.mechamodule.energy;

import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Basic energy storage module - can receive and provide energy
 * Базовый модуль-хранилище энергии
 */
public class BasicEnergyStorageModule extends AbstractEnergyModule {
    
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "basic_energy_storage");
    
    public BasicEnergyStorageModule() {
        this(5000, 100, 100, true, true);
    }
    
    public BasicEnergyStorageModule(int maxCapacity, int maxExtractRate, int maxReceiveRate, 
                                  boolean canExtract, boolean canReceive) {
        super(MODULE_ID, maxCapacity, maxExtractRate, maxReceiveRate, canExtract, canReceive);
    }
    
    @Override
    protected String getEcsModuleType() {
        return "energy_storage";
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Basic Energy Storage");
    }
    
    
    @Override
    protected void onEnergyTick(IModuleContext context) {
        super.onEnergyTick(context);
        
        // ALL STORAGE LOGIC IS IN RUST ECS!
        // Java just provides facade for GUI and external integration
        // Actual energy transfers are handled by Rust batch processing
    }
    
    /**
     * Get storage status for display
     */
    public StorageStatus getStorageStatus() {
        double fillPercentage = getEnergyFillPercentage();
        
        if (fillPercentage >= 0.95) return StorageStatus.FULL;
        if (fillPercentage >= 0.75) return StorageStatus.HIGH;
        if (fillPercentage >= 0.25) return StorageStatus.MEDIUM;
        if (fillPercentage > 0.05) return StorageStatus.LOW;
        return StorageStatus.EMPTY;
    }
    
    public enum StorageStatus {
        EMPTY, LOW, MEDIUM, HIGH, FULL
    }
}