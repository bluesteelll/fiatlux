package art.boyko.fiatlux.mechamodule.test;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.capability.ConnectionType;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import art.boyko.fiatlux.mechamodule.capability.ModuleConnection;
import art.boyko.fiatlux.mechamodule.capability.standard.EnergyCapability;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Simple test module for demonstrating the MechaModule system.
 * This module generates energy and can connect to other modules.
 */
public class TestModule extends AbstractMechaModule {
    
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "test_module");
    
    private int energyStored = 0;
    private final int maxEnergy = 1000;
    private final int energyPerTick = 10;
    
    public TestModule() {
        super(MODULE_ID, createProperties());
    }
    
    private static ModuleProperties createProperties() {
        return ModuleProperties.builder()
                .needsTicking(true)
                .canRotate(false)
                .hasDirectionalConnections(true)
                .energyProduction(10)
                .maxConnections(6)
                .hardness(2.0f)
                .build();
    }
    
    @Override
    protected void initializeCapabilities() {
        // Add unified energy capability
        addCapability(EnergyCapability.class, new EnergyCapability(this, new TestEnergyProvider()));
    }
    
    @Override
    public BlockState getRenderState() {
        return Blocks.GOLD_BLOCK.defaultBlockState();
    }
    
    @Override
    protected void onActivated() {
        // Module activated - ready for operation
    }
    
    @Override
    public void onPlacedInGrid(art.boyko.fiatlux.mechamodule.context.IModuleContext context) {
        super.onPlacedInGrid(context);
    }
    
    @Override
    protected void onDeactivated() {
        // Module deactivated
    }
    
    @Override
    protected void onTick(IModuleContext context) {
        // Generate energy
        int oldEnergy = energyStored;
        if (energyStored < maxEnergy) {
            energyStored = Math.min(maxEnergy, energyStored + energyPerTick);
        }
        
        // Energy generated successfully
        
        // Try to share energy with neighbors
        distributeEnergy(context);
    }
    
    private void distributeEnergy(IModuleContext context) {
        if (energyStored <= 0) {
            return;
        }
        
        // Try to distribute energy to neighbors
        
        // Find neighbors with energy capabilities (now unified!)
        var energyCapabilities = context.findCapabilities(EnergyCapability.class);
        
        if (energyCapabilities.isEmpty()) {
            return;
        }
        
        int energyPerNeighbor = Math.min(energyStored / energyCapabilities.size(), 50); // Max 50 RF per neighbor per tick
        if (energyPerNeighbor <= 0) {
            return;
        }
        
        // Distribute energy to all neighbors with energy capabilities
        for (var entry : energyCapabilities.entrySet()) {
            if (energyStored <= 0) break;
            
            Direction direction = entry.getKey();
            EnergyCapability neighborCapability = entry.getValue();
            
            // Only transfer to neighbors that can receive energy
            if (neighborCapability.canReceive()) {
                int transferred = neighborCapability.receiveEnergy(energyPerNeighbor, false);
                energyStored -= transferred;
                
                // Energy transferred successfully
            }
        }
    }
    
    @Override
    public void onNeighborChanged(IModuleContext context, Direction direction, @Nullable IMechaModule neighbor) {
        if (neighbor != null) {
            // Try to establish energy connection with unified capability
            context.establishConnection(direction, EnergyCapability.class);
        }
    }
    
    @Override
    protected void saveCustomData(CompoundTag nbt) {
        nbt.putInt("EnergyStored", energyStored);
    }
    
    @Override
    protected void loadCustomData(CompoundTag nbt) {
        energyStored = nbt.getInt("EnergyStored");
    }
    
    @Override
    public ItemStack toItemStack() {
        return new ItemStack(art.boyko.fiatlux.init.ModItems.TEST_MODULE_ITEM.get());
    }
    
    @Override
    public List<Component> getTooltip() {
        List<Component> tooltip = super.getTooltip();
        tooltip.add(Component.literal("Energy: " + energyStored + "/" + maxEnergy));
        tooltip.add(Component.literal("Generation: " + energyPerTick + " RF/t"));
        return tooltip;
    }
    
    // Getter for energy (for UI display)
    public int getEnergyStored() {
        return energyStored;
    }
    
    public int getMaxEnergy() {
        return maxEnergy;
    }
    
    /**
     * Energy provider implementation for TestModule
     */
    public class TestEnergyProvider implements EnergyCapability.EnergyProvider {
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = Math.min(maxReceive, maxEnergy - energyStored);
            
            if (!simulate && energyReceived > 0) {
                energyStored += energyReceived;
            }
            
            return energyReceived;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(maxExtract, energyStored);
            
            if (!simulate && energyExtracted > 0) {
                energyStored -= energyExtracted;
            }
            
            return energyExtracted;
        }
        
        @Override
        public int getEnergyStored() {
            return energyStored;
        }
        
        @Override
        public int getMaxEnergyStored() {
            return maxEnergy;
        }
        
        @Override
        public boolean canReceive() {
            return energyStored < maxEnergy;
        }
        
        @Override
        public boolean canExtract() {
            return energyStored > 0;
        }
    }
}