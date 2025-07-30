package art.boyko.fiatlux.datagen;

import art.boyko.fiatlux.FiatLux;
import art.boyko.fiatlux.init.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
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
    }
    
    /**
     * Creates a simple cube block model and blockstate
     * Also generates the block item model
     */
    private void blockWithItem(DeferredBlock<Block> deferredBlock) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }
}