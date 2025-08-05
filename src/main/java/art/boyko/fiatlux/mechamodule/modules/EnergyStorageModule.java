package art.boyko.fiatlux.mechamodule.modules;

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
        // No capabilities for now - simplified
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
        return new ItemStack(art.boyko.fiatlux.init.ModItems.ENERGY_STORAGE_MODULE_ITEM.get());
    }
}