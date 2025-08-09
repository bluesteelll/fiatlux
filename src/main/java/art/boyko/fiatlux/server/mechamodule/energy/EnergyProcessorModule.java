package art.boyko.fiatlux.server.mechamodule.energy;

import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.server.ecs.EnergyEventSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Module that consumes energy to perform processing tasks
 * Combines energy consumption with processing capabilities
 * Модуль который потребляет энергию для выполнения обработки
 */
public class EnergyProcessorModule extends BasicEnergyConsumerModule {
    
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "energy_processor");
    
    private float processingProgress = 0.0f;
    
    public EnergyProcessorModule() {
        super(100, 0, 100, false, true, 20);
    }
    
    
    @Override
    protected String getEcsModuleType() {
        return "processor"; // Uses processor type in ECS which has both energy and processing
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Energy Processor");
    }
    
    
    /**
     * Get processing speed (items per second or similar)
     */
    public float getProcessingSpeed() {
        // Processing speed depends on available energy
        return isWorking() ? 1.0f : 0.0f;
    }
    
    /**
     * Get current processing progress (0.0 to 1.0)
     * This data comes from Rust ECS processing component
     */
    public float getProcessingProgress() {
        // In real implementation, this would sync from Rust ECS processing component
        return processingProgress;
    }
    
    /**
     * Check if currently processing something
     */
    public boolean isProcessing() {
        return isWorking() && getProcessingProgress() > 0.0f;
    }
    
    @Override
    protected void onEnergyTick(IModuleContext context) {
        super.onEnergyTick(context);
        
        // ALL PROCESSING LOGIC IS IN RUST ECS!
        // This includes:
        // - Energy consumption based on processing activity
        // - Processing progress updates
        // - Item transformation/creation
        // - Processing completion events
        
        // Java just provides facade for GUI updates
        
        if (context.getGameTime() % 4 == 0) {
            // Sync processing progress from ECS (every 4 ticks to match batch processing)
            syncProcessingFromEcs();
        }
    }
    
    /**
     * Sync processing state from Rust ECS
     */
    private void syncProcessingFromEcs() {
        // In full implementation, this would query ECS for processing component state
        // For now, simulate basic processing
        if (isWorking()) {
            processingProgress += 0.05f; // Simulated progress
            if (processingProgress >= 1.0f) {
                processingProgress = 0.0f; // Reset after completion
                onProcessingComplete();
            }
        } else {
            processingProgress = 0.0f;
        }
    }
    
    /**
     * Called when processing completes
     */
    protected void onProcessingComplete() {
        // Override in subclasses for specific processing results
        // E.g., create items, transform materials, etc.
    }
    
    /**
     * Get energy cost per processing operation
     */
    public int getEnergyPerOperation() {
        return getEnergyConsumptionRate() * 20; // 20 ticks per operation
    }
    
    /**
     * Check if has enough energy for one operation
     */
    public boolean hasEnergyForOperation() {
        return getEnergyStored() >= getEnergyPerOperation();
    }
}