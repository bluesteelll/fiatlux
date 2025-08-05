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
 * Display module that cycles through different colored blocks for easy visual testing
 */
public class DisplayModule extends AbstractMechaModule {
    public static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath("fiatlux", "display");
    
    private int colorIndex = 0;
    private int tickCounter = 0;
    private final int changeInterval = 60; // Change color every 3 seconds (60 ticks)
    
    // Color blocks to cycle through
    private final BlockState[] colorBlocks = {
        Blocks.WHITE_CONCRETE.defaultBlockState(),
        Blocks.ORANGE_CONCRETE.defaultBlockState(),
        Blocks.MAGENTA_CONCRETE.defaultBlockState(),
        Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
        Blocks.YELLOW_CONCRETE.defaultBlockState(),
        Blocks.LIME_CONCRETE.defaultBlockState(),
        Blocks.PINK_CONCRETE.defaultBlockState(),
        Blocks.GRAY_CONCRETE.defaultBlockState(),
        Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(),
        Blocks.CYAN_CONCRETE.defaultBlockState(),
        Blocks.PURPLE_CONCRETE.defaultBlockState(),
        Blocks.BLUE_CONCRETE.defaultBlockState(),
        Blocks.BROWN_CONCRETE.defaultBlockState(),
        Blocks.GREEN_CONCRETE.defaultBlockState(),
        Blocks.RED_CONCRETE.defaultBlockState(),
        Blocks.BLACK_CONCRETE.defaultBlockState()
    };

    public DisplayModule() {
        super(MODULE_ID, ModuleProperties.builder()
            .needsTicking(true)
            .build()
        );
    }

    @Override
    protected void initializeCapabilities() {
        // No capabilities - just for display
    }

    @Override
    protected void onTick(IModuleContext context) {
        super.onTick(context);
        
        tickCounter++;
        
        if (tickCounter >= changeInterval) {
            int oldColorIndex = colorIndex;
            colorIndex = (colorIndex + 1) % colorBlocks.length;
            tickCounter = 0;
            
            // Update render if color changed
            if (oldColorIndex != colorIndex) {
                context.markForRenderUpdate();
            }
        }
    }

    @Override
    protected void saveCustomData(CompoundTag tag) {
        tag.putInt("ColorIndex", colorIndex);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    protected void loadCustomData(CompoundTag tag) {
        colorIndex = tag.getInt("ColorIndex");
        tickCounter = tag.getInt("TickCounter");
    }

    @Override
    public List<Component> getTooltip() {
        String[] colorNames = {
            "White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", 
            "Pink", "Gray", "Light Gray", "Cyan", "Purple", "Blue", 
            "Brown", "Green", "Red", "Black"
        };
        
        return List.of(
            Component.literal("Display Module"),
            Component.literal("Cycles through colors every 3 seconds"),
            Component.literal("Current: " + colorNames[colorIndex % colorNames.length]),
            Component.literal("Next change in: " + (changeInterval - tickCounter) + " ticks")
        );
    }

    @Override
    public BlockState getRenderState() {
        return colorBlocks[colorIndex % colorBlocks.length];
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemStack(art.boyko.fiatlux.init.ModItems.DISPLAY_MODULE_ITEM.get());
    }
}