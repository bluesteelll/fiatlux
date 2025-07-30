package art.boyko.fiatlux.datagen;

import art.boyko.fiatlux.FiatLux;
import art.boyko.fiatlux.init.ModBlocks;
import art.boyko.fiatlux.init.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    
    public ModLanguageProvider(PackOutput output, String locale) {
        super(output, FiatLux.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        // Creative tabs
        add("itemGroup.fiatlux", "Fiat Lux");
        add("itemGroup.fiatlux.blocks", "Fiat Lux - Blocks");
        add("itemGroup.fiatlux.tools", "Fiat Lux - Tools & Materials");
        
        // Blocks
        addBlock(ModBlocks.EXAMPLE_BLOCK, "Example Block");
        addBlock(ModBlocks.LIGHT_BLOCK, "Light Block");
        addBlock(ModBlocks.DECORATIVE_BLOCK, "Decorative Block");
        addBlock(ModBlocks.REINFORCED_BLOCK, "Reinforced Block");
        
        // New blocks with BlockEntity
        addBlock(ModBlocks.SIMPLE_STORAGE_BLOCK, "Simple Storage Block");
        addBlock(ModBlocks.ENERGY_STORAGE_BLOCK, "Energy Storage Block");
        
        // Items
        addItem(ModItems.EXAMPLE_ITEM, "Example Item");
        addItem(ModItems.MAGIC_GEM, "Magic Gem");
        addItem(ModItems.LIGHT_SWORD, "Light Sword");
        addItem(ModItems.LIGHT_CRYSTAL, "Light Crystal");
        addItem(ModItems.COMPRESSED_COAL, "Compressed Coal");
        addItem(ModItems.TORCH_ITEM, "Eternal Torch");
        
        // Config translations
        add("fiatlux.configuration.title", "Fiat Lux Configs");
        add("fiatlux.configuration.section.fiatlux.common.toml", "Fiat Lux Configs");
        add("fiatlux.configuration.section.fiatlux.common.toml.title", "Fiat Lux Configs");
        add("fiatlux.configuration.items", "Item List");
        add("fiatlux.configuration.logDirtBlock", "Log Dirt Block");
        add("fiatlux.configuration.magicNumberIntroduction", "Magic Number Text");
        add("fiatlux.configuration.magicNumber", "Magic Number");
        
        // Tooltips and descriptions
        add("item.fiatlux.magic_gem.tooltip", "A mystical gem radiating with magical energy");
        add("item.fiatlux.light_sword.tooltip", "A sword infused with pure light");
        add("item.fiatlux.compressed_coal.tooltip", "Burns 4 times longer than regular coal");
        add("block.fiatlux.light_block.tooltip", "Illuminates the darkness");
        add("block.fiatlux.simple_storage_block.tooltip", "Stores items in a simple way");
        add("block.fiatlux.energy_storage_block.tooltip", "Stores and manages energy");
        
        // Creative tab descriptions
        add("itemGroup.fiatlux.tooltip", "Main Fiat Lux items and blocks");
        add("itemGroup.fiatlux.blocks.tooltip", "Decorative and functional blocks from Fiat Lux");
        add("itemGroup.fiatlux.tools.tooltip", "Tools, weapons and crafting materials from Fiat Lux");
        
        // BlockEntity related messages
        add("message.fiatlux.simple_storage.stored_items", "Stored items: %d");
        add("message.fiatlux.simple_storage.added_item", "Added item. New count: %d");
        add("message.fiatlux.simple_storage.removed_item", "Removed item. New count: %d");
        add("message.fiatlux.energy_storage.energy_info", "Energy: %d/%d FE");
        add("message.fiatlux.energy_storage.extracted", "Extracted %d FE");
        add("message.fiatlux.energy_storage.added", "Added %d FE");
    }
}