package art.boyko.fiatlux.server.modules.energy;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.capability.IModuleCapability;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Energy generator module that produces energy from fuel
 */
public class EnergyGeneratorModule extends AbstractMechaModule {
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "energy_generator");
    
    private int energyStored = 0;
    private int maxEnergyStorage = 10000;
    private int energyGeneration = 20; // RF per tick
    private int fuelBurnTime = 0;
    private int maxBurnTime = 0;

    public EnergyGeneratorModule() {
        super(MODULE_ID, ModuleProperties.builder()
            .needsTicking(true)
            .energyProduction(20)
            .build()
        );
    }

    @Override
    protected void onTick(IModuleContext context) {
        super.onTick(context);
        
        boolean wasWorking = fuelBurnTime > 0;
        
        // Burn fuel if available
        if (fuelBurnTime > 0) {
            fuelBurnTime--;
            
            // Generate energy
            if (energyStored < maxEnergyStorage) {
                energyStored = Math.min(maxEnergyStorage, energyStored + energyGeneration);
            }
        }
        
        // Auto-refuel (simplified - in real implementation would check inventory)
        if (fuelBurnTime <= 0) {
            startBurnCycle();
        }
        
        // Mark for render update if working state changed
        boolean isWorkingNow = fuelBurnTime > 0;
        if (wasWorking != isWorkingNow) {
            context.markForRenderUpdate();
        }
    }

    private void startBurnCycle() {
        // Simplified auto-fuel for testing
        fuelBurnTime = 200; // 10 seconds
        maxBurnTime = 200;
    }


    @Override
    protected void initializeCapabilities() {
        // No capabilities for now - simplified
    }

    @Override
    protected void saveCustomData(CompoundTag tag) {
        tag.putInt("EnergyStored", energyStored);
        tag.putInt("FuelBurnTime", fuelBurnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
    }

    @Override
    protected void loadCustomData(CompoundTag tag) {
        energyStored = tag.getInt("EnergyStored");
        fuelBurnTime = tag.getInt("FuelBurnTime");
        maxBurnTime = tag.getInt("MaxBurnTime");
    }

    @Override
    public List<Component> getTooltip() {
        return List.of(
            Component.literal("Energy Generator Module"),
            Component.literal("Generates: " + energyGeneration + " RF/tick"),
            Component.literal("Stored: " + energyStored + "/" + maxEnergyStorage + " RF"),
            Component.literal("Fuel: " + fuelBurnTime + "/" + maxBurnTime + " ticks")
        );
    }

    @Override
    public BlockState getRenderState() {
        // Show very distinct blocks based on fuel state
        if (fuelBurnTime > 0) {
            // Active - show glowing orange block
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        } else {
            // Inactive - show gray block
            return Blocks.COBBLESTONE.defaultBlockState();
        }
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemStack(art.boyko.fiatlux.server.init.ModItems.ENERGY_GENERATOR_MODULE_ITEM.get());
    }
}