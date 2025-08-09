package art.boyko.fiatlux.ecs;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for modules that integrate with Bevy ECS.
 * Provides automatic ECS entity creation/destruction and data synchronization.
 */
public abstract class EcsIntegratedModule extends AbstractMechaModule {
    
    @Nullable
    private EcsEntity ecsEntity;
    private boolean ecsEnabled = true;
    
    protected EcsIntegratedModule(ResourceLocation moduleId, ModuleProperties properties) {
        super(moduleId, properties);
    }
    
    /**
     * Get the ECS entity type name for this module.
     * Override in subclasses to specify the Rust ECS component setup.
     */
    protected abstract String getEcsModuleType();
    
    /**
     * Get the current ECS entity
     */
    @Nullable
    public EcsEntity getEcsEntity() {
        return ecsEntity;
    }
    
    /**
     * Check if ECS integration is enabled for this module
     */
    public boolean isEcsEnabled() {
        return ecsEnabled;
    }
    
    /**
     * Enable or disable ECS integration
     */
    public void setEcsEnabled(boolean enabled) {
        if (this.ecsEnabled == enabled) return;
        
        this.ecsEnabled = enabled;
        
        if (context != null) {
            if (enabled) {
                createEcsEntity();
            } else {
                destroyEcsEntity();
            }
        }
    }
    
    @Override
    public void onPlacedInGrid(IModuleContext context) {
        super.onPlacedInGrid(context);
        
        if (ecsEnabled) {
            createEcsEntity();
        }
    }
    
    @Override
    public void onRemovedFromGrid(IModuleContext context) {
        destroyEcsEntity();
        super.onRemovedFromGrid(context);
    }
    
    @Override
    protected void onTick(IModuleContext context) {
        super.onTick(context);
        
        // Synchronize data between Java and ECS
        if (ecsEntity != null && ecsEntity.isValid()) {
            syncToEcs();
            syncFromEcs();
        }
    }
    
    /**
     * Synchronize Java module data to ECS entity.
     * Override in subclasses to sync specific data.
     */
    protected void syncToEcs() {
        // Default implementation does nothing
    }
    
    /**
     * Synchronize ECS entity data to Java module.
     * Override in subclasses to sync specific data.
     */
    protected void syncFromEcs() {
        // Default implementation does nothing
    }
    
    /**
     * Create ECS entity for this module
     */
    private void createEcsEntity() {
        if (!ecsEnabled || context == null) return;
        
        BlockPos worldPos = context.getWorldPosition();
        IModuleContext.GridPosition gridPos = context.getGridPosition();
        
        ecsEntity = EcsManager.spawnEntity(
            worldPos, 
            getEcsModuleType(), 
            gridPos.x(), 
            gridPos.y(), 
            gridPos.z()
        );
        
        if (ecsEntity != null) {
            onEcsEntityCreated(ecsEntity);
        }
    }
    
    /**
     * Destroy ECS entity for this module
     */
    private void destroyEcsEntity() {
        if (ecsEntity != null) {
            onEcsEntityDestroyed(ecsEntity);
            
            if (context != null) {
                BlockPos worldPos = context.getWorldPosition();
                IModuleContext.GridPosition gridPos = context.getGridPosition();
                EcsManager.removeEntity(worldPos, gridPos.x(), gridPos.y(), gridPos.z());
            }
            
            ecsEntity = null;
        }
    }
    
    /**
     * Called when ECS entity is successfully created.
     * Override in subclasses to perform initialization.
     */
    protected void onEcsEntityCreated(EcsEntity entity) {
        // Default implementation does nothing
    }
    
    /**
     * Called before ECS entity is destroyed.
     * Override in subclasses to perform cleanup.
     */
    protected void onEcsEntityDestroyed(EcsEntity entity) {
        // Default implementation does nothing
    }
    
    @Override
    public CompoundTag saveToNBT() {
        CompoundTag nbt = super.saveToNBT();
        nbt.putBoolean("EcsEnabled", ecsEnabled);
        nbt.putString("EcsModuleType", getEcsModuleType());
        return nbt;
    }
    
    @Override
    public void loadFromNBT(CompoundTag nbt) {
        super.loadFromNBT(nbt);
        ecsEnabled = nbt.getBoolean("EcsEnabled");
    }
    
    /**
     * Utility method to get energy level from ECS entity
     */
    protected int getEcsEnergyLevel() {
        if (ecsEntity != null && ecsEntity.isValid()) {
            return ecsEntity.getEnergyLevel();
        }
        return -1;
    }
    
    /**
     * Utility method to set display text in ECS entity
     */
    protected boolean setEcsDisplayText(String text) {
        if (ecsEntity != null && ecsEntity.isValid()) {
            return ecsEntity.setDisplayText(text);
        }
        return false;
    }
}