package art.boyko.fiatlux.server.modules.energy;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.ecs.RustModuleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * EnergyStorageModule that delegates ALL LOGIC to Rust ECS.
 * This Java class is now ONLY for presentation (rendering, UI, serialization).
 * 
 * Separation of Concerns:
 * - Rust ECS: ALL logic (energy storage, transfer in/out)
 * - Java: ONLY presentation (block state based on fill level, tooltips)
 */
public class RustBackedEnergyStorageModule extends AbstractMechaModule {
    
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "energy_storage");
    
    // PRESENTATION DATA ONLY - logic is in Rust
    private long rustEntityId = -1;  // Reference to Rust ECS entity
    private int cachedEnergy = 0;    // Cached for display only
    private int cachedMaxEnergy = 50000;
    private int cachedTransferRate = 100;
    
    public RustBackedEnergyStorageModule() {
        super(MODULE_ID, ModuleProperties.builder()
            .needsTicking(false)  // No Java ticking - Rust handles everything!
            .build()
        );
    }

    @Override
    protected void initializeCapabilities() {
        // NO CAPABILITIES - all energy logic is in Rust ECS
        // Java no longer handles energy operations
    }
    
    @Override
    public void onPlacedInGrid(IModuleContext context) {
        super.onPlacedInGrid(context);
        
        // CREATE RUST ECS ENTITY WITH ALL LOGIC
        if (context != null && context.getGridEntity() != null) {
            BlockPos worldPos = context.getGridEntity().getBlockPos();
            var gridPos = context.getGridPosition();
            
            try {
                rustEntityId = RustModuleManager.createModuleAtPosition(
                    worldPos,
                    gridPos.x(), gridPos.y(), gridPos.z(),
                    "energy_storage"  // Rust will handle all EnergyStorage logic
                );
                
                if (rustEntityId != -1) {
                    System.out.println("🚀 Java EnergyStorage: Created Rust entity " + rustEntityId + 
                                     " with FULL logic at [" + gridPos.x() + "," + gridPos.y() + "," + gridPos.z() + "]");
                }
            } catch (UnsatisfiedLinkError e) {
                System.err.println("⚠️ Rust ECS not available for EnergyStorage, falling back to Java logic");
                rustEntityId = -1;
                
                // Initialize Java fallback capabilities
                addCapability(art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability.class,
                    new art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability(this, new StorageEnergyProvider()));
            }
        }
    }
    
    @Override
    public void onRemovedFromGrid(IModuleContext context) {
        // REMOVE RUST ECS ENTITY
        if (rustEntityId != -1 && context != null && context.getGridEntity() != null) {
            BlockPos worldPos = context.getGridEntity().getBlockPos();
            RustModuleManager.removeModuleAtPosition(worldPos, rustEntityId);
            rustEntityId = -1;
            System.out.println("🗑️ Java EnergyStorage: Removed Rust entity with all logic");
        }
        super.onRemovedFromGrid(context);
    }

    @Override
    public BlockState getRenderState() {
        // PRESENTATION: Update visual based on energy level from Rust
        updateCachedDataFromRust();
        
        // Change appearance based on energy level
        double fillPercent = cachedMaxEnergy > 0 ? (double) cachedEnergy / cachedMaxEnergy : 0;
        
        if (fillPercent > 0.75) {
            return Blocks.EMERALD_BLOCK.defaultBlockState();
        } else if (fillPercent > 0.5) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        } else if (fillPercent > 0.25) {
            return Blocks.IRON_BLOCK.defaultBlockState();
        } else if (fillPercent > 0) {
            return Blocks.COPPER_BLOCK.defaultBlockState();
        } else {
            return Blocks.STONE.defaultBlockState();
        }
    }

    @Override
    public List<Component> getTooltip() {
        // PRESENTATION: Show energy data from Rust
        updateCachedDataFromRust();
        
        double fillPercent = cachedMaxEnergy > 0 ? (double) cachedEnergy / cachedMaxEnergy * 100 : 0;
        return List.of(
            Component.literal("🚀 Rust-Powered Energy Storage"),
            Component.literal("Capacity: " + cachedMaxEnergy + " RF"),
            Component.literal("Stored: " + cachedEnergy + " RF (" + String.format("%.1f", fillPercent) + "%)"),
            Component.literal("Transfer Rate: " + cachedTransferRate + " RF/tick"),
            Component.literal("Entity ID: " + rustEntityId)
        );
    }

    @Override
    protected void saveCustomData(CompoundTag tag) {
        // CRITICAL FIX: Update cache from Rust BEFORE saving for client sync
        updateCachedDataFromRust();
        
        // SERIALIZATION: Save presentation data for client sync
        tag.putLong("RustEntityId", rustEntityId);
        tag.putInt("CachedEnergy", cachedEnergy);
        tag.putInt("CachedMaxEnergy", cachedMaxEnergy);
        tag.putInt("CachedTransferRate", cachedTransferRate);
        
        System.out.println("💾 EnergyStorage: Saving cached energy " + cachedEnergy + "/" + cachedMaxEnergy + " for client sync");
    }

    @Override
    protected void loadCustomData(CompoundTag tag) {
        // DESERIALIZATION: Load presentation data for client display
        rustEntityId = tag.getLong("RustEntityId");
        cachedEnergy = tag.getInt("CachedEnergy");
        cachedMaxEnergy = tag.getInt("CachedMaxEnergy");
        cachedTransferRate = tag.getInt("CachedTransferRate");
        
        System.out.println("📂 EnergyStorage: Loaded cached energy " + cachedEnergy + "/" + cachedMaxEnergy + " from sync data");
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemStack(art.boyko.fiatlux.server.init.ModItems.ENERGY_STORAGE_MODULE_ITEM.get());
    }
    
    // ===== PRESENTATION HELPER METHODS =====
    
    /**
     * Force update cached data from Rust ECS (called by MechaGridBlockEntity during sync)
     */
    public void forceCacheUpdateFromRust() {
        updateCachedDataFromRust();
    }
    
    /**
     * Update cached data from Rust ECS for display purposes
     */
    private void updateCachedDataFromRust() {
        if (rustEntityId == -1 || getContext() == null || getContext().getGridEntity() == null) {
            return;
        }
        
        // Only try to get data from Rust on server side
        if (getContext().getGridEntity().getLevel() != null && !getContext().getGridEntity().getLevel().isClientSide()) {
            BlockPos worldPos = getContext().getGridEntity().getBlockPos();
            try {
                var energyData = RustModuleManager.getModuleEnergyData(worldPos, rustEntityId);
                
                if (energyData != null) {
                    if (cachedEnergy != energyData.current || cachedMaxEnergy != energyData.maxCapacity) {
                        cachedEnergy = energyData.current;
                        cachedMaxEnergy = energyData.maxCapacity;
                        System.out.println("🔋 EnergyStorage: Updated cache from Rust: " + cachedEnergy + "/" + cachedMaxEnergy + " RF");
                    }
                }
            } catch (UnsatisfiedLinkError e) {
                // Fallback: keep cached values or use defaults
                System.out.println("⚠️ EnergyStorage: Cannot update cache from Rust - using cached values");
            }
        }
        // On client side, use cached values from sync data
    }
    
    /**
     * Get cached energy for display (NOT the actual energy - that's in Rust)
     */
    public int getCachedEnergy() {
        return cachedEnergy;
    }
    
    /**
     * Get cached max energy for display
     */
    public int getCachedMaxEnergy() {
        return cachedMaxEnergy;
    }
    
    /**
     * Get Rust entity ID for debugging
     */
    public long getRustEntityId() {
        return rustEntityId;
    }
    
    // ===== REMOVED METHODS - ALL LOGIC NOW IN RUST =====
    
    // ❌ REMOVED: onTick() - Rust handles energy management
    // ❌ REMOVED: receiveEnergy() - Rust handles energy input
    // ❌ REMOVED: extractEnergy() - Rust handles energy output
    // ❌ REMOVED: StorageEnergyProvider class - Rust ECS manages energy
    // ❌ REMOVED: updateEnergyFromSync() - Rust manages synchronization
    // ❌ REMOVED: All energy capabilities - Rust ECS handles everything
    
    @Override
    protected void onActivated() {
        System.out.println("🚀 EnergyStorage activated - logic runs in Rust ECS");
    }
    
    @Override
    protected void onDeactivated() {
        System.out.println("🚀 EnergyStorage deactivated - Rust ECS entity will be cleaned up");
    }
    
    /**
     * Java fallback energy provider for storage (used when Rust ECS is not available)
     */
    public class StorageEnergyProvider implements art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability.EnergyProvider {
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = Math.min(maxReceive, Math.min(cachedTransferRate, cachedMaxEnergy - cachedEnergy));
            
            if (!simulate && energyReceived > 0) {
                cachedEnergy += energyReceived;
                System.out.println("💾 EnergyStorage: Received " + energyReceived + " RF, now: " + cachedEnergy);
            }
            
            return energyReceived;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(maxExtract, Math.min(cachedTransferRate, cachedEnergy));
            
            if (!simulate && energyExtracted > 0) {
                cachedEnergy -= energyExtracted;
                System.out.println("💾 EnergyStorage: Extracted " + energyExtracted + " RF, now: " + cachedEnergy);
            }
            
            return energyExtracted;
        }
        
        @Override
        public int getEnergyStored() {
            return cachedEnergy;
        }
        
        @Override
        public int getMaxEnergyStored() {
            return cachedMaxEnergy;
        }
        
        @Override
        public boolean canReceive() {
            return cachedEnergy < cachedMaxEnergy;
        }
        
        @Override
        public boolean canExtract() {
            return cachedEnergy > 0;
        }
    }
}