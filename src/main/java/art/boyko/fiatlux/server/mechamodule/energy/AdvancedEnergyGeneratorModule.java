package art.boyko.fiatlux.server.mechamodule.energy;

import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Advanced energy generator with higher capacity and configurable generation rate
 * Продвинутый генератор энергии с большей мощностью
 */
public class AdvancedEnergyGeneratorModule extends BasicEnergyGeneratorModule {
    
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "advanced_energy_generator");
    
    private boolean isOverclocked = false;
    private int baseGenerationRate;
    
    public AdvancedEnergyGeneratorModule() {
        super(2000, 200, 0, true, false, 25);
        this.baseGenerationRate = 25;
    }
    
    
    @Override
    protected String getEcsModuleType() {
        return "advanced_energy_generator";
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Advanced Energy Generator")
                .append(isOverclocked ? Component.literal(" (OC)") : Component.empty());
    }
    
    
    @Override
    public int getCurrentGenerationRate() {
        // Overclocking increases generation rate but might have drawbacks
        return isOverclocked ? (int)(baseGenerationRate * 1.5) : baseGenerationRate;
    }
    
    /**
     * Toggle overclocking mode
     * This is just for Java-side display - actual logic is in Rust ECS
     */
    public void setOverclocked(boolean overclocked) {
        if (this.isOverclocked != overclocked) {
            this.isOverclocked = overclocked;
            
            // Notify Rust ECS about configuration change
            // In a full implementation, this would update ECS component
            syncToEcs();
        }
    }
    
    public boolean isOverclocked() {
        return isOverclocked;
    }
    
    /**
     * Get overclock efficiency penalty
     */
    public double getOverclockPenalty() {
        // Example: overclocking might reduce efficiency or cause wear
        return isOverclocked ? 0.1 : 0.0;
    }
    
    @Override
    protected void onEnergyTick(IModuleContext context) {
        super.onEnergyTick(context);
        
        // Advanced generator might have special behaviors
        // But all actual logic is still in Rust ECS
        
        if (isOverclocked && context.getGameTime() % 100 == 0) {
            // Could trigger particle effects, sounds, etc.
        }
    }
    
    @Override
    protected void saveCustomData(CompoundTag tag) {
        super.saveCustomData(tag);
        tag.putBoolean("Overclocked", isOverclocked);
        tag.putInt("BaseGenerationRate", baseGenerationRate);
    }
    
    @Override
    protected void loadCustomData(CompoundTag tag) {
        super.loadCustomData(tag);
        isOverclocked = tag.getBoolean("Overclocked");
        baseGenerationRate = tag.getInt("BaseGenerationRate");
    }
}