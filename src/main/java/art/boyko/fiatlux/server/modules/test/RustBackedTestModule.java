package art.boyko.fiatlux.server.modules.test;

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
 * TestModule that delegates ALL LOGIC to Rust ECS.
 * This Java class is now ONLY for presentation (rendering, UI, serialization).
 * 
 * Separation of Concerns:
 * - Rust ECS: ALL logic (energy generation, transfer, consumption) 
 * - Java: ONLY presentation (block state, tooltips, item form)
 */
public class RustBackedTestModule extends AbstractMechaModule {
    
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "test_module");
    
    // PRESENTATION DATA ONLY - logic is in Rust
    private long rustEntityId = -1;  // Reference to Rust ECS entity
    private int cachedEnergy = 0;    // Cached for display only
    private int cachedMaxEnergy = 1000;
    
    public RustBackedTestModule() {
        super(MODULE_ID, createProperties());
    }
    
    private static ModuleProperties createProperties() {
        return ModuleProperties.builder()
                .needsTicking(false)  // No Java ticking - Rust handles everything!
                .canRotate(false)
                .hasDirectionalConnections(true)
                .energyProduction(10)
                .maxConnections(6)
                .hardness(2.0f)
                .build();
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
                    "test_module"  // Rust will handle all TestModule logic
                );
                
                if (rustEntityId != -1) {
                    System.out.println("🚀 Java TestModule: Created Rust entity " + rustEntityId + 
                                     " with FULL logic at [" + gridPos.x() + "," + gridPos.y() + "," + gridPos.z() + "]");
                }
            } catch (UnsatisfiedLinkError e) {
                System.err.println("⚠️ Rust ECS not available for TestModule, falling back to Java logic");
                rustEntityId = -1;
                
                // Initialize Java fallback capabilities
                addCapability(art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability.class, 
                    new art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability(this, new TestEnergyProvider()));
            }
        }
    }
    
    @Override
    public void onRemovedFromGrid(IModuleContext context) {
        // REMOVE RUST ECS ENTITY
        if (rustEntityId != -1 && context != null && context.getGridEntity() != null) {
            BlockPos worldPos = context.getGridEntity().getBlockPos();
            try {
                RustModuleManager.removeModuleAtPosition(worldPos, rustEntityId);
                System.out.println("🗑️ Java TestModule: Removed Rust entity with all logic");
            } catch (UnsatisfiedLinkError e) {
                System.err.println("⚠️ Could not remove Rust entity - Rust ECS not available");
            }
            rustEntityId = -1;
        }
        super.onRemovedFromGrid(context);
    }
    
    @Override
    public BlockState getRenderState() {
        // PRESENTATION: Update visual based on energy level from Rust
        updateCachedDataFromRust();
        
        // Visual changes based on energy level
        double fillPercent = cachedMaxEnergy > 0 ? (double) cachedEnergy / cachedMaxEnergy : 0;
        
        if (fillPercent > 0.75) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        } else if (fillPercent > 0.5) {
            return Blocks.IRON_BLOCK.defaultBlockState();
        } else if (fillPercent > 0.25) {
            return Blocks.COPPER_BLOCK.defaultBlockState();
        } else {
            return Blocks.STONE.defaultBlockState();
        }
    }
    
    @Override
    public List<Component> getTooltip() {
        // PRESENTATION: Show energy data from Rust
        updateCachedDataFromRust();
        
        List<Component> tooltip = super.getTooltip();
        tooltip.add(Component.literal("🚀 Rust-Powered TestModule"));
        tooltip.add(Component.literal("Energy: " + cachedEnergy + "/" + cachedMaxEnergy + " RF"));
        tooltip.add(Component.literal("Generation: 10 RF/t (handled in Rust)"));
        tooltip.add(Component.literal("Entity ID: " + rustEntityId));
        return tooltip;
    }
    
    @Override
    protected void saveCustomData(CompoundTag nbt) {
        // CRITICAL FIX: Update cache from Rust BEFORE saving for client sync
        updateCachedDataFromRust();
        
        // SERIALIZATION: Save presentation data for client sync
        nbt.putLong("RustEntityId", rustEntityId);
        nbt.putInt("CachedEnergy", cachedEnergy);
        nbt.putInt("CachedMaxEnergy", cachedMaxEnergy);
        
        System.out.println("💾 TestModule: Saving cached energy " + cachedEnergy + "/" + cachedMaxEnergy + " for client sync");
    }
    
    @Override
    protected void loadCustomData(CompoundTag nbt) {
        // DESERIALIZATION: Load presentation data for client display
        rustEntityId = nbt.getLong("RustEntityId");
        cachedEnergy = nbt.getInt("CachedEnergy");
        cachedMaxEnergy = nbt.getInt("CachedMaxEnergy");
        
        System.out.println("📂 TestModule: Loaded cached energy " + cachedEnergy + "/" + cachedMaxEnergy + " from sync data");
    }
    
    @Override
    public ItemStack toItemStack() {
        return new ItemStack(art.boyko.fiatlux.server.init.ModItems.TEST_MODULE_ITEM.get());
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
                        System.out.println("🔋 TestModule: Updated cache from Rust: " + cachedEnergy + "/" + cachedMaxEnergy + " RF");
                    }
                }
            } catch (UnsatisfiedLinkError e) {
                // Fallback: keep cached values or use defaults
                System.out.println("⚠️ TestModule: Cannot update cache from Rust - using cached values");
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
     * Get Rust entity ID for debugging
     */
    public long getRustEntityId() {
        return rustEntityId;
    }
    
    // ===== FALLBACK JAVA LOGIC (if Rust ECS not available) =====
    
    @Override
    public boolean needsTicking() {
        return rustEntityId == -1; // Only tick if not using Rust ECS
    }
    
    @Override
    protected void onTick(art.boyko.fiatlux.mechamodule.context.IModuleContext context) {
        if (rustEntityId != -1) return; // Skip if Rust is handling logic
        
        // Java fallback energy generation
        int oldEnergy = cachedEnergy;
        if (cachedEnergy < cachedMaxEnergy) {
            cachedEnergy = Math.min(cachedMaxEnergy, cachedEnergy + 10); // 10 RF/tick
        }
        
        if (oldEnergy != cachedEnergy) {
            // Try to distribute energy to neighbors
            distributeEnergyFallback(context);
        }
    }
    
    private void distributeEnergyFallback(art.boyko.fiatlux.mechamodule.context.IModuleContext context) {
        if (cachedEnergy <= 0) return;
        
        var energyCapabilities = context.findCapabilities(art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability.class);
        if (energyCapabilities.isEmpty()) return;
        
        int energyPerNeighbor = Math.min(cachedEnergy / energyCapabilities.size(), 50);
        if (energyPerNeighbor <= 0) return;
        
        for (var entry : energyCapabilities.entrySet()) {
            if (cachedEnergy <= 0) break;
            
            var neighborCapability = entry.getValue();
            if (neighborCapability.canReceive()) {
                int transferred = neighborCapability.receiveEnergy(energyPerNeighbor, false);
                cachedEnergy -= transferred;
                
                if (transferred > 0) {
                    context.markForRenderUpdate();
                }
            }
        }
    }
    
    @Override
    protected void onActivated() {
        System.out.println("🚀 TestModule activated - logic runs in Rust ECS");
    }
    
    @Override
    protected void onDeactivated() {
        System.out.println("🚀 TestModule deactivated - Rust ECS entity will be cleaned up");
    }
    
    /**
     * Java fallback energy provider (used when Rust ECS is not available)
     */
    public class TestEnergyProvider implements art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability.EnergyProvider {
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = Math.min(maxReceive, cachedMaxEnergy - cachedEnergy);
            
            if (!simulate && energyReceived > 0) {
                cachedEnergy += energyReceived;
            }
            
            return energyReceived;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(maxExtract, cachedEnergy);
            
            if (!simulate && energyExtracted > 0) {
                cachedEnergy -= energyExtracted;
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