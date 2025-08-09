package art.boyko.fiatlux.common.module;

import art.boyko.fiatlux.mechamodule.base.AbstractMechaModule;
import art.boyko.fiatlux.mechamodule.base.ModuleProperties;
import art.boyko.fiatlux.mechamodule.context.IModuleContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Processor module that consumes energy to perform work
 */
public class ProcessorModule extends AbstractMechaModule {
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "processor");
    
    private int energyBuffer = 0;
    private int maxEnergyBuffer = 1000;
    private int energyConsumption = 50; // RF per tick when working
    private int workProgress = 0;
    private int maxWorkProgress = 100; // 5 seconds of work
    private boolean isWorking = false;

    public ProcessorModule() {
        super(MODULE_ID, ModuleProperties.builder()
            .needsTicking(true)
            .energyConsumption(50)
            .build()
        );
    }

    @Override
    protected void initializeCapabilities() {
        // No capabilities for now - simplified
    }

    @Override
    protected void onTick(IModuleContext context) {
        super.onTick(context);
        
        // Add some energy for testing (normally would come from energy modules)
        energyBuffer = Math.min(maxEnergyBuffer, energyBuffer + 25); // Add 25 RF per tick for testing
        
        boolean wasWorking = isWorking;
        int oldProgress = workProgress;
        
        // Work if we have enough energy
        if (energyBuffer >= energyConsumption) {
            if (!isWorking) {
                isWorking = true;
                workProgress = 0;
            }
            
            energyBuffer -= energyConsumption;
            workProgress++;
            
            if (workProgress >= maxWorkProgress) {
                // Work completed
                completeWork();
                workProgress = 0;
                isWorking = false;
            }
        } else {
            isWorking = false;
        }
        
        // Mark for render update if state changed significantly
        boolean progressChanged = (oldProgress / 25) != (workProgress / 25); // Update every 25% progress
        if (wasWorking != isWorking || progressChanged) {
            context.markForRenderUpdate();
        }
    }

    private void completeWork() {
        // Placeholder for actual work completion
        // In a real implementation, this might produce items, send data, etc.
    }

    @Override
    protected void saveCustomData(CompoundTag tag) {
        tag.putInt("EnergyBuffer", energyBuffer);
        tag.putInt("WorkProgress", workProgress);
        tag.putBoolean("IsWorking", isWorking);
    }

    @Override
    protected void loadCustomData(CompoundTag tag) {
        energyBuffer = tag.getInt("EnergyBuffer");
        workProgress = tag.getInt("WorkProgress");
        isWorking = tag.getBoolean("IsWorking");
    }

    @Override
    public List<Component> getTooltip() {
        return List.of(
            Component.literal("Processor Module"),
            Component.literal("Consumes: " + energyConsumption + " RF/tick"),
            Component.literal("Energy: " + energyBuffer + "/" + maxEnergyBuffer + " RF"),
            Component.literal(isWorking ? "Working: " + workProgress + "/" + maxWorkProgress : "Idle"),
            Component.literal("Status: " + (isWorking ? "Processing" : "Waiting for Energy"))
        );
    }

    @Override
    public BlockState getRenderState() {
        if (isWorking) {
            // Show very distinct colors based on work progress
            double progress = (double) workProgress / maxWorkProgress;
            if (progress > 0.75) {
                return Blocks.REDSTONE_BLOCK.defaultBlockState(); // Red - almost done
            } else if (progress > 0.5) {
                return Blocks.LAPIS_BLOCK.defaultBlockState(); // Blue - 75%
            } else if (progress > 0.25) {
                return Blocks.GOLD_BLOCK.defaultBlockState(); // Yellow - 50%
            } else {
                return Blocks.IRON_BLOCK.defaultBlockState(); // Gray - 25%
            }
        } else {
            // Idle - show distinct idle block
            return Blocks.OBSIDIAN.defaultBlockState();
        }
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemStack(art.boyko.fiatlux.server.init.ModItems.PROCESSOR_MODULE_ITEM.get());
    }
}