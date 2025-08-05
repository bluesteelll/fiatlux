package art.boyko.fiatlux.init;

import art.boyko.fiatlux.FiatLux;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "fiatlux" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FiatLux.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FIAT_LUX_TAB = 
        CREATIVE_MODE_TABS.register("fiat_lux_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fiatlux"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.MAGIC_GEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {

                output.accept(ModItems.EXAMPLE_ITEM.get());
                output.accept(ModItems.MAGIC_GEM.get());
                output.accept(ModItems.LIGHT_SWORD.get());
                output.accept(ModItems.LIGHT_CRYSTAL.get());
                output.accept(ModItems.COMPRESSED_COAL.get());
                output.accept(ModItems.TORCH_ITEM.get());
                
                output.accept(ModItems.EXAMPLE_BLOCK_ITEM.get());
                output.accept(ModItems.LIGHT_BLOCK_ITEM.get());
                output.accept(ModItems.DECORATIVE_BLOCK_ITEM.get());
                output.accept(ModItems.REINFORCED_BLOCK_ITEM.get());
                
                // Add new blocks with BlockEntity
                output.accept(ModItems.SIMPLE_STORAGE_BLOCK_ITEM.get());
                output.accept(ModItems.ENERGY_STORAGE_BLOCK_ITEM.get());
                
                // Add MechaGrid block
                output.accept(ModItems.MECHA_GRID_BLOCK_ITEM.get());
            }).build());

    // Alternative tab focused on blocks only 
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FIAT_LUX_BLOCKS_TAB = 
        CREATIVE_MODE_TABS.register("fiat_lux_blocks_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fiatlux.blocks"))
            .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
            .icon(() -> ModItems.LIGHT_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Add only block items
                output.accept(ModItems.EXAMPLE_BLOCK_ITEM.get());
                output.accept(ModItems.LIGHT_BLOCK_ITEM.get());
                output.accept(ModItems.DECORATIVE_BLOCK_ITEM.get());
                output.accept(ModItems.REINFORCED_BLOCK_ITEM.get());
                
                // Add new blocks with BlockEntity
                output.accept(ModItems.SIMPLE_STORAGE_BLOCK_ITEM.get());
                output.accept(ModItems.ENERGY_STORAGE_BLOCK_ITEM.get());
                
                // Add MechaGrid block
                output.accept(ModItems.MECHA_GRID_BLOCK_ITEM.get());
            }).build());

    // Alternative tab focused on tools and materials 
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FIAT_LUX_TOOLS_TAB = 
        CREATIVE_MODE_TABS.register("fiat_lux_tools_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fiatlux.tools"))
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .icon(() -> ModItems.LIGHT_SWORD.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Add tools and materials
                output.accept(ModItems.LIGHT_SWORD.get());
                output.accept(ModItems.MAGIC_GEM.get());
                output.accept(ModItems.LIGHT_CRYSTAL.get());
                output.accept(ModItems.COMPRESSED_COAL.get());
                output.accept(ModItems.TORCH_ITEM.get());
                output.accept(ModItems.EXAMPLE_ITEM.get());
            }).build());

    // MechaModules tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MECHA_MODULES_TAB = 
        CREATIVE_MODE_TABS.register("mecha_modules_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fiatlux.mecha_modules"))
            .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .icon(() -> ModItems.TEST_MODULE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Add MechaGrid block first
                output.accept(ModItems.MECHA_GRID_BLOCK_ITEM.get());
                
                // Add all MechaModules
                output.accept(ModItems.TEST_MODULE_ITEM.get());
                output.accept(ModItems.ENERGY_GENERATOR_MODULE_ITEM.get());
                output.accept(ModItems.ENERGY_STORAGE_MODULE_ITEM.get());
                output.accept(ModItems.PROCESSOR_MODULE_ITEM.get());
                output.accept(ModItems.DISPLAY_MODULE_ITEM.get());
            }).build());

    /**
     * Register all creative mode tabs to the event bus
     * This method should be called in the mod constructor
     */
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
        FiatLux.LOGGER.info("Registering creative mode tabs for " + FiatLux.MODID);
    }
}