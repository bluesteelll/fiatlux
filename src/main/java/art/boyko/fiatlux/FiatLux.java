package art.boyko.fiatlux;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import art.boyko.fiatlux.init.ModBlocks;
import art.boyko.fiatlux.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FiatLux.MODID)
public class FiatLux {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "fiatlux";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "fiatlux" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a creative tab with the id "fiatlux:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.fiatlux")) 
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // Add all mod items to the creative tab
                output.accept(ModItems.EXAMPLE_ITEM.get());
                output.accept(ModItems.MAGIC_GEM.get());
                output.accept(ModItems.LIGHT_SWORD.get());
                output.accept(ModItems.LIGHT_CRYSTAL.get());
                output.accept(ModItems.COMPRESSED_COAL.get());
                output.accept(ModItems.TORCH_ITEM.get());
                
                // Add all block items to the creative tab
                output.accept(ModItems.EXAMPLE_BLOCK_ITEM.get());
                output.accept(ModItems.LIGHT_BLOCK_ITEM.get());
                output.accept(ModItems.DECORATIVE_BLOCK_ITEM.get());
                output.accept(ModItems.REINFORCED_BLOCK_ITEM.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public FiatLux(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        
        CREATIVE_MODE_TABS.register(modEventBus);

        // Note that this is necessary if and only if we want *this* class (FiatLux) to respond directly to events.
        NeoForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.EXAMPLE_BLOCK_ITEM);
            event.accept(ModItems.LIGHT_BLOCK_ITEM);
            event.accept(ModItems.DECORATIVE_BLOCK_ITEM);
            event.accept(ModItems.REINFORCED_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}