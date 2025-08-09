package art.boyko.fiatlux.server.mechamodule.energy;

import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Basic energy consumer module - consumes energy to perform work
 * Базовый модуль-потребитель энергии
 */
public class BasicEnergyConsumerModule extends AbstractEnergyModule {
    
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "basic_energy_consumer");
    
    private final int consumptionRate;
    
    public BasicEnergyConsumerModule() {
        this(0, 0, 50, false, true, 10);
    }
    
    public BasicEnergyConsumerModule(int maxCapacity, int maxExtractRate, int maxReceiveRate,
                                   boolean canExtract, boolean canReceive, int consumptionRate) {
        super(MODULE_ID, maxCapacity, maxExtractRate, maxReceiveRate, canExtract, canReceive);
        this.consumptionRate = consumptionRate;
    }
    
    
    @Override
    protected String getEcsModuleType() {
        return "energy_consumer";
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Basic Energy Consumer");
    }
    
    
    @Override
    public int getEnergyConsumptionRate() {
        return consumptionRate;
    }
    
    @Override
    protected void onEnergyTick(IModuleContext context) {
        super.onEnergyTick(context);
        
        // ALL CONSUMPTION LOGIC IS IN RUST ECS!
        // Rust handles energy consumption and work processing
        // Java just provides facade for display and external integration
    }
    
    /**
     * Check if consumer is actively working (has energy)
     */
    public boolean isWorking() {
        // Data comes from Rust ECS
        return energyStored > 0;
    }
    
    /**
     * Get work efficiency based on available energy
     */
    public double getWorkEfficiency() {
        // Could be enhanced to consider energy level
        return isWorking() ? 1.0 : 0.0;
    }
    
    /**
     * Get current consumption rate from ECS
     */
    public int getCurrentConsumptionRate() {
        // Actual consumption is managed by Rust ECS
        return isWorking() ? consumptionRate : 0;
    }
}