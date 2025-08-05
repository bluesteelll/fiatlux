package art.boyko.fiatlux.datagen;

import art.boyko.fiatlux.FiatLux;
import art.boyko.fiatlux.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemModelProvider extends ItemModelProvider {
    
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, FiatLux.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleItem(ModItems.EXAMPLE_ITEM);
        simpleItem(ModItems.MAGIC_GEM);
        simpleItem(ModItems.LIGHT_CRYSTAL);
        simpleItem(ModItems.COMPRESSED_COAL);
        simpleItem(ModItems.TORCH_ITEM);
        
        handheldItem(ModItems.LIGHT_SWORD);
        
        // MechaModule items
        simpleItem(ModItems.TEST_MODULE_ITEM);
        simpleItem(ModItems.ENERGY_GENERATOR_MODULE_ITEM);
        simpleItem(ModItems.ENERGY_STORAGE_MODULE_ITEM);
        simpleItem(ModItems.PROCESSOR_MODULE_ITEM);
        simpleItem(ModItems.DISPLAY_MODULE_ITEM);
        
        // Block items are automatically handled by ModBlockStateProvider via simpleBlockWithItem()
        // No need to create them here
    }
    
    /**
     * Creates a simple item model with generated texture
     */
    private ItemModelBuilder simpleItem(DeferredItem<? extends Item> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(modid, "item/" + item.getId().getPath()));
    }
    
    /**
     * Creates a handheld item model (for tools/weapons)
     */
    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(modid, "item/" + item.getId().getPath()));
    }
}