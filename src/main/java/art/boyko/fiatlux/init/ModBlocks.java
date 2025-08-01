package art.boyko.fiatlux.init;

import art.boyko.fiatlux.FiatLux;
import art.boyko.fiatlux.custom.block.EnergyStorageBlock;
import art.boyko.fiatlux.custom.block.MechaGridBlock;
import art.boyko.fiatlux.custom.block.SimpleStorageBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    // Create a Deferred Register to hold Blocks which will all be registered under the "fiatlux" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FiatLux.MODID);

    // Example block 
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", 
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(3.0f, 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE)
    );

    // Light block example 
    public static final DeferredBlock<Block> LIGHT_BLOCK = BLOCKS.registerSimpleBlock("light_block",
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD)
            .strength(2.0f, 2.0f)
            .lightLevel(state -> 15)
            .sound(SoundType.GLASS)
    );

    // Decorative block example
    public static final DeferredBlock<Block> DECORATIVE_BLOCK = BLOCKS.registerSimpleBlock("decorative_block",
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(1.5f, 1.5f)
            .sound(SoundType.WOOD)
            .ignitedByLava()
    );

    // Unbreakable block example
    public static final DeferredBlock<Block> REINFORCED_BLOCK = BLOCKS.registerSimpleBlock("reinforced_block",
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(-1.0f, 3600000.0f)
            .sound(SoundType.NETHERITE_BLOCK)
            .pushReaction(PushReaction.BLOCK)
    );

    // Custom blocks with BlockEntity
    // Simple storage block with BlockEntity
    public static final DeferredBlock<SimpleStorageBlock> SIMPLE_STORAGE_BLOCK = BLOCKS.register("simple_storage_block",
        () -> new SimpleStorageBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)
            .requiresCorrectToolForDrops()
        )
    );

    // Energy storage block with BlockEntity
    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_BLOCK = BLOCKS.register("energy_storage_block",
        () -> new EnergyStorageBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .lightLevel(state -> 8) // Slightly glowing
        )
    );

// MechaGrid block - transparent block that holds 4x4x4 grid of blocks
public static final DeferredBlock<MechaGridBlock> MECHA_GRID_BLOCK = BLOCKS.register("mecha_grid_block",
    () -> new MechaGridBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.DIAMOND)
        .strength(3.0f, 8.0f)
        .sound(SoundType.GLASS)
        .requiresCorrectToolForDrops()
        .noOcclusion() // Important for transparency
        .isViewBlocking((state, level, pos) -> false) // Allow seeing through
        .isSuffocating((state, level, pos) -> false) // Don't suffocate entities
        .isRedstoneConductor((state, level, pos) -> false) // Don't conduct redstone
        .lightLevel(state -> 1) // Very slight glow to make it visible
    )
);
    /**
     * Register all blocks to the event bus
     * This method should be called in the mod constructor
     */
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        FiatLux.LOGGER.info("Registering blocks for " + FiatLux.MODID);
    }
}