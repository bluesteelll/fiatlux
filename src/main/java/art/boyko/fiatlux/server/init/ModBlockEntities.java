package art.boyko.fiatlux.server.init;

import art.boyko.fiatlux.server.FiatLux;
import art.boyko.fiatlux.server.blockentity.EnergyStorageBlockEntity;
import art.boyko.fiatlux.server.blockentity.MechaGridBlockEntity;
import art.boyko.fiatlux.server.blockentity.SimpleStorageBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "fiatlux" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, FiatLux.MODID);

    // Simple storage block entity example
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SimpleStorageBlockEntity>> SIMPLE_STORAGE_BE = 
        BLOCK_ENTITIES.register("simple_storage_be", () -> 
            BlockEntityType.Builder.of(SimpleStorageBlockEntity::new, 
                ModBlocks.SIMPLE_STORAGE_BLOCK.get()).build(null));

    // Energy storage block entity example
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_BE = 
        BLOCK_ENTITIES.register("energy_storage_be", () -> 
            BlockEntityType.Builder.of(EnergyStorageBlockEntity::new, 
                ModBlocks.ENERGY_STORAGE_BLOCK.get()).build(null));

    // MechaGrid block entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechaGridBlockEntity>> MECHA_GRID_BE = 
        BLOCK_ENTITIES.register("mecha_grid_be", () -> 
            BlockEntityType.Builder.of(MechaGridBlockEntity::new, 
                ModBlocks.MECHA_GRID_BLOCK.get()).build(null));

    /**
     * Register all block entities to the event bus
     * This method should be called in the mod constructor
     */
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
        FiatLux.LOGGER.info("Registering block entities for " + FiatLux.MODID);
    }
}