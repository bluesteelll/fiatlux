package art.boyko.fiatlux.mechamodule.modules;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import art.boyko.fiatlux.mechamodule.capability.ConnectionType;
import art.boyko.fiatlux.mechamodule.capability.ModuleConnection;
import art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import net.minecraft.core.Direction;
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
        System.out.println("🆕 EnergyStorageModule CONSTRUCTOR CALLED!");
    }

    @Override
    protected void initializeCapabilities() {
        addCapability(EnergyCapability.class, new EnergyCapability(this, new StorageEnergyProvider()));
    }

    @Override
    protected void onTick(IModuleContext context) {
        super.onTick(context);
        
        // Try to establish connections with energy neighbors using unified capability
        for (Direction direction : Direction.values()) {
            if (context.hasNeighbor(direction)) {
                context.establishConnection(direction, EnergyCapability.class);
            }
        }
    }
    
    public int receiveEnergy(int amount, boolean simulate) {
        int energyReceived = Math.min(amount, Math.min(maxTransferRate, maxEnergyStorage - energyStored));
        if (!simulate && energyReceived > 0) {
            energyStored += energyReceived;
            System.out.println("💾 EnergyStorageModule: Received " + energyReceived + " RF. Total: " + energyStored + "/" + maxEnergyStorage);
            
            // Force block update for visual changes
            if (getContext() != null) {
                getContext().markForRenderUpdate();
            }
        }
        return energyReceived;
    }
    
    public int extractEnergy(int amount, boolean simulate) {
        int energyExtracted = Math.min(amount, Math.min(maxTransferRate, energyStored));
        if (!simulate) {
            energyStored -= energyExtracted;
        }
        return energyExtracted;
    }
    
    public int getEnergyStored() {
        return energyStored;
    }
    
    public int getMaxEnergyStorage() {
        return maxEnergyStorage;
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
        return new ItemStack(art.boyko.fiatlux.init.ModItems.ENERGY_STORAGE_MODULE_ITEM.get());
    }
    
    /**
     * Energy provider implementation for EnergyStorageModule
     */
    public class StorageEnergyProvider implements EnergyCapability.EnergyProvider {
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return EnergyStorageModule.this.receiveEnergy(maxReceive, simulate);
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return EnergyStorageModule.this.extractEnergy(maxExtract, simulate);
        }
        
        @Override
        public int getEnergyStored() {
            return EnergyStorageModule.this.getEnergyStored();
        }
        
        @Override
        public int getMaxEnergyStored() {
            return EnergyStorageModule.this.getMaxEnergyStorage();
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