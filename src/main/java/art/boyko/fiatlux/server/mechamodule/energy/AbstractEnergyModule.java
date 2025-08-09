package art.boyko.fiatlux.server.mechamodule.energy;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.server.ecs.EcsManager;
import art.boyko.fiatlux.server.ecs.EnergyEventSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * Abstract base class for all energy-handling modules
 * Базовый класс для всех модулей работающих с энергией
 */
public abstract class AbstractEnergyModule extends AbstractMechaModule implements IEnergyStorage {
    
    protected int energyStored = 0;
    protected int maxCapacity;
    protected int maxExtractRate;
    protected int maxReceiveRate;
    protected boolean canExtract;
    protected boolean canReceive;
    
    // ECS integration
    protected long ecsEntityId = -1;
    protected long ecsWorldId = -1;
    
    public AbstractEnergyModule(ResourceLocation moduleId, int maxCapacity, int maxExtractRate, int maxReceiveRate, 
                               boolean canExtract, boolean canReceive) {
        super(moduleId, createEnergyProperties(maxCapacity, maxExtractRate, maxReceiveRate, canExtract, canReceive));
        this.maxCapacity = maxCapacity;
        this.maxExtractRate = maxExtractRate;
        this.maxReceiveRate = maxReceiveRate;
        this.canExtract = canExtract;
        this.canReceive = canReceive;
    }
    
    /**
     * Create ModuleProperties for energy modules
     */
    private static ModuleProperties createEnergyProperties(int maxCapacity, int maxExtractRate, int maxReceiveRate, 
                                                         boolean canExtract, boolean canReceive) {
        return ModuleProperties.builder()
                .needsTicking(true) // Energy modules need ticking for ECS sync
                .canRotate(false)
                .hasDirectionalConnections(true) // Energy modules connect to neighbors
                .energyConsumption(canReceive && !canExtract ? maxReceiveRate : 0)
                .energyProduction(canExtract && !canReceive ? maxExtractRate : 0)
                .maxConnections(6) // Can connect in all directions
                .rarity(Rarity.COMMON)
                .hardness(2.0f)
                .translucent(false)
                .build();
    }
    
    @Override
    public void onPlacedInGrid(IModuleContext context) {
        super.onPlacedInGrid(context);
        
        // Register with ECS
        registerWithEcs(context);
    }
    
    @Override
    public void onRemovedFromGrid(IModuleContext context) {
        // Unregister from ECS
        if (ecsEntityId >= 0 && ecsWorldId >= 0) {
            EnergyEventSystem.clearCachedData(ecsEntityId);
        }
        super.onRemovedFromGrid(context);
    }
    
    /**
     * Register this module with the Rust ECS system
     */
    protected void registerWithEcs(IModuleContext context) {
        BlockPos worldPos = context.getWorldPosition(); // Get grid block position
        var gridPos = context.getGridPosition(); // Get position within grid
        
        ecsWorldId = EcsManager.getWorldId(worldPos);
        if (ecsWorldId >= 0) {
            var entity = EcsManager.spawnEntity(worldPos, getEcsModuleType(), 
                                              gridPos.x(), gridPos.y(), gridPos.z());
            if (entity != null) {
                ecsEntityId = entity.getId();
                
                // Sync initial energy state to ECS
                syncToEcs();
            }
        }
    }
    
    /**
     * Get the ECS module type identifier for this energy module
     */
    protected abstract String getEcsModuleType();
    
    /**
     * Synchronize Java energy state to Rust ECS
     */
    protected void syncToEcs() {
        if (ecsEntityId >= 0 && ecsWorldId >= 0) {
            EnergyEventSystem.setEnergyLevel(ecsWorldId, ecsEntityId, energyStored);
        }
    }
    
    /**
     * Synchronize energy state from Rust ECS to Java
     */
    protected void syncFromEcs() {
        if (ecsEntityId >= 0 && ecsWorldId >= 0) {
            var cachedData = EnergyEventSystem.getCachedEnergyData(ecsEntityId);
            if (cachedData != null) {
                energyStored = cachedData.currentEnergy;
            }
        }
    }
    
    // IEnergyStorage implementation
    
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) {
            return 0;
        }
        
        int extractable = Math.min(maxExtract, Math.min(maxExtractRate, energyStored));
        
        if (!simulate && extractable > 0) {
            energyStored -= extractable;
            syncToEcs();
        }
        
        return extractable;
    }
    
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0) {
            return 0;
        }
        
        int receivable = Math.min(maxReceive, Math.min(maxReceiveRate, maxCapacity - energyStored));
        
        if (!simulate && receivable > 0) {
            energyStored += receivable;
            syncToEcs();
        }
        
        return receivable;
    }
    
    @Override
    public boolean canExtract() {
        return canExtract && energyStored > 0;
    }
    
    @Override
    public boolean canReceive() {
        return canReceive && energyStored < maxCapacity;
    }
    
    @Override
    public int getEnergyStored() {
        return energyStored;
    }
    
    @Override
    public int getMaxEnergyStored() {
        return maxCapacity;
    }
    
    @Override
    public int getMaxExtractRate() {
        return maxExtractRate;
    }
    
    @Override
    public int getMaxReceiveRate() {
        return maxReceiveRate;
    }
    
    @Override
    public void setEnergyStored(int energy) {
        energyStored = Math.max(0, Math.min(energy, maxCapacity));
        syncToEcs();
    }
    
    // Required abstract method implementation
    @Override
    protected void initializeCapabilities() {
        // Energy modules can add specific capabilities here
        // For now, energy functionality is handled through the interface methods
    }
    
    // Required IMechaModule method implementation
    @Override
    public ItemStack toItemStack() {
        // Create ItemStack from this module - need to find the corresponding item
        // This is a placeholder - in real implementation, would get from ModuleRegistry
        return new ItemStack(net.minecraft.world.level.block.Blocks.STONE);
    }
    
    // Tick processing - sync with ECS
    @Override
    protected void onTick(IModuleContext context) {
        // Sync energy state from ECS (where the actual processing happens)
        // Use game time for batch processing sync
        if (context.getGameTime() % 4 == 0) { // Sync every 4 ticks to match ECS batch processing
            syncFromEcs();
        }
        
        // Call energy-specific tick
        onEnergyTick(context);
    }
    
    /**
     * Called every tick for energy-specific logic
     * Override for custom energy behavior
     */
    protected void onEnergyTick(IModuleContext context) {
        // Override in subclasses
    }
    
    // NBT Serialization using AbstractMechaModule's custom data methods
    
    @Override
    protected void saveCustomData(CompoundTag tag) {
        tag.putInt("EnergyStored", energyStored);
        tag.putInt("MaxCapacity", maxCapacity);
        tag.putInt("MaxExtractRate", maxExtractRate);
        tag.putInt("MaxReceiveRate", maxReceiveRate);
        tag.putBoolean("CanExtract", canExtract);
        tag.putBoolean("CanReceive", canReceive);
        tag.putLong("EcsEntityId", ecsEntityId);
    }
    
    @Override
    protected void loadCustomData(CompoundTag tag) {
        energyStored = tag.getInt("EnergyStored");
        maxCapacity = tag.getInt("MaxCapacity");
        maxExtractRate = tag.getInt("MaxExtractRate");
        maxReceiveRate = tag.getInt("MaxReceiveRate");
        canExtract = tag.getBoolean("CanExtract");
        canReceive = tag.getBoolean("CanReceive");
        ecsEntityId = tag.getLong("EcsEntityId");
        
        // Re-sync after loading
        syncToEcs();
    }
    
    // Capability integration for NeoForge compatibility
    
    /**
     * Get NeoForge IEnergyStorage capability
     * This bridges our custom energy system with NeoForge's standard
     */
    public net.neoforged.neoforge.energy.IEnergyStorage getForgeEnergyCapability() {
        return new ForgeEnergyAdapter(this);
    }
    
    /**
     * Adapter class to bridge our energy system with NeoForge's IEnergyStorage
     */
    protected static class ForgeEnergyAdapter implements net.neoforged.neoforge.energy.IEnergyStorage {
        private final IEnergyStorage energyModule;
        
        public ForgeEnergyAdapter(IEnergyStorage energyModule) {
            this.energyModule = energyModule;
        }
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return energyModule.receiveEnergy(maxReceive, simulate);
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return energyModule.extractEnergy(maxExtract, simulate);
        }
        
        @Override
        public int getEnergyStored() {
            return energyModule.getEnergyStored();
        }
        
        @Override
        public int getMaxEnergyStored() {
            return energyModule.getMaxEnergyStored();
        }
        
        @Override
        public boolean canExtract() {
            return energyModule.canExtract();
        }
        
        @Override
        public boolean canReceive() {
            return energyModule.canReceive();
        }
    }
}