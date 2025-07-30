package art.boyko.fiatlux.custom.block;

import art.boyko.fiatlux.custom.blockentity.SimpleStorageBlockEntity;
import art.boyko.fiatlux.init.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SimpleStorageBlock extends BaseEntityBlock {
    public static final MapCodec<SimpleStorageBlock> CODEC = simpleCodec(SimpleStorageBlock::new);

    public SimpleStorageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleStorageBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SimpleStorageBlockEntity storageEntity) {
                int storedItems = storageEntity.getStoredItems();
                player.sendSystemMessage(Component.literal("Stored items: " + storedItems));
                
                // Simple interaction: right-click to add/remove items
                if (player.isShiftKeyDown()) {
                    storageEntity.removeItem();
                    player.sendSystemMessage(Component.literal("Removed item. New count: " + storageEntity.getStoredItems()));
                } else {
                    storageEntity.addItem();
                    player.sendSystemMessage(Component.literal("Added item. New count: " + storageEntity.getStoredItems()));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SimpleStorageBlockEntity) {
                // Handle block removal - could drop items here
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        
        return createTickerHelper(blockEntityType, ModBlockEntities.SIMPLE_STORAGE_BE.get(),
                (level1, pos, state1, blockEntity) -> blockEntity.tick(level1, pos, state1));
    }
}