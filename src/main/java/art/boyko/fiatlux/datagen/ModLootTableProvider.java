package art.boyko.fiatlux.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import art.boyko.fiatlux.init.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class ModLootTableProvider extends LootTableProvider {
    
    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)
        ), registries);
    }
    
    public static class ModBlockLootTables extends BlockLootSubProvider {
        
        public ModBlockLootTables(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            // Simple blocks that drop themselves
            dropSelf(ModBlocks.EXAMPLE_BLOCK.get());
            dropSelf(ModBlocks.LIGHT_BLOCK.get());
            dropSelf(ModBlocks.DECORATIVE_BLOCK.get());
            dropSelf(ModBlocks.REINFORCED_BLOCK.get());
            
            // Blocks with BlockEntity that drop themselves
            // Note: BlockEntity data is automatically preserved in item form
            dropSelf(ModBlocks.SIMPLE_STORAGE_BLOCK.get());
            dropSelf(ModBlocks.ENERGY_STORAGE_BLOCK.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream()
                    .map(registryObject -> (Block) registryObject.get())
                    .toList();
        }
    }
}