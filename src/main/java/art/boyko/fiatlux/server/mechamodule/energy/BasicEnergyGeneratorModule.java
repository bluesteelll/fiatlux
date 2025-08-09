package art.boyko.fiatlux.server.mechamodule.energy;

import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Basic energy generator module that produces energy over time
 * Базовый модуль-генератор который производит энергию
 */
public class BasicEnergyGeneratorModule extends AbstractEnergyModule {
    
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "basic_energy_generator");
    
    private final int generationRate;
    private int ticksSinceLastGeneration = 0;
    
    public BasicEnergyGeneratorModule() {
        this(1000, 100, 0, true, false, 10);
    }
    
    public BasicEnergyGeneratorModule(int maxCapacity, int maxExtractRate, int maxReceiveRate, 
                                    boolean canExtract, boolean canReceive, int generationRate) {
        super(MODULE_ID, maxCapacity, maxExtractRate, maxReceiveRate, canExtract, canReceive);
        this.generationRate = generationRate;
    }
    
    @Override
    protected String getEcsModuleType() {
        return "energy_generator";
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Basic Energy Generator");
    }
    
    @Override
    public int getEnergyGenerationRate() {
        return generationRate;
    }
    
    @Override
    protected void onEnergyTick(IModuleContext context) {
        super.onEnergyTick(context);
        
        // ALL LOGIC IS IN RUST ECS!
        // Java is just a facade for rendering and external integration
        // Actual energy generation, storage, and transfers are handled by Rust
        
        // Only sync display state for rendering purposes
        ticksSinceLastGeneration++;
    }
    
    /**
     * Check if generator is actively producing energy (read from ECS)
     */
    public boolean isGenerating() {
        // Data comes from Rust ECS via sync
        return energyStored < maxCapacity;
    }
    
    /**
     * Get current generation rate from ECS data
     */
    public int getCurrentGenerationRate() {
        // This value is managed by Rust ECS
        return generationRate; // Static config, actual generation is in Rust
    }
}