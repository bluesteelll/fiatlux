package art.boyko.fiatlux.datagen;

import art.boyko.fiatlux.server.FiatLux;
import art.boyko.fiatlux.server.init.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ModBlockStateProvider extends BlockStateProvider {
    
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, FiatLux.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Simple cube blocks with all sides same texture
        blockWithItem(ModBlocks.EXAMPLE_BLOCK);
        blockWithItem(ModBlocks.LIGHT_BLOCK);
        blockWithItem(ModBlocks.DECORATIVE_BLOCK);
        blockWithItem(ModBlocks.REINFORCED_BLOCK);
        
        // Custom blocks with BlockEntity
        blockWithItem(ModBlocks.SIMPLE_STORAGE_BLOCK);
        blockWithItem(ModBlocks.ENERGY_STORAGE_BLOCK);
        
        // MechaGrid block - special transparent block
        mechaGridBlock();
    }
    
    /**
     * Creates a simple cube block model and blockstate
     * Also generates the block item model
     */
    private void blockWithItem(DeferredBlock<? extends Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
    
    /**
     * Creates a transparent block model for MechaGrid
     */
    private void mechaGridBlock() {
        Block block = ModBlocks.MECHA_GRID_BLOCK.get();
        
        // Create transparent glass-like model
        ModelFile model = models().cubeAll("mecha_grid_block", 
            ResourceLocation.fromNamespaceAndPath(FiatLux.MODID, "block/mecha_grid_block"));
        
        // Use the model for blockstate
        simpleBlock(block, model);
        
        // Create block item model
        itemModels().withExistingParent("mecha_grid_block", 
            ResourceLocation.fromNamespaceAndPath(FiatLux.MODID, "block/mecha_grid_block"));
    }
}