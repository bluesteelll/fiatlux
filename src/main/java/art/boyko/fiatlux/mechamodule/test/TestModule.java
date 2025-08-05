package art.boyko.fiatlux.mechamodule.test;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.IMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.capability.ConnectionType;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import art.boyko.fiatlux.mechamodule.capability.ModuleConnection;
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
        // Add a simple energy capability
        addCapability(TestEnergyCapability.class, new TestEnergyCapability(this));
    }
    
    @Override
    public BlockState getRenderState() {
        return Blocks.GOLD_BLOCK.defaultBlockState();
    }
    
    @Override
    protected void onActivated() {
        System.out.println("TestModule activated at position: " + 
                          (getContext() != null ? getContext().getGridPosition() : "unknown"));
    }
    
    @Override
    protected void onDeactivated() {
        System.out.println("TestModule deactivated");
    }
    
    @Override
    protected void onTick(IModuleContext context) {
        // Generate energy
        if (energyStored < maxEnergy) {
            energyStored = Math.min(maxEnergy, energyStored + energyPerTick);
        }
        
        // Try to share energy with neighbors
        distributeEnergy(context);
    }
    
    private void distributeEnergy(IModuleContext context) {
        if (energyStored <= 0) {
            return;
        }
        
        // Find neighbors with energy capabilities
        var energyCapabilities = context.findCapabilities(TestEnergyCapability.class);
        
        if (energyCapabilities.isEmpty()) {
            return;
        }
        
        int energyPerNeighbor = energyStored / energyCapabilities.size();
        if (energyPerNeighbor <= 0) {
            return;
        }
        
        for (var entry : energyCapabilities.entrySet()) {
            Direction direction = entry.getKey();
            TestEnergyCapability neighborCapability = entry.getValue();
            
            int transferred = neighborCapability.receiveEnergy(energyPerNeighbor, false);
            energyStored -= transferred;
            
            if (energyStored <= 0) {
                break;
            }
        }
    }
    
    @Override
    public void onNeighborChanged(IModuleContext context, Direction direction, @Nullable IMechaModule neighbor) {
        System.out.println("TestModule neighbor changed in direction " + direction.name() + 
                          ": " + (neighbor != null ? neighbor.getModuleId() : "removed"));
        
        if (neighbor != null) {
            // Try to establish energy connection
            context.establishConnection(direction, TestEnergyCapability.class);
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
     * Simple energy capability implementation for testing
     */
    public static class TestEnergyCapability implements IModuleCapability {
        
        private final TestModule owner;
        
        public TestEnergyCapability(TestModule owner) {
            this.owner = owner;
        }
        
        @Override
        public ResourceLocation getCapabilityId() {
            return ResourceLocation.fromNamespaceAndPath("fiatlux", "test_energy");
        }
        
        @Override
        public IMechaModule getOwnerModule() {
            return owner;
        }
        
        @Override
        public boolean canConnectTo(Direction direction, IModuleCapability other) {
            return other instanceof TestEnergyCapability;
        }
        
        @Override
        public void onConnectionEstablished(Direction direction, IModuleCapability other, ModuleConnection connection) {
            System.out.println("Energy connection established: " + direction.name());
        }
        
        @Override
        public void onConnectionBroken(Direction direction, IModuleCapability other) {
            System.out.println("Energy connection broken: " + direction.name());
        }
        
        @Override
        public boolean needsTicking() {
            return false; // Energy management is handled by the module itself
        }
        
        @Override
        public boolean supportsConnectionType(ConnectionType connectionType) {
            return connectionType.canTransferEnergy();
        }
        
        /**
         * Receive energy from another source
         * @param maxReceive Maximum amount to receive
         * @param simulate If true, don't actually receive the energy
         * @return Amount actually received
         */
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = Math.min(maxReceive, owner.maxEnergy - owner.energyStored);
            
            if (!simulate && energyReceived > 0) {
                owner.energyStored += energyReceived;
            }
            
            return energyReceived;
        }
        
        /**
         * Extract energy to another destination
         * @param maxExtract Maximum amount to extract
         * @param simulate If true, don't actually extract the energy
         * @return Amount actually extracted
         */
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(maxExtract, owner.energyStored);
            
            if (!simulate && energyExtracted > 0) {
                owner.energyStored -= energyExtracted;
            }
            
            return energyExtracted;
        }
        
        /**
         * Get the current energy stored
         */
        public int getEnergyStored() {
            return owner.energyStored;
        }
        
        /**
         * Get the maximum energy capacity
         */
        public int getMaxEnergyStored() {
            return owner.maxEnergy;
        }
        
        /**
         * Check if this capability can receive energy
         */
        public boolean canReceive() {
            return owner.energyStored < owner.maxEnergy;
        }
        
        /**
         * Check if this capability can extract energy
         */
        public boolean canExtract() {
            return owner.energyStored > 0;
        }
    }
}