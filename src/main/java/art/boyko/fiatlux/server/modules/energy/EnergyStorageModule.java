package art.boyko.fiatlux.server.modules.energy;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Energy storage module that can store and distribute energy
 */
public class EnergyStorageModule extends AbstractMechaModule {
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "energy_storage");
    
    private int energyStored = 0;
    private int maxEnergyStorage = 50000;
    private int maxTransferRate = 100; // RF per tick

    public EnergyStorageModule() {
        super(MODULE_ID, ModuleProperties.builder()
            .needsTicking(true)
            .build()
        );
    }

    @Override
    protected void initializeCapabilities() {
        // Add energy storage capability
        addCapability(EnergyCapability.class, new EnergyCapability(this, new StorageEnergyProvider(this)));
    }

    @Override
    protected void onTick(IModuleContext context) {
        super.onTick(context);
        // Simplified for now
    }

    @Override
    protected void saveCustomData(CompoundTag tag) {
        tag.putInt("EnergyStored", energyStored);
    }

    @Override
    protected void loadCustomData(CompoundTag tag) {
        energyStored = tag.getInt("EnergyStored");
    }

    @Override
    public List<Component> getTooltip() {
        double fillPercent = maxEnergyStorage > 0 ? (double) energyStored / maxEnergyStorage * 100 : 0;
        return List.of(
            Component.literal("Energy Storage Module"),
            Component.literal("Capacity: " + maxEnergyStorage + " RF"),
            Component.literal("Stored: " + energyStored + " RF (" + String.format("%.1f", fillPercent) + "%)"),
            Component.literal("Transfer Rate: " + maxTransferRate + " RF/tick")
        );
    }

    @Override
    public BlockState getRenderState() {
        // Change appearance based on energy level
        double fillPercent = maxEnergyStorage > 0 ? (double) energyStored / maxEnergyStorage : 0;
        
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
    public ItemStack toItemStack() {
        return new ItemStack(art.boyko.fiatlux.server.init.ModItems.ENERGY_STORAGE_MODULE_ITEM.get());
    }
    
    /**
     * Update energy from Rust ECS synchronization without triggering additional sync
     * This is called by the high-performance batch sync system
     */
    public void updateEnergyFromSync(int newEnergy) {
        this.energyStored = Math.max(0, Math.min(maxEnergyStorage, newEnergy));
        // No markForRenderUpdate call - sync is handled by batch system
        System.out.println("🔄 EnergyStorageModule: Energy updated from sync to " + energyStored);
    }
    
    // Getter for energy stored (for sync system)
    public int getEnergyStored() {
        return energyStored;
    }
    
    public int getMaxEnergyStorage() {
        return maxEnergyStorage;
    }

    /**
     * Energy provider implementation for EnergyStorageModule
     */
    public class StorageEnergyProvider implements EnergyCapability.EnergyProvider {
        
        private final EnergyStorageModule module;
        
        public StorageEnergyProvider(EnergyStorageModule module) {
            this.module = module;
        }
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = Math.min(maxReceive, Math.min(maxTransferRate, maxEnergyStorage - energyStored));
            
            if (!simulate && energyReceived > 0) {
                energyStored += energyReceived;
                
                // HIGH-PERFORMANCE: Energy changes are now synchronized via Rust ECS batch system
                // This eliminates ~20 individual sync calls per second per module
                // Rust ECS detects energy changes and batches them for 4x/sec efficient sync
                System.out.println("💾 EnergyStorage: Received " + energyReceived + " RF, now: " + energyStored);
            }
            
            return energyReceived;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(maxExtract, Math.min(maxTransferRate, energyStored));
            
            if (!simulate && energyExtracted > 0) {
                energyStored -= energyExtracted;
                
                // HIGH-PERFORMANCE: Energy changes are now synchronized via Rust ECS batch system  
                // This eliminates ~20 individual sync calls per second per module
                // Rust ECS detects energy changes and batches them for 4x/sec efficient sync
                System.out.println("💾 EnergyStorage: Extracted " + energyExtracted + " RF, now: " + energyStored);
            }
            
            return energyExtracted;
        }
        
        @Override
        public int getEnergyStored() {
            return energyStored;
        }
        
        @Override
        public int getMaxEnergyStored() {
            return maxEnergyStorage;
        }
        
        @Override
        public boolean canReceive() {
            return energyStored < maxEnergyStorage;
        }
        
        @Override
        public boolean canExtract() {
            return energyStored > 0;
        }
    }
}