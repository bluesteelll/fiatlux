package art.boyko.fiatlux.server.init;

import art.boyko.fiatlux.server.FiatLux;
import art.boyko.fiatlux.server.items.TestModuleItem;
import art.boyko.fiatlux.server.items.EnergyGeneratorModuleItem;
import art.boyko.fiatlux.server.items.EnergyStorageModuleItem;
import art.boyko.fiatlux.server.items.ProcessorModuleItem;
import art.boyko.fiatlux.server.items.DisplayModuleItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    // Create a Deferred Register to hold Items which will all be registered under the "fiatlux" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FiatLux.MODID);

    // Block Items - automatically create items for blocks
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = 
        ITEMS.registerSimpleBlockItem("example_block", ModBlocks.EXAMPLE_BLOCK);
    
    public static final DeferredItem<BlockItem> LIGHT_BLOCK_ITEM = 
        ITEMS.registerSimpleBlockItem("light_block", ModBlocks.LIGHT_BLOCK);
    
    public static final DeferredItem<BlockItem> DECORATIVE_BLOCK_ITEM = 
        ITEMS.registerSimpleBlockItem("decorative_block", ModBlocks.DECORATIVE_BLOCK);
    
    public static final DeferredItem<BlockItem> REINFORCED_BLOCK_ITEM = 
        ITEMS.registerSimpleBlockItem("reinforced_block", ModBlocks.REINFORCED_BLOCK);

    // Block items for custom blocks with BlockEntity
    public static final DeferredItem<BlockItem> SIMPLE_STORAGE_BLOCK_ITEM = 
        ITEMS.registerSimpleBlockItem("simple_storage_block", ModBlocks.SIMPLE_STORAGE_BLOCK);
    
    public static final DeferredItem<BlockItem> ENERGY_STORAGE_BLOCK_ITEM = 
        ITEMS.registerSimpleBlockItem("energy_storage_block", ModBlocks.ENERGY_STORAGE_BLOCK);

    // MechaGrid block item
    public static final DeferredItem<BlockItem> MECHA_GRID_BLOCK_ITEM = 
        ITEMS.registerSimpleBlockItem("mecha_grid_block", ModBlocks.MECHA_GRID_BLOCK);

    // Regular Items
    // Example food item
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", 
        new Item.Properties()
            .food(new FoodProperties.Builder()
                .alwaysEdible()
                .nutrition(1)
                .saturationModifier(2f)
                .build())
    );

    // Magic gem example 
    public static final DeferredItem<Item> MAGIC_GEM = ITEMS.registerSimpleItem("magic_gem",
        new Item.Properties()
            .stacksTo(16)
            .rarity(Rarity.RARE)
            .fireResistant()
    );

    // Custom sword
    public static final DeferredItem<SwordItem> LIGHT_SWORD = ITEMS.register("light_sword",
        () -> new SwordItem(Tiers.DIAMOND, 
            new Item.Properties()
                .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 4, -2.0f))
                .rarity(Rarity.UNCOMMON)
        )
    );

    // Material item example
    public static final DeferredItem<Item> LIGHT_CRYSTAL = ITEMS.registerSimpleItem("light_crystal",
        new Item.Properties()
            .stacksTo(64)
            .rarity(Rarity.COMMON)
    );

    // Fuel item example 
    public static final DeferredItem<Item> COMPRESSED_COAL = ITEMS.register("compressed_coal",
        () -> new Item(new Item.Properties().stacksTo(64)) {
            @Override
            public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
                return 3200; // Burns 4 times longer than regular coal
            }
        }
    );

    // Glowing item example 
    public static final DeferredItem<Item> TORCH_ITEM = ITEMS.registerSimpleItem("torch_item",
        new Item.Properties()
            .stacksTo(1)
            .rarity(Rarity.UNCOMMON)
    );

    // MechaModule items
    public static final DeferredItem<TestModuleItem> TEST_MODULE_ITEM = 
        ITEMS.register("test_module", TestModuleItem::new);
    
    public static final DeferredItem<EnergyGeneratorModuleItem> ENERGY_GENERATOR_MODULE_ITEM = 
        ITEMS.register("energy_generator_module", EnergyGeneratorModuleItem::new);
    
    public static final DeferredItem<EnergyStorageModuleItem> ENERGY_STORAGE_MODULE_ITEM = 
        ITEMS.register("energy_storage_module", EnergyStorageModuleItem::new);
    
    public static final DeferredItem<ProcessorModuleItem> PROCESSOR_MODULE_ITEM = 
        ITEMS.register("processor_module", ProcessorModuleItem::new);
    
    public static final DeferredItem<DisplayModuleItem> DISPLAY_MODULE_ITEM = 
        ITEMS.register("display_module", DisplayModuleItem::new);

    /**
     * Register all items to the event bus
     * This method should be called in the mod constructor
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        FiatLux.LOGGER.info("Registering items for " + FiatLux.MODID);
    }
}