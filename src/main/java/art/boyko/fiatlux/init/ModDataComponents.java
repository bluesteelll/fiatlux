package art.boyko.fiatlux.init;

import art.boyko.fiatlux.FiatLux;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = 
        DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, FiatLux.MODID);

    // Data component for MechaGrid block data
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MechaGridData>> MECHA_GRID_DATA = 
        DATA_COMPONENTS.register("mecha_grid_data", () -> 
            DataComponentType.<MechaGridData>builder()
                .persistent(MechaGridData.CODEC)
                .networkSynchronized(MechaGridData.STREAM_CODEC)
                .build()
        );

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
        FiatLux.LOGGER.info("Registering data components for " + FiatLux.MODID);
    }

    // Data class for MechaGrid component
    public record MechaGridData(long occupiedMask, List<BlockEntry> blocks) {
        
        public static final Codec<MechaGridData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.LONG.fieldOf("occupied_mask").forGetter(MechaGridData::occupiedMask),
                BlockEntry.CODEC.listOf().fieldOf("blocks").forGetter(MechaGridData::blocks)
            ).apply(instance, MechaGridData::new)
        );

        public static final StreamCodec<FriendlyByteBuf, MechaGridData> STREAM_CODEC = 
            StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, 
                MechaGridData::occupiedMask,
                ByteBufCodecs.collection(java.util.ArrayList::new, BlockEntry.STREAM_CODEC, 64), 
                MechaGridData::blocks,
                MechaGridData::new
            );
    }

    // Helper record for block entries
    public record BlockEntry(int x, int y, int z, String blockId) {
        
        public static final Codec<BlockEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.fieldOf("x").forGetter(BlockEntry::x),
                Codec.INT.fieldOf("y").forGetter(BlockEntry::y),
                Codec.INT.fieldOf("z").forGetter(BlockEntry::z),
                Codec.STRING.fieldOf("block_id").forGetter(BlockEntry::blockId)
            ).apply(instance, BlockEntry::new)
        );

        public static final StreamCodec<FriendlyByteBuf, BlockEntry> STREAM_CODEC = 
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT, 
                BlockEntry::x,
                ByteBufCodecs.VAR_INT, 
                BlockEntry::y,
                ByteBufCodecs.VAR_INT, 
                BlockEntry::z,
                ByteBufCodecs.STRING_UTF8, 
                BlockEntry::blockId,
                BlockEntry::new
            );
    }
}