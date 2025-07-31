package art.boyko.fiatlux;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import art.boyko.fiatlux.init.ModBlockEntities;
import art.boyko.fiatlux.init.ModBlocks;
import art.boyko.fiatlux.init.ModCreativeTabs;
import art.boyko.fiatlux.init.ModDataComponents;
import art.boyko.fiatlux.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
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

@Mod(FiatLux.MODID)
public class FiatLux {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "fiatlux";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public FiatLux(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register all mod content
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus); // Register block entities
        ModCreativeTabs.register(modEventBus);
        ModDataComponents.register(modEventBus); // Register data components

        // Note that this is necessary if and only if we want *this* class (FiatLux) to respond directly to events.
        NeoForge.EVENT_BUS.register(this);

        // Add items to existing vanilla creative tabs
        modEventBus.addListener(this::addCreative);

        // Register mod configuration
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

    // Add the mod block items to existing vanilla creative tabs
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.EXAMPLE_BLOCK_ITEM);
            event.accept(ModItems.LIGHT_BLOCK_ITEM);
            event.accept(ModItems.DECORATIVE_BLOCK_ITEM);
            event.accept(ModItems.REINFORCED_BLOCK_ITEM);
            // Add new blocks with BlockEntity
            event.accept(ModItems.SIMPLE_STORAGE_BLOCK_ITEM);
            event.accept(ModItems.ENERGY_STORAGE_BLOCK_ITEM);
            // Add MechaGrid block
            event.accept(ModItems.MECHA_GRID_BLOCK_ITEM);
        }
        
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.LIGHT_SWORD);
            event.accept(ModItems.TORCH_ITEM);
        }
        
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.MAGIC_GEM);
            event.accept(ModItems.LIGHT_CRYSTAL);
            event.accept(ModItems.COMPRESSED_COAL);
        }
        
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.EXAMPLE_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}